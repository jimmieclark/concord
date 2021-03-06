package com.walmartlabs.concord.agent;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2019 Walmart Inc.
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

import com.walmartlabs.concord.dependencymanager.DependencyManager;
import com.walmartlabs.concord.imports.ImportManager;
import com.walmartlabs.concord.imports.ImportManagerFactory;
import com.walmartlabs.concord.imports.RepositoryExporter;

import java.nio.file.Path;

public class ImportManagerProvider {

    private final ImportManagerFactory factory;

    public ImportManagerProvider(RepositoryManager repositoryManager, DependencyManager dependencyManager) {
        this.factory = new ImportManagerFactory(dependencyManager, (RepositoryExporter) (entry, workDir) -> {
            Path dst = workDir;
            if (entry.dest() != null) {
                dst = dst.resolve(entry.dest());
            }
            repositoryManager.export(entry.url(), entry.version(), null, entry.path(), dst, entry.secret());
            return null;
        });
    }

    public ImportManager get() {
        return factory.create();
    }
}
