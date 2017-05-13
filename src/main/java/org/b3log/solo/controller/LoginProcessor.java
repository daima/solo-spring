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
import java.util.Calendar;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.b3log.solo.Keys;
import org.b3log.solo.Latkes;
import org.b3log.solo.SoloConstant;
import org.b3log.solo.dao.OptionDao;
import org.b3log.solo.dao.repository.RepositoryException;
import org.b3log.solo.model.Common;
import org.b3log.solo.model.MailMessage;
import org.b3log.solo.model.Option;
import org.b3log.solo.model.Role;
import org.b3log.solo.model.User;
import org.b3log.solo.module.util.Randoms;
import org.b3log.solo.renderer.ConsoleRenderer;
import org.b3log.solo.renderer.JSONRenderer;
import org.b3log.solo.renderer.freemarker.AbstractFreeMarkerRenderer;
import org.b3log.solo.service.LangPropsService;
import org.b3log.solo.service.MailService;
import org.b3log.solo.service.OptionQueryService;
import org.b3log.solo.service.PreferenceQueryService;
import org.b3log.solo.service.ServiceException;
import org.b3log.solo.service.UserMgmtService;
import org.b3log.solo.service.UserQueryService;
import org.b3log.solo.service.UserService;
import org.b3log.solo.service.html.Filler;
import org.b3log.solo.util.MD5;
import org.b3log.solo.util.Requests;
import org.b3log.solo.util.Sessions;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Login/logout processor.
 *
 * <p>
 * Initializes administrator
 * </p>
 * .
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @author <a href="http://vanessa.b3log.org">Liyuan Li</a>
 * @author <a href="mailto:dongxu.wang@acm.org">Dongxu Wang</a>
 * @version 1.1.1.7, Nov 20, 2015
 * @since 0.3.1
 */
@Controller
public class LoginProcessor {

	/**
	 * Logger.
	 */
	private static Logger logger = LoggerFactory.getLogger(LoginProcessor.class);

	/**
	 * User query service.
	 */
	@Autowired
	private UserQueryService userQueryService;

	/**
	 * User service.
	 */
	@Autowired
	private UserService userService;

	/**
	 * Mail service.
	 */
	@Autowired
	private MailService mailService;

	/**
	 * User management service.
	 */
	@Autowired
	private UserMgmtService userMgmtService;

	/**
	 * Language service.
	 */
	@Autowired
	private LangPropsService langPropsService;

	/**
	 * Filler.
	 */
	@Autowired
	private Filler filler;

	/**
	 * Preference query service.
	 */
	@Autowired
	private PreferenceQueryService preferenceQueryService;

	/**
	 * Option query service.
	 */
	@Autowired
	private OptionQueryService optionQueryService;

	/**
	 * Option repository.
	 */
	@Autowired
	private OptionDao optionRepository;

	/**
	 * Shows login page.
	 *
	 * @param context
	 *            the specified context
	 * @throws Exception
	 *             exception
	 */
	@RequestMapping(value = "/login", method = RequestMethod.GET)
	public void showLogin(final HttpServletRequest request, final HttpServletResponse response) throws Exception {
		String destinationURL = request.getParameter(Common.GOTO);

		if (StringUtils.isBlank(destinationURL)) {
			destinationURL = Latkes.getServePath() + Common.ADMIN_INDEX_URI;
		}

		userMgmtService.tryLogInWithCookie(request, response);

		if (null != userService.getCurrentUser(request)) { // User has already
															// logged in
			response.sendRedirect(destinationURL);
			return;
		}

		renderPage(request, response, "login.ftl", destinationURL);
	}

	/**
	 * Logins.
	 * 
	 * <p>
	 * Renders the response with a json object, for example,
	 * 
	 * <pre>
	 * {
	 *     "isLoggedIn": boolean,
	 *     "msg": "" // optional, exists if isLoggedIn equals to false
	 * }
	 * </pre>
	 * </p>
	 *
	 * @param context
	 *            the specified context
	 */
	@RequestMapping(value = "/login", method = RequestMethod.POST)
	public void login(final HttpServletRequest request, final HttpServletResponse response) {
		final JSONRenderer renderer = new JSONRenderer();
		final JSONObject jsonObject = new JSONObject();

		renderer.setJSONObject(jsonObject);

		try {
			jsonObject.put(Common.IS_LOGGED_IN, false);
			final String loginFailLabel = langPropsService.get("loginFailLabel");

			jsonObject.put(Keys.MSG, loginFailLabel);

			final JSONObject requestJSONObject = Requests.parseRequestJSONObject(request, response);
			final String userEmail = requestJSONObject.getString(User.USER_EMAIL);
			final String userPwd = requestJSONObject.getString(User.USER_PASSWORD);

			if (StringUtils.isBlank(userEmail) || StringUtils.isBlank(userPwd)) {
				renderer.render(request, response);
				return;
			}

			logger.info("Login[email={0}]", userEmail);

			final JSONObject user = userQueryService.getUserByEmail(userEmail);

			if (null == user) {
				logger.warn("Not found user[email={0}]", userEmail);
				renderer.render(request, response);
				return;
			}

			if (MD5.hash(userPwd).equals(user.getString(User.USER_PASSWORD))) {
				Sessions.login(request, response, user);

				logger.info("Logged in[email={0}]", userEmail);

				jsonObject.put(Common.IS_LOGGED_IN, true);

				if (Role.VISITOR_ROLE.equals(user.optString(User.USER_ROLE))) {
					jsonObject.put("to", Latkes.getServePath());
				} else {
					jsonObject.put("to", Latkes.getServePath() + Common.ADMIN_INDEX_URI);
				}

				jsonObject.remove(Keys.MSG);
				renderer.render(request, response);
				return;
			}

			logger.warn("Wrong password[{0}]", userPwd);
		} catch (final Exception e) {
			logger.error(e.getMessage(), e);
		}
		renderer.render(request, response);
	}

