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

package software.amazon.awssdk.services.dynamodb.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.UUID;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.utils.ImmutableMap;
import software.amazon.awssdk.services.dynamodb.DynamoDBMapperIntegrationTestBase;
import software.amazon.awssdk.services.dynamodb.TableUtils;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbAutoGeneratedKey;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbHashKey;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbIndexHashKey;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbIndexRangeKey;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbMapper;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbMappingException;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbRangeKey;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbSaveExpression;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbTable;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.ConditionalOperator;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.ExpectedAttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

/**
 * Tests using auto-generated keys for range keys, hash keys, or both.
 */
public class AutoGeneratedKeysIntegrationTest extends DynamoDBMapperIntegrationTestBase {

    private static final String TABLE_NAME = "aws-java-sdk-string-range";

    private static final String GSI_NAME = "gsi-with-autogenerated-keys";
    private static final String GSI_HASH_KEY = "gis-hash-key";
    private static final String GSI_RANGE_KEY = "gis-range-key";

    @BeforeClass
    public static void setUp() throws Exception {
        DynamoDBMapperIntegrationTestBase.setUp();

        String keyName = DynamoDBMapperIntegrationTestBase.KEY_NAME;
        String rangeKeyAttributeName = "rangeKey";

        CreateTableRequest createTableRequest = CreateTableRequest.builder()
                .tableName(TABLE_NAME)
                .keySchema(
                        KeySchemaElement.builder().attributeName(keyName).keyType(KeyType.HASH).build(),
                        KeySchemaElement.builder().attributeName(rangeKeyAttributeName).keyType(KeyType.RANGE).build())
                .globalSecondaryIndexes(GlobalSecondaryIndex.builder()
                                                    .indexName(GSI_NAME)
                                                    .keySchema(
                                                            KeySchemaElement.builder().attributeName(GSI_HASH_KEY).keyType(KeyType.HASH).build(),
                                                            KeySchemaElement.builder().attributeName(GSI_RANGE_KEY).keyType(KeyType.RANGE).build())
                                                    .projection(Projection.builder().projectionType(ProjectionType.ALL).build())
                                                    .provisionedThroughput(ProvisionedThroughput.builder().readCapacityUnits(3L).writeCapacityUnits(3L).build()).build())
                .attributeDefinitions(
                        AttributeDefinition.builder().attributeName(keyName).attributeType(ScalarAttributeType.S).build(),
                        AttributeDefinition.builder().attributeName(rangeKeyAttributeName).attributeType(ScalarAttributeType.S).build(),
                        AttributeDefinition.builder().attributeName(GSI_HASH_KEY).attributeType(ScalarAttributeType.S).build(),
                        AttributeDefinition.builder().attributeName(GSI_RANGE_KEY).attributeType(ScalarAttributeType.S).build())
                .provisionedThroughput(ProvisionedThroughput.builder().readCapacityUnits(10L).writeCapacityUnits(5L).build()).build();

        if (TableUtils.createTableIfNotExists(dynamo, createTableRequest)) {
            TableUtils.waitUntilActive(dynamo, TABLE_NAME);
        }
    }

    @Test
    public void testHashKeyRangeKeyBothAutogenerated() {
        DynamoDbMapper mapper = new DynamoDbMapper(dynamo);
        HashKeyRangeKeyBothAutoGenerated obj = new HashKeyRangeKeyBothAutoGenerated();
        obj.setOtherAttribute("blah");

        assertNull(obj.getKey());
        assertNull(obj.getRangeKey());
        mapper.save(obj);
        assertNotNull(obj.getKey());
        assertNotNull(obj.getRangeKey());

        HashKeyRangeKeyBothAutoGenerated other = mapper.load(HashKeyRangeKeyBothAutoGenerated.class, obj.getKey(),
                                                             obj.getRangeKey());
        assertEquals(other, obj);
    }

