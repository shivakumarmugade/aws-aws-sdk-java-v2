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

package software.amazon.awssdk.core.client.builder;

import java.net.URI;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.utils.builder.SdkBuilder;

/**
 * This includes required and optional override configuration required by every client builder. An instance can be acquired by
 * calling the static "builder" method on the type of client you wish to create.
 *
 * <p>Implementations of this interface are mutable and not thread-safe.</p>
 *
 * @param <B> The type of builder that should be returned by the fluent builder methods in this interface.
 * @param <C> The type of client generated by this builder.
 */
@SdkPublicApi
public interface SdkClientBuilder<B extends SdkClientBuilder<B, C>, C> extends SdkBuilder<B, C> {
    /**
     * Specify overrides to the default SDK configuration that should be used for clients created by this builder.
     */
    B overrideConfiguration(ClientOverrideConfiguration overrideConfiguration);

    /**
     * Similar to {@link #overrideConfiguration(ClientOverrideConfiguration)}, but takes a lambda to configure a new
     * {@link ClientOverrideConfiguration.Builder}. This removes the need to called {@link ClientOverrideConfiguration#builder()}
     * and {@link ClientOverrideConfiguration.Builder#build()}.
     */
    default B overrideConfiguration(Consumer<ClientOverrideConfiguration.Builder> overrideConfiguration) {
        return overrideConfiguration(ClientOverrideConfiguration.builder().applyMutation(overrideConfiguration).build());
    }

    /**
     * Configure the endpoint with which the SDK should communicate.
     */
    B endpointOverride(URI endpointOverride);
}
