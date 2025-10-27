# Build Stage: use Java SDK to compile the application and fetch runtime libs
FROM amazoncorretto:21-alpine AS base

FROM base AS build

WORKDIR /app

# install curl/ca-certificates to download jars
RUN apk add --no-cache curl ca-certificates

# copy java source and static files
COPY ./src/ ./
COPY ./static/ ./static
# create lib folder and download postgres jdbc + jackson for compilation
RUN mkdir -p /lib && \
    curl -L -o /lib/postgresql.jar https://repo1.maven.org/maven2/org/postgresql/postgresql/42.6.0/postgresql-42.6.0.jar && \
    curl -L -o /lib/jackson-databind.jar https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-databind/2.15.2/jackson-databind-2.15.2.jar && \
    curl -L -o /lib/jackson-core.jar https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-core/2.15.2/jackson-core-2.15.2.jar && \
    curl -L -o /lib/jackson-annotations.jar https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-annotations/2.15.2/jackson-annotations-2.15.2.jar

# compile all sources (include downloaded jars on classpath)
RUN mkdir -p target && javac -d target -cp "/lib/*" Main.java handlers/*.java

# Runtime Stage
FROM base AS runtime
WORKDIR /app

# copy static files and compiled classes from build stage
COPY --from=build /app/static ./static
COPY --from=build /app/target ./
# copy runtime libs
COPY --from=build /app/lib /app/lib

EXPOSE ${SERVER_EXPOSED_PORT:-8080}

# run with lib jars on classpath
CMD ["java", "-cp", ".:/app/lib/*:target", "Main"]