	/**
	 * Logout.
	 *
	 * @param context
	 *            the specified context
	 * @throws IOException
	 *             io exception
	 */
	@RequestMapping(value = "/logout", method = RequestMethod.GET)
	public void logout(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
		Sessions.logout(request, response);

		String destinationURL = request.getParameter(Common.GOTO);

		if (StringUtils.isBlank(destinationURL)) {
			destinationURL = "/";
		}

		response.sendRedirect(destinationURL);
	}

	/**
	 * Shows forgotten password page.
	 *
	 * @param context
	 *            the specified context
	 * @throws Exception
	 *             exception
	 */
	@RequestMapping(value = "/forgot", method = RequestMethod.GET)
	public void showForgot(final HttpServletRequest request, final HttpServletResponse response) throws Exception {
		String destinationURL = request.getParameter(Common.GOTO);

		if (StringUtils.isBlank(destinationURL)) {
			destinationURL = Latkes.getServePath() + Common.ADMIN_INDEX_URI;
		}

		renderPage(request, response, "reset-pwd.ftl", destinationURL);
	}

	/**
	 * Resets forgotten password.
	 * 
	 * <p>
	 * Renders the response with a json object, for example,
	 * 
	 * <pre>
	 * {
	 *     "isLoggedIn": boolean,
	 *     "msg": "" // optional, exists if isLoggedIn equals to false
	 * }
	 * </pre>
	 * </p>
	 *
	 * @param context
	 *            the specified context
	 */
	@RequestMapping(value = "/forgot", method = RequestMethod.POST)
	public void forgot(final HttpServletRequest request, final HttpServletResponse response) {
		final JSONRenderer renderer = new JSONRenderer();
		final JSONObject jsonObject = new JSONObject();

		renderer.setJSONObject(jsonObject);

		try {
			jsonObject.put("succeed", false);
			jsonObject.put(Keys.MSG, langPropsService.get("resetPwdSuccessMsg"));

			final JSONObject requestJSONObject = Requests.parseRequestJSONObject(request, response);
			final String userEmail = requestJSONObject.getString(User.USER_EMAIL);

			if (StringUtils.isBlank(userEmail)) {
				logger.warn("Why user's email is empty");
				return;
			}

			logger.info("Login[email={0}]", userEmail);

			final JSONObject user = userQueryService.getUserByEmail(userEmail);

			if (null == user) {
				logger.warn("Not found user[email={0}]", userEmail);
				jsonObject.put(Keys.MSG, langPropsService.get("userEmailNotFoundMsg"));
				return;
			}

			sendResetUrl(userEmail, jsonObject);
		} catch (final Exception e) {
			logger.error(e.getMessage(), e);
		}
		renderer.render(request, response);
	}

	/**
	 * Resets forgotten password.
	 *
	 * <p>
	 * Renders the response with a json object, for example,
	 * 
	 * <pre>
	 * {
	 *     "isLoggedIn": boolean,
	 *     "msg": "" // optional, exists if isLoggedIn equals to false
	 * }
	 * </pre>
	 * </p>
	 *
	 * @param context
	 *            the specified context
	 */
	@RequestMapping(value = "/reset", method = RequestMethod.POST)
	public void reset(final HttpServletRequest request, final HttpServletResponse response) {
		final JSONRenderer renderer = new JSONRenderer();
		final JSONObject jsonObject = new JSONObject();
		renderer.setJSONObject(jsonObject);

		try {
			final JSONObject requestJSONObject;
			requestJSONObject = Requests.parseRequestJSONObject(request, response);
			final String userEmail = requestJSONObject.getString(User.USER_EMAIL);
			final String newPwd = requestJSONObject.getString("newPwd");
			final JSONObject user = userQueryService.getUserByEmail(userEmail);

			user.put(User.USER_PASSWORD, newPwd);
			userMgmtService.updateUser(user);
			logger.debug("[{0}]'s password updated successfully.", userEmail);

			jsonObject.put("succeed", true);
			jsonObject.put("to", Latkes.getServePath() + "/login?from=reset");
			jsonObject.put(Keys.MSG, langPropsService.get("resetPwdSuccessMsg"));
		} catch (final Exception e) {
			logger.error(e.getMessage(), e);
		}
		renderer.render(request, response);
	}

