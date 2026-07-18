package com.agrodairy.ai.client;

import com.agrodairy.ai.dto.DemandForecastRequest;
import com.agrodairy.ai.dto.DemandForecastResponse;
import com.agrodairy.common.exception.ApiException;
import com.agrodairy.common.exception.NotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

/**
 * Talks to ai-service directly over the JDK's HttpClient rather than Spring's RestClient, which
 * reproducibly sent empty POST bodies against uvicorn in this environment. Root cause: the JDK
 * HttpClient defaults to HTTP/2 with an h2c upgrade attempt over cleartext, which uvicorn (HTTP/1.1
 * only) does not negotiate correctly, silently dropping the request body. Forcing HTTP/1.1 below
 * fixes it.
 *
 * The ObjectMapper here is the classic Jackson 2 one (pulled in transitively via jjwt-jackson),
 * not Spring Boot 4.1's auto-configured Jackson 3 bean — kept as a local static instance since no
 * Jackson 2 ObjectMapper bean exists in this app's context. findAndRegisterModules() picks up the
 * jsr310 module already on the classpath so LocalDate fields in the response deserialize correctly.
 */
@Component
public class AiServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AiServiceClient.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final String baseUrl;
    private final String internalApiKey;

    public AiServiceClient(@Value("${app.ai-service.base-url}") String baseUrl,
                            @Value("${app.ai-service.internal-api-key}") String internalApiKey) {
        this.baseUrl = baseUrl;
        this.internalApiKey = internalApiKey;
    }

    public DemandForecastResponse predictDemand(UUID productId, int horizonDays) {
        try {
            String requestBody = OBJECT_MAPPER.writeValueAsString(new DemandForecastRequest(productId, horizonDays));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/predict/demand"))
                    .header("Content-Type", "application/json")
                    .header("X-Internal-Api-Key", internalApiKey)
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 404) {
                throw new NotFoundException("No forecast model available for this product yet");
            }
            if (response.statusCode() != 200) {
                log.error("ai-service returned {} for /predict/demand: {}", response.statusCode(), response.body());
                throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "AI_SERVICE_UNAVAILABLE",
                        "The AI forecasting service returned an error");
            }
            return OBJECT_MAPPER.readValue(response.body(), DemandForecastResponse.class);
        } catch (IOException e) {
            log.error("Failed to reach ai-service for /predict/demand", e);
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "AI_SERVICE_UNAVAILABLE",
                    "The AI forecasting service is unavailable");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "AI_SERVICE_UNAVAILABLE",
                    "The AI forecasting service call was interrupted");
        }
    }
}
