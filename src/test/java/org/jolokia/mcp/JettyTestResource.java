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

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.junit.QuarkusTestProfile;
import java.io.IOException;
import java.util.Map;
import org.eclipse.jetty.http.UriCompliance;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.jolokia.server.core.http.AgentServlet;

public class JettyTestResource implements QuarkusTestResourceLifecycleManager, QuarkusTestProfile {

    static int port;

    static {
        try {
            port = TestUtil.getFreePort();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    Server jettyServer;

    @Override
    public Map<String, String> start() {
        jettyServer = new Server(port);
        var compliance = UriCompliance.DEFAULT.with("JOLOKIA", UriCompliance.Violation.AMBIGUOUS_EMPTY_SEGMENT);
        var connFactory = (HttpConnectionFactory) jettyServer.getConnectors()[0].getDefaultConnectionFactory();
        connFactory.getHttpConfiguration().setUriCompliance(compliance);
        var context = new ServletContextHandler(jettyServer, "/");
        var servlet = new ServletHolder(new AgentServlet());
        servlet.setInitParameter("includeStackTrace", "true");
        context.addServlet(servlet, "/jolokia/*");
        try {
            jettyServer.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return Map.of();
    }

    @Override
    public void stop() {
        if (jettyServer == null) {
            return;
        }
        try {
            jettyServer.stop();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("jolokia.mcp.url", "http://localhost:%s/jolokia".formatted(port));
    }
}
