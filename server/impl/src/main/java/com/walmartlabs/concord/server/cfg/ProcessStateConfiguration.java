package com.walmartlabs.concord.server.cfg;

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

import com.walmartlabs.ollie.config.Config;
import org.eclipse.sisu.Nullable;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Named
@Singleton
public class ProcessStateConfiguration implements Serializable {

    @Inject
    @Config("process.cleanupInterval")
    private long cleanupInterval;

    @Inject
    @Config("process.queueCleanup")
    private boolean queueCleanup;

    @Inject
    @Config("process.stateCleanup")
    private boolean stateCleanup;

    @Inject
    @Config("process.eventsCleanup")
    private boolean eventsCleanup;

    @Inject
    @Config("process.eventStatsCleanup")
    private boolean eventStatsCleanup;

    @Inject
    @Config("process.logsCleanup")
    private boolean logsCleanup;

    @Inject
    @Config("process.checkpointCleanup")
    private boolean checkpointCleanup;

    @Inject
    @Config("process.maxStateAge")
    private long maxStateAge;
    @Inject
    @Config("process.secureFiles")
    private List<String> secureFiles;

    @Inject
    @Config("process.signingKeyAlgorithm")
    @Nullable
    private String signingKeyAlgorithm;

    @Inject
    @Config("process.signingAlgorithm")
    @Nullable
    private String signingAlgorithm;

    private Path signingKeyPath;

    @Inject
    public ProcessStateConfiguration(@Config("process.signingKeyPath") @Nullable String signingKeyPath) {
        this.signingKeyPath = signingKeyPath != null ? Paths.get(signingKeyPath) : null;
    }

    public ProcessStateConfiguration(long maxStateAge, List<String> secureFiles) {
        this.maxStateAge = maxStateAge;
        this.secureFiles = secureFiles;
    }

    public long getCleanupInterval() {
        return cleanupInterval;
    }

    public boolean isQueueCleanup() {
        return queueCleanup;
    }

    public boolean isStateCleanup() {
        return stateCleanup;
    }

    public boolean isEventsCleanup() {
        return eventsCleanup;
    }

    public boolean isEventStatsCleanup() {
        return eventStatsCleanup;
    }

    public boolean isLogsCleanup() {
        return logsCleanup;
    }

    public boolean isCheckpointCleanup() {
        return checkpointCleanup;
    }

    public long getMaxStateAge() {
        return maxStateAge;
    }

    public List<String> getSecureFiles() {
        return secureFiles;
    }

    public String getSigningAlgorithm() {
        return signingAlgorithm;
    }

    public String getSigningKeyAlgorithm() {
        return signingKeyAlgorithm;
    }

    public Path getSigningKeyPath() {
        return signingKeyPath;
    }
}
