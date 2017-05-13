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
package org.b3log.solo.service;

import java.util.List;

import org.b3log.solo.Keys;
import org.b3log.solo.dao.LinkDao;
import org.b3log.solo.dao.repository.Query;
import org.b3log.solo.dao.repository.SortDirection;
import org.b3log.solo.model.Link;
import org.b3log.solo.model.Pagination;
import org.b3log.solo.util.Paginator;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Link query service.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.0.0.2, Oct 31, 2011
 * @since 0.4.0
 */
@Service
public class LinkQueryService {

	/**
	 * Logger.
	 */
	private static Logger logger = LoggerFactory.getLogger(LinkQueryService.class);

	/**
	 * Link repository.
	 */
	@Autowired
	private LinkDao linkDao;

	/**
	 * Gets links by the specified request json object.
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
	 *     "links": [{
	 *         "oId": "",
	 *         "linkTitle": "",
	 *         "linkAddress": "",
	 *         ""linkDescription": ""
	 *      }, ....]
	 * }
	 *         </pre>
	 * 
	 * @throws ServiceException
	 *             service exception
	 * @see Pagination
	 */
	public JSONObject getLinks(final JSONObject requestJSONObject) throws ServiceException {
		final JSONObject ret = new JSONObject();

		try {
			final int currentPageNum = requestJSONObject.getInt(Pagination.PAGINATION_CURRENT_PAGE_NUM);
			final int pageSize = requestJSONObject.getInt(Pagination.PAGINATION_PAGE_SIZE);
			final int windowSize = requestJSONObject.getInt(Pagination.PAGINATION_WINDOW_SIZE);

			final Query query = new Query().setCurrentPageNum(currentPageNum).setPageSize(pageSize)
					.addSort(Link.LINK_ORDER, SortDirection.ASCENDING);
			final JSONObject result = linkDao.get(query);
			final int pageCount = result.getJSONObject(Pagination.PAGINATION).getInt(Pagination.PAGINATION_PAGE_COUNT);

			final JSONObject pagination = new JSONObject();
			final List<Integer> pageNums = Paginator.paginate(currentPageNum, pageSize, pageCount, windowSize);

			pagination.put(Pagination.PAGINATION_PAGE_COUNT, pageCount);
			pagination.put(Pagination.PAGINATION_PAGE_NUMS, pageNums);

			final JSONArray links = result.getJSONArray(Keys.RESULTS);

			ret.put(Pagination.PAGINATION, pagination);
			ret.put(Link.LINKS, links);

			return ret;
		} catch (final Exception e) {
			logger.error("Gets links failed", e);
			throw new ServiceException(e);
		}
	}

	/**
	 * Gets a link by the specified link id.
	 *
	 * @param linkId
	 *            the specified link id
	 * @return for example,
	 * 
	 *         <pre>
	 * {
	 *     "link": {
	 *         "oId": "",
	 *         "linkTitle": "",
	 *         "linkAddress": "",
	 *         "linkDescription": ""
	 *     }
	 * }
	 *         </pre>
	 * 
	 *         , returns {@code null} if not found
	 * @throws ServiceException
	 *             service exception
	 */
	public JSONObject getLink(final String linkId) throws ServiceException {
		final JSONObject ret = new JSONObject();

		try {
			final JSONObject link = linkDao.get(linkId);

			if (null == link) {
				return null;
			}

			ret.put(Link.LINK, link);

			return ret;
		} catch (final Exception e) {
			logger.error("Gets a link failed", e);

			throw new ServiceException(e);
		}
	}

	/**
	 * Sets the link repository with the specified link repository.
	 * 
	 * @param linkDao
	 *            the specified link repository
	 */
	public void setLinkRepository(final LinkDao linkDao) {
		this.linkDao = linkDao;
	}
}
