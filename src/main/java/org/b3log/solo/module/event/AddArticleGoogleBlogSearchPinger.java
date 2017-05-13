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

import java.net.URL;
import java.net.URLEncoder;

import org.b3log.solo.Latkes;
import org.b3log.solo.frame.event.Event;
import org.b3log.solo.frame.event.EventException;
import org.b3log.solo.frame.urlfetch.HTTPRequest;
import org.b3log.solo.frame.urlfetch.URLFetchService;
import org.b3log.solo.frame.urlfetch.URLFetchServiceFactory;
import org.b3log.solo.model.Article;
import org.b3log.solo.model.Option;
import org.b3log.solo.service.PreferenceQueryService;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This listener is responsible for pinging
 * <a href="http://blogsearch.google.com"> Google Blog Search Service</a>
 * asynchronously while adding an article.
 *
 * <p>
 * <li><a href="http://www.google.com/help/blogsearch/pinging_API.html"> About
 * Google Blog Search Pinging Service API</a></li>
 * </p>
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.0.0.4, Nov 20, 2015
 * @see UpdateArticleGoogleBlogSearchPinger
 * @since 0.3.1
 */
@Component
public final class AddArticleGoogleBlogSearchPinger {
	@Autowired
	private PreferenceQueryService preferenceQueryService;
	/**
	 * Logger.
	 */
	private static Logger logger = LoggerFactory.getLogger(AddArticleGoogleBlogSearchPinger.class);

	/**
	 * URL fetch service.
	 */
	private static final URLFetchService URL_FETCH_SERVICE = URLFetchServiceFactory.getURLFetchService();

	/**
	 * Gets the event type {@linkplain EventTypes#ADD_ARTICLE}.
	 *
	 * @return event type
	 */

	public String getEventType() {
		return EventTypes.ADD_ARTICLE;
	}

	public void action(final Event<JSONObject> event) throws EventException {
		final JSONObject eventData = event.getData();

		String articleTitle = null;

		try {
			final JSONObject article = eventData.getJSONObject(Article.ARTICLE);

			articleTitle = article.getString(Article.ARTICLE_TITLE);
			final JSONObject preference = preferenceQueryService.getPreference();
			final String blogTitle = preference.getString(Option.ID_C_BLOG_TITLE);

			if (Latkes.getServePath().contains("localhost")) {
				logger.trace(
						"Solo runs on local server, so should not ping "
								+ "Google Blog Search Service for the article[title={}]",
						article.getString(Article.ARTICLE_TITLE));
				return;
			}

			final String articlePermalink = Latkes.getServePath() + article.getString(Article.ARTICLE_PERMALINK);
			final String spec = "http://blogsearch.google.com/ping?name=" + URLEncoder.encode(blogTitle, "UTF-8")
					+ "&url=" + URLEncoder.encode(Latkes.getServePath(), "UTF-8") + "&changesURL="
					+ URLEncoder.encode(articlePermalink, "UTF-8");

			logger.debug("Request Google Blog Search Service API[{}] while adding an " + "article[title="
					+ articleTitle + "]", spec);
			final HTTPRequest request = new HTTPRequest();

			request.setURL(new URL(spec));

			URL_FETCH_SERVICE.fetchAsync(request);
		} catch (final Exception e) {
			logger.error("Ping Google Blog Search Service fail while adding an article[title=" + articleTitle + "]", e);
		}
	}
}
