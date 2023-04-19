/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, AutoTimestamp 2.0 (the "License").
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

package software.amazon.awssdk.enhanced.dynamodb.functionaltests;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static software.amazon.awssdk.enhanced.dynamodb.extensions.AutoGeneratedTimestampRecordExtension.AttributeTags.autoGeneratedTimestampAttribute;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.stringValue;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.updateBehavior;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.OperationContext;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.converters.EpochMillisFormatTestConverter;
import software.amazon.awssdk.enhanced.dynamodb.converters.TimeFormatUpdateTestConverter;
import software.amazon.awssdk.enhanced.dynamodb.extensions.AutoGeneratedTimestampRecordExtension;
import software.amazon.awssdk.enhanced.dynamodb.internal.extensions.DefaultDynamoDbExtensionContext;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.DefaultOperationContext;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.UpdateBehavior;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;

public class AutoGeneratedTimestampRecordTest extends LocalDynamoDbSyncTestBase {

    public static final Instant MOCKED_INSTANT_NOW = Instant.now(Clock.fixed(Instant.parse("2019-01-13T14:00:00Z"),
                                                                             ZoneOffset.UTC));

    public static final Instant MOCKED_INSTANT_UPDATE_ONE = Instant.now(Clock.fixed(Instant.parse("2019-01-14T14:00:00Z"),
                                                                                    ZoneOffset.UTC));


    public static final Instant MOCKED_INSTANT_UPDATE_TWO = Instant.now(Clock.fixed(Instant.parse("2019-01-15T14:00:00Z"),
                                                                                    ZoneOffset.UTC));

    private static final String TABLE_NAME = "table-name";
    private static final OperationContext PRIMARY_CONTEXT =
        DefaultOperationContext.create(TABLE_NAME, TableMetadata.primaryIndexName());

    private static final TableSchema<FlattenedRecord> FLATTENED_TABLE_SCHEMA =
        StaticTableSchema.builder(FlattenedRecord.class)
                         .newItemSupplier(FlattenedRecord::new)
                         .addAttribute(Instant.class, a -> a.name("generated")
                                                           .getter(FlattenedRecord::getGenerated)
                                                           .setter(FlattenedRecord::setGenerated)
                                                           .tags(autoGeneratedTimestampAttribute()))
                         .build();

    private static final TableSchema<Record> TABLE_SCHEMA =
        StaticTableSchema.builder(Record.class)
                         .newItemSupplier(Record::new)
                         .addAttribute(String.class, a -> a.name("id")
                                                           .getter(Record::getId)
                                                           .setter(Record::setId)
                                                           .tags(primaryPartitionKey()))
                         .addAttribute(String.class, a -> a.name("attribute")
                                                           .getter(Record::getAttribute)
                                                           .setter(Record::setAttribute))
                         .addAttribute(Instant.class, a -> a.name("lastUpdatedDate")
                                                            .getter(Record::getLastUpdatedDate)
                                                            .setter(Record::setLastUpdatedDate)
                                                            .tags(autoGeneratedTimestampAttribute()))
                         .addAttribute(Instant.class, a -> a.name("createdDate")
                                                            .getter(Record::getCreatedDate)
                                                            .setter(Record::setCreatedDate)
                                                            .tags(autoGeneratedTimestampAttribute(),
                                                                  updateBehavior(UpdateBehavior.WRITE_IF_NOT_EXISTS)))
                         .addAttribute(Instant.class, a -> a.name("lastUpdatedDateInEpochMillis")
                                                         .getter(Record::getLastUpdatedDateInEpochMillis)
                                                         .setter(Record::setLastUpdatedDateInEpochMillis)
                                                         .attributeConverter(EpochMillisFormatTestConverter.create())
                                                         .tags(autoGeneratedTimestampAttribute()))
                         .addAttribute(Instant.class, a -> a.name("convertedLastUpdatedDate")
                                                            .getter(Record::getConvertedLastUpdatedDate)
                                                            .setter(Record::setConvertedLastUpdatedDate)
                                                            .attributeConverter(TimeFormatUpdateTestConverter.create())
                                                            .tags(autoGeneratedTimestampAttribute()))
                         .flatten(FLATTENED_TABLE_SCHEMA, Record::getFlattenedRecord, Record::setFlattenedRecord)
                         .build();

    private final List<Map<String, AttributeValue>> fakeItems =
        IntStream.range(0, 4)
                 .mapToObj($ -> createUniqueFakeItem())
                 .map(fakeItem -> TABLE_SCHEMA.itemToMap(fakeItem, true))
                 .collect(toList());
    private final DynamoDbTable<Record> mappedTable;

