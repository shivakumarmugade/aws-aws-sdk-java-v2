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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbExtensionContext;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTag;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbUpdateBehavior;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.utils.Validate;


/**
 * This extension facilitates the automatic generation of a unique UUID (Universally Unique Identifier) for a specified attribute
 * every time a new record is written to the database. The generated UUID is obtained using the
 * {@link java.util.UUID#randomUUID()} method.
 * <p>
 * This extension is not loaded by default when you instantiate a
 * {@link software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient}. Therefore, you need to specify it in a custom
 * extension when creating the enhanced client.
 * <p>
 * Example to add AutoGeneratedUuidExtension along with default extensions is
 * {@snippet :
 * DynamoDbEnhancedClient.builder().extensions(Stream.concat(ExtensionResolver.defaultExtensions().stream(),
 * Stream.of(AutoGeneratedUuidExtension.create())).collect(Collectors.toList())).build();
 *}
 * </p>
 * <p>
 * Example to just add AutoGeneratedUuidExtension without default extensions is
 * {@snippet :
 *             DynamoDbEnhancedClient.builder().extensions(AutoGeneratedUuidExtension.create()).build();
 *}
 * </p>
 * <p>
 * To utilize the auto-generated UUID feature, first, create a field in your model that will store the UUID for the attribute.
 * This class field must be of type {@link java.lang.String}, and you need to tag it as the autoGeneratedUuidAttribute. If you are
 * using the {@link software.amazon.awssdk.enhanced.dynamodb.mapper.BeanTableSchema}, then you should use the
 * {@link software.amazon.awssdk.enhanced.dynamodb.extensions.annotations.DynamoDbAutoGeneratedUuid} annotation. If you are using
 * the {@link software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema}, then you should use the
 * {@link
 * software.amazon.awssdk.enhanced.dynamodb.extensions.AutoGeneratedUuidExtension.AttributeTags#autoGeneratedUuidAttribute()}
 * static attribute tag.
 * </p>
 * <p>
 * Every time a new record is successfully put into the database, the specified attribute will be automatically populated with a
 * unique UUID generated using {@link java.util.UUID#randomUUID()}. If the UUID needs to be created only for `putItem` and should
 * not be generated for an `updateItem`, then
 * {@link software.amazon.awssdk.enhanced.dynamodb.mapper.UpdateBehavior#WRITE_IF_NOT_EXISTS} must be along with
 * {@link DynamoDbUpdateBehavior}
 *
 * </p>
 */
@SdkPublicApi
@ThreadSafe
public final class AutoGeneratedUuidExtension implements DynamoDbEnhancedClientExtension {
    private static final String CUSTOM_METADATA_KEY =
        "software.amazon.awssdk.enhanced.dynamodb.extensions.AutoGeneratedUuidExtension:AutoGeneratedUuidAttribute";
    private static final AutoGeneratedUuidAttribute AUTO_GENERATED_UUID_ATTRIBUTE = new AutoGeneratedUuidAttribute();

    private AutoGeneratedUuidExtension() {
    }

    /**
     * @return an Instance of {@link AutoGeneratedUuidExtension}
     */
    public static AutoGeneratedUuidExtension create() {
        return new AutoGeneratedUuidExtension();
    }

    /**
     * Modifies the WriteModification UUID string with the attribute updated with the extension.
     *
     * @param context The {@link DynamoDbExtensionContext.BeforeWrite} context containing the state of the execution.
     * @return WriteModification String updated with attribute updated with Extension.
     */
    @Override
    public WriteModification beforeWrite(DynamoDbExtensionContext.BeforeWrite context) {


        Collection<String> customMetadataObject = context.tableMetadata()
                                                         .customMetadataObject(CUSTOM_METADATA_KEY, Collection.class)
                                                         .orElse(null);

        if (customMetadataObject == null) {
            return WriteModification.builder().build();
        }

        Map<String, AttributeValue> itemToTransform = new HashMap<>(context.items());
        customMetadataObject.forEach(key -> insertUuidInItemToTransform(itemToTransform, key));
        return WriteModification.builder()
                                .transformedItem(Collections.unmodifiableMap(itemToTransform))
                                .build();
    }

    /**
     * Inserts a new UUID into the given map of attributes if the specified attribute key is absent or has an empty value.
     *
     * This method checks whether the attribute with the specified key is already present in the `itemToTransform` map
     * and if its value is a non-empty string. If the attribute exists and has a value, the method does nothing.
     * Otherwise, it generates a new UUID using {@link UUID#randomUUID()} and sets it as the value for the given key.
     *
     * @param itemToTransform The map containing attributes of the item being transformed.
     * @param key The attribute key that should be checked and potentially updated with a new UUID.
     */
    private void insertUuidInItemToTransform(Map<String, AttributeValue> itemToTransform,
                                             String key) {
        if (itemToTransform.containsKey(key) && itemToTransform.get(key).s() != null && !itemToTransform.get(key).s().isEmpty()) {
            return;
        }
        itemToTransform.put(key, AttributeValue.builder().s(UUID.randomUUID().toString()).build());
    }

    public static final class AttributeTags {

        private AttributeTags() {
        }

        /**
         * Tags which indicate that the given attribute is supported wih Auto Generated UUID Record Extension.
         *
         * @return Tag name for AutoGenerated UUID Records
         */
        public static StaticAttributeTag autoGeneratedUuidAttribute() {
            return AUTO_GENERATED_UUID_ATTRIBUTE;
        }
    }

    private static class AutoGeneratedUuidAttribute implements StaticAttributeTag {

        @Override
        public <R> void validateType(String attributeName, EnhancedType<R> type,
                                     AttributeValueType attributeValueType) {

            Validate.notNull(type, "type is null");
            Validate.notNull(type.rawClass(), "rawClass is null");
            Validate.notNull(attributeValueType, "attributeValueType is null");

            if (!type.rawClass().equals(String.class)) {
                throw new IllegalArgumentException(String.format(
                    "Attribute '%s' of Class type %s is not a suitable Java Class type to be used as a Auto Generated "
                    + "Uuid attribute. Only String Class type is supported.", attributeName, type.rawClass()));
            }
        }

        @Override
        public Consumer<StaticTableMetadata.Builder> modifyMetadata(String attributeName,
                                                                    AttributeValueType attributeValueType) {
            return metadata -> metadata.addCustomMetadataObject(CUSTOM_METADATA_KEY, Collections.singleton(attributeName))
                                       .markAttributeAsKey(attributeName, attributeValueType);
        }
    }
}