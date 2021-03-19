/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.kie.maven.plugin;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.drools.modelcompiler.CanonicalKieModule;
import org.drools.modelcompiler.builder.ModelWriter;
import org.kie.api.builder.ReleaseId;

@Mojo(name = "generateModel-post-compile",
      defaultPhase = LifecyclePhase.PROCESS_CLASSES,
      threadSafe = true,
      requiresDependencyResolution = ResolutionScope.NONE)
public class GenerateModelPostCompileMojo extends AbstractMojo {

    @Parameter(required = true, defaultValue = "${project.build.outputDirectory}")
    private File outputDirectory;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            getLog().info("GenerateModelPostCompileMojo!!");
            Object value1 = project.getContextValue(GenerateModelMojo.GENERATED_FILES);
            if (value1 == null) {
                return;
            }
            List<String> generatedFiles = (List<String>) value1;

            Object value2 = project.getContextValue(GenerateModelMojo.RELEASE_ID);
            if (value2 == null) {
                return;
            }
            ReleaseId releaseId = (ReleaseId) value2;

            String outputDirectoryPath = outputDirectory.getPath();
            getLog().info("outputDirectoryPath : " + outputDirectoryPath);
            Set<String> generatedClassNames = new HashSet<>();

            getLog().info("generatedFiles : " + generatedFiles);

            for (String generatedFile : generatedFiles) {
                getLog().info("generatedFile : " + generatedFile);
                String className = convertInternalResourcePathToClassName(generatedFile);
                getLog().info("className : " + className);
                String resourcePathStr = convertClassNameToPhysicalResourcePath(className);
                getLog().info("resourcePathStr : " + resourcePathStr);
                Path fullResourcePath = Paths.get(outputDirectoryPath, resourcePathStr);
                getLog().info("fullResourcePath : " + fullResourcePath.toString());
                if (Files.exists(fullResourcePath)) {
                    getLog().info("adding : " + className);
                    generatedClassNames.add(className);
                    String packageStr = className.substring(0, className.lastIndexOf("."));
                    getLog().info("packageStr : " + packageStr);
                    // inner classes
                    String packagePathStr = resourcePathStr.substring(0, resourcePathStr.lastIndexOf(File.separatorChar));
                    getLog().info("packagePathStr : " + packagePathStr);
                    Path fullPackagePath = Paths.get(outputDirectoryPath, packagePathStr);
                    getLog().info("fullPackagePath : " + fullPackagePath.toString());
                    String simpleClassName = className.substring(className.lastIndexOf(".") + 1);
                    getLog().info("simpleClassName : " + simpleClassName);
                    try (final Stream<Path> files = Files.list(fullPackagePath)) {
                        files.filter(p -> {
                            String fileName = p.getFileName().toString();
                            getLog().info(" fileName -> " + fileName);
                            if (!fileName.endsWith(".class")) {
                                return false;
                            }
                            return fileName.startsWith(simpleClassName + "$");
                        }).forEach(p -> {
                            String innerClassName = packageStr + "." + stripExtension(p.getFileName().toString());
                            getLog().info("   innerClassName -> " + innerClassName);
                            generatedClassNames.add(innerClassName);
                        });
                    } catch (IOException e) {
                        throw e;
                    }
                }
            }
            getLog().info("generatedClassNames : " + generatedClassNames);

            // write a metadata file "generated-class-names"
            String content = ModelWriter.generatedClassNamesFileContent(generatedClassNames);
            String genClassNamesFilePathStr = CanonicalKieModule.getGeneratedClassNamesFile(releaseId);
            Path genClassNamesDestinationPath = Paths.get(outputDirectoryPath, convertInternalResourcePathToPhysicalResourcePath(genClassNamesFilePathStr));

            if (!Files.exists(genClassNamesDestinationPath.getParent())) {
                Files.createDirectories(genClassNamesDestinationPath.getParent());
            }
            Files.copy(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), genClassNamesDestinationPath, StandardCopyOption.REPLACE_EXISTING);

            getLog().info(genClassNamesFilePathStr + " is added");
        } catch (Exception e) {
            getLog().warn("Failed to produce generated-class-names file. But it's not critical so you can still use the created kjar", e);
        }
    }

    public static String convertInternalResourcePathToClassName(String resourcePath) {
        // in case of 'internal', file separator is '/'
        if (resourcePath.startsWith("./")) {
            resourcePath = resourcePath.substring(2);
        }
        return stripExtension(resourcePath).replace('/', '.');
    }

    public static String convertInternalResourcePathToPhysicalResourcePath(String resourcePath) {
        return resourcePath.replace('/', File.separatorChar);
    }

    public static String convertPhysicalResourcePathToClassName(String resourcePath) {
        // in case of 'physical', file separator is File.separator
        if (resourcePath.startsWith("." + File.separator)) {
            resourcePath = resourcePath.substring(2);
        }
        return stripExtension(resourcePath).replace(File.separatorChar, '.');
    }

    public static String convertClassNameToPhysicalResourcePath(String className) {
        return className.replace('.', File.separatorChar) + ".class";
    }

    public static String stripExtension(final String resourcePath) {
        final int i = resourcePath.lastIndexOf('.');
        return i == -1 ? resourcePath : resourcePath.substring(0, i);
    }
}