    private final Clock mockCLock = Mockito.mock(Clock.class);


    private final DynamoDbEnhancedClient enhancedClient =
        DynamoDbEnhancedClient.builder()
                              .dynamoDbClient(getDynamoDbClient())
                              .extensions(AutoGeneratedTimestampRecordExtension.builder().baseClock(mockCLock).build())
                              .build();
    private final String concreteTableName;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    {
        concreteTableName = getConcreteTableName("table-name");
        mappedTable = enhancedClient.table(concreteTableName, TABLE_SCHEMA);
    }

    public static Record createUniqueFakeItem() {
        Record record = new Record();
        record.setId(UUID.randomUUID().toString());
        return record;
    }

    @Before
    public void createTable() {
        Mockito.when(mockCLock.instant()).thenReturn(MOCKED_INSTANT_NOW);
        mappedTable.createTable(r -> r.provisionedThroughput(getDefaultProvisionedThroughput()));
    }

    @After
    public void deleteTable() {
        getDynamoDbClient().deleteTable(DeleteTableRequest.builder()
                                                          .tableName(getConcreteTableName("table-name"))
                                                          .build());
    }

    @Test
    public void putNewRecordSetsInitialAutoGeneratedTimestamp() {
        Record item = new Record().setId("id").setAttribute("one");
        mappedTable.putItem(r -> r.item(item));
        Record result = mappedTable.getItem(r -> r.key(k -> k.partitionValue("id")));
        GetItemResponse itemAsStoredInDDB = getItemAsStoredFromDDB();
        FlattenedRecord flattenedRecord = new FlattenedRecord().setGenerated(MOCKED_INSTANT_NOW);
        Record expectedRecord = new Record().setId("id")
                                            .setAttribute("one")
                                            .setLastUpdatedDate(MOCKED_INSTANT_NOW)
                                            .setConvertedLastUpdatedDate(MOCKED_INSTANT_NOW)
                                            .setCreatedDate(MOCKED_INSTANT_NOW)
                                            .setLastUpdatedDateInEpochMillis(MOCKED_INSTANT_NOW)
                                            .setFlattenedRecord(flattenedRecord);
        assertThat(result, is(expectedRecord));
        // The data in DDB is stored in converted time format
        assertThat(itemAsStoredInDDB.item().get("convertedLastUpdatedDate").s(), is("13 01 2019 14:00:00"));
    }

    @Test
    public void updateNewRecordSetsAutoFormattedDate() {
        Record result = mappedTable.updateItem(r -> r.item(new Record().setId("id").setAttribute("one")));
        GetItemResponse itemAsStoredInDDB = getItemAsStoredFromDDB();
        FlattenedRecord flattenedRecord = new FlattenedRecord().setGenerated(MOCKED_INSTANT_NOW);
        Record expectedRecord = new Record().setId("id")
                                            .setAttribute("one")
                                            .setLastUpdatedDate(MOCKED_INSTANT_NOW)
                                            .setConvertedLastUpdatedDate(MOCKED_INSTANT_NOW)
                                            .setCreatedDate(MOCKED_INSTANT_NOW)
                                            .setLastUpdatedDateInEpochMillis(MOCKED_INSTANT_NOW)
                                            .setFlattenedRecord(flattenedRecord);
        assertThat(result, is(expectedRecord));
        // The data in DDB is stored in converted time format
        assertThat(itemAsStoredInDDB.item().get("convertedLastUpdatedDate").s(), is("13 01 2019 14:00:00"));
    }

