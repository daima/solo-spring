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
import org.b3log.solo.dao.CommentDao;
import org.b3log.solo.dao.PageDao;
import org.b3log.solo.dao.repository.RepositoryException;
import org.b3log.solo.model.Comment;
import org.b3log.solo.model.Option;
import org.b3log.solo.model.Page;
import org.b3log.solo.module.util.Comments;
import org.b3log.solo.util.Ids;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Page management service.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.1.0.8, Nov 20, 2015
 * @since 0.4.0
 */
@Service
public class PageMgmtService {

	/**
	 * Logger.
	 */
	private static Logger logger = LoggerFactory.getLogger(PageMgmtService.class);

	/**
	 * Page repository.
	 */
	@Autowired
	private PageDao pageDao;

	/**
	 * Comment repository.
	 */
	@Autowired
	private CommentDao commentDao;

	/**
	 * Language service.
	 */
	@Autowired
	private LangPropsService langPropsService;

	/**
	 * Permalink query service.
	 */
	@Autowired
	private PermalinkQueryService permalinkQueryService;

	/**
	 * Preference query service.
	 */
	@Autowired
	private PreferenceQueryService preferenceQueryService;

	/**
	 * Statistic management service.
	 */
	@Autowired
	private StatisticMgmtService statisticMgmtService;

	/**
	 * Statistic query service.
	 */
	@Autowired
	private StatisticQueryService statisticQueryService;

	/**
	 * Updates a page by the specified request json object.
	 *
	 * @param requestJSONObject
	 *            the specified request json object, for example,
	 * 
	 *            <pre>
	 * {
	 *     "page": {
	 *         "oId": "",
	 *         "pageTitle": "",
	 *         "pageContent": "",
	 *         "pageOrder": int,
	 *         "pageCommentCount": int,
	 *         "pagePermalink": "",
	 *         "pageCommentable": boolean,
	 *         "pageType": "",
	 *         "pageOpenTarget": "",
	 *         "pageEditorType": "" // optional, preference specified if not exists this key
	 *     }
	 * }, see {@link Page} for more details
	 *            </pre>
	 *
	 * @throws ServiceException
	 *             service exception
	 */
	public void updatePage(final JSONObject requestJSONObject) throws ServiceException {

		// final Transaction transaction = pageDao.beginTransaction();

		try {
			final JSONObject page = requestJSONObject.getJSONObject(Page.PAGE);
			final String pageId = page.getString(Keys.OBJECT_ID);
			final JSONObject oldPage = pageDao.get(pageId);
			final JSONObject newPage = new JSONObject(page, JSONObject.getNames(page));

			newPage.put(Page.PAGE_ORDER, oldPage.getInt(Page.PAGE_ORDER));
			newPage.put(Page.PAGE_COMMENT_COUNT, oldPage.getInt(Page.PAGE_COMMENT_COUNT));
			String permalink = page.optString(Page.PAGE_PERMALINK).trim();

			final String oldPermalink = oldPage.getString(Page.PAGE_PERMALINK);

			if (!oldPermalink.equals(permalink)) {
				if (StringUtils.isBlank(permalink)) {
					permalink = "/pages/" + pageId + ".html";
				}

				if (Page.PAGE.equals(page.getString(Page.PAGE_TYPE))) {
					if (!permalink.startsWith("/")) {
						permalink = "/" + permalink;
					}

					if (PermalinkQueryService.invalidPagePermalinkFormat(permalink)) {
						// if (transaction.isActive()) {
						// transaction.rollback();
						// }

						throw new ServiceException(langPropsService.get("invalidPermalinkFormatLabel"));
					}

					if (!oldPermalink.equals(permalink) && permalinkQueryService.exist(permalink)) {
						// if (transaction.isActive()) {
						// transaction.rollback();
						// }

						throw new ServiceException(langPropsService.get("duplicatedPermalinkLabel"));
					}
				}
			}

			newPage.put(Page.PAGE_PERMALINK, permalink.replaceAll(" ", "-"));

			if (!oldPage.getString(Page.PAGE_PERMALINK).equals(permalink)) { // The
																				// permalink
																				// has
																				// been
																				// updated
				// Updates related comments' links
				processCommentsForPageUpdate(newPage);
			}

			// Set editor type
			if (!newPage.has(Page.PAGE_EDITOR_TYPE)) {
				final JSONObject preference = preferenceQueryService.getPreference();
				newPage.put(Page.PAGE_EDITOR_TYPE, preference.optString(Option.ID_C_EDITOR_TYPE));
			}

			pageDao.update(pageId, newPage);

			// transaction.commit();

			logger.debug("Updated a page[id={0}]", pageId);
		} catch (final Exception e) {
			logger.error(e.getMessage(), e);
			// if (transaction.isActive()) {
			// transaction.rollback();
			// }

			throw new ServiceException(e);
		}
	}

