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

package software.amazon.awssdk.codegen.poet.model;

import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PROTECTED;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.AwsServiceClientConfiguration;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;

public class ServiceClientConfigurationClass implements ClassSpec {
    private final ClassName defaultClientMetadataClassName;

    public ServiceClientConfigurationClass(IntermediateModel model) {
        String basePackage = model.getMetadata().getFullInternalPackageName();
        String serviceId = model.getMetadata().getServiceName();
        this.defaultClientMetadataClassName = ClassName.get(basePackage, serviceId + "ServiceClientConfiguration");
    }

    @Override
    public TypeSpec poetSpec() {
        return PoetUtils.createClassBuilder(defaultClientMetadataClassName)
                        .superclass(AwsServiceClientConfiguration.class)
                        .addMethod(constructor())
                        .addMethod(builderMethod())
                        .addModifiers(PUBLIC, FINAL)
                        .addAnnotation(SdkInternalApi.class)
                        .addType(builderInterfaceSpec())
                        .addType(builderImplSpec())
                        .build();
    }

    @Override
    public ClassName className() {
        return defaultClientMetadataClassName;
    }

    public MethodSpec constructor() {
        return MethodSpec.constructorBuilder()
                         .addModifiers(PROTECTED)
                         .addParameter(className().nestedClass("Builder"), "builder")
                         .addStatement("super(builder)")
                         .build();
    }

    public MethodSpec builderMethod() {
        return MethodSpec.methodBuilder("builder")
                         .addModifiers(PUBLIC, STATIC)
                         .addStatement("return new BuilderImpl()")
                         .returns(className().nestedClass("Builder"))
                         .build();
    }

    private TypeSpec builderInterfaceSpec() {
        return TypeSpec.interfaceBuilder("Builder")
                       .addModifiers(PUBLIC)
                       .addSuperinterface(ClassName.get(AwsServiceClientConfiguration.class).nestedClass("Builder"))
                       .addMethod(MethodSpec.methodBuilder("build")
                                            .addAnnotation(Override.class)
                                            .addModifiers(PUBLIC, ABSTRACT)
                                            .returns(className())
                                            .build())
                       .addMethod(MethodSpec.methodBuilder("region")
                                            .addModifiers(PUBLIC, ABSTRACT)
                                            .addParameter(Region.class, "region")
                                            .returns(className().nestedClass("Builder"))
                                            .build())
                       .addMethod(MethodSpec.methodBuilder("overrideConfiguration")
                                            .addModifiers(PUBLIC, ABSTRACT)
                                            .addParameter(ClientOverrideConfiguration.class, "clientOverrideConfiguration")
                                            .returns(className().nestedClass("Builder"))
                                            .build())
                       .build();
    }

    private TypeSpec builderImplSpec() {
        return TypeSpec.classBuilder("BuilderImpl")
                       .addModifiers(STATIC, FINAL)
                       .addSuperinterface(className().nestedClass("Builder"))
                       .superclass(ClassName.get(AwsServiceClientConfiguration.class).nestedClass("BuilderImpl"))
                       .addMethod(MethodSpec.constructorBuilder()
                                            .addModifiers(PUBLIC)
                                            .build())
                       .addMethod(MethodSpec.constructorBuilder()
                                            .addModifiers(PUBLIC)
                                            .addParameter(className(), "serviceClientConfiguration")
                                            .addStatement("super(serviceClientConfiguration)")
                                            .build())
                       .addMethod(MethodSpec.methodBuilder("region")
                                            .addAnnotation(Override.class)
                                            .addModifiers(PUBLIC)
                                            .addParameter(Region.class, "region")
                                            .returns(className().nestedClass("Builder"))
                                            .addStatement("this.region = region")
                                            .addStatement("return this")
                                            .build())
                       .addMethod(MethodSpec.methodBuilder("overrideConfiguration")
                                            .addAnnotation(Override.class)
                                            .addModifiers(PUBLIC)
                                            .addParameter(ClientOverrideConfiguration.class, "clientOverrideConfiguration")
                                            .returns(className().nestedClass("Builder"))
                                            .addStatement("this.overrideConfiguration = clientOverrideConfiguration")
                                            .addStatement("return this")
                                            .build())
                       .addMethod(MethodSpec.methodBuilder("build")
                                      .addAnnotation(Override.class)
                                      .addModifiers(PUBLIC)
                                      .returns(className())
                                      .addStatement("return new $T(this)", className())
                                      .build())
                       .build();
    }
}
