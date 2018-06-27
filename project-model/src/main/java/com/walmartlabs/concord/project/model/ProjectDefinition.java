package com.walmartlabs.concord.project.model;

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


import io.takari.bpm.model.ProcessDefinition;
import io.takari.bpm.model.form.FormDefinition;

import java.util.List;
import java.util.Map;

public class ProjectDefinition extends Profile {

    private final Map<String, Profile> profiles;

    private final List<Trigger> triggers;

    public ProjectDefinition(Map<String, ProcessDefinition> flows,
                             Map<String, FormDefinition> forms,
                             Map<String, Object> variables,
                             Map<String, Profile> profiles,
                             List<Trigger> triggers) {

        super(flows, forms, variables);
        this.profiles = profiles;
        this.triggers = triggers;
    }

    public Map<String, Profile> getProfiles() {
        return profiles;
    }

    public List<Trigger> getTriggers() {
        return triggers;
    }

    @Override
    public String toString() {
        return "ProjectDefinition{" +
                "profiles=" + profiles +
                "triggers=" + triggers +
                "} " + super.toString();
    }
}
