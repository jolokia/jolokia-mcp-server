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

import java.util.Optional;
import java.util.stream.Stream;
import jakarta.inject.Inject;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.jolokia.client.request.HttpMethod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.jolokia.mcp.JolokiaClient.toPath;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

@QuarkusTest
@TestProfile(JettyTestResource.class)
@QuarkusTestResource(JettyTestResource.class)
class JolokiaClientTest {

    @Inject
    JolokiaClient jolokiaClient;

    @Test
    void testList() throws Exception {
        var result = jolokiaClient.listMBeans();
        assertNotNull(result);
        assertFalse(result.isEmpty());
        // Test cache
        var result2 = jolokiaClient.listMBeans();
        assertSame(result, result2);
    }

    @Test
    void testToPath() {
        assertEquals("java.lang/type=Memory", toPath("java.lang:type=Memory"));
        assertEquals("test/name=\"a!/b!/c\",type=Memory", toPath("test:name=\"a/b/c\",type=Memory"));
    }

    @Test
    void testRead() throws Exception {
        assertNotNull(jolokiaClient.read("java.lang:type=OperatingSystem", "Name").orElse(null));
    }

    @Test
    void testWrite() throws Exception {
        assertNotNull(jolokiaClient.write("java.lang:type=Memory", "Verbose", true).orElse(null));
    }

    @Test
    void testExec() {
        assertDoesNotThrow(() -> jolokiaClient.exec("java.lang:type=Memory", "gc"));
    }

    @ParameterizedTest
    @MethodSource("providePreferredHttpMethodTestCases")
    void testPreferredHttpMethod(String config, Optional<HttpMethod> expected) throws Exception {
        if (!"DEFAULT".equalsIgnoreCase(config)) {
            jolokiaClient.setPreferredHttpMethod(config);
        }
        assertEquals(expected, jolokiaClient.getPreferredHttpMethod());
    }

    static Stream<Arguments> providePreferredHttpMethodTestCases() {
        return Stream.of(
            Arguments.of("DEFAULT", Optional.empty()),
            Arguments.of("GET", Optional.of(HttpMethod.GET)),
            Arguments.of("get", Optional.of(HttpMethod.GET)),
            Arguments.of("POST", Optional.of(HttpMethod.POST)),
            Arguments.of("post", Optional.of(HttpMethod.POST)),
            Arguments.of("INVALID", Optional.empty())
        );
    }
}
