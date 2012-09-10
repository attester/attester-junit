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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonInputStream {

    private static ObjectMapper objectMapper = new ObjectMapper();
    private static JsonFactory jsonFactory = objectMapper.getJsonFactory();
    private JsonParser parser;

    private static Reader createReader(InputStream inputStream) {
        try {
            return new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public JsonInputStream(InputStream inputStream) {
        this(createReader(inputStream));
    }

    public JsonInputStream(Reader reader) {
        try {
            parser = jsonFactory.createJsonParser(reader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public JsonNode read() {
        try {
            return objectMapper.readTree(parser);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static ObjectMapper getObjectMapper() {
        return objectMapper;
    }

}