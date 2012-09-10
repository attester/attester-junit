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

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;

import com.fasterxml.jackson.databind.JsonNode;

public class Attester extends Runner {
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface ConfigFile {
        String value();
    }

    private Description rootTestDescription;
    private Map<Integer, Description> testDescriptions = new HashMap<Integer, Description>();
    private Set<String> testNames = new HashSet<String>();
    private boolean receivedTestsList = false;
    private boolean allTestsFinished = false;

    private AttesterProcess nodeProcess;
    private Collection<Process> browserProcesses = new Vector<Process>();
    private RunNotifier runNotifier;

    private Map<String, MessageHandler> messageHandlers = new HashMap<String, MessageHandler>();
    {
        messageHandlers.put("tasksList", new MessageHandler() {
            public void handleMessage(JsonNode message) {
                TaskNode[] tests = treeToValue(message.get("tasks"), TaskNode[].class);
                convertTestNodesArray(rootTestDescription, tests);
                receivedTestsList = true;
            }
        });
        messageHandlers.put("campaignFinished", new MessageHandler() {
            public void handleMessage(JsonNode message) {
                allTestsFinished = true;
            }
        });
        messageHandlers.put("testStarted", new MessageHandler() {
            @Override
            public void handleMessage(JsonNode message) {

            }
        });
        messageHandlers.put("testFinished", new MessageHandler() {
            @Override
            public void handleMessage(JsonNode message) {

            }
        });
        messageHandlers.put("taskStarted", new MessageHandler() {
            public void handleMessage(JsonNode message) {
                Description test = getCorrespondingTest(message, testDescriptions);
                runNotifier.fireTestStarted(test);
            }
        });
        messageHandlers.put("taskFinished", new MessageHandler() {
            public void handleMessage(JsonNode message) {
                Description test = getCorrespondingTest(message, testDescriptions);
                runNotifier.fireTestFinished(test);
            }
        });
        messageHandlers.put("taskIgnored", new MessageHandler() {
            public void handleMessage(JsonNode message) {
                Description test = getCorrespondingTest(message, testDescriptions);
                runNotifier.fireTestIgnored(test);
            }
        });
        messageHandlers.put("error", new MessageHandler() {
            public void handleMessage(JsonNode message) {
                Description test = getCorrespondingTest(message, testDescriptions);
                TestError error = treeToValue(message.get("error"), TestError.class);
                Throwable javaException = TestErrorJavaException.createFromTestFailure(error);
                runNotifier.fireTestFailure(new Failure(test, javaException));
            }
        });
        messageHandlers.put("serverAttached", new MessageHandler() {
            @Override
            public void handleMessage(JsonNode message) {
                // Start the browser
                String[] phantomJSCmd = treeToValue(message.get("phantomJS"), String[].class);

                for (int i = 0; i < 15; i++) {
                    Process browserProcess = ExternalProcess.phantomjs.run(phantomJSCmd);
                    StreamRedirector.redirectStream(browserProcess.getInputStream(), System.out);
                    StreamRedirector.redirectStream(browserProcess.getErrorStream(), System.err);
                    browserProcesses.add(browserProcess);
                }
            }
        });
    }

    private List<String> cmdLine = new Vector<String>();

    private static String getConfigFile(Class<?> testClass) {
        ConfigFile configFileAnnotation = testClass.getAnnotation(ConfigFile.class);
        if (configFileAnnotation == null) {
            return null;
        }
        File configFile = new File(configFileAnnotation.value());
        if (!configFile.exists()) {
            throw new RuntimeException(new FileNotFoundException(configFile.getAbsolutePath()));
        }
        return configFileAnnotation.value();
    }

    public Attester(Class<?> testClass) {
        this(testClass.getName(), getConfigFile(testClass));
    }

    public Attester(String rootTestName, String configFile) {
        rootTestDescription = Description.createSuiteDescription(rootTestName);
        cmdLine.add("--no-colors");
        cmdLine.add(configFile);
    }

    @Override
    public Description getDescription() {
        if (!receivedTestsList) {
            checkNodeStarted();
            while (!receivedTestsList) {
                readAndProcessMessage();
            }
        }
        return rootTestDescription;
    }

    @Override
    public void run(RunNotifier notifier) {
        if (runNotifier != null) {
            throw new IllegalStateException();
        }
        runNotifier = notifier;
        try {
            checkNodeStarted();
            while (!allTestsFinished) {
                readAndProcessMessage();
            }
        } finally {
            runNotifier = null;
        }
    }

    private void checkNodeStarted() {
        if (nodeProcess != null) {
            return;
        }
        nodeProcess = new AttesterProcess(cmdLine.toArray(new String[] {}));
    }

    private void readAndProcessMessage() {
        JsonNode message = nodeProcess.readMessage();
        String type = message.get("event").asText();
        MessageHandler handler = messageHandlers.get(type);
        if (handler != null) {
            handler.handleMessage(message);
        }
    }

    private void convertTestNodesArray(Description parent, TaskNode[] tests) {
        for (TaskNode testNode : tests) {
            Description currentNode = convertTestNode(testNode);
            parent.addChild(currentNode);
        }
    }

    private Description convertTestNode(TaskNode testNode) {
        String initialName = testNode.getFilteredName();
        String name = initialName;
        int i = 2;
        while (!testNames.add(name)) {
            name = initialName + i;
            i++;
        }
        Description res = Description.createSuiteDescription(name);
        if (testNode.taskId != null) {
            Description previousValue = testDescriptions.put(testNode.taskId, res);
            if (previousValue != null) {
                throw new RuntimeException("Received several tests with the same id: " + testNode.taskId);
            }
        }
        if (testNode.subTasks != null) {
            convertTestNodesArray(res, testNode.subTasks);
        }
        return res;
    }
}
