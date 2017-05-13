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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;
import org.b3log.solo.Keys;
import org.b3log.solo.Latkes;
import org.b3log.solo.SoloConstant;
import org.b3log.solo.dao.ArticleDao;
import org.b3log.solo.dao.TagArticleDao;
import org.b3log.solo.dao.TagDao;
import org.b3log.solo.frame.model.User;
import org.b3log.solo.frame.repository.CompositeFilter;
import org.b3log.solo.frame.repository.CompositeFilterOperator;
import org.b3log.solo.frame.repository.Filter;
import org.b3log.solo.frame.repository.FilterOperator;
import org.b3log.solo.frame.repository.PropertyFilter;
import org.b3log.solo.frame.repository.Query;
import org.b3log.solo.frame.repository.SortDirection;
import org.b3log.solo.frame.servlet.renderer.AtomRenderer;
import org.b3log.solo.frame.servlet.renderer.RssRenderer;
import org.b3log.solo.model.Article;
import org.b3log.solo.model.Option;
import org.b3log.solo.model.Tag;
import org.b3log.solo.model.feed.atom.Category;
import org.b3log.solo.model.feed.atom.Entry;
import org.b3log.solo.model.feed.atom.Feed;
import org.b3log.solo.model.feed.rss.Channel;
import org.b3log.solo.model.feed.rss.Item;
import org.b3log.solo.service.ArticleQueryService;
import org.b3log.solo.service.PreferenceQueryService;
import org.b3log.solo.service.UserQueryService;
import org.b3log.solo.util.Locales;
import org.b3log.solo.util.Strings;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Feed (Atom/RSS) processor.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @author <a href="https://github.com/feroozkhanchintu">feroozkhanchintu</a>
 * @version 1.1.0.6, Sep 28, 2016
 * @since 0.3.1
 */
@Controller
public class FeedProcessor {

	/**
	 * Logger.
	 */
	private static Logger logger = LoggerFactory.getLogger(FeedProcessor.class);

	/**
	 * Article query service.
	 */
	@Autowired
	private ArticleQueryService articleQueryService;

	/**
	 * Article repository.
	 */
	@Autowired
	private ArticleDao articleDao;

	/**
	 * Preference query service.
	 */
	@Autowired
	private PreferenceQueryService preferenceQueryService;

	/**
	 * User query service.
	 */
	@Autowired
	private UserQueryService userQueryService;

	/**
	 * Tag repository.
	 */
	@Autowired
	private TagDao tagDao;

	/**
	 * Tag-Article repository.
	 */
	@Autowired
	private TagArticleDao tagArticleDao;

	/**
	 * Blog articles Atom output.
	 *
	 * @param context
	 *            the specified context
	 */
	@RequestMapping(value = { "/blog-articles-feed.do" }, method = { RequestMethod.GET, RequestMethod.HEAD })
	public void blogArticlesAtom(final HttpServletRequest request, final HttpServletResponse response) {
		final AtomRenderer renderer = new AtomRenderer();
		final Feed feed = new Feed();

		try {
			final JSONObject preference = preferenceQueryService.getPreference();

			final String blogTitle = preference.getString(Option.ID_C_BLOG_TITLE);
			final String blogSubtitle = preference.getString(Option.ID_C_BLOG_SUBTITLE);
			final int outputCnt = preference.getInt(Option.ID_C_FEED_OUTPUT_CNT);

			feed.setTitle(StringEscapeUtils.escapeXml(blogTitle));
			feed.setSubtitle(StringEscapeUtils.escapeXml(blogSubtitle));
			feed.setUpdated(new Date());
			feed.setAuthor(StringEscapeUtils.escapeXml(blogTitle));
			feed.setLink(Latkes.getServePath() + "/blog-articles-feed.do");
			feed.setId(Latkes.getServePath() + "/");

			final List<Filter> filters = new ArrayList<>();

			filters.add(new PropertyFilter(Article.ARTICLE_IS_PUBLISHED, FilterOperator.EQUAL, true));
			filters.add(new PropertyFilter(Article.ARTICLE_VIEW_PWD, FilterOperator.EQUAL, ""));
			final Query query = new Query().setCurrentPageNum(1).setPageSize(outputCnt)
					.setFilter(new CompositeFilter(CompositeFilterOperator.AND, filters))
					.addSort(Article.ARTICLE_UPDATE_DATE, SortDirection.DESCENDING).setPageCount(1);

			final boolean hasMultipleUsers = userQueryService.hasMultipleUsers();
			String authorName = "";

			final JSONObject articleResult = articleDao.get(query);
			final JSONArray articles = articleResult.getJSONArray(Keys.RESULTS);

			if (!hasMultipleUsers && 0 != articles.length()) {
				authorName = articleQueryService.getAuthor(articles.getJSONObject(0)).getString(User.USER_NAME);
			}

			final boolean isFullContent = "fullContent".equals(preference.getString(Option.ID_C_FEED_OUTPUT_MODE));

			for (int i = 0; i < articles.length(); i++) {
				Entry entry = getEntry(hasMultipleUsers, authorName, articles, isFullContent, i);
				feed.addEntry(entry);
			}

			renderer.setContent(feed.toString());
		} catch (final Exception e) {
			logger.error("Get blog article feed error", e);

			try {
				response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
			} catch (final IOException ex) {
				throw new RuntimeException(ex);
			}
		}
		renderer.render(request, response);
	}

