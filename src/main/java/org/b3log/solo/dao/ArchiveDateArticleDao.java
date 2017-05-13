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
import org.b3log.solo.dao.repository.SortDirection;
import org.b3log.solo.model.ArchiveDate;
import org.b3log.solo.model.Article;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Repository;

/**
 * Archive date-Article relation repository.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.0.0.6, Nov 9, 2011
 * @since 0.3.1
 */
@Repository
public class ArchiveDateArticleDao extends AbstractBlogDao {

	public JSONObject getByArchiveDateId(final String archiveDateId, final int currentPageNum, final int pageSize)
			throws RepositoryException {
		final Query query = new Query()
				.setFilter(new PropertyFilter(ArchiveDate.ARCHIVE_DATE + "_" + Keys.OBJECT_ID, FilterOperator.EQUAL,
						archiveDateId))
				.addSort(Article.ARTICLE + "_" + Keys.OBJECT_ID, SortDirection.DESCENDING)
				.setCurrentPageNum(currentPageNum).setPageSize(pageSize).setPageCount(1);

		return get(query);
	}

	public JSONObject getByArticleId(final String articleId) throws RepositoryException {
		final Query query = new Query();

		query.setFilter(new PropertyFilter(Article.ARTICLE + "_" + Keys.OBJECT_ID, FilterOperator.EQUAL, articleId));

		final JSONObject result = get(query);
		final JSONArray array = result.optJSONArray(Keys.RESULTS);

		if (0 == array.length()) {
			return null;
		}

		return array.optJSONObject(0);
	}

	@Override
	public String getTableNamePostfix() {
		return (ArchiveDate.ARCHIVE_DATE + "_" + Article.ARTICLE).toLowerCase();
	}

}
