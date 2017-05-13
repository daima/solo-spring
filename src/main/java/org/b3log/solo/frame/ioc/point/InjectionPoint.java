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
package org.b3log.solo.frame.ioc.point;

import org.b3log.solo.frame.ioc.annotated.Annotated;
import org.b3log.solo.frame.ioc.bean.Bean;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.lang.reflect.Type;
import java.util.Set;

/**
 * JSR-299 SPI.
 *
 * @author Gavin King
 * @author Pete Muir
 */
public interface InjectionPoint
{

    /**
     * Get the required type of injection point.
     *
     * @return the required type
     */
    public Type getType();

    /**
     * Get the required qualifiers of the injection point.
     *
     * @return the required qualifiers
     */
    public Set<Annotation> getQualifiers();

    /**
     * Get the {@link javax.enterprise.inject.spi.Bean} object representing the
     * bean that defines the injection point. If the injection point does not
     * belong to a bean, return a null value.
     *
     * @return the {@link javax.enterprise.inject.spi.Bean} object representing
     *         bean that defines the injection point, of null if the injection
     *         point does not belong to a bean
     */
    public Bean<?> getBean();

    /**
     * Get the {@link java.lang.reflect.Field} object in the case of field
     * injection, the {@link java.lang.reflect.Method} object in
     * the case of method parameter injection or the
     * {@link java.lang.reflect.Constructor} object in the case of constructor
     * parameter injection.
     *
     * @return the member
     */
    public Member getMember();

    /**
     * Obtain an instance of {@link javax.enterprise.inject.spi.AnnotatedField}
     * or {@link javax.enterprise.inject.spi.AnnotatedParameter}, depending upon
     * whether the injection point is an injected field or a constructor/method parameter.
     *
     * @return an {@code AnnotatedField} or {@code AnnotatedParameter}
     */
    public Annotated getAnnotated();

    /**
     * Determines if the injection point is a decorator delegate injection point.
     *
     * @return <tt>true</tt> if the injection point is a decorator delegate injection point,
     * and <tt>false</tt> otherwise
     */
    public boolean isDelegate();

    /**
     * Determines if the injection is a transient field.
     *
     * @return <tt>true</tt> if the injection point is a transient field, and <tt>false</tt>
     * otherwise
     */
    public boolean isTransient();
}
