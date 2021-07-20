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

package software.amazon.awssdk.core.internal.batchutilities;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.testng.Assert;

public class BatchBufferTest {

    private BatchBuffer<String, String, List<RequestWithId>> buffer;
    private String destination;

    BatchAndSendFunction<String, List<RequestWithId>> batchingFunction =
        (identifiedRequests, destination) -> {
            List<RequestWithId> entries = new ArrayList<>(identifiedRequests.size());
            identifiedRequests.forEach(identifiedRequest -> {
                String id = identifiedRequest.getId();
                String request = identifiedRequest.getRequest();
                entries.add(new RequestWithId(id, request));
            });
            return CompletableFuture.supplyAsync(() -> {
                waitForTime(150);
                return entries;
            });
        };

    UnpackBatchResponseFunction<List<RequestWithId>, String> unpackResponseFunction =
        requestBatchResponse -> {
            List<IdentifiedResponse<String>> identifiedResponses = new ArrayList<>();
            for (RequestWithId requestWithId : requestBatchResponse) {
                identifiedResponses.add(new IdentifiedResponse<>(requestWithId.getId(), requestWithId.getMessage()));
            }
            return identifiedResponses;
        };

    //Object to mimic a batch request entry
    private static class RequestWithId {

        private final String id;
        private final String message;

        public RequestWithId(String id, String message) {
            this.id = id;
            this.message = message;
        }

        public String getId() {
            return id;
        }

        public String getMessage() {
            return message;
        }
    }

    @Before
    public void beforeEachBufferTest() {
        buffer = new BatchBuffer<>(10, Duration.ofMillis(200), batchingFunction, unpackResponseFunction);
        destination = "testDestination";
    }

    @After
    public void afterEachBufferTest() {
        buffer.close();
    }

    @Test
    public void sendTenRequestsTest() {
        String[] requests = createRequestsOfSize(10);
        List<CompletableFuture<String>> responses = createAndSendResponses(requests);
        CompletableFuture.allOf(responses.toArray(new CompletableFuture[0])).join();
        checkAllResponses(responses);
    }

    @Test
    public void sendTwentyRequestsTest() {
        String[] requests = createRequestsOfSize(20);
        List<CompletableFuture<String>> responses = createAndSendResponses(requests);
        checkAllResponses(responses);
    }

    @Test
    public void scheduleSendFiveRequestsTest() {
        String[] requests = createRequestsOfSize(5);

        long startTime = System.nanoTime();
        List<CompletableFuture<String>> responses = createAndSendResponses(requests);
        CompletableFuture.allOf(responses.toArray(new CompletableFuture[0])).join();
        long endTime = System.nanoTime();

        Assert.assertTrue(Duration.ofNanos(endTime - startTime).toMillis() > 200);
        checkAllResponses(responses);
    }

    @Test
    public void cancelScheduledBatchTest() {
        String[] requestsBatch1 = createRequestsOfSize(5);
        String[] requestsBatch2 = createRequestsOfSize(5, 5);

        long startTime = System.nanoTime();
        List<CompletableFuture<String>> responses = createAndSendResponses(requestsBatch1);
        waitForTime(197);
        responses.addAll(createAndSendResponses(requestsBatch2));
        CompletableFuture.allOf(responses.toArray(new CompletableFuture[0])).join();
        long endTime = System.nanoTime();

        Assert.assertTrue(Duration.ofNanos(endTime - startTime).toMillis() < 400);
        checkAllResponses(responses);
    }

    @Test
    public void scheduleTwoBatchTests() {
        String[] requestsBatch1 = createRequestsOfSize(5);
        String[] requestsBatch2 = createRequestsOfSize(5, 5);

        long startTime = System.nanoTime();
        List<CompletableFuture<String>> responses = createAndSendResponses(requestsBatch1);
        waitForTime(250);
        responses.addAll(createAndSendResponses(requestsBatch2));
        CompletableFuture.allOf(responses.toArray(new CompletableFuture[0])).join();
        long endTime = System.nanoTime();

        Assert.assertEquals(responses.size(), 10);
        Assert.assertTrue(Duration.ofNanos(endTime - startTime).toMillis() > 400);
        checkAllResponses(responses);
    }

