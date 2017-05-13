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
package org.b3log.solo.frame.servlet.filter;


import org.b3log.solo.frame.logging.Level;
import org.b3log.solo.frame.logging.Logger;
import org.b3log.solo.util.Sessions;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


/**
 * Authentication filter.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.0.1.1, Aug 15, 2010
 */
public final class AuthenticationFilter implements Filter {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(AuthenticationFilter.class.getName());

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {}

    /**
     * Filters the has not logged in user for the specified request. Send error
     * {@linkplain HttpServletResponse#SC_FORBIDDEN}.
     *
     * @param request the specified request
     * @param response the specified response
     * @param chain filter chain
     * @throws IOException io exception
     * @throws ServletException servlet exception
     */
    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
        throws IOException, ServletException {
        final HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        final HttpServletResponse httpServletResponse = (HttpServletResponse) response;

        if (!hasLoggedIn(httpServletRequest)) {
            LOGGER.log(Level.WARN, "Authenticate fail for request[{0}]", request);

            httpServletResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {}

    /**
     * Determines whether the user for the specified request has logged in.
     *
     * @param request the specified request
     * @return {@code true} if logged in, {@code false} otherwise
     */
    private boolean hasLoggedIn(final HttpServletRequest request) {
        final String requestURI = request.getRequestURI();
        final String requestURL = request.getRequestURL().toString();

        LOGGER.log(Level.TRACE, "Request[URI={0}, URL={1}]", new Object[] {requestURI, requestURL});

        final String userName = Sessions.currentUserName(request);

        LOGGER.log(Level.TRACE, "Session[userName={0}]", userName);

        return null != userName ? true : false;
    }
}
