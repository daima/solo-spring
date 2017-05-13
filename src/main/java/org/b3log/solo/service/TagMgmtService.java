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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.b3log.solo.frame.repository.RepositoryException;
import org.b3log.solo.frame.repository.Transaction;
import org.b3log.solo.frame.service.ServiceException;
import org.b3log.solo.dao.CategoryTagDao;
import org.b3log.solo.dao.TagDao;
import org.b3log.solo.model.Tag;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * Tag management service.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.0.0.2, Mar 31, 2017
 * @since 0.4.0
 */
@Service
public class TagMgmtService {

    /**
     * Logger.
     */
    private static Logger logger = LoggerFactory.getLogger(TagMgmtService.class);

    /**
     * Tag query service.
     */
    @Autowired
    private TagQueryService tagQueryService;

    /**
     * Tag repository.
     */
    @Autowired
    private TagDao tagDao;

    /**
     * Category-tag repository.
     */
    @Autowired
    private CategoryTagDao categoryTagDao;

    /**
     * Decrements reference count of every tag of an published article specified
     * by the given article id.
     *
     * @param articleId the given article id
     * @throws JSONException       json exception
     * @throws RepositoryException repository exception
     */
    public void decTagPublishedRefCount(final String articleId) throws JSONException, RepositoryException {
        final List<JSONObject> tags = tagDao.getByArticleId(articleId);

        for (final JSONObject tag : tags) {
            final String tagId = tag.getString(Keys.OBJECT_ID);
            final int refCnt = tag.getInt(Tag.TAG_REFERENCE_COUNT);

            tag.put(Tag.TAG_REFERENCE_COUNT, refCnt);
            final int publishedRefCnt = tag.getInt(Tag.TAG_PUBLISHED_REFERENCE_COUNT);

            tag.put(Tag.TAG_PUBLISHED_REFERENCE_COUNT, publishedRefCnt - 1);
            tagDao.update(tagId, tag);
        }
    }

    /**
     * Removes all unused tags.
     *
     * @throws ServiceException if get tags failed, or remove failed
     */
    public void removeUnusedTags() throws ServiceException {
//        final Transaction transaction = tagDao.beginTransaction();

        try {
            final List<JSONObject> tags = tagQueryService.getTags();

            for (int i = 0; i < tags.size(); i++) {
                final JSONObject tag = tags.get(i);
                final int tagRefCnt = tag.getInt(Tag.TAG_REFERENCE_COUNT);

                if (0 == tagRefCnt) {
                    final String tagId = tag.getString(Keys.OBJECT_ID);

                    categoryTagDao.removeByTagId(tagId);
                    tagDao.remove(tagId);
                }
            }

//            transaction.commit();
        } catch (final Exception e) {
//            if (transaction.isActive()) {
//                transaction.rollback();
//            }

            logger.error("Removes unused tags failed", e);

            throw new ServiceException(e);
        }
    }

    /**
     * Sets the tag repository with the specified tag repository.
     *
     * @param tagDao the specified tag repository
     */
    public void setTagRepository(final TagDao tagDao) {
        this.tagDao = tagDao;
    }

    /**
     * Sets the tag query service with the specified tag query service.
     *
     * @param tagQueryService the specified tag query service
     */
    public void setTagQueryService(final TagQueryService tagQueryService) {
        this.tagQueryService = tagQueryService;
    }
}
