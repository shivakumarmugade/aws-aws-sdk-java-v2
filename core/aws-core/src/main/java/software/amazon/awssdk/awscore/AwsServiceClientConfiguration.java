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

package software.amazon.awssdk.awscore;

import java.util.Objects;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.SdkServiceClientConfiguration;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;

/**
 * Class to expose AWS service client settings to the user, e.g., region
 */
@SdkPublicApi
public abstract class AwsServiceClientConfiguration extends SdkServiceClientConfiguration {

    private final Region region;

    protected AwsServiceClientConfiguration(Builder builder) {
        super(builder);
        this.region = builder.region();
    }

    /**
     *
     * @return The configured region of the AwsClient
     */
    public Region region() {
        return this.region;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AwsServiceClientConfiguration serviceClientConfiguration = (AwsServiceClientConfiguration) o;
        return Objects.equals(region, serviceClientConfiguration.region)
               && Objects.equals(overrideConfiguration, serviceClientConfiguration.overrideConfiguration());
    }

    @Override
    public int hashCode() {
        int result = region != null ? region.hashCode() : 0;
        result = 31 * result + (overrideConfiguration != null ? overrideConfiguration.hashCode() : 0);;
        return result;
    }

    /**
     * The base interface for all AWS service client configurations
     */
    public interface Builder extends SdkServiceClientConfiguration.Builder {
        /**
         * Return the region
         */
        Region region();

        /**
         * Configure the region
         */
        Builder region(Region region);

        @Override
        AwsServiceClientConfiguration build();
    }

    private abstract static class BuilderImpl implements Builder {
        protected ClientOverrideConfiguration overrideConfiguration;
        protected Region region;

        protected BuilderImpl() {
        }

        protected BuilderImpl(AwsServiceClientConfiguration awsServiceClientConfiguration) {
            this.overrideConfiguration = awsServiceClientConfiguration.overrideConfiguration();
            this.region = awsServiceClientConfiguration.region();
        }


        @Override
        public Builder overrideConfiguration(ClientOverrideConfiguration clientOverrideConfiguration) {
            this.overrideConfiguration = clientOverrideConfiguration;
            return this;
        }

        @Override
        public Builder region(Region region) {
            this.region = region;
            return this;
        }

        @Override
        public final ClientOverrideConfiguration overrideConfiguration() {
            return overrideConfiguration;
        }

        @Override
        public final Region region() {
            return region;
        }
    }

}
