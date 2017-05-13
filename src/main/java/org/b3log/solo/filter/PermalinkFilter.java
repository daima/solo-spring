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
package org.b3log.solo.filter;

import java.io.File;
import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.b3log.solo.Keys;
import org.b3log.solo.Latkes;
import org.b3log.solo.SoloConstant;
import org.b3log.solo.dao.ArticleDao;
import org.b3log.solo.dao.PageDao;
import org.b3log.solo.dao.repository.RepositoryException;
import org.b3log.solo.model.Article;
import org.b3log.solo.model.Option;
import org.b3log.solo.model.Page;
import org.b3log.solo.module.util.Skins;
import org.b3log.solo.service.ArticleQueryService;
import org.b3log.solo.service.PermalinkQueryService;
import org.b3log.solo.service.PreferenceQueryService;
import org.b3log.solo.service.ServiceException;
import org.b3log.solo.util.freemarker.Templates;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Article/Page permalink filter.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.0.1.7, Jan 8, 2013
 * @since 0.3.1
 * @see org.b3log.solo.controller.ArticleController#showArticle(org.b3log.solo.frame.servlet.HTTPRequestContext,
 *      javax.servlet.http.HttpServletRequest,
 *      javax.servlet.http.HttpServletResponse)
 * @see org.b3log.solo.controller.PageProcessor#showPage(org.b3log.solo.frame.servlet.HTTPRequestContext)
 */
@Component
public final class PermalinkFilter implements Filter {
	@Autowired
	private ArticleDao articleDao;
	@Autowired
	private PageDao pageDao;
	@Autowired
	private ArticleQueryService articleQueryService;
	@Autowired
	private PreferenceQueryService preferenceQueryService;
	/**
	 * Logger.
	 */
	private static Logger logger = LoggerFactory.getLogger(PermalinkFilter.class);

	@Override
	public void init(final FilterConfig filterConfig) throws ServletException {
	}

	/**
	 * Tries to dispatch request to article processor.
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
		final HttpServletRequest httpServletRequest = (HttpServletRequest) request;
		final HttpServletResponse httpServletResponse = (HttpServletResponse) response;

		try {
			JSONObject preference = preferenceQueryService.getPreference();
			// https://github.com/b3log/solo/issues/12060
			String specifiedSkin = Skins.getSkinDirName(httpServletRequest);
			if (null != specifiedSkin) {
				if ("default".equals(specifiedSkin)) {
					specifiedSkin = preference.optString(Option.ID_C_SKIN_DIR_NAME);
				}
			} else {
				specifiedSkin = preference.optString(Option.ID_C_SKIN_DIR_NAME);
			}
			request.setAttribute(Keys.TEMAPLTE_DIR_NAME, specifiedSkin);
		} catch (ServiceException e1) {
			e1.printStackTrace();
		}

		final String requestURI = httpServletRequest.getRequestURI();

		logger.debug("Request URI[{}]", requestURI);

		final String contextPath = Latkes.getContextPath();
		final String permalink = StringUtils.substringAfter(requestURI, contextPath);

		if (PermalinkQueryService.invalidPermalinkFormat(permalink)) {
			logger.debug("Skip filter request[URI={}]", permalink);
			chain.doFilter(request, response);

			return;
		}

		JSONObject article;
		JSONObject page = null;

		try {

			article = articleDao.getByPermalink(permalink);

			if (null == article) {
				page = pageDao.getByPermalink(permalink);
			}

			if (null == page && null == article) {
				logger.debug("Not found article/page with permalink[{}]", permalink);
				chain.doFilter(request, response);

				return;
			}
		} catch (final RepositoryException e) {
			logger.error("Processes article permalink filter failed", e);
			httpServletResponse.sendError(HttpServletResponse.SC_NOT_FOUND);

			return;
		}

		// If requests an article and the article need view passowrd, sends
		// redirect to the password form
		if (null != article && articleQueryService.needViewPwd(httpServletRequest, article)) {
			try {
				httpServletResponse.sendRedirect(
						Latkes.getServePath() + "/console/article-pwd?articleId=" + article.optString(Keys.OBJECT_ID));
				return;
			} catch (final Exception e) {
				httpServletResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}
		}

		dispatchToArticleOrPageProcessor(chain, request, response, article, page);
	}

	/**
	 * Dispatches the specified request to the specified article or page
	 * processor with the specified response.
	 *
	 * @param request
	 *            the specified request
	 * @param response
	 *            the specified response
	 * @param article
	 *            the specified article
	 * @param page
	 *            the specified page
	 * @throws ServletException
	 *             servlet exception
	 * @throws IOException
	 *             io exception
	 * @see HTTPRequestDispatcher#dispatch(org.b3log.solo.frame.servlet.HTTPRequestContext)
	 */
	private void dispatchToArticleOrPageProcessor(FilterChain chain, final ServletRequest request,
			final ServletResponse response, final JSONObject article, final JSONObject page)
			throws ServletException, IOException {

		request.setAttribute(Keys.HttpRequest.REQUEST_METHOD, RequestMethod.GET.name());
		if (null != article) {
			request.setAttribute(Article.ARTICLE, article);
			request.setAttribute(Keys.HttpRequest.REQUEST_URI, "/article");
			request.getRequestDispatcher("/article").forward(request, response);
		} else {
			request.setAttribute(Page.PAGE, page);
			request.setAttribute(Keys.HttpRequest.REQUEST_URI, "/page");
			request.getRequestDispatcher("/page").forward(request, response);
		}

		// chain.doFilter(request, response);

		/*
		 * final HttpControl httpControl = new
		 * HttpControl(DispatcherServlet.SYS_HANDLER.iterator(), context);
		 *
		 * try { httpControl.nextHandler(); } catch (final Exception e) {
		 * context.setRenderer(new HTTP500Renderer(e)); }
		 *
		 * DispatcherServlet.result(context);
		 */
	}

	@Override
	public void destroy() {
	}
}