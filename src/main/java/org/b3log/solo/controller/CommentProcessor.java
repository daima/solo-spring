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
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.b3log.solo.Keys;
import org.b3log.solo.frame.model.User;
import org.b3log.solo.frame.servlet.renderer.JSONRenderer;
import org.b3log.solo.model.Article;
import org.b3log.solo.model.Comment;
import org.b3log.solo.model.Common;
import org.b3log.solo.model.Option;
import org.b3log.solo.model.Page;
import org.b3log.solo.module.util.Skins;
import org.b3log.solo.service.CommentMgmtService;
import org.b3log.solo.service.LangPropsService;
import org.b3log.solo.service.PreferenceQueryService;
import org.b3log.solo.service.UserMgmtService;
import org.b3log.solo.service.UserQueryService;
import org.b3log.solo.util.Requests;
import org.b3log.solo.util.freemarker.Templates;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import freemarker.template.Template;

/**
 * Comment processor.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @author ArmstrongCN
 * @version 1.3.2.13, Feb 18, 2017
 * @since 0.3.1
 */
@Controller
public class CommentProcessor {

	/**
	 * Logger.
	 */
	private static Logger logger = LoggerFactory.getLogger(CommentProcessor.class);

	@Autowired
	private Skins skins;
	/**
	 * Language service.
	 */
	@Autowired
	private LangPropsService langPropsService;

	/**
	 * Comment management service.
	 */
	@Autowired
	private CommentMgmtService commentMgmtService;

	/**
	 * User query service.
	 */
	@Autowired
	private UserQueryService userQueryService;

	/**
	 * User management service.
	 */
	@Autowired
	private UserMgmtService userMgmtService;

	/**
	 * Preference query service.
	 */
	@Autowired
	private PreferenceQueryService preferenceQueryService;

	/**
	 * Adds a comment to a page.
	 * <p>
	 * Renders the response with a json object, for example,
	 * 
	 * <pre>
	 * {
	 *     "oId": generatedCommentId,
	 *     "sc": "COMMENT_PAGE_SUCC"
	 *     "commentDate": "", // yyyy/MM/dd HH:mm:ss
	 *     "commentSharpURL": "",
	 *     "commentThumbnailURL": "",
	 *     "commentOriginalCommentName": "" // if exists this key, the comment is an reply
	 * }
	 * </pre>
	 * </p>
	 *
	 * @param context
	 *            the specified context, including a request json object, for
	 *            example, "captcha": "", "oId": pageId, "commentName": "",
	 *            "commentEmail": "", "commentURL": "", "commentContent": "", //
	 *            HTML "commentOriginalCommentId": "" // optional, if exists
	 *            this key, the comment is an reply
	 * @throws ServletException
	 *             servlet exception
	 * @throws IOException
	 *             io exception
	 */
	@RequestMapping(value = "/add-page-comment.do", method = RequestMethod.POST)
	public void addPageComment(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		final JSONObject requestJSONObject = Requests.parseRequestJSONObject(request, response);
		requestJSONObject.put(Common.TYPE, Page.PAGE);

		fillCommenter(requestJSONObject, request, response);

		final JSONObject jsonObject = commentMgmtService.checkAddCommentRequest(requestJSONObject);
		final JSONRenderer renderer = new JSONRenderer();

		renderer.setJSONObject(jsonObject);

		if (!jsonObject.optBoolean(Keys.STATUS_CODE)) {
			logger.warn("Can't add comment[msg={0}]", jsonObject.optString(Keys.MSG));
			renderer.render(request, response);
			return;
		}

		final HttpSession session = request.getSession(false);

		if (null == session) {
			jsonObject.put(Keys.STATUS_CODE, false);
			jsonObject.put(Keys.MSG, langPropsService.get("captchaErrorLabel"));
			renderer.render(request, response);
			return;
		}

		final String storedCaptcha = (String) session.getAttribute(CaptchaProcessor.CAPTCHA);
		session.removeAttribute(CaptchaProcessor.CAPTCHA);

		if (!userQueryService.isLoggedIn(request, response)) {
			final String captcha = requestJSONObject.optString(CaptchaProcessor.CAPTCHA);

			if (null == storedCaptcha || !storedCaptcha.equals(captcha)) {
				jsonObject.put(Keys.STATUS_CODE, false);
				jsonObject.put(Keys.MSG, langPropsService.get("captchaErrorLabel"));
				renderer.render(request, response);
				return;
			}
		}

		try {
			final JSONObject addResult = commentMgmtService.addPageComment(requestJSONObject);

			final Map<String, Object> dataModel = new HashMap<>();
			dataModel.put(Comment.COMMENT, addResult);

			final JSONObject page = addResult.optJSONObject(Page.PAGE);
			page.put(Common.COMMENTABLE, addResult.opt(Common.COMMENTABLE));
			page.put(Common.PERMALINK, addResult.opt(Common.PERMALINK));
			dataModel.put(Article.ARTICLE, page);

			// https://github.com/b3log/solo/issues/12246
			try {
				final String skinDirName = (String) request.getAttribute(Keys.TEMAPLTE_DIR_NAME);
				final Template template = Templates.MAIN_CFG.getTemplate("common-comment.ftl");
				final JSONObject preference = preferenceQueryService.getPreference();
				skins.fillLangs(preference.optString(Option.ID_C_LOCALE_STRING), skinDirName, dataModel);
				Keys.fillServer(dataModel);
				final StringWriter stringWriter = new StringWriter();
				template.process(dataModel, stringWriter);
				stringWriter.close();
				addResult.put("cmtTpl", stringWriter.toString());
			} catch (final Exception e) {
				// 1.9.0 向后兼容
			}

			addResult.put(Keys.STATUS_CODE, true);

			renderer.setJSONObject(addResult);
		} catch (final Exception e) {
			logger.error("Can not add comment on page", e);

			jsonObject.put(Keys.STATUS_CODE, false);
			jsonObject.put(Keys.MSG, langPropsService.get("addFailLabel"));
		}
		renderer.render(request, response);
	}

