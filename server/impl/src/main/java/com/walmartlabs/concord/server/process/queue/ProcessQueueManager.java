package com.walmartlabs.concord.server.process.queue;

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

import com.walmartlabs.concord.sdk.Constants;
import com.walmartlabs.concord.sdk.EventType;
import com.walmartlabs.concord.server.ConcordObjectMapper;
import com.walmartlabs.concord.server.RequestId;
import com.walmartlabs.concord.server.process.*;
import com.walmartlabs.concord.server.process.event.ProcessEventDao;
import com.walmartlabs.concord.server.queueclient.message.Imports;
import com.walmartlabs.concord.server.sdk.ProcessStatus;
import org.jooq.DSLContext;

import javax.inject.Inject;
import javax.inject.Named;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Named
public class ProcessQueueManager {

    private final ProcessQueueDao queueDao;
    private final ProcessEventDao eventDao;
    private final ConcordObjectMapper objectMapper;

    @Inject
    public ProcessQueueManager(ProcessQueueDao queueDao,
                               ProcessEventDao eventDao,
                               ConcordObjectMapper objectMapper) {

        this.queueDao = queueDao;
        this.eventDao = eventDao;
        this.objectMapper = objectMapper;
    }

    /**
     * Creates the initial queue record for the specified process payload.
     */
    public void insert(Payload payload, ProcessStatus status) {
        ProcessKey processKey = payload.getProcessKey();
        ProcessKind kind = payload.getHeader(Payload.PROCESS_KIND, ProcessKind.DEFAULT);
        UUID projectId = payload.getHeader(Payload.PROJECT_ID);
        UUID repoId = payload.getHeader(Payload.REPOSITORY_ID);
        UUID parentInstanceId = payload.getHeader(Payload.PARENT_INSTANCE_ID);
        UUID initiatorId = payload.getHeader(Payload.INITIATOR_ID);
        Map<String, Object> cfg = getCfg(payload);
        Map<String, Object> meta = getMeta(cfg);
        TriggeredByEntry triggeredBy = payload.getHeader(Payload.TRIGGERED_BY);

        queueDao.tx(tx -> {
            queueDao.insert(tx, processKey, status, kind, parentInstanceId, projectId, repoId, initiatorId, meta, triggeredBy);
            eventDao.insertStatusHistory(tx, processKey, status, Collections.emptyMap());
        });
    }

    /**
     * Updates the existing record, moving the process into the ENQUEUED status.
     */
    public void enqueue(Payload payload) {
        ProcessKey processKey = payload.getProcessKey();

        ProcessStatus s = queueDao.getStatus(processKey.getInstanceId());
        if (s == null) {
            throw new ProcessException(processKey, "Process not found: " + processKey);
        }

        if (s != ProcessStatus.PREPARING && s != ProcessStatus.RESUMING && s != ProcessStatus.SUSPENDED) {
            throw new ProcessException(processKey, "Invalid process status: " + s);
        }

        Set<String> tags = payload.getHeader(Payload.PROCESS_TAGS);
        Instant startAt = PayloadUtils.getStartAt(payload);
        Map<String, Object> requirements = PayloadUtils.getRequirements(payload);
        Long processTimeout = getProcessTimeout(payload);
        Set<String> handlers = payload.getHeader(Payload.PROCESS_HANDLERS);
        Map<String, Object> meta = getMeta(getCfg(payload));
        Imports imports = payload.getHeader(Payload.IMPORTS);
        Map<String, Object> exclusive = PayloadUtils.getExclusive(payload);

        queueDao.tx(tx -> {
            queueDao.enqueue(tx, processKey, tags, startAt, requirements, processTimeout, handlers, meta, imports, exclusive);
            eventDao.insertStatusHistory(tx, processKey, ProcessStatus.ENQUEUED, Collections.emptyMap());
        });
    }

    /**
     * @see #updateStatus(ProcessKey, ProcessStatus, Map)
     */
    public void updateStatus(ProcessKey processKey, ProcessStatus status) {
        updateStatus(processKey, status, Collections.emptyMap());
    }

