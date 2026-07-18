package com.agrodairy.auth.security;

import com.agrodairy.common.exception.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;

/**
 * Verifies a Google "Sign in with Google" ID token by asking Google's own tokeninfo endpoint,
 * rather than pulling in google-api-client's JWKS-verification stack — that library's dependency
 * tree (guava, google-http-client, etc.) risks reintroducing the OOM/startup-time pressure this
 * app's JVM memory limits were specifically tuned against (see backend/Dockerfile). A network
 * round-trip per login is a fine tradeoff at this app's traffic volume.
 */
@Component
public class GoogleTokenVerifier {

    private static final Logger log = LoggerFactory.getLogger(GoogleTokenVerifier.class);
    private static final Set<String> VALID_ISSUERS = Set.of("accounts.google.com", "https://accounts.google.com");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final String clientId;

    public GoogleTokenVerifier(@Value("${app.google.client-id:}") String clientId) {
        this.clientId = clientId;
    }

    public GoogleIdentity verify(String idToken) {
        if (clientId.isBlank()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "GOOGLE_SIGNIN_NOT_CONFIGURED",
                    "Google sign-in is not configured on this server");
        }
        try {
            String url = "https://oauth2.googleapis.com/tokeninfo?id_token="
                    + URLEncoder.encode(idToken, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Invalid Google sign-in token");
            }
            JsonNode body = OBJECT_MAPPER.readTree(response.body());

            String issuer = body.path("iss").asText("");
            if (!VALID_ISSUERS.contains(issuer)) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Invalid Google sign-in token");
            }
            String audience = body.path("aud").asText("");
            if (!clientId.equals(audience)) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Invalid Google sign-in token");
            }

            String email = body.path("email").asText(null);
            boolean emailVerified = "true".equals(body.path("email_verified").asText(""));
            String sub = body.path("sub").asText(null);
            String name = body.path("name").asText(email);
            if (email == null || sub == null) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Invalid Google sign-in token");
            }
            return new GoogleIdentity(sub, email, emailVerified, name);
        } catch (IOException e) {
            log.error("Failed to verify Google sign-in token", e);
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "GOOGLE_SIGNIN_UNAVAILABLE",
                    "Could not verify the Google sign-in token");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "GOOGLE_SIGNIN_UNAVAILABLE",
                    "The Google sign-in verification call was interrupted");
        }
    }
}
