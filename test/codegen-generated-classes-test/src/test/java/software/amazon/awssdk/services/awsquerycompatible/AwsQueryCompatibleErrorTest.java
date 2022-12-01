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

package software.amazon.awssdk.services.awsquerycompatible;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.net.URI;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.querycompatiblejson.QueryCompatibleJsonAsyncClient;
import software.amazon.awssdk.services.querycompatiblejson.QueryCompatibleJsonClient;
import software.amazon.awssdk.utils.builder.SdkBuilder;

public class AwsQueryCompatibleErrorTest {

    @Rule
    public WireMockRule wireMock = new WireMockRule(0);

    private QueryCompatibleJsonClient client;
    private QueryCompatibleJsonAsyncClient asyncClient;
    private static final String SERVICE_NAME = "QueryCompatibleJson";
    private static final String QUERY_HEADER_VALUE = "CustomException;Sender";
    private static final String INVALID_QUERY_HEADER_VALUE = "CustomException Sender";
    private static final String X_AMZN_QUERY_ERROR = "x-amzn-query-error";

    @Before
    public void setupClient() {
        client = QueryCompatibleJsonClient.builder()
                                       .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                                       .region(Region.US_EAST_1)
                                       .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                       .build();

        asyncClient = QueryCompatibleJsonAsyncClient.builder()
                                                 .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                                                 .region(Region.US_EAST_1)
                                                 .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                                 .build();
    }

    @Test
    public void verifySyncClientException_shouldRetrievedFromHeader() {
        stubResponseWithQueryHeaderAndBody(QUERY_HEADER_VALUE);
        try {
            client.allType(SdkBuilder::build);
        } catch (AwsServiceException e) {
            verifyErrorResponse(e, "CustomException");
        }
    }

    @Test
    public void verifySyncClientException_shouldRetrievedFromContent() {
        stubResponseWithQueryHeaderAndBody(INVALID_QUERY_HEADER_VALUE);
        try {
            client.allType(SdkBuilder::build);
        } catch (AwsServiceException e) {
            verifyErrorResponse(e, "ServiceModeledException");
        }
    }

    @Test
    public void verifyAsyncClientException_shouldRetrievedFromHeader() {
        stubResponseWithQueryHeaderAndBody(QUERY_HEADER_VALUE);
        try {
            asyncClient.allType(SdkBuilder::build);
        } catch (AwsServiceException e) {
            verifyErrorResponse(e, "CustomException");
        }
    }

    @Test
    public void verifyAsyncClientException_shouldRetrievedFromContent() {
        stubResponseWithQueryHeaderAndBody(INVALID_QUERY_HEADER_VALUE);
        try {
            asyncClient.allType(SdkBuilder::build);
        } catch (AwsServiceException e) {
            verifyErrorResponse(e, "ServiceModeledException");
        }
    }

    private void stubResponseWithQueryHeaderAndBody(String queryHeaderValue) {
        stubFor(post(anyUrl())
                    .willReturn(aResponse()
                                    .withStatus(403)
                                    .withHeader(X_AMZN_QUERY_ERROR, queryHeaderValue)
                                    .withBody("{\"__type\": \"ServiceModeledException\"}")));
    }

    private void verifyErrorResponse(AwsServiceException e, String expectedErrorCode) {
        AwsErrorDetails awsErrorDetails = e.awsErrorDetails();
        assertThat(e.statusCode()).isEqualTo(403);
        assertThat(awsErrorDetails.errorCode()).isEqualTo(expectedErrorCode);
        assertThat(awsErrorDetails.serviceName()).isEqualTo(SERVICE_NAME);
        assertThat(awsErrorDetails.sdkHttpResponse()).isNotNull();
    }
}