    @Test
    public void testHashKeyRangeKeyBothAutogeneratedBatchWrite() {
        DynamoDbMapper mapper = new DynamoDbMapper(dynamo);
        HashKeyRangeKeyBothAutoGenerated obj = new HashKeyRangeKeyBothAutoGenerated();
        obj.setOtherAttribute("blah");
        HashKeyRangeKeyBothAutoGenerated obj2 = new HashKeyRangeKeyBothAutoGenerated();
        obj2.setOtherAttribute("blah");

        assertNull(obj.getKey());
        assertNull(obj.getRangeKey());
        assertNull(obj2.getKey());
        assertNull(obj2.getRangeKey());
        mapper.batchSave(obj, obj2);
        assertNotNull(obj.getKey());
        assertNotNull(obj.getRangeKey());
        assertNotNull(obj2.getKey());
        assertNotNull(obj2.getRangeKey());

        assertEquals(mapper.load(HashKeyRangeKeyBothAutoGenerated.class, obj.getKey(),
                                 obj.getRangeKey()), obj);
        assertEquals(mapper.load(HashKeyRangeKeyBothAutoGenerated.class, obj2.getKey(),
                                 obj2.getRangeKey()), obj2);
    }

    /**
     * Tests providing additional expected conditions when saving item with
     * auto-generated keys.
     */
    @Test
    public void testAutogeneratedKeyWithUserProvidedExpectedConditions() {
        DynamoDbMapper mapper = new DynamoDbMapper(dynamo);
        HashKeyRangeKeyBothAutoGenerated obj = new HashKeyRangeKeyBothAutoGenerated();
        obj.setOtherAttribute("blah");

        assertNull(obj.getKey());
        assertNull(obj.getRangeKey());

        // Add additional expected conditions via DynamoDBSaveExpression.
        // Expected conditions joined by AND are compatible with the conditions
        // for auto-generated keys.
        DynamoDbSaveExpression saveExpression = new DynamoDbSaveExpression();
        saveExpression
                .withExpected(Collections.singletonMap(
                        "otherAttribute", ExpectedAttributeValue.builder().exists(false).build()))
                .withConditionalOperator(ConditionalOperator.AND);
        // The save should succeed since the user provided conditions are joined by AND.
        mapper.save(obj, saveExpression);
        assertNotNull(obj.getKey());
        assertNotNull(obj.getRangeKey());

        HashKeyRangeKeyBothAutoGenerated other = mapper.load(HashKeyRangeKeyBothAutoGenerated.class, obj.getKey(),
                                                             obj.getRangeKey());
        assertEquals(other, obj);

        // Change the conditional operator to OR.
        // IllegalArgumentException is expected since the additional expected
        // conditions cannot be joined with the conditions for auto-generated
        // keys.
        saveExpression.setConditionalOperator(ConditionalOperator.OR);
        try {
            mapper.save(new HashKeyRangeKeyBothAutoGenerated(), saveExpression);
        } catch (IllegalArgumentException expected) {
            // Expected.
        }

        // User-provided OR conditions should work if they completely override the generated conditions.
        saveExpression
                .withExpected(ImmutableMap.of(
                        "otherAttribute", ExpectedAttributeValue.builder().exists(false).build(),
                        "key", ExpectedAttributeValue.builder().exists(false).build(),
                        "rangeKey", ExpectedAttributeValue.builder().exists(false).build()))
                .withConditionalOperator(ConditionalOperator.OR);
        mapper.save(new HashKeyRangeKeyBothAutoGenerated(), saveExpression);

        saveExpression
                .withExpected(ImmutableMap.of(
                        "otherAttribute", ExpectedAttributeValue.builder().value(AttributeValue.builder().s("non-existent-value").build()).build(),
                        "key", ExpectedAttributeValue.builder().value(AttributeValue.builder().s("non-existent-value").build()).build(),
                        "rangeKey", ExpectedAttributeValue.builder().value(AttributeValue.builder().s("non-existent-value").build()).build()))
                .withConditionalOperator(ConditionalOperator.OR);
        try {
            mapper.save(new HashKeyRangeKeyBothAutoGenerated(), saveExpression);
        } catch (ConditionalCheckFailedException expected) {
            // Expected.
        }
    }

    @Test
    public void testHashKeyAutogenerated() {
        DynamoDbMapper mapper = new DynamoDbMapper(dynamo);
        HashKeyAutoGenerated obj = new HashKeyAutoGenerated();
        obj.setOtherAttribute("blah");
        obj.setRangeKey("" + System.currentTimeMillis());

        assertNull(obj.getKey());
        assertNotNull(obj.getRangeKey());
        mapper.save(obj);
        assertNotNull(obj.getKey());
        assertNotNull(obj.getRangeKey());

        HashKeyAutoGenerated other = mapper.load(HashKeyAutoGenerated.class, obj.getKey(), obj.getRangeKey());
        assertEquals(other, obj);
    }

