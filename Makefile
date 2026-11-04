RELEASE_VERSION = 0.6.2
DEV_VERSION = 0.6.3-SNAPSHOT
RUNNER ?= $(shell pwd)/target/jolokia-mcp-${DEV_VERSION}-runner.jar

mcp-inspector:
	npx @modelcontextprotocol/inspector

mcp-cli:
	npx @wong2/mcp-cli java -jar ${RUNNER} $(ARGS)

download-runner:
	curl -LO https://repo1.maven.org/maven2/org/jolokia/mcp/jolokia-mcp-server/${RELEASE_VERSION}/jolokia-mcp-server-${RELEASE_VERSION}-runner.jar
	curl -LO https://repo1.maven.org/maven2/org/jolokia/mcp/jolokia-mcp-server/${RELEASE_VERSION}/jolokia-mcp-server-${RELEASE_VERSION}-runner.jar.asc
	curl -LO https://repo1.maven.org/maven2/org/jolokia/mcp/jolokia-mcp-server/${RELEASE_VERSION}/jolokia-mcp-server-${RELEASE_VERSION}-runner.jar.md5
	curl -LO https://repo1.maven.org/maven2/org/jolokia/mcp/jolokia-mcp-server/${RELEASE_VERSION}/jolokia-mcp-server-${RELEASE_VERSION}-runner.jar.sha1
	curl -LO https://repo1.maven.org/maven2/org/jolokia/mcp/jolokia-mcp-server/${RELEASE_VERSION}/jolokia-mcp-server-${RELEASE_VERSION}-runner.jar.sha256
	curl -LO https://repo1.maven.org/maven2/org/jolokia/mcp/jolokia-mcp-server/${RELEASE_VERSION}/jolokia-mcp-server-${RELEASE_VERSION}-runner.jar.sha512
	$(MAKE) verify-runner

verify-runner:
	@echo "$$(cat jolokia-mcp-server-${RELEASE_VERSION}-runner.jar.sha256)  jolokia-mcp-server-${RELEASE_VERSION}-runner.jar" | shasum -a 256 -c - \
		|| (echo "SHA-256 verification failed for jolokia-mcp-server-${RELEASE_VERSION}-runner.jar" && exit 1)

download-javaagent:
	curl -LO https://repo1.maven.org/maven2/org/jolokia/mcp/jolokia-mcp-agent-jvm/${RELEASE_VERSION}/jolokia-mcp-agent-jvm-${RELEASE_VERSION}-javaagent.jar
	curl -LO https://repo1.maven.org/maven2/org/jolokia/mcp/jolokia-mcp-agent-jvm/${RELEASE_VERSION}/jolokia-mcp-agent-jvm-${RELEASE_VERSION}-javaagent.jar.asc
	curl -LO https://repo1.maven.org/maven2/org/jolokia/mcp/jolokia-mcp-agent-jvm/${RELEASE_VERSION}/jolokia-mcp-agent-jvm-${RELEASE_VERSION}-javaagent.jar.md5
	curl -LO https://repo1.maven.org/maven2/org/jolokia/mcp/jolokia-mcp-agent-jvm/${RELEASE_VERSION}/jolokia-mcp-agent-jvm-${RELEASE_VERSION}-javaagent.jar.sha1
	curl -LO https://repo1.maven.org/maven2/org/jolokia/mcp/jolokia-mcp-agent-jvm/${RELEASE_VERSION}/jolokia-mcp-agent-jvm-${RELEASE_VERSION}-javaagent.jar.sha256
	curl -LO https://repo1.maven.org/maven2/org/jolokia/mcp/jolokia-mcp-agent-jvm/${RELEASE_VERSION}/jolokia-mcp-agent-jvm-${RELEASE_VERSION}-javaagent.jar.sha512
	$(MAKE) verify-javaagent

verify-javaagent:
	@echo "$$(cat jolokia-mcp-agent-jvm-${RELEASE_VERSION}-javaagent.jar.sha256)  jolokia-mcp-agent-jvm-${RELEASE_VERSION}-javaagent.jar" | shasum -a 256 -c - \
		|| (echo "SHA-256 verification failed for jolokia-mcp-agent-jvm-${RELEASE_VERSION}-javaagent.jar" && exit 1)

verify-gpg:
	gpg --verify jolokia-mcp-server-${RELEASE_VERSION}-runner.jar.asc jolokia-mcp-server-${RELEASE_VERSION}-runner.jar
	gpg --verify jolokia-mcp-agent-jvm-${RELEASE_VERSION}-javaagent.jar.asc jolokia-mcp-agent-jvm-${RELEASE_VERSION}-javaagent.jar
