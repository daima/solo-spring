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
package org.b3log.solo.service;

import java.io.IOException;
import java.net.URL;
import java.util.Date;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.commons.validator.routines.UrlValidator;
import org.b3log.solo.Keys;
import org.b3log.solo.Latkes;
import org.b3log.solo.dao.ArticleDao;
import org.b3log.solo.dao.CommentDao;
import org.b3log.solo.dao.PageDao;
import org.b3log.solo.dao.UserDao;
import org.b3log.solo.dao.repository.RepositoryException;
import org.b3log.solo.frame.event.Event;
import org.b3log.solo.frame.urlfetch.HTTPRequest;
import org.b3log.solo.frame.urlfetch.HTTPResponse;
import org.b3log.solo.frame.urlfetch.URLFetchService;
import org.b3log.solo.frame.urlfetch.URLFetchServiceFactory;
import org.b3log.solo.model.Article;
import org.b3log.solo.model.Comment;
import org.b3log.solo.model.Common;
import org.b3log.solo.model.MailMessage;
import org.b3log.solo.model.Option;
import org.b3log.solo.model.Page;
import org.b3log.solo.model.UserExt;
import org.b3log.solo.module.event.EventTypes;
import org.b3log.solo.module.event.PageCommentReplyNotifier;
import org.b3log.solo.module.event.SymphonyCommentSender;
import org.b3log.solo.module.util.Comments;
import org.b3log.solo.module.util.Emotions;
import org.b3log.solo.module.util.Markdowns;
import org.b3log.solo.module.util.Thumbnails;
import org.b3log.solo.util.Ids;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Comment management service.
 *
 * @author <a href="http://cxy7.com">XyCai</a>
 * @version 1.3.2.10, Feb 18, 2017
 * @since 0.3.5
 */
@Service
public class CommentMgmtService {

	/**
	 * Logger.
	 */
	private static Logger logger = LoggerFactory.getLogger(CommentMgmtService.class);

	/**
	 * Article management service.
	 */
	@Autowired
	private ArticleMgmtService articleMgmtService;

	/**
	 * Comment repository.
	 */
	@Autowired
	private CommentDao commentDao;

	/**
	 * Article repository.
	 */
	@Autowired
	private ArticleDao articleDao;

	/**
	 * User repository.
	 */
	@Autowired
	private UserDao userDao;

	/**
	 * Statistic management service.
	 */
	@Autowired
	private StatisticMgmtService statisticMgmtService;

	/**
	 * Page repository.
	 */
	@Autowired
	private PageDao pageDao;

	/**
	 * Preference query service.
	 */
	@Autowired
	private PreferenceQueryService preferenceQueryService;

	/**
	 * Default user thumbnail.
	 */
	private static final String DEFAULT_USER_THUMBNAIL = "default-user-thumbnail.png";

	/**
	 * URL fetch service.
	 */
	private static URLFetchService urlFetchService = URLFetchServiceFactory.getURLFetchService();

	/**
	 * Language service.
	 */
	@Autowired
	private LangPropsService langPropsService;
	@Autowired
	private PageCommentReplyNotifier pageCommentReplyNotifier;
	@Autowired
	private SymphonyCommentSender symphonyCommentSender;

	/**
	 * Minimum length of comment name.
	 */
	private static final int MIN_COMMENT_NAME_LENGTH = 2;

	/**
	 * Maximum length of comment name.
	 */
	private static final int MAX_COMMENT_NAME_LENGTH = 20;

	/**
	 * Minimum length of comment content.
	 */
	private static final int MIN_COMMENT_CONTENT_LENGTH = 2;

	/**
	 * Maximum length of comment content.
	 */
	private static final int MAX_COMMENT_CONTENT_LENGTH = 500;

	@Autowired
	private MailService mailService;

	/**
	 * Comment mail HTML body.
	 */
	public static final String COMMENT_MAIL_HTML_BODY = "<p>{articleOrPage} [<a href=\"" + "{articleOrPageURL}\">"
			+ "{title}</a>]" + " received a new comment:</p>" + "{commenter}: <span><a href=\"{commentSharpURL}\">"
			+ "{commentContent}</a></span>";

