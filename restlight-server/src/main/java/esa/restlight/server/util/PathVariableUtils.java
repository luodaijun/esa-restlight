/*
 * Copyright 2020 OPPO ESA Stack Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package esa.restlight.server.util;

import esa.commons.StringUtils;
import esa.commons.collection.LinkedMultiValueMap;
import esa.commons.collection.MultiValueMap;
import esa.httpserver.core.AsyncRequest;
import esa.restlight.server.route.predicate.PatternsPredicate;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringTokenizer;

public final class PathVariableUtils {

    private static final String MATRIX_VARIABLES_ATTRIBUTE = "$matrix.vars";

    public static String getPathVariable(AsyncRequest request, String name) {
        Map<String, String> variables = getPathVariables(request);
        if (variables == null || variables.isEmpty()) {
            return null;
        }
        return variables.get(name);
    }

    public static Map<String, String> getPathVariables(AsyncRequest request) {
        return request.getUncheckedAttribute(PatternsPredicate.TEMPLATE_VARIABLES);
    }

    public static Map<String, MultiValueMap<String, String>> getMatrixVariables(AsyncRequest request) {
        Map<String, MultiValueMap<String, String>> matrixVars =
                request.getUncheckedAttribute(MATRIX_VARIABLES_ATTRIBUTE);
        if (matrixVars == null) {
            matrixVars = extractMatrixVariables(getPathVariables(request));
            request.setAttribute(MATRIX_VARIABLES_ATTRIBUTE, matrixVars);
        }

        return matrixVars;
    }

    private static Map<String, MultiValueMap<String, String>> extractMatrixVariables(Map<String, String> uriVariables) {
        if (uriVariables == null || uriVariables.isEmpty()) {
            return Collections.emptyMap();
        }
        final Map<String, MultiValueMap<String, String>> result = new LinkedHashMap<>();
        uriVariables.forEach((uriVarKey, uriVarValue) -> {
            int equalsIndex = uriVarValue.indexOf('=');
            if (equalsIndex == -1) {
                return;
            }

            int semicolonIndex = uriVarValue.indexOf(';');
            if (semicolonIndex != -1 && semicolonIndex != 0) {
                uriVariables.put(uriVarKey, uriVarValue.substring(0, semicolonIndex));
            }

            String matrixVariables;
            if (semicolonIndex == -1 || semicolonIndex == 0 || equalsIndex < semicolonIndex) {
                matrixVariables = uriVarValue;
            } else {
                matrixVariables = uriVarValue.substring(semicolonIndex + 1);
            }

            result.put(uriVarKey, parseFrom(matrixVariables));
        });
        return result;
    }

    private static MultiValueMap<String, String> parseFrom(String matrixVariables) {
        final MultiValueMap<String, String> result = new LinkedMultiValueMap<>();
        if (StringUtils.isBlank(matrixVariables)) {
            return result;
        }
        StringTokenizer tokenizer = new StringTokenizer(matrixVariables, ";");
        while (tokenizer.hasMoreTokens()) {
            String entry = tokenizer.nextToken();
            final int index = entry.indexOf('=');
            if (index != -1) {
                parse(entry.substring(index + 1), entry.substring(0, index), result);
            } else {
                result.add(entry, StringUtils.empty());
            }
        }
        return result;
    }

    private static void parse(String str, String name, MultiValueMap<String, String> target) {
        if (StringUtils.isEmpty(str)) {
            return;
        }

        int pos = 0;
        int delPos;
        while ((delPos = str.indexOf(",", pos)) != -1) {
            target.add(name, str.substring(pos, delPos));
            pos = delPos + 1;
        }
        if (pos <= str.length()) {
            target.add(name, str.substring(pos));
        }
    }

}
