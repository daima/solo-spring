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

import org.b3log.solo.Keys;
import org.b3log.solo.Latkes;
import org.b3log.solo.SoloConstant;
import org.b3log.solo.frame.event.Event;
import org.b3log.solo.frame.event.EventException;
import org.b3log.solo.frame.urlfetch.HTTPRequest;
import org.b3log.solo.frame.urlfetch.URLFetchService;
import org.b3log.solo.frame.urlfetch.URLFetchServiceFactory;
import org.b3log.solo.model.Comment;
import org.b3log.solo.model.Option;
import org.b3log.solo.service.PreferenceQueryService;
import org.b3log.solo.util.PropsUtil;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * This listener is responsible for sending comment to B3log Symphony.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.0.0.4, Nov 20, 2015
 * @since 0.5.5
 */
@Component
public final class SymphonyCommentSender {
	@Autowired
	private PreferenceQueryService preferenceQueryService;

	/**
	 * Logger.
	 */
	private static Logger logger = LoggerFactory.getLogger(SymphonyCommentSender.class);

	/**
	 * URL fetch service.
	 */
	private final URLFetchService urlFetchService = URLFetchServiceFactory.getURLFetchService();

	/**
	 * URL of adding comment to Symphony.
	 */
	private static final URL ADD_COMMENT_URL;

	static {
		try {
			ADD_COMMENT_URL = new URL(PropsUtil.getString("symphony.servePath") + "/solo/comment");
		} catch (final MalformedURLException e) {
			logger.error("Creates remote service address[symphony add comment] error!");
			throw new IllegalStateException(e);
		}
	}

	public void action(final Event<JSONObject> event) throws EventException {
		final JSONObject data = event.getData();

		logger.debug("Processing an event[type={}, data={}] in listener[className={}]", event.getType(), data,
				RhythmArticleSender.class);
		try {
			final JSONObject originalComment = data.getJSONObject(Comment.COMMENT);

			final JSONObject preference = preferenceQueryService.getPreference();

			if (null == preference) {
				throw new EventException("Not found preference");
			}

			if (Latkes.getServePath().contains("localhost")) {
				logger.trace("Solo runs on local server, so should not send this comment[id={}] to Symphony",
						originalComment.getString(Keys.OBJECT_ID));
				return;
			}

			final HTTPRequest httpRequest = new HTTPRequest();

			httpRequest.setURL(ADD_COMMENT_URL);
			httpRequest.setRequestMethod(RequestMethod.POST);
			final JSONObject requestJSONObject = new JSONObject();
			final JSONObject comment = new JSONObject();

			comment.put("commentId", originalComment.optString(Keys.OBJECT_ID));
			comment.put("commentAuthorName", originalComment.getString(Comment.COMMENT_NAME));
			comment.put("commentAuthorEmail", originalComment.getString(Comment.COMMENT_EMAIL));
			comment.put(Comment.COMMENT_CONTENT, originalComment.getString(Comment.COMMENT_CONTENT));
			comment.put("articleId", originalComment.getString(Comment.COMMENT_ON_ID));

			requestJSONObject.put(Comment.COMMENT, comment);
			requestJSONObject.put("clientVersion", SoloConstant.VERSION);
			requestJSONObject.put("clientRuntimeEnv", Latkes.getRuntimeEnv().name());
			requestJSONObject.put("clientName", "B3log Solo");
			requestJSONObject.put("clientHost", Latkes.getServerHost() + ":" + Latkes.getServerPort());
			requestJSONObject.put("clientAdminEmail", preference.optString(Option.ID_C_ADMIN_EMAIL));
			requestJSONObject.put("userB3Key", preference.optString(Option.ID_C_KEY_OF_SOLO));

			httpRequest.setPayload(requestJSONObject.toString().getBytes("UTF-8"));

			urlFetchService.fetchAsync(httpRequest);
		} catch (final Exception e) {
			logger.error("Sends a comment to Symphony error: {}", e.getMessage());
		}

		logger.debug("Sent a comment to Symphony");
	}

	/**
	 * Gets the event type {@linkplain EventTypes#ADD_COMMENT_TO_ARTICLE}.
	 * 
	 * @return event type
	 */

	public String getEventType() {
		return EventTypes.ADD_COMMENT_TO_ARTICLE;
	}
}
