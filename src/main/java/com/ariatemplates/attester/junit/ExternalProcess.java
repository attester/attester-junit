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

package com.ariatemplates.attester.junit;

import java.io.IOException;

public class ExternalProcess {
    private static final String START_FAILED = "Failed to start %s.\nThe following executable was used: %s\nYou can configure the path to the executable by setting the following Java system property: %s";

    private final String executableName;
    private final String sysProperty;

    public ExternalProcess(String executableName, String sysProperty) {
        this.executableName = executableName;
        this.sysProperty = sysProperty;
    }

    public Process run(String... cmd) {
        Runtime runtime = Runtime.getRuntime();
        String[] args = new String[cmd.length + 1];
        System.arraycopy(cmd, 0, args, 1, cmd.length);
        args[0] = System.getProperty(sysProperty, executableName);

        try {
            return runtime.exec(args);
        } catch (IOException e) {
            throw new ExternalProcessException(String.format(START_FAILED, executableName, args[0], sysProperty), e);
        }
    }

    public static final ExternalProcess node = new ExternalProcess("node", "org.nodejs.node.path");
    public static final ExternalProcess phantomjs = new ExternalProcess("phantomjs", "com.google.code.phantomjs.path");
}
