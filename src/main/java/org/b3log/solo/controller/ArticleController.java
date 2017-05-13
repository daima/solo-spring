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
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.b3log.solo.Keys;
import org.b3log.solo.Latkes;
import org.b3log.solo.SoloConstant;
import org.b3log.solo.frame.event.EventException;
import org.b3log.solo.model.ArchiveDate;
import org.b3log.solo.model.Article;
import org.b3log.solo.model.Common;
import org.b3log.solo.model.Option;
import org.b3log.solo.model.Pagination;
import org.b3log.solo.model.Tag;
import org.b3log.solo.model.User;
import org.b3log.solo.model.UserExt;
import org.b3log.solo.module.util.Skins;
import org.b3log.solo.module.util.Thumbnails;
import org.b3log.solo.renderer.ConsoleRenderer;
import org.b3log.solo.renderer.JSONRenderer;
import org.b3log.solo.renderer.TextHTMLRenderer;
import org.b3log.solo.renderer.freemarker.AbstractFreeMarkerRenderer;
import org.b3log.solo.renderer.freemarker.FreeMarkerRenderer;
import org.b3log.solo.service.ArchiveDateQueryService;
import org.b3log.solo.service.ArticleMgmtService;
import org.b3log.solo.service.ArticleQueryService;
import org.b3log.solo.service.CommentQueryService;
import org.b3log.solo.service.LangPropsService;
import org.b3log.solo.service.PreferenceQueryService;
import org.b3log.solo.service.ServiceException;
import org.b3log.solo.service.StatisticMgmtService;
import org.b3log.solo.service.TagQueryService;
import org.b3log.solo.service.UserQueryService;
import org.b3log.solo.service.html.Filler;
import org.b3log.solo.util.Dates;
import org.b3log.solo.util.Locales;
import org.b3log.solo.util.Paginator;
import org.b3log.solo.util.Requests;
import org.b3log.solo.util.Stopwatchs;
import org.apache.commons.lang3.StringUtils;
import org.b3log.solo.util.comparator.Comparators;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Article processor.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @author <a href="http://zephyr.b3log.org">Zephyr</a>
 * @version 1.4.2.16, Nov 17, 2016
 * @since 0.3.1
 */
@Controller
public class ArticleController {

	/**
	 * Logger.
	 */
	private static Logger logger = LoggerFactory.getLogger(ArticleController.class);

	@Autowired
	private Skins skins;

	/**
	 * Article query service.
	 */
	@Autowired
	private ArticleQueryService articleQueryService;

	/**
	 * Tag query service.
	 */
	@Autowired
	private TagQueryService tagQueryService;

	/**
	 * Comment query service.
	 */
	@Autowired
	private CommentQueryService commentQueryService;

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
	 * Preference query service.
	 */
	@Autowired
	private PreferenceQueryService preferenceQueryService;

	/**
	 * Archive date query service.
	 */
	@Autowired
	private ArchiveDateQueryService archiveDateQueryService;

	/**
	 * User query service.
	 */
	@Autowired
	private UserQueryService userQueryService;

	/**
	 * Article management service.
	 */
	@Autowired
	private ArticleMgmtService articleMgmtService;

	/**
	 * Statistic management service.
	 */
	@Autowired
	private StatisticMgmtService statisticMgmtService;

	/**
	 * Shows the article view password form.
	 *
	 * @param context
	 *            the specified context
	 * @param request
	 *            the specified HTTP servlet request
	 * @param response
	 *            the specified HTTP servlet response
	 * @throws Exception
	 *             exception
	 */
	@RequestMapping(value = "/console/article-pwd", method = RequestMethod.GET)
	public void showArticlePwdForm(final HttpServletRequest request, final HttpServletResponse response)
			throws Exception {
		final AbstractFreeMarkerRenderer renderer = new ConsoleRenderer();
		renderer.setTemplateName("article-pwd.ftl");
		final String articleId = request.getParameter("articleId");

		if (StringUtils.isBlank(articleId)) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		final JSONObject article = articleQueryService.getArticleById(articleId);

		if (null == article) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		final Map<String, Object> dataModel = renderer.getDataModel();

		dataModel.put("articleId", articleId);
		dataModel.put("articlePermalink", article.optString(Article.ARTICLE_PERMALINK));
		dataModel.put("articleTitle", article.optString(Article.ARTICLE_TITLE));
		dataModel.put("articleAbstract", article.optString(Article.ARTICLE_ABSTRACT));
		final String msg = request.getParameter(Keys.MSG);

		if (!StringUtils.isBlank(msg)) {
			dataModel.put(Keys.MSG, langPropsService.get("passwordNotMatchLabel"));
		}

		final Map<String, String> langs = langPropsService.getAll(Latkes.getLocale());

		dataModel.putAll(langs);

		final JSONObject preference = preferenceQueryService.getPreference();

		dataModel.put(Option.ID_C_BLOG_TITLE, preference.getString(Option.ID_C_BLOG_TITLE));
		dataModel.put(Common.VERSION, SoloConstant.VERSION);
		dataModel.put(Common.STATIC_RESOURCE_VERSION, Latkes.getStaticResourceVersion());
		dataModel.put(Common.YEAR, String.valueOf(Calendar.getInstance().get(Calendar.YEAR)));

		Keys.fillRuntime(dataModel);
		filler.fillMinified(dataModel);

		renderer.render(request, response);
	}

	/**
	 * Processes the article view password form submits.
	 *
	 * @param context
	 *            the specified context
	 * @param request
	 *            the specified HTTP servlet request
	 * @param response
	 *            the specified HTTP servlet response
	 * @throws Exception
	 *             exception
	 */
	@RequestMapping(value = "/console/article-pwd", method = RequestMethod.POST)
	public void onArticlePwdForm(final HttpServletRequest request, final HttpServletResponse response)
			throws Exception {
		try {
			final String articleId = request.getParameter("articleId");
			final String pwdTyped = request.getParameter("pwdTyped");

			final JSONObject article = articleQueryService.getArticleById(articleId);

			if (article.getString(Article.ARTICLE_VIEW_PWD).equals(pwdTyped)) {
				final HttpSession session = request.getSession(false);

				if (null != session) {
					@SuppressWarnings("unchecked")
					Map<String, String> viewPwds = (Map<String, String>) session.getAttribute(Common.ARTICLES_VIEW_PWD);

					if (null == viewPwds) {
						viewPwds = new HashMap<>();
					}

					viewPwds.put(articleId, pwdTyped);

					session.setAttribute(Common.ARTICLES_VIEW_PWD, viewPwds);
				}

				response.sendRedirect(Latkes.getServePath() + article.getString(Article.ARTICLE_PERMALINK));
				return;
			}
			response.sendRedirect(Latkes.getServePath() + "/console/article-pwd?articleId="
					+ article.optString(Keys.OBJECT_ID) + "&msg=1");
		} catch (final Exception e) {
			logger.error("Processes article view password form submits failed", e);
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
		}
	}

