package com.walmartlabs.concord.server;

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

import java.util.List;

public final class Utils {

    public static String[] toArray(List<String> l) {
        if (l == null) {
            return null;
        }
        return l.toArray(new String[0]);
    }

    public static String[] toString(Enum<?>... e) {
        String[] as = new String[e.length];
        for (int i = 0; i < e.length; i++) {
            as[i] = e[i].toString();
        }
        return as;
    }

    public static String getEnv(String key, String defaultValue) {
        String s = System.getenv(key);
        if (s == null) {
            return defaultValue;
        }
        return s;
    }

    private Utils() {
    }
}
