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

package software.amazon.awssdk.modulepath.tests.mocktests;

import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.http.ExecuteRequest;
import software.amazon.awssdk.http.InvokeableHttpRequest;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;

/**
 * Mock implementation of {@link SdkHttpClient}.
 */
public final class MockHttpClient implements SdkHttpClient {

    private final List<SdkHttpFullRequest> capturedRequests = new ArrayList<>();
    private SdkHttpFullResponse nextResponse;

    @Override
    public InvokeableHttpRequest prepareRequest(ExecuteRequest request) {
        capturedRequests.add(request.httpRequest());
        return new InvokeableHttpRequest() {
            @Override
            public SdkHttpFullResponse call() {
                return nextResponse;
            }

            @Override
            public void abort() {
            }
        };
    }


    @Override
    public void close() {
    }

    /**
     * Resets this mock by clearing any captured requests and wiping any stubbed responses.
     */
    public void reset() {
        this.capturedRequests.clear();
        this.nextResponse = null;
    }

    /**
     * Sets up the next HTTP response that will be returned by the mock.
     *
     * @param nextResponse Next {@link SdkHttpFullResponse} to return from
     *                     {@link #prepareRequest(ExecuteRequest)}
     */
    public void stubNextResponse(SdkHttpFullResponse nextResponse) {
        this.nextResponse = nextResponse;
    }

    /**
     * @return The last executed request that went through this mock client.
     * @throws IllegalStateException If no requests have been captured.
     */
    public SdkHttpFullRequest getLastRequest() {
        if (capturedRequests.isEmpty()) {
            throw new IllegalStateException("No requests were captured by the mock");
        }
        return capturedRequests.get(capturedRequests.size() - 1);
    }

}