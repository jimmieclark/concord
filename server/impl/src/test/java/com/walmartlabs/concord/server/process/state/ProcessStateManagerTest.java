package com.walmartlabs.concord.server.process.state;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2018 Walmart Inc.
 * -----
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
 * =====
 */


import com.google.common.base.Charsets;
import com.walmartlabs.concord.server.AbstractDaoTest;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.UUID;

import static com.walmartlabs.concord.server.process.state.ProcessStateManager.copyTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Ignore("requires a local DB instance")
public class ProcessStateManagerTest extends AbstractDaoTest {

    @Test
    public void testUpdateState() throws Exception {
        UUID instanceId = UUID.randomUUID();
        Path baseDir = Files.createTempDirectory("testImport");

        writeTempFile(baseDir.resolve("file-1"), "123".getBytes());
        writeTempFile(baseDir.resolve("file-2"), "456".getBytes());

        // ---
        ProcessStateManager stateManager = new ProcessStateManagerImpl(getConfiguration());
        stateManager.importPath(instanceId, null, baseDir);

        Path tmpDir = Files.createTempDirectory("testExport");

        boolean result = stateManager.export(instanceId, copyTo(tmpDir));
        assertTrue(result);
        assertFileContent("123", tmpDir.resolve("file-1"));
        assertFileContent("456", tmpDir.resolve("file-2"));

        // --- update

        writeTempFile(baseDir.resolve("file-1"), "123-up".getBytes());

        stateManager.importPath(instanceId, null, baseDir);

        result = stateManager.export(instanceId, copyTo(tmpDir));
        assertTrue(result);
        assertFileContent("123-up", tmpDir.resolve("file-1"));
        assertFileContent("456", tmpDir.resolve("file-2"));
    }

    @Ignore
    @Test
    public void testLargeImport() throws Exception {
        int files = 100;
        int chunkSize = 1024 * 1024;
        int fileSize = 10 * chunkSize;

        byte[] ab = new byte[chunkSize];
        Arrays.fill(ab, (byte) 0);

        Path baseDir = Files.createTempDirectory("test");
        for (int i = 0; i < files; i++) {
            Path p = baseDir.resolve("file" + i);
            try (OutputStream out = Files.newOutputStream(p, StandardOpenOption.CREATE)) {
                for (int j = 0; j < fileSize; j += chunkSize) {
                    out.write(ab);
                }
            }
        }

        ProcessStateManager stateManager = new ProcessStateManagerImpl(getConfiguration());
        stateManager.importPath(UUID.randomUUID(), "/", baseDir);
    }

    private static void assertFileContent(String expected, Path f) throws IOException {
        String str = com.google.common.io.Files.toString(f.toFile(), Charsets.UTF_8);
        assertEquals(expected, str);
    }

    private static void writeTempFile(Path p, byte[] ab) throws IOException {
        try (OutputStream out = Files.newOutputStream(p, StandardOpenOption.CREATE)) {
            out.write(ab);
        }
    }
}
