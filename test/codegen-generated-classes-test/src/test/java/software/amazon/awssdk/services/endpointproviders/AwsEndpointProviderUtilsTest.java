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

package software.amazon.awssdk.services.endpointproviders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.rules.AwsEndpointAttribute;
import software.amazon.awssdk.awscore.rules.AwsEndpointProviderUtils;
import software.amazon.awssdk.awscore.rules.EndpointAuthScheme;
import software.amazon.awssdk.awscore.rules.SigV4AuthScheme;
import software.amazon.awssdk.awscore.rules.SigV4aAuthScheme;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.rules.Identifier;
import software.amazon.awssdk.core.rules.Value;
import software.amazon.awssdk.core.rules.model.Endpoint;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.RegionScope;
import software.amazon.awssdk.services.protocolquery.model.AllTypesRequest;
import software.amazon.awssdk.utils.MapUtils;

public class AwsEndpointProviderUtilsTest {
    @Test
    public void endpointOverridden_attrIsFalse_returnsFalse() {
        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(SdkExecutionAttribute.ENDPOINT_OVERRIDDEN, false);
        assertThat(AwsEndpointProviderUtils.endpointIsOverridden(attrs)).isFalse();
    }

    @Test
    public void endpointOverridden_attrIsAbsent_returnsFalse() {
        ExecutionAttributes attrs = new ExecutionAttributes();
        assertThat(AwsEndpointProviderUtils.endpointIsOverridden(attrs)).isFalse();
    }

    @Test
    public void endpointOverridden_attrIsTrue_returnsTrue() {
        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(SdkExecutionAttribute.ENDPOINT_OVERRIDDEN, true);
        assertThat(AwsEndpointProviderUtils.endpointIsOverridden(attrs)).isTrue();
    }

    @Test
    public void endpointIsDiscovered_attrIsFalse_returnsFalse() {
        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(SdkInternalExecutionAttribute.IS_DISCOVERED_ENDPOINT, false);
        assertThat(AwsEndpointProviderUtils.endpointIsDiscovered(attrs)).isFalse();
    }

    @Test
    public void endpointIsDiscovered_attrIsAbsent_returnsFalse() {
        ExecutionAttributes attrs = new ExecutionAttributes();
        assertThat(AwsEndpointProviderUtils.endpointIsDiscovered(attrs)).isFalse();
    }

    @Test
    public void endpointIsDiscovered_attrIsTrue_returnsTrue() {
        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(SdkInternalExecutionAttribute.IS_DISCOVERED_ENDPOINT, true);
        assertThat(AwsEndpointProviderUtils.endpointIsDiscovered(attrs)).isTrue();
    }

    @Test
    public void valueAsEndpoint_isNone_throws() {
        assertThatThrownBy(() -> AwsEndpointProviderUtils.valueAsEndpointOrThrow(Value.none()))
            .isInstanceOf(SdkClientException.class);
    }

    @Test
    public void valueAsEndpoint_isString_throwsAsMsg() {
        assertThatThrownBy(() -> AwsEndpointProviderUtils.valueAsEndpointOrThrow(Value.fromStr("oops!")))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("oops!");
    }

    @Test
    public void valueAsEndpoint_isEndpoint_returnsEndpoint() {
        Value.Endpoint endpointVal = Value.Endpoint.builder()
                                                   .url("https://myservice.aws")
                                                   .build();

        Endpoint expected = Endpoint.builder()
                                    .url(URI.create("https://myservice.aws"))
                                    .build();

        assertThat(expected.url()).isEqualTo(AwsEndpointProviderUtils.valueAsEndpointOrThrow(endpointVal).url());
    }

