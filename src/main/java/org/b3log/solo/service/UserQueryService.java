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

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.b3log.solo.Keys;
import org.b3log.solo.dao.UserDao;
import org.b3log.solo.dao.repository.Query;
import org.b3log.solo.dao.repository.RepositoryException;
import org.b3log.solo.model.GeneralUser;
import org.b3log.solo.model.Pagination;
import org.b3log.solo.model.User;
import org.b3log.solo.util.Paginator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * User query service.
 *
 * @author <a href="http://cxy7.com">XyCai</a>
 * @version 1.0.0.3, Jul 10, 2013
 * @since 0.4.0
 */
@Service
public class UserQueryService {

	/**
	 * Logger.
	 */
	private static Logger logger = LoggerFactory.getLogger(UserQueryService.class);

	/**
	 * User service.
	 */
	@Autowired
	private UserService userService;

	/**
	 * User repository.
	 */
	@Autowired
	private UserDao userDao;

	/**
	 * User management service.
	 */
	@Autowired
	private UserMgmtService userMgmtService;

	/**
	 * Determines whether if exists multiple users in current Solo.
	 *
	 * @return {@code true} if exists, {@code false} otherwise
	 * @throws ServiceException
	 *             service exception
	 */
	public boolean hasMultipleUsers() throws ServiceException {
		final Query query = new Query().setPageCount(1);

		try {
			final JSONArray users = userDao.get(query).getJSONArray(Keys.RESULTS);

			return 1 != users.length();
		} catch (final RepositoryException e) {
			logger.error("Determines multiple users failed", e);

			throw new ServiceException(e);
		} catch (final JSONException e) {
			logger.error("Determines multiple users failed", e);

			throw new ServiceException(e);
		}
	}

	/**
	 * Checks whether the current request is made by a logged in user (including
	 * default user and administrator lists in <i>users</i>).
	 * 
	 * <p>
	 * Invokes this method will try to login with cookie first.
	 * </p>
	 *
	 * @param request
	 *            the specified request
	 * @param response
	 *            the specified response
	 * @return {@code true} if the current request is made by logged in user,
	 *         returns {@code false} otherwise
	 */
	public boolean isLoggedIn(final HttpServletRequest request, final HttpServletResponse response) {
		userMgmtService.tryLogInWithCookie(request, response);

		final GeneralUser currentUser = userService.getCurrentUser(request);

		return null != currentUser;
	}

	/**
	 * Checks whether the current request is made by logged in administrator.
	 *
	 * @param request
	 *            the specified request
	 * @return {@code true} if the current request is made by logged in
	 *         administrator, returns {@code false} otherwise
	 */
	public boolean isAdminLoggedIn(final HttpServletRequest request) {
		return userService.isUserLoggedIn(request) && userService.isUserAdmin(request);
	}

	/**
	 * Gets the current user.
	 *
	 * @param request
	 *            the specified request
	 * @return the current user, {@code null} if not found
	 */
	public JSONObject getCurrentUser(final HttpServletRequest request) {
		final GeneralUser currentUser = userService.getCurrentUser(request);

		if (null == currentUser) {
			return null;
		}

		final String email = currentUser.getEmail();

		try {
			return userDao.getByEmail(email);
		} catch (final RepositoryException e) {
			logger.error("Gets current user by request failed, returns null", e);

			return null;
		}
	}

	/**
	 * Gets the administrator.
	 * 
	 * @return administrator, returns {@code null} if not found
	 * @throws ServiceException
	 *             service exception
	 */
	public JSONObject getAdmin() throws ServiceException {
		try {
			return userDao.getAdmin();
		} catch (final RepositoryException e) {
			logger.error("Gets admin failed", e);
			throw new ServiceException(e);
		}
	}

	/**
	 * Gets a user by the specified email.
	 *
	 * @param email
	 *            the specified email
	 * @return user, returns {@code null} if not found
	 * @throws ServiceException
	 *             service exception
	 */
	public JSONObject getUserByEmail(final String email) throws ServiceException {
		try {
			return userDao.getByEmail(email);
		} catch (final RepositoryException e) {
			logger.error("Gets user by email[" + email + "] failed", e);
			throw new ServiceException(e);
		}
	}

