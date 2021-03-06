package com.walmartlabs.concord.project.yaml.converter;

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

import com.walmartlabs.concord.project.yaml.YamlConverterException;
import com.walmartlabs.concord.project.yaml.model.YamlSetVariablesStep;
import io.takari.bpm.model.ExpressionType;
import io.takari.bpm.model.ServiceTask;
import io.takari.bpm.model.VariableMapping;

import java.util.Collections;
import java.util.Set;

public class YamlSetVariablesStepConverter implements StepConverter<YamlSetVariablesStep> {

    @Override
    public Chunk convert(ConverterContext ctx, YamlSetVariablesStep s) throws YamlConverterException {
        Chunk c = new Chunk();

        String id = ctx.nextId();
        String expression = "${vars.set(execution, __0)}";

        Object vars = StepConverter.deepConvert(s.getVariables());
        Set<VariableMapping> inVars = Collections.singleton(new VariableMapping(null, null, vars, "__0", true));

        c.addElement(new ServiceTask(id, ExpressionType.SIMPLE, expression, inVars, null, true));
        c.addOutput(id);
        c.addSourceMap(id, toSourceMap(s, "Set variables"));

        return c;
    }
}