	/**
	 * Sends a notification mail to administrator for notifying the specified
	 * article or page received the specified comment and original comment.
	 *
	 * @param articleOrPage
	 *            the specified article or page
	 * @param comment
	 *            the specified comment
	 * @param originalComment
	 *            original comment, if not exists, set it as {@code null}
	 * @param preference
	 *            the specified preference
	 * @throws IOException
	 *             io exception
	 * @throws JSONException
	 *             json exception
	 */
	public void sendNotificationMail(final JSONObject articleOrPage, final JSONObject comment,
			final JSONObject originalComment, final JSONObject preference) throws IOException, JSONException {
		final String commentEmail = comment.getString(Comment.COMMENT_EMAIL);
		final String commentId = comment.getString(Keys.OBJECT_ID);
		final String commentContent = comment.getString(Comment.COMMENT_CONTENT);

		final String adminEmail = preference.getString(Option.ID_C_ADMIN_EMAIL);

		if (adminEmail.equalsIgnoreCase(commentEmail)) {
			logger.debug("Do not send comment notification mail to admin itself[{}]", adminEmail);

			return;
		}

		if (Latkes.getServePath().contains("localhost")) {
			logger.info("Solo runs on local server, so should not send mail");

			return;
		}

		if (null != originalComment && comment.has(Comment.COMMENT_ORIGINAL_COMMENT_ID)) {
			final String originalEmail = originalComment.getString(Comment.COMMENT_EMAIL);

			if (originalEmail.equalsIgnoreCase(adminEmail)) {
				logger.debug(
						"Do not send comment notification mail to admin while the specified comment[{}] is an reply",
						commentId);
				return;
			}
		}

		final String blogTitle = preference.getString(Option.ID_C_BLOG_TITLE);
		boolean isArticle = true;
		String title = articleOrPage.optString(Article.ARTICLE_TITLE);

		if (StringUtils.isBlank(title)) {
			title = articleOrPage.getString(Page.PAGE_TITLE);
			isArticle = false;
		}

		final String commentSharpURL = comment.getString(Comment.COMMENT_SHARP_URL);
		final MailMessage message = new MailMessage();

		message.setFrom(adminEmail);
		message.addRecipient(adminEmail);
		String mailSubject;
		String articleOrPageURL;
		String mailBody;

		if (isArticle) {
			mailSubject = blogTitle + ": New comment on article [" + title + "]";
			articleOrPageURL = Latkes.getServePath() + articleOrPage.getString(Article.ARTICLE_PERMALINK);
			mailBody = COMMENT_MAIL_HTML_BODY.replace("{articleOrPage}", "Article");
		} else {
			mailSubject = blogTitle + ": New comment on page [" + title + "]";
			articleOrPageURL = Latkes.getServePath() + articleOrPage.getString(Page.PAGE_PERMALINK);
			mailBody = COMMENT_MAIL_HTML_BODY.replace("{articleOrPage}", "Page");
		}

		message.setSubject(mailSubject);
		final String commentName = comment.getString(Comment.COMMENT_NAME);
		final String commentURL = comment.getString(Comment.COMMENT_URL);
		String commenter;

		if (!"http://".equals(commentURL)) {
			commenter = "<a target=\"_blank\" " + "href=\"" + commentURL + "\">" + commentName + "</a>";
		} else {
			commenter = commentName;
		}

		mailBody = mailBody.replace("{articleOrPageURL}", articleOrPageURL).replace("{title}", title)
				.replace("{commentContent}", commentContent)
				.replace("{commentSharpURL}", Latkes.getServePath() + commentSharpURL)
				.replace("{commenter}", commenter);
		message.setHtmlBody(mailBody);

		logger.debug("Sending a mail[mailSubject={}, mailBody=[{}] to admin[email={}]", mailSubject, mailBody,
				adminEmail);

		mailService.send(message);
	}

