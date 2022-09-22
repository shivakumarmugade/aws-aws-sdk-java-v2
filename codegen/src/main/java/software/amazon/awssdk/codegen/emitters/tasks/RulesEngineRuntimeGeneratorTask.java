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

package software.amazon.awssdk.codegen.emitters.tasks;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import software.amazon.awssdk.codegen.emitters.GeneratorTask;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.emitters.SimpleGeneratorTask;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.Validate;

public final class RulesEngineRuntimeGeneratorTask extends BaseGeneratorTasks {
    public static final String RUNTIME_CLASS_NAME = "WaitersRuntime";

    private final String engineInternalClassDir;
    private final String engineInternalPackageName;
    private final String fileHeader;

    public RulesEngineRuntimeGeneratorTask(GeneratorTaskParams generatorTaskParams) {
        super(generatorTaskParams);
        this.engineInternalClassDir = generatorTaskParams.getPathProvider().getEndpointRulesInternalDirectory();
        this.engineInternalPackageName = generatorTaskParams.getModel().getMetadata().getFullInternalEndpointRulesPackageName();
        this.fileHeader = generatorTaskParams.getModel().getFileHeader();
    }

    @Override
    protected List<GeneratorTask> createTasks() throws Exception {
        List<GeneratorTask> copyTasks = new ArrayList<>();

        for (String path : rulesEngineFilePaths()) {
            String[] pathComponents = path.split("/");
            String newFileName = StringUtils.replace(pathComponents[pathComponents.length - 1], ".resource", "");
            copyTasks.add(new SimpleGeneratorTask(engineInternalClassDir,
                                                  newFileName,
                                                  fileHeader,
                                                  () -> rulesEngineFileContent("/" + path)));
        }

        return copyTasks;
    }

    private List<String> rulesEngineFilePaths() {
        URL currentJarUrl = RulesEngineRuntimeGeneratorTask.class.getProtectionDomain().getCodeSource().getLocation();
        try (JarFile jarFile = new JarFile(currentJarUrl.getFile())) {
            return jarFile.stream()
                          .map(ZipEntry::getName)
                          .filter(e -> e.startsWith("software/amazon/awssdk/codegen/rules"))
                          .filter(e -> e.endsWith(".java.resource"))
                          .collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String rulesEngineFileContent(String path) {
        return "package " + engineInternalPackageName + ";\n" +
               "\n"
               + loadResourceAsString(path);
    }

    private String loadResourceAsString(String path) {
        try {
            return IoUtils.toUtf8String(loadResource(path));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private InputStream loadResource(String name) {
        InputStream resourceAsStream = RulesEngineRuntimeGeneratorTask.class.getResourceAsStream(name);
        Validate.notNull(resourceAsStream, "Failed to load resource from %s", name);
        return resourceAsStream;
    }
}