	/**
	 * Sends the password resetting URL with a random token.
	 *
	 * @param userEmail
	 *            the given email
	 * @param jsonObject
	 *            return code and message object
	 * @throws JSONException
	 *             the JSONException
	 * @throws ServiceException
	 *             the ServiceException
	 * @throws IOException
	 *             the IOException
	 * @throws RepositoryException
	 *             the RepositoryException
	 */
	private void sendResetUrl(final String userEmail, final JSONObject jsonObject)
			throws JSONException, ServiceException, IOException, RepositoryException {
		final JSONObject preference = preferenceQueryService.getPreference();
		final String token = new Randoms().nextStringWithMD5();
		final String adminEmail = preference.getString(Option.ID_C_ADMIN_EMAIL);
		final String mailSubject = langPropsService.get("resetPwdMailSubject");
		final String mailBody = langPropsService.get("resetPwdMailBody") + " " + Latkes.getServePath()
				+ "/forgot?token=" + token + "&login=" + userEmail;
		final MailMessage message = new MailMessage();

		final JSONObject option = new JSONObject();

		option.put(Keys.OBJECT_ID, token);
		option.put(Option.OPTION_CATEGORY, "passwordReset");
		option.put(Option.OPTION_VALUE, System.currentTimeMillis());

		// final Transaction transaction = optionRepository.beginTransaction();

		optionRepository.add(option);
		// transaction.commit();

		message.setFrom(adminEmail);
		message.addRecipient(userEmail);
		message.setSubject(mailSubject);
		message.setHtmlBody(mailBody);

		mailService.send(message);

		jsonObject.put("succeed", true);
		jsonObject.put("to", Latkes.getServePath() + "/login?from=forgot");
		jsonObject.put(Keys.MSG, langPropsService.get("resetPwdSuccessSend"));

		logger.debug("Sent a mail[mailSubject={0}, mailBody=[{1}] to [{2}]", mailSubject, mailBody, userEmail);
	}

	/**
	 * Render a page template with the destination URL.
	 *
	 * @param context
	 *            the context
	 * @param pageTemplate
	 *            the page template
	 * @param destinationURL
	 *            the destination URL
	 * @param request
	 *            for reset password page
	 * @throws JSONException
	 *             the JSONException
	 * @throws ServiceException
	 *             the ServiceException
	 */
	private void renderPage(final HttpServletRequest request, final HttpServletResponse response,
			final String pageTemplate, final String destinationURL) throws JSONException, ServiceException {
		final AbstractFreeMarkerRenderer renderer = new ConsoleRenderer();

		renderer.setTemplateName(pageTemplate);

		final Map<String, Object> dataModel = renderer.getDataModel();
		final Map<String, String> langs = langPropsService.getAll(Latkes.getLocale());
		final JSONObject preference = preferenceQueryService.getPreference();

		dataModel.putAll(langs);
		dataModel.put(Common.GOTO, destinationURL);
		dataModel.put(Common.YEAR, String.valueOf(Calendar.getInstance().get(Calendar.YEAR)));
		dataModel.put(Common.VERSION, SoloConstant.VERSION);
		dataModel.put(Common.STATIC_RESOURCE_VERSION, Latkes.getStaticResourceVersion());
		dataModel.put(Option.ID_C_BLOG_TITLE, preference.getString(Option.ID_C_BLOG_TITLE));

		final String token = request.getParameter("token");
		final String email = request.getParameter("login");
		final JSONObject tokenObj = optionQueryService.getOptionById(token);

		if (tokenObj == null) {
			dataModel.put("inputType", "email");
		} else {
			// TODO verify the expired time in the tokenObj
			dataModel.put("inputType", "password");
			dataModel.put("userEmailHidden", email);
		}

		final String from = request.getParameter("from");

		if ("forgot".equals(from)) {
			dataModel.put("resetMsg", langPropsService.get("resetPwdSuccessSend"));
		} else if ("reset".equals(from)) {
			dataModel.put("resetMsg", langPropsService.get("resetPwdSuccessMsg"));
		} else {
			dataModel.put("resetMsg", "");
		}

		Keys.fillRuntime(dataModel);
		filler.fillMinified(dataModel);
		renderer.render(request, response);
	}
}
