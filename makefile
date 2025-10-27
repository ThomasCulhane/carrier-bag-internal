.PHONY: build-frontend-local start-frontend-local build-prod start-prod build-server-local start-server-local start-local download-server-libs docker-build-server download-server-libs

# Java Server Directories
SV_DIR = server
SV_SRC_DIR=$(SV_DIR)/src
SV_OUT_DIR=$(SV_DIR)/target
SV_LIB_DIR=$(SV_DIR)/lib

# LOCAL DEVELOPMENT TARGETS
download-server-libs:
	@mkdir -p $(SV_LIB_DIR)
	@if [ ! -f $(SV_LIB_DIR)/postgresql.jar ]; then \
		echo "Downloading postgres JDBC..."; \
		curl -sSL -o $(SV_LIB_DIR)/postgresql.jar https://repo1.maven.org/maven2/org/postgresql/postgresql/42.6.0/postgresql-42.6.0.jar; \
	fi
	@if [ ! -f $(SV_LIB_DIR)/jackson-databind.jar ]; then \
		echo "Downloading jackson jars..."; \
		curl -sSL -o $(SV_LIB_DIR)/jackson-databind.jar https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-databind/2.15.2/jackson-databind-2.15.2.jar; \
		curl -sSL -o $(SV_LIB_DIR)/jackson-core.jar https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-core/2.15.2/jackson-core-2.15.2.jar; \
		curl -sSL -o $(SV_LIB_DIR)/jackson-annotations.jar https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-annotations/2.15.2/jackson-annotations-2.15.2.jar; \
	fi

# compile server Java sources (includes handlers/)
$(SV_OUT_DIR)/Main.class: $(SV_SRC_DIR)/Main.java $(wildcard $(SV_SRC_DIR)/handlers/*.java) | download-server-libs
	@mkdir -p $(SV_OUT_DIR)
	javac -d $(SV_OUT_DIR) -cp "$(SV_LIB_DIR)/*" $(SV_SRC_DIR)/Main.java $(SV_SRC_DIR)/handlers/*.java

build-server-local: $(SV_OUT_DIR)/Main.class

# build docker image for server (uses Dockerfile in server/)
docker-build-server:
	docker build -t carrier-bag-server -f $(SV_DIR)/Dockerfile $(SV_DIR)

# convenience: build everything needed locally
start-local: build-server-local

build-prod:
	docker-compose --env-file ./env/ports.env.production build

start-prod:
	docker-compose --env-file ./env/ports.env.production up