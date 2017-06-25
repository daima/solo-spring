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
package org.b3log.solo.controller.console;

import java.net.URLDecoder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.b3log.solo.Keys;
import org.b3log.solo.Latkes;
import org.b3log.solo.model.Option;
import org.b3log.solo.model.Role;
import org.b3log.solo.model.User;
import org.b3log.solo.module.util.QueryResults;
import org.b3log.solo.renderer.JSONRenderer;
import org.b3log.solo.service.LangPropsService;
import org.b3log.solo.service.PreferenceQueryService;
import org.b3log.solo.service.ServiceException;
import org.b3log.solo.service.UserMgmtService;
import org.b3log.solo.service.UserQueryService;
import org.b3log.solo.util.Requests;
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
 * User console request processing.
 *
 * @author <a href="http://cxy7.com">XyCai</a>
 * @author <a href="mailto:385321165@qq.com">DASHU</a>
 * @version 1.2.0.5, Mar 31, 2017
 * @since 0.4.0
 */
@Controller
public class UserConsole {

	/**
	 * Logger.
	 */
	private static Logger logger = LoggerFactory.getLogger(UserConsole.class);

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
	 * Language service.
	 */
	@Autowired
	private LangPropsService langPropsService;

	/**
	 * Updates a user by the specified request.
	 * <p>
	 * Renders the response with a json object, for example,
	 * 
	 * <pre>
	 * {
	 *     "sc": boolean,
	 *     "msg": ""
	 * }
	 * </pre>
	 * </p>
	 *
	 * @param request
	 *            the specified http servlet request, for example, "oId": "",
	 *            "userName": "", "userEmail": "", "userPassword": "", //
	 *            Unhashed "userRole": "", // optional "userURL": "", //
	 *            optional "userAvatar": "" // optional
	 * @param context
	 *            the specified http request context
	 * @param response
	 *            the specified http servlet response
	 * @throws Exception
	 *             exception
	 */
	@RequestMapping(value = "/console/user/", method = RequestMethod.PUT)
	public void updateUser(final HttpServletRequest request, final HttpServletResponse response, @RequestParam String body) throws Exception {
		if (!userQueryService.isAdminLoggedIn(request)) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
			return;
		}

		final JSONRenderer renderer = new JSONRenderer();
		final JSONObject ret = new JSONObject();

