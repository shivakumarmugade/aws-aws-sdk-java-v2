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

package software.amazon.awssdk.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class MetricEventTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void methodSetup() {
        MetricEvent.clearEvents();
    }

    @Test
    public void testOf_variadicOverload_createdProperly() {
        MetricEvent<Integer> event = MetricEvent.of("event", Integer.class, MetricCategory.DEFAULT);

        assertThat(event.categories()).containsExactly(MetricCategory.DEFAULT);
        assertThat(event.name()).isEqualTo("event");
        assertThat(event.dataClass()).isEqualTo(Integer.class);
    }

    @Test
    public void testOf_setOverload_createdProperly() {
        MetricEvent<Integer> event = MetricEvent.of("event", Integer.class, Stream.of(MetricCategory.DEFAULT).collect(Collectors.toSet()));

        assertThat(event.categories()).containsExactly(MetricCategory.DEFAULT);
        assertThat(event.name()).isEqualTo("event");
        assertThat(event.dataClass()).isEqualTo(Integer.class);
    }

    @Test
    public void testOf_variadicOverload_c1Null_throws() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("must not contain null elements");
        MetricEvent.of("event", Integer.class, (MetricCategory) null);
    }

    @Test
    public void testOf_variadicOverload_c1NotNull_cnNull_doesNotThrow() {
        MetricEvent.of("event", Integer.class, MetricCategory.DEFAULT, null);
    }

    @Test
    public void testOf_variadicOverload_cnContainsNull_throws() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("must not contain null elements");
        MetricEvent.of("event", Integer.class, MetricCategory.DEFAULT, new MetricCategory[]{ null });
    }

    @Test
    public void testOf_setOverload_null_throws() {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("object is null");
        MetricEvent.of("event", Integer.class, (Set<MetricCategory>) null);
    }

    @Test
    public void testOf_setOverload_nullElement_throws() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("categories must not contain null elements");
        MetricEvent.of("event", Integer.class, Stream.of((MetricCategory) null).collect(Collectors.toSet()));
    }

    @Test
    public void testOf_namePreviouslyUsed_throws() {
        String fooName = "metricEvent";

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(fooName + " has already been created");

        MetricEvent.of(fooName, Integer.class, MetricCategory.DEFAULT);
        MetricEvent.of(fooName, Integer.class, MetricCategory.DEFAULT);
    }

    @Test
    public void testOf_namePreviouslyUsed_differentArgs_throws() {
        String fooName = "metricEvent";

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(fooName + " has already been created");

        MetricEvent.of(fooName, Integer.class, MetricCategory.DEFAULT);
        MetricEvent.of(fooName, Long.class, MetricCategory.STREAMING);
    }

    @Test
    public void testOf_namePreviouslyUsed_doesNotReplaceExisting() {
        String fooName = "metricEvent";

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(fooName + " has already been created");

        MetricEvent.of(fooName, Integer.class, MetricCategory.DEFAULT);
        try {
            MetricEvent.of(fooName, Long.class, MetricCategory.STREAMING);
        } finally {
            Map<String, ? extends MetricEvent<?>> eventsMap = MetricEvent.declaredEvents()
                    .stream()
                    .collect(Collectors.toMap(MetricEvent::name, e -> e));

            MetricEvent<?> fooEvent = eventsMap.get(fooName);

            assertThat(fooEvent.name()).isEqualTo(fooName);
            assertThat(fooEvent.dataClass()).isEqualTo(Integer.class);
            assertThat(fooEvent.categories()).containsExactly(MetricCategory.DEFAULT);
        }
    }
}
