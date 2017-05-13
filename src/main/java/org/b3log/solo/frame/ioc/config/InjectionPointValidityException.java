/*
 * Copyright (c) 2009-2016, b3log.org & hacpai.com
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
package org.b3log.solo.frame.ioc.config;


/**
 * Injection point validity exception.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.0.0.0, Nov 20, 2009
 */
public final class InjectionPointValidityException extends RuntimeException {

    /**
     * Creates a new instance of <code>InjectionPointValidityException</code>
     * without detail message.
     */
    public InjectionPointValidityException() {}

    /**
     * Constructs an instance of <code>InjectionPointValidityException</code>
     * with the specified detail message.
     *
     * @param msg the detail message.
     */
    public InjectionPointValidityException(final String msg) {
        super(msg);
    }
}