	/**
	 * Removes a page specified by the given page id.
	 *
	 * @param pageId
	 *            the given page id
	 * @throws ServiceException
	 *             service exception
	 */
	public void removePage(final String pageId) throws ServiceException {
		// final Transaction transaction = pageDao.beginTransaction();

		try {
			logger.debug("Removing a page[id={0}]", pageId);
			removePageComments(pageId);
			pageDao.remove(pageId);

			// transaction.commit();

		} catch (final Exception e) {
			// if (transaction.isActive()) {
			// transaction.rollback();
			// }

			logger.error("Removes a page[id=" + pageId + "] failed", e);

			throw new ServiceException(e);
		}
	}

	/**
	 * Adds a page with the specified request json object.
	 *
	 * @param requestJSONObject
	 *            the specified request json object, for example,
	 * 
	 *            <pre>
	 * {
	 *     "page": {
	 *         "pageTitle": "",
	 *         "pageContent": "",
	 *         "pageOpenTarget": "",
	 *         "pageCommentable": boolean,
	 *         "pageType": "",
	 *         "pagePermalink": "", // optional
	 *         "pageEditorType": "" // optional, preference specified if not exists this key
	 *     }
	 * }, see {@link Page} for more details
	 *            </pre>
	 *
	 * @return generated page id
	 * @throws ServiceException
	 *             if permalink format checks failed or persists failed
	 */
	public String addPage(final JSONObject requestJSONObject) throws ServiceException {
		// final Transaction transaction = pageDao.beginTransaction();

		try {
			final JSONObject page = requestJSONObject.getJSONObject(Page.PAGE);

			page.put(Page.PAGE_COMMENT_COUNT, 0);
			final int maxOrder = pageDao.getMaxOrder();

			page.put(Page.PAGE_ORDER, maxOrder + 1);

			String permalink = page.optString(Page.PAGE_PERMALINK);

			if (StringUtils.isBlank(permalink)) {
				permalink = "/pages/" + Ids.genTimeMillisId() + ".html";
			}

			if (Page.PAGE.equals(page.getString(Page.PAGE_TYPE))) {
				if (!permalink.startsWith("/")) {
					permalink = "/" + permalink;
				}

				if (PermalinkQueryService.invalidPagePermalinkFormat(permalink)) {
					// if (transaction.isActive()) {
					// transaction.rollback();
					// }

					throw new ServiceException(langPropsService.get("invalidPermalinkFormatLabel"));
				}

				if (permalinkQueryService.exist(permalink)) {
					// if (transaction.isActive()) {
					// transaction.rollback();
					// }

					throw new ServiceException(langPropsService.get("duplicatedPermalinkLabel"));
				}
			}

			page.put(Page.PAGE_PERMALINK, permalink.replaceAll(" ", "-"));

			// Set editor type
			if (!page.has(Page.PAGE_EDITOR_TYPE)) {
				final JSONObject preference = preferenceQueryService.getPreference();
				page.put(Page.PAGE_EDITOR_TYPE, preference.optString(Option.ID_C_EDITOR_TYPE));
			}

			final String ret = pageDao.add(page);

			// transaction.commit();

			return ret;
		} catch (final JSONException e) {
			logger.error(e.getMessage(), e);
			// if (transaction.isActive()) {
			// transaction.rollback();
			// }

			throw new ServiceException(e);
		} catch (final RepositoryException e) {
			logger.error(e.getMessage(), e);
			// if (transaction.isActive()) {
			// transaction.rollback();
			// }

			throw new ServiceException(e);
		}
	}

	/**
	 * Changes the order of a page specified by the given page id with the
	 * specified direction.
	 *
	 * @param pageId
	 *            the given page id
	 * @param direction
	 *            the specified direction, "up"/"down"
	 * @throws ServiceException
	 *             service exception
	 */
	public void changeOrder(final String pageId, final String direction) throws ServiceException {

		// final Transaction transaction = pageDao.beginTransaction();

		try {
			final JSONObject srcPage = pageDao.get(pageId);
			final int srcPageOrder = srcPage.getInt(Page.PAGE_ORDER);

			JSONObject targetPage;

			if ("up".equals(direction)) {
				targetPage = pageDao.getUpper(pageId);
			} else { // Down
				targetPage = pageDao.getUnder(pageId);
			}

			if (null == targetPage) {
				// if (transaction.isActive()) {
				// transaction.rollback();
				// }

				logger.warn("Cant not find the target page of source page[order={0}]", srcPageOrder);
				return;
			}

			// Swaps
			srcPage.put(Page.PAGE_ORDER, targetPage.getInt(Page.PAGE_ORDER));
			targetPage.put(Page.PAGE_ORDER, srcPageOrder);

			pageDao.update(srcPage.getString(Keys.OBJECT_ID), srcPage);
			pageDao.update(targetPage.getString(Keys.OBJECT_ID), targetPage);

			// transaction.commit();
		} catch (final Exception e) {
			// if (transaction.isActive()) {
			// transaction.rollback();
			// }

			logger.error("Changes page's order failed", e);

			throw new ServiceException(e);
		}
	}

