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

import org.b3log.solo.dao.ArchiveDateDao;
import org.b3log.solo.dao.repository.RepositoryException;
import org.b3log.solo.model.ArchiveDate;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Archive date query service.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.0.0.0, Feb 7, 2012
 * @since 0.4.0
 */
@Service
public class ArchiveDateQueryService {

	/**
	 * Logger.
	 */
	private static Logger logger = LoggerFactory.getLogger(ArchiveDateQueryService.class);

	/**
	 * Archive date repository.
	 */
	@Autowired
	private ArchiveDateDao archiveDateDao;

	/**
	 * Gets all archive dates.
	 * 
	 * @return a list of archive dates, returns an empty list if not found
	 * @throws ServiceException
	 *             service exception
	 */
	public List<JSONObject> getArchiveDates() throws ServiceException {
		try {
			return archiveDateDao.getArchiveDates();
		} catch (final RepositoryException e) {
			logger.error("Gets archive dates failed", e);
			throw new ServiceException("Gets archive dates failed");
		}
	}

	/**
	 * Gets an archive date by the specified archive date string.
	 * 
	 * @param archiveDateString
	 *            the specified archive date string (yyyy/MM)
	 * @return for example,
	 * 
	 *         <pre>
	 * {
	 *     "archiveDate": {
	 *         "oId": "",
	 *         "archiveTime": "",
	 *         "archiveDatePublishedArticleCount": int,
	 *         "archiveDateArticleCount": int
	 *     }
	 * }
	 *         </pre>
	 * 
	 *         , returns {@code null} if not found
	 * @throws ServiceException
	 *             service exception
	 */
	public JSONObject getByArchiveDateString(final String archiveDateString) throws ServiceException {
		final JSONObject ret = new JSONObject();

		try {
			final JSONObject archiveDate = archiveDateDao.getByArchiveDate(archiveDateString);

			if (null == archiveDate) {
				return null;
			}

			ret.put(ArchiveDate.ARCHIVE_DATE, archiveDate);

			return ret;
		} catch (final RepositoryException e) {
			logger.error("Gets archive date[string=" + archiveDateString + "] failed", e);
			throw new ServiceException("Gets archive date[string=" + archiveDateString + "] failed");
		}
	}

	/**
	 * Sets archive date repository with the specified archive date repository.
	 * 
	 * @param archiveDateDao
	 *            the specified archive date repository
	 */
	public void setArchiveDateRepository(final ArchiveDateDao archiveDateDao) {
		this.archiveDateDao = archiveDateDao;
	}
}
