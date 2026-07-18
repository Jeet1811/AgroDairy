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
# Explicit, tight memory caps for 512MB free-tier containers (e.g. Render). -Xmx alone isn't
# enough — JIT code cache, direct-memory buffers, and G1's region bookkeeping all sit on top
# of the heap and were pushing total usage over 512MB even with -Xmx320m, causing repeated
# OOM-kills/restart-looping. UseSerialGC trades multi-threaded GC (irrelevant at this heap
# size and low traffic) for much lower memory overhead than the default G1 collector.
ENTRYPOINT ["java", "-Duser.timezone=UTC", \
    "-XX:+UseSerialGC", \
    "-Xss256k", \
    "-Xmx200m", \
    "-XX:MaxMetaspaceSize=96m", \
    "-XX:MaxDirectMemorySize=32m", \
    "-XX:ReservedCodeCacheSize=64m", \
    "-XX:CICompilerCount=2", \
    "-jar", "app.jar"]