    @Test
    public void putExistingRecordUpdatedWithAutoFormattedTimestamps() {
        mappedTable.putItem(r -> r.item(new Record().setId("id").setAttribute("one")));
        Record result = mappedTable.getItem(r -> r.key(k -> k.partitionValue("id")));
        GetItemResponse itemAsStoredInDDB = getItemAsStoredFromDDB();
        FlattenedRecord flattenedRecord = new FlattenedRecord().setGenerated(MOCKED_INSTANT_NOW);
        Record expectedRecord = new Record().setId("id")
                                            .setAttribute("one")
                                            .setLastUpdatedDate(MOCKED_INSTANT_NOW)
                                            .setConvertedLastUpdatedDate(MOCKED_INSTANT_NOW)
                                            .setCreatedDate(MOCKED_INSTANT_NOW)
                                            .setLastUpdatedDateInEpochMillis(MOCKED_INSTANT_NOW)
                                            .setFlattenedRecord(flattenedRecord);
        assertThat(result, is(expectedRecord));
        // The data in DDB is stored in converted time format
        assertThat(itemAsStoredInDDB.item().get("convertedLastUpdatedDate").s(), is("13 01 2019 14:00:00"));

        Mockito.when(mockCLock.instant()).thenReturn(MOCKED_INSTANT_UPDATE_ONE);
        mappedTable.putItem(r -> r.item(new Record().setId("id").setAttribute("one")));
        result = mappedTable.getItem(r -> r.key(k -> k.partitionValue("id")));
        itemAsStoredInDDB = getItemAsStoredFromDDB();
        flattenedRecord = new FlattenedRecord().setGenerated(MOCKED_INSTANT_UPDATE_ONE);
        expectedRecord = new Record().setId("id")
                                     .setAttribute("one")
                                     .setLastUpdatedDate(MOCKED_INSTANT_UPDATE_ONE)
                                     .setConvertedLastUpdatedDate(MOCKED_INSTANT_UPDATE_ONE)
                                     // Note : Since we are doing PutItem second time, the createDate gets updated,
                                     .setCreatedDate(MOCKED_INSTANT_UPDATE_ONE)
                                     .setLastUpdatedDateInEpochMillis(MOCKED_INSTANT_UPDATE_ONE)
                                     .setFlattenedRecord(flattenedRecord);

        System.out.println("result "+result);
        assertThat(result, is(expectedRecord));
        // The data in DDB is stored in converted time format
        assertThat(itemAsStoredInDDB.item().get("convertedLastUpdatedDate").s(), is("14 01 2019 14:00:00"));
    }

    @Test
    public void putItemFollowedByUpdates() {
        mappedTable.putItem(r -> r.item(new Record().setId("id").setAttribute("one")));
        Record result = mappedTable.getItem(r -> r.key(k -> k.partitionValue("id")));
        GetItemResponse itemAsStoredInDDB = getItemAsStoredFromDDB();
        FlattenedRecord flattenedRecord = new FlattenedRecord().setGenerated(MOCKED_INSTANT_NOW);
        Record expectedRecord = new Record().setId("id")
                                            .setAttribute("one")
                                            .setLastUpdatedDate(MOCKED_INSTANT_NOW)
                                            .setConvertedLastUpdatedDate(MOCKED_INSTANT_NOW)
                                            .setCreatedDate(MOCKED_INSTANT_NOW)
                                            .setLastUpdatedDateInEpochMillis(MOCKED_INSTANT_NOW)
                                            .setFlattenedRecord(flattenedRecord);
        assertThat(result, is(expectedRecord));
        // The data in DDB is stored in converted time format
        assertThat(itemAsStoredInDDB.item().get("convertedLastUpdatedDate").s(), is("13 01 2019 14:00:00"));

        //First Update
        Mockito.when(mockCLock.instant()).thenReturn(MOCKED_INSTANT_UPDATE_ONE);

        result = mappedTable.updateItem(r -> r.item(new Record().setId("id").setAttribute("one")));
        itemAsStoredInDDB = getItemAsStoredFromDDB();
        flattenedRecord = new FlattenedRecord().setGenerated(MOCKED_INSTANT_UPDATE_ONE);
        expectedRecord = new Record().setId("id")
                                            .setAttribute("one")
                                            .setLastUpdatedDate(MOCKED_INSTANT_UPDATE_ONE)
                                            .setConvertedLastUpdatedDate(MOCKED_INSTANT_UPDATE_ONE)
                                            .setCreatedDate(MOCKED_INSTANT_NOW)
                                            .setLastUpdatedDateInEpochMillis(MOCKED_INSTANT_UPDATE_ONE)
                                            .setFlattenedRecord(flattenedRecord);
        assertThat(result, is(expectedRecord));
        // The data in DDB is stored in converted time format
        assertThat(itemAsStoredInDDB.item().get("convertedLastUpdatedDate").s(), is("14 01 2019 14:00:00"));

        //Second Update
        Mockito.when(mockCLock.instant()).thenReturn(MOCKED_INSTANT_UPDATE_TWO);
        result = mappedTable.updateItem(r -> r.item(new Record().setId("id").setAttribute("one")));
        itemAsStoredInDDB = getItemAsStoredFromDDB();
        flattenedRecord = new FlattenedRecord().setGenerated(MOCKED_INSTANT_UPDATE_TWO);
        expectedRecord = new Record().setId("id")
                                     .setAttribute("one")
                                     .setLastUpdatedDate(MOCKED_INSTANT_UPDATE_TWO)
                                     .setConvertedLastUpdatedDate(MOCKED_INSTANT_UPDATE_TWO)
                                     .setCreatedDate(MOCKED_INSTANT_NOW)
                                     .setLastUpdatedDateInEpochMillis(MOCKED_INSTANT_UPDATE_TWO)
                                     .setFlattenedRecord(flattenedRecord);
        assertThat(result, is(expectedRecord));
        // The data in DDB is stored in converted time format
        assertThat(itemAsStoredInDDB.item().get("convertedLastUpdatedDate").s(), is("15 01 2019 14:00:00"));

        System.out.println(Instant.ofEpochMilli(Long.parseLong(itemAsStoredInDDB.item().get("lastUpdatedDateInEpochMillis").n())));
        assertThat(Long.parseLong(itemAsStoredInDDB.item().get("lastUpdatedDateInEpochMillis").n()),
                   is(MOCKED_INSTANT_UPDATE_TWO.toEpochMilli()));
    }

