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

package software.amazon.awssdk.http.auth.aws.internal.signer;

import static software.amazon.awssdk.checksums.DefaultChecksumAlgorithm.SHA256;
import static software.amazon.awssdk.http.auth.aws.internal.util.ChecksumUtil.fromChecksumAlgorithm;
import static software.amazon.awssdk.http.auth.aws.internal.util.ChecksumUtil.readAll;
import static software.amazon.awssdk.http.auth.aws.util.SignerConstant.X_AMZ_CONTENT_SHA256;
import static software.amazon.awssdk.http.auth.aws.util.SignerUtils.getBinaryRequestPayloadStream;
import static software.amazon.awssdk.utils.BinaryUtils.toHex;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.internal.checksums.SdkChecksum;
import software.amazon.awssdk.http.auth.aws.internal.io.ChecksumInputStream;
import software.amazon.awssdk.http.auth.aws.internal.io.ChecksumSubscriber;
import software.amazon.awssdk.http.auth.aws.signer.Checksummer;

/**
 * The default implementation of a checksummer. By default, this will calculate the SHA256 checksum of a payload and add it as the
 * value for the 'x-amz-content-sha256' header on the request.
 */
@SdkInternalApi
public final class DefaultChecksummer implements Checksummer {

    @Override
    public void checksum(ContentStreamProvider payload, SdkHttpRequest.Builder request) {
        SdkChecksum sdkChecksum = fromChecksumAlgorithm(SHA256);
        InputStream payloadStream = new ChecksumInputStream(
            getBinaryRequestPayloadStream(payload),
            Collections.singletonList(sdkChecksum)
        );

        readAll(payloadStream);

        request.putHeader(X_AMZ_CONTENT_SHA256, toHex(sdkChecksum.getChecksumBytes()));
    }

    @Override
    public CompletableFuture<Void> checksum(Publisher<ByteBuffer> payload, SdkHttpRequest.Builder request) {
        SdkChecksum sdkChecksum = fromChecksumAlgorithm(SHA256);

        ChecksumSubscriber checksumSubscriber = new ChecksumSubscriber(Collections.singletonList(sdkChecksum));

        if (payload != null) {
            payload.subscribe(checksumSubscriber);
        }

        return checksumSubscriber.checksum().thenRun(() -> {
            String checksum = toHex(sdkChecksum.getChecksumBytes());
            request.putHeader(X_AMZ_CONTENT_SHA256, checksum);
        });
    }
}
