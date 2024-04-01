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

package software.amazon.awssdk.transfer.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;
import static software.amazon.awssdk.transfer.s3.SizeConstant.MB;
import static software.amazon.awssdk.transfer.s3.util.ChecksumUtils.computeCheckSum;

import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.transfer.s3.model.CompletedCopy;
import software.amazon.awssdk.transfer.s3.model.Copy;
import software.amazon.awssdk.transfer.s3.progress.LoggingTransferListener;

public class S3TransferManagerCopyIntegrationTest extends S3IntegrationTestBase {
    private static final String BUCKET = temporaryBucketName(S3TransferManagerCopyIntegrationTest.class);
    private static final String ORIGINAL_OBJ = "test_file.dat";
    private static final String COPIED_OBJ = "test_file_copy.dat";
    private static final String ORIGINAL_OBJ_SPECIAL_CHARACTER = "original-special-chars-@$%";
    private static final String COPIED_OBJ_SPECIAL_CHARACTER = "special-special-chars-@$%";
    private static final long OBJ_SIZE = ThreadLocalRandom.current().nextLong(8 * MB, 16 * MB + 1);

    @BeforeAll
    public static void setUp() throws Exception {
        createBucket(BUCKET);
    }

    @AfterAll
    public static void teardown() throws Exception {
        deleteBucketAndAllContents(BUCKET);
    }

    enum TmType{
        JAVA, CRT
    }

    private static Stream<Arguments> transferManagers() {
        return Stream.of(
            Arguments.of(TmType.JAVA)
            //Arguments.of(TmType.CRT)
        );
    }

    @ParameterizedTest
    @MethodSource("transferManagers")
    void copy_copiedObject_hasSameContent(TmType tmType) throws Exception {
        CaptureTransferListener transferListener = new CaptureTransferListener();
        byte[] originalContent = randomBytes(OBJ_SIZE);
        createOriginalObject(originalContent, ORIGINAL_OBJ);
        copyObject(ORIGINAL_OBJ, COPIED_OBJ, transferListener, tmType);
        validateCopiedObject(originalContent, ORIGINAL_OBJ);
    }

    @ParameterizedTest
    @MethodSource("transferManagers")
    void copy_specialCharacters_hasSameContent(TmType tmType) throws Exception {
        CaptureTransferListener transferListener = new CaptureTransferListener();
        byte[] originalContent = randomBytes(OBJ_SIZE);
        createOriginalObject(originalContent, ORIGINAL_OBJ_SPECIAL_CHARACTER);
        copyObject(ORIGINAL_OBJ_SPECIAL_CHARACTER, COPIED_OBJ_SPECIAL_CHARACTER, transferListener, tmType);
        validateCopiedObject(originalContent, COPIED_OBJ_SPECIAL_CHARACTER);
    }

    private void createOriginalObject(byte[] originalContent, String originalKey) {
        s3.putObject(r -> r.bucket(BUCKET)
                           .key(originalKey),
                     RequestBody.fromBytes(originalContent));
    }

    private void copyObject(String original, String destination, CaptureTransferListener transferListener, TmType tmType) throws Exception {
        S3TransferManager tm = tmType == TmType.JAVA ? tmJava : tmCrt;
        Copy copy = tm.copy(c -> c
            .copyObjectRequest(r -> r
                .sourceBucket(BUCKET)
                .sourceKey(original)
                .destinationBucket(BUCKET)
                .destinationKey(destination))
            .addTransferListener(LoggingTransferListener.create())
            .addTransferListener(transferListener));

        CompletedCopy completedCopy = copy.completionFuture().join();
        assertThat(completedCopy.response().responseMetadata().requestId()).isNotNull();
        assertThat(completedCopy.response().sdkHttpResponse()).isNotNull();

        if (tmType == TmType.JAVA) {
            Thread.sleep(500);
            assertListenerForSuccessfulTransferComplete(transferListener);
        }
    }

    private static void assertListenerForSuccessfulTransferComplete(CaptureTransferListener transferListener) {
        assertThat(transferListener.isTransferInitiated()).isTrue();
        assertThat(transferListener.isTransferComplete()).isTrue();
        assertThat(transferListener.getRatioTransferredList()).isNotEmpty();
        assertThat(transferListener.getRatioTransferredList()).contains(0.0);
        assertThat(transferListener.getRatioTransferredList()).contains(1.0);
        assertThat(transferListener.getExceptionCaught()).isNull();
    }

    private void validateCopiedObject(byte[] originalContent, String originalKey) {
        ResponseBytes<GetObjectResponse> copiedObject = s3.getObject(r -> r.bucket(BUCKET)
                                                                           .key(originalKey),
                                                                     ResponseTransformer.toBytes());
        assertThat(computeCheckSum(copiedObject.asByteArrayUnsafe())).isEqualTo(computeCheckSum(originalContent));
    }

    private static byte[] randomBytes(long size) {
        byte[] bytes = new byte[Math.toIntExact(size)];
        ThreadLocalRandom.current().nextBytes(bytes);
        return bytes;
    }
}
