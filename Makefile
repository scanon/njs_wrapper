KB_TOP ?= /kb/dev_container
KB_RUNTIME ?= /kb/runtime
DEPLOY_RUNTIME ?= $(KB_RUNTIME)
TARGET ?= /kb/deployment
CURR_DIR = $(shell pwd)
SERVICE_NAME = $(shell basename $(CURR_DIR))
SERVICE_CAPS = NarrativeJobService
SERVICE_DIR = $(TARGET)/services/$(SERVICE_NAME)
WAR_FILE = NJSWrapper.war

ASADMIN = $(GLASSFISH_HOME)/glassfish/bin/asadmin

SERVICE_PORT = 8200
THREADPOOL_SIZE = 50

default: compile

deploy-all: deploy

deploy: deploy-client deploy-service deploy-scripts deploy-docs

test: test-client test-service test-scripts

test-client:
	@echo "No tests for client"

test-service:
	@echo "No tests for service"

test-scripts:
	@echo "No tests for scripts"

compile: src
	ant war

deploy-client:
	@echo "No deployment for client"

deploy-service:
	@echo "Service folder: $(SERVICE_DIR)"
	mkdir -p $(SERVICE_DIR)
	cp -f ./deploy.cfg $(SERVICE_DIR)
	cp -f ./dist/$(WAR_FILE) $(SERVICE_DIR)
	mkdir $(SERVICE_DIR)/webapps
	cp ./dist/$(WAR_FILE) $(SERVICE_DIR)/webapps/root.war
	cp server_scripts/jetty.xml $(SERVICE_DIR)
	server_scripts/build_server_control_scripts.py $(SERVICE_DIR) $(WAR_FILE)\
		$(TARGET) $(JAVA_HOME) deploy.cfg $(ASADMIN) $(SERVICE_CAPS)\
		$(SERVICE_PORT)

	chmod +x $(SERVICE_DIR)/start_service
	chmod +x $(SERVICE_DIR)/stop_service

deploy-scripts:
	@echo "No deployment for scripts"

deploy-docs:
	@echo "No documentation"

clean:
	ant clean