	/**
	 * Removes page comments by the specified page id.
	 *
	 * <p>
	 * Removes related comments, sets page/blog comment statistic count.
	 * </p>
	 *
	 * @param pageId
	 *            the specified page id
	 * @throws JSONException
	 *             json exception
	 * @throws RepositoryException
	 *             repository exception
	 */
	private void removePageComments(final String pageId) throws JSONException, RepositoryException {
		final int removedCnt = commentDao.removeComments(pageId);

		int blogCommentCount = statisticQueryService.getBlogCommentCount();

		blogCommentCount -= removedCnt;
		statisticMgmtService.setBlogCommentCount(blogCommentCount);

		int publishedBlogCommentCount = statisticQueryService.getPublishedBlogCommentCount();

		publishedBlogCommentCount -= removedCnt;
		statisticMgmtService.setPublishedBlogCommentCount(publishedBlogCommentCount);
	}

	/**
	 * Processes comments for page update.
	 *
	 * @param page
	 *            the specified page to update
	 * @throws Exception
	 *             exception
	 */
	public void processCommentsForPageUpdate(final JSONObject page) throws Exception {
		final String pageId = page.getString(Keys.OBJECT_ID);

		final List<JSONObject> comments = commentDao.getComments(pageId, 1, Integer.MAX_VALUE);

		for (final JSONObject comment : comments) {
			final String commentId = comment.getString(Keys.OBJECT_ID);
			final String sharpURL = Comments.getCommentSharpURLForPage(page, commentId);

			comment.put(Comment.COMMENT_SHARP_URL, sharpURL);

			if (StringUtils.isBlank(comment.optString(Comment.COMMENT_ORIGINAL_COMMENT_ID))) {
				comment.put(Comment.COMMENT_ORIGINAL_COMMENT_ID, "");
			}
			if (StringUtils.isBlank(comment.optString(Comment.COMMENT_ORIGINAL_COMMENT_NAME))) {
				comment.put(Comment.COMMENT_ORIGINAL_COMMENT_NAME, "");
			}

			commentDao.update(commentId, comment);
		}
	}

	/**
	 * Sets the permalink query service with the specified permalink query
	 * service.
	 *
	 * @param permalinkQueryService
	 *            the specified permalink query service
	 */
	public void setPermalinkQueryService(final PermalinkQueryService permalinkQueryService) {
		this.permalinkQueryService = permalinkQueryService;
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

	/**
	 * Sets the preference query service with the specified preference query
	 * service.
	 *
	 * @param preferenceQueryService
	 *            the specified preference query service
	 */
	public void setPreferenceQueryService(final PreferenceQueryService preferenceQueryService) {
		this.preferenceQueryService = preferenceQueryService;
	}

	/**
	 * Sets the statistic query service with the specified statistic query
	 * service.
	 *
	 * @param statisticQueryService
	 *            the specified statistic query service
	 */
	public void setStatisticQueryService(final StatisticQueryService statisticQueryService) {
		this.statisticQueryService = statisticQueryService;
	}

	/**
	 * Sets the statistic management service with the specified statistic
	 * management service.
	 *
	 * @param statisticMgmtService
	 *            the specified statistic management service
	 */
	public void setStatisticMgmtService(final StatisticMgmtService statisticMgmtService) {
		this.statisticMgmtService = statisticMgmtService;
	}

	/**
	 * Sets the comment repository with the specified comment repository.
	 *
	 * @param commentDao
	 *            the specified comment repository
	 */
	public void setCommentRepository(final CommentDao commentDao) {
		this.commentDao = commentDao;
	}

	/**
	 * Sets the language service with the specified language service.
	 *
	 * @param langPropsService
	 *            the specified language service
	 */
	public void setLangPropsService(final LangPropsService langPropsService) {
		this.langPropsService = langPropsService;
	}
}
