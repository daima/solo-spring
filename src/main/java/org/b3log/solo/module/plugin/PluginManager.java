/*
 * Copyright (c) 2009-2016, b3log.org & hacpai.com
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
package org.b3log.solo.module.plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;

import javax.servlet.ServletContext;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.b3log.solo.Keys;
import org.b3log.solo.Latkes;
import org.b3log.solo.frame.event.Event;
import org.b3log.solo.frame.event.EventException;
import org.b3log.solo.frame.plugin.PluginType;
import org.b3log.solo.model.Plugin;
import org.b3log.solo.module.event.AbstractPlugin;
import org.b3log.solo.module.event.PluginRefresher;
import org.b3log.solo.util.PropsUtil;
import org.b3log.solo.util.Stopwatchs;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.ContextLoader;

/**
 * Plugin loader.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.0.2.2, May 31, 2014
 */
@Component
public class PluginManager {
	@Autowired
	private PluginRefresher pluginRefresher;

	/**
	 * Logger.
	 */
	private static Logger logger = LoggerFactory.getLogger(PluginManager.class);

	/**
	 * Type of loaded event.
	 */
	public static final String PLUGIN_LOADED_EVENT = "pluginLoadedEvt";

	/**
	 * Plugins cache.
	 *
	 * <p>
	 * Caches plugins with the key "plugins" and its value is the real holder, a
	 * map: &lt;"hosting view name", plugins&gt;
	 * </p>
	 */
	private Map<String, HashSet<AbstractPlugin>> pluginCache = new HashMap<>();

	/**
	 * Plugin class loaders.
	 */
	private Set<ClassLoader> classLoaders = new HashSet<>();

	/**
	 * Gets all plugins.
	 *
	 * @return all plugins, returns an empty list if not found
	 */
	public List<AbstractPlugin> getPlugins() {
		if (pluginCache.isEmpty()) {
			logger.info("Plugin cache miss, reload");

			load();
		}

		final List<AbstractPlugin> ret = new ArrayList<>();

		for (final Map.Entry<String, HashSet<AbstractPlugin>> entry : pluginCache.entrySet()) {
			ret.addAll(entry.getValue());
		}

		return ret;
	}

	/**
	 * Gets a plugin by the specified view name.
	 *
	 * @param viewName
	 *            the specified view name
	 * @return a plugin, returns an empty list if not found
	 */
	public Set<AbstractPlugin> getPlugins(final String viewName) {
		if (pluginCache.isEmpty()) {
			logger.info("Plugin cache miss, reload");

			load();
		}

		final Set<AbstractPlugin> ret = pluginCache.get(viewName);

		if (null == ret) {
			return Collections.emptySet();
		}

		return ret;
	}

	/**
	 * Loads plugins from directory {@literal webRoot/plugins/}.
	 */
	public void load() {
		Stopwatchs.start("Load Plugins");

		classLoaders.clear();

		final ServletContext servletContext = ContextLoader.getCurrentWebApplicationContext().getServletContext();

		final Set<String> pluginDirPaths = servletContext.getResourcePaths("/plugins");

		final List<AbstractPlugin> plugins = new ArrayList<AbstractPlugin>();

		if (null != pluginDirPaths) {
			for (final String pluginDirPath : pluginDirPaths) {
				try {
					logger.info("Loading plugin under directory[{}]", pluginDirPath);

					final AbstractPlugin plugin = load(pluginDirPath, pluginCache);

					if (plugin != null) {
						plugins.add(plugin);
					}
				} catch (final Exception e) {
					logger.warn("Load plugin under directory[" + pluginDirPath + "] failed", e);
				}
			}
		}

		try {
			pluginRefresher.action(new Event<List<AbstractPlugin>>(PLUGIN_LOADED_EVENT, plugins)); //
		} catch (final EventException e) {
			throw new RuntimeException("Plugin load error", e);
		}

		Stopwatchs.end();
	}

