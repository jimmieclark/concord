package com.walmartlabs.concord.server.api.org.secret;

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


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import java.io.Serializable;
import java.util.UUID;

@JsonInclude(Include.NON_NULL)
public class UsernamePasswordResponse implements Serializable {

    private final UUID id;
    private final Value value;

    public UsernamePasswordResponse(UUID id, Value value) {
        this.id = id;
        this.value = value;
    }

    public UUID getId() {
        return id;
    }

    public Value getValue() {
        return value;
    }

    @JsonInclude(Include.NON_NULL)
    public static class Value implements Serializable {

        private final String username;
        private final String password;

        public Value(String username, String password) {
            this.username = username;
            this.password = password;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }
    }
}