	/**
	 * Gets users by the specified request json object.
	 *
	 * @param requestJSONObject
	 *            the specified request json object, for example,
	 * 
	 *            <pre>
	 * {
	 *     "paginationCurrentPageNum": 1,
	 *     "paginationPageSize": 20,
	 *     "paginationWindowSize": 10,
	 * }, see {@link Pagination} for more details
	 *            </pre>
	 * 
	 * @return for example,
	 * 
	 *         <pre>
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
	 * }
	 *         </pre>
	 * 
	 * @throws ServiceException
	 *             service exception
	 * @see Pagination
	 */
	public JSONObject getUsers(final JSONObject requestJSONObject) throws ServiceException {
		final JSONObject ret = new JSONObject();

		final int currentPageNum = requestJSONObject.optInt(Pagination.PAGINATION_CURRENT_PAGE_NUM);
		final int pageSize = requestJSONObject.optInt(Pagination.PAGINATION_PAGE_SIZE);
		final int windowSize = requestJSONObject.optInt(Pagination.PAGINATION_WINDOW_SIZE);
		final Query query = new Query().setCurrentPageNum(currentPageNum).setPageSize(pageSize);

		JSONObject result = null;

		try {
			result = userDao.get(query);
		} catch (final RepositoryException e) {
			logger.error("Gets users failed", e);

			throw new ServiceException(e);
		}

		final int pageCount = result.optJSONObject(Pagination.PAGINATION).optInt(Pagination.PAGINATION_PAGE_COUNT);

		final JSONObject pagination = new JSONObject();

		ret.put(Pagination.PAGINATION, pagination);
		final List<Integer> pageNums = Paginator.paginate(currentPageNum, pageSize, pageCount, windowSize);

		pagination.put(Pagination.PAGINATION_PAGE_COUNT, pageCount);
		pagination.put(Pagination.PAGINATION_PAGE_NUMS, pageNums);

		final JSONArray users = result.optJSONArray(Keys.RESULTS);

		ret.put(User.USERS, users);

		return ret;
	}

	/**
	 * Gets a user by the specified user id.
	 *
	 * @param userId
	 *            the specified user id
	 * @return for example,
	 * 
	 *         <pre>
	 * {
	 *     "user": {
	 *         "oId": "",
	 *         "userName": "",
	 *         "userEmail": "",
	 *         "userPassword": ""
	 *     }
	 * }
	 *         </pre>
	 * 
	 *         , returns {@code null} if not found
	 * @throws ServiceException
	 *             service exception
	 */
	public JSONObject getUser(final String userId) throws ServiceException {
		final JSONObject ret = new JSONObject();

		JSONObject user = null;

		try {
			user = userDao.get(userId);
		} catch (final RepositoryException e) {
			logger.error("Gets a user failed", e);
			throw new ServiceException(e);
		}

		if (null == user) {
			return null;
		}

		ret.put(User.USER, user);

		return ret;
	}

	/**
	 * Gets the URL of user logout.
	 *
	 * @return logout URL, returns {@code null} if the user is not logged in
	 */
	public String getLogoutURL() {
		return userService.createLogoutURL("/");
	}

	/**
	 * Gets the URL of user login.
	 *
	 * @param redirectURL
	 *            redirect URL after logged in
	 * @return login URL
	 */
	public String getLoginURL(final String redirectURL) {
		return userService.createLoginURL(redirectURL);
	}

	/**
	 * Sets the user management service with the specified user management
	 * service.
	 * 
	 * @param userMgmtService
	 *            the specified user management service
	 */
	public void setUserMgmtService(final UserMgmtService userMgmtService) {
		this.userMgmtService = userMgmtService;
	}

	/**
	 * Sets the user repository with the specified user repository.
	 * 
	 * @param userDao
	 *            the specified user repository
	 */
	public void setUserRepository(final UserDao userDao) {
		this.userDao = userDao;
	}
}