	/**
	 * Gets random articles with the specified context.
	 *
	 * @param context
	 *            the specified context
	 * @throws Exception
	 *             exception
	 */
	@RequestMapping(value = "/get-random-articles.do", method = RequestMethod.POST)
	public void getRandomArticles(final HttpServletRequest request, final HttpServletResponse response)
			throws Exception {
		final JSONRenderer renderer = new JSONRenderer();

		final JSONObject jsonObject = new JSONObject();

		final JSONObject preference = preferenceQueryService.getPreference();
		final int displayCnt = preference.getInt(Option.ID_C_RANDOM_ARTICLES_DISPLAY_CNT);

		if (0 == displayCnt) {
			jsonObject.put(Common.RANDOM_ARTICLES, new ArrayList<JSONObject>());

			renderer.setJSONObject(jsonObject);
			renderer.render(request, response);
			return;
		}

		Stopwatchs.start("Get Random Articles");
		final List<JSONObject> randomArticles = getRandomArticles(preference);

		jsonObject.put(Common.RANDOM_ARTICLES, randomArticles);

		renderer.setJSONObject(jsonObject);

		Stopwatchs.end();
		renderer.render(request, response);
	}

	/**
	 * Gets relevant articles with the specified context.
	 *
	 * @param context
	 *            the specified context
	 * @param request
	 *            the specified request
	 * @param response
	 *            the specified response
	 * @throws Exception
	 *             exception
	 */
	@RequestMapping(value = "/article/id/*/relevant/articles", method = RequestMethod.GET)
	public void getRelevantArticles(final HttpServletRequest request, final HttpServletResponse response)
			throws Exception {
		final JSONRenderer renderer = new JSONRenderer();

		final JSONObject jsonObject = new JSONObject();

		final JSONObject preference = preferenceQueryService.getPreference();

		final int displayCnt = preference.getInt(Option.ID_C_RELEVANT_ARTICLES_DISPLAY_CNT);

		if (0 == displayCnt) {
			jsonObject.put(Common.RANDOM_ARTICLES, new ArrayList<JSONObject>());
			renderer.setJSONObject(jsonObject);
			renderer.render(request, response);
			return;
		}

		Stopwatchs.start("Get Relevant Articles");
		final String requestURI = request.getRequestURI();

		final String articleId = StringUtils.substringBetween(requestURI, "/article/id/", "/relevant/articles");

		if (StringUtils.isBlank(articleId)) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		final JSONObject article = articleQueryService.getArticleById(articleId);

		if (null == article) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		final List<JSONObject> relevantArticles = articleQueryService.getRelevantArticles(article, preference);

		jsonObject.put(Common.RELEVANT_ARTICLES, relevantArticles);

		renderer.setJSONObject(jsonObject);

		Stopwatchs.end();
		renderer.render(request, response);
	}

	/**
	 * Gets article content with the specified context.
	 *
	 * @param context
	 *            the specified context
	 * @param request
	 *            the specified request
	 */
	@RequestMapping(value = "/get-article-content", method = RequestMethod.GET)
	public void getArticleContent(final HttpServletRequest request, final HttpServletResponse response) {
		final TextHTMLRenderer renderer = new TextHTMLRenderer();
		final String articleId = request.getParameter("id");

		if (StringUtils.isBlank(articleId)) {
			renderer.render(request, response);
			return;
		}

		String content;

		try {
			content = articleQueryService.getArticleContent(request, articleId);
		} catch (final ServiceException e) {
			logger.error("Can not get article content", e);
			renderer.render(request, response);
			return;
		}

		if (null == content) {
			renderer.render(request, response);
			return;
		}

		renderer.setContent(content);
		renderer.render(request, response);
	}

	/**
	 * Gets articles paged with the specified context.
	 *
	 * @param context
	 *            the specified context
	 * @param request
	 *            the specified request
	 */
	@RequestMapping(value = "/articles/\\d+", method = RequestMethod.GET)
	public void getArticlesByPage(final HttpServletRequest request, final HttpServletResponse response) {
		final JSONObject jsonObject = new JSONObject();
		final int currentPageNum = getArticlesPagedCurrentPageNum(request.getRequestURI());

		Stopwatchs.start("Get Articles Paged[pageNum=" + currentPageNum + ']');

		try {
			jsonObject.put(Keys.STATUS_CODE, true);

			final JSONObject preference = preferenceQueryService.getPreference();
			final int pageSize = preference.getInt(Option.ID_C_ARTICLE_LIST_DISPLAY_COUNT);
			final int windowSize = preference.getInt(Option.ID_C_ARTICLE_LIST_PAGINATION_WINDOW_SIZE);

			final StringBuilder pathBuilder = new StringBuilder();

			pathBuilder.append(currentPageNum).append('/').append(pageSize).append('/').append(windowSize);

			final JSONObject requestJSONObject = Requests.buildPaginationRequest(pathBuilder.toString());

			requestJSONObject.put(Article.ARTICLE_IS_PUBLISHED, true);

			final JSONObject result = articleQueryService.getArticles(requestJSONObject);
			final List<JSONObject> articles = org.b3log.solo.util.CollectionUtils
					.jsonArrayToList(result.getJSONArray(Article.ARTICLES));

			final boolean hasMultipleUsers = userQueryService.hasMultipleUsers();

			if (hasMultipleUsers) {
				filler.setArticlesExProperties(request, articles, preference);
			} else if (!articles.isEmpty()) {
				final JSONObject author = articleQueryService.getAuthor(articles.get(0));

				filler.setArticlesExProperties(request, articles, author, preference);
			}

			jsonObject.put(Keys.RESULTS, result);
		} catch (final Exception e) {
			jsonObject.put(Keys.STATUS_CODE, false);
			logger.error("Gets article paged failed", e);
		} finally {
			Stopwatchs.end();
		}

		final JSONRenderer renderer = new JSONRenderer();
		renderer.setJSONObject(jsonObject);
		renderer.render(request, response);
	}

