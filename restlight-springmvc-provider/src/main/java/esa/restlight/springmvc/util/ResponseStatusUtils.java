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
package esa.restlight.springmvc.util;

import esa.commons.StringUtils;
import esa.commons.reflect.AnnotationUtils;
import esa.restlight.springmvc.annotation.shaded.ResponseStatus0;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class ResponseStatusUtils {

    private static final int MAX_THROWABLE_STACK = 8;

    private static final Map<Class<? extends Throwable>, Optional<HttpResponseStatus>> CUSTOM_RESPONSE_MAP =
            new ConcurrentHashMap<>(16);

    public static HttpResponseStatus getCustomResponse(Class<?> userType, Method targetMethod) {
        ResponseStatus0 annotation = ResponseStatus0.fromShade(AnnotationUtils.findAnnotation(targetMethod,
                ResponseStatus0.shadedClass()));
        if (annotation == null) {
            annotation = ResponseStatus0.fromShade(AnnotationUtils.findAnnotation(userType,
                    ResponseStatus0.shadedClass()));
        }
        if (annotation != null) {
            if (StringUtils.isEmpty(annotation.reason())) {
                return HttpResponseStatus.valueOf(annotation.code());
            } else {
                return HttpResponseStatus.valueOf(annotation.code(), annotation.reason());
            }
        }

        return null;
    }

    public static HttpResponseStatus getCustomResponse(Throwable ex) {
        if (ex == null) {
            return null;
        }
        Optional<HttpResponseStatus> customResponse = CUSTOM_RESPONSE_MAP
                .computeIfAbsent(ex.getClass(), tClass -> findCustomResponse(ex, 0));
        return customResponse.orElse(null);
    }

    private static Optional<HttpResponseStatus> findCustomResponse(Throwable ex, int deep) {
        if (deep < MAX_THROWABLE_STACK) {
            ResponseStatus0 status = ResponseStatus0.fromShade(AnnotationUtils.findAnnotation(ex.getClass(),
                    ResponseStatus0.shadedClass()));
            if (status != null) {
                return Optional.of(new HttpResponseStatus(status.code(), status.reason()));
            } else if (ex.getCause() instanceof Exception) {
                return findCustomResponse(ex.getCause(), ++deep);
            }
        }
        return Optional.empty();
    }

}
