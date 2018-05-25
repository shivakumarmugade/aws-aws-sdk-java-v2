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

package software.amazon.awssdk.core.client;

import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.ServiceConfiguration;
import software.amazon.awssdk.core.config.SdkAsyncClientConfiguration;
import software.amazon.awssdk.core.http.AmazonAsyncHttpClient;

/**
 * Default implementation of {@link AsyncClientHandler}.
 */
@Immutable
@ThreadSafe
@SdkProtectedApi
final class SdkAsyncClientHandlerImpl extends BaseAsyncClientHandler implements AsyncClientHandler {

    SdkAsyncClientHandlerImpl(SdkAsyncClientConfiguration asyncClientConfiguration,
                              ServiceConfiguration serviceConfiguration) {
        super(asyncClientConfiguration, serviceConfiguration, new AmazonAsyncHttpClient(asyncClientConfiguration));
    }
}
