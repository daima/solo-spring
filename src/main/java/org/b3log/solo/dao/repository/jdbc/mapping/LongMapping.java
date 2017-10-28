/*
 * Copyright (c) 2017, cxy7.com
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
package org.b3log.solo.dao.repository.jdbc.mapping;

import org.b3log.solo.dao.repository.jdbc.util.FieldDefinition;

/**
 * Long type mapping.
 *
 * <p>
 * Maps Java long type to SQL bigint type.
 * </p>
 *
 * @author <a href="http://cxy7.com">XyCai</a>
 * @version 1.0.0.0, Feb 29, 2012
 */
public final class LongMapping implements Mapping {

	@Override
	public String toDataBaseSting(final FieldDefinition definition) {
		final StringBuilder builder = new StringBuilder(definition.getName()).append(" bigint");

		if (!definition.getNullable()) {
			builder.append(" not null");
		}

		return builder.toString();
	}
}
