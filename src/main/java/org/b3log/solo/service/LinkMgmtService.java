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


import org.b3log.solo.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.b3log.solo.frame.repository.Transaction;
import org.b3log.solo.frame.service.ServiceException;
import org.springframework.stereotype.Service;
import org.b3log.solo.dao.LinkDao;
import org.b3log.solo.model.Link;
import org.json.JSONObject;


/**
 * Link management service.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.0.0.1, Nov 2, 2011
 * @since 0.4.0
 */
@Service
public class LinkMgmtService {

    /**
     * Logger.
     */
    private static Logger logger = LoggerFactory.getLogger(LinkMgmtService.class);

    /**
     * Link repository.
     */
    @Autowired
    private LinkDao linkDao;

    /**
     * Removes a link specified by the given link id.
     *
     * @param linkId the given link id
     * @throws ServiceException service exception
     */
    public void removeLink(final String linkId)
        throws ServiceException {
//        final Transaction transaction = linkDao.beginTransaction();

        try {
            linkDao.remove(linkId);

//            transaction.commit();
        } catch (final Exception e) {
//            if (transaction.isActive()) {
//                transaction.rollback();
//            }

            logger.error("Removes a link[id=" + linkId + "] failed", e);
            throw new ServiceException(e);
        }
    }

    /**
     * Updates a link by the specified request json object.
     *
     * @param requestJSONObject the specified request json object, for example,
     * <pre>
     * {
     *     "link": {
     *         "oId": "",
     *         "linkTitle": "",
     *         "linkAddress": ""
     *     }
     * }, see {@link Link} for more details
     * </pre>
     * @throws ServiceException service exception
     */
    public void updateLink(final JSONObject requestJSONObject)
        throws ServiceException {
//        final Transaction transaction = linkDao.beginTransaction();

        try {
            final JSONObject link = requestJSONObject.getJSONObject(Link.LINK);
            final String linkId = link.getString(Keys.OBJECT_ID);
            final JSONObject oldLink = linkDao.get(linkId);

            link.put(Link.LINK_ORDER, oldLink.getInt(Link.LINK_ORDER));

            linkDao.update(linkId, link);

//            transaction.commit();
        } catch (final Exception e) {
//            if (transaction.isActive()) {
//                transaction.rollback();
//            }

            logger.error(e.getMessage(), e);

            throw new ServiceException(e);
        }
    }

    /**
     * Changes the order of a link specified by the given link id with the 
     * specified direction.
     *
     * @param linkId the given link id
     * @param direction the specified direction, "up"/"down"
     * @throws ServiceException service exception
     */
    public void changeOrder(final String linkId, final String direction)
        throws ServiceException {
//        final Transaction transaction = linkDao.beginTransaction();

        try {
            final JSONObject srcLink = linkDao.get(linkId);
            final int srcLinkOrder = srcLink.getInt(Link.LINK_ORDER);

            JSONObject targetLink = null;

            if ("up".equals(direction)) {
                targetLink = linkDao.getUpper(linkId);
            } else { // Down
                targetLink = linkDao.getUnder(linkId);
            }

            if (null == targetLink) {
//                if (transaction.isActive()) {
//                    transaction.rollback();
//                }

                logger.warn("Cant not find the target link of source link[order={0}]", srcLinkOrder);
                return;
            }

            // Swaps
            srcLink.put(Link.LINK_ORDER, targetLink.getInt(Link.LINK_ORDER));
            targetLink.put(Link.LINK_ORDER, srcLinkOrder);

            linkDao.update(srcLink.getString(Keys.OBJECT_ID), srcLink);
            linkDao.update(targetLink.getString(Keys.OBJECT_ID), targetLink);

//            transaction.commit();
        } catch (final Exception e) {
//            if (transaction.isActive()) {
//                transaction.rollback();
//            }

            logger.error("Changes link's order failed", e);

            throw new ServiceException(e);
        }
    }

    /**
     * Adds a link with the specified request json object.
     * 
     * @param requestJSONObject the specified request json object, for example,
     * <pre>
     * {
     *     "link": {
     *         "linkTitle": "",
     *         "linkAddress": "",
     *         "linkDescription": "" // optional
     *     }
     * }, see {@link Link} for more details
     * </pre>
     * @return generated link id
     * @throws ServiceException service exception
     */
    public String addLink(final JSONObject requestJSONObject)
        throws ServiceException {
//        final Transaction transaction = linkDao.beginTransaction();

        try {
            final JSONObject link = requestJSONObject.getJSONObject(Link.LINK);
            final int maxOrder = linkDao.getMaxOrder();

            link.put(Link.LINK_ORDER, maxOrder + 1);
            final String ret = linkDao.add(link);

//            transaction.commit();

            return ret;
        } catch (final Exception e) {
//            if (transaction.isActive()) {
//                transaction.rollback();
//            }

            logger.error("Adds a link failed", e);
            throw new ServiceException(e);
        }
    }

    /**
     * Sets the link repository with the specified link repository.
     * 
     * @param linkDao the specified link repository
     */
    public void setLinkRepository(final LinkDao linkDao) {
        this.linkDao = linkDao;
    }
}
