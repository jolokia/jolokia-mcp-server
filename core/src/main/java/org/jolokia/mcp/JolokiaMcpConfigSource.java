/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jolokia.mcp;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;

public class JolokiaMcpConfigSource implements ConfigSource {

    private static final String QUARKUS_HTTP_HOST_ENABLED = "quarkus.http.host-enabled";
    private static final String QUARKUS_HTTP_PORT = "quarkus.http.port";
    private static final String QUARKUS_MCP_SERVER_STDIO_ENABLED = "quarkus.mcp.server.stdio.enabled";
    private static final String QUARKUS_MCP_SERVER_HTTP_ROOT_PATH = "quarkus.mcp.server.http.root-path";
    private static final String JOLOKIA_MCP_URL = "jolokia.mcp.url";
    private static final String JOLOKIA_MCP_PREFERRED_HTTP_METHOD = "jolokia.mcp.preferred-http-method";

    private static final Map<String, String> configuration = new HashMap<>();

    public static void setup(String... args) {
        List<String> remains = Arrays.stream(args)
            .filter(arg -> {
                if (arg.startsWith("--") || arg.startsWith("-D")) {
                    String[] parts = arg.substring(2).split("=");
                    if (parts.length == 2) {
                        configuration.put(parts[0], parts[1]);
                    } else {
                        configuration.put(parts[0], "true");
                    }
                    return false;
                }
                return !arg.startsWith("-");
            })
            .toList();
        remains.stream()
            .findFirst()
            .ifPresent(arg -> configuration.put(JOLOKIA_MCP_URL, arg));

        // Set up options
        setupPort();
        setupMethod();
        // Set up HTTP last as other options can affect it
        setupHttp();
    }

    public static void setup(Map<String, String> config, boolean setupHttp) {
        configuration.putAll(config);
        if (setupHttp) {
            setupHttp();
        }
    }

    /**
     * Convert --port to `quarkus.http.port`.
     */
    private static void setupPort() {
        if (configuration.containsKey("port")) {
            int port = Integer.parseInt(configuration.get("port"));
            // System property precedes over the custom option if it's set
            configuration.putIfAbsent(QUARKUS_HTTP_PORT, String.valueOf(port));
            // HTTP is always enabled when the option is configured
            configuration.put("http", "true");
        }
    }

    /**
     * Convert --method to `jolokia.mcp.preferred-http-method`.
     */
    private static void setupMethod() {
        if (configuration.containsKey("method")) {
            String method = configuration.get("method");
            // System property precedes over the custom option if it's set
            configuration.putIfAbsent(JOLOKIA_MCP_PREFERRED_HTTP_METHOD, method);
        }
    }

    /**
     * Convert --http to `quarkus.*` properties.
     */
    private static void setupHttp() {
        boolean http = Boolean.parseBoolean(configuration.get("http"));

        // For backward compatibility
        if (!http) {
            http = Boolean.parseBoolean(configuration.get("sse"));
        }

        configuration.put(QUARKUS_HTTP_HOST_ENABLED, http ? "true" : "false");
        configuration.put(QUARKUS_MCP_SERVER_STDIO_ENABLED, http ? "false" : "true");
    }

    @Override
    public int getOrdinal() {
        return 275;
    }

    @Override
    public Set<String> getPropertyNames() {
        return configuration.keySet();
    }

    @Override
    public String getValue(String propertyName) {
        return configuration.get(propertyName);
    }

    @Override
    public String getName() {
        return JolokiaMcpConfigSource.class.getSimpleName();
    }
}
