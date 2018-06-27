package com.walmartlabs.concord.server.process.keys;

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


import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * @param <K> type of a key
 */
public final class KeyIndex<K extends Key<?>> implements Serializable {

    private final Map<String, K> keys = new HashMap<>();
    private final BiFunction<String, Class<?>, K> keyMaker;

    public KeyIndex(BiFunction<String, Class<?>, K> keyMaker) {
        this.keyMaker = keyMaker;
    }

    public K register(String name, Class<?> type) {
        synchronized (keys) {
            if (keys.containsKey(name)) {
                throw new IllegalStateException("Key '" + name + "' is already registered. " +
                        "Check for duplicate declarations in the code");
            }

            K k = keyMaker.apply(name, type);
            keys.put(name, k);
            return k;
        }
    }
}
