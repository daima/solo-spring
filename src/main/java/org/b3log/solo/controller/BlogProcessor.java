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
package org.b3log.solo.controller;

import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.b3log.solo.Keys;
import org.b3log.solo.Latkes;
import org.b3log.solo.RuntimeEnv;
import org.b3log.solo.SoloConstant;
import org.b3log.solo.frame.urlfetch.HTTPRequest;
import org.b3log.solo.frame.urlfetch.URLFetchService;
import org.b3log.solo.frame.urlfetch.URLFetchServiceFactory;
import org.b3log.solo.model.Article;
import org.b3log.solo.model.Option;
import org.b3log.solo.model.Pagination;
import org.b3log.solo.model.Statistic;
import org.b3log.solo.model.Tag;
import org.b3log.solo.model.User;
import org.b3log.solo.renderer.JSONRenderer;
import org.b3log.solo.service.ArticleQueryService;
import org.b3log.solo.service.PreferenceQueryService;
import org.b3log.solo.service.StatisticQueryService;
import org.b3log.solo.service.TagQueryService;
import org.b3log.solo.service.UserQueryService;
import org.b3log.solo.util.MD5;
import org.b3log.solo.util.PropsUtil;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Blog processor.
 *
 * @author <a href="http://cxy7.com">XyCai</a>
 * @version 1.3.0.4, Dec 17, 2015
 * @since 0.4.6
 */
@Controller
public class BlogProcessor {

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
	 * Statistic query service.
	 */
	@Autowired
	private StatisticQueryService statisticQueryService;

	/**
	 * User query service.
	 */
	@Autowired
	private UserQueryService userQueryService;

	/**
	 * Preference query service.
	 */
	@Autowired
	private PreferenceQueryService preferenceQueryService;

	/**
	 * URL fetch service.
	 */
	private final URLFetchService urlFetchService = URLFetchServiceFactory.getURLFetchService();

	/**
	 * Gets blog information.
	 *
	 * <ul>
	 * <li>Time of the recent updated article</li>
	 * <li>Article count</li>
	 * <li>Comment count</li>
	 * <li>Tag count</li>
	 * <li>Serve path</li>
	 * <li>Static serve path</li>
	 * <li>Solo version</li>
	 * <li>Runtime environment (LOCAL)</li>
	 * <li>Locale</li>
	 * </ul>
	 *
	 * @param context
	 *            the specified context
	 * @throws Exception
	 *             exception
	 */
	@RequestMapping(value = "/blog/info", method = RequestMethod.GET)
	public void getBlogInfo(final HttpServletRequest request, final HttpServletResponse response) throws Exception {
		final JSONObject jsonObject = new JSONObject();

		jsonObject.put("recentArticleTime", articleQueryService.getRecentArticleTime());
		final JSONObject statistic = statisticQueryService.getStatistic();

		jsonObject.put("articleCount", statistic.getLong(Statistic.STATISTIC_PUBLISHED_ARTICLE_COUNT));
		jsonObject.put("commentCount", statistic.getLong(Statistic.STATISTIC_PUBLISHED_BLOG_COMMENT_COUNT));
		jsonObject.put("tagCount", tagQueryService.getTagCount());
		jsonObject.put("servePath", Latkes.getServePath());
		jsonObject.put("staticServePath", Latkes.getStaticServePath());
		jsonObject.put("version", SoloConstant.VERSION);
		jsonObject.put("locale", Latkes.getLocale());
		jsonObject.put("runtimeMode", Latkes.getRuntimeMode());
		final RuntimeEnv runtimeEnv = Latkes.getRuntimeEnv();

		jsonObject.put("runtimeEnv", runtimeEnv);
		if (RuntimeEnv.LOCAL == runtimeEnv) {
			jsonObject.put("runtimeDatabase", Latkes.getRuntimeDatabase());
		}
		final JSONRenderer renderer = new JSONRenderer();
		renderer.setJSONObject(jsonObject);
		renderer.render(request, response);
	}

	/**
	 * Sync user to https://hacpai.com.
	 *
	 * @param context
	 *            the specified context
	 * @throws Exception
	 *             exception
	 */
	@RequestMapping(value = "/blog/symphony/user", method = RequestMethod.GET)
	public void syncUser(final HttpServletRequest request, final HttpServletResponse response) throws Exception {
		if (Latkes.getServePath().contains("localhost")) {
			return;
		}

		final JSONObject preference = preferenceQueryService.getPreference();

		if (null == preference) {
			return; // not init yet
		}

		final HTTPRequest httpRequest = new HTTPRequest();

		httpRequest.setURL(new URL(PropsUtil.getString("symphony.servePath") + "/apis/user"));
		httpRequest.setRequestMethod(RequestMethod.POST);
		final JSONObject jsonObject = new JSONObject();

		final JSONObject admin = userQueryService.getAdmin();

		jsonObject.put(User.USER_NAME, admin.getString(User.USER_NAME));
		jsonObject.put(User.USER_EMAIL, admin.getString(User.USER_EMAIL));
		jsonObject.put(User.USER_PASSWORD, admin.getString(User.USER_PASSWORD));
		jsonObject.put("userB3Key", preference.optString(Option.ID_C_KEY_OF_SOLO));
		jsonObject.put("clientHost", Latkes.getServePath());

		httpRequest.setPayload(jsonObject.toString().getBytes("UTF-8"));

		urlFetchService.fetchAsync(httpRequest);
		final JSONRenderer renderer = new JSONRenderer();
		renderer.setJSONObject(jsonObject);
		renderer.render(request, response);
	}

