# Build Stage: use Java SDK to compile the application
FROM ibmjava:sdk-alpine AS build

WORKDIR /app
COPY src .
RUN mkdir target

RUN javac Main.java -d target

# Runtime Stage: use IBM Small Footprint Java Runtime to run the application
FROM ibmjava:sfj-alpine AS runtime
WORKDIR /app
COPY --from=build /app/target .

EXPOSE 8080
CMD ["java", "Main"]