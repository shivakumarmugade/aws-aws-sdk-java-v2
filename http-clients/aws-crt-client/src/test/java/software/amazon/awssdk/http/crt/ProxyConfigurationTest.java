/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.http.crt;

import static org.assertj.core.api.Assertions.assertThat;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;
import java.util.stream.Stream;
import org.junit.Test;

/**
 * Tests for {@link ProxyConfiguration}.
 */
public class ProxyConfigurationTest {
    private static final Random RNG = new Random();
    private static final String TEST_HOST = "foo.com";
    private static final int TEST_PORT = 7777;
    private static final String TEST_USER = "testuser";
    private static final String TEST_PASSWORD = "123";

    @Test
    public void build_setsAllProperties() {
        verifyAllPropertiesSet(allPropertiesSetConfig());
    }

    @Test
    public void build_systemPropertyEnabled() {
        setSystemProperty();
        ProxyConfiguration config = ProxyConfiguration.builder().useSystemPropertyValues(true).build();

        assertThat(config.host()).isEqualTo(TEST_HOST);
        assertThat(config.port()).isEqualTo(TEST_PORT);
        assertThat(config.username()).isEqualTo(TEST_USER);
        assertThat(config.password()).isEqualTo(TEST_PASSWORD);
        assertThat(config.scheme()).isNull();
    }

    @Test
    public void build_systemPropertyDisabled() {
        setSystemProperty();
        ProxyConfiguration config = ProxyConfiguration.builder()
                                                      .host("localhost")
                                                      .port(8888)
                                                      .username("username")
                                                      .password("password")
                                                      .useSystemPropertyValues(false).build();

        assertThat(config.host()).isEqualTo("localhost");
        assertThat(config.port()).isEqualTo(8888);
        assertThat(config.username()).isEqualTo("username");
        assertThat(config.password()).isEqualTo("password");
        assertThat(config.scheme()).isNull();
    }

    @Test
    public void toBuilder_roundTrip_producesExactCopy() {
        ProxyConfiguration original = allPropertiesSetConfig();

        ProxyConfiguration copy = original.toBuilder().build();

        assertThat(copy).isEqualTo(original);
    }

    @Test
    public void toBuilderModified_doesNotModifySource() {
        ProxyConfiguration original = allPropertiesSetConfig();

        ProxyConfiguration modified = setAllPropertiesToRandomValues(original.toBuilder()).build();

        assertThat(original).isNotEqualTo(modified);
    }

    private ProxyConfiguration allPropertiesSetConfig() {
        return setAllPropertiesToRandomValues(ProxyConfiguration.builder()).build();
    }

    private ProxyConfiguration.Builder setAllPropertiesToRandomValues(ProxyConfiguration.Builder builder) {
        Stream.of(builder.getClass().getDeclaredMethods())
                .filter(m -> m.getParameterCount() == 1 && m.getReturnType().equals(ProxyConfiguration.Builder.class))
                .forEach(m -> {
                    try {
                        m.setAccessible(true);
                        setRandomValue(builder, m);
                    } catch (Exception e) {
                        throw new RuntimeException("Could not create random proxy config", e);
                    }
                });
        return builder;
    }

    private void setRandomValue(Object o, Method setter) throws InvocationTargetException, IllegalAccessException {
        Class<?> paramClass = setter.getParameterTypes()[0];

        if (String.class.equals(paramClass)) {
            setter.invoke(o, randomString());
        } else if (int.class.equals(paramClass)) {
            setter.invoke(o, RNG.nextInt());
        } else if (Boolean.class.equals(paramClass)) {
            setter.invoke(o, Boolean.FALSE);
        } else {
            throw new RuntimeException("Don't know how create random value for type " + paramClass);
        }
    }

    private void verifyAllPropertiesSet(ProxyConfiguration cfg) {
        boolean hasNullProperty = Stream.of(cfg.getClass().getDeclaredMethods())
                .filter(m -> !m.getReturnType().equals(Void.class) && m.getParameterCount() == 0)
                .anyMatch(m -> {
                    m.setAccessible(true);
                    try {
                        return m.invoke(cfg) == null;
                    } catch (Exception e) {
                        return true;
                    }
                });

        if (hasNullProperty) {
            throw new RuntimeException("Given configuration has unset property");
        }
    }

    private String randomString() {
        String alpha = "abcdefghijklmnopqrstuwxyz";

        StringBuilder sb = new StringBuilder(16);
        for (int i = 0; i < 16; ++i) {
            sb.append(alpha.charAt(RNG.nextInt(16)));
        }

        return sb.toString();
    }

    private void setSystemProperty() {
        System.setProperty("http.proxyHost", TEST_HOST);
        System.setProperty("http.proxyPort", Integer.toString(TEST_PORT));
        System.setProperty("http.proxyUser", TEST_USER);
        System.setProperty("http.proxyPassword", TEST_PASSWORD);
    }
}