    @Test
    public void testRangeKeyAutogenerated() {
        DynamoDbMapper mapper = new DynamoDbMapper(dynamo);
        RangeKeyAutoGenerated obj = new RangeKeyAutoGenerated();
        obj.setOtherAttribute("blah");
        obj.setKey("" + System.currentTimeMillis());

        assertNotNull(obj.getKey());
        assertNull(obj.getRangeKey());
        mapper.save(obj);
        assertNotNull(obj.getKey());
        assertNotNull(obj.getRangeKey());

        RangeKeyAutoGenerated other = mapper.load(RangeKeyAutoGenerated.class, obj.getKey(), obj.getRangeKey());
        assertEquals(other, obj);
    }

    @Test
    public void testNothingAutogenerated() {
        DynamoDbMapper mapper = new DynamoDbMapper(dynamo);
        NothingAutoGenerated obj = new NothingAutoGenerated();
        obj.setOtherAttribute("blah");
        obj.setKey("" + System.currentTimeMillis());
        obj.setRangeKey("" + System.currentTimeMillis());

        assertNotNull(obj.getKey());
        assertNotNull(obj.getRangeKey());
        mapper.save(obj);
        assertNotNull(obj.getKey());
        assertNotNull(obj.getRangeKey());

        NothingAutoGenerated other = mapper.load(NothingAutoGenerated.class, obj.getKey(), obj.getRangeKey());
        assertEquals(other, obj);
    }

    @Test
    public void testNothingAutogeneratedErrors() {
        DynamoDbMapper mapper = new DynamoDbMapper(dynamo);
        NothingAutoGenerated obj = new NothingAutoGenerated();

        try {
            mapper.save(obj);
            fail("Expected a mapping exception");
        } catch (DynamoDbMappingException expected) {
            // Expected.
        }

        obj.setKey("" + System.currentTimeMillis());
        try {
            mapper.save(obj);
            fail("Expected a mapping exception");
        } catch (DynamoDbMappingException expected) {
            // Expected.
        }

        obj.setRangeKey("" + System.currentTimeMillis());
        obj.setKey(null);
        try {
            mapper.save(obj);
            fail("Expected a mapping exception");
        } catch (DynamoDbMappingException expected) {
            // Expected.
        }

        obj.setRangeKey("");
        obj.setKey("" + System.currentTimeMillis());
        try {
            mapper.save(obj);
            fail("Expected a mapping exception");
        } catch (DynamoDbMappingException expected) {
            // Expected.
        }

        obj.setRangeKey("" + System.currentTimeMillis());
        mapper.save(obj);
    }

    @Test
    public void testHashKeyRangeKeyBothAutogeneratedKeyOnly() {
        DynamoDbMapper mapper = new DynamoDbMapper(dynamo);
        HashKeyRangeKeyBothAutoGeneratedKeyOnly obj = new HashKeyRangeKeyBothAutoGeneratedKeyOnly();

        assertNull(obj.getKey());
        assertNull(obj.getRangeKey());
        mapper.save(obj);
        assertNotNull(obj.getKey());
        assertNotNull(obj.getRangeKey());

        HashKeyRangeKeyBothAutoGeneratedKeyOnly other = mapper.load(HashKeyRangeKeyBothAutoGeneratedKeyOnly.class, obj.getKey(),
                                                                    obj.getRangeKey());
        assertEquals(other, obj);
    }

    @Test
    public void testHashKeyAutogeneratedKeyOnly() {
        DynamoDbMapper mapper = new DynamoDbMapper(dynamo);
        HashKeyAutoGeneratedKeyOnly obj = new HashKeyAutoGeneratedKeyOnly();
        obj.setRangeKey("" + System.currentTimeMillis());

        assertNull(obj.getKey());
        assertNotNull(obj.getRangeKey());
        mapper.save(obj);
        assertNotNull(obj.getKey());
        assertNotNull(obj.getRangeKey());

        HashKeyAutoGeneratedKeyOnly other = mapper.load(HashKeyAutoGeneratedKeyOnly.class, obj.getKey(), obj.getRangeKey());
        assertEquals(other, obj);
    }

