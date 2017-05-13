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

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.b3log.solo.controller.renderer.ConsoleRenderer;
import org.b3log.solo.controller.util.Filler;
import org.b3log.solo.frame.user.UserService;
import org.b3log.solo.frame.user.UserServiceFactory;
import org.b3log.solo.model.Common;
import org.b3log.solo.service.LangPropsService;
import org.b3log.solo.service.PreferenceQueryService;
import org.b3log.solo.util.Locales;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Error processor.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.0.1.2, Oct 9, 2016
 * @since 0.4.5
 */
@Controller
public class ErrorProcessor {

	/**
	 * Logger.
	 */
	private static Logger logger = LoggerFactory.getLogger(ArticleController.class);

	/**
	 * Filler.
	 */
	@Autowired
	private Filler filler;

	/**
	 * Preference query service.
	 */
	@Autowired
	private PreferenceQueryService preferenceQueryService;

	/**
	 * Language service.
	 */
	@Autowired
	private LangPropsService langPropsService;

	/**
	 * User service.
	 */
	private static UserService userService = UserServiceFactory.getUserService();

	/**
	 * Shows the user template page.
	 *
	 * @param context
	 *            the specified context
	 * @param request
	 *            the specified HTTP servlet request
	 * @param response
	 *            the specified HTTP servlet response
	 * @throws IOException
	 *             io exception
	 */
	@RequestMapping(value = "/error/*.html", method = RequestMethod.GET)
	public void showErrorPage(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
		final String requestURI = request.getRequestURI();
		String templateName = StringUtils.substringAfterLast(requestURI, "/");

		templateName = StringUtils.substringBefore(templateName, ".") + ".ftl";
		logger.debug("Shows error page[requestURI={0}, templateName={1}]", requestURI, templateName);

		final ConsoleRenderer renderer = new ConsoleRenderer();
		renderer.setTemplateName("error/" + templateName);

		final Map<String, Object> dataModel = renderer.getDataModel();

		try {
			final Map<String, String> langs = langPropsService.getAll(Locales.getLocale(request));

			dataModel.putAll(langs);
			final JSONObject preference = preferenceQueryService.getPreference();

			filler.fillBlogHeader(request, response, dataModel, preference);
			filler.fillBlogFooter(request, dataModel, preference);

			dataModel.put(Common.LOGIN_URL, userService.createLoginURL(Common.ADMIN_INDEX_URI));
		} catch (final Exception e) {
			logger.error(e.getMessage(), e);

			try {
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
			} catch (final IOException ex) {
				logger.error(ex.getMessage());
			}
		}
		renderer.render(request, response);
	}
}