	/**
	 * Checks the specified comment adding request.
	 *
	 * <p>
	 * XSS process (name, content) in this method.
	 * </p>
	 *
	 * @param requestJSONObject
	 *            the specified comment adding request, for example,
	 * 
	 *            <pre>
	 * {
	 *     "type": "", // "article"/"page"
	 *     "oId": "",
	 *     "commentName": "",
	 *     "commentEmail": "",
	 *     "commentURL": "",
	 *     "commentContent": "",
	 * }
	 *            </pre>
	 *
	 * @return check result, for example,
	 * 
	 *         <pre>
	 * {
	 *     "sc": boolean,
	 *     "msg": "" // Exists if "sc" equals to false
	 * }
	 *         </pre>
	 */
	public JSONObject checkAddCommentRequest(final JSONObject requestJSONObject) {
		final JSONObject ret = new JSONObject();

		try {
			ret.put(Keys.STATUS_CODE, false);
			final JSONObject preference = preferenceQueryService.getPreference();

			if (null == preference || !preference.optBoolean(Option.ID_C_COMMENTABLE)) {
				ret.put(Keys.MSG, langPropsService.get("notAllowCommentLabel"));

				return ret;
			}

			final String id = requestJSONObject.optString(Keys.OBJECT_ID);
			final String type = requestJSONObject.optString(Common.TYPE);

			if (Article.ARTICLE.equals(type)) {
				final JSONObject article = articleDao.get(id);

				if (null == article || !article.optBoolean(Article.ARTICLE_COMMENTABLE)) {
					ret.put(Keys.MSG, langPropsService.get("notAllowCommentLabel"));

					return ret;
				}
			} else {
				final JSONObject page = pageDao.get(id);

				if (null == page || !page.optBoolean(Page.PAGE_COMMENTABLE)) {
					ret.put(Keys.MSG, langPropsService.get("notAllowCommentLabel"));

					return ret;
				}
			}

			String commentName = requestJSONObject.getString(Comment.COMMENT_NAME);

			if (MAX_COMMENT_NAME_LENGTH < commentName.length() || MIN_COMMENT_NAME_LENGTH > commentName.length()) {
				logger.warn("Comment name is too long[{}]", commentName);
				ret.put(Keys.MSG, langPropsService.get("nameTooLongLabel"));

				return ret;
			}

			final String commentEmail = requestJSONObject.getString(Comment.COMMENT_EMAIL).trim().toLowerCase();

			if (!EmailValidator.getInstance().isValid(commentEmail)) {
				logger.warn("Comment email is invalid[{}]", commentEmail);
				ret.put(Keys.MSG, langPropsService.get("mailInvalidLabel"));

				return ret;
			}

			final String commentURL = requestJSONObject.optString(Comment.COMMENT_URL);

			if (!UrlValidator.getInstance().isValid(commentURL) || StringUtils.contains(commentURL, "<")) {
				logger.warn("Comment URL is invalid[{}]", commentURL);
				ret.put(Keys.MSG, langPropsService.get("urlInvalidLabel"));

				return ret;
			}

			String commentContent = requestJSONObject.optString(Comment.COMMENT_CONTENT);

			if (MAX_COMMENT_CONTENT_LENGTH < commentContent.length()
					|| MIN_COMMENT_CONTENT_LENGTH > commentContent.length()) {
				logger.warn("Comment conent length is invalid[{}]", commentContent.length());
				ret.put(Keys.MSG, langPropsService.get("commentContentCannotEmptyLabel"));

				return ret;
			}

			ret.put(Keys.STATUS_CODE, true);

			// name XSS process
			commentName = Jsoup.clean(commentName, Whitelist.none());
			requestJSONObject.put(Comment.COMMENT_NAME, commentName);

			// content Markdown & XSS process
			commentContent = Markdowns.toHTML(commentContent);
			commentContent = Jsoup.clean(commentContent, Whitelist.relaxed());

			// Emoji
			commentContent = Emotions.toAliases(commentContent);

			requestJSONObject.put(Comment.COMMENT_CONTENT, commentContent);

			return ret;
		} catch (final Exception e) {
			logger.warn("Checks add comment request[" + requestJSONObject.toString() + "] failed", e);

			ret.put(Keys.STATUS_CODE, false);
			ret.put(Keys.MSG, langPropsService.get("addFailLabel"));

			return ret;
		}
	}

