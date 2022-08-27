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

package software.amazon.awssdk.transfer.s3.internal;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.internal.crt.S3MetaRequestPauseObservable;
import software.amazon.awssdk.transfer.s3.internal.model.DefaultFileUpload;

public class DefaultFileUploadTest {

    @Test
    public void equals_hashcode() {
        EqualsVerifier.forClass(DefaultFileUpload.class)
                      .withNonnullFields("completionFuture", "progress", "request", "observable", "resumableFileUpload")
                      .withPrefabValues(S3MetaRequestPauseObservable.class, new S3MetaRequestPauseObservable(), new S3MetaRequestPauseObservable())
                      .verify();
    }
}