    @Test
    public void testRangeKeyAutogeneratedKeyOnly() {
        DynamoDbMapper mapper = new DynamoDbMapper(dynamo);
        RangeKeyAutoGeneratedKeyOnly obj = new RangeKeyAutoGeneratedKeyOnly();
        obj.setKey("" + System.currentTimeMillis());

        assertNotNull(obj.getKey());
        assertNull(obj.getRangeKey());
        mapper.save(obj);
        assertNotNull(obj.getKey());
        assertNotNull(obj.getRangeKey());

        RangeKeyAutoGeneratedKeyOnly other = mapper.load(RangeKeyAutoGeneratedKeyOnly.class, obj.getKey(), obj.getRangeKey());
        assertEquals(other, obj);
    }

    @Test
    public void testNothingAutogeneratedKeyOnly() {
        DynamoDbMapper mapper = new DynamoDbMapper(dynamo);
        NothingAutoGeneratedKeyOnly obj = new NothingAutoGeneratedKeyOnly();
        obj.setKey("" + System.currentTimeMillis());
        obj.setRangeKey("" + System.currentTimeMillis());

        assertNotNull(obj.getKey());
        assertNotNull(obj.getRangeKey());
        mapper.save(obj);
        assertNotNull(obj.getKey());
        assertNotNull(obj.getRangeKey());

        NothingAutoGeneratedKeyOnly other = mapper.load(NothingAutoGeneratedKeyOnly.class, obj.getKey(), obj.getRangeKey());
        assertEquals(other, obj);
    }

    @Test
    public void testNothingAutogeneratedKeyOnlyErrors() {
        DynamoDbMapper mapper = new DynamoDbMapper(dynamo);
        NothingAutoGeneratedKeyOnly obj = new NothingAutoGeneratedKeyOnly();

        try {
            mapper.save(obj);
            fail("Expected a mapping exception");
        } catch (DynamoDbMappingException expected) {
            // Expected.
        }

        obj.setKey("" + System.currentTimeMillis());
        try {
            mapper.save(obj);
            fail("Expected a mapping exception");
        } catch (DynamoDbMappingException expected) {
            // Expected.
        }

        obj.setRangeKey("" + System.currentTimeMillis());
        obj.setKey(null);
        try {
            mapper.save(obj);
            fail("Expected a mapping exception");
        } catch (DynamoDbMappingException expected) {
            // Expected.
        }

        obj.setRangeKey("");
        obj.setKey("" + System.currentTimeMillis());
        try {
            mapper.save(obj);
            fail("Expected a mapping exception");
        } catch (DynamoDbMappingException expected) {
            // Expected.
        }

        obj.setRangeKey("" + System.currentTimeMillis());
        mapper.save(obj);
    }

    @Test
    public void testIndexKeyWithAutogeneratedAnnotation_StillRequirePrimaryKeyValue() {
        DynamoDbMapper mapper = new DynamoDbMapper(dynamo);
        PrimaryKeysNotAutogeneratedIndexKeysAutogenerated obj = new PrimaryKeysNotAutogeneratedIndexKeysAutogenerated();

        try {
            mapper.save(obj);
            fail("DynamoDBMappingException is expected.");
        } catch (DynamoDbMappingException expected) {
            // Expected.
        }

        obj.setGsiHashKey("foo");
        obj.setGsiRangeKey("foo");
        try {
            mapper.save(obj);
            fail("DynamoDBMappingException is expected.");
        } catch (DynamoDbMappingException expected) {
            // Expected.
        }
    }

    @Test
    public void testIndexKeyWithAutogeneratedAnnotation_AutogenerateIndexKeyValueIfNull() {
        DynamoDbMapper mapper = new DynamoDbMapper(dynamo);
        PrimaryKeysNotAutogeneratedIndexKeysAutogenerated obj = new PrimaryKeysNotAutogeneratedIndexKeysAutogenerated();

        String randomPrimaryKeyValue = UUID.randomUUID().toString();
        obj.setKey(randomPrimaryKeyValue);
        obj.setRangeKey(randomPrimaryKeyValue);

        assertNull(obj.getGsiHashKey());
        assertNull(obj.getGsiRangeKey());
        mapper.save(obj);

        // check in-memory value
        assertNotNull(obj.getGsiHashKey());
        assertNotNull(obj.getGsiRangeKey());

        PrimaryKeysNotAutogeneratedIndexKeysAutogenerated retrieved = mapper.load(obj);
        assertEquals(obj, retrieved);
    }