    @Test
    public void valueAsEndpoint_endpointHasAuthSchemes_includesAuthSchemes() {
        List<Value> authSchemes = Arrays.asList(
            Value.fromRecord(MapUtils.of(Identifier.of("name"), Value.fromStr("sigv4"),
                                         Identifier.of("signingRegion"), Value.fromStr("us-west-2"),
                                         Identifier.of("signingName"), Value.fromStr("myservice"),
                                         Identifier.of("disableDoubleEncoding"), Value.fromBool(false))),

            Value.fromRecord(MapUtils.of(Identifier.of("name"), Value.fromStr("sigv4a"),
                                         Identifier.of("signingRegionSet"),
                                         Value.fromArray(Collections.singletonList(Value.fromStr("*"))),
                                         Identifier.of("signingName"), Value.fromStr("myservice"),
                                         Identifier.of("disableDoubleEncoding"), Value.fromBool(false))),

            // Unknown scheme name, should ignore
            Value.fromRecord(MapUtils.of(Identifier.of("name"), Value.fromStr("sigv5")))
        );


        Value.Endpoint endpointVal = Value.Endpoint.builder()
                                                   .url("https://myservice.aws")
                                                   .property("authSchemes", Value.fromArray(authSchemes))
                                                   .build();


        EndpointAuthScheme sigv4 = SigV4AuthScheme.builder()
                                                  .signingName("myservice")
                                                  .signingRegion("us-west-2")
                                                  .disableDoubleEncoding(false)
                                                  .build();

        EndpointAuthScheme sigv4a = SigV4aAuthScheme.builder()
                                                    .signingName("myservice")
                                                    .addSigningRegion("*")
                                                    .disableDoubleEncoding(false)
                                                    .build();

        assertThat(AwsEndpointProviderUtils.valueAsEndpointOrThrow(endpointVal).attribute(AwsEndpointAttribute.AUTH_SCHEMES))
            .containsExactly(sigv4, sigv4a);
    }

    @Test
    public void valueAsEndpoint_endpointHasUnknownProperty_ignores() {
        Value.Endpoint endpointVal = Value.Endpoint.builder()
                                                   .url("https://myservice.aws")
                                                   .property("foo", Value.fromStr("baz"))
                                                   .build();

        assertThat(AwsEndpointProviderUtils.valueAsEndpointOrThrow(endpointVal).attribute(AwsEndpointAttribute.AUTH_SCHEMES)).isNull();
    }

    @Test
    public void valueAsEndpoint_endpointHasHeaders_includesHeaders() {
        Value.Endpoint endpointVal = Value.Endpoint.builder()
                                                   .url("https://myservice.aws")
            .addHeader("foo1", "bar1")
            .addHeader("foo1", "bar2")
            .addHeader("foo2", "baz")
                                                   .build();

        Map<String, List<String>> expectedHeaders = MapUtils.of("foo1", Arrays.asList("bar1", "bar2"),
                                                                "foo2", Arrays.asList("baz"));

        assertThat(AwsEndpointProviderUtils.valueAsEndpointOrThrow(endpointVal).headers()).isEqualTo(expectedHeaders);
    }

    @Test
    public void regionBuiltIn_returnsAttrValue() {
        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(AwsExecutionAttribute.AWS_REGION, Region.US_EAST_1);
        assertThat(AwsEndpointProviderUtils.regionBuiltIn(attrs)).isEqualTo(Region.US_EAST_1);
    }

    @Test
    public void dualStackEnabledBuiltIn_returnsAttrValue() {
        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(AwsExecutionAttribute.DUALSTACK_ENDPOINT_ENABLED, true);
        assertThat(AwsEndpointProviderUtils.dualStackEnabledBuiltIn(attrs)).isEqualTo(true);
    }

    @Test
    public void fipsEnabledBuiltIn_returnsAttrValue() {
        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(AwsExecutionAttribute.FIPS_ENDPOINT_ENABLED, true);
        assertThat(AwsEndpointProviderUtils.fipsEnabledBuiltIn(attrs)).isEqualTo(true);
    }

    @Test
    public void endpointBuiltIn_doesNotIncludeQueryParams() {
        URI endpoint = URI.create("https://example.com/path?foo=bar");
        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(SdkExecutionAttribute.ENDPOINT_OVERRIDDEN, true);
        attrs.putAttribute(SdkExecutionAttribute.CLIENT_ENDPOINT, endpoint);

        assertThat(AwsEndpointProviderUtils.endpointBuiltIn(attrs).toString()).isEqualTo("https://example.com/path");
    }

    @Test
    public void useGlobalEndpointBuiltIn_returnsAttrValue() {
        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(SdkInternalExecutionAttribute.USE_GLOBAL_ENDPOINT, true);
        assertThat(AwsEndpointProviderUtils.useGlobalEndpointBuiltIn(attrs)).isEqualTo(true);
    }