    @Test
    public void sendTenMessagesWithEachThread() {
        int numThreads = 10;
        ConcurrentLinkedQueue<CompletableFuture<String>> responses = new ConcurrentLinkedQueue<>();
        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
        CompletionService<List<CompletableFuture<String>>> completionService = new ExecutorCompletionService<>(executorService);

        for (int i = 0; i < numThreads; i++) {
            String[] requests = createRequestsOfSize(i*10, 10);
            completionService.submit(() -> {
                List<CompletableFuture<String>> newResponses = createAndSendResponses(requests);
                responses.addAll(newResponses);
                return newResponses;
            });
        }
        for (int i = 0; i < numThreads; i++) {
            try {
                CompletableFuture.allOf(completionService.take()
                                                         .get(1, TimeUnit.SECONDS)
                                                         .toArray(new CompletableFuture[0]))
                                 .join();
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                System.err.println(e);
            }
        }

        Iterator<CompletableFuture<String>> responsesIterator = responses.iterator();
        for (int i = 0; responsesIterator.hasNext(); i++) {
            // Can't properly check messages like in previous tests since messages aren't inserted in order.
            System.err.println(responsesIterator.next().join());
        }
        Assert.assertEquals(responses.size(), numThreads*10);
    }

    @Test
    public void scheduleFiveMessagesWithEachThread() {
        int numThreads = 10;
        ConcurrentLinkedQueue<CompletableFuture<String>> responses = new ConcurrentLinkedQueue<>();
        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
        CompletionService<List<CompletableFuture<String>>> completionService = new ExecutorCompletionService<>(executorService);

        for (int i = 0; i < numThreads; i++) {
            String[] requests = createRequestsOfSize(i*5, 5);
            int finalI = i;
            completionService.submit(() -> {
                List<CompletableFuture<String>> newResponses = createAndSendResponses(requests, Integer.toString(finalI));
                responses.addAll(newResponses);
                return newResponses;
            });
        }
        for (int i = 0; i < numThreads; i++) {
            try {
                CompletableFuture.allOf(completionService.take()
                                                         .get(300, TimeUnit.MILLISECONDS)
                                                         .toArray(new CompletableFuture[0]))
                                 .join();
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                System.err.println(e);
            }
        }
        Iterator<CompletableFuture<String>> responsesIterator = responses.iterator();
        for (int i = 0; responsesIterator.hasNext(); i++) {
            // Can't properly check messages like in previous tests since messages aren't inserted in order.
            System.err.println(responsesIterator.next().join());
        }
        Assert.assertEquals(responses.size(), numThreads*5);
    }

    private void waitForTime(int msToWait) {
        try {
            Thread.sleep(msToWait);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void checkAllResponses(List<CompletableFuture<String>> responses) {
        for (int i = 0; i < responses.size(); i++) {
            Assert.assertEquals(responses.get(i).join(), "Message " + i);
        }
    }

    private String[] createRequestsOfSize(int size) {
        return createRequestsOfSize(0, size);
    }

    private String[] createRequestsOfSize(int startingId, int size) {
        String[] requests = new String[size];
        for (int i = 0; i < size; i++) {
            requests[i] = "Message " + (i + startingId);
        }
        return requests;
    }

    private List<CompletableFuture<String>> createAndSendResponses(String[] requests) {
        return createAndSendResponses(requests, destination);
    }


    private List<CompletableFuture<String>> createAndSendResponses(String[] requests, String destination) {
        List<CompletableFuture<String>> responses = new ArrayList<>();
        for (String request : requests) {
            try {
                responses.add(buffer.sendRequest(request, destination));
            } catch (Exception e) {
                System.err.println("Error" + e);
            }
        }
        return responses;
    }
}
