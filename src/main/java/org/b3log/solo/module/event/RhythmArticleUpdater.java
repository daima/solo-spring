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
package org.b3log.solo.module.event;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

import org.b3log.solo.Keys;
import org.b3log.solo.Latkes;
import org.b3log.solo.SoloConstant;
import org.b3log.solo.frame.event.Event;
import org.b3log.solo.frame.event.EventException;
import org.b3log.solo.frame.urlfetch.HTTPRequest;
import org.b3log.solo.frame.urlfetch.URLFetchService;
import org.b3log.solo.frame.urlfetch.URLFetchServiceFactory;
import org.b3log.solo.model.Article;
import org.b3log.solo.model.Common;
import org.b3log.solo.model.Option;
import org.b3log.solo.service.PreferenceQueryService;
import org.b3log.solo.util.PropsUtil;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * This listener is responsible for updating article to B3log Rhythm.
 *
 * <p>
 * The B3log Rhythm article update interface: http://rhythm.b3log.org/article
 * (PUT).
 * </p>
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.0.0.2, Nov 20, 2015
 * @since 0.6.0
 */
@Component
public final class RhythmArticleUpdater {
	@Autowired
	private PreferenceQueryService preferenceQueryService;
	/**
	 * Logger.
	 */
	private static Logger logger = LoggerFactory.getLogger(RhythmArticleUpdater.class);

	/**
	 * URL fetch service.
	 */
	private final URLFetchService urlFetchService = URLFetchServiceFactory.getURLFetchService();

	/**
	 * URL of updating article to Rhythm.
	 */
	private static final URL UPDATE_ARTICLE_URL;

	static {
		try {
			UPDATE_ARTICLE_URL = new URL(PropsUtil.getProperty("rhythm.servePath") + "/article");
		} catch (final MalformedURLException e) {
			logger.error("Creates remote service address[rhythm update article] error!");
			throw new IllegalStateException(e);
		}
	}

	public void action(final Event<JSONObject> event) throws EventException {
		final JSONObject data = event.getData();

		logger.debug("Processing an event[type={}, data={}] in listener[className={}]", event.getType(), data,
				RhythmArticleUpdater.class);
		try {
			final JSONObject originalArticle = data.getJSONObject(Article.ARTICLE);

			if (!originalArticle.getBoolean(Article.ARTICLE_IS_PUBLISHED)) {
				logger.debug("Ignores post article[title={}] to Rhythm",
						originalArticle.getString(Article.ARTICLE_TITLE));

				return;
			}

			final JSONObject preference = preferenceQueryService.getPreference();

			if (null == preference) {
				throw new EventException("Not found preference");
			}

			if (!StringUtils.isBlank(originalArticle.optString(Article.ARTICLE_VIEW_PWD))) {
				return;
			}

			if (Latkes.getServePath().contains("localhost")) {
				logger.info("Solo runs on local server, so should not send this article[id={}, title={}] to Rhythm",
						originalArticle.getString(Keys.OBJECT_ID), originalArticle.getString(Article.ARTICLE_TITLE));
				return;
			}

			final HTTPRequest httpRequest = new HTTPRequest();

			httpRequest.setURL(UPDATE_ARTICLE_URL);
			httpRequest.setRequestMethod(RequestMethod.PUT);
			final JSONObject requestJSONObject = new JSONObject();
			final JSONObject article = new JSONObject();

			article.put(Keys.OBJECT_ID, originalArticle.getString(Keys.OBJECT_ID));
			article.put(Article.ARTICLE_TITLE, originalArticle.getString(Article.ARTICLE_TITLE));
			article.put(Article.ARTICLE_PERMALINK, originalArticle.getString(Article.ARTICLE_PERMALINK));
			article.put(Article.ARTICLE_TAGS_REF, originalArticle.getString(Article.ARTICLE_TAGS_REF));
			article.put(Article.ARTICLE_AUTHOR_EMAIL, originalArticle.getString(Article.ARTICLE_AUTHOR_EMAIL));
			article.put(Article.ARTICLE_CONTENT, originalArticle.getString(Article.ARTICLE_CONTENT));
			article.put(Article.ARTICLE_CREATE_DATE,
					((Date) originalArticle.get(Article.ARTICLE_CREATE_DATE)).getTime());
			article.put(Common.POST_TO_COMMUNITY, originalArticle.getBoolean(Common.POST_TO_COMMUNITY));

			// Removes this property avoid to persist
			originalArticle.remove(Common.POST_TO_COMMUNITY);

			requestJSONObject.put(Article.ARTICLE, article);
			requestJSONObject.put(Common.BLOG_VERSION, SoloConstant.VERSION);
			requestJSONObject.put(Common.BLOG, "B3log Solo");
			requestJSONObject.put(Option.ID_C_BLOG_TITLE, preference.getString(Option.ID_C_BLOG_TITLE));
			requestJSONObject.put("blogHost", Latkes.getServerHost() + ":" + Latkes.getServerPort());
			requestJSONObject.put("userB3Key", preference.optString(Option.ID_C_KEY_OF_SOLO));
			requestJSONObject.put("clientAdminEmail", preference.optString(Option.ID_C_ADMIN_EMAIL));
			requestJSONObject.put("clientRuntimeEnv", Latkes.getRuntimeEnv().name());

			httpRequest.setPayload(requestJSONObject.toString().getBytes("UTF-8"));

			urlFetchService.fetchAsync(httpRequest);
		} catch (final Exception e) {
			logger.error("Sends an article to Rhythm error: {}", e.getMessage());
		}

		logger.debug("Sent an article to Rhythm");
	}

	/**
	 * Gets the event type {@linkplain EventTypes#UPDATE_ARTICLE}.
	 *
	 * @return event type
	 */

	public String getEventType() {
		return EventTypes.UPDATE_ARTICLE;
	}
}