    @Test
    public void testIndexKeyWithAutogeneratedAnnotation_DoNotAutogenerateIndexKeyValueIfAlreadySpecified() {
        DynamoDbMapper mapper = new DynamoDbMapper(dynamo);
        PrimaryKeysNotAutogeneratedIndexKeysAutogenerated obj = new PrimaryKeysNotAutogeneratedIndexKeysAutogenerated();

        String randomValue = UUID.randomUUID().toString();
        obj.setKey(randomValue);
        obj.setRangeKey(randomValue);
        obj.setGsiHashKey(randomValue);
        obj.setGsiRangeKey(randomValue);
        mapper.save(obj);

        // check in-memory value
        assertEquals(randomValue, obj.getGsiHashKey());
        assertEquals(randomValue, obj.getGsiRangeKey());

        PrimaryKeysNotAutogeneratedIndexKeysAutogenerated retrieved = mapper.load(obj);
        assertEquals(obj, retrieved);
    }

    @DynamoDbTable(tableName = TABLE_NAME)
    public static class HashKeyRangeKeyBothAutoGenerated {

        private String key;
        private String rangeKey;
        private String otherAttribute;

        @DynamoDbAutoGeneratedKey
        @DynamoDbHashKey
        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        @DynamoDbAutoGeneratedKey
        @DynamoDbRangeKey
        public String getRangeKey() {
            return rangeKey;
        }

        public void setRangeKey(String rangeKey) {
            this.rangeKey = rangeKey;
        }

        public String getOtherAttribute() {
            return otherAttribute;
        }

        public void setOtherAttribute(String otherAttribute) {
            this.otherAttribute = otherAttribute;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((key == null) ? 0 : key.hashCode());
            result = prime * result + ((otherAttribute == null) ? 0 : otherAttribute.hashCode());
            result = prime * result + ((rangeKey == null) ? 0 : rangeKey.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            HashKeyRangeKeyBothAutoGenerated other = (HashKeyRangeKeyBothAutoGenerated) obj;
            if (key == null) {
                if (other.key != null) {
                    return false;
                }
            } else if (!key.equals(other.key)) {
                return false;
            }
            if (otherAttribute == null) {
                if (other.otherAttribute != null) {
                    return false;
                }
            } else if (!otherAttribute.equals(other.otherAttribute)) {
                return false;
            }
            if (rangeKey == null) {
                if (other.rangeKey != null) {
                    return false;
                }
            } else if (!rangeKey.equals(other.rangeKey)) {
                return false;
            }
            return true;
        }
    }

    @DynamoDbTable(tableName = TABLE_NAME)
    public static class HashKeyAutoGenerated {

        private String key;
        private String rangeKey;
        private String otherAttribute;

        @DynamoDbAutoGeneratedKey
        @DynamoDbHashKey
        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        @DynamoDbRangeKey
        public String getRangeKey() {
            return rangeKey;
        }

        public void setRangeKey(String rangeKey) {
            this.rangeKey = rangeKey;
        }

        public String getOtherAttribute() {
            return otherAttribute;
        }

        public void setOtherAttribute(String otherAttribute) {
            this.otherAttribute = otherAttribute;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((key == null) ? 0 : key.hashCode());
            result = prime * result + ((otherAttribute == null) ? 0 : otherAttribute.hashCode());
            result = prime * result + ((rangeKey == null) ? 0 : rangeKey.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            HashKeyAutoGenerated other = (HashKeyAutoGenerated) obj;
            if (key == null) {
                if (other.key != null) {
                    return false;
                }
            } else if (!key.equals(other.key)) {
                return false;
            }
            if (otherAttribute == null) {
                if (other.otherAttribute != null) {
                    return false;
                }
            } else if (!otherAttribute.equals(other.otherAttribute)) {
                return false;
            }
            if (rangeKey == null) {
                if (other.rangeKey != null) {
                    return false;
                }
            } else if (!rangeKey.equals(other.rangeKey)) {
                return false;
            }
            return true;
        }
    }

    @DynamoDbTable(tableName = "aws-java-sdk-string-range")
    public static class RangeKeyAutoGenerated {

        private String key;
        private String rangeKey;
        private String otherAttribute;

        @DynamoDbHashKey
        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        @DynamoDbAutoGeneratedKey
        @DynamoDbRangeKey
        public String getRangeKey() {
            return rangeKey;
        }

