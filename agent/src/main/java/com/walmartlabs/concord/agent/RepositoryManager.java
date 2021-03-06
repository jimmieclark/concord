package com.walmartlabs.concord.agent;

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

import com.walmartlabs.concord.client.SecretClient;
import com.walmartlabs.concord.repository.*;
import com.walmartlabs.concord.sdk.Secret;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static com.walmartlabs.concord.project.model.Import.SecretDefinition;

public class RepositoryManager {

    private final RepositoryProviders providers;
    private final SecretClient secretClient;
    private final Path cacheDir;

    public RepositoryManager(Configuration cfg, SecretClient secretClient) {
        GitClientConfiguration gitCfg = GitClientConfiguration.builder()
                .oauthToken(cfg.getRepositoryOauthToken())
                .shallowClone(cfg.isShallowClone())
                .httpLowSpeedLimit(cfg.getRepositoryHttpLowSpeedLimit())
                .httpLowSpeedTime(cfg.getRepositoryHttpLowSpeedTime())
                .sshTimeout(cfg.getRepositorySshTimeout())
                .sshTimeoutRetryCount(cfg.getRepositorySshTimeoutRetryCount())
                .build();

        List<RepositoryProvider> providers = Collections.singletonList(new GitCliRepositoryProvider(gitCfg));
        this.providers = new RepositoryProviders(providers, cfg.getRepositoryLockTimeout());

        this.secretClient = secretClient;
        this.cacheDir = cfg.getRepositoryCacheDir();
    }

    public void export(String repoUrl, String commitId, String repoPath, Path dest, SecretDefinition secretDefinition) throws ExecutionException {
        export(repoUrl, null, commitId, repoPath, dest, secretDefinition);
    }

    public void export(String repoUrl, String branch, String commitId, String repoPath, Path dest, SecretDefinition secretDefinition) throws ExecutionException {
        Secret secret = getSecret(secretDefinition);

        providers.withLock(repoUrl, () -> {
            Repository repo = providers.fetch(repoUrl, branch, commitId, repoPath, secret, cacheDir);
            repo.export(dest);
            return null;
        });
    }

    private Secret getSecret(SecretDefinition secret) throws ExecutionException {
        if (secret == null) {
            return null;
        }

        try {
            return secretClient.getData(secret.org(), secret.name(), secret.password(), null);
        } catch (Exception e) {
            throw new ExecutionException("Error while retrieving a secret '" + secret.name() + "' in org '" + secret.org() + "': " + e.getMessage(), e);
        }
    }
}
