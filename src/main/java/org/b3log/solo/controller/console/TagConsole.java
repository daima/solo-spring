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


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.b3log.solo.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.b3log.solo.frame.servlet.renderer.JSONRenderer;
import org.b3log.solo.model.Common;
import org.b3log.solo.model.Tag;
import org.b3log.solo.service.LangPropsService;
import org.b3log.solo.service.TagMgmtService;
import org.b3log.solo.service.TagQueryService;
import org.b3log.solo.service.UserQueryService;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;


/**
 * Tag console request processing.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.0.0.0, Oct 24, 2011
 * @since 0.4.0
 */
@Controller
public class TagConsole {

    /**
     * Logger.
     */
    private static Logger logger = LoggerFactory.getLogger(TagConsole.class);

    /**
     * Tag query service.
     */
    @Autowired
    private TagQueryService tagQueryService;

    /**
     * Tag management service.
     */
    @Autowired
    private TagMgmtService tagMgmtService;

    /**
     * User query service.
     */
    @Autowired
    private UserQueryService userQueryService;

    /**
     * Language service.
     */
    @Autowired
    private LangPropsService langPropsService;

    /**
     * Gets all tags.
     * 
     * <p>
     * Renders the response with a json object, for example,
     * <pre>
     * {
     *     "sc": boolean,
     *     "tags": [
     *         {"tagTitle": "", tagReferenceCount": int, ....},
     *         ....
     *     ]
     * }
     * </pre>
     * </p>
     *
     * @param request the specified http servlet request
     * @param response the specified http servlet response
     * @param context the specified http request context
     * @throws IOException io exception
     */
    @RequestMapping(value = "/console/tags", method=RequestMethod.GET)
    public void getTags(final HttpServletRequest request,
        final HttpServletResponse response)
        throws IOException {
        if (!userQueryService.isLoggedIn(request, response)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        final JSONRenderer renderer = new JSONRenderer();

        final JSONObject jsonObject = new JSONObject();

        renderer.setJSONObject(jsonObject);

        try {
            jsonObject.put(Tag.TAGS, tagQueryService.getTags());

            jsonObject.put(Keys.STATUS_CODE, true);
        } catch (final Exception e) {
            logger.error("Gets tags failed", e);

            jsonObject.put(Keys.STATUS_CODE, false);
        }
        renderer.render(request, response);
    }

    /**
     * Gets all unused tags.
     * 
     * <p>
     * Renders the response with a json object, for example,
     * <pre>
     * {
     *     "sc": boolean,
     *     "unusedTags": [
     *         {"tagTitle": "", tagReferenceCount": int, ....},
     *         ....
     *     ]
     * }
     * </pre>
     * </p>
     *
     * @param request the specified http servlet request
     * @param response the specified http servlet response
     * @param context the specified http request context
     * @throws IOException io exception
     */
    @RequestMapping(value = "/console/tag/unused",
        method=RequestMethod.GET)
    public void getUnusedTags(final HttpServletRequest request,
        final HttpServletResponse response)
        throws IOException {
        if (!userQueryService.isLoggedIn(request, response)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        final JSONRenderer renderer = new JSONRenderer();
        final JSONObject jsonObject = new JSONObject();
        renderer.setJSONObject(jsonObject);

        final List<JSONObject> unusedTags = new ArrayList<JSONObject>();

        try {
            jsonObject.put(Common.UNUSED_TAGS, unusedTags);

            final List<JSONObject> tags = tagQueryService.getTags();

            for (int i = 0; i < tags.size(); i++) {
                final JSONObject tag = tags.get(i);
                final int tagRefCnt = tag.getInt(Tag.TAG_REFERENCE_COUNT);

                if (0 == tagRefCnt) {
                    unusedTags.add(tag);
                }
            }

            jsonObject.put(Keys.STATUS_CODE, true);
        } catch (final Exception e) {
            logger.error("Gets unused tags failed", e);

            jsonObject.put(Keys.STATUS_CODE, false);
        }
        renderer.render(request, response);
    }

    /**
     * Removes all unused tags.
     * 
     * <p>
     * Renders the response with a json object, for example,
     * <pre>
     * {
     *     "msg": ""
     * }
     * </pre>
     * </p>
     *
     * @param request the specified http servlet request
     * @param response the specified http servlet response
     * @param context the specified http request context
     * @throws IOException io exception
     */
    @RequestMapping(value = "/console/tag/unused",
        method=RequestMethod.DELETE)
    public void removeUnusedTags(final HttpServletRequest request,
        final HttpServletResponse response)
        throws IOException {
        if (!userQueryService.isAdminLoggedIn(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        final JSONRenderer renderer = new JSONRenderer();
        final JSONObject jsonObject = new JSONObject();
        renderer.setJSONObject(jsonObject);
        try {
            tagMgmtService.removeUnusedTags();
            jsonObject.put(Keys.MSG, langPropsService.get("removeSuccLabel"));
        } catch (final Exception e) {
            logger.error("Removes unused tags failed", e);
            jsonObject.put(Keys.MSG, langPropsService.get("removeFailLabel"));
        }
        renderer.render(request, response);
    }
}
