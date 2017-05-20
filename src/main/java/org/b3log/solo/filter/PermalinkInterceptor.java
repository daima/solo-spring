package org.b3log.solo.filter;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.b3log.solo.Keys;
import org.b3log.solo.Latkes;
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
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Component
public class PermalinkInterceptor implements HandlerInterceptor {
	private static Logger logger = LoggerFactory.getLogger(PermalinkInterceptor.class);
	@Autowired
	private ArticleDao articleDao;
	@Autowired
	private PageDao pageDao;
	@Autowired
	private ArticleQueryService articleQueryService;
	@Autowired
	private PreferenceQueryService preferenceQueryService;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		try {
			JSONObject preference = preferenceQueryService.getPreference();
			// https://github.com/b3log/solo/issues/12060
			String specifiedSkin = Skins.getSkinDirName(request);
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
		final String requestURI = request.getRequestURI();

		logger.info("Request URI[{}]", requestURI);

		final String contextPath = Latkes.getContextPath();
		final String permalink = StringUtils.substringAfter(requestURI, contextPath);

		if (PermalinkQueryService.invalidPermalinkFormat(permalink)) {
			logger.debug("Skip filter request[URI={}]", permalink);
			return true;
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
				return true;
			}
		} catch (final RepositoryException e) {
			logger.error("Processes article permalink filter failed", e);
			response.sendError(HttpServletResponse.SC_NOT_FOUND);

			return false;
		}

		// If requests an article and the article need view passowrd, sends
		// redirect to the password form
		if (null != article && articleQueryService.needViewPwd(request, article)) {
			try {
				response.sendRedirect(
						Latkes.getServePath() + "/console/article-pwd?articleId=" + article.optString(Keys.OBJECT_ID));
				return true;
			} catch (final Exception e) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
				return false;
			}
		}

		dispatchToArticleOrPageProcessor(request, response, article, page);
		return false;
	}

	private void dispatchToArticleOrPageProcessor(final ServletRequest request, final ServletResponse response,
			final JSONObject article, final JSONObject page) throws ServletException, IOException {

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
	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			ModelAndView modelAndView) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
			throws Exception {
		// TODO Auto-generated method stub

	}

}