	/**
	 * Gets tags of all articles.
	 *
	 * <pre>
	 * {
	 *     "data": [
	 *         ["tag1", "tag2", ....], // tags of one article
	 *         ["tagX", "tagY", ....], // tags of another article
	 *         ....
	 *     ]
	 * }
	 * </pre>
	 *
	 * @param context
	 *            the specified context
	 * @param request
	 *            the specified HTTP servlet request
	 * @param response
	 *            the specified HTTP servlet response
	 * @throws Exception
	 *             io exception
	 */
	@RequestMapping(value = "/blog/articles-tags", method = RequestMethod.GET)
	public void getArticlesTags(final HttpServletRequest request, final HttpServletResponse response) throws Exception {
		final String pwd = request.getParameter("pwd");

		if (StringUtils.isBlank(pwd)) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
			return;
		}

		final JSONObject admin = userQueryService.getAdmin();

		if (!MD5.hash(pwd).equals(admin.getString(User.USER_PASSWORD))) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
			return;
		}

		final JSONObject requestJSONObject = new JSONObject();

		requestJSONObject.put(Pagination.PAGINATION_CURRENT_PAGE_NUM, 1);
		requestJSONObject.put(Pagination.PAGINATION_PAGE_SIZE, Integer.MAX_VALUE);
		requestJSONObject.put(Pagination.PAGINATION_WINDOW_SIZE, Integer.MAX_VALUE);
		requestJSONObject.put(Article.ARTICLE_IS_PUBLISHED, true);

		final JSONArray excludes = new JSONArray();

		excludes.put(Article.ARTICLE_CONTENT);
		excludes.put(Article.ARTICLE_UPDATE_DATE);
		excludes.put(Article.ARTICLE_CREATE_DATE);
		excludes.put(Article.ARTICLE_AUTHOR_EMAIL);
		excludes.put(Article.ARTICLE_HAD_BEEN_PUBLISHED);
		excludes.put(Article.ARTICLE_IS_PUBLISHED);
		excludes.put(Article.ARTICLE_RANDOM_DOUBLE);

		requestJSONObject.put(Keys.EXCLUDES, excludes);

		final JSONObject result = articleQueryService.getArticles(requestJSONObject);
		final JSONArray articles = result.optJSONArray(Article.ARTICLES);

		final JSONObject ret = new JSONObject();

		final JSONArray data = new JSONArray();

		ret.put("data", data);

		for (int i = 0; i < articles.length(); i++) {
			final JSONObject article = articles.optJSONObject(i);
			final String tagString = article.optString(Article.ARTICLE_TAGS_REF);

			final JSONArray tagArray = new JSONArray();

			data.put(tagArray);

			final String[] tags = tagString.split(",");

			for (final String tag : tags) {
				final String trim = tag.trim();

				if (!StringUtils.isBlank(trim)) {
					tagArray.put(tag);
				}
			}
		}
		final JSONRenderer renderer = new JSONRenderer();
		renderer.setJSONObject(ret);
		renderer.render(request, response);
	}

	/**
	 * Gets interest tags (top 10 and bottom 10).
	 *
	 * <pre>
	 * {
	 *     "data": ["tag1", "tag2", ....]
	 * }
	 * </pre>
	 *
	 * @param context
	 *            the specified context
	 * @param request
	 *            the specified HTTP servlet request
	 * @param response
	 *            the specified HTTP servlet response
	 * @throws Exception
	 *             io exception
	 */
	@RequestMapping(value = "/blog/interest-tags", method = RequestMethod.GET)
	public void getInterestTags(final HttpServletRequest request, final HttpServletResponse response) throws Exception {
		final JSONObject ret = new JSONObject();
		final Set<String> tagTitles = new HashSet<>();

		final List<JSONObject> topTags = tagQueryService.getTopTags(10);
		for (final JSONObject topTag : topTags) {
			tagTitles.add(topTag.optString(Tag.TAG_TITLE));
		}

		final List<JSONObject> bottomTags = tagQueryService.getBottomTags(10);
		for (final JSONObject bottomTag : bottomTags) {
			tagTitles.add(bottomTag.optString(Tag.TAG_TITLE));
		}

		ret.put("data", tagTitles);
		final JSONRenderer renderer = new JSONRenderer();
		renderer.setJSONObject(ret);
		renderer.render(request, response);
	}
}