    @Test
    public void putExistingRecordWithConditionExpressions() {
        mappedTable.putItem(r -> r.item(new Record().setId("id").setAttribute("one")));
        Record result = mappedTable.getItem(r -> r.key(k -> k.partitionValue("id")));
        GetItemResponse itemAsStoredInDDB = getItemAsStoredFromDDB();
        FlattenedRecord flattenedRecord = new FlattenedRecord().setGenerated(MOCKED_INSTANT_NOW);
        Record expectedRecord = new Record().setId("id")
                                            .setAttribute("one")
                                            .setLastUpdatedDate(MOCKED_INSTANT_NOW)
                                            .setConvertedLastUpdatedDate(MOCKED_INSTANT_NOW)
                                            .setCreatedDate(MOCKED_INSTANT_NOW)
                                            .setLastUpdatedDateInEpochMillis(MOCKED_INSTANT_NOW)
                                            .setFlattenedRecord(flattenedRecord);
        assertThat(result, is(expectedRecord));
        // The data in DDB is stored in converted time format
        assertThat(itemAsStoredInDDB.item().get("convertedLastUpdatedDate").s(), is("13 01 2019 14:00:00"));

        Expression conditionExpression = Expression.builder()
                                                   .expression("#k = :v OR #k = :v1")
                                                   .putExpressionName("#k", "attribute")
                                                   .putExpressionValue(":v", stringValue("one"))
                                                   .putExpressionValue(":v1", stringValue("wrong2"))
                                                   .build();

        Mockito.when(mockCLock.instant()).thenReturn(MOCKED_INSTANT_UPDATE_ONE);
        mappedTable.putItem(PutItemEnhancedRequest.builder(Record.class)
                                                  .item(new Record().setId("id").setAttribute("one"))
                                                  .conditionExpression(conditionExpression)
                                                  .build());

        result = mappedTable.getItem(r -> r.key(k -> k.partitionValue("id")));
        flattenedRecord = new FlattenedRecord().setGenerated(MOCKED_INSTANT_UPDATE_ONE);
        expectedRecord = new Record().setId("id")
                                     .setAttribute("one")
                                     .setLastUpdatedDate(MOCKED_INSTANT_UPDATE_ONE)
                                     .setConvertedLastUpdatedDate(MOCKED_INSTANT_UPDATE_ONE)
                                     //  Note that this is a second putItem call so create date is updated.
                                     .setCreatedDate(MOCKED_INSTANT_UPDATE_ONE)
                                     .setLastUpdatedDateInEpochMillis(MOCKED_INSTANT_UPDATE_ONE)
                                     .setFlattenedRecord(flattenedRecord);
        assertThat(result, is(expectedRecord));
    }

