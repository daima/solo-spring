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
package org.b3log.solo.service;

import java.util.List;

import org.b3log.solo.Keys;
import org.b3log.solo.dao.PageDao;
import org.b3log.solo.dao.repository.Query;
import org.b3log.solo.dao.repository.SortDirection;
import org.b3log.solo.model.Page;
import org.b3log.solo.model.Pagination;
import org.b3log.solo.util.Paginator;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Page query service.
 *
 * @author <a href="http://cxy7.com">XyCai</a>
 * @version 1.0.0.0, Oct 27, 2011
 * @since 0.4.0
 */
@Service
public class PageQueryService {

	/**
	 * Logger.
	 */
	private static Logger logger = LoggerFactory.getLogger(PageQueryService.class);

	/**
	 * Page repository.
	 */
	@Autowired
	private PageDao pageDao;

	/**
	 * Gets a page by the specified page id.
	 *
	 * @param pageId
	 *            the specified page id
	 * @return for example,
	 * 
	 *         <pre>
	 * {
	 *     "page": {
	 *         "oId": "",
	 *         "pageTitle": "",
	 *         "pageContent": ""
	 *         "pageOrder": int,
	 *         "pagePermalink": "",
	 *         "pageCommentCount": int,
	 *         "pageCommentable": boolean,
	 *         "pageType": "",
	 *         "pageOpenTarget": ""
	 *     }
	 * }
	 *         </pre>
	 * 
	 *         , returns {@code null} if not found
	 * @throws ServiceException
	 *             service exception
	 */
	public JSONObject getPage(final String pageId) throws ServiceException {
		final JSONObject ret = new JSONObject();

		try {
			final JSONObject page = pageDao.get(pageId);

			if (null == page) {
				return null;
			}

			ret.put(Page.PAGE, page);

			return ret;
		} catch (final Exception e) {
			logger.error(e.getMessage(), e);

			throw new ServiceException(e);
		}
	}

	/**
	 * Gets pages by the specified request json object.
	 * 
	 * @param requestJSONObject
	 *            the specified request json object, for example,
	 * 
	 *            <pre>
	 * {
	 *     "paginationCurrentPageNum": 1,
	 *     "paginationPageSize": 20,
	 *     "paginationWindowSize": 10
	 * }, see {@link Pagination} for more details
	 *            </pre>
	 * 
	 * @return for example,
	 * 
	 *         <pre>
	 * {
	 *     "pagination": {
	 *         "paginationPageCount": 100,
	 *         "paginationPageNums": [1, 2, 3, 4, 5]
	 *     },
	 *     "pages": [{
	 *         "oId": "",
	 *         "pageTitle": "",
	 *         "pageCommentCount": int,
	 *         "pageOrder": int,
	 *         "pagePermalink": "",
	 *         "pageCommentable": boolean,
	 *         "pageType": "",
	 *         "pageOpenTarget": ""
	 *      }, ....]
	 * }
	 *         </pre>
	 * 
	 * @throws ServiceException
	 *             service exception
	 * @see Pagination
	 */
	public JSONObject getPages(final JSONObject requestJSONObject) throws ServiceException {
		final JSONObject ret = new JSONObject();

		try {
			final int currentPageNum = requestJSONObject.getInt(Pagination.PAGINATION_CURRENT_PAGE_NUM);
			final int pageSize = requestJSONObject.getInt(Pagination.PAGINATION_PAGE_SIZE);
			final int windowSize = requestJSONObject.getInt(Pagination.PAGINATION_WINDOW_SIZE);

			final Query query = new Query().setCurrentPageNum(currentPageNum).setPageSize(pageSize)
					.addSort(Page.PAGE_ORDER, SortDirection.ASCENDING).setPageCount(1);
			final JSONObject result = pageDao.get(query);
			final int pageCount = result.getJSONObject(Pagination.PAGINATION).getInt(Pagination.PAGINATION_PAGE_COUNT);

			final JSONObject pagination = new JSONObject();
			final List<Integer> pageNums = Paginator.paginate(currentPageNum, pageSize, pageCount, windowSize);

			pagination.put(Pagination.PAGINATION_PAGE_COUNT, pageCount);
			pagination.put(Pagination.PAGINATION_PAGE_NUMS, pageNums);

			final JSONArray pages = result.getJSONArray(Keys.RESULTS);

			for (int i = 0; i < pages.length(); i++) { // remove unused
														// properties
				final JSONObject page = pages.getJSONObject(i);

				page.remove(Page.PAGE_CONTENT);
			}

			ret.put(Pagination.PAGINATION, pagination);
			ret.put(Page.PAGES, pages);

			return ret;
		} catch (final Exception e) {
			logger.error("Gets pages failed", e);

			throw new ServiceException(e);
		}
	}

	/**
	 * Set the page repository with the specified page repository.
	 * 
	 * @param pageDao
	 *            the specified page repository
	 */
	public void setPageRepository(final PageDao pageDao) {
		this.pageDao = pageDao;
	}
}
