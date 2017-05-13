/*
 * Copyright (c) 2009-2016, b3log.org & hacpai.com
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
package org.b3log.solo.frame.user.local;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.servlet.http.HttpServletRequest;

import org.b3log.solo.Keys;
import org.b3log.solo.Latkes;
import org.b3log.solo.frame.model.Role;
import org.b3log.solo.frame.model.User;
import org.b3log.solo.frame.user.GeneralUser;
import org.b3log.solo.frame.user.UserService;
import org.b3log.solo.util.Sessions;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Local user service.
 *
 * @author <a href="mailto:wmainlove@gmail.com">Love Yao</a>
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.0.0.1, May 4, 2012
 */
public final class LocalUserService implements UserService {

	/**
	 * Logger.
	 */
	private static Logger logger = LoggerFactory.getLogger(LocalUserService.class);

	@Override
	public GeneralUser getCurrentUser(final HttpServletRequest request) {
		final JSONObject currentUser = Sessions.currentUser(request);

		if (null == currentUser) {
			return null;
		}

		final GeneralUser ret = new GeneralUser();

		ret.setEmail(currentUser.optString(User.USER_EMAIL));
		ret.setId(currentUser.optString(Keys.OBJECT_ID));
		ret.setNickname(currentUser.optString(User.USER_NAME));

		return ret;
	}

	@Override
	public boolean isUserLoggedIn(final HttpServletRequest request) {
		return null != Sessions.currentUser(request);
	}

	@Override
	public boolean isUserAdmin(final HttpServletRequest request) {
		final JSONObject currentUser = Sessions.currentUser(request);

		if (null == currentUser) {
			return false;
		}

		return Role.ADMIN_ROLE.equals(currentUser.optString(User.USER_ROLE));
	}

	@Override
	public String createLoginURL(final String destinationURL) {
		String to = Latkes.getServePath();

		try {
			to = URLEncoder.encode(to + destinationURL, "UTF-8");
		} catch (final UnsupportedEncodingException e) {
			logger.error("URL encode[string={0}]", destinationURL);
		}

		return Latkes.getContextPath() + "/login?goto=" + to;
	}

	@Override
	public String createLogoutURL(final String destinationURL) {
		String to = Latkes.getServePath();

		try {
			to = URLEncoder.encode(to + destinationURL, "UTF-8");
		} catch (final UnsupportedEncodingException e) {
			logger.error("URL encode[string={0}]", destinationURL);
		}

		return Latkes.getContextPath() + "/logout?goto=" + to;
	}
}
