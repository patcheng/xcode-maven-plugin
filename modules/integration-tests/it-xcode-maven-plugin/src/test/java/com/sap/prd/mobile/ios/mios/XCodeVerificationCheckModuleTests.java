/*
 * #%L
 * it-xcode-maven-plugin
 * %%
 * Copyright (C) 2012 SAP AG
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.sap.prd.mobile.ios.mios;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Model;
import org.junit.Test;

public class XCodeVerificationCheckModuleTests extends XCodeTest
{

  @Test
  public void testModuleBuild() throws Exception {

    final String testName = getTestName();

    final String dynamicVersion = "1.0." + String.valueOf(System.currentTimeMillis());

    final File remoteRepositoryDirectory = getRemoteRepositoryDirectory(getClass().getName());

    Properties pomReplacements = new Properties();
    pomReplacements.setProperty(PROP_NAME_DEPLOY_REPO_DIR, remoteRepositoryDirectory.getAbsolutePath());
    pomReplacements.setProperty(PROP_NAME_DYNAMIC_VERSION, dynamicVersion);
    
    final List<String> additionalSystemProperties = Arrays.asList("-Dxcode.verification.checks.skip=false", "-Dxcode.verification.checks.definitionFile=file:./checkDefinitions.xml");

    test(null, testName, new File(getTestRootDirectory(), "moduleBuild"),
          Arrays.asList("clean", "install", getMavenXcodePluginGroupId() + ":" + getMavenXcodePluginArtifactId() + ":" + getMavenXcodePluginVersion() + ":" + "verification-check"),
          additionalSystemProperties,
          null, pomReplacements, new ChainProjectModifier(new ProjectModifier() {

            @Override
            void execute() throws Exception
            {
              FileUtils.copyDirectory(new File(testExecutionDirectory, "MyApp"), new File(testExecutionDirectory, "MyApp2"));
            }
          }, new AbstractProjectModifier() {

            @Override
            void execute() throws Exception
            {
              final File pom = new File(testExecutionDirectory, "MyApp2/pom.xml");
              Model model = getModel(pom);
              model.setArtifactId("MyApp2");
              persistModel(pom, model);
            }
          }, new AbstractProjectModifier() {

            @Override
            void execute() throws Exception
            {
              final File myAppProjectFile = new File(testExecutionDirectory, "MyApp2/src/xcode/MyApp.xcodeproj");
              FileUtils.copyDirectory(myAppProjectFile, new File(testExecutionDirectory, "MyApp2/src/xcode/MyApp2.xcodeproj"));
              FileUtils.deleteDirectory(myAppProjectFile);
            }
          }, new AbstractProjectModifier() {

            @Override
            void execute() throws Exception
            {
              final File pom = new File(testExecutionDirectory, "pom.xml");
              Model model = getModel(pom);
              List<String> modules = model.getModules();
              modules.add("MyApp2/pom.xml");
              persistModel(pom, model);
            }
          }, new ProjectModifier() {

            @Override
            void execute() throws Exception
            {
              String checks = FileUtils.readFileToString(new File("src/test/resources/checkDefinitions.xml"));
              checks = checks.replaceAll("\\$\\{SERVERITY\\}", "WARNING");
              checks = checks.replaceAll("\\$\\{xcode.maven.plugin.version\\}", getMavenXcodePluginVersion()); //plugin version is not acurate, but does work.
              FileUtils.writeStringToFile(new File(testExecutionDirectory, "checkDefinitions.xml"), checks);
            }
          }));
  }
}
