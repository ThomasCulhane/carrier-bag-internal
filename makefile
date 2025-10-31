.PHONY: build-frontend-local start-frontend-local build-prod start-prod build-server-local start-server-local start-local download-server-libs docker-build-server download-server-libs lint-server

# Java Server Directories
SV_DIR = server
SV_SRC_DIR=$(SV_DIR)/src
SV_OUT_DIR=$(SV_DIR)/target
SV_LIB_DIR=$(SV_DIR)/lib

JAVAC_COMMAND = javac -d $(SV_OUT_DIR) -cp "$(SV_LIB_DIR)/*" $(SV_SRC_DIR)/Main.java $(SV_SRC_DIR)/handlers/*.java

# LOCAL DEVELOPMENT TARGETS
download-server-libs: $(SV_DIR)/download_libs.sh

$(SV_DIR)/download_libs.sh:
	cd $(SV_DIR) && bash download_libs.sh

# compile server Java sources (includes handlers/)
$(SV_OUT_DIR)/Main.class: $(SV_SRC_DIR)/Main.java $(wildcard $(SV_SRC_DIR)/handlers/*.java) $(SV_DIR)/download_libs.sh
	bash -c "mkdir -p $(SV_OUT_DIR)"
	$(JAVAC_COMMAND)

build-server-local: $(SV_OUT_DIR)/Main.class

lint-server:
	@LINT_OUTPUT=$($(JAVAC_COMMAND) -Xlint:all -Werror 2>&1)
ifeq ($(strip $(LINT_OUTPUT)),)
	@echo "Linting did not return any errors."
else
	@echo $(LINT_OUTPUT)
endif

# build docker image for server (uses Dockerfile in server/)
docker-build-server:
	docker build -t carrier-bag-server -f $(SV_DIR)/Dockerfile $(SV_DIR)

# convenience: build everything needed locally
start-local: build-server-local

build-prod:
	docker-compose --env-file ./env/compose.env build

start-prod:
	docker-compose --env-file ./env/compose.env up