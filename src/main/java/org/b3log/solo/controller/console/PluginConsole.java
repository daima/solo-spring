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
package org.b3log.solo.controller.console;


import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.b3log.solo.Keys;
import org.b3log.solo.Latkes;
import org.b3log.solo.controller.renderer.ConsoleRenderer;
import org.b3log.solo.frame.logging.Level;
import org.b3log.solo.frame.logging.Logger;
import org.b3log.solo.frame.model.Plugin;
import org.b3log.solo.frame.servlet.renderer.JSONRenderer;
import org.b3log.solo.module.util.QueryResults;
import org.b3log.solo.service.LangPropsService;
import org.b3log.solo.service.PluginMgmtService;
import org.b3log.solo.service.PluginQueryService;
import org.b3log.solo.util.Requests;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;


/**
 * Plugin console request processing.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @author <a href="mailto:wmainlove@gmail.com">Love Yao</a>
 * @version 1.1.0.0, Jan 17, 2013
 * @since 0.4.0
 */
@Controller
public class PluginConsole {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(PluginConsole.class.getName());

    /**
     * Plugin query service.
     */
    @Autowired
    private PluginQueryService pluginQueryService;

    /**
     * Plugin management service.
     */
    @Autowired
    private PluginMgmtService pluginMgmtService;

    /**
     * Language service.
     */
    @Autowired
    private LangPropsService langPropsService;

    /**
     * Sets a plugin's status with the specified plugin id, status.
     * 
     * <p>
     * Renders the response with a json object, for example,
     * <pre>
     * {
     *     "sc": boolean,
     *     "msg": "" 
     * }
     * </pre>
     * </p>
     * 
     * @param request the specified http servlet request
     * @param response the specified http servlet response
     * @param context the specified http request context
     * @throws Exception exception
     */
    @RequestMapping(value = "/console/plugin/status/", method=RequestMethod.PUT)
    public void setPluginStatus(final HttpServletRequest request, final HttpServletResponse response)
        throws Exception {

        final JSONRenderer renderer = new JSONRenderer();
        final JSONObject requestJSONObject = Requests.parseRequestJSONObject(request, response);

        final String pluginId = requestJSONObject.getString(Keys.OBJECT_ID);
        final String status = requestJSONObject.getString(Plugin.PLUGIN_STATUS);

        final JSONObject result = pluginMgmtService.setPluginStatus(pluginId, status);

        renderer.setJSONObject(result);
        renderer.render(request, response);
    }

    /**
     * Gets plugins by the specified request.
     * 
     * <p>
     * The request URI contains the pagination arguments. For example, the 
     * request URI is /console/plugins/1/10/20, means the current page is 1, the 
     * page size is 10, and the window size is 20.
     * </p>
     * 
     * <p>
     * Renders the response with a json object, for example,
     * <pre>
     * {
     *     "sc": boolean,
     *     "pagination": {
     *         "paginationPageCount": 100,
     *         "paginationPageNums": [1, 2, 3, 4, 5]
     *     },
     *     "plugins": [{
     *         "name": "",
     *         "version": "",
     *         "author": "",
     *         "status": "", // Enumeration name of {@link org.b3log.solo.frame.plugin.PluginStatus}
     *      }, ....]
     * }
     * </pre>
     * </p>
     * 
     * @param request the specified http servlet request
     * @param response the specified http servlet response
     * @param context the specified http request context
     * @throws Exception exception
     * @see Requests#PAGINATION_PATH_PATTERN
     */
    @RequestMapping(value = "/console/plugins/*/*/*"/* Requests.PAGINATION_PATH_PATTERN */,
        method=RequestMethod.GET)
    public void getPlugins(final HttpServletRequest request, final HttpServletResponse response)
        throws Exception {

        final JSONRenderer renderer = new JSONRenderer();
        try {
            final String requestURI = request.getRequestURI();
            final String path = requestURI.substring((Latkes.getContextPath() + "/console/plugins/").length());

            final JSONObject requestJSONObject = Requests.buildPaginationRequest(path);

            final JSONObject result = pluginQueryService.getPlugins(requestJSONObject);

            renderer.setJSONObject(result);

            result.put(Keys.STATUS_CODE, true);
        } catch (final Exception e) {
            LOGGER.log(Level.ERROR, e.getMessage(), e);

            final JSONObject jsonObject = QueryResults.defaultResult();

            renderer.setJSONObject(jsonObject);
            jsonObject.put(Keys.MSG, langPropsService.get("getFailLabel"));
        }
        renderer.render(request, response);
    }

    /**
     * get the info of the specified pluginoId,just fot the plugin-setting.
     * @param request the specified http servlet request
     * @param response the specified http servlet response
     * @param context the specified http request context
     * @param renderer the specified {@link ConsoleRenderer}
     * @throws Exception exception
     */
    @RequestMapping(value = "/console/plugin/toSetting", method=RequestMethod.POST)
    public void toSetting(final HttpServletRequest request, final HttpServletResponse response,
        final ConsoleRenderer renderer) throws Exception {
        try {
            final JSONObject requestJSONObject = Requests.parseRequestJSONObject(request, response);
            final String pluginId = requestJSONObject.getString(Keys.OBJECT_ID);

            final String setting = pluginQueryService.getPluginSetting(pluginId);

            renderer.setTemplateName("admin-plugin-setting.ftl");
            final Map<String, Object> dataModel = renderer.getDataModel();

            Keys.fillRuntime(dataModel);

            dataModel.put(Plugin.PLUGIN_SETTING, setting);
            dataModel.put(Keys.OBJECT_ID, pluginId);
        } catch (final Exception e) {
            LOGGER.log(Level.ERROR, e.getMessage(), e);

            final JSONObject jsonObject = QueryResults.defaultResult();
            final JSONRenderer jsonRenderer = new JSONRenderer();

            jsonRenderer.setJSONObject(jsonObject);
            jsonObject.put(Keys.MSG, langPropsService.get("getFailLabel"));
        }
        renderer.render(request, response);
    }

    /**
     * update the setting of the plugin. 
     * 
     * @param request the specified http servlet request
     * @param response the specified http servlet response
     * @param context the specified http request context
     * @param renderer the specified {@link ConsoleRenderer}
     * @throws Exception exception
     */
    @RequestMapping(value = "/console/plugin/updateSetting", method=RequestMethod.POST)
    public void updateSetting(final HttpServletRequest request, final HttpServletResponse response,
        final JSONRenderer renderer) throws Exception {
        final JSONObject requestJSONObject = Requests.parseRequestJSONObject(request, response);
        final String pluginoId = requestJSONObject.getString(Keys.OBJECT_ID);
        final String settings = requestJSONObject.getString(Plugin.PLUGIN_SETTING);

        final JSONObject ret = pluginMgmtService.updatePluginSetting(pluginoId, settings);
        renderer.setJSONObject(ret);
        renderer.render(request, response);
    }
}
