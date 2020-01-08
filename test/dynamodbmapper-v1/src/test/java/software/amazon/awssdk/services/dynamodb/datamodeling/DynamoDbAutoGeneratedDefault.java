/*
 * Copyright 2010-2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.dynamodb.datamodeling;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to assign a default value on creation if value is null.
 *
 * <pre class="brush: java">
 * &#064;DynamoDBAutoGeneratedDefault(&quot;OPEN&quot;)
 * public String status()
 * </pre>
 *
 * <p>Only compatible with standard string types.</p>
 *
 */
@DynamoDb
@DynamoDbAutoGenerated(generator = DynamoDbAutoGeneratedDefault.Generator.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface DynamoDbAutoGeneratedDefault {

    /**
     * The default value.
     */
    String value();


    /**
     * Default generator.
     */
    final class Generator<T> extends DynamoDbAutoGenerator.AbstractGenerator<T> {
        private final DynamoDbTypeConverter<T, String> converter;
        private final String defaultValue;

        Generator(Class<T> targetType, DynamoDbAutoGeneratedDefault annotation) {
            super(DynamoDbAutoGenerateStrategy.CREATE);
            this.converter = StandardTypeConverters.factory().getConverter(targetType, String.class);
            this.defaultValue = annotation.value();
        }

        @Override
        public T generate(T currentValue) {
            return converter.convert(defaultValue);
        }
    }


}