	/**
	 * Gets tag articles paged with the specified context.
	 *
	 * @param context
	 *            the specified context
	 * @param request
	 *            the specified request
	 */
	@RequestMapping(value = "/articles/tags/.+/\\d+", method = RequestMethod.GET)
	public void getTagArticlesByPage(final HttpServletRequest request, final HttpServletResponse response) {
		final JSONObject jsonObject = new JSONObject();

		String tagTitle = getTagArticlesPagedTag(request.getRequestURI());

		try {
			tagTitle = URLDecoder.decode(tagTitle, "UTF-8");
		} catch (final UnsupportedEncodingException e) {
			logger.error("Gets tag title failed[requestURI=" + request.getRequestURI() + ']', e);
			tagTitle = "";
		}

		final int currentPageNum = getTagArticlesPagedCurrentPageNum(request.getRequestURI());

		Stopwatchs.start("Get Tag-Articles Paged[tagTitle=" + tagTitle + ", pageNum=" + currentPageNum + ']');

		try {
			jsonObject.put(Keys.STATUS_CODE, true);

			final JSONObject preference = preferenceQueryService.getPreference();
			final int pageSize = preference.getInt(Option.ID_C_ARTICLE_LIST_DISPLAY_COUNT);

			final JSONObject tagQueryResult = tagQueryService.getTagByTitle(tagTitle);

			if (null == tagQueryResult) {
				throw new Exception("Can not foud tag[title=" + tagTitle + "]");
			}

			final JSONObject tag = tagQueryResult.getJSONObject(Tag.TAG);
			final String tagId = tag.getString(Keys.OBJECT_ID);
			final List<JSONObject> articles = articleQueryService.getArticlesByTag(tagId, currentPageNum, pageSize);

			final int tagArticleCount = tag.getInt(Tag.TAG_PUBLISHED_REFERENCE_COUNT);
			final int pageCount = (int) Math.ceil((double) tagArticleCount / (double) pageSize);

			final boolean hasMultipleUsers = userQueryService.hasMultipleUsers();

			if (hasMultipleUsers) {
				filler.setArticlesExProperties(request, articles, preference);
			} else if (!articles.isEmpty()) {
				final JSONObject author = articleQueryService.getAuthor(articles.get(0));

				filler.setArticlesExProperties(request, articles, author, preference);
			}

			Collections.sort(articles, Comparators.ARTICLE_CREATE_DATE_COMPARATOR);

			final JSONObject result = new JSONObject();
			final JSONObject pagination = new JSONObject();

			pagination.put(Pagination.PAGINATION_PAGE_COUNT, pageCount);
			result.put(Pagination.PAGINATION, pagination);

			result.put(Article.ARTICLES, articles);

			jsonObject.put(Keys.RESULTS, result);
		} catch (final Exception e) {
			jsonObject.put(Keys.STATUS_CODE, false);
			logger.error("Gets article paged failed", e);
		} finally {
			Stopwatchs.end();
		}

		final JSONRenderer renderer = new JSONRenderer();
		renderer.setJSONObject(jsonObject);
		renderer.render(request, response);
	}

	/**
	 * Gets tag articles paged with the specified context.
	 *
	 * @param context
	 *            the specified context
	 * @param request
	 *            the specified request
	 */
	@RequestMapping(value = "/articles/archives/.+/\\d+", method = RequestMethod.GET)
	public void getArchivesArticlesByPage(final HttpServletRequest request, final HttpServletResponse response) {
		final JSONObject jsonObject = new JSONObject();

		final String archiveDateString = getArchivesArticlesPagedArchive(request.getRequestURI());
		final int currentPageNum = getArchivesArticlesPagedCurrentPageNum(request.getRequestURI());

		Stopwatchs
				.start("Get Archive-Articles Paged[archive=" + archiveDateString + ", pageNum=" + currentPageNum + ']');

		try {
			jsonObject.put(Keys.STATUS_CODE, true);

			final JSONObject preference = preferenceQueryService.getPreference();
			final int pageSize = preference.getInt(Option.ID_C_ARTICLE_LIST_DISPLAY_COUNT);

			final JSONObject archiveQueryResult = archiveDateQueryService.getByArchiveDateString(archiveDateString);

			if (null == archiveQueryResult) {
				throw new Exception("Can not found archive[archiveDate=" + archiveDateString + "]");
			}

			final JSONObject archiveDate = archiveQueryResult.getJSONObject(ArchiveDate.ARCHIVE_DATE);
			final String archiveDateId = archiveDate.getString(Keys.OBJECT_ID);

			final int articleCount = archiveDate.getInt(ArchiveDate.ARCHIVE_DATE_PUBLISHED_ARTICLE_COUNT);
			final int pageCount = (int) Math.ceil((double) articleCount / (double) pageSize);

			final List<JSONObject> articles = articleQueryService.getArticlesByArchiveDate(archiveDateId,
					currentPageNum, pageSize);

			final boolean hasMultipleUsers = userQueryService.hasMultipleUsers();

			if (hasMultipleUsers) {
				filler.setArticlesExProperties(request, articles, preference);
			} else if (!articles.isEmpty()) {
				final JSONObject author = articleQueryService.getAuthor(articles.get(0));
				filler.setArticlesExProperties(request, articles, author, preference);
			}

			Collections.sort(articles, Comparators.ARTICLE_CREATE_DATE_COMPARATOR);

			final JSONObject result = new JSONObject();
			final JSONObject pagination = new JSONObject();

			pagination.put(Pagination.PAGINATION_PAGE_COUNT, pageCount);
			result.put(Pagination.PAGINATION, pagination);

			result.put(Article.ARTICLES, articles);

			jsonObject.put(Keys.RESULTS, result);
		} catch (final Exception e) {
			jsonObject.put(Keys.STATUS_CODE, false);
			logger.error("Gets article paged failed", e);
		} finally {
			Stopwatchs.end();
		}

		final JSONRenderer renderer = new JSONRenderer();
		renderer.setJSONObject(jsonObject);
		renderer.render(request, response);
	}

