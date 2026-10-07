SHELL := /bin/zsh
export JAVA_HOME := $(shell /usr/libexec/java_home -v 21)
export PATH := $(JAVA_HOME)/bin:$(PATH)

.PHONY: setup run build test clean

setup:
	@java -version

run:
	@./gradlew runClient

build: setup
	@./gradlew build

test:
	@./gradlew runGameTestServer

clean:
	@./gradlew clean
