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
package org.b3log.solo.frame.repository;

import java.util.Arrays;

/**
 * Composite filter operator.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.0.0.0, Jun 27, 2012
 * @see CompositeFilter
 */
public enum CompositeFilterOperator {

    /**
     * And.
     */
    AND,
    /**
     * Or.
     */
    OR;

    /**
     * Builds an composite filter with 'AND' all the specified sub filters.
     * 
     * @param subFilters the specified sub filters
     * @return composite filter
     */
    public static CompositeFilter and(final Filter... subFilters) {
        return new CompositeFilter(AND, Arrays.asList(subFilters));
    }

    /**
     * Builds an composite filter with 'OR' all the specified sub filters.
     * 
     * @param subFilters the specified sub filters
     * @return composite filter
     */
    public static CompositeFilter or(final Filter... subFilters) {
        return new CompositeFilter(OR, Arrays.asList(subFilters));
    }
}
