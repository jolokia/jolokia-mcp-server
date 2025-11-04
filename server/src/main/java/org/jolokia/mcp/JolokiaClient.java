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

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.management.MalformedObjectNameException;
import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jolokia.client.EscapeUtil;
import org.jolokia.client.exception.JolokiaException;
import org.jolokia.client.request.JolokiaExecRequest;
import org.jolokia.client.request.JolokiaListRequest;
import org.jolokia.client.request.JolokiaReadRequest;
import org.jolokia.client.request.JolokiaWriteRequest;
import org.jolokia.client.response.JolokiaExecResponse;
import org.jolokia.client.response.JolokiaListResponse;
import org.jolokia.client.response.JolokiaReadResponse;
import org.jolokia.client.response.JolokiaWriteResponse;
import org.jolokia.json.JSONObject;

@ApplicationScoped
public class JolokiaClient implements JolokiaService {

    org.jolokia.client.JolokiaClient jolokiaClient;
    MBeanListCache mbeanListCache;

    public JolokiaClient(@ConfigProperty(name = "jolokia.mcp.url", defaultValue = "http://localhost:8778/jolokia")
                         String jolokiaUrl) {
        jolokiaClient = new org.jolokia.client.JolokiaClientBuilder().url(jolokiaUrl).build();
    }

    public List<String> listMBeans() throws JolokiaException {
        if (mbeanListCache != null && mbeanListCache.isValid()) {
            return mbeanListCache.getMBeans();
        }

        JSONObject domains = list(null);
        List<String> result = domains.entrySet().stream()
            .flatMap(domain -> ((JSONObject) domain.getValue()).keySet().stream()
                .map(props -> domain.getKey() + ":" + props))
            .collect(Collectors.toList());
        mbeanListCache = new MBeanListCache(result);
        return result;
    }

    public JSONObject listOperations(String mbean) throws JolokiaException {
        return getFromMBean(mbean, "op");
    }

    public JSONObject listAttributes(String mbean) throws JolokiaException {
        return getFromMBean(mbean, "attr");
    }

    JSONObject getFromMBean(String mbean, String key) throws JolokiaException {
        JSONObject mbeanInfo = list(toPath(mbean));
        return (JSONObject) mbeanInfo.getOrDefault(key, new JSONObject());
    }

    static String toPath(String mbean) {
        return EscapeUtil.escape(mbean).replaceFirst(":", "/");
    }

    JSONObject list(String path) throws JolokiaException {
        JolokiaListRequest req = new JolokiaListRequest(path);
        JolokiaListResponse resp = jolokiaClient.execute(req);
        return resp.getValue();
    }

    public Optional<Object> read(String mbean, String attr) throws JolokiaException, MalformedObjectNameException {
        JolokiaReadRequest req = new JolokiaReadRequest(mbean, attr);
        JolokiaReadResponse resp = jolokiaClient.execute(req);
        return Optional.ofNullable(resp.getValue());
    }

    public Optional<Object> write(String mbean, String attr, Object value) throws JolokiaException, MalformedObjectNameException {
        JolokiaWriteRequest req = new JolokiaWriteRequest(mbean, attr, value);
        JolokiaWriteResponse resp = jolokiaClient.execute(req);
        return Optional.ofNullable(resp.getValue());
    }

    public Optional<Object> exec(String mbean, String op, Object... args) throws JolokiaException, MalformedObjectNameException {
        JolokiaExecRequest req = new JolokiaExecRequest(mbean, op, args);
        JolokiaExecResponse resp = jolokiaClient.execute(req);
        return Optional.ofNullable(resp.getValue());
    }
}

