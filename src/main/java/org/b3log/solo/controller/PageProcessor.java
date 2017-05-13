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
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.b3log.solo.Keys;
import org.b3log.solo.controller.util.Filler;
import org.b3log.solo.frame.servlet.renderer.freemarker.AbstractFreeMarkerRenderer;
import org.b3log.solo.frame.servlet.renderer.freemarker.FreeMarkerRenderer;
import org.b3log.solo.model.Common;
import org.b3log.solo.model.Option;
import org.b3log.solo.model.Page;
import org.b3log.solo.module.util.Emotions;
import org.b3log.solo.module.util.Markdowns;
import org.b3log.solo.module.util.Skins;
import org.b3log.solo.service.CommentQueryService;
import org.b3log.solo.service.PreferenceQueryService;
import org.b3log.solo.service.StatisticMgmtService;
import org.b3log.solo.util.Stopwatchs;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Page processor.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.1.0.6, Nov 8, 2016
 * @since 0.3.1
 */
@Controller
public class PageProcessor {

	/**
	 * Logger.
	 */
	private static Logger logger = LoggerFactory.getLogger(PageProcessor.class);

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
	 * Comment query service.
	 */
	@Autowired
	private CommentQueryService commentQueryService;

	/**
	 * Statistic management service.
	 */
	@Autowired
	private StatisticMgmtService statisticMgmtService;

	/**
	 * Shows page with the specified context.
	 *
	 * @param context
	 *            the specified context
	 */
	@RequestMapping(value = "/page", method = RequestMethod.GET)
	public void showPage(final HttpServletRequest request, final HttpServletResponse response) {
		final AbstractFreeMarkerRenderer renderer = new FreeMarkerRenderer();
		renderer.setTemplateName("page.ftl");
		final Map<String, Object> dataModel = renderer.getDataModel();

		try {
			final JSONObject preference = preferenceQueryService.getPreference();

			if (null == preference) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}

			skins.fillLangs(preference.getString(Option.ID_C_LOCALE_STRING),
					(String) request.getAttribute(Keys.TEMAPLTE_DIR_NAME), dataModel);

			// See PermalinkFilter#dispatchToArticleOrPageProcessor()
			final JSONObject page = (JSONObject) request.getAttribute(Page.PAGE);

			if (null == page) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}

			final String pageId = page.getString(Keys.OBJECT_ID);

			page.put(Common.COMMENTABLE,
					preference.getBoolean(Option.ID_C_COMMENTABLE) && page.getBoolean(Page.PAGE_COMMENTABLE));
			page.put(Common.PERMALINK, page.getString(Page.PAGE_PERMALINK));
			dataModel.put(Page.PAGE, page);
			final List<JSONObject> comments = commentQueryService.getComments(pageId);

			dataModel.put(Page.PAGE_COMMENTS_REF, comments);

			// Markdown
			if ("CodeMirror-Markdown".equals(page.optString(Page.PAGE_EDITOR_TYPE))) {
				Stopwatchs.start("Markdown Page[id=" + page.optString(Keys.OBJECT_ID) + "]");

				String content = page.optString(Page.PAGE_CONTENT);
				content = Emotions.convert(content);
				content = Markdowns.toHTML(content);
				page.put(Page.PAGE_CONTENT, content);

				Stopwatchs.end();
			}

			filler.fillSide(request, dataModel, preference);
			filler.fillBlogHeader(request, response, dataModel, preference);
			filler.fillBlogFooter(request, dataModel, preference);

			statisticMgmtService.incBlogViewCount(request, response);
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
