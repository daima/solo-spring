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
import java.net.URLEncoder;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.b3log.solo.Keys;
import org.b3log.solo.Latkes;
import org.b3log.solo.dao.ArchiveDateDao;
import org.b3log.solo.dao.ArticleDao;
import org.b3log.solo.dao.PageDao;
import org.b3log.solo.dao.TagDao;
import org.b3log.solo.dao.repository.FilterOperator;
import org.b3log.solo.dao.repository.PropertyFilter;
import org.b3log.solo.dao.repository.Query;
import org.b3log.solo.dao.repository.SortDirection;
import org.b3log.solo.model.ArchiveDate;
import org.b3log.solo.model.Article;
import org.b3log.solo.model.Page;
import org.b3log.solo.model.Tag;
import org.b3log.solo.model.sitemap.Sitemap;
import org.b3log.solo.model.sitemap.URL;
import org.b3log.solo.renderer.TextXMLRenderer;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Site map (sitemap) processor.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.0.0.6, May 17, 2013
 * @since 0.3.1
 */
@Controller
public class SitemapProcessor {

	/**
	 * Logger.
	 */
	private static Logger logger = LoggerFactory.getLogger(SitemapProcessor.class);

	/**
	 * Article repository.
	 */
	@Autowired
	private ArticleDao articleDao;

	/**
	 * Page repository.
	 */
	@Autowired
	private PageDao pageDao;

	/**
	 * Tag repository.
	 */
	@Autowired
	private TagDao tagDao;

	/**
	 * Archive date repository.
	 */
	@Autowired
	private ArchiveDateDao archiveDateDao;

	/**
	 * Returns the sitemap.
	 * 
	 * @param context
	 *            the specified context
	 */
	@RequestMapping(value = "/sitemap.xml", method = RequestMethod.GET)
	public void sitemap(final HttpServletRequest request, final HttpServletResponse response) {
		final TextXMLRenderer renderer = new TextXMLRenderer();
		final Sitemap sitemap = new Sitemap();

		try {
			addArticles(sitemap);
			addNavigations(sitemap);
			addTags(sitemap);
			addArchives(sitemap);

			logger.info("Generating sitemap....");
			final String content = sitemap.toString();

			logger.info("Generated sitemap");
			renderer.setContent(content);
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

	/**
	 * Adds articles into the specified sitemap.
	 * 
	 * @param sitemap
	 *            the specified sitemap
	 * @throws Exception
	 *             exception
	 */
	private void addArticles(final Sitemap sitemap) throws Exception {
		// XXX: query all articles?
		final Query query = new Query().setCurrentPageNum(1)
				.setFilter(new PropertyFilter(Article.ARTICLE_IS_PUBLISHED, FilterOperator.EQUAL, true))
				.addSort(Article.ARTICLE_CREATE_DATE, SortDirection.DESCENDING);

		// XXX: maybe out of memory
		final JSONObject articleResult = articleDao.get(query);

		final JSONArray articles = articleResult.getJSONArray(Keys.RESULTS);

		for (int i = 0; i < articles.length(); i++) {
			final JSONObject article = articles.getJSONObject(i);
			final String permalink = article.getString(Article.ARTICLE_PERMALINK);

			final URL url = new URL();

			url.setLoc(Latkes.getServePath() + permalink);

			final Date updateDate = (Date) article.get(Article.ARTICLE_UPDATE_DATE);
			final String lastMod = DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.format(updateDate);

			url.setLastMod(lastMod);

			sitemap.addURL(url);
		}
	}

	/**
	 * Adds navigations into the specified sitemap.
	 * 
	 * @param sitemap
	 *            the specified sitemap
	 * @throws Exception
	 *             exception
	 */
	private void addNavigations(final Sitemap sitemap) throws Exception {
		final JSONObject result = pageDao.get(new Query());
		final JSONArray pages = result.getJSONArray(Keys.RESULTS);

		for (int i = 0; i < pages.length(); i++) {
			final JSONObject page = pages.getJSONObject(i);
			final String permalink = page.getString(Page.PAGE_PERMALINK);

			final URL url = new URL();

			// The navigation maybe a page or a link
			// Just filters for user mistakes tolerance
			if (!permalink.contains("://")) {
				url.setLoc(Latkes.getServePath() + permalink);
			} else {
				url.setLoc(permalink);
			}

			sitemap.addURL(url);
		}
	}

	/**
	 * Adds tags (tag-articles) and tags wall (/tags.html) into the specified
	 * sitemap.
	 * 
	 * @param sitemap
	 *            the specified sitemap
	 * @throws Exception
	 *             exception
	 */
	private void addTags(final Sitemap sitemap) throws Exception {
		final JSONObject result = tagDao.get(new Query());
		final JSONArray tags = result.getJSONArray(Keys.RESULTS);

		for (int i = 0; i < tags.length(); i++) {
			final JSONObject tag = tags.getJSONObject(i);
			final String link = URLEncoder.encode(tag.getString(Tag.TAG_TITLE), "UTF-8");

			final URL url = new URL();

			url.setLoc(Latkes.getServePath() + "/tags/" + link);

			sitemap.addURL(url);
		}

		// Tags wall
		final URL url = new URL();

		url.setLoc(Latkes.getServePath() + "/tags.html");
		sitemap.addURL(url);
	}

	/**
	 * Adds archives (archive-articles) into the specified sitemap.
	 * 
	 * @param sitemap
	 *            the specified sitemap
	 * @throws Exception
	 *             exception
	 */
	private void addArchives(final Sitemap sitemap) throws Exception {
		final JSONObject result = archiveDateDao.get(new Query());
		final JSONArray archiveDates = result.getJSONArray(Keys.RESULTS);

		for (int i = 0; i < archiveDates.length(); i++) {
			final JSONObject archiveDate = archiveDates.getJSONObject(i);
			final long time = archiveDate.getLong(ArchiveDate.ARCHIVE_TIME);
			final String dateString = DateFormatUtils.format(time, "yyyy/MM");

			final URL url = new URL();

			url.setLoc(Latkes.getServePath() + "/archives/" + dateString);

			sitemap.addURL(url);
		}
	}
}
