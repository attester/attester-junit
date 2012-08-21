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

import junit.framework.AssertionFailedError;

public abstract class TestErrorJavaException {

    private TestErrorJavaException() {
    }

    private static StackTraceElement[] createJavaStackTrace(TestError testFailure) {
        TestErrorStackElement[] stack = testFailure.stack;
        if (stack == null) {
            stack = new TestErrorStackElement[0];
        }
        StackTraceElement[] javaStack = new StackTraceElement[stack.length];
        for (int i = stack.length - 1; i >= 0; i--) {
            TestErrorStackElement element = stack[i];
            javaStack[i] = new StackTraceElement(element.className, element.function, element.file, element.line);
        }
        return javaStack;
    }

    public static Throwable createFromTestFailure(TestError failure) {
        final TestError testFailure;
        if (failure == null) {
            testFailure = new TestError();
        } else {
            testFailure = failure;
        }
        if (testFailure.failure) {
            return new AssertionFailedError() {
                @Override
                public String toString() {
                    return testFailure.message;
                }

                @Override
                public synchronized Throwable fillInStackTrace() {
                    setStackTrace(createJavaStackTrace(testFailure));
                    return this;
                }

            };
        } else {
            return new Exception() {
                @Override
                public String toString() {
                    return testFailure.message;
                }

                @Override
                public synchronized Throwable fillInStackTrace() {
                    setStackTrace(createJavaStackTrace(testFailure));
                    return this;
                }

            };
        }
    }
}
