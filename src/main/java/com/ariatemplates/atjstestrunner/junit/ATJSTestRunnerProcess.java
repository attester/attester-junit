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

import java.io.File;
import java.io.FileNotFoundException;

import com.fasterxml.jackson.databind.JsonNode;

public class ATJSTestRunnerProcess {
    public static final String ATJSTESTRUNNER_EXPECTED_VERSION = "1.0";
    public static final String ATJSTESTRUNNER_PATH_SYS_PROPERTY = "com.ariatemplates.atjstestrunner.path";
    private static final String PATH_IN_ATJSTESTRUNNER_DIRECTORY = "bin" + File.separator + "atjstestrunner.js";

    private static final String MISSING_SYS_PROPERTY = "Please define the following Java system property to specify the path to atjstestrunner: "
        + ATJSTESTRUNNER_PATH_SYS_PROPERTY;
    private static final String INVALID_PATH_IN_SYS_PROPERTY = "The path specified in the " + ATJSTESTRUNNER_PATH_SYS_PROPERTY
        + " Java system property is invalid: %s";
    private static final String INCOMPATIBLE_ATJSTESTRUNNER = "The atjstestrunner external program does not behave as expected. Either its version is incompatible or there was an unknown error.";
    private static final String UNEXPECTED_ATJSTESTRUNNER_VERSION = "Warning: using atjstestrunner version %s, which is a different version than expected (%s).";
    private static final String UNEXPECTED_PROCESS_TERMINATION = "The atjstestrunner external program terminated unexpectedly.";

    private Process nodeProcess;
    private JsonInputStream inputMessages;

    protected void finalize() throws Throwable {
        checkProcessEnded();
    };

    private void checkProcessEnded() {
        if (nodeProcess != null) {
            try {
                nodeProcess.exitValue();
            } catch (IllegalThreadStateException e) {
                nodeProcess.destroy();
            }
            nodeProcess = null;
        }
    }

    public ATJSTestRunnerProcess(String... cmd) {
        try {
            String path = System.getProperty(ATJSTESTRUNNER_PATH_SYS_PROPERTY);
            if (path == null) {
                throw new ExternalProcessException(MISSING_SYS_PROPERTY);
            }
            File jsFile = new File(path, PATH_IN_ATJSTESTRUNNER_DIRECTORY);
            String[] newCmd = new String[cmd.length + 2];
            newCmd[0] = jsFile.getAbsolutePath();
            if (!jsFile.exists()) {
                throw new ExternalProcessException(String.format(INVALID_PATH_IN_SYS_PROPERTY, path, new FileNotFoundException(newCmd[0])));
            }
            newCmd[1] = "-j"; // JSON output
            System.arraycopy(cmd, 0, newCmd, 2, cmd.length);
            nodeProcess = ExternalProcess.node.run(newCmd);
            StreamRedirector.redirectStream(nodeProcess.getErrorStream(), System.out);
            inputMessages = new JsonInputStream(nodeProcess.getInputStream());
            checkCompatibility();
        } catch (RuntimeException e) {
            checkProcessEnded();
            throw e;
        }
    }

    private void checkCompatibility() {
        try {
            JsonNode firstMessage = inputMessages.read();
            String application = firstMessage.get("application").asText();
            if (!application.equals("atjstestrunner")) {
                throw new ExternalProcessException(INCOMPATIBLE_ATJSTESTRUNNER);
            }
            String version = firstMessage.get("version").asText();
            if (!version.startsWith(ATJSTESTRUNNER_EXPECTED_VERSION)) {
                System.err.println(String.format(UNEXPECTED_ATJSTESTRUNNER_VERSION, version, ATJSTESTRUNNER_EXPECTED_VERSION));
            }
        } catch (ExternalProcessException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ExternalProcessException(INCOMPATIBLE_ATJSTESTRUNNER, e);
        }
    }

    public JsonNode readMessage() {
        try {
            JsonNode response = inputMessages.read();
            if (response == null) {
                throw new RuntimeException(UNEXPECTED_PROCESS_TERMINATION);
            }
            return response;
        } catch (RuntimeException e) {
            // end the process in case the message is not readable
            checkProcessEnded();
            throw e;
        }
    }
}