    @Test
    public void updateExistingRecordWithConditionExpressions() {
        mappedTable.updateItem(r -> r.item(new Record().setId("id").setAttribute("one")));
        GetItemResponse itemAsStoredInDDB = getItemAsStoredFromDDB();
        // The data in DDB is stored in converted time format
        assertThat(itemAsStoredInDDB.item().get("convertedLastUpdatedDate").s(), is("13 01 2019 14:00:00"));
        Expression conditionExpression = Expression.builder()
                                                   .expression("#k = :v OR #k = :v1")
                                                   .putExpressionName("#k", "attribute")
                                                   .putExpressionValue(":v", stringValue("one"))
                                                   .putExpressionValue(":v1", stringValue("wrong2"))
                                                   .build();

        Mockito.when(mockCLock.instant()).thenReturn(MOCKED_INSTANT_UPDATE_ONE);
        mappedTable.updateItem(r -> r.item(new Record().setId("id").setAttribute("one"))
                                     .conditionExpression(conditionExpression));

        Record result = mappedTable.getItem(r -> r.key(k -> k.partitionValue("id")));
        FlattenedRecord flattenedRecord = new FlattenedRecord().setGenerated(MOCKED_INSTANT_UPDATE_ONE);
        Record expectedRecord = new Record().setId("id")
                                     .setAttribute("one")
                                     .setLastUpdatedDate(MOCKED_INSTANT_UPDATE_ONE)
                                     .setConvertedLastUpdatedDate(MOCKED_INSTANT_UPDATE_ONE)
                                     .setCreatedDate(MOCKED_INSTANT_NOW)
                                     .setLastUpdatedDateInEpochMillis(MOCKED_INSTANT_UPDATE_ONE)
                                     .setFlattenedRecord(flattenedRecord);
        assertThat(result, is(expectedRecord));
    }

    @Test
    public void putItemConditionTestFailure() {

        mappedTable.putItem(r -> r.item(new Record().setId("id").setAttribute("one")));

        Expression conditionExpression = Expression.builder()
                                                   .expression("#k = :v OR #k = :v1")
                                                   .putExpressionName("#k", "attribute")
                                                   .putExpressionValue(":v", stringValue("wrong1"))
                                                   .putExpressionValue(":v1", stringValue("wrong2"))
                                                   .build();

        thrown.expect(ConditionalCheckFailedException.class);
        mappedTable.putItem(PutItemEnhancedRequest.builder(Record.class)
                                                                      .item(new Record().setId("id").setAttribute("one"))
                                                                      .conditionExpression(conditionExpression)
                                                                      .build());

    }

    @Test
    public void updateItemConditionTestFailure() {
        mappedTable.updateItem(r -> r.item(new Record().setId("id").setAttribute("one")));
        Expression conditionExpression = Expression.builder()
                                                   .expression("#k = :v OR #k = :v1")
                                                   .putExpressionName("#k", "attribute")
                                                   .putExpressionValue(":v", stringValue("wrong1"))
                                                   .putExpressionValue(":v1", stringValue("wrong2"))
                                                   .build();
        thrown.expect(ConditionalCheckFailedException.class);
        mappedTable.putItem(PutItemEnhancedRequest.builder(Record.class)
                                                  .item(new Record().setId("id").setAttribute("one"))
                                                  .conditionExpression(conditionExpression)
                                                  .build());
    }

