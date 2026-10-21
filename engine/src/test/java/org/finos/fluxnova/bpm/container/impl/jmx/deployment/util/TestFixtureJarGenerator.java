/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.finos.fluxnova.bpm.container.impl.jmx.deployment.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * Generates test fixture JAR files at test time from existing directory structures.
 * This replaces pre-committed JAR files to comply with OpenSSF Baseline control OSPS-QA-05.01.
 */
public class TestFixtureJarGenerator {

  private static final String RESOURCE_BASE = "src/test/resources/org/finos/fluxnova/bpm/container/impl/jmx/deployment/util/";

  private static final String[][] JAR_MAPPINGS = {
      {"ClassPathScannerTest.testScanClassPathWithFiles", "ClassPathScannerTest.testScanClassPath.jar"},
      {"ClassPathScannerTest.testScanClassPathWithFilesRecursive", "ClassPathScannerTest.testScanClassPathRecursive.jar"},
      {"ClassPathScannerTest.testScanClassPathWithFilesRecursiveTwoDirectories", "ClassPathScannerTest.testScanClassPathRecursiveTwoDirectories.jar"}
  };

  private static volatile boolean initialized = false;

  public static synchronized void ensureTestJarsExist() throws IOException {
    if (initialized) {
      return;
    }

    Path resourceBase = Path.of(RESOURCE_BASE);

    for (String[] mapping : JAR_MAPPINGS) {
      String sourceDir = mapping[0];
      String targetJar = mapping[1];

      Path sourcePath = resourceBase.resolve(sourceDir);
      Path targetPath = resourceBase.resolve(targetJar);

      if (Files.exists(sourcePath) && Files.isDirectory(sourcePath)) {
        createJarFromDirectory(sourcePath, targetPath);
      }
    }

    initialized = true;
  }

  private static void createJarFromDirectory(Path sourceDir, Path targetJar) throws IOException {
    try (OutputStream fos = Files.newOutputStream(targetJar);
         JarOutputStream jos = new JarOutputStream(fos)) {

      Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
          if (!dir.equals(sourceDir)) {
            String entryName = sourceDir.relativize(dir).toString().replace('\\', '/') + "/";
            JarEntry entry = new JarEntry(entryName);
            jos.putNextEntry(entry);
            jos.closeEntry();
          }
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          String entryName = sourceDir.relativize(file).toString().replace('\\', '/');
          JarEntry entry = new JarEntry(entryName);
          jos.putNextEntry(entry);

          try (InputStream is = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) > 0) {
              jos.write(buffer, 0, len);
            }
          }

          jos.closeEntry();
          return FileVisitResult.CONTINUE;
        }
      });
    }
  }
}
