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
package org.jolokia.mcp.jvmagent;

import java.lang.instrument.Instrumentation;
import java.util.HashMap;
import java.util.Map;

import io.quarkus.runtime.Quarkus;
import org.jolokia.core.util.EscapeUtil;
import org.jolokia.mcp.JolokiaMcpConfigSource;

public class JvmAgent {

    public static final String QUARKUS_HTTP_PORT = "quarkus.http.port";
    public static final int DEFAULT_PORT = 8779;

    private static Instrumentation instrumentation;

    /**
     * Entry point for the agent, using command line attach
     * (that is via -javaagent command line argument)
     *
     * @param agentArgs arguments as given on the command line
     */
    public static void premain(String agentArgs, Instrumentation inst) {
        startAgent(inst);
    }

    /**
     * Entry point for the agent, using dynamic attach.
     * (this is a post VM initialisation attachment, via com.sun.attach)
     *
     * @param agentArgs arguments as given on the command line
     */
    public static void agentmain(String agentArgs, Instrumentation inst) {
        // TODO: impl stop mode
        startAgent(inst);
    }

    private static void startAgent(Instrumentation inst) {
        instrumentation = inst;

        Thread jolokiaStartThread = new Thread("JolokiaStart") {
            @Override
            public void run() {
                Map<String, String> mcpConfig = new HashMap<>();
                mcpConfig.putIfAbsent(QUARKUS_HTTP_PORT, Integer.toString(DEFAULT_PORT));
                JolokiaMcpConfigSource.setup(mcpConfig, false);
                Quarkus.run();
            }
        };
        jolokiaStartThread.setDaemon(true);

        // Ensure LogManager is initialized before starting the JolokiaStart thread.
        // https://github.com/jolokia/jolokia/issues/535 - sun.net.httpserver.ServerImpl constructor may also
        // concurrently lead to LogManager initialization
        System.getLogger("org.jolokia.mcp");

        jolokiaStartThread.start();
    }

    private static Map<String, String> split(String agentArgs) {
        Map<String, String> ret = new HashMap<>();
        if (agentArgs == null || agentArgs.isEmpty()) {
            return ret;
        }
        for (String arg : EscapeUtil.splitAsArray(agentArgs, EscapeUtil.CSV_ESCAPE, ",")) {
            String[] prop = arg.split("=", 2);
            if (prop.length != 2) {
                throw new IllegalArgumentException("jolokia mcp: Invalid option '" + arg + "'");
            } else {
                ret.put(prop[0], prop[1]);
            }
        }
        return ret;
    }
}
