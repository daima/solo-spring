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

import org.apache.commons.lang3.StringUtils;
import org.b3log.solo.Keys;
import org.b3log.solo.Latkes;
import org.b3log.solo.dao.CommentDao;
import org.b3log.solo.frame.event.Event;
import org.b3log.solo.frame.event.EventException;
import org.b3log.solo.model.Article;
import org.b3log.solo.model.Comment;
import org.b3log.solo.model.MailMessage;
import org.b3log.solo.model.Option;
import org.b3log.solo.service.MailService;
import org.b3log.solo.service.PreferenceQueryService;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This listener is responsible for processing article comment reply.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @author <a href="http://www.wanglay.com">Lei Wang</a>
 * @version 1.2.1.7, May 6, 2016
 * @since 0.3.1
 */
@Component
public final class ArticleCommentReplyNotifier {
	@Autowired
	private PreferenceQueryService preferenceQueryService;
	@Autowired
	private CommentDao commentDao;
	/**
	 * Logger.
	 */
	private static Logger logger = LoggerFactory.getLogger(ArticleCommentReplyNotifier.class);

	/**
	 * Mail service.
	 */
	@Autowired
	private MailService mailService;

	public void action(final Event<JSONObject> event) throws EventException {
		final JSONObject eventData = event.getData();
		final JSONObject comment = eventData.optJSONObject(Comment.COMMENT);
		final JSONObject article = eventData.optJSONObject(Article.ARTICLE);

		logger.debug("Processing an event[type={0}, data={1}] in listener[className={2}]", event.getType(), eventData,
				ArticleCommentReplyNotifier.class);
		final String originalCommentId = comment.optString(Comment.COMMENT_ORIGINAL_COMMENT_ID);

		if (StringUtils.isBlank(originalCommentId)) {
			logger.debug("This comment[id={0}] is not a reply", comment.optString(Keys.OBJECT_ID));

			return;
		}

		if (Latkes.getServePath().contains("localhost")) {
			logger.info("Solo runs on local server, so should not send mail");

			return;
		}

		try {
			final String commentEmail = comment.getString(Comment.COMMENT_EMAIL);
			final JSONObject originalComment = commentDao.get(originalCommentId);
			final String originalCommentEmail = originalComment.getString(Comment.COMMENT_EMAIL);

			if (originalCommentEmail.equalsIgnoreCase(commentEmail)) {
				return;
			}

			final JSONObject preference = preferenceQueryService.getPreference();

			if (null == preference) {
				throw new EventException("Not found preference");
			}

			final String blogTitle = preference.getString(Option.ID_C_BLOG_TITLE);
			final String adminEmail = preference.getString(Option.ID_C_ADMIN_EMAIL);

			final String commentContent = comment.getString(Comment.COMMENT_CONTENT);
			final String commentSharpURL = comment.getString(Comment.COMMENT_SHARP_URL);
			final MailMessage message = new MailMessage();

			message.setFrom(adminEmail);
			message.addRecipient(originalCommentEmail);
			final JSONObject replyNotificationTemplate = preferenceQueryService.getReplyNotificationTemplate();

			final String articleTitle = article.getString(Article.ARTICLE_TITLE);
			final String articleLink = Latkes.getServePath() + article.getString(Article.ARTICLE_PERMALINK);
			final String commentName = comment.getString(Comment.COMMENT_NAME);
			final String commentURL = comment.getString(Comment.COMMENT_URL);
			String commenter;

			if (!"http://".equals(commentURL)) {
				commenter = "<a target=\"_blank\" " + "href=\"" + commentURL + "\">" + commentName + "</a>";
			} else {
				commenter = commentName;
			}
			final String mailSubject = replyNotificationTemplate.getString("subject")
					.replace("${postLink}", articleLink).replace("${postTitle}", articleTitle)
					.replace("${replier}", commenter).replace("${blogTitle}", blogTitle)
					.replace("${replyURL}", Latkes.getServePath() + commentSharpURL)
					.replace("${replyContent}", commentContent);

			message.setSubject(mailSubject);
			final String mailBody = replyNotificationTemplate.getString("body").replace("${postLink}", articleLink)
					.replace("${postTitle}", articleTitle).replace("${replier}", commenter)
					.replace("${blogTitle}", blogTitle).replace("${replyURL}", Latkes.getServePath() + commentSharpURL)
					.replace("${replyContent}", commentContent);

			message.setHtmlBody(mailBody);
			logger.debug("Sending a mail[mailSubject={0}, mailBody=[{1}] to [{2}]", mailSubject, mailBody,
					originalCommentEmail);
			mailService.send(message);

		} catch (final Exception e) {
			logger.error(e.getMessage(), e);
			throw new EventException("Reply notifier error!");
		}
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
