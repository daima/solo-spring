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
package org.b3log.solo.module.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.b3log.solo.frame.model.Role;
import org.b3log.solo.frame.model.User;
import org.b3log.solo.service.UserMgmtService;
import org.b3log.solo.service.UserQueryService;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Authentication filter.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.0.0.4, Jul 10, 2013
 * @since 0.3.1
 */
@Component
public final class AuthFilter implements Filter {
	@Autowired
	private UserMgmtService userMgmtService;
	@Autowired
	private UserQueryService userQueryService;

	/**
	 * Logger.
	 */
	private static Logger logger = LoggerFactory.getLogger(AuthFilter.class);

	@Override
	public void init(final FilterConfig filterConfig) throws ServletException {
	}

	/**
	 * If the specified request is NOT made by an authenticated user, sends
	 * error 403.
	 *
	 * @param request
	 *            the specified request
	 * @param response
	 *            the specified response
	 * @param chain
	 *            filter chain
	 * @throws IOException
	 *             io exception
	 * @throws ServletException
	 *             servlet exception
	 */
	@Override
	public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
			throws IOException, ServletException {
		final HttpServletResponse httpServletResponse = (HttpServletResponse) response;
		final HttpServletRequest httpServletRequest = (HttpServletRequest) request;

		try {
			userMgmtService.tryLogInWithCookie(httpServletRequest, httpServletResponse);

			final JSONObject currentUser = userQueryService.getCurrentUser(httpServletRequest);

			if (null == currentUser) {
				logger.warn("The request has been forbidden");
				httpServletResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
				return;
			}

			final String userRole = currentUser.optString(User.USER_ROLE);

			if (Role.VISITOR_ROLE.equals(userRole)) {
				logger.warn("The request [Visitor] has been forbidden");
				httpServletResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
				return;
			}

			chain.doFilter(request, response);
		} catch (final IOException e) {
			logger.error("Auth filter failed", e);
			httpServletResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
		}
	}

	@Override
	public void destroy() {
	}
}
