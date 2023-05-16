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

package software.amazon.awssdk.http.auth.spi;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.TokenIdentity;

/**
 * An authentication scheme, composed of:
 * <ol>
 *     <li>A scheme ID - A unique identifier for the authentication scheme.</li>
 *     <li>An identity provider - An API that can be queried to acquire the customer's identity.</li>
 *     <li>A signer - An API that can be used to sign HTTP requests.</li>
 * </ol>
 *
 * @see IdentityProvider
 * @see HttpSigner
 *
 * @param <T> The type of the {@link Identity} used by this authentication scheme.
 */
@SdkPublicApi
public interface HttpAuthScheme<T extends Identity> {

    /**
     * Retrieve the scheme ID, a unique identifier for the authentication scheme (aws.auth#sigv4, smithy.api#httpBearerAuth).
     */
    String schemeId();

    /**
     * Retrieve the identity provider associated with this authentication scheme. The identity generated by this provider is
     * guaranteed to be supported by the signer in this authentication scheme.
     * <p>
     * For example, if the scheme ID is aws.auth#sigv4, the provider returns an {@link AwsCredentialsIdentity}, if the scheme
     * ID is httpBearerAuth, the provider returns a {@link TokenIdentity}.
     * <p>
     * Note, the returned identity provider may differ from the type of identity provider retrieved from the provided identity
     * provider configuration.
     */
    IdentityProvider<T> identityProvider(IdentityProviderConfiguration providers);

    /**
     * Retrieve the signer associated with this authentication scheme. This signer is guaranteed to support the identity
     * generated by the identity provider in this authentication scheme.
     */
    HttpSigner<T> signer();
}