	private Entry getEntry(final boolean hasMultipleUsers, String authorName, final JSONArray articles,
			final boolean isFullContent, int i)
			throws org.json.JSONException, org.b3log.solo.frame.service.ServiceException {
		final JSONObject article = articles.getJSONObject(i);
		final Entry ret = new Entry();
		final String title = StringEscapeUtils.escapeXml(article.getString(Article.ARTICLE_TITLE));
		ret.setTitle(title);
		final String summary = isFullContent ? StringEscapeUtils.escapeXml(article.getString(Article.ARTICLE_CONTENT))
				: StringEscapeUtils.escapeXml(article.optString(Article.ARTICLE_ABSTRACT));
		ret.setSummary(summary);
		final Date updated = (Date) article.get(Article.ARTICLE_UPDATE_DATE);
		ret.setUpdated(updated);
		final String link = Latkes.getServePath() + article.getString(Article.ARTICLE_PERMALINK);
		ret.setLink(link);
		ret.setId(link);
		if (hasMultipleUsers) {
			authorName = StringEscapeUtils.escapeXml(articleQueryService.getAuthor(article).getString(User.USER_NAME));
		}
		ret.setAuthor(authorName);
		final String tagsString = article.getString(Article.ARTICLE_TAGS_REF);
		final String[] tagStrings = tagsString.split(",");
		for (final String tagString : tagStrings) {
			final Category catetory = new Category();
			ret.addCatetory(catetory);
			final String tag = tagString;
			catetory.setTerm(tag);
		}

		return ret;
	}

	/**
	 * Tag articles Atom output.
	 *
	 * @param context
	 *            the specified context
	 * @throws IOException
	 *             io exception
	 */
	@RequestMapping(value = { "/tag-articles-feed.do" }, method = { RequestMethod.GET, RequestMethod.HEAD })
	public void tagArticlesAtom(final HttpServletRequest request, final HttpServletResponse response)
			throws IOException {
		final AtomRenderer renderer = new AtomRenderer();
		final String queryString = request.getQueryString();

		if (Strings.isEmptyOrNull(queryString)) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		final String oIdMap = queryString.split("&")[0];
		final String tagId = oIdMap.split("=")[1];

		final Feed feed = new Feed();

		try {
			final JSONObject tag = tagDao.get(tagId);

			if (null == tag) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}

			final String tagTitle = tag.getString(Tag.TAG_TITLE);

			final JSONObject preference = preferenceQueryService.getPreference();

			if (null == preference) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}

			final String blogTitle = preference.getString(Option.ID_C_BLOG_TITLE);
			final String blogSubtitle = preference.getString(Option.ID_C_BLOG_SUBTITLE) + ", " + tagTitle;
			final int outputCnt = preference.getInt(Option.ID_C_FEED_OUTPUT_CNT);

			feed.setTitle(StringEscapeUtils.escapeXml(blogTitle));
			feed.setSubtitle(StringEscapeUtils.escapeXml(blogSubtitle));
			feed.setUpdated(new Date());
			feed.setAuthor(StringEscapeUtils.escapeXml(blogTitle));
			feed.setLink(Latkes.getServePath() + "/tag-articles-feed.do");
			feed.setId(Latkes.getServePath() + "/");

			final JSONObject tagArticleResult = tagArticleDao.getByTagId(tagId, 1, outputCnt);
			final JSONArray tagArticleRelations = tagArticleResult.getJSONArray(Keys.RESULTS);

