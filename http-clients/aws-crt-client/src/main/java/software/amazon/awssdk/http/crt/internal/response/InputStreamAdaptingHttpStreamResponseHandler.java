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

package software.amazon.awssdk.http.crt.internal.response;

import static software.amazon.awssdk.http.crt.internal.CrtUtils.wrapWithIoExceptionIfRetryable;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.crt.CRT;
import software.amazon.awssdk.crt.http.HttpClientConnection;
import software.amazon.awssdk.crt.http.HttpException;
import software.amazon.awssdk.crt.http.HttpHeader;
import software.amazon.awssdk.crt.http.HttpHeaderBlock;
import software.amazon.awssdk.crt.http.HttpStream;
import software.amazon.awssdk.crt.http.HttpStreamResponseHandler;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.HttpStatusFamily;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.crt.AwsCrtHttpClient;
import software.amazon.awssdk.utils.async.InputStreamSubscriber;
import software.amazon.awssdk.utils.async.SimplePublisher;

/**
 * Response handler adaptor for {@link AwsCrtHttpClient}.
 */
@SdkInternalApi
public final class InputStreamAdaptingHttpStreamResponseHandler implements HttpStreamResponseHandler {
    private final SdkHttpFullResponse.Builder responseBuilder = SdkHttpFullResponse.builder();
    private volatile InputStreamSubscriber inputStreamSubscriber;
    private final SimplePublisher<ByteBuffer> simplePublisher = new SimplePublisher<>();

    private final CompletableFuture<SdkHttpFullResponse> requestCompletionFuture;
    private final HttpClientConnection crtConn;

    public InputStreamAdaptingHttpStreamResponseHandler(HttpClientConnection crtConn,
                                                        CompletableFuture<SdkHttpFullResponse> requestCompletionFuture) {
        this.crtConn = crtConn;
        this.requestCompletionFuture = requestCompletionFuture;
    }

    @Override
    public void onResponseHeaders(HttpStream stream, int responseStatusCode, int blockType,
                                  HttpHeader[] nextHeaders) {
        if (blockType == HttpHeaderBlock.MAIN.getValue()) {
            for (HttpHeader h : nextHeaders) {
                responseBuilder.appendHeader(h.getName(), h.getValue());
            }
        }

        responseBuilder.statusCode(responseStatusCode);
    }

    @Override
    public int onResponseBody(HttpStream stream, byte[] bodyBytesIn) {
        if (inputStreamSubscriber == null) {
            inputStreamSubscriber = new InputStreamSubscriber();
            simplePublisher.subscribe(inputStreamSubscriber);
            responseBuilder.content(AbortableInputStream.create(inputStreamSubscriber));
        }

        CompletableFuture<Void> writeFuture = simplePublisher.send(ByteBuffer.wrap(bodyBytesIn));

        if (writeFuture.isDone() && !writeFuture.isCompletedExceptionally()) {
            // Optimization: If write succeeded immediately, return non-zero to avoid the extra call back into the CRT.
            return bodyBytesIn.length;
        }

        writeFuture.whenComplete((result, failure) -> {
            if (failure != null) {
                failFutureAndCloseConnection(stream, failure);
                return;
            }

            // increment the window upon buffer consumption.
            stream.incrementWindow(bodyBytesIn.length);
        });

        // the bodyBytesIn have not cleared the queues yet, so do let backpressure do its thing.
        return 0;
    }

    @Override
    public void onResponseComplete(HttpStream stream, int errorCode) {
        if (errorCode == CRT.AWS_CRT_SUCCESS) {
            onSuccessfulResponseComplete(stream);
        } else {
            onFailedResponseComplete(stream, errorCode);
        }
    }

    private void failFutureAndCloseConnection(HttpStream stream, Throwable failure) {
        requestCompletionFuture.completeExceptionally(failure);
        crtConn.shutdown();
        crtConn.close();
        stream.close();
    }

    private void onFailedResponseComplete(HttpStream stream, int errorCode) {
        Throwable toThrow =
            wrapWithIoExceptionIfRetryable(new HttpException(errorCode));

        simplePublisher.error(toThrow);
        failFutureAndCloseConnection(stream, toThrow);
    }

    private void onSuccessfulResponseComplete(HttpStream stream) {
        // always close the connection on a 5XX response code.
        if (HttpStatusFamily.of(responseBuilder.statusCode()) == HttpStatusFamily.SERVER_ERROR) {
            crtConn.shutdown();
        }

        requestCompletionFuture.complete(responseBuilder.build());
        simplePublisher.complete();
        crtConn.close();
        stream.close();
    }
}
