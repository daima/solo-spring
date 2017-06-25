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

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.b3log.solo.Keys;
import org.b3log.solo.Latkes;
import org.b3log.solo.dao.PluginDao;
import org.b3log.solo.dao.repository.Query;
import org.b3log.solo.frame.plugin.PluginStatus;
import org.b3log.solo.model.Plugin;
import org.b3log.solo.module.event.AbstractPlugin;
import org.b3log.solo.module.plugin.PluginManager;
import org.b3log.solo.util.CollectionUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Plugin management service.
 *
 * @author <a href="http://cxy7.com">XyCai</a>
 * @version 1.0.0.0, Oct 27, 2011
 * @since 0.4.0
 */
@Service
public class PluginMgmtService {

	/**
	 * Logger.
	 */
	private static Logger logger = LoggerFactory.getLogger(PluginMgmtService.class);

	/**
	 * Plugin repository.
	 */
	@Autowired
	private PluginDao pluginRepository;

	/**
	 * Language service.
	 */
	@Autowired
	private LangPropsService langPropsService;

	/**
	 * Initialization service.
	 */
	@Autowired
	private InitService initService;

	/**
	 * Plugin manager.
	 */
	@Autowired
	private PluginManager pluginManager;

	/**
	 * Updates datastore plugin descriptions with the specified plugins.
	 * 
	 * @param plugins
	 *            the specified plugins
	 * @throws Exception
	 *             exception
	 */
	public void refresh(final List<AbstractPlugin> plugins) throws Exception {
		if (!initService.isInited()) {
			return;
		}

		final JSONObject result = pluginRepository.get(new Query());
		final JSONArray pluginArray = result.getJSONArray(Keys.RESULTS);
		final List<JSONObject> persistedPlugins = CollectionUtils.jsonArrayToList(pluginArray);

		try {
			// Reads plugin status from datastore and clear plugin datastore
			for (final JSONObject oldPluginDesc : persistedPlugins) {
				final String descId = oldPluginDesc.getString(Keys.OBJECT_ID);
				final AbstractPlugin plugin = get(plugins, descId);

				pluginRepository.remove(descId);

				if (null != plugin) {
					final String status = oldPluginDesc.getString(Plugin.PLUGIN_STATUS);
					final String setting = oldPluginDesc.optString(Plugin.PLUGIN_SETTING);

					plugin.setStatus(PluginStatus.valueOf(status));
					try {
						if (StringUtils.isNotBlank(setting)) {
							plugin.setSetting(new JSONObject(setting));
						}
					} catch (final JSONException e) {
						logger.warn("the formatter of the old config failed to convert to json", e);
					}
				}
			}

			// Adds these plugins into datastore
			for (final AbstractPlugin plugin : plugins) {
				final JSONObject pluginDesc = plugin.toJSONObject();

				pluginRepository.add(pluginDesc);

				logger.trace("Refreshed plugin[{}]", pluginDesc);
			}

		} catch (final Exception e) {
			logger.error("Refresh plugins failed", e);
		}
	}

	/**
	 * Gets a plugin in the specified plugins with the specified id.
	 * 
	 * @param plugins
	 *            the specified plugins
	 * @param id
	 *            the specified id, must NOT be {@code null}
	 * @return a plugin, returns {@code null} if not found
	 */
	private AbstractPlugin get(final List<AbstractPlugin> plugins, final String id) {
		if (null == id) {
			throw new IllegalArgumentException("id must not be null");
		}

		for (final AbstractPlugin plugin : plugins) {
			if (id.equals(plugin.getId())) {
				return plugin;
			}
		}

		return null;
	}

	/**
	 * Sets a plugin's status with the specified plugin id, status.
	 * 
	 * @param pluginId
	 *            the specified plugin id
	 * @param status
	 *            the specified status, see {@link PluginStatus}
	 * @return for example,
	 * 
	 *         <pre>
	 * {
	 *     "sc": boolean,
	 *     "msg": "" 
	 * }
	 *         </pre>
	 */
	public JSONObject setPluginStatus(final String pluginId, final String status) {
		final Map<String, String> langs = langPropsService.getAll(Latkes.getLocale());

		final List<AbstractPlugin> plugins = pluginManager.getPlugins();

		final JSONObject ret = new JSONObject();

		for (final AbstractPlugin plugin : plugins) {
			if (plugin.getId().equals(pluginId)) {
				// final Transaction transaction =
				// pluginRepository.beginTransaction();

				try {
					plugin.setStatus(PluginStatus.valueOf(status));

					pluginRepository.update(pluginId, plugin.toJSONObject());

					// transaction.commit();

					plugin.changeStatus();

					ret.put(Keys.STATUS_CODE, true);
					ret.put(Keys.MSG, langs.get("setSuccLabel"));

					return ret;
				} catch (final Exception e) {
					// if (transaction.isActive()) {
					// transaction.rollback();
					// }

					logger.error("Set plugin status error", e);

					ret.put(Keys.STATUS_CODE, false);
					ret.put(Keys.MSG, langs.get("setFailLabel"));

					return ret;
				}
			}
		}

		ret.put(Keys.STATUS_CODE, false);
		ret.put(Keys.MSG, langs.get("refreshAndRetryLabel"));

		return ret;
	}

	/**
	 * updatePluginSetting.
	 * 
	 * @param pluginId
	 *            the specified pluginoId
	 * @param setting
	 *            the specified setting
	 * @return the ret json
	 */
	public JSONObject updatePluginSetting(final String pluginId, final String setting) {

		final Map<String, String> langs = langPropsService.getAll(Latkes.getLocale());

		final List<AbstractPlugin> plugins = pluginManager.getPlugins();

		final JSONObject ret = new JSONObject();

		for (final AbstractPlugin plugin : plugins) {
			if (plugin.getId().equals(pluginId)) {
				// final Transaction transaction =
				// pluginRepository.beginTransaction();

				try {
					final JSONObject pluginJson = plugin.toJSONObject();

					pluginJson.put(Plugin.PLUGIN_SETTING, setting);
					pluginRepository.update(pluginId, pluginJson);

					// transaction.commit();

					ret.put(Keys.STATUS_CODE, true);
					ret.put(Keys.MSG, langs.get("setSuccLabel"));

					return ret;
				} catch (final Exception e) {
					// if (transaction.isActive()) {
					// transaction.rollback();
					// }
					logger.error("Set plugin status error", e);
					ret.put(Keys.STATUS_CODE, false);
					ret.put(Keys.MSG, langs.get("setFailLabel"));

					return ret;
				}
			}
		}

		ret.put(Keys.STATUS_CODE, false);
		ret.put(Keys.MSG, langs.get("refreshAndRetryLabel"));

		return ret;

	}

	/**
	 * Sets the plugin repository with the specified plugin repository.
	 * 
	 * @param pluginRepository
	 *            the specified plugin repository
	 */
	public void setPluginRepository(final PluginDao pluginRepository) {
		this.pluginRepository = pluginRepository;
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
