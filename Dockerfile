FROM eclipse-temurin:21-jdk AS build
WORKDIR /build

# Wrapper e pom antes do código, para aproveitar o cache de camadas das dependências.
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -B dependency:go-offline

COPY src src
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd --system --no-create-home appuser
COPY --from=build /build/target/pode-gastar-*.jar app.jar
USER appuser

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
