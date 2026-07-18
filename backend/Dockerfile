FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
EXPOSE 8080
# Explicit heap/metaspace/thread-stack caps: free-tier PaaS containers (e.g. Render's 512MB)
# leave little headroom for the JVM's own defaults, which size themselves off total host
# memory rather than the container's cgroup limit in some environments — without these caps
# the app was getting OOM-killed and restart-looping mid-startup on Render's free tier.
ENTRYPOINT ["java", "-Duser.timezone=UTC", "-Xmx320m", "-XX:MaxMetaspaceSize=128m", "-Xss256k", "-jar", "app.jar"]
