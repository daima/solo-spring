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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.b3log.solo.module.event.AbstractPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Plugin loader.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.0.2.2, May 31, 2014
 */
@Component
public class PluginManager {

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

			// load();
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

			// load();
		}

		final Set<AbstractPlugin> ret = pluginCache.get(viewName);

		if (null == ret) {
			return Collections.emptySet();
		}

		return ret;
	}

	/**
	 * Loads plugins from directory {@literal webRoot/plugins/}.
	 *//*
		 * public void load() { Stopwatchs.start("Load Plugins");
		 * 
		 * classLoaders.clear();
		 * 
		 * final ServletContext servletContext =
		 * ContextLoader.getCurrentWebApplicationContext().getServletContext();
		 * 
		 * @SuppressWarnings("unchecked") final Set<String> pluginDirPaths =
		 * servletContext.getResourcePaths("/plugins");
		 * 
		 * final List<AbstractPlugin> plugins = new ArrayList<AbstractPlugin>();
		 * 
		 * if (null != pluginDirPaths) { for (final String pluginDirPath :
		 * pluginDirPaths) { try {
		 * logger.info("Loading plugin under directory[{0}]", pluginDirPath);
		 * 
		 * final AbstractPlugin plugin = load(pluginDirPath, pluginCache);
		 * 
		 * if (plugin != null) { plugins.add(plugin); } } catch (final Exception
		 * e) { logger.warn("Load plugin under directory[" + pluginDirPath +
		 * "] failed", e); } } }
		 * 
		 * try { pluginRefresher.action(new
		 * Event<List<AbstractPlugin>>(PLUGIN_LOADED_EVENT, plugins)); //
		 * eventManager.fireEventSynchronously(new
		 * Event<List<AbstractPlugin>>(PLUGIN_LOADED_EVENT, plugins)); } catch
		 * (final EventException e) { throw new
		 * RuntimeException("Plugin load error", e); }
		 * 
		 * Stopwatchs.end(); }
		 */

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
	 *//*
		 * private AbstractPlugin load(final String pluginDirPath, final
		 * Map<String, HashSet<AbstractPlugin>> holder) throws Exception { final
		 * Properties props = new Properties(); final ServletContext
		 * servletContext =
		 * ContextLoader.getCurrentWebApplicationContext().getServletContext();
		 * 
		 * String plugin = StringUtils.substringAfter(pluginDirPath,
		 * "/plugins");
		 * 
		 * plugin = plugin.replace("/", "");
		 * 
		 * final File file = Latkes.getWebFile("/plugins/" + plugin +
		 * "/plugin.properties");
		 * 
		 * props.load(new FileInputStream(file));
		 * 
		 * final URL defaultClassesFileDirURL =
		 * servletContext.getResource("/plugins/" + plugin + "classes");
		 * 
		 * URL classesFileDirURL = null;
		 * 
		 * try { classesFileDirURL =
		 * servletContext.getResource(props.getProperty("classesDirPath")); }
		 * catch (final MalformedURLException e) { logger.error("Reads [" +
		 * props.getProperty("classesDirPath") + "] failed", e); }
		 * 
		 * final URLClassLoader classLoader = new URLClassLoader(new URL[] {
		 * defaultClassesFileDirURL, classesFileDirURL},
		 * PluginManager.class.getClassLoader());
		 * 
		 * classLoaders.add(classLoader);
		 * 
		 * String pluginClassName = props.getProperty(Plugin.PLUGIN_CLASS);
		 * 
		 * if (StringUtils.isBlank(pluginClassName)) { pluginClassName =
		 * NotInteractivePlugin.class.getName(); }
		 * 
		 * final String rendererId =
		 * props.getProperty(Plugin.PLUGIN_RENDERER_ID);
		 * 
		 * if (StringUtils.isBlank(rendererId)) {
		 * logger.warn("no renderer defined by this plugin[" + plugin +
		 * "]，this plugin will be ignore!"); return null; }
		 * 
		 * final Class<?> pluginClass = classLoader.loadClass(pluginClassName);
		 * 
		 * logger.trace("Loading plugin class[name={0}]", pluginClassName);
		 * final AbstractPlugin ret = (AbstractPlugin)
		 * pluginClass.newInstance();
		 * 
		 * ret.setRendererId(rendererId);
		 * 
		 * setPluginProps(plugin, ret, props);
		 * 
		 * registerEventListeners(props, classLoader, ret);
		 * 
		 * register(ret, holder);
		 * 
		 * ret.changeStatus();
		 * 
		 * return ret; }
		 */

	/**
	 * Registers event listeners with the specified plugin properties, class
	 * loader and plugin.
	 *
	 * @param props
	 *            the specified plugin properties
	 * @param classLoader
	 *            the specified class loader
	 * @param plugin
	 *            the specified plugin
	 * @throws Exception
	 *             exception
	 */
	/*
	 * private void registerEventListeners(final Properties props, final
	 * URLClassLoader classLoader, final AbstractPlugin plugin) throws Exception
	 * { final String eventListenerClasses =
	 * props.getProperty(Plugin.PLUGIN_EVENT_LISTENER_CLASSES); final String[]
	 * eventListenerClassArray = eventListenerClasses.split(",");
	 * 
	 * for (int i = 0; i < eventListenerClassArray.length; i++) { final String
	 * eventListenerClassName = eventListenerClassArray[i];
	 * 
	 * if (StringUtils.isBlank(eventListenerClassName)) {
	 * logger.info("No event listener to load for plugin[name={0}]",
	 * plugin.getName()); return; }
	 * 
	 * logger.debug( "Loading event listener[className={0}]",
	 * eventListenerClassName);
	 * 
	 * final Class<?> eventListenerClass =
	 * classLoader.loadClass(eventListenerClassName);
	 * 
	 * final AbstractEventListener<?> eventListener = (AbstractEventListener)
	 * eventListenerClass.newInstance();
	 * 
	 * plugin.addEventListener(eventListener);
	 * 
	 * logger.debug(
	 * "Registered event listener[class={0}, eventType={1}] for plugin[name={2}]"
	 * , new Object[] {eventListener.getClass(), eventListener.getEventType(),
	 * plugin.getName()}); } }
	 */

	/**
	 * Gets the plugin class loaders.
	 *
	 * @return plugin class loaders
	 */
	public Set<ClassLoader> getClassLoaders() {
		return Collections.unmodifiableSet(classLoaders);
	}
}
