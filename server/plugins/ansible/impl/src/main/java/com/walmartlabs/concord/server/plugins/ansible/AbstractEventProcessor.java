package com.walmartlabs.concord.server.plugins.ansible;

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

import com.walmartlabs.concord.server.sdk.ScheduledTask;
import org.jooq.DSLContext;

import java.util.List;

// TODO: move to plugins sdk?
public abstract class AbstractEventProcessor<E extends AbstractEventProcessor.Event> implements ScheduledTask {

    private final String processorName;
    private final EventMarkerDao eventMarkerDao;
    private final int fetchLimit;

    protected AbstractEventProcessor(String processorName, EventMarkerDao eventMarkerDao, int fetchLimit) {
        this.processorName = processorName;
        this.eventMarkerDao = eventMarkerDao;
        this.fetchLimit = fetchLimit;
    }

    @Override
    public void performTask() {
        boolean continueProcess;

        do {
            continueProcess = false;

            List<EventMarkerDao.EventMarker> markers = eventMarkerDao.get(processorName);
            for (EventMarkerDao.EventMarker m : markers) {
                int processedEvents = process(m, fetchLimit);
                if (processedEvents >= fetchLimit) {
                    continueProcess = true;
                }
            }
        } while (continueProcess);

        eventMarkerDao.cleanup(processorName);
    }

    private int process(EventMarkerDao.EventMarker marker, int fetchLimit) {
        return eventMarkerDao.txResult(tx -> {
            List<E> events = processEvents(tx, marker, fetchLimit);
            if (events.isEmpty()) {
                eventMarkerDao.update(tx, processorName, marker.instanceCreatedStart(), marker.maxEventSeq());
                return 0;
            }

            E lastEvent = events.get(events.size() - 1);
            eventMarkerDao.update(tx, processorName, marker.instanceCreatedStart(), lastEvent.eventSeq());

            return events.size();
        });
    }

    protected abstract List<E> processEvents(DSLContext tx, EventMarkerDao.EventMarker marker, int fetchLimit);

    public interface Event {
        long eventSeq();
    }
}
