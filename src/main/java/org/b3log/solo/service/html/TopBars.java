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
package org.b3log.solo.service.html;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.b3log.solo.Keys;
import org.b3log.solo.model.Common;
import org.b3log.solo.model.Role;
import org.b3log.solo.model.User;
import org.b3log.solo.renderer.ConsoleRenderer;
import org.b3log.solo.service.LangPropsService;
import org.b3log.solo.service.ServiceException;
import org.b3log.solo.service.StatisticQueryService;
import org.b3log.solo.service.UserMgmtService;
import org.b3log.solo.service.UserQueryService;
import org.b3log.solo.service.UserService;
import org.b3log.solo.util.Requests;
import org.b3log.solo.util.Stopwatchs;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * Top bar utilities.
 *
 * @author <a href="http://cxy7.com">XyCai</a>
 * @author <a href="mailto:dongxu.wang@acm.org">Dongxu Wang</a>
 * @version 1.0.1.5, Apr 10, 2013
 * @since 0.3.5
 */
@Service
public class TopBars {

	/**
	 * Logger.
	 */
	private static Logger logger = LoggerFactory.getLogger(TopBars.class);

	@Autowired
	private UserService userService;

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
	 * User management service.
	 */
	@Autowired
	private UserMgmtService userMgmtService;

	/**
	 * Generates top bar HTML.
	 * 
	 * @param request
	 *            the specified request
	 * @param response
	 *            the specified response
	 * @return top bar HTML
	 * @throws ServiceException
	 *             service exception
	 */
	public String getTopBarHTML(final HttpServletRequest request, final HttpServletResponse response)
			throws ServiceException {
		Stopwatchs.start("Gens Top Bar HTML");

		try {
			final Template topBarTemplate = ConsoleRenderer.TEMPLATE_CFG.getTemplate("top-bar.ftl");
			final StringWriter stringWriter = new StringWriter();

			final Map<String, Object> topBarModel = new HashMap<>();

			userMgmtService.tryLogInWithCookie(request, response);
			final JSONObject currentUser = userQueryService.getCurrentUser(request);

			Keys.fillServer(topBarModel);
			topBarModel.put(Common.IS_LOGGED_IN, false);

			topBarModel.put(Common.IS_MOBILE_REQUEST, Requests.mobileRequest(request));
			topBarModel.put("mobileLabel", langPropsService.get("mobileLabel"));

			topBarModel.put("onlineVisitor1Label", langPropsService.get("onlineVisitor1Label"));
			topBarModel.put(Common.ONLINE_VISITOR_CNT, StatisticQueryService.getOnlineVisitorCount());

			if (null == currentUser) {
				topBarModel.put(Common.LOGIN_URL, userService.createLoginURL(Common.ADMIN_INDEX_URI));
				topBarModel.put("loginLabel", langPropsService.get("loginLabel"));
				topBarModel.put("registerLabel", langPropsService.get("registerLabel"));

				topBarTemplate.process(topBarModel, stringWriter);

				return stringWriter.toString();
			}

			topBarModel.put(Common.IS_LOGGED_IN, true);
			topBarModel.put(Common.LOGOUT_URL, userService.createLogoutURL("/"));
			topBarModel.put(Common.IS_ADMIN, Role.ADMIN_ROLE.equals(currentUser.getString(User.USER_ROLE)));
			topBarModel.put(Common.IS_VISITOR, Role.VISITOR_ROLE.equals(currentUser.getString(User.USER_ROLE)));

			topBarModel.put("adminLabel", langPropsService.get("adminLabel"));
			topBarModel.put("logoutLabel", langPropsService.get("logoutLabel"));

			final String userName = currentUser.getString(User.USER_NAME);

			topBarModel.put(User.USER_NAME, userName);

			topBarTemplate.process(topBarModel, stringWriter);

			return stringWriter.toString();
		} catch (final JSONException e) {
			logger.error("Gens top bar HTML failed", e);
			throw new ServiceException(e);
		} catch (final IOException e) {
			logger.error("Gens top bar HTML failed", e);
			throw new ServiceException(e);
		} catch (final TemplateException e) {
			logger.error("Gens top bar HTML failed", e);
			throw new ServiceException(e);
		} finally {
			Stopwatchs.end();
		}
	}
}
