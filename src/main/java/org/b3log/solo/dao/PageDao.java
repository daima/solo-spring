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


import java.util.List;

import org.b3log.solo.Keys;
import org.b3log.solo.frame.repository.FilterOperator;
import org.b3log.solo.frame.repository.PropertyFilter;
import org.b3log.solo.frame.repository.Query;
import org.b3log.solo.frame.repository.RepositoryException;
import org.b3log.solo.frame.repository.SortDirection;
import org.b3log.solo.util.CollectionUtils;
import org.b3log.solo.model.Page;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Repository;


/**
 * Page repository.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.0.0.9, Dec 31, 2011
 * @since 0.3.1
 */
@Repository
public class PageDao extends AbstractBlogDao{

	@Override
	public String getTableNamePostfix() {
        return Page.PAGE;
    }

   
    public JSONObject getByPermalink(final String permalink) throws RepositoryException {
        final Query query = new Query().setFilter(new PropertyFilter(Page.PAGE_PERMALINK, FilterOperator.EQUAL, permalink)).setPageCount(1);
        final JSONObject result = get(query);
        final JSONArray array = result.optJSONArray(Keys.RESULTS);

        if (0 == array.length()) {
            return null;
        }

        return array.optJSONObject(0);
    }

   
    public int getMaxOrder() throws RepositoryException {
        final Query query = new Query().addSort(Page.PAGE_ORDER, SortDirection.DESCENDING).setPageCount(1);
        final JSONObject result = get(query);
        final JSONArray array = result.optJSONArray(Keys.RESULTS);

        if (0 == array.length()) {
            return -1;
        }

        return array.optJSONObject(0).optInt(Page.PAGE_ORDER);
    }

   
    public JSONObject getUpper(final String id) throws RepositoryException {
        final JSONObject page = get(id);

        if (null == page) {
            return null;
        }

        final Query query = new Query().setFilter(new PropertyFilter(Page.PAGE_ORDER, FilterOperator.LESS_THAN, page.optInt(Page.PAGE_ORDER))).addSort(Page.PAGE_ORDER, SortDirection.DESCENDING).setCurrentPageNum(1).setPageSize(1).setPageCount(
            1);

        final JSONObject result = get(query);
        final JSONArray array = result.optJSONArray(Keys.RESULTS);

        if (1 != array.length()) {
            return null;
        }

        return array.optJSONObject(0);
    }

   
    public JSONObject getUnder(final String id) throws RepositoryException {
        final JSONObject page = get(id);

        if (null == page) {
            return null;
        }

        final Query query = new Query().setFilter(new PropertyFilter(Page.PAGE_ORDER, FilterOperator.GREATER_THAN, page.optInt(Page.PAGE_ORDER))).addSort(Page.PAGE_ORDER, SortDirection.ASCENDING).setCurrentPageNum(1).setPageSize(1).setPageCount(
            1);

        final JSONObject result = get(query);
        final JSONArray array = result.optJSONArray(Keys.RESULTS);

        if (1 != array.length()) {
            return null;
        }

        return array.optJSONObject(0);
    }

   
    public JSONObject getByOrder(final int order) throws RepositoryException {
        final Query query = new Query().setFilter(new PropertyFilter(Page.PAGE_ORDER, FilterOperator.EQUAL, order)).setPageCount(1);
        final JSONObject result = get(query);
        final JSONArray array = result.optJSONArray(Keys.RESULTS);

        if (0 == array.length()) {
            return null;
        }

        return array.optJSONObject(0);
    }

   
    public List<JSONObject> getPages() throws RepositoryException {
        final Query query = new Query().addSort(Page.PAGE_ORDER, SortDirection.ASCENDING).setPageCount(1);
        final JSONObject result = get(query);

        return CollectionUtils.jsonArrayToList(result.optJSONArray(Keys.RESULTS));
    }
}
