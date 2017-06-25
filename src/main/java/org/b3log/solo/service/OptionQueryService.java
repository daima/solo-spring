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

import org.b3log.solo.Keys;
import org.b3log.solo.dao.OptionDao;
import org.b3log.solo.dao.repository.FilterOperator;
import org.b3log.solo.dao.repository.PropertyFilter;
import org.b3log.solo.dao.repository.Query;
import org.b3log.solo.dao.repository.RepositoryException;
import org.b3log.solo.model.Option;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Option query service.
 *
 * @author <a href="http://cxy7.com">XyCai</a>
 * @version 1.0.0.0, Apr 16, 2013
 * @since 0.6.0
 */
@Service
public class OptionQueryService {

	/**
	 * Option repository.
	 */
	@Autowired
	private OptionDao optionRepository;

	/**
	 * Gets an option with the specified option id.
	 * 
	 * @param optionId
	 *            the specified option id
	 * @return an option, returns {@code null} if not found
	 * @throws ServiceException
	 *             service exception
	 */
	public JSONObject getOptionById(final String optionId) throws ServiceException {
		try {
			return optionRepository.get(optionId);
		} catch (final RepositoryException e) {
			throw new ServiceException(e);
		}
	}

	/**
	 * Gets options with the specified category.
	 * 
	 * <p>
	 * All options with the specified category will be merged into one json
	 * object as the return value.
	 * </p>
	 * 
	 * @param category
	 *            the specified category
	 * @return all options with the specified category, for example,
	 * 
	 *         <pre>
	 * {
	 *     "${optionId}": "${optionValue}",
	 *     ....
	 * }
	 *         </pre>
	 * 
	 *         , returns {@code null} if not found
	 * @throws ServiceException
	 *             service exception
	 */
	public JSONObject getOptions(final String category) throws ServiceException {
		final Query query = new Query();

		query.setFilter(new PropertyFilter(Option.OPTION_CATEGORY, FilterOperator.EQUAL, category));

		try {
			final JSONObject result = optionRepository.get(query);
			final JSONArray options = result.getJSONArray(Keys.RESULTS);

			if (0 == options.length()) {
				return null;
			}

			final JSONObject ret = new JSONObject();

			for (int i = 0; i < options.length(); i++) {
				final JSONObject option = options.getJSONObject(i);

				ret.put(option.getString(Keys.OBJECT_ID), option.getString(Option.OPTION_VALUE));
			}

			return ret;
		} catch (final Exception e) {
			throw new ServiceException(e);
		}
	}

	/**
	 * Sets the option repository with the specified option repository.
	 * 
	 * @param optionRepository
	 *            the specified option repository
	 */
	public void setOptionRepository(final OptionDao optionRepository) {
		this.optionRepository = optionRepository;
	}
}
