/*
 * Copyright 2012 Amadeus s.a.s.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ariatemplates.atjstestrunner.junit;

import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskNode {
    public Integer taskId;
    public String name;
    public TaskNode[] subTasks;

    private static final Pattern specialChars = Pattern.compile("[^._a-zA-Z0-9]+");

    public String getFilteredName() {
        String res = name;
        if (res != null) {
            res = specialChars.matcher(res).replaceAll("_");
        } else {
            res = "Unknown_test";
        }
        return res;
    }
}
