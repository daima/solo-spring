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
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.b3log.solo.Keys;
import org.b3log.solo.Latkes;
import org.b3log.solo.dao.ArticleDao;
import org.b3log.solo.dao.TagArticleDao;
import org.b3log.solo.dao.TagDao;
import org.b3log.solo.dao.repository.Query;
import org.b3log.solo.dao.repository.Repositories;
import org.b3log.solo.model.Article;
import org.b3log.solo.model.MailMessage;
import org.b3log.solo.model.Option;
import org.b3log.solo.model.Statistic;
import org.b3log.solo.model.Tag;
import org.b3log.solo.renderer.TextHTMLRenderer;
import org.b3log.solo.service.MailService;
import org.b3log.solo.service.PreferenceMgmtService;
import org.b3log.solo.service.PreferenceQueryService;
import org.b3log.solo.service.StatisticMgmtService;
import org.b3log.solo.service.StatisticQueryService;
import org.b3log.solo.util.CollectionUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Provides patches on some special issues.
 *
 * <p>
 * See AuthFilter filter configurations in web.xml for authentication.
 * </p>
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.2.0.11, Nov 20, 2015
 * @since 0.3.1
 */
@Controller
public class RepairProcessor {

	/**
	 * Logger.
	 */
	private static Logger logger = LoggerFactory.getLogger(RepairProcessor.class);

	/**
	 * Bean manager.
	 */
	// @Autowired
	// private LatkeBeanManager beanManager;

	/**
	 * Preference query service.
	 */
	@Autowired
	private PreferenceQueryService preferenceQueryService;

	/**
	 * Preference management service.
	 */
	@Autowired
	private PreferenceMgmtService preferenceMgmtService;

	/**
	 * Mail service.
	 */
	@Autowired
	private MailService mailService;

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
	 * Article repository.
	 */
	@Autowired
	private ArticleDao articleDao;

	/**
	 * Statistic query service.
	 */
	@Autowired
	private StatisticQueryService statisticQueryService;

	/**
	 * Statistic management service.
	 */
	@Autowired
	private StatisticMgmtService statisticMgmtService;

	/**
	 * Removes unused properties of each article.
	 *
	 * @param context
	 *            the specified context
	 */
	@RequestMapping(value = "/fix/normalization/articles/properties", method = RequestMethod.POST)
	public void removeUnusedArticleProperties(final HttpServletRequest request, final HttpServletResponse response) {
		logger.info("Processes remove unused article properties");

		final TextHTMLRenderer renderer = new TextHTMLRenderer();
		// Transaction transaction = null;

		try {
			final JSONArray articles = articleDao.get(new Query()).getJSONArray(Keys.RESULTS);

			if (articles.length() <= 0) {
				renderer.setContent("No unused article properties");
				renderer.render(request, response);
				return;
			}

			// transaction = articleDao.beginTransaction();

			final Set<String> keyNames = Repositories.getKeyNames(Article.ARTICLE);

			for (int i = 0; i < articles.length(); i++) {
				final JSONObject article = articles.getJSONObject(i);

				final JSONArray names = article.names();
				final Set<String> nameSet = CollectionUtils.jsonArrayToSet(names);

				if (nameSet.removeAll(keyNames)) {
					for (final String unusedName : nameSet) {
						article.remove(unusedName);
					}

					articleDao.update(article.getString(Keys.OBJECT_ID), article);
					logger.info("Found an article[id={}] exists unused properties[{}]",
							article.getString(Keys.OBJECT_ID), nameSet);
				}
			}

			// transaction.commit();
		} catch (final Exception e) {
			// if (null != transaction && transaction.isActive()) {
			// transaction.rollback();
			// }

			logger.error(e.getMessage(), e);
			renderer.setContent("Removes unused article properties failed, error msg[" + e.getMessage() + "]");
		}
		renderer.render(request, response);
	}

