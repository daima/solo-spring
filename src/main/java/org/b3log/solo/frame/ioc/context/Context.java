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

import java.lang.annotation.Annotation;

/**
 * <p>Provides an operation for obtaining contextual instances with a particular scope
 * of any contextual type. Any instance of {@code Context} is called a context object.</p>
 * <p>
 * <p>The context object is responsible for creating and destroying contextual instances
 * by calling operations of {@link Contextual}. In particular,
 * the context object is responsible for destroying any contextual instance it creates by
 * passing the instance to
 * {@link Contextual#destroy(Object, CreationalContext)}. A
 * destroyed instance must not subsequently be returned by {@code get()}.
 * The context object must pass the same instance of
 * {@link CreationalContext} to {@code Contextual.destroy()}
 * that it passed to {@code Contextual.create()} when it created the instance.</p>
 * <p>
 *
 * @author Gavin King
 * @author Pete Muir
 */
public interface Context {

    /**
     * Get the scope type of the context object.
     *
     * @return the scope
     */
    public Class<? extends Annotation> getScope();

    /**
     * Return an existing instance of certain contextual type or create a new
     * instance by calling
     * {@link Contextual#create(CreationalContext)}
     * and return the new instance.
     *
     * @param <T>               the type of contextual type
     * @param contextual        the contextual type
     * @param creationalContext the context in which the new instance will be created
     * @return the contextual instance
     * @throws ContextNotActiveException if the context is not active
     */
    public <T> T get(Contextual<T> contextual, CreationalContext<T> creationalContext);

    /**
     * Return an existing instance of a certain contextual type or a null value.
     *
     * @param <T>        the type of the contextual type
     * @param contextual the contextual type
     * @return the contextual instance, or a null value
     * @throws ContextNotActiveException if the context is not active
     */
    public <T> T get(Contextual<T> contextual);

    /**
     * Determines if the context object is active.
     *
     * @return <tt>true</tt> if the context is active, or <tt>false</tt> otherwise.
     */
    boolean isActive();

}

