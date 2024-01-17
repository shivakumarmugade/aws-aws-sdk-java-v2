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

package software.amazon.awssdk.enhanced.dynamodb.extensions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.OperationContext;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.internal.extensions.DefaultDynamoDbExtensionContext;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.DefaultOperationContext;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.OperationName;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class AutoGeneratedUuidExtensionTest {

    private static final String UUID_REGEX =
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";

    private static final Pattern UUID_PATTERN = Pattern.compile(UUID_REGEX);

    private static final String RECORD_ID = "id123";

    private static final String TABLE_NAME = "table-name";
    private static final OperationContext PRIMARY_CONTEXT =
        DefaultOperationContext.create(TABLE_NAME, TableMetadata.primaryIndexName());

    private final AutoGeneratedUuidExtension atomicCounterExtension = AutoGeneratedUuidExtension.create();


    private static final StaticTableSchema<ItemWithUuid> ITEM_WITH_UUID_MAPPER =
        StaticTableSchema.builder(ItemWithUuid.class)
                         .newItemSupplier(ItemWithUuid::new)
                         .addAttribute(String.class, a -> a.name("id")
                                                           .getter(ItemWithUuid::getId)
                                                           .setter(ItemWithUuid::setId)
                                                           .addTag(primaryPartitionKey()))
                         .addAttribute(String.class, a -> a.name("uuidAttribute")
                                                           .getter(ItemWithUuid::getUuidAttribute)
                                                           .setter(ItemWithUuid::setUuidAttribute)
                                                           .addTag(AutoGeneratedUuidExtension.AttributeTags.autoGeneratedUuidAttribute())
                         )
                         .addAttribute(String.class, a -> a.name("simpleString")
                                                           .getter(ItemWithUuid::getSimpleString)
                                                           .setter(ItemWithUuid::setSimpleString))
                         .build();

    @Test
    public void beforeWrite_updateItemOperation_hasUuidInItem_doesNotCreateUpdateExpressionAndFilters() {
        ItemWithUuid SimpleItem = new ItemWithUuid();
        SimpleItem.setId(RECORD_ID);
        String uuidAttribute = String.valueOf(UUID.randomUUID());
        SimpleItem.setUuidAttribute(uuidAttribute);

        Map<String, AttributeValue> items = ITEM_WITH_UUID_MAPPER.itemToMap(SimpleItem, true);
        assertThat(items).hasSize(2);

        WriteModification result =
            atomicCounterExtension.beforeWrite(DefaultDynamoDbExtensionContext.builder()
                                                                              .items(items)
                                                                              .tableMetadata(ITEM_WITH_UUID_MAPPER.tableMetadata())
                                                                              .operationName(OperationName.UPDATE_ITEM)
                                                                              .operationContext(PRIMARY_CONTEXT).build());

        Map<String, AttributeValue> transformedItem = result.transformedItem();
        assertThat(transformedItem).isNotNull().hasSize(2);
        assertThat(transformedItem).containsEntry("id", AttributeValue.fromS(RECORD_ID));
        isValidUuid(transformedItem.get("uuidAttribute").s());
        assertThat(result.updateExpression()).isNull();

    }

    @Test
    public void beforeWrite_updateItemOperation_hasNoUuidInItem_doesNotCreatesUpdateExpressionAndFilters() {
        ItemWithUuid SimpleItem = new ItemWithUuid();
        SimpleItem.setId(RECORD_ID);

        Map<String, AttributeValue> items = ITEM_WITH_UUID_MAPPER.itemToMap(SimpleItem, true);
        assertThat(items).hasSize(1);

        WriteModification result =
            atomicCounterExtension.beforeWrite(DefaultDynamoDbExtensionContext.builder()
                                                                              .items(items)
                                                                              .tableMetadata(ITEM_WITH_UUID_MAPPER.tableMetadata())
                                                                              .operationName(OperationName.UPDATE_ITEM)
                                                                              .operationContext(PRIMARY_CONTEXT).build());

        Map<String, AttributeValue> transformedItem = result.transformedItem();
        assertThat(transformedItem).isNotNull().hasSize(2);
        assertThat(transformedItem).containsEntry("id", AttributeValue.fromS(RECORD_ID));
        isValidUuid(transformedItem.get("uuidAttribute").s());
        assertThat(result.updateExpression()).isNull();
    }

    @Test
    public void beforeWrite_updateItemOperation_UuidNotPresent_newUuidCreated() {
        ItemWithUuid item = new ItemWithUuid();
        item.setId(RECORD_ID);

        Map<String, AttributeValue> items = ITEM_WITH_UUID_MAPPER.itemToMap(item, true);
        assertThat(items).hasSize(1);

        WriteModification result =
            atomicCounterExtension.beforeWrite(DefaultDynamoDbExtensionContext.builder()
                                                                              .items(items)
                                                                              .tableMetadata(ITEM_WITH_UUID_MAPPER.tableMetadata())
                                                                              .operationName(OperationName.UPDATE_ITEM)
                                                                              .operationContext(PRIMARY_CONTEXT).build());
        assertThat(result.transformedItem()).isNotNull();
        assertThat(result.updateExpression()).isNull();
        assertThat(result.transformedItem()).hasSize(2);
        assertThat(isValidUuid(result.transformedItem().get("uuidAttribute").s())).isTrue();
    }

    @Test
    void IllegalArgumentException_for_AutogeneratedUuid_withNonStringType() {

        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> StaticTableSchema.builder(ItemWithUuid.class)
                                               .newItemSupplier(ItemWithUuid::new)
                                               .addAttribute(String.class, a -> a.name("id")
                                                                                 .getter(ItemWithUuid::getId)
                                                                                 .setter(ItemWithUuid::setId)
                                                                                 .addTag(primaryPartitionKey()))
                                               .addAttribute(Integer.class, a -> a.name("intAttribute")
                                                                                  .getter(ItemWithUuid::getIntAttribute)
                                                                                  .setter(ItemWithUuid::setIntAttribute)
                                                                                  .addTag(AutoGeneratedUuidExtension.AttributeTags.autoGeneratedUuidAttribute())
                                               )
                                               .addAttribute(String.class, a -> a.name("simpleString")
                                                                                 .getter(ItemWithUuid::getSimpleString)
                                                                                 .setter(ItemWithUuid::setSimpleString))
                                               .build())

            .withMessage("Attribute 'intAttribute' of Class type class java.lang.Integer is not a suitable Java Class type"
                         + " to be used as a Auto Generated Uuid attribute. Only String Class type is supported.");
    }

    public static boolean isValidUuid(String uuid) {
        return UUID_PATTERN.matcher(uuid).matches();
    }

    private static class ItemWithUuid {

        private String id;
        private String uuidAttribute;
        private String simpleString;
        private Integer intAttribute;

        public Integer getIntAttribute() {
            return intAttribute;
        }

        public void setIntAttribute(Integer intAttribute) {
            this.intAttribute = intAttribute;
        }

        public ItemWithUuid() {
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getUuidAttribute() {
            return uuidAttribute;
        }

        public void setUuidAttribute(String uuidAttribute) {
            this.uuidAttribute = uuidAttribute;
        }

        public String getSimpleString() {
            return simpleString;
        }

        public void setSimpleString(String simpleString) {
            this.simpleString = simpleString;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ItemWithUuid that = (ItemWithUuid) o;
            return Objects.equals(id, that.id) && Objects.equals(uuidAttribute, that.uuidAttribute) && Objects.equals(simpleString, that.simpleString);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, uuidAttribute, simpleString);
        }
    }
}