        public void setRangeKey(String rangeKey) {
            this.rangeKey = rangeKey;
        }

        public String getOtherAttribute() {
            return otherAttribute;
        }

        public void setOtherAttribute(String otherAttribute) {
            this.otherAttribute = otherAttribute;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((key == null) ? 0 : key.hashCode());
            result = prime * result + ((otherAttribute == null) ? 0 : otherAttribute.hashCode());
            result = prime * result + ((rangeKey == null) ? 0 : rangeKey.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            RangeKeyAutoGenerated other = (RangeKeyAutoGenerated) obj;
            if (key == null) {
                if (other.key != null) {
                    return false;
                }
            } else if (!key.equals(other.key)) {
                return false;
            }
            if (otherAttribute == null) {
                if (other.otherAttribute != null) {
                    return false;
                }
            } else if (!otherAttribute.equals(other.otherAttribute)) {
                return false;
            }
            if (rangeKey == null) {
                if (other.rangeKey != null) {
                    return false;
                }
            } else if (!rangeKey.equals(other.rangeKey)) {
                return false;
            }
            return true;
        }
    }

    @DynamoDbTable(tableName = TABLE_NAME)
    public static class NothingAutoGenerated {

        private String key;
        private String rangeKey;
        private String otherAttribute;

        @DynamoDbHashKey
        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        @DynamoDbRangeKey
        public String getRangeKey() {
            return rangeKey;
        }

        public void setRangeKey(String rangeKey) {
            this.rangeKey = rangeKey;
        }

        public String getOtherAttribute() {
            return otherAttribute;
        }

        public void setOtherAttribute(String otherAttribute) {
            this.otherAttribute = otherAttribute;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((key == null) ? 0 : key.hashCode());
            result = prime * result + ((otherAttribute == null) ? 0 : otherAttribute.hashCode());
            result = prime * result + ((rangeKey == null) ? 0 : rangeKey.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            NothingAutoGenerated other = (NothingAutoGenerated) obj;
            if (key == null) {
                if (other.key != null) {
                    return false;
                }
            } else if (!key.equals(other.key)) {
                return false;
            }
            if (otherAttribute == null) {
                if (other.otherAttribute != null) {
                    return false;
                }
            } else if (!otherAttribute.equals(other.otherAttribute)) {
                return false;
            }
            if (rangeKey == null) {
                if (other.rangeKey != null) {
                    return false;
                }
            } else if (!rangeKey.equals(other.rangeKey)) {
                return false;
            }
            return true;
        }
    }

    @DynamoDbTable(tableName = TABLE_NAME)
    public static class HashKeyRangeKeyBothAutoGeneratedKeyOnly {

        private String key;
        private String rangeKey;

        @DynamoDbAutoGeneratedKey
        @DynamoDbHashKey
        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        @DynamoDbAutoGeneratedKey
        @DynamoDbRangeKey
        public String getRangeKey() {
            return rangeKey;
        }

        public void setRangeKey(String rangeKey) {
            this.rangeKey = rangeKey;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((key == null) ? 0 : key.hashCode());
            result = prime * result + ((rangeKey == null) ? 0 : rangeKey.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            HashKeyRangeKeyBothAutoGeneratedKeyOnly other = (HashKeyRangeKeyBothAutoGeneratedKeyOnly) obj;
            if (key == null) {
                if (other.key != null) {
                    return false;
                }
            } else if (!key.equals(other.key)) {
                return false;
            }
            if (rangeKey == null) {
                if (other.rangeKey != null) {
                    return false;
                }
            } else if (!rangeKey.equals(other.rangeKey)) {
                return false;
            }
            return true;
        }
    }

    @DynamoDbTable(tableName = TABLE_NAME)
    public static class HashKeyAutoGeneratedKeyOnly {

        private String key;
        private String rangeKey;

        @DynamoDbAutoGeneratedKey
        @DynamoDbHashKey
        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        @DynamoDbRangeKey
        public String getRangeKey() {
            return rangeKey;
        }

