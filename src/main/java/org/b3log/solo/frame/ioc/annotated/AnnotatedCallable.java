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
package org.b3log.solo.frame.ioc.annotated;

import java.util.List;

/**
 * <p>Represents a callable member of a Java type.</p>
 *
 * @author Gavin King
 * @author Pete Muir
 *
 * @param <X> the declaring type
 */
public interface AnnotatedCallable<X> extends AnnotatedMember<X>
{

    /**
     * <p>Get the parameters of the callable member.</p>
     *
     * @return the parameters
     */
    public List<AnnotatedParameter<X>> getParameters();

}
