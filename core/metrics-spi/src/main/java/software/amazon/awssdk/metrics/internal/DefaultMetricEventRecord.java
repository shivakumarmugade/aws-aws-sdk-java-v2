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

package software.amazon.awssdk.metrics.internal;

import software.amazon.awssdk.metrics.MetricEvent;
import software.amazon.awssdk.metrics.MetricEventRecord;

public class DefaultMetricEventRecord<T> implements MetricEventRecord<T> {
    private final MetricEvent<T> event;
    private final T data;

    public DefaultMetricEventRecord(MetricEvent<T> event, T data) {
        this.event = event;
        this.data = data;
    }

    @Override
    public MetricEvent<T> getEvent() {
        return event;
    }

    @Override
    public T getData() {
        return data;
    }
}