	/**
	 * Adds page comment with the specified request json object.
	 *
	 * @param requestJSONObject
	 *            the specified request json object, for example,
	 * 
	 *            <pre>
	 * {
	 *     "oId": "", // page id
	 *     "commentName": "",
	 *     "commentEmail": "",
	 *     "commentURL": "", // optional
	 *     "commentContent": "",
	 *     "commentOriginalCommentId": "" // optional
	 * }
	 *            </pre>
	 *
	 * @return add result, for example,
	 * 
	 *         <pre>
	 * {
	 *     "oId": "", // generated comment id
	 *     "commentDate": "", // format: yyyy-MM-dd HH:mm:ss
	 *     "commentOriginalCommentName": "" // optional, corresponding to argument "commentOriginalCommentId"
	 *     "commentThumbnailURL": "",
	 *     "commentSharpURL": "",
	 *     "commentContent": "", // processed XSS HTML
	 *     "commentName": "", // processed XSS
	 *     "commentURL": "", // optional
	 *     "isReply": boolean,
	 *     "page": {},
	 *     "commentOriginalCommentId": "" // optional
	 *     "commentable": boolean,
	 *     "permalink": "" // page.pagePermalink
	 * }
	 *         </pre>
	 *
	 * @throws ServiceException
	 *             service exception
	 */
	public JSONObject addPageComment(final JSONObject requestJSONObject) throws ServiceException {
		final JSONObject ret = new JSONObject();
		ret.put(Common.IS_REPLY, false);

		// final Transaction transaction = commentDao.beginTransaction();

		try {
			final String pageId = requestJSONObject.getString(Keys.OBJECT_ID);
			final JSONObject page = pageDao.get(pageId);
			ret.put(Page.PAGE, page);
			final String commentName = requestJSONObject.getString(Comment.COMMENT_NAME);
			final String commentEmail = requestJSONObject.getString(Comment.COMMENT_EMAIL).trim().toLowerCase();
			final String commentURL = requestJSONObject.optString(Comment.COMMENT_URL);
			final String commentContent = requestJSONObject.getString(Comment.COMMENT_CONTENT);

			final String originalCommentId = requestJSONObject.optString(Comment.COMMENT_ORIGINAL_COMMENT_ID);
			ret.put(Comment.COMMENT_ORIGINAL_COMMENT_ID, originalCommentId);
			// Step 1: Add comment
			final JSONObject comment = new JSONObject();

			comment.put(Comment.COMMENT_ORIGINAL_COMMENT_ID, "");
			comment.put(Comment.COMMENT_ORIGINAL_COMMENT_NAME, "");

			JSONObject originalComment = null;

			comment.put(Comment.COMMENT_NAME, commentName);
			comment.put(Comment.COMMENT_EMAIL, commentEmail);
			comment.put(Comment.COMMENT_URL, commentURL);
			comment.put(Comment.COMMENT_CONTENT, commentContent);
			final JSONObject preference = preferenceQueryService.getPreference();
			final Date date = new Date();

			comment.put(Comment.COMMENT_DATE, date);
			ret.put(Comment.COMMENT_DATE, DateFormatUtils.format(date, "yyyy-MM-dd HH:mm:ss"));
			ret.put("commentDate2", date);

			ret.put(Common.COMMENTABLE,
					preference.getBoolean(Option.ID_C_COMMENTABLE) && page.getBoolean(Page.PAGE_COMMENTABLE));
			ret.put(Common.PERMALINK, page.getString(Page.PAGE_PERMALINK));

			if (!StringUtils.isBlank(originalCommentId)) {
				originalComment = commentDao.get(originalCommentId);
				if (null != originalComment) {
					comment.put(Comment.COMMENT_ORIGINAL_COMMENT_ID, originalCommentId);
					final String originalCommentName = originalComment.getString(Comment.COMMENT_NAME);

					comment.put(Comment.COMMENT_ORIGINAL_COMMENT_NAME, originalCommentName);
					ret.put(Comment.COMMENT_ORIGINAL_COMMENT_NAME, originalCommentName);

					ret.put(Common.IS_REPLY, true);
				} else {
					logger.warn("Not found orginal comment[id={}] of reply[name={}, content={}]", originalCommentId,
							commentName, commentContent);
				}
			}
			setCommentThumbnailURL(comment);
			ret.put(Comment.COMMENT_THUMBNAIL_URL, comment.getString(Comment.COMMENT_THUMBNAIL_URL));
			// Sets comment on page....
			comment.put(Comment.COMMENT_ON_ID, pageId);
			comment.put(Comment.COMMENT_ON_TYPE, Page.PAGE);
			final String commentId = Ids.genTimeMillisId();

			ret.put(Keys.OBJECT_ID, commentId);
			// Save comment sharp URL
			final String commentSharpURL = Comments.getCommentSharpURLForPage(page, commentId);

			ret.put(Comment.COMMENT_NAME, commentName);
			ret.put(Comment.COMMENT_CONTENT, commentContent);
			ret.put(Comment.COMMENT_URL, commentURL);

			ret.put(Comment.COMMENT_SHARP_URL, commentSharpURL);
			comment.put(Comment.COMMENT_SHARP_URL, commentSharpURL);
			comment.put(Keys.OBJECT_ID, commentId);
			commentDao.add(comment);
			// Step 2: Update page comment count
			incPageCommentCount(pageId);
			// Step 3: Update blog statistic comment count
			statisticMgmtService.incBlogCommentCount();
			statisticMgmtService.incPublishedBlogCommentCount();
			// Step 4: Send an email to admin
			try {
				sendNotificationMail(page, comment, originalComment, preference);
			} catch (final Exception e) {
				logger.warn("Send mail failed", e);
			}
			// Step 5: Fire add comment event
			final JSONObject eventData = new JSONObject();

			eventData.put(Comment.COMMENT, comment);
			eventData.put(Page.PAGE, page);
			pageCommentReplyNotifier.action(new Event<>(EventTypes.ADD_COMMENT_TO_PAGE, eventData));

			// transaction.commit();
		} catch (final Exception e) {
			// if (transaction.isActive()) {
			// transaction.rollback();
			// }

			throw new ServiceException(e);
		}

		return ret;
	}

