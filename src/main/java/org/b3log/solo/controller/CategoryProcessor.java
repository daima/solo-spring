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
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.b3log.solo.Keys;
import org.b3log.solo.Latkes;
import org.b3log.solo.controller.util.Filler;
import org.b3log.solo.frame.model.Pagination;
import org.b3log.solo.frame.service.ServiceException;
import org.b3log.solo.frame.servlet.renderer.freemarker.AbstractFreeMarkerRenderer;
import org.b3log.solo.frame.servlet.renderer.freemarker.FreeMarkerRenderer;
import org.b3log.solo.model.Article;
import org.b3log.solo.model.Category;
import org.b3log.solo.model.Common;
import org.b3log.solo.model.Option;
import org.b3log.solo.module.util.Skins;
import org.b3log.solo.service.ArticleQueryService;
import org.b3log.solo.service.CategoryQueryService;
import org.b3log.solo.service.PreferenceQueryService;
import org.b3log.solo.service.StatisticMgmtService;
import org.b3log.solo.service.UserQueryService;
import org.b3log.solo.util.Requests;
import org.b3log.solo.util.Strings;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Category processor.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.0.0.2, Apr 10, 2017
 * @since 2.0.0
 */
@Controller
public class CategoryProcessor {

	/**
	 * Logger.
	 */
	private static Logger logger = LoggerFactory.getLogger(CategoryProcessor.class);

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
	 * Article query service.
	 */
	@Autowired
	private ArticleQueryService articleQueryService;

	/**
	 * User query service.
	 */
	@Autowired
	private UserQueryService userQueryService;

	/**
	 * Category query service.
	 */
	@Autowired
	private CategoryQueryService categoryQueryService;

	/**
	 * Statistic management service.
	 */
	@Autowired
	private StatisticMgmtService statisticMgmtService;

	/**
	 * Gets the request page number from the specified request URI and category
	 * URI.
	 *
	 * @param requestURI
	 *            the specified request URI
	 * @param categoryURI
	 *            the specified category URI
	 * @return page number, returns {@code -1} if the specified request URI can
	 *         not convert to an number
	 */
	private static int getCurrentPageNum(final String requestURI, final String categoryURI) {
		if (Strings.isEmptyOrNull(categoryURI)) {
			return -1;
		}

		final String pageNumString = requestURI
				.substring((Latkes.getContextPath() + "/category/" + categoryURI + "/").length());

		return Requests.getCurrentPageNum(pageNumString);
	}

	/**
	 * Gets category URI from the specified URI.
	 *
	 * @param requestURI
	 *            the specified request URI
	 * @return category URI
	 */
	private static String getCategoryURI(final String requestURI) {
		final String path = requestURI.substring((Latkes.getContextPath() + "/category/").length());

		if (path.contains("/")) {
			return path.substring(0, path.indexOf("/"));
		} else {
			return path.substring(0);
		}
	}

	/**
	 * Shows articles related with a category with the specified context.
	 *
	 * @param context
	 *            the specified context
	 * @throws IOException
	 *             io exception
	 */
	@RequestMapping(value = "/category/**", method = RequestMethod.GET)
	public void showCategoryArticles(final HttpServletRequest request, final HttpServletResponse response)
			throws IOException {
		final AbstractFreeMarkerRenderer renderer = new FreeMarkerRenderer();
		renderer.setTemplateName("category-articles.ftl");
		final Map<String, Object> dataModel = renderer.getDataModel();

		try {
			String requestURI = request.getRequestURI();

			if (!requestURI.endsWith("/")) {
				requestURI += "/";
			}

			String categoryURI = getCategoryURI(requestURI);
			final int currentPageNum = getCurrentPageNum(requestURI, categoryURI);
			if (-1 == currentPageNum) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}

			logger.debug("Category [URI={0}, currentPageNum={1}]", categoryURI, currentPageNum);

			categoryURI = URLDecoder.decode(categoryURI, "UTF-8");
			final JSONObject category = categoryQueryService.getByURI(categoryURI);

			if (null == category) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}

			dataModel.put(Category.CATEGORY, category);

			final JSONObject preference = preferenceQueryService.getPreference();
			final int pageSize = preference.getInt(Option.ID_C_ARTICLE_LIST_DISPLAY_COUNT);
			final String categoryId = category.optString(Keys.OBJECT_ID);

			final JSONObject result = articleQueryService.getCategoryArticles(categoryId, currentPageNum, pageSize);
			final List<JSONObject> articles = (List<JSONObject>) result.opt(Article.ARTICLES);

			skins.fillLangs(preference.optString(Option.ID_C_LOCALE_STRING),
					(String) request.getAttribute(Keys.TEMAPLTE_DIR_NAME), dataModel);

			final boolean hasMultipleUsers = userQueryService.hasMultipleUsers();
			if (hasMultipleUsers) {
				filler.setArticlesExProperties(request, articles, preference);
			} else {
				// All articles composed by the same author
				final JSONObject author = articleQueryService.getAuthor(articles.get(0));

				filler.setArticlesExProperties(request, articles, author, preference);
			}

			final int pageCount = result.optJSONObject(Pagination.PAGINATION).optInt(Pagination.PAGINATION_PAGE_COUNT);
			if (0 == pageCount) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}

			final List<Integer> pageNums = (List) result.optJSONObject(Pagination.PAGINATION)
					.opt(Pagination.PAGINATION_PAGE_NUMS);

			fillPagination(dataModel, pageCount, currentPageNum, articles, pageNums);
			dataModel.put(Common.PATH, "/category/" + URLEncoder.encode(categoryURI, "UTF-8"));

			filler.fillSide(request, dataModel, preference);
			filler.fillBlogHeader(request, response, dataModel, preference);
			filler.fillBlogFooter(request, dataModel, preference);

			statisticMgmtService.incBlogViewCount(request, response);
		} catch (final ServiceException | JSONException e) {
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
	 * Fills pagination.
	 *
	 * @param dataModel
	 *            the specified data model
	 * @param pageCount
	 *            the specified page count
	 * @param currentPageNum
	 *            the specified current page number
	 * @param articles
	 *            the specified articles
	 * @param pageNums
	 *            the specified page numbers
	 */
	private void fillPagination(final Map<String, Object> dataModel, final int pageCount, final int currentPageNum,
			final List<JSONObject> articles, final List<Integer> pageNums) {
		final String previousPageNum = Integer.toString(currentPageNum > 1 ? currentPageNum - 1 : 0);

		dataModel.put(Pagination.PAGINATION_PREVIOUS_PAGE_NUM, "0".equals(previousPageNum) ? "" : previousPageNum);
		if (pageCount == currentPageNum + 1) { // The next page is the last page
			dataModel.put(Pagination.PAGINATION_NEXT_PAGE_NUM, "");
		} else {
			dataModel.put(Pagination.PAGINATION_NEXT_PAGE_NUM, currentPageNum + 1);
		}
		dataModel.put(Article.ARTICLES, articles);
		dataModel.put(Pagination.PAGINATION_CURRENT_PAGE_NUM, currentPageNum);
		dataModel.put(Pagination.PAGINATION_FIRST_PAGE_NUM, pageNums.get(0));
		dataModel.put(Pagination.PAGINATION_LAST_PAGE_NUM, pageNums.get(pageNums.size() - 1));
		dataModel.put(Pagination.PAGINATION_PAGE_COUNT, pageCount);
		dataModel.put(Pagination.PAGINATION_PAGE_NUMS, pageNums);
	}
}
