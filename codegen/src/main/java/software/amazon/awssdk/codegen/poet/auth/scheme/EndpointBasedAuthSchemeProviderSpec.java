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

package software.amazon.awssdk.codegen.poet.auth.scheme;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.endpoints.AwsEndpointAttribute;
import software.amazon.awssdk.awscore.endpoints.authscheme.EndpointAuthScheme;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4AuthScheme;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.rules.endpoints.ParameterModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.codegen.poet.rules.EndpointRulesSpecUtils;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.http.auth.aws.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.spi.AuthSchemeOption;
import software.amazon.awssdk.utils.Validate;

public class EndpointBasedAuthSchemeProviderSpec implements ClassSpec {
    private final AuthSchemeSpecUtils authSchemeSpecUtils;
    private final EndpointRulesSpecUtils endpointRulesSpecUtils;

    public EndpointBasedAuthSchemeProviderSpec(IntermediateModel intermediateModel) {
        this.authSchemeSpecUtils = new AuthSchemeSpecUtils(intermediateModel);
        this.endpointRulesSpecUtils = new EndpointRulesSpecUtils(intermediateModel);
    }

    @Override
    public ClassName className() {
        return authSchemeSpecUtils.defaultAuthSchemeProviderName();
    }

    @Override
    public TypeSpec poetSpec() {
        return PoetUtils.createClassBuilder(className())
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .addAnnotation(SdkInternalApi.class)
                        .addSuperinterface(authSchemeSpecUtils.providerInterfaceName())
                        .addMethod(constructor())
                        .addField(defaultInstance())
                        .addField(modeledResolverInstance())
                        .addField(endpointDelegateInstance())
                        .addMethod(createMethod())
                        .addMethod(resolveAuthSchemeMethod())
                        .build();
    }

    private MethodSpec constructor() {
        return MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build();
    }

    private FieldSpec defaultInstance() {
        return FieldSpec.builder(className(), "DEFAULT")
                        .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer("new $T()", className())
                        .build();
    }

    private FieldSpec endpointDelegateInstance() {
        return FieldSpec.builder(endpointRulesSpecUtils.providerInterfaceName(), "DELEGATE")
                        .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer("$T.defaultProvider()", endpointRulesSpecUtils.providerInterfaceName())
                        .build();
    }

    private FieldSpec modeledResolverInstance() {
        return FieldSpec.builder(authSchemeSpecUtils.providerInterfaceName(), "MODELED_RESOLVER")
                        .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer("$T.create()", authSchemeSpecUtils.internalModeledAuthSchemeProviderName())
                        .build();
    }

    private MethodSpec createMethod() {
        return MethodSpec.methodBuilder("create")
                         .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                         .returns(authSchemeSpecUtils.providerInterfaceName())
                         .addStatement("return DEFAULT")
                         .build();
    }

    private MethodSpec resolveAuthSchemeMethod() {
        MethodSpec.Builder spec = MethodSpec.methodBuilder("resolveAuthScheme")
                                            .addModifiers(Modifier.PUBLIC)
                                            .addAnnotation(Override.class)
                                            .returns(authSchemeSpecUtils.resolverReturnType())
                                            .addParameter(authSchemeSpecUtils.parametersInterfaceName(), "params");

        spec.addCode("$1T endpointParameters = $1T.builder()\n$>",
                     endpointRulesSpecUtils.parametersClassName());

        parameters().forEach((name, model) -> {
            if (authSchemeSpecUtils.includeParam(name)) {
                spec.addCode(".$1L(params.$1L())\n", endpointRulesSpecUtils.paramMethodName(name));
            }
        });
        spec.addStatement(".build()");
        spec.addStatement("$T endpoint = DELEGATE.resolveEndpoint(endpointParameters).join()", Endpoint.class);
        spec.addStatement("$T authSchemes = endpoint.attribute($T.AUTH_SCHEMES)",
                          ParameterizedTypeName.get(List.class, EndpointAuthScheme.class), AwsEndpointAttribute.class);
        spec.beginControlFlow("if (authSchemes == null)");
        spec.addStatement("return MODELED_RESOLVER.resolveAuthScheme(params)");
        spec.endControlFlow();


        spec.addStatement("$T options = new $T<>()", ParameterizedTypeName.get(List.class, AuthSchemeOption.class),
                          TypeName.get(ArrayList.class));
        spec.beginControlFlow("for ($T authScheme : authSchemes)", EndpointAuthScheme.class);
        addAuthSchemeSwitch(spec);
        spec.endControlFlow();
        return spec.addStatement("return $T.unmodifiableList(options)", Collections.class)
                   .build();
    }

    private void addAuthSchemeSwitch(MethodSpec.Builder spec) {
        spec.addStatement("$T name = authScheme.name()", String.class);
        spec.beginControlFlow("switch(name)");

        spec.addCode("case $S:", "sigv4");
        spec.addStatement("$T sigv4AuthScheme = $T.isInstanceOf($T.class, authScheme, $S, authScheme.getClass().getName())",
                          SigV4AuthScheme.class, Validate.class, SigV4AuthScheme.class,
                          "Expecting auth scheme of class SigV4AuthScheme, got instead object of class %s");

        spec.addCode("options.add($T.builder().schemeId($S)", AuthSchemeOption.class, "aws.auth#sigv4")
            .addCode(".putSignerProperty($T.SERVICE_SIGNING_NAME, sigv4AuthScheme.signingName())", AwsV4HttpSigner.class)
            .addCode(".putSignerProperty($T.REGION_NAME, sigv4AuthScheme.signingRegion())", AwsV4HttpSigner.class)
            .addCode(".putSignerProperty($T.DOUBLE_URL_ENCODE, !sigv4AuthScheme.disableDoubleEncoding())",
                     AwsV4HttpSigner.class)
            .addCode(".build());");
        spec.addStatement("break");

        // SigV4a -- Not yet implemented, we throw UnsupportedOperationException for now.
        spec.addCode("case $S:", "sigv4a");
        spec.addStatement("throw new $T($S)", UnsupportedOperationException.class, "SigV4a is not yet supported.");


        spec.addCode("default:")
            .addStatement("throw new $T($S + name)", IllegalArgumentException.class, "Unknown auth scheme: ");
        spec.endControlFlow();
    }

    private Map<String, ParameterModel> parameters() {
        return endpointRulesSpecUtils.parameters();
    }
}
