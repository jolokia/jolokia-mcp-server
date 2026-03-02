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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;
import jakarta.enterprise.context.ApplicationScoped;

import org.jolokia.core.api.LogHandler;
import org.jolokia.core.util.ClassUtil;
import org.jolokia.core.util.LocalServiceFactory;
import org.jolokia.json.JSONObject;
import org.jolokia.mcp.JolokiaService;
import org.jolokia.server.core.config.ConfigKey;
import org.jolokia.server.core.config.Configuration;
import org.jolokia.server.core.config.StaticConfiguration;
import org.jolokia.server.core.detector.ServerDetectorLookup;
import org.jolokia.server.core.request.BadRequestException;
import org.jolokia.server.core.request.EmptyResponseException;
import org.jolokia.server.core.restrictor.RestrictorFactory;
import org.jolokia.server.core.service.JolokiaServiceManagerFactory;
import org.jolokia.server.core.service.api.JolokiaContext;
import org.jolokia.server.core.service.api.JolokiaServiceManager;
import org.jolokia.server.core.service.api.Restrictor;
import org.jolokia.server.core.service.impl.CachingServerDetectorLookup;
import org.jolokia.server.core.service.impl.ClasspathServerDetectorLookup;
import org.jolokia.server.core.service.impl.ClasspathServiceCreator;
import org.jolokia.server.core.service.impl.StdoutLogHandler;
import org.jolokia.server.core.vm.InVmRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class InVmJolokiaService implements JolokiaService {

    private static final Logger LOG = LoggerFactory.getLogger(InVmJolokiaService.class);

    private static final String DEFAULT_JOLOKIA_AGENT_PROPERTIES = "/default-jolokia-agent.properties";

    final JolokiaServiceManager serviceManager;
    final InVmRequestHandler requestHandler;

    public InVmJolokiaService() {
        serviceManager = initServiceManager();
        JolokiaContext context = serviceManager.start();
        requestHandler = new InVmRequestHandler(context);
    }

    private JolokiaServiceManager initServiceManager() {
        Map<String, String> defaultConfig = getDefaultConfig();
        addJolokiaId(defaultConfig);
        Configuration config = new StaticConfiguration(defaultConfig);
        LogHandler log = createLogHandler(
            config.getConfig(ConfigKey.LOGHANDLER_CLASS),
            config.getConfig(ConfigKey.LOGHANDLER_NAME),
            Boolean.parseBoolean(config.getConfig(ConfigKey.DEBUG)));
        Restrictor restrictor = RestrictorFactory.createRestrictor(config, log);
        ServerDetectorLookup lookup = new CachingServerDetectorLookup(new ClasspathServerDetectorLookup());
        JolokiaServiceManager serviceManager = JolokiaServiceManagerFactory.createJolokiaServiceManager(config, log, restrictor, lookup);

        // loader used to load services - may be configured by one of the detectors
        ClassLoader loader = LocalServiceFactory.class.getClassLoader();
        serviceManager.addServices(new ClasspathServiceCreator(loader, "services"));

        return serviceManager;
    }

    /**
     * Add a unique jolokia id for this agent
     */
    private void addJolokiaId(Map<String, String> config) {
        if (config.containsKey(ConfigKey.AGENT_ID.getKeyValue())) {
            return;
        }
        String id = Integer.toHexString(hashCode()) + "-jvm";
        config.put(ConfigKey.AGENT_ID.getKeyValue(), id);
    }

    private static Map<String, String> getDefaultConfig() {
        InputStream is = InVmJolokiaService.class.getResourceAsStream(DEFAULT_JOLOKIA_AGENT_PROPERTIES);
        Map<String, String> ret = new HashMap<>();
        if (is == null) {
            return ret;
        }
        Properties props = new Properties();
        try {
            props.load(is);
            props.forEach((key, value) -> ret.put((String) key, (String) value));
        } catch (IOException e) {
            throw new IllegalArgumentException(
                "jolokia: Cannot load properties %s : %s".formatted(DEFAULT_JOLOKIA_AGENT_PROPERTIES, e),
                e);
        }
        return ret;
    }

    /**
     * Create a log handler from either the given class or by creating a default log handler printing
     * out to stderr
     */
    private static LogHandler createLogHandler(String logHandlerClass, String logHandlerName, boolean isDebug) {
        if (logHandlerClass != null) {
            return ClassUtil.newLogHandlerInstance(logHandlerClass, logHandlerName, isDebug);
        }
        return new StdoutLogHandler(isDebug);
    }

    @Override
    public List<String> listMBeans() throws EmptyResponseException, BadRequestException {
        JSONObject response = requestHandler.handleList(null);
        JSONObject domains = (JSONObject) response.get("value");
        LOG.debug("listMBeans: {}", domains);
        return domains.entrySet().stream()
            .flatMap(domain -> ((JSONObject) domain.getValue()).keySet().stream()
                .map(props -> domain.getKey() + ":" + props))
            .collect(Collectors.toList());
    }

    @Override
    public JSONObject listOperations(String mbean) throws EmptyResponseException, BadRequestException {
        return getFromMBean(mbean, "op");
    }

    @Override
    public JSONObject listAttributes(String mbean) throws EmptyResponseException, BadRequestException {
        return getFromMBean(mbean, "attr");
    }

    private JSONObject getFromMBean(String mbean, String key) throws EmptyResponseException, BadRequestException {
        JSONObject response = requestHandler.handleList(mbean);
        JSONObject mbeanInfo = (JSONObject) response.get("value");
        LOG.debug("getFromMBean( {}, {} ): {}", mbean, key, mbeanInfo);
        return (JSONObject) mbeanInfo.getOrDefault(key, new JSONObject());
    }

    @Override
    public Optional<Object> read(String mbean, String attr) throws EmptyResponseException, BadRequestException {
        JSONObject response = requestHandler.handleRead(mbean, attr);
        LOG.debug("read( {}, {} ): {}", mbean, attr, response);
        return Optional.ofNullable(response.get("value"));
    }

    @Override
    public Optional<Object> write(String mbean, String attr, Object value) throws EmptyResponseException, BadRequestException {
        JSONObject response = requestHandler.handleWrite(mbean, attr, value);
        LOG.debug("write( {}, {}, {} ): {}", mbean, attr, value, response);
        return Optional.ofNullable(response.get("value"));
    }

    @Override
    public Optional<Object> exec(String mbean, String op, Object... args) throws EmptyResponseException, BadRequestException {
        JSONObject response = requestHandler.handleExec(mbean, op, args);
        LOG.debug("exec( {}, {}, {} ): {}", mbean, op, args, response);
        return Optional.ofNullable(response.get("value"));
    }
}
