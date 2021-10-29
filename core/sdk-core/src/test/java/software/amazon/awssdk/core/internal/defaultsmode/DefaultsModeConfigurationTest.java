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

package software.amazon.awssdk.core.internal.defaultsmode;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.Test;
import software.amazon.awssdk.defaultsmode.DefaultsMode;
import software.amazon.awssdk.internal.defaultsmode.DefaultsModeConfiguration;
import software.amazon.awssdk.utils.AttributeMap;

public class DefaultsModeConfigurationTest {

    @Test
    public void defaultConfig_shouldPresentExceptLegacyAndAuto() {
        Arrays.stream(DefaultsMode.values()).forEach(m -> {
            if (m == DefaultsMode.LEGACY || m == DefaultsMode.AUTO) {
                assertThat(DefaultsModeConfiguration.defaultConfig(m)).isEqualTo(AttributeMap.empty());
                assertThat(DefaultsModeConfiguration.defaultHttpConfig(m)).isEqualTo(AttributeMap.empty());
            } else {
                assertThat(DefaultsModeConfiguration.defaultConfig(m)).isNotEqualTo(AttributeMap.empty());
                assertThat(DefaultsModeConfiguration.defaultHttpConfig(m)).isNotEqualTo(AttributeMap.empty());
            }
        });
    }
}