        public void setRangeKey(String rangeKey) {
            this.rangeKey = rangeKey;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((key == null) ? 0 : key.hashCode());
            result = prime * result + ((rangeKey == null) ? 0 : rangeKey.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            HashKeyAutoGeneratedKeyOnly other = (HashKeyAutoGeneratedKeyOnly) obj;
            if (key == null) {
                if (other.key != null) {
                    return false;
                }
            } else if (!key.equals(other.key)) {
                return false;
            }
            if (rangeKey == null) {
                if (other.rangeKey != null) {
                    return false;
                }
            } else if (!rangeKey.equals(other.rangeKey)) {
                return false;
            }
            return true;
        }

    }

    @DynamoDbTable(tableName = TABLE_NAME)
    public static class RangeKeyAutoGeneratedKeyOnly {

        private String key;
        private String rangeKey;

        @DynamoDbHashKey
        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        @DynamoDbAutoGeneratedKey
        @DynamoDbRangeKey
        public String getRangeKey() {
            return rangeKey;
        }

        public void setRangeKey(String rangeKey) {
            this.rangeKey = rangeKey;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((key == null) ? 0 : key.hashCode());
            result = prime * result + ((rangeKey == null) ? 0 : rangeKey.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            RangeKeyAutoGeneratedKeyOnly other = (RangeKeyAutoGeneratedKeyOnly) obj;
            if (key == null) {
                if (other.key != null) {
                    return false;
                }
            } else if (!key.equals(other.key)) {
                return false;
            }
            if (rangeKey == null) {
                if (other.rangeKey != null) {
                    return false;
                }
            } else if (!rangeKey.equals(other.rangeKey)) {
                return false;
            }
            return true;
        }

    }

    @DynamoDbTable(tableName = TABLE_NAME)
    public static class NothingAutoGeneratedKeyOnly {

        private String key;
        private String rangeKey;

        @DynamoDbHashKey
        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        @DynamoDbRangeKey
        public String getRangeKey() {
            return rangeKey;
        }

        public void setRangeKey(String rangeKey) {
            this.rangeKey = rangeKey;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((key == null) ? 0 : key.hashCode());
            result = prime * result + ((rangeKey == null) ? 0 : rangeKey.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            NothingAutoGeneratedKeyOnly other = (NothingAutoGeneratedKeyOnly) obj;
            if (key == null) {
                if (other.key != null) {
                    return false;
                }
            } else if (!key.equals(other.key)) {
                return false;
            }
            if (rangeKey == null) {
                if (other.rangeKey != null) {
                    return false;
                }
            } else if (!rangeKey.equals(other.rangeKey)) {
                return false;
            }
            return true;
        }
    }

    @DynamoDbTable(tableName = TABLE_NAME)
    public static class PrimaryKeysNotAutogeneratedIndexKeysAutogenerated {

        private String key;
        private String rangeKey;
        private String gsiHashKey;
        private String gsiRangeKey;

        private static boolean isEqual(Object a, Object b) {
            if (a == null || b == null) {
                return a == null && b == null;
            }
            return a.equals(b);
        }

        @DynamoDbHashKey
        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        @DynamoDbRangeKey
        public String getRangeKey() {
            return rangeKey;
        }

        public void setRangeKey(String rangeKey) {
            this.rangeKey = rangeKey;
        }

        @DynamoDbIndexHashKey(globalSecondaryIndexName = GSI_NAME, attributeName = GSI_HASH_KEY)
        @DynamoDbAutoGeneratedKey
        public String getGsiHashKey() {
            return gsiHashKey;
        }

        public void setGsiHashKey(String gsiHashKey) {
            this.gsiHashKey = gsiHashKey;
        }

        @DynamoDbIndexRangeKey(globalSecondaryIndexName = GSI_NAME, attributeName = GSI_RANGE_KEY)
        @DynamoDbAutoGeneratedKey
        public String getGsiRangeKey() {
            return gsiRangeKey;
        }

        public void setGsiRangeKey(String gsiRangeKey) {
            this.gsiRangeKey = gsiRangeKey;
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof PrimaryKeysNotAutogeneratedIndexKeysAutogenerated)) {
                return false;
            }
            PrimaryKeysNotAutogeneratedIndexKeysAutogenerated other = (PrimaryKeysNotAutogeneratedIndexKeysAutogenerated) object;

            return isEqual(this.getKey(), other.getKey())
                   && isEqual(this.getRangeKey(), other.getRangeKey())
                   && isEqual(this.getGsiHashKey(), other.getGsiHashKey())
                   && isEqual(this.getGsiRangeKey(), other.getGsiRangeKey());
        }

    }
}