	/**
	 * Adds an article comment with the specified request json object.
	 *
	 * @param requestJSONObject
	 *            the specified request json object, for example,
	 * 
	 *            <pre>
	 * {
	 *     "oId": "", // article id
	 *     "commentName": "",
	 *     "commentEmail": "",
	 *     "commentURL": "", // optional
	 *     "commentContent": "",
	 *     "commentOriginalCommentId": "" // optional
	 * }
	 *            </pre>
	 *
	 * @return add result, for example,
	 * 
	 *         <pre>
	 * {
	 *     "oId": "", // generated comment id
	 *     "commentDate": "", // format: yyyy-MM-dd HH:mm:ss
	 *     "commentOriginalCommentName": "" // optional, corresponding to argument "commentOriginalCommentId"
	 *     "commentThumbnailURL": "",
	 *     "commentSharpURL": "",
	 *     "commentContent": "", // processed XSS HTML
	 *     "commentName": "", // processed XSS
	 *     "commentURL": "", // optional
	 *     "isReply": boolean,
	 *     "article": {},
	 *     "commentOriginalCommentId": "", // optional
	 *     "commentable": boolean,
	 *     "permalink": "" // article.articlePermalink
	 * }
	 *         </pre>
	 *
	 * @throws ServiceException
	 *             service exception
	 */
	public JSONObject addArticleComment(final JSONObject requestJSONObject) throws ServiceException {
		final JSONObject ret = new JSONObject();
		ret.put(Common.IS_REPLY, false);

		// final Transaction transaction = commentDao.beginTransaction();

		try {
			final String articleId = requestJSONObject.getString(Keys.OBJECT_ID);
			final JSONObject article = articleDao.get(articleId);
			ret.put(Article.ARTICLE, article);
			final String commentName = requestJSONObject.getString(Comment.COMMENT_NAME);
			final String commentEmail = requestJSONObject.getString(Comment.COMMENT_EMAIL).trim().toLowerCase();
			final String commentURL = requestJSONObject.optString(Comment.COMMENT_URL);
			final String commentContent = requestJSONObject.getString(Comment.COMMENT_CONTENT);

			final String originalCommentId = requestJSONObject.optString(Comment.COMMENT_ORIGINAL_COMMENT_ID);
			ret.put(Comment.COMMENT_ORIGINAL_COMMENT_ID, originalCommentId);
			// Step 1: Add comment
			final JSONObject comment = new JSONObject();

			comment.put(Comment.COMMENT_ORIGINAL_COMMENT_ID, "");
			comment.put(Comment.COMMENT_ORIGINAL_COMMENT_NAME, "");

			JSONObject originalComment = null;

			comment.put(Comment.COMMENT_NAME, commentName);
			comment.put(Comment.COMMENT_EMAIL, commentEmail);
			comment.put(Comment.COMMENT_URL, commentURL);
			comment.put(Comment.COMMENT_CONTENT, commentContent);
			comment.put(Comment.COMMENT_ORIGINAL_COMMENT_ID,
					requestJSONObject.optString(Comment.COMMENT_ORIGINAL_COMMENT_ID));
			comment.put(Comment.COMMENT_ORIGINAL_COMMENT_NAME,
					requestJSONObject.optString(Comment.COMMENT_ORIGINAL_COMMENT_NAME));
			final JSONObject preference = preferenceQueryService.getPreference();
			final Date date = new Date();

			comment.put(Comment.COMMENT_DATE, date);
			ret.put(Comment.COMMENT_DATE, DateFormatUtils.format(date, "yyyy-MM-dd HH:mm:ss"));
			ret.put("commentDate2", date);

			ret.put(Common.COMMENTABLE,
					preference.getBoolean(Option.ID_C_COMMENTABLE) && article.getBoolean(Article.ARTICLE_COMMENTABLE));
			ret.put(Common.PERMALINK, article.getString(Article.ARTICLE_PERMALINK));

			ret.put(Comment.COMMENT_NAME, commentName);
			ret.put(Comment.COMMENT_CONTENT, commentContent);
			ret.put(Comment.COMMENT_URL, commentURL);

			if (!StringUtils.isBlank(originalCommentId)) {
				originalComment = commentDao.get(originalCommentId);
				if (null != originalComment) {
					comment.put(Comment.COMMENT_ORIGINAL_COMMENT_ID, originalCommentId);
					final String originalCommentName = originalComment.getString(Comment.COMMENT_NAME);

					comment.put(Comment.COMMENT_ORIGINAL_COMMENT_NAME, originalCommentName);
					ret.put(Comment.COMMENT_ORIGINAL_COMMENT_NAME, originalCommentName);

					ret.put(Common.IS_REPLY, true);
				} else {
					logger.warn("Not found orginal comment[id={}] of reply[name={}, content={}]", originalCommentId,
							commentName, commentContent);
				}
			}
			setCommentThumbnailURL(comment);
			ret.put(Comment.COMMENT_THUMBNAIL_URL, comment.getString(Comment.COMMENT_THUMBNAIL_URL));
			// Sets comment on article....
			comment.put(Comment.COMMENT_ON_ID, articleId);
			comment.put(Comment.COMMENT_ON_TYPE, Article.ARTICLE);
			final String commentId = Ids.genTimeMillisId();

			comment.put(Keys.OBJECT_ID, commentId);
			ret.put(Keys.OBJECT_ID, commentId);
			final String commentSharpURL = Comments.getCommentSharpURLForArticle(article, commentId);

			comment.put(Comment.COMMENT_SHARP_URL, commentSharpURL);
			ret.put(Comment.COMMENT_SHARP_URL, commentSharpURL);

			commentDao.add(comment);
			// Step 2: Update article comment count
			articleMgmtService.incArticleCommentCount(articleId);
			// Step 3: Update blog statistic comment count
			statisticMgmtService.incBlogCommentCount();
			statisticMgmtService.incPublishedBlogCommentCount();
			// Step 4: Send an email to admin
			try {
				sendNotificationMail(article, comment, originalComment, preference);
			} catch (final Exception e) {
				logger.warn("Send mail failed", e);
			}
			// Step 5: Fire add comment event
			final JSONObject eventData = new JSONObject();

			eventData.put(Comment.COMMENT, comment);
			eventData.put(Article.ARTICLE, article);
			symphonyCommentSender.action(new Event<>(EventTypes.ADD_COMMENT_TO_ARTICLE, eventData));
			// transaction.commit();
		} catch (final Exception e) {
			// if (transaction.isActive()) {
			// transaction.rollback();
			// }

			throw new ServiceException(e);
		}

		return ret;
	}

