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

import java.util.Iterator;
import java.util.List;

import org.b3log.solo.Keys;
import org.b3log.solo.dao.TagDao;
import org.b3log.solo.dao.repository.Query;
import org.b3log.solo.dao.repository.RepositoryException;
import org.b3log.solo.dao.repository.SortDirection;
import org.b3log.solo.model.Tag;
import org.b3log.solo.util.CollectionUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Tag query service.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.1.0.3, Dec 17, 2015
 * @since 0.4.0
 */
@Service
public class TagQueryService {

	/**
	 * Logger.
	 */
	private static Logger logger = LoggerFactory.getLogger(TagQueryService.class);

	/**
	 * Tag repository.
	 */
	@Autowired
	private TagDao tagDao;

	/**
	 * Gets a tag by the specified tag title.
	 *
	 * @param tagTitle
	 *            the specified tag title
	 * @return for example,
	 * 
	 *         <pre>
	 * {
	 *     "tag": {
	 *         "oId": "",
	 *         "tagTitle": "",
	 *         "tagReferenceCount": int,
	 *         "tagPublishedRefCount": int
	 *     }
	 * }
	 *         </pre>
	 * 
	 *         , returns {@code null} if not found
	 *
	 * @throws ServiceException
	 *             service exception
	 */
	public JSONObject getTagByTitle(final String tagTitle) throws ServiceException {
		try {
			final JSONObject ret = new JSONObject();

			final JSONObject tag = tagDao.getByTitle(tagTitle);

			if (null == tag) {
				return null;
			}

			ret.put(Tag.TAG, tag);

			logger.debug("Got an tag[title={0}]", tagTitle);

			return ret;
		} catch (final RepositoryException e) {
			logger.error("Gets an article failed", e);
			throw new ServiceException(e);
		}
	}

	/**
	 * Gets the count of tags.
	 *
	 * @return count of tags
	 * @throws ServiceException
	 *             service exception
	 */
	public long getTagCount() throws ServiceException {
		try {
			return tagDao.count();
		} catch (final RepositoryException e) {
			logger.error("Gets tags failed", e);

			throw new ServiceException(e);
		}
	}

	/**
	 * Gets all tags.
	 *
	 * @return for example,
	 * 
	 *         <pre>
	 * [
	 *     {"tagTitle": "", "tagReferenceCount": int, ....},
	 *     ....
	 * ]
	 *         </pre>
	 * 
	 *         , returns an empty list if not found
	 *
	 * @throws ServiceException
	 *             service exception
	 */
	public List<JSONObject> getTags() throws ServiceException {
		try {
			final Query query = new Query().setPageCount(1);

			final JSONObject result = tagDao.get(query);
			final JSONArray tagArray = result.optJSONArray(Keys.RESULTS);

			return CollectionUtils.jsonArrayToList(tagArray);
		} catch (final RepositoryException e) {
			logger.error("Gets tags failed", e);

			throw new ServiceException(e);
		}
	}

	/**
	 * Gets top (reference count descending) tags.
	 *
	 * @param fetchSize
	 *            the specified fetch size
	 * @return for example,
	 * 
	 *         <pre>
	 * [
	 *     {"tagTitle": "", "tagReferenceCount": int, ....},
	 *     ....
	 * ]
	 *         </pre>
	 * 
	 *         , returns an empty list if not found
	 *
	 * @throws ServiceException
	 *             service exception
	 */
	public List<JSONObject> getTopTags(final int fetchSize) throws ServiceException {
		try {
			final Query query = new Query().setPageCount(1).setPageSize(fetchSize)
					.addSort(Tag.TAG_PUBLISHED_REFERENCE_COUNT, SortDirection.DESCENDING);

			final JSONObject result = tagDao.get(query);
			final JSONArray tagArray = result.optJSONArray(Keys.RESULTS);

			return CollectionUtils.jsonArrayToList(tagArray);
		} catch (final RepositoryException e) {
			logger.error("Gets top tags failed", e);

			throw new ServiceException(e);
		}
	}

	/**
	 * Gets bottom (reference count ascending) tags.
	 *
	 * @param fetchSize
	 *            the specified fetch size
	 * @return for example,
	 * 
	 *         <pre>
	 * [
	 *     {"tagTitle": "", "tagReferenceCount": int, ....},
	 *     ....
	 * ]
	 *         </pre>
	 * 
	 *         , returns an empty list if not found
	 *
	 * @throws ServiceException
	 *             service exception
	 */
	public List<JSONObject> getBottomTags(final int fetchSize) throws ServiceException {
		try {
			final Query query = new Query().setPageCount(1).setPageSize(fetchSize)
					.addSort(Tag.TAG_PUBLISHED_REFERENCE_COUNT, SortDirection.ASCENDING);

			final JSONObject result = tagDao.get(query);
			final JSONArray tagArray = result.optJSONArray(Keys.RESULTS);

			return CollectionUtils.jsonArrayToList(tagArray);
		} catch (final RepositoryException e) {
			logger.error("Gets bottom tags failed", e);

			throw new ServiceException(e);
		}
	}

	/**
	 * Removes tags of unpublished articles from the specified tags.
	 *
	 * @param tags
	 *            the specified tags
	 * @throws JSONException
	 *             json exception
	 * @throws RepositoryException
	 *             repository exception
	 */
	public void removeForUnpublishedArticles(final List<JSONObject> tags) throws JSONException, RepositoryException {
		final Iterator<JSONObject> iterator = tags.iterator();

		while (iterator.hasNext()) {
			final JSONObject tag = iterator.next();

			if (0 == tag.getInt(Tag.TAG_PUBLISHED_REFERENCE_COUNT)) {
				iterator.remove();
			}
		}
	}

	/**
	 * Sets the tag repository with the specified tag repository.
	 *
	 * @param tagDao
	 *            the specified tag repository
	 */
	public void setTagRepository(final TagDao tagDao) {
		this.tagDao = tagDao;
	}
}
