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
import org.b3log.solo.dao.OptionDao;
import org.b3log.solo.frame.repository.FilterOperator;
import org.b3log.solo.frame.repository.PropertyFilter;
import org.b3log.solo.frame.repository.Query;
import org.b3log.solo.frame.repository.RepositoryException;
import org.b3log.solo.frame.service.ServiceException;
import org.b3log.solo.model.Option;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Preference query service.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.1.0.2, Dec 13, 2015
 * @since 0.4.0
 */
@Service
public class PreferenceQueryService {

	/**
	 * Logger.
	 */
	private static Logger logger = LoggerFactory.getLogger(PreferenceQueryService.class);

	/**
	 * Option repository.
	 */
	@Autowired
	private OptionDao optionRepository;

	/**
	 * Gets the reply notification template.
	 *
	 * @return reply notification template, returns {@code null} if not found
	 * @throws ServiceException
	 *             service exception
	 */
	public JSONObject getReplyNotificationTemplate() throws ServiceException {
		try {
			final JSONObject ret = new JSONObject();
			final JSONObject preference = getPreference();

			ret.put("subject", preference.optString(Option.ID_C_REPLY_NOTI_TPL_SUBJECT));
			ret.put("body", preference.optString(Option.ID_C_REPLY_NOTI_TPL_BODY));

			return ret;
		} catch (final Exception e) {
			logger.error("Updates reply notification template failed", e);
			throw new ServiceException(e);
		}
	}

	/**
	 * Gets the user preference.
	 *
	 * @return user preference, returns {@code null} if not found
	 * @throws ServiceException
	 *             if repository exception
	 */
	public JSONObject getPreference() throws ServiceException {
		try {
			final JSONObject checkInit = optionRepository.get(Option.ID_C_ADMIN_EMAIL);
			if (null == checkInit) {
				return null;
			}

			final Query query = new Query();
			query.setFilter(
					new PropertyFilter(Option.OPTION_CATEGORY, FilterOperator.EQUAL, Option.CATEGORY_C_PREFERENCE));
			final JSONArray opts = optionRepository.get(query).optJSONArray(Keys.RESULTS);

			final JSONObject ret = new JSONObject();
			for (int i = 0; i < opts.length(); i++) {
				final JSONObject opt = opts.optJSONObject(i);

				ret.put(opt.optString(Keys.OBJECT_ID), opt.opt(Option.OPTION_VALUE));
			}

			return ret;
		} catch (final RepositoryException e) {
			return null;
		}
	}

	public void setOptionRepository(OptionDao optionRepository) {
		this.optionRepository = optionRepository;
	}

}