	/**
	 * Restores the statistics.
	 *
	 * <p>
	 * <ul>
	 * <li>Uses the value of
	 * {@link Statistic#STATISTIC_PUBLISHED_BLOG_COMMENT_COUNT} for
	 * {@link Statistic#STATISTIC_BLOG_COMMENT_COUNT}</li>
	 * <li>Uses the value of {@link Statistic#STATISTIC_PUBLISHED_ARTICLE_COUNT}
	 * for {@link Statistic#STATISTIC_BLOG_ARTICLE_COUNT}</li>
	 * </ul>
	 * </p>
	 *
	 * @param context
	 *            the specified context
	 */
	@RequestMapping(value = "/fix/restore-stat.do", method = RequestMethod.GET)
	public void restoreStat(final HttpServletRequest request, final HttpServletResponse response) {
		final TextHTMLRenderer renderer = new TextHTMLRenderer();
		try {
			final JSONObject statistic = statisticQueryService.getStatistic();

			if (statistic.has(Statistic.STATISTIC_BLOG_COMMENT_COUNT)
					&& statistic.has(Statistic.STATISTIC_BLOG_ARTICLE_COUNT)) {
				logger.info("No need for repairing statistic");
				renderer.setContent("No need for repairing statistic.");
				renderer.render(request, response);
				return;
			}

			if (!statistic.has(Statistic.STATISTIC_BLOG_COMMENT_COUNT)) {
				statistic.put(Statistic.STATISTIC_BLOG_COMMENT_COUNT,
						statistic.getInt(Statistic.STATISTIC_PUBLISHED_BLOG_COMMENT_COUNT));
			}

			if (!statistic.has(Statistic.STATISTIC_BLOG_ARTICLE_COUNT)) {
				statistic.put(Statistic.STATISTIC_BLOG_ARTICLE_COUNT,
						statistic.getInt(Statistic.STATISTIC_PUBLISHED_ARTICLE_COUNT));
			}

			statisticMgmtService.updateStatistic(statistic);

			renderer.setContent("Restores statistic succeeded.");
		} catch (final Exception e) {
			logger.error(e.getMessage(), e);
			renderer.setContent("Restores statistics failed, error msg[" + e.getMessage() + "]");
		}
		renderer.render(request, response);
	}

	/**
	 * Restores the signs of preference to default.
	 *
	 * @param context
	 *            the specified context
	 */
	@RequestMapping(value = "/fix/restore-signs.do", method = RequestMethod.GET)
	public void restoreSigns(final HttpServletRequest request, final HttpServletResponse response) {
		final TextHTMLRenderer renderer = new TextHTMLRenderer();
		try {
			final JSONObject preference = preferenceQueryService.getPreference();
			final String originalSigns = preference.getString(Option.ID_C_SIGNS);

			preference.put(Option.ID_C_SIGNS, Option.DefaultPreference.DEFAULT_SIGNS);

			preferenceMgmtService.updatePreference(preference);

			// Sends the sample signs to developer
			final MailMessage msg = new MailMessage();

			msg.setFrom(preference.getString(Option.ID_C_ADMIN_EMAIL));
			msg.addRecipient("DL88250@gmail.com");
			msg.setSubject("Restore signs");
			msg.setHtmlBody(
					originalSigns + "<p>Admin email: " + preference.getString(Option.ID_C_ADMIN_EMAIL) + "</p>");

			mailService.send(msg);
			renderer.setContent("Restores signs succeeded.");
		} catch (final Exception e) {
			logger.error(e.getMessage(), e);
			renderer.setContent("Restores signs failed, error msg[" + e.getMessage() + "]");
		}
		renderer.render(request, response);
	}

	/**
	 * Repairs tag article counter.
	 *
	 * @param context
	 *            the specified context
	 */
	@RequestMapping(value = "/fix/tag-article-counter-repair.do", method = RequestMethod.GET)
	public void repairTagArticleCounter(final HttpServletRequest request, final HttpServletResponse response) {
		final TextHTMLRenderer renderer = new TextHTMLRenderer();
		try {
			final JSONObject result = tagDao.get(new Query());
			final JSONArray tagArray = result.getJSONArray(Keys.RESULTS);
			final List<JSONObject> tags = CollectionUtils.jsonArrayToList(tagArray);

			for (final JSONObject tag : tags) {
				final String tagId = tag.getString(Keys.OBJECT_ID);
				final JSONObject tagArticleResult = tagArticleDao.getByTagId(tagId, 1, Integer.MAX_VALUE);
				final JSONArray tagArticles = tagArticleResult.getJSONArray(Keys.RESULTS);
				final int tagRefCnt = tagArticles.length();
				int publishedTagRefCnt = 0;

				for (int i = 0; i < tagRefCnt; i++) {
					final JSONObject tagArticle = tagArticles.getJSONObject(i);
					final String articleId = tagArticle.getString(Article.ARTICLE + "_" + Keys.OBJECT_ID);
					final JSONObject article = articleDao.get(articleId);

					if (null == article) {
						tagArticleDao.remove(tagArticle.optString(Keys.OBJECT_ID));
						continue;
					}

					if (article.getBoolean(Article.ARTICLE_IS_PUBLISHED)) {
						publishedTagRefCnt++;
					}
				}

				tag.put(Tag.TAG_REFERENCE_COUNT, tagRefCnt);
				tag.put(Tag.TAG_PUBLISHED_REFERENCE_COUNT, publishedTagRefCnt);

				tagDao.update(tagId, tag);

				logger.info("Repaired tag[title={}, refCnt={}, publishedTagRefCnt={}]", tag.getString(Tag.TAG_TITLE),
						tagRefCnt, publishedTagRefCnt);
			}

			renderer.setContent("Repair sucessfully!");
		} catch (final Exception e) {
			logger.error(e.getMessage(), e);
			renderer.setContent("Repairs failed, error msg[" + e.getMessage() + "]");
		}
		renderer.render(request, response);
	}