			if (0 == tagArticleRelations.length()) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}

			final List<JSONObject> articles = new ArrayList<>();

			for (int i = 0; i < tagArticleRelations.length(); i++) {
				final JSONObject tagArticleRelation = tagArticleRelations.getJSONObject(i);
				final String articleId = tagArticleRelation.getString(Article.ARTICLE + "_" + Keys.OBJECT_ID);
				final JSONObject article = articleDao.get(articleId);

				if (article.getBoolean(Article.ARTICLE_IS_PUBLISHED) // Skips
																		// the
																		// unpublished
																		// article
						&& Strings.isEmptyOrNull(article.optString(Article.ARTICLE_VIEW_PWD))) { // Skips
																									// article
																									// with
																									// password
					articles.add(article);
				}
			}

			final boolean hasMultipleUsers = userQueryService.hasMultipleUsers();
			String authorName = "";

			if (!hasMultipleUsers && !articles.isEmpty()) {
				authorName = articleQueryService.getAuthor(articles.get(0)).getString(User.USER_NAME);
			}

			final boolean isFullContent = "fullContent".equals(preference.getString(Option.ID_C_FEED_OUTPUT_MODE));

			for (int i = 0; i < articles.size(); i++) {
				Entry entry = getEntryForArticle(articles, hasMultipleUsers, authorName, isFullContent, i);
				feed.addEntry(entry);
			}

			renderer.setContent(feed.toString());
		} catch (final Exception e) {
			logger.error("Get tag article feed error", e);

			try {
				response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
			} catch (final IOException ex) {
				throw new RuntimeException(ex);
			}
		}
		renderer.render(request, response);
	}

	private Entry getEntryForArticle(final List<JSONObject> articles, final boolean hasMultipleUsers, String authorName,
			final boolean isFullContent, int i)
			throws org.json.JSONException, org.b3log.solo.frame.service.ServiceException {
		final JSONObject article = articles.get(i);
		final Entry ret = new Entry();
		final String title = StringEscapeUtils.escapeXml(article.getString(Article.ARTICLE_TITLE));
		ret.setTitle(title);
		final String summary = isFullContent ? StringEscapeUtils.escapeXml(article.getString(Article.ARTICLE_CONTENT))
				: StringEscapeUtils.escapeXml(article.optString(Article.ARTICLE_ABSTRACT));
		ret.setSummary(summary);
		final Date updated = (Date) article.get(Article.ARTICLE_UPDATE_DATE);
		ret.setUpdated(updated);
		final String link = Latkes.getServePath() + article.getString(Article.ARTICLE_PERMALINK);
		ret.setLink(link);
		ret.setId(link);
		if (hasMultipleUsers) {
			authorName = StringEscapeUtils.escapeXml(articleQueryService.getAuthor(article).getString(User.USER_NAME));
		}
		ret.setAuthor(authorName);
		final String tagsString = article.getString(Article.ARTICLE_TAGS_REF);
		final String[] tagStrings = tagsString.split(",");
		for (final String tagString : tagStrings) {
			final Category catetory = new Category();
			ret.addCatetory(catetory);
			catetory.setTerm(tagString);
		}

		return ret;
	}

	/**
	 * Blog articles RSS output.
	 *
	 * @param context
	 *            the specified context
	 */
	@RequestMapping(value = { "/blog-articles-rss.do" }, method = { RequestMethod.GET, RequestMethod.HEAD })
	public void blogArticlesRSS(final HttpServletRequest request, final HttpServletResponse response) {
		final RssRenderer renderer = new RssRenderer();
		final Channel channel = new Channel();

		try {
			final JSONObject preference = preferenceQueryService.getPreference();

			if (null == preference) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}

			final String blogTitle = preference.getString(Option.ID_C_BLOG_TITLE);
			final String blogSubtitle = preference.getString(Option.ID_C_BLOG_SUBTITLE);
			final int outputCnt = preference.getInt(Option.ID_C_FEED_OUTPUT_CNT);

			channel.setTitle(StringEscapeUtils.escapeXml(blogTitle));
			channel.setLastBuildDate(new Date());
			channel.setLink(Latkes.getServePath());
			channel.setAtomLink(Latkes.getServePath() + "/blog-articles-rss.do");
			channel.setGenerator("Solo, ver " + SoloConstant.VERSION);
			final String localeString = preference.getString(Option.ID_C_LOCALE_STRING);
			final String country = Locales.getCountry(localeString).toLowerCase();
			final String language = Locales.getLanguage(localeString).toLowerCase();

			channel.setLanguage(language + '-' + country);
			channel.setDescription(blogSubtitle);

			final List<Filter> filters = new ArrayList<>();

			filters.add(new PropertyFilter(Article.ARTICLE_IS_PUBLISHED, FilterOperator.EQUAL, true));
			filters.add(new PropertyFilter(Article.ARTICLE_VIEW_PWD, FilterOperator.EQUAL, ""));
			final Query query = new Query().setCurrentPageNum(1).setPageSize(outputCnt)
					.setFilter(new CompositeFilter(CompositeFilterOperator.AND, filters))
					.addSort(Article.ARTICLE_UPDATE_DATE, SortDirection.DESCENDING).setPageCount(1);

			final JSONObject articleResult = articleDao.get(query);
			final JSONArray articles = articleResult.getJSONArray(Keys.RESULTS);

			final boolean hasMultipleUsers = userQueryService.hasMultipleUsers();
			String authorName = "";

			if (!hasMultipleUsers && 0 != articles.length()) {
				authorName = articleQueryService.getAuthor(articles.getJSONObject(0)).getString(User.USER_NAME);
			}

			final boolean isFullContent = "fullContent".equals(preference.getString(Option.ID_C_FEED_OUTPUT_MODE));

			for (int i = 0; i < articles.length(); i++) {
				Item item = getItem(articles, hasMultipleUsers, authorName, isFullContent, i);
				channel.addItem(item);
			}

			renderer.setContent(channel.toString());
		} catch (final Exception e) {
			logger.error("Get blog article rss error", e);

			try {
				response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
			} catch (final IOException ex) {
				throw new RuntimeException(ex);
			}
		}
		renderer.render(request, response);
	}

	private Item getItem(final JSONArray articles, final boolean hasMultipleUsers, String authorName,
			final boolean isFullContent, int i)
			throws org.json.JSONException, org.b3log.solo.frame.service.ServiceException {
		final JSONObject article = articles.getJSONObject(i);
		final Item ret = new Item();
		final String title = StringEscapeUtils.escapeXml(article.getString(Article.ARTICLE_TITLE));
		ret.setTitle(title);
		final String description = isFullContent
				? StringEscapeUtils.escapeXml(article.getString(Article.ARTICLE_CONTENT))
				: StringEscapeUtils.escapeXml(article.optString(Article.ARTICLE_ABSTRACT));
		ret.setDescription(description);
		final Date pubDate = (Date) article.get(Article.ARTICLE_UPDATE_DATE);
		ret.setPubDate(pubDate);
		final String link = Latkes.getServePath() + article.getString(Article.ARTICLE_PERMALINK);
		ret.setLink(link);
		ret.setGUID(link);
		final String authorEmail = article.getString(Article.ARTICLE_AUTHOR_EMAIL);
		if (hasMultipleUsers) {
			authorName = StringEscapeUtils.escapeXml(articleQueryService.getAuthor(article).getString(User.USER_NAME));
		}
		ret.setAuthor(authorEmail + "(" + authorName + ")");
		final String tagsString = article.getString(Article.ARTICLE_TAGS_REF);
		final String[] tagStrings = tagsString.split(",");
		for (final String tagString : tagStrings) {
			final org.b3log.solo.model.feed.rss.Category catetory = new org.b3log.solo.model.feed.rss.Category();
			ret.addCatetory(catetory);
			final String tag = tagString;
			catetory.setTerm(tag);
		}

		return ret;
	}

	/**
	 * Tag articles RSS output.
	 *
	 * @param context
	 *            the specified context
	 * @throws IOException
	 *             io exception
	 */
	@RequestMapping(value = { "/tag-articles-rss.do" }, method = { RequestMethod.GET, RequestMethod.HEAD })
	public void tagArticlesRSS(final HttpServletRequest request, final HttpServletResponse response)
			throws IOException {
		final RssRenderer renderer = new RssRenderer();
		final String queryString = request.getQueryString();

		if (Strings.isEmptyOrNull(queryString)) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		final String oIdMap = queryString.split("&")[0];
		final String tagId = oIdMap.split("=")[1];

		final Channel channel = new Channel();

		try {
			final JSONObject tag = tagDao.get(tagId);

			if (null == tag) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}

			final String tagTitle = tag.getString(Tag.TAG_TITLE);

			final JSONObject preference = preferenceQueryService.getPreference();

			if (null == preference) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}

			final String blogTitle = preference.getString(Option.ID_C_BLOG_TITLE);
			final String blogSubtitle = preference.getString(Option.ID_C_BLOG_SUBTITLE) + ", " + tagTitle;
			final int outputCnt = preference.getInt(Option.ID_C_FEED_OUTPUT_CNT);

			channel.setTitle(StringEscapeUtils.escapeXml(blogTitle));
			channel.setLastBuildDate(new Date());
			channel.setLink(Latkes.getServePath());
			channel.setAtomLink(Latkes.getServePath() + "/tag-articles-rss.do");
			channel.setGenerator("Solo, ver " + SoloConstant.VERSION);
			final String localeString = preference.getString(Option.ID_C_LOCALE_STRING);
			final String country = Locales.getCountry(localeString).toLowerCase();
			final String language = Locales.getLanguage(localeString).toLowerCase();

			channel.setLanguage(language + '-' + country);
			channel.setDescription(blogSubtitle);

			final JSONObject tagArticleResult = tagArticleDao.getByTagId(tagId, 1, outputCnt);
			final JSONArray tagArticleRelations = tagArticleResult.getJSONArray(Keys.RESULTS);

			if (0 == tagArticleRelations.length()) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}

			final List<JSONObject> articles = new ArrayList<>();

			for (int i = 0; i < tagArticleRelations.length(); i++) {
				final JSONObject tagArticleRelation = tagArticleRelations.getJSONObject(i);
				final String articleId = tagArticleRelation.getString(Article.ARTICLE + "_" + Keys.OBJECT_ID);
				final JSONObject article = articleDao.get(articleId);

				if (article.getBoolean(Article.ARTICLE_IS_PUBLISHED) // Skips
																		// the
																		// unpublished
																		// article
						&& Strings.isEmptyOrNull(article.optString(Article.ARTICLE_VIEW_PWD))) { // Skips
																									// article
																									// with
																									// password
					articles.add(article);
				}
			}

			final boolean hasMultipleUsers = userQueryService.hasMultipleUsers();
			String authorName = "";

			if (!hasMultipleUsers && !articles.isEmpty()) {
				authorName = articleQueryService.getAuthor(articles.get(0)).getString(User.USER_NAME);
			}

			final boolean isFullContent = "fullContent".equals(preference.getString(Option.ID_C_FEED_OUTPUT_MODE));

			for (int i = 0; i < articles.size(); i++) {
				Item item = getItemForArticles(articles, hasMultipleUsers, authorName, isFullContent, i);
				channel.addItem(item);
			}

			renderer.setContent(channel.toString());
		} catch (final Exception e) {
			logger.error("Get tag article rss error", e);

			try {
				response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
			} catch (final IOException ex) {
				throw new RuntimeException(ex);
			}
		}
		renderer.render(request, response);
	}

	private Item getItemForArticles(final List<JSONObject> articles, final boolean hasMultipleUsers, String authorName,
			final boolean isFullContent, int i)
			throws org.json.JSONException, org.b3log.solo.frame.service.ServiceException {
		final JSONObject article = articles.get(i);
		final Item ret = new Item();
		final String title = StringEscapeUtils.escapeXml(article.getString(Article.ARTICLE_TITLE));
		ret.setTitle(title);
		final String description = isFullContent
				? StringEscapeUtils.escapeXml(article.getString(Article.ARTICLE_CONTENT))
				: StringEscapeUtils.escapeXml(article.optString(Article.ARTICLE_ABSTRACT));
		ret.setDescription(description);
		final Date pubDate = (Date) article.get(Article.ARTICLE_UPDATE_DATE);
		ret.setPubDate(pubDate);
		final String link = Latkes.getServePath() + article.getString(Article.ARTICLE_PERMALINK);
		ret.setLink(link);
		ret.setGUID(link);
		final String authorEmail = article.getString(Article.ARTICLE_AUTHOR_EMAIL);
		if (hasMultipleUsers) {
			authorName = StringEscapeUtils.escapeXml(articleQueryService.getAuthor(article).getString(User.USER_NAME));
		}
		ret.setAuthor(authorEmail + "(" + authorName + ")");
		final String tagsString = article.getString(Article.ARTICLE_TAGS_REF);
		final String[] tagStrings = tagsString.split(",");
		for (final String tagString : tagStrings) {
			final org.b3log.solo.model.feed.rss.Category catetory = new org.b3log.solo.model.feed.rss.Category();
			ret.addCatetory(catetory);
			catetory.setTerm(tagString);
		}

		return ret;
	}
}
