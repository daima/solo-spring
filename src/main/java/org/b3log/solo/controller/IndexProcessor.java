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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.b3log.solo.Keys;
import org.b3log.solo.controller.renderer.ConsoleRenderer;
import org.b3log.solo.controller.util.Filler;
import org.b3log.solo.frame.model.Pagination;
import org.b3log.solo.frame.service.ServiceException;
import org.b3log.solo.frame.servlet.renderer.freemarker.AbstractFreeMarkerRenderer;
import org.b3log.solo.frame.servlet.renderer.freemarker.FreeMarkerRenderer;
import org.b3log.solo.model.Common;
import org.b3log.solo.model.Option;
import org.b3log.solo.model.Skin;
import org.b3log.solo.module.util.Skins;
import org.b3log.solo.service.LangPropsService;
import org.b3log.solo.service.PreferenceQueryService;
import org.b3log.solo.service.StatisticMgmtService;
import org.b3log.solo.util.Locales;
import org.b3log.solo.util.Requests;
import org.b3log.solo.util.freemarker.Templates;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import freemarker.template.Template;

/**
 * Index processor.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @author <a href="mailto:385321165@qq.com">DASHU</a>
 * @version 1.2.2.6, Dec 27, 2015
 * @since 0.3.1
 */
@Controller
public class IndexProcessor {

	/**
	 * Logger.
	 */
	private static Logger logger = LoggerFactory.getLogger(IndexProcessor.class);

	@Autowired
	private Skins skins;
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
	 * Statistic management service.
	 */
	@Autowired
	private StatisticMgmtService statisticMgmtService;

	/**
	 * Shows index with the specified context.
	 *
	 * @param context
	 *            the specified context
	 * @param request
	 *            the specified HTTP servlet request
	 * @param response
	 *            the specified HTTP servlet response
	 */
	@RequestMapping(value = { "/\\d*", "" }, method = RequestMethod.GET)
	public void showIndex(final HttpServletRequest request, final HttpServletResponse response) {
		final AbstractFreeMarkerRenderer renderer = new FreeMarkerRenderer();
		renderer.setTemplateName("index.ftl");
		final Map<String, Object> dataModel = renderer.getDataModel();
		final String requestURI = request.getRequestURI();

		try {
			final int currentPageNum = getCurrentPageNum(requestURI);
			final JSONObject preference = preferenceQueryService.getPreference();

			// https://github.com/b3log/solo/issues/12060
			String specifiedSkin = Skins.getSkinDirName(request);
			if (null != specifiedSkin) {
				if ("default".equals(specifiedSkin)) {
					specifiedSkin = preference.optString(Option.ID_C_SKIN_DIR_NAME);
				}
			} else {
				specifiedSkin = preference.optString(Option.ID_C_SKIN_DIR_NAME);
			}
			Templates.MAIN_CFG.setServletContextForTemplateLoading(request.getServletContext(),
					"/skins/" + specifiedSkin);
			request.setAttribute(Keys.TEMAPLTE_DIR_NAME, specifiedSkin);

			skins.fillLangs(preference.optString(Option.ID_C_LOCALE_STRING),
					(String) request.getAttribute(Keys.TEMAPLTE_DIR_NAME), dataModel);

			filler.fillIndexArticles(request, dataModel, currentPageNum, preference);

			filler.fillSide(request, dataModel, preference);
			filler.fillBlogHeader(request, response, dataModel, preference);
			filler.fillBlogFooter(request, dataModel, preference);

			dataModel.put(Pagination.PAGINATION_CURRENT_PAGE_NUM, currentPageNum);
			final int previousPageNum = currentPageNum > 1 ? currentPageNum - 1 : 0;
			dataModel.put(Pagination.PAGINATION_PREVIOUS_PAGE_NUM, previousPageNum);

			final Integer pageCount = (Integer) dataModel.get(Pagination.PAGINATION_PAGE_COUNT);
			final int nextPageNum = currentPageNum + 1 > pageCount ? pageCount : currentPageNum + 1;
			dataModel.put(Pagination.PAGINATION_NEXT_PAGE_NUM, nextPageNum);

			dataModel.put(Common.PATH, "");

			statisticMgmtService.incBlogViewCount(request, response);

			// https://github.com/b3log/solo/issues/12060
			final Cookie cookie = new Cookie(Skin.SKIN, specifiedSkin);
			cookie.setPath("/");
			response.addCookie(cookie);
			renderer.render(request, response);
		} catch (final ServiceException e) {
			logger.error(e.getMessage(), e);

			try {
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
			} catch (final IOException ex) {
				logger.error(ex.getMessage());
			}
		}
	}

