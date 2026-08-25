RELEASE_VERSION = 0.5.2
DEV_VERSION = 0.5.3-SNAPSHOT
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

download-javaagent:
	curl -LO https://repo1.maven.org/maven2/org/jolokia/mcp/jolokia-mcp-agent-jvm/${RELEASE_VERSION}/jolokia-mcp-agent-jvm-${RELEASE_VERSION}-javaagent.jar
	curl -LO https://repo1.maven.org/maven2/org/jolokia/mcp/jolokia-mcp-agent-jvm/${RELEASE_VERSION}/jolokia-mcp-agent-jvm-${RELEASE_VERSION}-javaagent.jar.asc
	curl -LO https://repo1.maven.org/maven2/org/jolokia/mcp/jolokia-mcp-agent-jvm/${RELEASE_VERSION}/jolokia-mcp-agent-jvm-${RELEASE_VERSION}-javaagent.jar.md5
	curl -LO https://repo1.maven.org/maven2/org/jolokia/mcp/jolokia-mcp-agent-jvm/${RELEASE_VERSION}/jolokia-mcp-agent-jvm-${RELEASE_VERSION}-javaagent.jar.sha1
	curl -LO https://repo1.maven.org/maven2/org/jolokia/mcp/jolokia-mcp-agent-jvm/${RELEASE_VERSION}/jolokia-mcp-agent-jvm-${RELEASE_VERSION}-javaagent.jar.sha256
	curl -LO https://repo1.maven.org/maven2/org/jolokia/mcp/jolokia-mcp-agent-jvm/${RELEASE_VERSION}/jolokia-mcp-agent-jvm-${RELEASE_VERSION}-javaagent.jar.sha512
