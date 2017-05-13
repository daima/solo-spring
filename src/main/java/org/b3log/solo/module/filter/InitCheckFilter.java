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

import org.b3log.solo.Keys;
import org.b3log.solo.Latkes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Checks initialization filter.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.1.1.2, Sep 13, 2016
 * @since 0.3.1
 */
public final class InitCheckFilter implements Filter {
//	private InitService initService;

    /**
     * Logger.
     */
    private static Logger logger = LoggerFactory.getLogger(InitCheckFilter.class);

    /**
     * Whether initialization info reported.
     */
    private static boolean initReported;

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {
    }

    /**
     * If Solo has not been initialized, so redirects to /init.
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
        final String requestURI = httpServletRequest.getRequestURI();

        logger.trace("Request[URI={0}]", requestURI);

        // If requests Latke Remote APIs, skips this filter 
        if (requestURI.startsWith(Latkes.getContextPath() + "/latke/remote")) {
            chain.doFilter(request, response);

            return;
        }

//        if (initService.isInited()) {
//            chain.doFilter(request, response);
//
//            return;
//        }

        if ("POST".equalsIgnoreCase(httpServletRequest.getMethod()) && (Latkes.getContextPath() + "/init").equals(requestURI)) {
            // Do initailization
            chain.doFilter(request, response);

            return;
        }

//        if (!initReported) {
//            logger.debug( "Solo has not been initialized, so redirects to /init");
//            initReported = true;
//        }

        request.setAttribute(Keys.HttpRequest.REQUEST_URI, Latkes.getContextPath() + "/init");
        request.setAttribute(Keys.HttpRequest.REQUEST_METHOD, RequestMethod.GET.name());

//        final HttpControl httpControl = new HttpControl(DispatcherServlet.SYS_HANDLER.iterator(), context);
//
//        try {
//            httpControl.nextHandler();
//        } catch (final Exception e) {
//            context.setRenderer(new HTTP500Renderer(e));
//        }
//
//        DispatcherServlet.result(context);
    }

    @Override
    public void destroy() {
    }
}