    @Test
    public void incorrectTypeForAutoUpdateTimestampThrowsException(){

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Attribute 'lastUpdatedDate' of Class type class java.lang.String is not a suitable "
                             + "Java Class type to be used as a Auto Generated Timestamp attribute. Only java.time."
                             + "Instant Class type is supported.");
        StaticTableSchema.builder(RecordWithStringUpdateDate.class)
                         .newItemSupplier(RecordWithStringUpdateDate::new)
                         .addAttribute(String.class, a -> a.name("id")
                                                           .getter(RecordWithStringUpdateDate::getId)
                                                           .setter(RecordWithStringUpdateDate::setId)
                                                           .tags(primaryPartitionKey()))
                         .addAttribute(String.class, a -> a.name("lastUpdatedDate")
                                                           .getter(RecordWithStringUpdateDate::getLastUpdatedDate)
                                                           .setter(RecordWithStringUpdateDate::setLastUpdatedDate)
                                                           .tags(autoGeneratedTimestampAttribute()))
                         .build();
    }

    private DefaultDynamoDbExtensionContext getExtensionContext() {
        return DefaultDynamoDbExtensionContext.builder()
                                              .tableMetadata(TABLE_SCHEMA.tableMetadata())
                                              .operationContext(PRIMARY_CONTEXT)
                                              .tableSchema(TABLE_SCHEMA)
                                              .items(fakeItems.get(0)).build();
    }

    private GetItemResponse getItemAsStoredFromDDB() {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("id", AttributeValue.builder().s("id").build());
        return getDynamoDbClient().getItem(GetItemRequest
                                               .builder().tableName(concreteTableName)
                                               .key(key)
                                               .consistentRead(true).build());
    }

    private static class Record {
        private String id;
        private String attribute;
        private Instant createdDate;
        private Instant lastUpdatedDate;
        private Instant convertedLastUpdatedDate;
        private Instant lastUpdatedDateInEpochMillis;
        private FlattenedRecord flattenedRecord;

        private String getId() {
            return id;
        }

        private Record setId(String id) {
            this.id = id;
            return this;
        }

        private String getAttribute() {
            return attribute;
        }

        private Record setAttribute(String attribute) {
            this.attribute = attribute;
            return this;
        }

        private Instant getLastUpdatedDate() {
            return lastUpdatedDate;
        }

        private Record setLastUpdatedDate(Instant lastUpdatedDate) {
            this.lastUpdatedDate = lastUpdatedDate;
            return this;
        }

        private Instant getCreatedDate() {
            return createdDate;
        }

        private Record setCreatedDate(Instant createdDate) {
            this.createdDate = createdDate;
            return this;
        }

        private Instant getConvertedLastUpdatedDate() {
            return convertedLastUpdatedDate;
        }

        private Record setConvertedLastUpdatedDate(Instant convertedLastUpdatedDate) {
            this.convertedLastUpdatedDate = convertedLastUpdatedDate;
            return this;
        }

        private Instant getLastUpdatedDateInEpochMillis() {
            return lastUpdatedDateInEpochMillis;
        }

        private Record setLastUpdatedDateInEpochMillis(Instant lastUpdatedDateInEpochMillis) {
            this.lastUpdatedDateInEpochMillis = lastUpdatedDateInEpochMillis;
            return this;
        }

        public FlattenedRecord getFlattenedRecord() {
            return flattenedRecord;
        }

        public Record setFlattenedRecord(FlattenedRecord flattenedRecord) {
            this.flattenedRecord = flattenedRecord;
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Record record = (Record) o;
            return Objects.equals(id, record.id) &&
                   Objects.equals(attribute, record.attribute) &&
                   Objects.equals(lastUpdatedDate, record.lastUpdatedDate) &&
                   Objects.equals(createdDate, record.createdDate) &&
                   Objects.equals(lastUpdatedDateInEpochMillis, record.lastUpdatedDateInEpochMillis) &&
                   Objects.equals(convertedLastUpdatedDate, record.convertedLastUpdatedDate) &&
                   Objects.equals(flattenedRecord, record.flattenedRecord);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, attribute, lastUpdatedDate, createdDate, lastUpdatedDateInEpochMillis,
                                convertedLastUpdatedDate, flattenedRecord);
        }

        @Override
        public String toString() {
            return "Record{" +
                   "id='" + id + '\'' +
                   ", attribute='" + attribute + '\'' +
                   ", createdDate=" + createdDate +
                   ", lastUpdatedDate=" + lastUpdatedDate +
                   ", convertedLastUpdatedDate=" + convertedLastUpdatedDate +
                   ", lastUpdatedDateInEpochMillis=" + lastUpdatedDateInEpochMillis +
                   ", flattenedRecord=" + flattenedRecord +
                   '}';
        }
    }

    private static class FlattenedRecord {
        private Instant generated;

        public Instant getGenerated() {
            return generated;
        }

        public FlattenedRecord setGenerated(Instant generated) {
            this.generated = generated;
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            FlattenedRecord that = (FlattenedRecord) o;
            return Objects.equals(generated, that.generated);
        }

        @Override
        public int hashCode() {
            return Objects.hash(generated);
        }

        @Override
        public String toString() {
            return "FlattenedRecord{" +
                   "generated=" + generated +
                   '}';
        }
    }

    private static class RecordWithStringUpdateDate {
        private String id;
        private String lastUpdatedDate;


        private String getId() {
            return id;
        }

        private RecordWithStringUpdateDate setId(String id) {
            this.id = id;
            return this;
        }


        private String getLastUpdatedDate() {
            return lastUpdatedDate;
        }

        private RecordWithStringUpdateDate setLastUpdatedDate(String lastUpdatedDate) {
            this.lastUpdatedDate = lastUpdatedDate;
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            RecordWithStringUpdateDate record = (RecordWithStringUpdateDate) o;
            return Objects.equals(id, record.id) &&
                   Objects.equals(lastUpdatedDate, record.lastUpdatedDate);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, lastUpdatedDate);
        }

        @Override
        public String toString() {
            return "RecordWithStringUpdateDate{" +
                   "id='" + id + '\'' +
                   ", lastUpdatedDate=" + lastUpdatedDate +
                   '}';
        }
    }


}
