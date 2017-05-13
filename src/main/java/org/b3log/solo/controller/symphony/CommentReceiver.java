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
package org.b3log.solo.controller.symphony;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.b3log.solo.Keys;
import org.b3log.solo.dao.ArticleDao;
import org.b3log.solo.dao.CommentDao;
import org.b3log.solo.model.Article;
import org.b3log.solo.model.Comment;
import org.b3log.solo.model.Option;
import org.b3log.solo.module.util.Comments;
import org.b3log.solo.module.util.QueryResults;
import org.b3log.solo.renderer.JSONRenderer;
import org.b3log.solo.service.ArticleMgmtService;
import org.b3log.solo.service.CommentMgmtService;
import org.b3log.solo.service.PreferenceQueryService;
import org.b3log.solo.service.ServiceException;
import org.b3log.solo.service.StatisticMgmtService;
import org.b3log.solo.util.Requests;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Comment receiver (from B3log Symphony).
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.1.1.12, Apr 25, 2017
 * @since 0.5.5
 */
@Controller
public class CommentReceiver {

	/**
	 * Logger.
	 */
	private static Logger logger = LoggerFactory.getLogger(CommentReceiver.class);

	/**
	 * Comment management service.
	 */
	@Autowired
	private CommentMgmtService commentMgmtService;

	/**
	 * Comment repository.
	 */
	@Autowired
	private CommentDao commentDao;

	/**
	 * Preference query service.
	 */
	@Autowired
	private PreferenceQueryService preferenceQueryService;

	/**
	 * Article management service.
	 */
	@Autowired
	private ArticleMgmtService articleMgmtService;

	/**
	 * Article repository.
	 */
	@Autowired
	private ArticleDao articleDao;

	/**
	 * Statistic management service.
	 */
	@Autowired
	private StatisticMgmtService statisticMgmtService;