	/**
	 * Loads a plugin by the specified plugin directory and put it into the
	 * specified holder.
	 *
	 * @param pluginDirPath
	 *            the specified plugin directory
	 * @param holder
	 *            the specified holder
	 * @return loaded plugin
	 * @throws Exception
	 *             exception
	 */
	private AbstractPlugin load(final String pluginDirPath, final Map<String, HashSet<AbstractPlugin>> holder)
			throws Exception {
		final ServletContext servletContext = ContextLoader.getCurrentWebApplicationContext().getServletContext();

		String plugin = StringUtils.substringAfter(pluginDirPath, "/plugins");

		plugin = plugin.replace("/", "");

		final File file = Latkes.getWebFile("/plugins/" + plugin + "/plugin.properties");

		PropsUtil.loadFromInputStream(new FileInputStream(file));

		final URL defaultClassesFileDirURL = servletContext.getResource("/plugins/" + plugin + "classes");

		URL classesFileDirURL = null;

		try {
			classesFileDirURL = servletContext.getResource(PropsUtil.getProperty("classesDirPath"));
		} catch (final MalformedURLException e) {
			logger.error("Reads [" + PropsUtil.getProperty("classesDirPath") + "] failed", e);
		}

		final URLClassLoader classLoader = new URLClassLoader(new URL[] { defaultClassesFileDirURL, classesFileDirURL },
				PluginManager.class.getClassLoader());

		classLoaders.add(classLoader);

		String pluginClassName = PropsUtil.getProperty(Plugin.PLUGIN_CLASS);

		if (StringUtils.isBlank(pluginClassName)) {
			pluginClassName = NotInteractivePlugin.class.getName();
		}

		final String rendererId = PropsUtil.getProperty(Plugin.PLUGIN_RENDERER_ID);

		if (StringUtils.isBlank(rendererId)) {
			logger.warn("no renderer defined by this plugin[" + plugin + "]，this plugin will be ignore!");
			return null;
		}

		final Class<?> pluginClass = classLoader.loadClass(pluginClassName);

		logger.trace("Loading plugin class[name={}]", pluginClassName);
		final AbstractPlugin ret = (AbstractPlugin) pluginClass.newInstance();

		ret.setRendererId(rendererId);

		setPluginProps(plugin, ret);

		register(ret, holder);

		ret.changeStatus();

		return ret;
	}

	/**
	 * Registers the specified plugin into the specified holder.
	 *
	 * @param plugin
	 *            the specified plugin
	 * @param holder
	 *            the specified holder
	 */
	private void register(final AbstractPlugin plugin, final Map<String, HashSet<AbstractPlugin>> holder) {

		final String rendererId = plugin.getRendererId();

		/**
		 * the rendererId support multiple,using ';' to split. and using Map to
		 * match the plugin is not flexible, a regular expression match pattern
		 * may be needed in futrue.
		 */
		final String[] redererIds = rendererId.split(";");

		for (String rid : redererIds) {

			HashSet<AbstractPlugin> set = holder.get(rid);

			if (null == set) {
				set = new HashSet<AbstractPlugin>();
				holder.put(rid, set);
			}
			set.add(plugin);
		}

		logger.debug("Registered plugin[name={}, version={}] for rendererId[name={}], [{}] plugins totally",
				new Object[] { plugin.getName(), plugin.getVersion(), rendererId, holder.size() });
	}

	/**
	 * Sets the specified plugin's properties from the specified properties file
	 * under the specified plugin directory.
	 *
	 * @param pluginDirName
	 *            the specified plugin directory
	 * @param plugin
	 *            the specified plugin
	 * @param props
	 *            the specified properties file
	 * @throws Exception
	 *             exception
	 */
	private static void setPluginProps(final String pluginDirName, final AbstractPlugin plugin) throws Exception {
		final String author = PropsUtil.getProperty(Plugin.PLUGIN_AUTHOR);
		final String name = PropsUtil.getProperty(Plugin.PLUGIN_NAME);
		final String version = PropsUtil.getProperty(Plugin.PLUGIN_VERSION);
		final String types = PropsUtil.getProperty(Plugin.PLUGIN_TYPES);

		logger.trace("Plugin[name={}, author={}, version={}, types={}]", new Object[] { name, author, version, types });

		plugin.setAuthor(author);
		plugin.setName(name);
		plugin.setId(name + "_" + version);
		plugin.setVersion(version);
		plugin.setDir(pluginDirName);
		plugin.readLangs();

		// try to find the setting config.json
		final File settingFile = Latkes.getWebFile("/plugins/" + pluginDirName + "/config.json");

		if (null != settingFile && settingFile.exists()) {
			try {
				final String config = FileUtils.readFileToString(settingFile);
				final JSONObject jsonObject = new JSONObject(config);

				plugin.setSetting(jsonObject);
			} catch (final IOException ie) {
				logger.error("reading the config of the plugin[" + name + "]  failed", ie);
			} catch (final JSONException e) {
				logger.error("convert the  config of the plugin[" + name + "] to json failed", e);
			}
		}

		final String[] typeArray = types.split(",");

		for (int i = 0; i < typeArray.length; i++) {
			final PluginType type = PluginType.valueOf(typeArray[i]);

			plugin.addType(type);
		}
	}

	/**
	 * Gets the plugin class loaders.
	 *
	 * @return plugin class loaders
	 */
	public Set<ClassLoader> getClassLoaders() {
		return Collections.unmodifiableSet(classLoaders);
	}
}
