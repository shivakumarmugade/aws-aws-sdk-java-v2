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

package software.amazon.awssdk.awscore;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.SdkResponse;

/**
 * Base class for all AWS Service responses.
 */
@SdkPublicApi
public abstract class AwsResponse extends SdkResponse {

    private AwsResponseMetadata responseMetadata;

    protected AwsResponse(Builder builder) {
        super(builder);
        this.responseMetadata = builder.responseMetadata();
    }

    public AwsResponseMetadata responseMetadata() {
        return responseMetadata;
    }

    @Override
    public abstract Builder toBuilder();

    public interface Builder extends SdkResponse.Builder {

        AwsResponseMetadata responseMetadata();

        Builder responseMetadata(AwsResponseMetadata metadata);

        @Override
        AwsResponse build();
    }

    protected abstract static class BuilderImpl extends SdkResponse.BuilderImpl implements Builder {

        private AwsResponseMetadata responseMetadata;

        protected BuilderImpl() {
        }

        protected BuilderImpl(AwsResponse response) {
            super(response);
            this.responseMetadata = response.responseMetadata();
        }

        @Override
        public Builder responseMetadata(AwsResponseMetadata responseMetadata) {
            this.responseMetadata = responseMetadata;
            return this;
        }

        @Override
        public AwsResponseMetadata responseMetadata() {
            return responseMetadata;
        }
    }
}