	/**
	 * Adds an article with the specified request.
	 *
	 * <p>
	 * Renders the response with a json object, for example,
	 * 
	 * <pre>
	 * {
	 *     "sc": true
	 * }
	 * </pre>
	 * </p>
	 *
	 * @param request
	 *            the specified http servlet request, for example,
	 * 
	 *            <pre>
	 * {
	 *     "comment": {
	 *         "userB3Key": "",
	 *         "oId": "",
	 *         "commentSymphonyArticleId": "",
	 *         "commentOnArticleId": "",
	 *         "commentAuthorName": "",
	 *         "commentAuthorEmail": "",
	 *         "commentAuthorURL": "",
	 *         "commentAuthorThumbnailURL": "",
	 *         "commentContent": "",
	 *         "commentOriginalCommentId": "" // optional, if exists this key, the comment is an reply
	 *     }
	 * }
	 *            </pre>
	 *
	 * @param response
	 *            the specified http servlet response
	 * @param context
	 *            the specified http request context
	 * @throws Exception
	 *             exception
	 */
	@RequestMapping(value = "/apis/symphony/comment", method = RequestMethod.PUT)
	public void addComment(final HttpServletRequest request, final HttpServletResponse response, @RequestParam String body) throws Exception {
		final JSONRenderer renderer = new JSONRenderer();
		final JSONObject ret = new JSONObject();
		renderer.setJSONObject(ret);

		// final Transaction transaction = commentDao.beginTransaction();

		try {
			body = URLDecoder.decode(body, "UTF-8");final JSONObject requestJSONObject = new JSONObject(body);
			final JSONObject symphonyCmt = requestJSONObject.optJSONObject(Comment.COMMENT);
			final JSONObject preference = preferenceQueryService.getPreference();
			final String keyOfSolo = preference.optString(Option.ID_C_KEY_OF_SOLO);
			final String key = symphonyCmt.optString("userB3Key");

			if (StringUtils.isBlank(keyOfSolo) || !keyOfSolo.equals(key)) {
				ret.put(Keys.STATUS_CODE, HttpServletResponse.SC_FORBIDDEN);
				ret.put(Keys.MSG, "Wrong key");
				renderer.render(request, response);
				return;
			}

			final String articleId = symphonyCmt.getString("commentOnArticleId");
			final JSONObject article = articleDao.get(articleId);

			if (null == article) {
				ret.put(Keys.STATUS_CODE, HttpServletResponse.SC_NOT_FOUND);
				ret.put(Keys.MSG, "Not found the specified article[id=" + articleId + "]");
				renderer.render(request, response);
				return;
			}

			final String commentName = symphonyCmt.getString("commentAuthorName");
			final String commentEmail = symphonyCmt.getString("commentAuthorEmail").trim().toLowerCase();
			String commentURL = symphonyCmt.optString("commentAuthorURL");
			if (!commentURL.contains("://")) {
				commentURL = "http://" + commentURL;
			}
			try {
				new URL(commentURL);
			} catch (final MalformedURLException e) {
				logger.warn("The comment URL is invalid [{}]", commentURL);
				commentURL = "";
			}
			final String commentThumbnailURL = symphonyCmt.getString("commentAuthorThumbnailURL");

			final String commentId = symphonyCmt.optString(Keys.OBJECT_ID);
			String commentContent = symphonyCmt.getString(Comment.COMMENT_CONTENT);

			// commentContent += "<p class='cmtFromSym'><i>该评论同步自 <a href='" +
			// SoloServletListener.B3LOG_SYMPHONY_SERVE_PATH
			// + "/article/" + symphonyCmt.optString("commentSymphonyArticleId")
			// + "#" + commentId
			// + "' target='_blank'>黑客派</a></i></p>";
			final String originalCommentId = symphonyCmt.optString(Comment.COMMENT_ORIGINAL_COMMENT_ID);
			// Step 1: Add comment
			final JSONObject comment = new JSONObject();
			JSONObject originalComment = null;

			comment.put(Keys.OBJECT_ID, commentId);
			comment.put(Comment.COMMENT_NAME, commentName);
			comment.put(Comment.COMMENT_EMAIL, commentEmail);
			comment.put(Comment.COMMENT_URL, commentURL);
			comment.put(Comment.COMMENT_THUMBNAIL_URL, commentThumbnailURL);
			comment.put(Comment.COMMENT_CONTENT, commentContent);
			final Date date = new Date();

			comment.put(Comment.COMMENT_DATE, date);
			ret.put(Comment.COMMENT_DATE, DateFormatUtils.format(date, "yyyy-MM-dd HH:mm:ss"));
			if (!StringUtils.isBlank(originalCommentId)) {
				originalComment = commentDao.get(originalCommentId);
				if (null != originalComment) {
					comment.put(Comment.COMMENT_ORIGINAL_COMMENT_ID, originalCommentId);
					final String originalCommentName = originalComment.getString(Comment.COMMENT_NAME);

					comment.put(Comment.COMMENT_ORIGINAL_COMMENT_NAME, originalCommentName);
					ret.put(Comment.COMMENT_ORIGINAL_COMMENT_NAME, originalCommentName);
				} else {
					comment.put(Comment.COMMENT_ORIGINAL_COMMENT_ID, "");
					comment.put(Comment.COMMENT_ORIGINAL_COMMENT_NAME, "");
					logger.warn("Not found orginal comment[id={}] of reply[name={}, content={}]", originalCommentId,
							commentName, commentContent);
				}
			} else {
				comment.put(Comment.COMMENT_ORIGINAL_COMMENT_ID, "");
				comment.put(Comment.COMMENT_ORIGINAL_COMMENT_NAME, "");
			}

			ret.put(Comment.COMMENT_THUMBNAIL_URL, comment.getString(Comment.COMMENT_THUMBNAIL_URL));
			// Sets comment on article....
			comment.put(Comment.COMMENT_ON_ID, articleId);
			comment.put(Comment.COMMENT_ON_TYPE, Article.ARTICLE);

			final String commentSharpURL = Comments.getCommentSharpURLForArticle(article, commentId);

			comment.put(Comment.COMMENT_SHARP_URL, commentSharpURL);

			commentDao.add(comment);
			// Step 2: Update article comment count
			articleMgmtService.incArticleCommentCount(articleId);
			// Step 3: Update blog statistic comment count
			statisticMgmtService.incBlogCommentCount();
			statisticMgmtService.incPublishedBlogCommentCount();
			// Step 4: Send an email to admin
			try {
				commentMgmtService.sendNotificationMail(article, comment, originalComment, preference);
			} catch (final Exception e) {
				logger.warn("Send mail failed", e);
			}
			// Step 5: Fire add comment event
			final JSONObject eventData = new JSONObject();

			eventData.put(Comment.COMMENT, comment);
			eventData.put(Article.ARTICLE, article);
			// 未找到对应的插件
			// eventManager.fireEventSynchronously(new
			// Event<JSONObject>(EventTypes.ADD_COMMENT_TO_ARTICLE_FROM_SYMPHONY,
			// eventData));

			// transaction.commit();
			ret.put(Keys.STATUS_CODE, true);
			ret.put(Keys.OBJECT_ID, commentId);

			ret.put(Keys.OBJECT_ID, articleId);
			ret.put(Keys.MSG, "add a comment to an article from symphony succ");
			ret.put(Keys.STATUS_CODE, true);

			renderer.setJSONObject(ret);
		} catch (final ServiceException e) {
			logger.error(e.getMessage(), e);

			final JSONObject jsonObject = QueryResults.defaultResult();

			renderer.setJSONObject(jsonObject);
			jsonObject.put(Keys.MSG, e.getMessage());
		}
		renderer.render(request, response);
	}
}
