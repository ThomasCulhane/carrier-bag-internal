# Build Stage: use Java SDK to compile the application and fetch runtime libs
FROM amazoncorretto:21-alpine AS base

FROM base AS build

WORKDIR /app

# install curl/ca-certificates to download jars
RUN apk add --no-cache curl ca-certificates

# copy java source and static files
COPY ./src/ ./
COPY ./static/ ./static
COPY ./download_libs.sh ./

# create lib folder and download postgres jdbc + jackson for compilation
RUN ./download_libs.sh

# compile all sources (include downloaded jars on classpath)
RUN mkdir -p target && pwd && javac -d target -cp "lib/*" Main.java handlers/*.java

# Runtime Stage
FROM base AS runtime
WORKDIR /app

# copy static files and compiled classes from build stage
COPY --from=build /app/static ./static
COPY --from=build /app/target ./
# copy runtime libs
COPY --from=build /app/lib ./lib

EXPOSE ${SERVER_EXPOSED_PORT:-8080}

RUN pwd && ls

# run with lib jars on classpath
CMD ["java", "-cp", ".:lib/*:target", "Main"]