	/**
	 * Adds a comment to an article.
	 * <p>
	 * Renders the response with a json object, for example,
	 * 
	 * <pre>
	 * {
	 *     "oId": generatedCommentId,
	 *     "sc": "COMMENT_ARTICLE_SUCC",
	 *     "commentDate": "", // yyyy/MM/dd HH:mm:ss
	 *     "commentSharpURL": "",
	 *     "commentThumbnailURL": "",
	 *     "commentOriginalCommentName": "", // if exists this key, the comment is an reply
	 *     "commentContent": "" // HTML
	 * }
	 * </pre>
	 * </p>
	 *
	 * @param context
	 *            the specified context, including a request json object, for
	 *            example, "captcha": "", "oId": articleId, "commentName": "",
	 *            "commentEmail": "", "commentURL": "", "commentContent": "",
	 *            "commentOriginalCommentId": "" // optional, if exists this
	 *            key, the comment is an reply
	 * @throws ServletException
	 *             servlet exception
	 * @throws IOException
	 *             io exception
	 */
	@RequestMapping(value = "/add-article-comment.do", method = RequestMethod.POST)
	public void addArticleComment(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		final JSONObject requestJSONObject = Requests.parseRequestJSONObject(request, response);
		requestJSONObject.put(Common.TYPE, Article.ARTICLE);

		fillCommenter(requestJSONObject, request, response);

		final JSONObject jsonObject = commentMgmtService.checkAddCommentRequest(requestJSONObject);
		final JSONRenderer renderer = new JSONRenderer();

		renderer.setJSONObject(jsonObject);

		if (!jsonObject.optBoolean(Keys.STATUS_CODE)) {
			logger.warn("Can't add comment[msg={0}]", jsonObject.optString(Keys.MSG));
			renderer.render(request, response);
			return;
		}

		final HttpSession session = request.getSession(false);
		if (null == session) {
			jsonObject.put(Keys.STATUS_CODE, false);
			jsonObject.put(Keys.MSG, langPropsService.get("captchaErrorLabel"));
			renderer.render(request, response);
			return;
		}

		final String storedCaptcha = (String) session.getAttribute(CaptchaProcessor.CAPTCHA);
		session.removeAttribute(CaptchaProcessor.CAPTCHA);

		if (!userQueryService.isLoggedIn(request, response)) {
			final String captcha = requestJSONObject.optString(CaptchaProcessor.CAPTCHA);
			if (null == storedCaptcha || !storedCaptcha.equals(captcha)) {
				jsonObject.put(Keys.STATUS_CODE, false);
				jsonObject.put(Keys.MSG, langPropsService.get("captchaErrorLabel"));
				renderer.render(request, response);
				return;
			}
		}

		try {
			final JSONObject addResult = commentMgmtService.addArticleComment(requestJSONObject);

			final Map<String, Object> dataModel = new HashMap<>();
			dataModel.put(Comment.COMMENT, addResult);
			final JSONObject article = addResult.optJSONObject(Article.ARTICLE);
			article.put(Common.COMMENTABLE, addResult.opt(Common.COMMENTABLE));
			article.put(Common.PERMALINK, addResult.opt(Common.PERMALINK));
			dataModel.put(Article.ARTICLE, article);

			// https://github.com/b3log/solo/issues/12246
			try {
				final String skinDirName = (String) request.getAttribute(Keys.TEMAPLTE_DIR_NAME);
				final Template template = Templates.MAIN_CFG.getTemplate("common-comment.ftl");
				final JSONObject preference = preferenceQueryService.getPreference();
				skins.fillLangs(preference.optString(Option.ID_C_LOCALE_STRING), skinDirName, dataModel);
				Keys.fillServer(dataModel);
				final StringWriter stringWriter = new StringWriter();
				template.process(dataModel, stringWriter);
				stringWriter.close();
				addResult.put("cmtTpl", stringWriter.toString());
			} catch (final Exception e) {
				// 1.9.0 向后兼容
			}

			addResult.put(Keys.STATUS_CODE, true);

			renderer.setJSONObject(addResult);
		} catch (final Exception e) {

			logger.error("Can not add comment on article", e);
			jsonObject.put(Keys.STATUS_CODE, false);
			jsonObject.put(Keys.MSG, langPropsService.get("addFailLabel"));
		}
		renderer.render(request, response);
	}

	/**
	 * Fills commenter info if logged in.
	 *
	 * @param requestJSONObject
	 *            the specified request json object
	 * @param request
	 *            the specified HTTP servlet request
	 * @param response
	 *            the specified HTTP servlet response
	 */
	private void fillCommenter(final JSONObject requestJSONObject, final HttpServletRequest request,
			final HttpServletResponse response) {
		userMgmtService.tryLogInWithCookie(request, response);

		final JSONObject currentUser = userQueryService.getCurrentUser(request);
		if (null == currentUser) {
			return;
		}

		requestJSONObject.put(Comment.COMMENT_NAME, currentUser.optString(User.USER_NAME));
		requestJSONObject.put(Comment.COMMENT_EMAIL, currentUser.optString(User.USER_EMAIL));
		requestJSONObject.put(Comment.COMMENT_URL, currentUser.optString(User.USER_URL));
	}
}