    /**
     * Updates the process' status. Adds a process status history event with an optional {@code statusPayload}.
     */
    public void updateStatus(ProcessKey processKey, ProcessStatus status, Map<String, Object> statusPayload) {
        queueDao.tx(tx -> {
            queueDao.updateStatus(tx, processKey, status);
            eventDao.insertStatusHistory(tx, processKey, status, statusPayload);
        });
    }

    /**
     * Updates the process' status but only if it's in the {@code expected} status.
     *
     * @return {@code true} if the process was updated
     */
    public boolean updateExpectedStatus(ProcessKey processKey, ProcessStatus expected, ProcessStatus status) {
        return queueDao.txResult(tx -> {
            boolean success = queueDao.updateStatus(tx, processKey, expected, status);
            eventDao.insertStatusHistory(tx, processKey, status, Collections.emptyMap());
            return success;
        });
    }

    /**
     * Updates status of multiple processes but only if their current status is
     * in the {@code expected} list of statuses.
     *
     * @return {@code true} if every processes was updated
     */
    public boolean updateExpectedStatus(List<ProcessKey> processKeys, List<ProcessStatus> expected, ProcessStatus status) {
        return queueDao.txResult(tx -> {
            boolean success = queueDao.updateStatus(processKeys, expected, status);
            eventDao.insertStatusHistory(tx, processKeys, status);
            return success;
        });
    }

    /**
     * @see #updateAgentId(DSLContext, ProcessKey, String, ProcessStatus)
     */
    public void updateAgentId(ProcessKey processKey, String agentId, ProcessStatus status) {
        queueDao.tx(tx -> updateAgentId(tx, processKey, agentId, status));
    }

    /**
     * Updates the process' agent ID and status.
     */
    public void updateAgentId(DSLContext tx, ProcessKey processKey, String agentId, ProcessStatus status) {
        queueDao.updateAgentId(tx, processKey, agentId, status);
        eventDao.insertStatusHistory(tx, processKey, status, Collections.emptyMap());
    }

    /**
     * @see #updateWait(DSLContext, ProcessKey, AbstractWaitCondition)
     */
    public void updateWait(ProcessKey key, AbstractWaitCondition wait) {
        queueDao.tx(tx -> updateWait(tx, key, wait));
    }

    /**
     * Updates the process' wait conditions. Adds a wait condition history event.
     */
    public void updateWait(DSLContext tx, ProcessKey key, AbstractWaitCondition wait) {
        queueDao.updateWait(tx, key, wait);

        Map<String, Object> eventData = objectMapper.convertToMap(wait != null ? wait : new NoneCondition());
        eventDao.insert(tx, key, EventType.PROCESS_WAIT.name(), null, eventData);
    }

    /**
     * @see #updateStatus(ProcessKey, ProcessStatus, Map)
     */
    public void updateExclusive(DSLContext tx, ProcessKey processKey, Map<String, Object> exclusive) {
        queueDao.updateExclusive(tx, processKey, exclusive);
    }

    private static Map<String, Object> getCfg(Payload payload) {
        return payload.getHeader(Payload.CONFIGURATION, Collections.emptyMap());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> getMeta(Map<String, Object> cfg) {
        Map<String, Object> m = (Map<String, Object>) cfg.get(Constants.Request.META);
        if (m == null) {
            m = Collections.emptyMap();
        }

        m = new HashMap<>(m);
        m.put(Constants.Meta.SYSTEM_GROUP, Collections.singletonMap(Constants.Meta.REQUEST_ID, RequestId.get()));

        return m;
    }

    @SuppressWarnings("unchecked")
    private static Long getProcessTimeout(Payload p) {
        Map<String, Object> cfg = p.getHeader(Payload.CONFIGURATION);
        if (cfg == null) {
            return null;
        }

        Object processTimeout = cfg.get(Constants.Request.PROCESS_TIMEOUT);
        if (processTimeout == null) {
            return null;
        }

        if (processTimeout instanceof String) {
            Duration duration = Duration.parse((CharSequence) processTimeout);
            return duration.get(ChronoUnit.SECONDS);
        }

        if (processTimeout instanceof Number) {
            return ((Number) processTimeout).longValue();
        }

        throw new IllegalArgumentException("Invalid '" + Constants.Request.PROCESS_TIMEOUT + "' value: expected an ISO-8601 value, got: " + processTimeout);
    }
}
