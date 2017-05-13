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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.b3log.solo.dao.ArticleDao;
import org.b3log.solo.dao.StatisticDao;
import org.b3log.solo.dao.repository.RepositoryException;
import org.b3log.solo.model.Statistic;
import org.b3log.solo.util.Requests;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Statistic management service.
 *
 * <p>
 * <b>Note</b>: The {@link #onlineVisitorCount online visitor counting} is NOT
 * cluster-safe.
 * </p>
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.0.0.0, Jul 18, 2012
 * @since 0.5.0
 */
@Service
public class StatisticMgmtService {

	/**
	 * Logger.
	 */
	private static Logger logger = LoggerFactory.getLogger(StatisticMgmtService.class);

	/**
	 * Statistic repository.
	 */
	@Autowired
	private StatisticDao statisticDao;

	/**
	 * Online visitor cache.
	 *
	 * <p>
	 * &lt;ip, recentTime&gt;
	 * </p>
	 */
	public static final Map<String, Long> ONLINE_VISITORS = new HashMap<>();

	/**
	 * Online visitor expiration in 5 minutes.
	 */
	private static final int ONLINE_VISITOR_EXPIRATION = 300000;

	/**
	 * Blog statistic view count +1.
	 *
	 * <p>
	 * If it is a search engine bot made the specified request, will NOT
	 * increment blog statistic view count.
	 * </p>
	 *
	 * <p>
	 * There is a cron job (/console/stat/viewcnt) to flush the blog view count
	 * from cache to datastore.
	 * </p>
	 *
	 * @param request
	 *            the specified request
	 * @param response
	 *            the specified response
	 * @throws ServiceException
	 *             service exception
	 * @see Requests#searchEngineBotRequest(javax.servlet.http.HttpServletRequest)
	 */
	public void incBlogViewCount(final HttpServletRequest request, final HttpServletResponse response)
			throws ServiceException {
		if (Requests.searchEngineBotRequest(request)) {
			return;
		}

		if (Requests.hasBeenServed(request, response)) {
			return;
		}

		// final Transaction transaction = statisticDao.beginTransaction();
		JSONObject statistic = null;

		try {
			statistic = statisticDao.get(Statistic.STATISTIC);
			if (null == statistic) {
				return;
			}

			logger.trace("Before inc blog view count[statistic={0}]", statistic);

			int blogViewCnt = statistic.optInt(Statistic.STATISTIC_BLOG_VIEW_COUNT);

			++blogViewCnt;
			statistic.put(Statistic.STATISTIC_BLOG_VIEW_COUNT, blogViewCnt);

			statisticDao.update(Statistic.STATISTIC, statistic);

			// transaction.commit();
		} catch (final RepositoryException e) {
			// if (transaction.isActive()) {
			// transaction.rollback();
			// }

			logger.error("Updates blog view count failed", e);

			return;
		}

		logger.trace("Inced blog view count[statistic={0}]", statistic);
	}

	/**
	 * Blog statistic article count +1.
	 *
	 * @throws RepositoryException
	 *             repository exception
	 */
	public void incBlogArticleCount() throws RepositoryException {
		final JSONObject statistic = statisticDao.get(Statistic.STATISTIC);

		if (null == statistic) {
			throw new RepositoryException("Not found statistic");
		}

		statistic.put(Statistic.STATISTIC_BLOG_ARTICLE_COUNT,
				statistic.optInt(Statistic.STATISTIC_BLOG_ARTICLE_COUNT) + 1);
		statisticDao.update(Statistic.STATISTIC, statistic);
	}

	/**
	 * Blog statistic published article count +1.
	 *
	 * @throws RepositoryException
	 *             repository exception
	 */
	public void incPublishedBlogArticleCount() throws RepositoryException {
		final JSONObject statistic = statisticDao.get(Statistic.STATISTIC);

		if (null == statistic) {
			throw new RepositoryException("Not found statistic");
		}

		statistic.put(Statistic.STATISTIC_PUBLISHED_ARTICLE_COUNT,
				statistic.optInt(Statistic.STATISTIC_PUBLISHED_ARTICLE_COUNT) + 1);
		statisticDao.update(Statistic.STATISTIC, statistic);
	}

	/**
	 * Blog statistic article count -1.
	 *
	 * @throws JSONException
	 *             json exception
	 * @throws RepositoryException
	 *             repository exception
	 */
	public void decBlogArticleCount() throws JSONException, RepositoryException {
		final JSONObject statistic = statisticDao.get(Statistic.STATISTIC);

		if (null == statistic) {
			throw new RepositoryException("Not found statistic");
		}

		statistic.put(Statistic.STATISTIC_BLOG_ARTICLE_COUNT,
				statistic.getInt(Statistic.STATISTIC_BLOG_ARTICLE_COUNT) - 1);
		statisticDao.update(Statistic.STATISTIC, statistic);
	}

	/**
	 * Blog statistic published article count -1.
	 *
	 * @throws JSONException
	 *             json exception
	 * @throws RepositoryException
	 *             repository exception
	 */
	public void decPublishedBlogArticleCount() throws JSONException, RepositoryException {
		final JSONObject statistic = statisticDao.get(Statistic.STATISTIC);

		if (null == statistic) {
			throw new RepositoryException("Not found statistic");
		}

		statistic.put(Statistic.STATISTIC_PUBLISHED_ARTICLE_COUNT,
				statistic.getInt(Statistic.STATISTIC_PUBLISHED_ARTICLE_COUNT) - 1);
		statisticDao.update(Statistic.STATISTIC, statistic);
	}

	/**
	 * Blog statistic comment count +1.
	 *
	 * @throws JSONException
	 *             json exception
	 * @throws RepositoryException
	 *             repository exception
	 */
	public void incBlogCommentCount() throws JSONException, RepositoryException {
		final JSONObject statistic = statisticDao.get(Statistic.STATISTIC);

		if (null == statistic) {
			throw new RepositoryException("Not found statistic");
		}
		statistic.put(Statistic.STATISTIC_BLOG_COMMENT_COUNT,
				statistic.getInt(Statistic.STATISTIC_BLOG_COMMENT_COUNT) + 1);
		statisticDao.update(Statistic.STATISTIC, statistic);
	}

	/**
	 * Blog statistic comment(published article) count +1.
	 *
	 * @throws JSONException
	 *             json exception
	 * @throws RepositoryException
	 *             repository exception
	 */
	public void incPublishedBlogCommentCount() throws JSONException, RepositoryException {
		final JSONObject statistic = statisticDao.get(Statistic.STATISTIC);

		if (null == statistic) {
			throw new RepositoryException("Not found statistic");
		}
		statistic.put(Statistic.STATISTIC_PUBLISHED_BLOG_COMMENT_COUNT,
				statistic.getInt(Statistic.STATISTIC_PUBLISHED_BLOG_COMMENT_COUNT) + 1);
		statisticDao.update(Statistic.STATISTIC, statistic);
	}

	/**
	 * Blog statistic comment count -1.
	 *
	 * @throws JSONException
	 *             json exception
	 * @throws RepositoryException
	 *             repository exception
	 */
	public void decBlogCommentCount() throws JSONException, RepositoryException {
		final JSONObject statistic = statisticDao.get(Statistic.STATISTIC);

		if (null == statistic) {
			throw new RepositoryException("Not found statistic");
		}

		statistic.put(Statistic.STATISTIC_BLOG_COMMENT_COUNT,
				statistic.getInt(Statistic.STATISTIC_BLOG_COMMENT_COUNT) - 1);
		statisticDao.update(Statistic.STATISTIC, statistic);
	}

	/**
	 * Blog statistic comment(published article) count -1.
	 *
	 * @throws JSONException
	 *             json exception
	 * @throws RepositoryException
	 *             repository exception
	 */
	public void decPublishedBlogCommentCount() throws JSONException, RepositoryException {
		final JSONObject statistic = statisticDao.get(Statistic.STATISTIC);

		if (null == statistic) {
			throw new RepositoryException("Not found statistic");
		}

		statistic.put(Statistic.STATISTIC_PUBLISHED_BLOG_COMMENT_COUNT,
				statistic.getInt(Statistic.STATISTIC_PUBLISHED_BLOG_COMMENT_COUNT) - 1);
		statisticDao.update(Statistic.STATISTIC, statistic);
	}

	/**
	 * Sets blog comment count with the specified count.
	 *
	 * @param count
	 *            the specified count
	 * @throws JSONException
	 *             json exception
	 * @throws RepositoryException
	 *             repository exception
	 */
	public void setBlogCommentCount(final int count) throws JSONException, RepositoryException {
		final JSONObject statistic = statisticDao.get(Statistic.STATISTIC);

		if (null == statistic) {
			throw new RepositoryException("Not found statistic");
		}

		statistic.put(Statistic.STATISTIC_BLOG_COMMENT_COUNT, count);
		statisticDao.update(Statistic.STATISTIC, statistic);
	}

	/**
	 * Sets blog comment(published article) count with the specified count.
	 *
	 * @param count
	 *            the specified count
	 * @throws JSONException
	 *             json exception
	 * @throws RepositoryException
	 *             repository exception
	 */
	public void setPublishedBlogCommentCount(final int count) throws JSONException, RepositoryException {
		final JSONObject statistic = statisticDao.get(Statistic.STATISTIC);

		if (null == statistic) {
			throw new RepositoryException("Not found statistic");
		}

		statistic.put(Statistic.STATISTIC_PUBLISHED_BLOG_COMMENT_COUNT, count);
		statisticDao.update(Statistic.STATISTIC, statistic);
	}

	/**
	 * Refreshes online visitor count for the specified request.
	 *
	 * @param request
	 *            the specified request
	 */
	public void onlineVisitorCount(final HttpServletRequest request) {
		final String remoteAddr = Requests.getRemoteAddr(request);

		logger.debug("Current request [IP={0}]", remoteAddr);

		ONLINE_VISITORS.put(remoteAddr, System.currentTimeMillis());
		logger.debug("Current online visitor count [{0}]", ONLINE_VISITORS.size());
	}

	/**
	 * Removes the expired online visitor.
	 */
	public static void removeExpiredOnlineVisitor() {
		final long currentTimeMillis = System.currentTimeMillis();

		final Iterator<Map.Entry<String, Long>> iterator = ONLINE_VISITORS.entrySet().iterator();

		while (iterator.hasNext()) {
			final Map.Entry<String, Long> onlineVisitor = iterator.next();

			if (currentTimeMillis > (onlineVisitor.getValue() + ONLINE_VISITOR_EXPIRATION)) {
				iterator.remove();
				logger.trace("Removed online visitor[ip={0}]", onlineVisitor.getKey());
			}
		}

		logger.debug("Current online visitor count [{0}]", ONLINE_VISITORS.size());
	}

	/**
	 * Updates the statistic with the specified statistic.
	 *
	 * @param statistic
	 *            the specified statistic
	 * @throws ServiceException
	 *             service exception
	 */
	public void updateStatistic(final JSONObject statistic) throws ServiceException {
		// final Transaction transaction = statisticDao.beginTransaction();

		try {
			statisticDao.update(Statistic.STATISTIC, statistic);
			// transaction.commit();
		} catch (final RepositoryException e) {
			// if (transaction.isActive()) {
			// transaction.rollback();
			// }
			logger.error("Updates statistic failed", e);
		}

		logger.debug("Updates statistic successfully");
	}

	/**
	 * Sets the article repository with the specified article repository.
	 *
	 * @param articleDao
	 *            the specified article repository
	 */
	public void setArticleRepository(final ArticleDao articleDao) {
	}

	/**
	 * Sets the statistic repository with the specified statistic repository.
	 *
	 * @param statisticDao
	 *            the specified statistic repository
	 */
	public void setStatisticRepository(final StatisticDao statisticDao) {
		this.statisticDao = statisticDao;
	}

	/**
	 * Sets the language service with the specified language service.
	 *
	 * @param langPropsService
	 *            the specified language service
	 */
	public void setLangPropsService(final LangPropsService langPropsService) {
	}
}