    @Test
    public void setUri_combinesPathsCorrectly() {
        URI clientEndpoint = URI.create("https://override.example.com/a");
        URI requestUri = URI.create("https://override.example.com/a/c");
        URI resolvedUri = URI.create("https://override.example.com/a/b");

        SdkHttpRequest request = SdkHttpRequest.builder()
                                               .uri(requestUri)
                                               .method(SdkHttpMethod.GET)
                                               .build();

        assertThat(AwsEndpointProviderUtils.setUri(request, clientEndpoint, resolvedUri).getUri().toString())
            .isEqualTo("https://override.example.com/a/b/c");
    }

    @Test
    public void setHeaders_existingValuesOnOverride_combinesWithNewValues() {
        AwsRequest request = AllTypesRequest.builder()
                                            .overrideConfiguration(o -> o.putHeader("foo", Arrays.asList("a", "b")))
                                            .build();

        Map<String, List<String>> newHeaders = MapUtils.of("foo", Arrays.asList("c"));
        AwsRequest newRequest = AwsEndpointProviderUtils.addHeaders(request, newHeaders);

        Map<String, List<String>> expectedHeaders = MapUtils.of("foo", Arrays.asList("a", "b", "c"));

        assertThat(newRequest.overrideConfiguration().get().headers()).isEqualTo(expectedHeaders);
    }

    @Test
    public void setHeaders_noExistingValues_setCorrectly() {
        AwsRequest request = AllTypesRequest.builder()
                                            .overrideConfiguration(o -> {})
                                            .build();

        Map<String, List<String>> newHeaders = MapUtils.of("foo", Arrays.asList("a"));
        AwsRequest newRequest = AwsEndpointProviderUtils.addHeaders(request, newHeaders);

        Map<String, List<String>> expectedHeaders = MapUtils.of("foo", Arrays.asList("a"));

        assertThat(newRequest.overrideConfiguration().get().headers()).isEqualTo(expectedHeaders);
    }

    @Test
    public void setHeaders_noExistingOverrideConfig_createsOverrideConfig() {
        AwsRequest request = AllTypesRequest.builder()
                                            .build();

        Map<String, List<String>> newHeaders = MapUtils.of("foo", Arrays.asList("a"));
        AwsRequest newRequest = AwsEndpointProviderUtils.addHeaders(request, newHeaders);

        Map<String, List<String>> expectedHeaders = MapUtils.of("foo", Arrays.asList("a"));

        assertThat(newRequest.overrideConfiguration().get().headers()).isEqualTo(expectedHeaders);
    }

    @Test
    public void chooseAuthScheme_noSchemesInList_throws() {
        assertThatThrownBy(() -> AwsEndpointProviderUtils.chooseAuthScheme(Collections.emptyList()))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Endpoint did not contain any known auth schemes");
    }

    @Test
    public void chooseAuthScheme_noKnownSchemes_throws() {
        EndpointAuthScheme sigv1 = mock(EndpointAuthScheme.class);
        when(sigv1.name()).thenReturn("sigv1");

        EndpointAuthScheme sigv5 = mock(EndpointAuthScheme.class);
        when(sigv5.name()).thenReturn("sigv5");

        assertThatThrownBy(() -> AwsEndpointProviderUtils.chooseAuthScheme(Arrays.asList(sigv1, sigv5)))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Endpoint did not contain any known auth schemes");
    }

    @Test
    public void chooseAuthScheme_multipleSchemesKnown_choosesFirst() {
        EndpointAuthScheme sigv4 = SigV4AuthScheme.builder().build();
        EndpointAuthScheme sigv4a = SigV4aAuthScheme.builder().build();

        assertThat(AwsEndpointProviderUtils.chooseAuthScheme(Arrays.asList(sigv4, sigv4a))).isEqualTo(sigv4);
    }

    @Test
    public void setSigningParams_typeUnknown_throws() {
        EndpointAuthScheme sigv5 = mock(EndpointAuthScheme.class);
        assertThatThrownBy(() -> AwsEndpointProviderUtils.setSigningParams(new ExecutionAttributes(), sigv5))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Don't know how to set signing params for auth scheme");
    }

