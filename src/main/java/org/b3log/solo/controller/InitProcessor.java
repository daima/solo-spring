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
package org.b3log.solo.controller;

import java.util.Calendar;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.b3log.solo.Keys;
import org.b3log.solo.Latkes;
import org.b3log.solo.SoloConstant;
import org.b3log.solo.controller.renderer.ConsoleRenderer;
import org.b3log.solo.controller.util.Filler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.b3log.solo.frame.model.Role;
import org.b3log.solo.frame.model.User;
import org.b3log.solo.frame.servlet.renderer.JSONRenderer;
import org.b3log.solo.frame.servlet.renderer.freemarker.AbstractFreeMarkerRenderer;
import org.b3log.solo.model.Common;
import org.b3log.solo.model.UserExt;
import org.b3log.solo.module.util.QueryResults;
import org.b3log.solo.module.util.Thumbnails;
import org.b3log.solo.service.InitService;
import org.b3log.solo.service.LangPropsService;
import org.b3log.solo.util.Locales;
import org.b3log.solo.util.Requests;
import org.b3log.solo.util.Sessions;
import org.b3log.solo.util.Strings;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Solo initialization service.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.2.0.10, Aug 9, 2016
 * @since 0.4.0
 */
@Controller
public class InitProcessor {

    /**
     * Logger.
     */
    private static Logger logger = LoggerFactory.getLogger(InitProcessor.class);

    /**
     * Initialization service.
     */
    @Autowired
    private InitService initService;

    /**
     * Filler.
     */
    @Autowired
    private Filler filler;

    /**
     * Language service.
     */
    @Autowired
    private LangPropsService langPropsService;

    /**
     * Max user name length.
     */
    public static final int MAX_USER_NAME_LENGTH = 20;

    /**
     * Min user name length.
     */
    public static final int MIN_USER_NAME_LENGTH = 1;

    /**
     * Shows initialization page.
     *
     * @param context the specified http request context
     * @param request the specified http servlet request
     * @param response the specified http servlet response
     * @throws Exception exception
     */
    @RequestMapping(value = "/init", method=RequestMethod.GET)
    public void showInit(final HttpServletRequest request, final HttpServletResponse response)
            throws Exception {
        if (initService.isInited()) {
            response.sendRedirect("/");
            return;
        }

        final AbstractFreeMarkerRenderer renderer = new ConsoleRenderer();
        renderer.setTemplateName("init.ftl");

        final Map<String, Object> dataModel = renderer.getDataModel();

        final Map<String, String> langs = langPropsService.getAll(Locales.getLocale(request));

        dataModel.putAll(langs);

        dataModel.put(Common.VERSION, SoloConstant.VERSION);
        dataModel.put(Common.STATIC_RESOURCE_VERSION, Latkes.getStaticResourceVersion());
        dataModel.put(Common.YEAR, String.valueOf(Calendar.getInstance().get(Calendar.YEAR)));

        Keys.fillRuntime(dataModel);
        filler.fillMinified(dataModel);
        renderer.render(request, response);
    }

    /**
     * Initializes Solo.
     *
     * @param context the specified http request context
     * @param request the specified http servlet request, for example,      <pre>
     * {
     *     "userName": "",
     *     "userEmail": "",
     *     "userPassword": ""
     * }
     * </pre>
     *
     * @param response the specified http servlet response
     * @throws Exception exception
     */
    @RequestMapping(value = "/init", method=RequestMethod.POST)
    public void initSolo(final HttpServletRequest request, final HttpServletResponse response) throws Exception {
        if (initService.isInited()) {
            response.sendRedirect("/");
            return;
        }


        final JSONObject ret = QueryResults.defaultResult();
        final JSONRenderer renderer = new JSONRenderer();
        renderer.setJSONObject(ret);

        try {
            final JSONObject requestJSONObject = Requests.parseRequestJSONObject(request, response);

            final String userName = requestJSONObject.optString(User.USER_NAME);
            final String userEmail = requestJSONObject.optString(User.USER_EMAIL);
            final String userPassword = requestJSONObject.optString(User.USER_PASSWORD);

            if (Strings.isEmptyOrNull(userName) || Strings.isEmptyOrNull(userEmail) || Strings.isEmptyOrNull(userPassword)
                    || !Strings.isEmail(userEmail)) {
                ret.put(Keys.MSG, "Init failed, please check your input");
                renderer.render(request, response);
                return;
            }

            if (invalidUserName(userName)) {
                ret.put(Keys.MSG, "Init failed, please check your username (length [1, 20], content {a-z, A-Z, 0-9}, do not contain 'admin' for security reason]");
                renderer.render(request, response);
                return;
            }

            final Locale locale = Locales.getLocale(request);
            requestJSONObject.put(Keys.LOCALE, locale.toString());

            initService.init(requestJSONObject);

            // If initialized, login the admin
            final JSONObject admin = new JSONObject();
            admin.put(User.USER_NAME, userName);
            admin.put(User.USER_EMAIL, userEmail);
            admin.put(User.USER_ROLE, Role.ADMIN_ROLE);
            admin.put(User.USER_PASSWORD, userPassword);
            admin.put(UserExt.USER_AVATAR, Thumbnails.getGravatarURL(userEmail, "128"));

            Sessions.login(request, response, admin);

            ret.put(Keys.STATUS_CODE, true);
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
            ret.put(Keys.MSG, e.getMessage());
        }
        renderer.render(request, response);
    }

    /**
     * Checks whether the specified name is invalid.
     *
     * <p>
     * A valid user name:
     * <ul>
     * <li>length [1, 20]</li>
     * <li>content {a-z, A-Z, 0-9}</li>
     * <li>Not contains "admin"/"Admin"</li>
     * </ul>
     * </p>
     *
     * @param name the specified name
     * @return {@code true} if it is invalid, returns {@code false} otherwise
     */
    public static boolean invalidUserName(final String name) {
        final int length = name.length();
        if (length < MIN_USER_NAME_LENGTH || length > MAX_USER_NAME_LENGTH) {
            return true;
        }

        char c;
        for (int i = 0; i < length; i++) {
            c = name.charAt(i);

            if (('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z') || '0' <= c && c <= '9') {
                continue;
            }

            return true;
        }

        return name.contains("admin") || name.contains("Admin");
    }
}
