/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.awscore.client.handler;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.AwsExecutionAttributes;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.signer.internal.AwsSignerParams;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfig;
import software.amazon.awssdk.awscore.config.AwsAdvancedClientOption;
import software.amazon.awssdk.awscore.config.AwsClientConfiguration;
import software.amazon.awssdk.core.RequestOverrideConfig;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.ServiceAdvancedConfiguration;
import software.amazon.awssdk.core.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptorChain;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.core.signer.SignerContext;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
final class AwsClientHandlerUtils {

    private AwsClientHandlerUtils() {

    }

    static ExecutionContext createExecutionContext(SdkRequest originalRequest,
                                                   AwsClientConfiguration clientConfiguration,
                                                   ServiceAdvancedConfiguration serviceAdvancedConfiguration) {

        AwsCredentialsProvider credentialsProvider = originalRequest.requestOverrideConfig()
                                                                    .filter(c -> c instanceof AwsRequestOverrideConfig)
                                                                    .map(c -> (AwsRequestOverrideConfig) c)
                                                                    .flatMap(AwsRequestOverrideConfig::credentialsProvider)
                                                                    .orElse(clientConfiguration.credentialsProvider());

        ClientOverrideConfiguration overrideConfiguration = clientConfiguration.overrideConfiguration();

        AwsCredentials credentials = credentialsProvider.getCredentials();

        Validate.validState(credentials != null, "Credential providers must never return null.");

        ExecutionAttributes executionAttributes = new ExecutionAttributes()
            .putAttribute(AwsExecutionAttributes.SERVICE_ADVANCED_CONFIG, serviceAdvancedConfiguration)
            .putAttribute(AwsExecutionAttributes.AWS_CREDENTIALS, credentials)
            .putAttribute(AwsExecutionAttributes.REQUEST_CONFIG, originalRequest.requestOverrideConfig()
                                                                                .map(c -> (RequestOverrideConfig) c)
                                                                                .orElse(AwsRequestOverrideConfig.builder()
                                                                                                                .build()))
            .putAttribute(AwsExecutionAttributes.SERVICE_SIGNING_NAME,
                          overrideConfiguration.advancedOption(AwsAdvancedClientOption.SERVICE_SIGNING_NAME))
            .putAttribute(AwsExecutionAttributes.AWS_REGION,
                          overrideConfiguration.advancedOption(AwsAdvancedClientOption.AWS_REGION));

        return ExecutionContext.builder()
                               .interceptorChain(new ExecutionInterceptorChain(overrideConfiguration.executionInterceptors()))
                               .interceptorContext(InterceptorContext.builder()
                                                                     .request(originalRequest)
                                                                     .build())
                               .executionAttributes(executionAttributes)
                               .signer(overrideConfiguration.advancedOption(AwsAdvancedClientOption.SIGNER))
                               .signerContext(createSignerContext(overrideConfiguration.advancedOption(AwsAdvancedClientOption
                                                                                                        .SIGNER_CONTEXT),
                                                                  executionAttributes))
                               .build();
    }

    private static SignerContext createSignerContext(SignerContext userProvidedContext,
                                                     ExecutionAttributes executionAttributes) {
        // If SignerContext is set on the client by user, we will use it
        if (userProvidedContext != null) {
            return userProvidedContext;
        }

        // TODO How to set doubleUrlEncoding and chunkedBodySigning options?
        final AwsSignerParams signerParams = AwsSignerParams.builder()
                                                            .awsCredentials(executionAttributes.getAttribute(
                                                                AwsExecutionAttributes.AWS_CREDENTIALS))
                                                            .signingName(executionAttributes.getAttribute(
                                                                AwsExecutionAttributes.SERVICE_SIGNING_NAME))
                                                            .region(executionAttributes.getAttribute(
                                                                AwsExecutionAttributes.AWS_REGION))
                                                            .timeOffset(executionAttributes.getAttribute(
                                                                AwsExecutionAttributes.TIME_OFFSET))
                                                            .build();

        return SignerContext.builder()
                            .putAttribute(AwsExecutionAttributes.AWS_SIGNER_PARAMS, signerParams)
                            .build();
    }
}
