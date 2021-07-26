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

package software.amazon.awssdk.core.internal.batchutilities;

import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * Takes a request and extracts a batchGroupId as determined by the caller.
 * TODO: For right now, the batchGroupId is a String but this may change as needed in the future.
 * @param <RequestT> the request.
 */
@FunctionalInterface
@SdkProtectedApi
public interface GetBatchGroupIdFunction<RequestT> {
    String getBatchGroupId(RequestT request);
}
