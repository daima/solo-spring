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
package org.b3log.solo.dao.repository.jdbc.mapping;

import org.b3log.solo.dao.repository.jdbc.util.FieldDefinition;

/**
 * String mapping.
 *
 * @author <a href="mailto:wmainlove@gmail.com">Love Yao</a>
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.0.0.1, Feb 21, 2013
 */
public class StringMapping implements Mapping {

	@Override
	public String toDataBaseSting(final FieldDefinition definition) {
		final StringBuilder sql = new StringBuilder();

		sql.append(definition.getName());

		if (definition.getLength() == null) {
			definition.setLength(new Integer("0"));
		}

		final Integer length = definition.getLength();

		if (length > new Integer("255")) {
			if (length > new Integer("16777215")) {
				sql.append(" longtext");
			} else if (length > new Integer("65535")) {
				sql.append(" mediumtext");
			} else {
				sql.append(" text");
			}
		} else {
			sql.append(" varchar(").append(length < 1 ? new Integer("100") : length);
			sql.append(")");
		}

		if (!definition.getNullable()) {
			sql.append(" not null");
		}

		return sql.toString();
	}
}