	/**
	 * Gets author articles paged with the specified context.
	 *
	 * @param context
	 *            the specified context
	 * @param request
	 *            the specified request
	 */
	@RequestMapping(value = "/articles/authors/\\d+/\\d+", method = RequestMethod.GET)
	public void getAuthorsArticlesByPage(final HttpServletRequest request, final HttpServletResponse response) {
		final JSONObject jsonObject = new JSONObject();

		final String authorId = getAuthorsArticlesPagedAuthorId(request.getRequestURI());
		final int currentPageNum = getAuthorsArticlesPagedCurrentPageNum(request.getRequestURI());

		Stopwatchs.start("Get Author-Articles Paged[authorId=" + authorId + ", pageNum=" + currentPageNum + ']');

		try {
			jsonObject.put(Keys.STATUS_CODE, true);

			final JSONObject preference = preferenceQueryService.getPreference();
			final int pageSize = preference.getInt(Option.ID_C_ARTICLE_LIST_DISPLAY_COUNT);

			final JSONObject authorRet = userQueryService.getUser(authorId);

			if (null == authorRet) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}

			final JSONObject author = authorRet.getJSONObject(User.USER);
			final String authorEmail = author.optString(User.USER_EMAIL);

			final List<JSONObject> articles = articleQueryService.getArticlesByAuthorEmail(authorEmail, currentPageNum,
					pageSize);

			if (!articles.isEmpty()) {
				filler.setArticlesExProperties(request, articles, author, preference);
			}

			final int articleCount = author.getInt(UserExt.USER_PUBLISHED_ARTICLE_COUNT);
			final int pageCount = (int) Math.ceil((double) articleCount / (double) pageSize);

			final JSONObject result = new JSONObject();
			final JSONObject pagination = new JSONObject();

			pagination.put(Pagination.PAGINATION_PAGE_COUNT, pageCount);
			result.put(Pagination.PAGINATION, pagination);

			result.put(Article.ARTICLES, articles);

			jsonObject.put(Keys.RESULTS, result);
		} catch (final Exception e) {
			jsonObject.put(Keys.STATUS_CODE, false);
			logger.error("Gets article paged failed", e);
		} finally {
			Stopwatchs.end();
		}