	/**
	 * Removes a comment of a page with the specified comment id.
	 *
	 * @param commentId
	 *            the given comment id
	 * @throws ServiceException
	 *             service exception
	 */
	public void removePageComment(final String commentId) throws ServiceException {
		// final Transaction transaction = commentDao.beginTransaction();

		try {
			final JSONObject comment = commentDao.get(commentId);
			final String pageId = comment.getString(Comment.COMMENT_ON_ID);

			// Step 1: Remove comment
			commentDao.remove(commentId);
			// Step 2: Update page comment count
			decPageCommentCount(pageId);
			// Step 3: Update blog statistic comment count
			statisticMgmtService.decBlogCommentCount();
			statisticMgmtService.decPublishedBlogCommentCount();

			// transaction.commit();
		} catch (final Exception e) {
			// if (transaction.isActive()) {
			// transaction.rollback();
			// }

			logger.error("Removes a comment of a page failed", e);
			throw new ServiceException(e);
		}
	}

	/**
	 * Removes a comment of an article with the specified comment id.
	 *
	 * @param commentId
	 *            the given comment id
	 * @throws ServiceException
	 *             service exception
	 */
	public void removeArticleComment(final String commentId) throws ServiceException {
		// final Transaction transaction = commentDao.beginTransaction();

		try {
			final JSONObject comment = commentDao.get(commentId);
			final String articleId = comment.getString(Comment.COMMENT_ON_ID);

			// Step 1: Remove comment
			commentDao.remove(commentId);
			// Step 2: Update article comment count
			decArticleCommentCount(articleId);
			// Step 3: Update blog statistic comment count
			statisticMgmtService.decBlogCommentCount();
			statisticMgmtService.decPublishedBlogCommentCount();

			// transaction.commit();
		} catch (final Exception e) {
			// if (transaction.isActive()) {
			// transaction.rollback();
			// }

			logger.error("Removes a comment of an article failed", e);
			throw new ServiceException(e);
		}
	}

