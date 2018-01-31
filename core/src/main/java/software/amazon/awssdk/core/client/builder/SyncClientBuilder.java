/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.util.function.Consumer;

/**
 * This includes required and optional override configuration required by every sync client builder. An instance can be acquired
 * by calling the static "builder" method on the type of sync client you wish to create.
 *
 * <p>Implementations of this interface are mutable and not thread-safe.</p>
 *
 * @param <B> The type of builder that should be returned by the fluent builder methods in this interface.
 * @param <C> The type of client generated by this builder.
 */
public interface SyncClientBuilder<B extends SyncClientBuilder<B, C>, C>
        extends ClientBuilder<B, C> {

    /**
     * Configures the HTTP client used by the service client. Either a client factory may be provided (in which case
     * the SDK will merge any service specific configuration on top of customer supplied configuration) or provide an already
     * constructed instance of {@link software.amazon.awssdk.http.SdkHttpClient}. Note that if an {@link
     * software.amazon.awssdk.http.SdkHttpClient} is provided then it is up to the caller to close it when they are finished with
     * it, the SDK will only close HTTP clients that it creates.
     */
    B httpConfiguration(ClientHttpConfiguration httpConfiguration);

    /**
     * Similar to {@link #httpConfiguration(ClientHttpConfiguration)}, but takes a lambda to configure a new
     * {@link ClientHttpConfiguration.Builder}. This removes the need to called {@link ClientHttpConfiguration#builder()} and
     * {@link ClientHttpConfiguration.Builder#build()}.
     */
    default B httpConfiguration(Consumer<ClientHttpConfiguration.Builder> httpConfiguration) {
        return httpConfiguration(ClientHttpConfiguration.builder().apply(httpConfiguration).build());
    }
}