		final JSONRenderer renderer = new JSONRenderer();
		renderer.setJSONObject(jsonObject);
		renderer.render(request, response);
	}

	/**
	 * Shows author articles with the specified context.
	 *
	 * @param context
	 *            the specified context
	 * @param request
	 *            the specified request
	 * @param response
	 *            the specified response
	 * @throws IOException
	 *             io exception
	 * @throws JSONException
	 *             json exception
	 */
	@RequestMapping(value = "/authors/**", method = RequestMethod.GET)
	public void showAuthorArticles(final HttpServletRequest request, final HttpServletResponse response)
			throws IOException, JSONException {
		final AbstractFreeMarkerRenderer renderer = new FreeMarkerRenderer();
		renderer.setTemplateName("author-articles.ftl");

		try {
			String requestURI = request.getRequestURI();
			if (!requestURI.endsWith("/")) {
				requestURI += "/";
			}

			final String authorId = getAuthorId(requestURI);
			logger.debug("Request author articles[requestURI={0}, authorId={1}]", requestURI, authorId);

			final int currentPageNum = getAuthorCurrentPageNum(requestURI, authorId);

			if (-1 == currentPageNum) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}

			logger.debug("Request author articles[authorId={0}, currentPageNum={1}]", authorId, currentPageNum);

			final JSONObject preference = preferenceQueryService.getPreference();

			if (null == preference) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}

			final int pageSize = preference.getInt(Option.ID_C_ARTICLE_LIST_DISPLAY_COUNT);
			final int windowSize = preference.getInt(Option.ID_C_ARTICLE_LIST_PAGINATION_WINDOW_SIZE);

			final JSONObject result = userQueryService.getUser(authorId);

			if (null == result) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}

			final JSONObject author = result.getJSONObject(User.USER);
			final String authorEmail = author.getString(User.USER_EMAIL);
			final List<JSONObject> articles = articleQueryService.getArticlesByAuthorEmail(authorEmail, currentPageNum,
					pageSize);

			if (articles.isEmpty()) {
				try {
					response.sendError(HttpServletResponse.SC_NOT_FOUND);
					return;
				} catch (final IOException ex) {
					logger.error(ex.getMessage());
				}
			}

			filler.setArticlesExProperties(request, articles, author, preference);

			if (preference.optBoolean(Option.ID_C_ENABLE_ARTICLE_UPDATE_HINT)) {
				Collections.sort(articles, Comparators.ARTICLE_UPDATE_DATE_COMPARATOR);
			} else {
				Collections.sort(articles, Comparators.ARTICLE_CREATE_DATE_COMPARATOR);
			}

			final int articleCount = author.getInt(UserExt.USER_PUBLISHED_ARTICLE_COUNT);
			final int pageCount = (int) Math.ceil((double) articleCount / (double) pageSize);

			final List<Integer> pageNums = Paginator.paginate(currentPageNum, pageSize, pageCount, windowSize);

			final Map<String, Object> dataModel = renderer.getDataModel();

			prepareShowAuthorArticles(pageNums, dataModel, pageCount, currentPageNum, articles, author);
			filler.fillBlogHeader(request, response, dataModel, preference);
			filler.fillBlogFooter(request, dataModel, preference);
			filler.fillSide(request, dataModel, preference);
			skins.fillLangs(preference.optString(Option.ID_C_LOCALE_STRING),
					(String) request.getAttribute(Keys.TEMAPLTE_DIR_NAME), dataModel);

			statisticMgmtService.incBlogViewCount(request, response);
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
	 * Shows archive articles with the specified context.
	 *
	 * @param context
	 *            the specified context
	 * @param request
	 *            the specified request
	 * @param response
	 *            the specified response
	 */
	@RequestMapping(value = "/archives/**", method = RequestMethod.GET)
	public void showArchiveArticles(final HttpServletRequest request, final HttpServletResponse response) {
		final AbstractFreeMarkerRenderer renderer = new FreeMarkerRenderer();
		renderer.setTemplateName("archive-articles.ftl");

		try {
			String requestURI = request.getRequestURI();
			if (!requestURI.endsWith("/")) {
				requestURI += "/";
			}

			final String archiveDateString = getArchiveDate(requestURI);
			final int currentPageNum = getArchiveCurrentPageNum(requestURI);

			if (-1 == currentPageNum) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}

			logger.debug("Request archive date[string={0}, currentPageNum={1}]", archiveDateString, currentPageNum);
			final JSONObject result = archiveDateQueryService.getByArchiveDateString(archiveDateString);

			if (null == result) {
				logger.warn("Can not find articles for the specified archive date[string={0}]", archiveDateString);
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}

			final JSONObject archiveDate = result.getJSONObject(ArchiveDate.ARCHIVE_DATE);
			final String archiveDateId = archiveDate.getString(Keys.OBJECT_ID);

			final JSONObject preference = preferenceQueryService.getPreference();
			final int pageSize = preference.getInt(Option.ID_C_ARTICLE_LIST_DISPLAY_COUNT);

			final int articleCount = archiveDate.getInt(ArchiveDate.ARCHIVE_DATE_PUBLISHED_ARTICLE_COUNT);
			final int pageCount = (int) Math.ceil((double) articleCount / (double) pageSize);

			final List<JSONObject> articles = articleQueryService.getArticlesByArchiveDate(archiveDateId,
					currentPageNum, pageSize);

			if (articles.isEmpty()) {
				try {
					response.sendError(HttpServletResponse.SC_NOT_FOUND);
					return;
				} catch (final IOException ex) {
					logger.error(ex.getMessage());
				}
			}

			final boolean hasMultipleUsers = userQueryService.hasMultipleUsers();

			if (hasMultipleUsers) {
				filler.setArticlesExProperties(request, articles, preference);
			} else if (!articles.isEmpty()) {
				final JSONObject author = articleQueryService.getAuthor(articles.get(0));

				filler.setArticlesExProperties(request, articles, author, preference);
			}

			sort(preference, articles);

			final Map<String, Object> dataModel = renderer.getDataModel();

			skins.fillLangs(preference.optString(Option.ID_C_LOCALE_STRING),
					(String) request.getAttribute(Keys.TEMAPLTE_DIR_NAME), dataModel);

			prepareShowArchiveArticles(preference, dataModel, articles, currentPageNum, pageCount, archiveDateString,
					archiveDate);
			filler.fillBlogHeader(request, response, dataModel, preference);
			filler.fillBlogFooter(request, dataModel, preference);
			filler.fillSide(request, dataModel, preference);

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

	/**
	 * Updates article random double value.
	 *
	 * @param request
	 *            the specified request
	 */
	@RequestMapping(value = "/article-random-double-gen.do", method = RequestMethod.GET)
	public void updateArticlesRandomValue(final HttpServletRequest request) {
		// Commented for issue 308, see
		// http://code.google.com/p/b3log-solo/issues/detail?id=308#c4 and
		// cron.xml for more details.
		int DEFAULT_UPDATE_CNT = 0;
		int updateCnt = DEFAULT_UPDATE_CNT;
		try {
			updateCnt = Integer.valueOf(request.getParameter("cnt"));
		} catch (final NumberFormatException e) {
			logger.warn(e.getMessage(), e);
		}

		try {
			articleMgmtService.updateArticlesRandomValue(updateCnt);
		} catch (final ServiceException e) {
			logger.error("Updates articles random values failed", e);
		}
	}

	/**
	 * Shows an article with the specified context.
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
	@RequestMapping(value = "/article", method = RequestMethod.GET)
	public void showArticle(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
		// See PermalinkFilter#dispatchToArticleOrPageProcessor()
		final JSONObject article = (JSONObject) request.getAttribute(Article.ARTICLE);

		if (null == article) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		final String articleId = article.optString(Keys.OBJECT_ID);

		logger.debug("Article[id={0}]", articleId);
		final AbstractFreeMarkerRenderer renderer = new FreeMarkerRenderer();

		renderer.setTemplateName("article.ftl");

		try {
			final JSONObject preference = preferenceQueryService.getPreference();

			final boolean allowVisitDraftViaPermalink = preference
					.getBoolean(Option.ID_C_ALLOW_VISIT_DRAFT_VIA_PERMALINK);

			if (!article.optBoolean(Article.ARTICLE_IS_PUBLISHED) && !allowVisitDraftViaPermalink) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND);

				return;
			}

			logger.trace("Article[title={0}]", article.getString(Article.ARTICLE_TITLE));

			articleQueryService.markdown(article);

			// For <meta name="description"
			// content="${article.articleAbstract}"/>
			final String metaDescription = Jsoup.parse(article.optString(Article.ARTICLE_ABSTRACT)).text();

			article.put(Article.ARTICLE_ABSTRACT, metaDescription);

			if (preference.getBoolean(Option.ID_C_ENABLE_ARTICLE_UPDATE_HINT)) {
				article.put(Common.HAS_UPDATED, articleQueryService.hasUpdated(article));
			} else {
				article.put(Common.HAS_UPDATED, false);
			}

			final JSONObject author = articleQueryService.getAuthor(article);
			final String authorName = author.getString(User.USER_NAME);

			article.put(Common.AUTHOR_NAME, authorName);
			final String authorId = author.getString(Keys.OBJECT_ID);

			article.put(Common.AUTHOR_ID, authorId);
			article.put(Common.AUTHOR_ROLE, author.getString(User.USER_ROLE));
			final String userAvatar = author.optString(UserExt.USER_AVATAR);
			if (!StringUtils.isBlank(userAvatar)) {
				article.put(Common.AUTHOR_THUMBNAIL_URL, userAvatar);
			} else {
				final String thumbnailURL = Thumbnails.getGravatarURL(author.optString(User.USER_EMAIL), "128");
				article.put(Common.AUTHOR_THUMBNAIL_URL, thumbnailURL);
			}

			final Map<String, Object> dataModel = renderer.getDataModel();

			prepareShowArticle(preference, dataModel, article);

			filler.fillBlogHeader(request, response, dataModel, preference);
			filler.fillBlogFooter(request, dataModel, preference);
			filler.fillSide(request, dataModel, preference);
			skins.fillLangs(preference.optString(Option.ID_C_LOCALE_STRING),
					(String) request.getAttribute(Keys.TEMAPLTE_DIR_NAME), dataModel);

			if (!Requests.hasBeenServed(request, response)) {
				articleMgmtService.incViewCount(articleId);
			}

			statisticMgmtService.incBlogViewCount(request, response);

			// Fire [Before Render Article] event
			final JSONObject eventData = new JSONObject();

			eventData.put(Article.ARTICLE, article);

			action(eventData);
			/*
			 * try { eventManager.fireEventSynchronously(new
			 * Event<JSONObject>(EventTypes.BEFORE_RENDER_ARTICLE, eventData));
			 * } catch (final EventException e) { logger.error( "Fires [" +
			 * EventTypes.BEFORE_RENDER_ARTICLE + "] event failed", e); }
			 */
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

	public void action(JSONObject data) throws EventException {
		final JSONObject article = data.optJSONObject(Article.ARTICLE);

		String content = article.optString(Article.ARTICLE_CONTENT);

		final Document doc = Jsoup.parse(content, StringUtils.EMPTY, Parser.htmlParser());
		doc.outputSettings().prettyPrint(false);

		final StringBuilder listBuilder = new StringBuilder();

		listBuilder.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"" + Latkes.getStaticServePath()
				+ "/plugins/list/style.css\" />");

		final Elements hs = doc.select("h1, h2, h3, h4, h5");

		listBuilder.append("<ul class='b3-solo-list'>");
		for (int i = 0; i < hs.size(); i++) {
			final Element element = hs.get(i);
			final String tagName = element.tagName().toLowerCase();
			final String text = element.text();
			final String id = "b3_solo_" + tagName + "_" + i;

			element.before("<span id='" + id + "'></span>");

			listBuilder.append("<li class='b3-solo-list-").append(tagName).append("'><a href='#").append(id)
					.append("'>").append(text).append("</a></li>");
		}
		listBuilder.append("</ul>");

		final Element body = doc.getElementsByTag("body").get(0);

		content = listBuilder.toString() + body.html();

		article.put(Article.ARTICLE_CONTENT, content);
	}

	/**
	 * Sorts the specified articles by the specified preference.
	 *
	 * @param preference
	 *            the specified preference
	 * @param articles
	 *            the specified articles
	 * @throws JSONException
	 *             json exception
	 * @see Comparators#ARTICLE_UPDATE_DATE_COMPARATOR
	 * @see Comparators#ARTICLE_CREATE_DATE_COMPARATOR
	 */
	private void sort(final JSONObject preference, final List<JSONObject> articles) throws JSONException {
		// if (preference.getBoolean(Option.ID_C_ENABLE_ARTICLE_UPDATE_HINT)) {
		// Collections.sort(articles,
		// Comparators.ARTICLE_UPDATE_DATE_COMPARATOR);
		// } else {
		Collections.sort(articles, Comparators.ARTICLE_CREATE_DATE_COMPARATOR);
		// }
	}

	/**
	 * Gets archive date from the specified URI.
	 *
	 * @param requestURI
	 *            the specified request URI
	 * @return archive date
	 */
	private static String getArchiveDate(final String requestURI) {
		final String path = requestURI.substring((Latkes.getContextPath() + "/archives/").length());

		return path.substring(0, "yyyy/MM".length());
	}

	/**
	 * Gets the request page number from the specified request URI.
	 *
	 * @param requestURI
	 *            the specified request URI
	 * @return page number, returns {@code -1} if the specified request URI can
	 *         not convert to an number
	 */
	private static int getArchiveCurrentPageNum(final String requestURI) {
		final String pageNumString = requestURI.substring((Latkes.getContextPath() + "/archives/yyyy/MM/").length());

		return Requests.getCurrentPageNum(pageNumString);
	}

	/**
	 * Gets author id from the specified URI.
	 *
	 * @param requestURI
	 *            the specified request URI
	 * @return author id
	 */
	private static String getAuthorId(final String requestURI) {
		final String path = requestURI.substring((Latkes.getContextPath() + "/authors/").length());

		final int idx = path.indexOf("/");

		if (-1 == idx) {
			return path.substring(0);
		} else {
			return path.substring(0, idx);
		}
	}

	/**
	 * Gets the request page number from the specified request URI.
	 *
	 * @param requestURI
	 *            the specified request URI
	 * @return page number
	 */
	private static int getArticlesPagedCurrentPageNum(final String requestURI) {
		final String pageNumString = requestURI.substring((Latkes.getContextPath() + "/articles/").length());

		return Requests.getCurrentPageNum(pageNumString);
	}

	/**
	 * Gets the request page number from the specified request URI.
	 *
	 * @param requestURI
	 *            the specified request URI
	 * @return page number
	 */
	private static int getTagArticlesPagedCurrentPageNum(final String requestURI) {
		return Requests.getCurrentPageNum(StringUtils.substringAfterLast(requestURI, "/"));
	}

	/**
	 * Gets the request tag from the specified request URI.
	 *
	 * @param requestURI
	 *            the specified request URI
	 * @return tag
	 */
	private static String getTagArticlesPagedTag(final String requestURI) {
		String tagAndPageNum = requestURI.substring((Latkes.getContextPath() + "/articles/tags/").length());

		if (tagAndPageNum.endsWith("/")) {
			tagAndPageNum = StringUtils.removeEnd(tagAndPageNum, "/");
		}

		return StringUtils.substringBefore(tagAndPageNum, "/");
	}

	/**
	 * Gets the request page number from the specified request URI.
	 *
	 * @param requestURI
	 *            the specified request URI
	 * @return page number
	 */
	private static int getArchivesArticlesPagedCurrentPageNum(final String requestURI) {
		return Requests.getCurrentPageNum(StringUtils.substringAfterLast(requestURI, "/"));
	}

	/**
	 * Gets the request archive from the specified request URI.
	 *
	 * @param requestURI
	 *            the specified request URI
	 * @return archive, for example "2012/05"
	 */
	private static String getArchivesArticlesPagedArchive(final String requestURI) {
		String archiveAndPageNum = requestURI.substring((Latkes.getContextPath() + "/articles/archives/").length());

		if (archiveAndPageNum.endsWith("/")) {
			archiveAndPageNum = StringUtils.removeEnd(archiveAndPageNum, "/");
		}

		return StringUtils.substringBeforeLast(archiveAndPageNum, "/");
	}

	/**
	 * Gets the request page number from the specified request URI.
	 *
	 * @param requestURI
	 *            the specified request URI
	 * @return page number
	 */
	private static int getAuthorsArticlesPagedCurrentPageNum(final String requestURI) {
		return Requests.getCurrentPageNum(StringUtils.substringAfterLast(requestURI, "/"));
	}

	/**
	 * Gets the request author id from the specified request URI.
	 *
	 * @param requestURI
	 *            the specified request URI
	 * @return author id
	 */
	private static String getAuthorsArticlesPagedAuthorId(final String requestURI) {
		String authorIdAndPageNum = requestURI.substring((Latkes.getContextPath() + "/articles/authors/").length());

		if (authorIdAndPageNum.endsWith("/")) {
			authorIdAndPageNum = StringUtils.removeEnd(authorIdAndPageNum, "/");
		}

		return StringUtils.substringBefore(authorIdAndPageNum, "/");
	}

	/**
	 * Gets the request page number from the specified request URI and author
	 * id.
	 *
	 * @param requestURI
	 *            the specified request URI
	 * @param authorId
	 *            the specified author id
	 * @return page number
	 */
	private static int getAuthorCurrentPageNum(final String requestURI, final String authorId) {
		final String pageNumString = requestURI
				.substring((Latkes.getContextPath() + "/authors/" + authorId + "/").length());

		return Requests.getCurrentPageNum(pageNumString);
	}

	/**
	 * Gets the random articles.
	 *
	 * @param preference
	 *            the specified preference
	 * @return a list of articles, returns an empty list if not found
	 */
	private List<JSONObject> getRandomArticles(final JSONObject preference) {
		try {
			final int displayCnt = preference.getInt(Option.ID_C_RANDOM_ARTICLES_DISPLAY_CNT);
			final List<JSONObject> ret = articleQueryService.getArticlesRandomly(displayCnt);

			return ret;
		} catch (final Exception e) {
			logger.error(e.getMessage(), e);

			return Collections.emptyList();
		}
	}

	/**
	 * Prepares the specified data model for rendering author articles.
	 *
	 * @param pageNums
	 *            the specified page numbers
	 * @param dataModel
	 *            the specified data model
	 * @param pageCount
	 *            the specified page count
	 * @param currentPageNum
	 *            the specified current page number
	 * @param articles
	 *            the specified articles
	 * @param author
	 *            the specified author
	 * @throws ServiceException
	 *             service exception
	 */
	private void prepareShowAuthorArticles(final List<Integer> pageNums, final Map<String, Object> dataModel,
			final int pageCount, final int currentPageNum, final List<JSONObject> articles, final JSONObject author)
			throws ServiceException {
		if (0 != pageNums.size()) {
			dataModel.put(Pagination.PAGINATION_FIRST_PAGE_NUM, pageNums.get(0));
			dataModel.put(Pagination.PAGINATION_LAST_PAGE_NUM, pageNums.get(pageNums.size() - 1));
		}
		dataModel.put(Pagination.PAGINATION_PAGE_COUNT, pageCount);
		dataModel.put(Pagination.PAGINATION_PAGE_NUMS, pageNums);

		dataModel.put(Pagination.PAGINATION_CURRENT_PAGE_NUM, currentPageNum);
		final String previousPageNum = Integer.toString(currentPageNum > 1 ? currentPageNum - 1 : 0);

		dataModel.put(Pagination.PAGINATION_PREVIOUS_PAGE_NUM, "0".equals(previousPageNum) ? "" : previousPageNum);
		if (pageCount == currentPageNum + 1) { // The next page is the last page
			dataModel.put(Pagination.PAGINATION_NEXT_PAGE_NUM, "");
		} else {
			dataModel.put(Pagination.PAGINATION_NEXT_PAGE_NUM, currentPageNum + 1);
		}

		dataModel.put(Article.ARTICLES, articles);
		final String authorId = author.optString(Keys.OBJECT_ID);

		dataModel.put(Common.PATH, "/authors/" + authorId);
		dataModel.put(Keys.OBJECT_ID, authorId);

		dataModel.put(Common.AUTHOR_NAME, author.optString(User.USER_NAME));

		final String userAvatar = author.optString(UserExt.USER_AVATAR);
		if (!StringUtils.isBlank(userAvatar)) {
			dataModel.put(Common.AUTHOR_THUMBNAIL_URL, userAvatar);
		} else {
			final String thumbnailURL = Thumbnails.getGravatarURL(author.optString(User.USER_EMAIL), "128");
			dataModel.put(Common.AUTHOR_THUMBNAIL_URL, thumbnailURL);
		}

		dataModel.put(Pagination.PAGINATION_CURRENT_PAGE_NUM, currentPageNum);
	}

	/**
	 * Prepares the specified data model for rendering archive articles.
	 *
	 * @param preference
	 *            the specified preference
	 * @param dataModel
	 *            the specified data model
	 * @param articles
	 *            the specified articles
	 * @param currentPageNum
	 *            the specified current page number
	 * @param pageCount
	 *            the specified page count
	 * @param archiveDateString
	 *            the specified archive data string
	 * @param archiveDate
	 *            the specified archive date
	 * @return page title for caching
	 * @throws Exception
	 *             exception
	 */
	private String prepareShowArchiveArticles(final JSONObject preference, final Map<String, Object> dataModel,
			final List<JSONObject> articles, final int currentPageNum, final int pageCount,
			final String archiveDateString, final JSONObject archiveDate) throws Exception {
		final int pageSize = preference.getInt(Option.ID_C_ARTICLE_LIST_DISPLAY_COUNT);
		final int windowSize = preference.getInt(Option.ID_C_ARTICLE_LIST_PAGINATION_WINDOW_SIZE);

		final List<Integer> pageNums = Paginator.paginate(currentPageNum, pageSize, pageCount, windowSize);

		dataModel.put(Article.ARTICLES, articles);
		final String previousPageNum = Integer.toString(currentPageNum > 1 ? currentPageNum - 1 : 0);

		dataModel.put(Pagination.PAGINATION_PREVIOUS_PAGE_NUM, "0".equals(previousPageNum) ? "" : previousPageNum);
		if (pageCount == currentPageNum + 1) { // The next page is the last page
			dataModel.put(Pagination.PAGINATION_NEXT_PAGE_NUM, "");
		} else {
			dataModel.put(Pagination.PAGINATION_NEXT_PAGE_NUM, currentPageNum + 1);
		}
		dataModel.put(Pagination.PAGINATION_CURRENT_PAGE_NUM, currentPageNum);
		dataModel.put(Pagination.PAGINATION_FIRST_PAGE_NUM, pageNums.get(0));
		dataModel.put(Pagination.PAGINATION_LAST_PAGE_NUM, pageNums.get(pageNums.size() - 1));
		dataModel.put(Pagination.PAGINATION_PAGE_COUNT, pageCount);
		dataModel.put(Pagination.PAGINATION_PAGE_NUMS, pageNums);
		dataModel.put(Common.PATH, "/archives/" + archiveDateString);
		dataModel.put(Keys.OBJECT_ID, archiveDate.getString(Keys.OBJECT_ID));

		final long time = archiveDate.getLong(ArchiveDate.ARCHIVE_TIME);
		final String dateString = DateFormatUtils.format(time, "yyyy/MM");
		final String[] dateStrings = dateString.split("/");
		final String year = dateStrings[0];
		final String month = dateStrings[1];

		archiveDate.put(ArchiveDate.ARCHIVE_DATE_YEAR, year);
		final String language = Locales.getLanguage(preference.getString(Option.ID_C_LOCALE_STRING));
		String ret;

		if ("en".equals(language)) {
			archiveDate.put(ArchiveDate.ARCHIVE_DATE_MONTH, Dates.EN_MONTHS.get(month));
			ret = Dates.EN_MONTHS.get(month) + " " + year;
		} else {
			archiveDate.put(ArchiveDate.ARCHIVE_DATE_MONTH, month);
			ret = year + " " + dataModel.get("yearLabel") + " " + month + " " + dataModel.get("monthLabel");
		}
		dataModel.put(ArchiveDate.ARCHIVE_DATE, archiveDate);

		return ret;
	}

	/**
	 * Prepares the specified data model for rendering article.
	 *
	 * @param preference
	 *            the specified preference
	 * @param dataModel
	 *            the specified data model
	 * @param article
	 *            the specified article
	 * @throws Exception
	 *             exception
	 */
	private void prepareShowArticle(final JSONObject preference, final Map<String, Object> dataModel,
			final JSONObject article) throws Exception {
		article.put(Common.COMMENTABLE,
				preference.getBoolean(Option.ID_C_COMMENTABLE) && article.getBoolean(Article.ARTICLE_COMMENTABLE));
		article.put(Common.PERMALINK, article.getString(Article.ARTICLE_PERMALINK));
		dataModel.put(Article.ARTICLE, article);
		final String articleId = article.getString(Keys.OBJECT_ID);

		Stopwatchs.start("Get Article Sign");
		logger.debug("Getting article sign....");
		article.put(Common.ARTICLE_SIGN,
				articleQueryService.getSign(article.getString(Article.ARTICLE_SIGN_ID), preference));
		logger.debug("Got article sign");
		Stopwatchs.end();

		Stopwatchs.start("Get Next Article");
		logger.debug("Getting the next article....");
		final JSONObject nextArticle = articleQueryService.getNextArticle(articleId);

		if (null != nextArticle) {
			dataModel.put(Common.NEXT_ARTICLE_PERMALINK, nextArticle.getString(Article.ARTICLE_PERMALINK));
			dataModel.put(Common.NEXT_ARTICLE_TITLE, nextArticle.getString(Article.ARTICLE_TITLE));
			dataModel.put(Common.NEXT_ARTICLE_ABSTRACT, nextArticle.getString(Article.ARTICLE_ABSTRACT));
			logger.debug("Got the next article");
		}
		Stopwatchs.end();

		Stopwatchs.start("Get Previous Article");
		logger.debug("Getting the previous article....");
		final JSONObject previousArticle = articleQueryService.getPreviousArticle(articleId);

		if (null != previousArticle) {
			dataModel.put(Common.PREVIOUS_ARTICLE_PERMALINK, previousArticle.getString(Article.ARTICLE_PERMALINK));
			dataModel.put(Common.PREVIOUS_ARTICLE_TITLE, previousArticle.getString(Article.ARTICLE_TITLE));
			dataModel.put(Common.PREVIOUS_ARTICLE_ABSTRACT, previousArticle.getString(Article.ARTICLE_ABSTRACT));
			logger.debug("Got the previous article");
		}
		Stopwatchs.end();

		Stopwatchs.start("Get Article CMTs");
		logger.debug("Getting article's comments....");
		final int cmtCount = article.getInt(Article.ARTICLE_COMMENT_COUNT);

		if (0 != cmtCount) {
			final List<JSONObject> articleComments = commentQueryService.getComments(articleId);

			dataModel.put(Article.ARTICLE_COMMENTS_REF, articleComments);
		} else {
			dataModel.put(Article.ARTICLE_COMMENTS_REF, Collections.emptyList());
		}
		logger.debug("Got article's comments");
		Stopwatchs.end();

		dataModel.put(Option.ID_C_EXTERNAL_RELEVANT_ARTICLES_DISPLAY_CNT,
				preference.getInt(Option.ID_C_EXTERNAL_RELEVANT_ARTICLES_DISPLAY_CNT));
		dataModel.put(Option.ID_C_RANDOM_ARTICLES_DISPLAY_CNT,
				preference.getInt(Option.ID_C_RANDOM_ARTICLES_DISPLAY_CNT));
		dataModel.put(Option.ID_C_RELEVANT_ARTICLES_DISPLAY_CNT,
				preference.getInt(Option.ID_C_RELEVANT_ARTICLES_DISPLAY_CNT));
	}
}
