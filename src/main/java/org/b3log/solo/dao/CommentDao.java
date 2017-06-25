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
package org.b3log.solo.dao;

import java.util.Iterator;
import java.util.List;

import org.b3log.solo.Keys;
import org.b3log.solo.dao.repository.FilterOperator;
import org.b3log.solo.dao.repository.PropertyFilter;
import org.b3log.solo.dao.repository.Query;
import org.b3log.solo.dao.repository.RepositoryException;
import org.b3log.solo.dao.repository.SortDirection;
import org.b3log.solo.model.Article;
import org.b3log.solo.model.Comment;
import org.b3log.solo.util.CollectionUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * Comment repository.
 *
 * @author <a href="http://cxy7.com">XyCai</a>
 * @version 1.0.0.8, Oct 18, 2011
 * @since 0.3.1
 */
@Repository
public class CommentDao extends AbstractBlogDao {

	/**
	 * Logger.
	 */
	private static Logger logger = LoggerFactory.getLogger(CommentDao.class);

	/**
	 * Article repository.
	 */
	@Autowired
	private ArticleDao articleDao;

	@Override
	public String getTableNamePostfix() {
		return Comment.COMMENT;
	}

	public int removeComments(final String onId) throws RepositoryException {
		final List<JSONObject> comments = getComments(onId, 1, Integer.MAX_VALUE);

		for (final JSONObject comment : comments) {
			final String commentId = comment.optString(Keys.OBJECT_ID);

			remove(commentId);
		}

		logger.debug("Removed comments[onId={}, removedCnt={}]", onId, comments.size());

		return comments.size();
	}

	public List<JSONObject> getComments(final String onId, final int currentPageNum, final int pageSize)
			throws RepositoryException {
		final Query query = new Query().addSort(Keys.OBJECT_ID, SortDirection.DESCENDING)
				.setFilter(new PropertyFilter(Comment.COMMENT_ON_ID, FilterOperator.EQUAL, onId))
				.setCurrentPageNum(currentPageNum).setPageSize(pageSize).setPageCount(1);

		final JSONObject result = get(query);

		final JSONArray array = result.optJSONArray(Keys.RESULTS);

		return CollectionUtils.jsonArrayToList(array);
	}

	public List<JSONObject> getRecentComments(final int num) throws RepositoryException {
		final Query query = new Query().addSort(Keys.OBJECT_ID, SortDirection.DESCENDING).setCurrentPageNum(1)
				.setPageSize(num).setPageCount(1);

		List<JSONObject> ret;
		final JSONObject result = get(query);

		final JSONArray array = result.optJSONArray(Keys.RESULTS);

		ret = CollectionUtils.jsonArrayToList(array);

		// Removes unpublished article related comments
		removeForUnpublishedArticles(ret);

		return ret;
	}

	/**
	 * Removes comments of unpublished articles for the specified comments.
	 *
	 * @param comments
	 *            the specified comments
	 * @throws RepositoryException
	 *             repository exception
	 */
	private void removeForUnpublishedArticles(final List<JSONObject> comments) throws RepositoryException {
		logger.debug("Removing unpublished articles' comments....");
		final Iterator<JSONObject> iterator = comments.iterator();

		while (iterator.hasNext()) {
			final JSONObject comment = iterator.next();
			final String commentOnType = comment.optString(Comment.COMMENT_ON_TYPE);

			if (Article.ARTICLE.equals(commentOnType)) {
				final String articleId = comment.optString(Comment.COMMENT_ON_ID);

				if (!articleDao.isPublished(articleId)) {
					iterator.remove();
				}
			}
		}

		logger.debug("Removed unpublished articles' comments....");
	}

	/**
	 * Sets the article repository with the specified article repository.
	 * 
	 * @param articleDao
	 *            the specified article repository
	 */
	public void setArticleDao(final ArticleDao articleDao) {
		this.articleDao = articleDao;
	}
}