		try {
			body = URLDecoder.decode(body, "UTF-8");final JSONObject requestJSONObject = new JSONObject(body);
			userMgmtService.updateUser(requestJSONObject);
			ret.put(Keys.STATUS_CODE, true);
			ret.put(Keys.MSG, langPropsService.get("updateSuccLabel"));

			renderer.setJSONObject(ret);
		} catch (final ServiceException e) {
			logger.error(e.getMessage(), e);

			final JSONObject jsonObject = QueryResults.defaultResult();

			renderer.setJSONObject(jsonObject);
			jsonObject.put(Keys.MSG, e.getMessage());
		}
		renderer.render(request, response);
	}

	/**
	 * Adds a user with the specified request.
	 * <p>
	 * Renders the response with a json object, for example,
	 * 
	 * <pre>
	 * {
	 *     "sc": boolean,
	 *     "oId": "", // Generated user id
	 *     "msg": ""
	 * }
	 * </pre>
	 * </p>
	 *
	 * @param request
	 *            the specified http servlet request, for example, "userName":
	 *            "", "userEmail": "", "userPassword": "", "userURL": "", //
	 *            optional, uses 'servePath' instead if not specified
	 *            "userRole": "", // optional, uses
	 *            {@value org.b3log.solo.model.Role#DEFAULT_ROLE} instead if not
	 *            specified "userAvatar": "" // optional
	 * @param response
	 *            the specified http servlet response
	 * @param context
	 *            the specified http request context
	 * @throws Exception
	 *             exception
	 */
	@RequestMapping(value = "/console/user/", method = RequestMethod.POST)
	public void addUser(final HttpServletRequest request, final HttpServletResponse response, @RequestBody String body)
			throws Exception {
		final JSONRenderer renderer = new JSONRenderer();
		final JSONObject ret = new JSONObject();
		renderer.setJSONObject(ret);

		try {
			body = URLDecoder.decode(body, "UTF-8");
			final JSONObject requestJSONObject = new JSONObject(body);

			if (userQueryService.isAdminLoggedIn(request)) { // if the
																// administrator
																// register a
																// new user,
																// treats the
																// new user as a
																// normal user
				// (defaultRole) who could post article
				requestJSONObject.put(User.USER_ROLE, Role.DEFAULT_ROLE);
			} else {
				final JSONObject preference = preferenceQueryService.getPreference();

				if (!preference.optBoolean(Option.ID_C_ALLOW_REGISTER)) {
					ret.put(Keys.STATUS_CODE, false);
					ret.put(Keys.MSG, langPropsService.get("notAllowRegisterLabel"));
					renderer.render(request, response);
					return;
				}

				// if a normal user or a visitor register a new user, treates
				// the new user as a visitor
				// (visitorRole) who couldn't post article
				requestJSONObject.put(User.USER_ROLE, Role.VISITOR_ROLE);
			}

			final String userId = userMgmtService.addUser(requestJSONObject);

			ret.put(Keys.OBJECT_ID, userId);
			ret.put(Keys.MSG, langPropsService.get("addSuccLabel"));
			ret.put(Keys.STATUS_CODE, true);
		} catch (final ServiceException e) {
			logger.error(e.getMessage(), e);

			final JSONObject jsonObject = QueryResults.defaultResult();

			renderer.setJSONObject(jsonObject);
			jsonObject.put(Keys.MSG, e.getMessage());
		}
		renderer.render(request, response);
	}

	/**
	 * Removes a user by the specified request.
	 * <p>
	 * Renders the response with a json object, for example,
	 * 
	 * <pre>
	 * {
	 *     "sc": boolean,
	 *     "msg": ""
	 * }
	 * </pre>
	 * </p>
	 *
	 * @param request
	 *            the specified http servlet request
	 * @param response
	 *            the specified http servlet response
	 * @param context
	 *            the specified http request context
	 * @throws Exception
	 *             exception
	 */
	@RequestMapping(value = "/console/user/*", method = RequestMethod.DELETE)
	public void removeUser(final HttpServletRequest request, final HttpServletResponse response) throws Exception {
		if (!userQueryService.isAdminLoggedIn(request)) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
			return;
		}

		final JSONRenderer renderer = new JSONRenderer();

		final JSONObject jsonObject = new JSONObject();
		renderer.setJSONObject(jsonObject);
		try {
			final String userId = request.getRequestURI()
					.substring((Latkes.getContextPath() + "/console/user/").length());
			userMgmtService.removeUser(userId);

			jsonObject.put(Keys.STATUS_CODE, true);
			jsonObject.put(Keys.MSG, langPropsService.get("removeSuccLabel"));
		} catch (final ServiceException e) {
			logger.error(e.getMessage(), e);

			jsonObject.put(Keys.STATUS_CODE, false);
			jsonObject.put(Keys.MSG, langPropsService.get("removeFailLabel"));
		}
		renderer.render(request, response);
	}

	/**
	 * Gets users by the specified request json object.
	 * <p>
	 * The request URI contains the pagination arguments. For example, the
	 * request URI is /console/users/1/10/20, means the current page is 1, the
	 * page size is 10, and the window size is 20.
	 * </p>
	 * <p>
	 * Renders the response with a json object, for example,
	 * 
	 * <pre>
	 * {
	 *     "pagination": {
	 *         "paginationPageCount": 100,
	 *         "paginationPageNums": [1, 2, 3, 4, 5]
	 *     },
	 *     "users": [{
	 *         "oId": "",
	 *         "userName": "",
	 *         "userEmail": "",
	 *         "userPassword": "",
	 *         "roleName": ""
	 *      }, ....]
	 *     "sc": true
	 * }
	 * </pre>
	 * </p>
	 *
	 * @param request
	 *            the specified http servlet request
	 * @param response
	 *            the specified http servlet response
	 * @param context
	 *            the specified http request context
	 * @throws Exception
	 *             exception
	 */
	@RequestMapping(value = "/console/users/*/*/*"/*
													 * Requests.
													 * PAGINATION_PATH_PATTERN
													 */, method = RequestMethod.GET)
	public void getUsers(final HttpServletRequest request, final HttpServletResponse response) throws Exception {
		final JSONRenderer renderer = new JSONRenderer();
		if (!userQueryService.isAdminLoggedIn(request)) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
			return;
		}

		try {
			final String requestURI = request.getRequestURI();
			final String path = requestURI.substring((Latkes.getContextPath() + "/console/users/").length());

			final JSONObject requestJSONObject = Requests.buildPaginationRequest(path);

			final JSONObject result = userQueryService.getUsers(requestJSONObject);
			result.put(Keys.STATUS_CODE, true);
			renderer.setJSONObject(result);
		} catch (final ServiceException e) {
			logger.error(e.getMessage(), e);

			final JSONObject jsonObject = QueryResults.defaultResult();
			renderer.setJSONObject(jsonObject);
			jsonObject.put(Keys.MSG, langPropsService.get("getFailLabel"));
		}
		renderer.render(request, response);
	}

	/**
	 * Gets a user by the specified request.
	 * <p>
	 * Renders the response with a json object, for example,
	 * 
	 * <pre>
	 * {
	 *     "sc": boolean,
	 *     "user": {
	 *         "oId": "",
	 *         "userName": "",
	 *         "userEmail": "",
	 *         "userPassword": "",
	 *         "userAvatar": ""
	 *     }
	 * }
	 * </pre>
	 * </p>
	 *
	 * @param request
	 *            the specified http servlet request
	 * @param response
	 *            the specified http servlet response
	 * @param context
	 *            the specified http request context
	 * @throws Exception
	 *             exception
	 */
	@RequestMapping(value = "/console/user/*", method = RequestMethod.GET)
	public void getUser(final HttpServletRequest request, final HttpServletResponse response) throws Exception {
		if (!userQueryService.isAdminLoggedIn(request)) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
			return;
		}

		final JSONRenderer renderer = new JSONRenderer();
		try {
			final String requestURI = request.getRequestURI();
			final String userId = requestURI.substring((Latkes.getContextPath() + "/console/user/").length());

			final JSONObject result = userQueryService.getUser(userId);
			if (null == result) {
				renderer.setJSONObject(QueryResults.defaultResult());
				renderer.render(request, response);
				return;
			}

			renderer.setJSONObject(result);
			result.put(Keys.STATUS_CODE, true);
		} catch (final ServiceException e) {
			logger.error(e.getMessage(), e);

			final JSONObject jsonObject = QueryResults.defaultResult();
			renderer.setJSONObject(jsonObject);
			jsonObject.put(Keys.MSG, langPropsService.get("getFailLabel"));
		}
		renderer.render(request, response);
	}

	/**
	 * Change a user role.
	 * <p>
	 * Renders the response with a json object, for example,
	 * 
	 * <pre>
	 * {
	 *     "sc": boolean,
	 *     "msg": ""
	 * }
	 * </pre>
	 * </p>
	 *
	 * @param request
	 *            the specified http servlet request
	 * @param response
	 *            the specified http servlet response
	 * @param context
	 *            the specified http request context
	 * @throws Exception
	 *             exception
	 */
	@RequestMapping(value = "/console/changeRole/*", method = RequestMethod.GET)
	public void changeUserRole(final HttpServletRequest request, final HttpServletResponse response) throws Exception {
		if (!userQueryService.isAdminLoggedIn(request)) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
			return;
		}

		final JSONRenderer renderer = new JSONRenderer();
		final JSONObject jsonObject = new JSONObject();
		renderer.setJSONObject(jsonObject);
		try {
			final String userId = request.getRequestURI()
					.substring((Latkes.getContextPath() + "/console/changeRole/").length());
			userMgmtService.changeRole(userId);

			jsonObject.put(Keys.STATUS_CODE, true);
			jsonObject.put(Keys.MSG, langPropsService.get("updateSuccLabel"));
		} catch (final ServiceException e) {
			logger.error(e.getMessage(), e);

			jsonObject.put(Keys.STATUS_CODE, false);
			jsonObject.put(Keys.MSG, langPropsService.get("removeFailLabel"));
		}
		renderer.render(request, response);
	}
}
