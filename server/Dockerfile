# ==== Build stage ====
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# Copy the full project to build only the server module
COPY .. /app

# Build the server module jar
RUN ./mvnw -q -DskipTests -pl server -am package || mvn -q -DskipTests -pl server -am package

# ==== Runtime stage ====
FROM eclipse-temurin:17-jre
ENV JAVA_OPTS="-Xms256m -Xmx512m"
WORKDIR /opt/app

COPY --from=build /app/server/target/*.jar /opt/app/app.jar

EXPOSE 8080
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /opt/app/app.jar"]