	/**
	 * Page comment count +1 for an page specified by the given page id.
	 *
	 * @param pageId
	 *            the given page id
	 * @throws JSONException
	 *             json exception
	 * @throws RepositoryException
	 *             repository exception
	 */
	public void incPageCommentCount(final String pageId) throws JSONException, RepositoryException {
		final JSONObject page = pageDao.get(pageId);
		final JSONObject newPage = new JSONObject(page, JSONObject.getNames(page));
		final int commentCnt = page.getInt(Page.PAGE_COMMENT_COUNT);

		newPage.put(Page.PAGE_COMMENT_COUNT, commentCnt + 1);
		pageDao.update(pageId, newPage);
	}

	/**
	 * Article comment count -1 for an article specified by the given article
	 * id.
	 *
	 * @param articleId
	 *            the given article id
	 * @throws JSONException
	 *             json exception
	 * @throws RepositoryException
	 *             repository exception
	 */
	private void decArticleCommentCount(final String articleId) throws JSONException, RepositoryException {
		final JSONObject article = articleDao.get(articleId);
		final JSONObject newArticle = new JSONObject(article, JSONObject.getNames(article));
		final int commentCnt = article.getInt(Article.ARTICLE_COMMENT_COUNT);

		newArticle.put(Article.ARTICLE_COMMENT_COUNT, commentCnt - 1);

		articleDao.update(articleId, newArticle);
	}

