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

package software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans;

import java.time.Instant;
import java.util.Objects;
import software.amazon.awssdk.enhanced.dynamodb.extensions.annotations.DynamoDbAtomicCounter;
import software.amazon.awssdk.enhanced.dynamodb.extensions.annotations.DynamoDbAutoGeneratedTimestampAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.UpdateBehavior;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbUpdateBehavior;

@DynamoDbImmutable(builder = CustomMetadataImmutableBean.Builder.class)
public class CustomMetadataImmutableBean {
    private final String id;
    private final String customMetadataObjectAttribute;
    private final Instant customMetadataCollectionAttribute;
    private final Long customMetadataMapAttribute;

    private CustomMetadataImmutableBean(CustomMetadataImmutableBean.Builder b) {
        this.id = b.id;
        this.customMetadataObjectAttribute = b.customMetadataObjectAttribute;
        this.customMetadataCollectionAttribute = b.customMetadataCollectionAttribute;
        this.customMetadataMapAttribute = b.customMetadataMapAttribute;
    }

    @DynamoDbPartitionKey
    public String id() {
        return id;
    }

    @DynamoDbUpdateBehavior(UpdateBehavior.WRITE_IF_NOT_EXISTS)
    public String customMetadataObjectAttribute() {
        return customMetadataObjectAttribute;
    }

    @DynamoDbAutoGeneratedTimestampAttribute
    public Instant customMetadataCollectionAttribute() {
        return customMetadataCollectionAttribute;
    }

    @DynamoDbAtomicCounter
    public Long customMetadataMapAttribute() {
        return customMetadataMapAttribute;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomMetadataImmutableBean that = (CustomMetadataImmutableBean) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(customMetadataObjectAttribute, that.customMetadataObjectAttribute) &&
               Objects.equals(customMetadataCollectionAttribute, that.customMetadataCollectionAttribute) &&
               Objects.equals(customMetadataMapAttribute, that.customMetadataMapAttribute);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, customMetadataObjectAttribute, customMetadataCollectionAttribute, customMetadataMapAttribute);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String id;
        private String customMetadataObjectAttribute;
        private Instant customMetadataCollectionAttribute;
        private Long customMetadataMapAttribute;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder customMetadataObjectAttribute(String customMetadataObjectAttribute) {
            this.customMetadataObjectAttribute = customMetadataObjectAttribute;
            return this;
        }

        public Builder customMetadataCollectionAttribute(Instant customMetadataCollectionAttribute) {
            this.customMetadataCollectionAttribute = customMetadataCollectionAttribute;
            return this;
        }

        public Builder customMetadataMapAttribute(Long customMetadataMapAttribute) {
            this.customMetadataMapAttribute = customMetadataMapAttribute;
            return this;
        }

        public CustomMetadataImmutableBean build() {
            return new CustomMetadataImmutableBean(this);
        }
    }
}