	/**
	 * Shows remove all data page.
	 *
	 * @param context
	 *            the specified context
	 * @param request
	 *            the specified HTTP servlet request
	 */
	@RequestMapping(value = "/rm-all-data.do", method = RequestMethod.GET)
	public void removeAllDataGET(final HttpServletRequest request, final HttpServletResponse response) {
		final TextHTMLRenderer renderer = new TextHTMLRenderer();
		try {
			final StringBuilder htmlBuilder = new StringBuilder();

			htmlBuilder.append("<html><head><title>WARNING!</title>");
			htmlBuilder.append("<script type='text/javascript'");
			htmlBuilder.append("src='").append(Latkes.getStaticServer()).append("/js/lib/jquery/jquery.min.js'");
			htmlBuilder.append("></script></head><body>");
			htmlBuilder.append("<button id='ok' onclick='removeData()'>");
			htmlBuilder.append("Continue to delete ALL DATA</button></body>");
			htmlBuilder.append("<script type='text/javascript'>");
			htmlBuilder.append("function removeData() {");
			htmlBuilder.append("$.ajax({type: 'POST',url:'").append(Latkes.getContextPath())
					.append("/rm-all-data.do',");
			htmlBuilder.append("dataType: 'text/html',success: function(result){");
			htmlBuilder.append("$('html').html(result);}});}</script></html>");

			renderer.setContent(htmlBuilder.toString());
		} catch (final Exception e) {
			logger.error(e.getMessage(), e);

			try {
				response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
			} catch (final IOException ex) {
				throw new RuntimeException(ex);
			}
		}
		renderer.render(request, response);
	}

	/**
	 * Removes all data.
	 *
	 * @param context
	 *            the specified context
	 */
	@RequestMapping(value = "/rm-all-data.do", method = RequestMethod.POST)
	public void removeAllDataPOST(final HttpServletRequest request, final HttpServletResponse response) {
		logger.info("Removing all data....");

		boolean succeed = false;

		try {
			// remove(beanManager.getReference(ArchiveDateArticleDao.class));
			// remove(beanManager.getReference(ArchiveDateDao.class));
			// remove(beanManager.getReference(ArticleDao.class));
			// remove(beanManager.getReference(CommentDao.class));
			// remove(beanManager.getReference(LinkDao.class));
			// remove(beanManager.getReference(OptionDao.class));
			// remove(beanManager.getReference(PageDao.class));
			// remove(beanManager.getReference(PluginDao.class));
			// remove(beanManager.getReference(StatisticDao.class));
			// remove(beanManager.getReference(TagArticleDao.class));
			// remove(beanManager.getReference(TagDao.class));
			// remove(beanManager.getReference(UserDao.class));

			succeed = true;
		} catch (final Exception e) {
			logger.warn("Removed partial data only", e);
		}

		final StringBuilder htmlBuilder = new StringBuilder();

		htmlBuilder.append("<html><head><title>Result</title></head><body>");

		final TextHTMLRenderer renderer = new TextHTMLRenderer();
		try {

			if (succeed) {
				htmlBuilder.append("Removed all data!");
			} else {
				htmlBuilder.append("Refresh this page and run this remover again.");
			}
			htmlBuilder.append("</body></html>");

			renderer.setContent(htmlBuilder.toString());
		} catch (final Exception e) {
			logger.error(e.getMessage(), e);
			try {
				response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
			} catch (final IOException ex) {
				throw new RuntimeException(ex);
			}
		}
		logger.info("Removed all data....");
		renderer.render(request, response);
	}
}