	/**
	 * Page comment count -1 for an page specified by the given page id.
	 *
	 * @param pageId
	 *            the given page id
	 * @throws JSONException
	 *             json exception
	 * @throws RepositoryException
	 *             repository exception
	 */
	private void decPageCommentCount(final String pageId) throws JSONException, RepositoryException {
		final JSONObject page = pageDao.get(pageId);
		final JSONObject newPage = new JSONObject(page, JSONObject.getNames(page));
		final int commentCnt = page.getInt(Page.PAGE_COMMENT_COUNT);

		newPage.put(Page.PAGE_COMMENT_COUNT, commentCnt - 1);
		pageDao.update(pageId, newPage);
	}

	/**
	 * Sets commenter thumbnail URL for the specified comment.
	 *
	 * <p>
	 * Try to set thumbnail URL using:
	 * <ol>
	 * <li>User avatar</li>
	 * <li>Gravatar service</li>
	 * <ol>
	 * </p>
	 *
	 * @param comment
	 *            the specified comment
	 * @throws Exception
	 *             exception
	 */
	public void setCommentThumbnailURL(final JSONObject comment) throws Exception {
		final String commentEmail = comment.getString(Comment.COMMENT_EMAIL);

		// 1. user avatar
		final JSONObject user = userDao.getByEmail(commentEmail);
		if (null != user) {
			final String avatar = user.optString(UserExt.USER_AVATAR);
			if (!StringUtils.isBlank(avatar)) {
				comment.put(Comment.COMMENT_THUMBNAIL_URL, avatar);

				return;
			}
		}

		// 2. Gravatar
		String thumbnailURL = Thumbnails.getGravatarURL(commentEmail.toLowerCase(), "128");
		final URL gravatarURL = new URL(thumbnailURL);

		int statusCode = HttpServletResponse.SC_OK;

		try {
			final HTTPRequest request = new HTTPRequest();

			request.setURL(gravatarURL);
			final HTTPResponse response = urlFetchService.fetch(request);

			statusCode = response.getResponseCode();
		} catch (final IOException e) {
			logger.warn("Can not fetch thumbnail from Gravatar[commentEmail={}]", commentEmail);
		} finally {
			if (HttpServletResponse.SC_OK != statusCode) {
				thumbnailURL = Latkes.getStaticServePath() + "/images/" + DEFAULT_USER_THUMBNAIL;
			}
		}

		comment.put(Comment.COMMENT_THUMBNAIL_URL, thumbnailURL);
	}

	/**
	 * Sets the article repository with the specified article repository.
	 *
	 * @param articleDao
	 *            the specified article repository
	 */
	public void setArticleRepository(final ArticleDao articleDao) {
		this.articleDao = articleDao;
	}

	/**
	 * Sets the article management service with the specified article management
	 * service.
	 *
	 * @param articleMgmtService
	 *            the specified article management service
	 */
	public void setArticleMgmtService(final ArticleMgmtService articleMgmtService) {
		this.articleMgmtService = articleMgmtService;
	}

	/**
	 * Set the page repository with the specified page repository.
	 *
	 * @param pageDao
	 *            the specified page repository
	 */
	public void setPageRepository(final PageDao pageDao) {
		this.pageDao = pageDao;
	}

	/**
	 * Sets the preference query service with the specified preference query
	 * service.
	 *
	 * @param preferenceQueryService
	 *            the specified preference query service
	 */
	public void setPreferenceQueryService(final PreferenceQueryService preferenceQueryService) {
		this.preferenceQueryService = preferenceQueryService;
	}

	/**
	 * Sets the statistic management service with the specified statistic
	 * management service.
	 *
	 * @param statisticMgmtService
	 *            the specified statistic management service
	 */
	public void setStatisticMgmtService(final StatisticMgmtService statisticMgmtService) {
		this.statisticMgmtService = statisticMgmtService;
	}

	/**
	 * Sets the comment repository with the specified comment repository.
	 *
	 * @param commentDao
	 *            the specified comment repository
	 */
	public void setCommentRepository(final CommentDao commentDao) {
		this.commentDao = commentDao;
	}

	/**
	 * Sets the language service with the specified language service.
	 *
	 * @param langPropsService
	 *            the specified language service
	 */
	public void setLangPropsService(final LangPropsService langPropsService) {
		this.langPropsService = langPropsService;
	}
}