	/**
	 * Shows kill browser page with the specified context.
	 *
	 * @param context
	 *            the specified context
	 * @param request
	 *            the specified HTTP servlet request
	 * @param response
	 *            the specified HTTP servlet response
	 */
	@RequestMapping(value = "/kill-browser", method = RequestMethod.GET)
	public void showKillBrowser(final HttpServletRequest request, final HttpServletResponse response) {
		final AbstractFreeMarkerRenderer renderer = new KillBrowserRenderer();
		final Map<String, Object> dataModel = renderer.getDataModel();

		try {
			final Map<String, String> langs = langPropsService.getAll(Locales.getLocale(request));

			dataModel.putAll(langs);
			final JSONObject preference = preferenceQueryService.getPreference();

			filler.fillBlogHeader(request, response, dataModel, preference);
			filler.fillBlogFooter(request, dataModel, preference);
			Keys.fillServer(dataModel);
			Keys.fillRuntime(dataModel);
			filler.fillMinified(dataModel);
		} catch (final ServiceException e) {
			logger.error(e.getMessage(), e);

			try {
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
			} catch (final IOException ex) {
				logger.error(ex.getMessage());
			}
		}
		renderer.render(request, response);
	}

	/**
	 * Show register page.
	 *
	 * @param context
	 *            the specified context
	 * @param request
	 *            the specified HTTP servlet request
	 * @param response
	 *            the specified HTTP servlet response
	 */
	@RequestMapping(value = "/register", method = RequestMethod.GET)
	public void register(final HttpServletRequest request, final HttpServletResponse response) {
		final AbstractFreeMarkerRenderer renderer = new ConsoleRenderer();
		renderer.setTemplateName("register.ftl");

		final Map<String, Object> dataModel = renderer.getDataModel();

		try {
			final Map<String, String> langs = langPropsService.getAll(Locales.getLocale(request));

			dataModel.putAll(langs);

			final JSONObject preference = preferenceQueryService.getPreference();

			filler.fillBlogFooter(request, dataModel, preference);
			filler.fillMinified(dataModel);
		} catch (final ServiceException e) {
			logger.error(e.getMessage(), e);

			try {
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
			} catch (final IOException ex) {
				logger.error(ex.getMessage());
			}
		}
		renderer.render(request, response);
	}

	/**
	 * Gets the request page number from the specified request URI.
	 *
	 * @param requestURI
	 *            the specified request URI
	 * @return page number, returns {@code -1} if the specified request URI can
	 *         not convert to an number
	 */
	private static int getCurrentPageNum(final String requestURI) {
		final String pageNumString = StringUtils.substringAfterLast(requestURI, "/");

		return Requests.getCurrentPageNum(pageNumString);
	}

	/**
	 * Kill browser (kill-browser.ftl) HTTP response renderer.
	 *
	 * @author <a href="http://88250.b3log.org">Liang Ding</a>
	 * @version 1.0.0.0, Sep 18, 2011
	 * @since 0.3.1
	 */
	private static final class KillBrowserRenderer extends AbstractFreeMarkerRenderer {

		/**
		 * Logger.
		 */
		private static Logger logger = LoggerFactory.getLogger(KillBrowserRenderer.class);

		@Override
		public void render(final HttpServletRequest request, final HttpServletResponse response) {
			response.setContentType("text/html");
			response.setCharacterEncoding("UTF-8");

			try {
				final Template template = ConsoleRenderer.TEMPLATE_CFG.getTemplate("kill-browser.ftl");

				final PrintWriter writer = response.getWriter();

				final StringWriter stringWriter = new StringWriter();

				template.setOutputEncoding("UTF-8");
				template.process(getDataModel(), stringWriter);

				final String pageContent = stringWriter.toString();

				writer.write(pageContent);
				writer.flush();
				writer.close();
			} catch (final Exception e) {
				try {
					response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				} catch (final IOException ex) {
					logger.error("Can not sned error 500!", ex);
				}
			}
		}

		@Override
		protected void afterRender(final HttpServletRequest request, final HttpServletResponse response)
				throws Exception {
			throw new UnsupportedOperationException("Not supported yet.");
		}

		@Override
		protected void beforeRender(final HttpServletRequest request, final HttpServletResponse response)
				throws Exception {
			throw new UnsupportedOperationException("Not supported yet.");
		}
	}
}
