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
package org.b3log.solo.frame.ioc.context;

/**
 * <p>Provides operations that are used by the
 * {@link javax.enterprise.context.spi.Contextual} implementation during
 * instance creation and destruction.</p>
 *
 * @author Gavin King
 * @author Pete Muir
 */
public interface CreationalContext<T> {

    /**
     * Registers an incompletely initialized contextual instance the with the
     * container. A contextual instance is considered incompletely initialized
     * until it is returned by
     * {@link javax.enterprise.context.spi.Contextual#create(CreationalContext)}.
     *
     * @param incompleteInstance the incompletely initialized instance
     */
    public void push(T incompleteInstance);

    /**
     * Destroys all dependent objects of the instance which is being destroyed,
     * by passing each dependent object to
     * {@link javax.enterprise.context.spi.Contextual#destroy(Object, CreationalContext)}.
     */
    public void release();

}