    @Test
    public void setSigningParams_sigv4_setsParamsCorrectly() {
        EndpointAuthScheme sigv4 = SigV4AuthScheme.builder()
            .signingName("myservice")
            .disableDoubleEncoding(true)
            .signingRegion("us-west-2")
            .build();

        ExecutionAttributes attrs = new ExecutionAttributes();

        AwsEndpointProviderUtils.setSigningParams(attrs, sigv4);

        assertThat(attrs.getAttribute(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME)).isEqualTo("myservice");
        assertThat(attrs.getAttribute(AwsSignerExecutionAttribute.SIGNING_REGION)).isEqualTo(Region.of("us-west-2"));
        assertThat(attrs.getAttribute(AwsSignerExecutionAttribute.SIGNER_DOUBLE_URL_ENCODE)).isEqualTo(false);
    }

    @Test
    public void setSigningParams_sigv4_paramsAreNull_doesNotOverrideAttrs() {
        EndpointAuthScheme sigv4 = SigV4AuthScheme.builder()
                                                  .build();

        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME, "myservice");
        attrs.putAttribute(AwsSignerExecutionAttribute.SIGNING_REGION, Region.of("us-west-2"));
        // disableDoubleEncoding has a default value

        AwsEndpointProviderUtils.setSigningParams(attrs, sigv4);

        assertThat(attrs.getAttribute(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME)).isEqualTo("myservice");
        assertThat(attrs.getAttribute(AwsSignerExecutionAttribute.SIGNING_REGION)).isEqualTo(Region.of("us-west-2"));
        assertThat(attrs.getAttribute(AwsSignerExecutionAttribute.SIGNER_DOUBLE_URL_ENCODE)).isEqualTo(true);
    }

    @Test
    public void setSigningParams_sigv4a_setsParamsCorrectly() {
        EndpointAuthScheme sigv4 = SigV4aAuthScheme.builder()
                                                  .signingName("myservice")
                                                  .disableDoubleEncoding(true)
                                                  .addSigningRegion("*")
                                                  .build();

        ExecutionAttributes attrs = new ExecutionAttributes();

        AwsEndpointProviderUtils.setSigningParams(attrs, sigv4);

        assertThat(attrs.getAttribute(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME)).isEqualTo("myservice");
        assertThat(attrs.getAttribute(AwsSignerExecutionAttribute.SIGNING_REGION_SCOPE)).isEqualTo(RegionScope.GLOBAL);
        assertThat(attrs.getAttribute(AwsSignerExecutionAttribute.SIGNER_DOUBLE_URL_ENCODE)).isEqualTo(false);
    }

    @Test
    public void setSigningParams_sigv4a_throwsIfRegionSetEmpty() {
        EndpointAuthScheme sigv4 = SigV4aAuthScheme.builder()
                                                   .build();

        ExecutionAttributes attrs = new ExecutionAttributes();

        assertThatThrownBy(() -> AwsEndpointProviderUtils.setSigningParams(attrs, sigv4))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Signing region set is empty");
    }

    @Test
    public void setSigningParams_sigv4a_throwsIfRegionSetHasMultiple() {
        EndpointAuthScheme sigv4 = SigV4aAuthScheme.builder()
                                                   .addSigningRegion("a")
                                                   .addSigningRegion("b")
                                                   .build();

        ExecutionAttributes attrs = new ExecutionAttributes();

        assertThatThrownBy(() -> AwsEndpointProviderUtils.setSigningParams(attrs, sigv4))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Don't know how to set scope of > 1 region");
    }

    @Test
    public void setSigningParams_sigv4a_signingNameNull_doesNotOverrideAttrs() {
        EndpointAuthScheme sigv4a = SigV4aAuthScheme.builder()
                                                    .addSigningRegion("*")
                                                  .build();

        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME, "myservice");
        // utils validates that the region list is not empty
        // disableDoubleEncoding has a default value

        AwsEndpointProviderUtils.setSigningParams(attrs, sigv4a);

        assertThat(attrs.getAttribute(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME)).isEqualTo("myservice");
        assertThat(attrs.getAttribute(AwsSignerExecutionAttribute.SIGNING_REGION_SCOPE)).isEqualTo(RegionScope.GLOBAL);
        assertThat(attrs.getAttribute(AwsSignerExecutionAttribute.SIGNER_DOUBLE_URL_ENCODE)).isEqualTo(true);
    }
}
