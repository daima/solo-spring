/*
 * Copyright (c) 2010-2017, b3log.org & hacpai.com
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
package org.b3log.solo.dao;

import org.b3log.solo.Keys;
import org.b3log.solo.dao.repository.FilterOperator;
import org.b3log.solo.dao.repository.PropertyFilter;
import org.b3log.solo.dao.repository.Query;
import org.b3log.solo.dao.repository.RepositoryException;
import org.b3log.solo.model.Role;
import org.b3log.solo.model.User;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Repository;

/**
 * User repository.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.0.0.8, Nov 10, 2011
 * @since 0.3.1
 */
@Repository
public class UserDao extends AbstractBlogDao {

	@Override
	public String getTableNamePostfix() {
		return User.USER;
	}

	public JSONObject getByEmail(final String email) throws RepositoryException {
		final Query query = new Query().setPageCount(1);

		query.setFilter(new PropertyFilter(User.USER_EMAIL, FilterOperator.EQUAL, email.toLowerCase().trim()));

		final JSONObject result = get(query);
		final JSONArray array = result.optJSONArray(Keys.RESULTS);

		if (0 == array.length()) {
			return null;
		}

		return array.optJSONObject(0);
	}

	public JSONObject getAdmin() throws RepositoryException {
		final Query query = new Query()
				.setFilter(new PropertyFilter(User.USER_ROLE, FilterOperator.EQUAL, Role.ADMIN_ROLE)).setPageCount(1);
		final JSONObject result = get(query);
		final JSONArray array = result.optJSONArray(Keys.RESULTS);

		if (0 == array.length()) {
			return null;
		}

		return array.optJSONObject(0);
	}

	public boolean isAdminEmail(final String email) throws RepositoryException {
		final JSONObject user = getByEmail(email);

		if (null == user) {
			return false;
		}

		return Role.ADMIN_ROLE.equals(user.optString(User.USER_ROLE));
	}
}
