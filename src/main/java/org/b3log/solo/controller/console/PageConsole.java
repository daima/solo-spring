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
import org.b3log.solo.model.Common;
import org.b3log.solo.model.Page;
import org.b3log.solo.module.util.QueryResults;
import org.b3log.solo.renderer.JSONRenderer;
import org.b3log.solo.service.LangPropsService;
import org.b3log.solo.service.PageMgmtService;
import org.b3log.solo.service.PageQueryService;
import org.b3log.solo.service.ServiceException;
import org.b3log.solo.service.UserQueryService;
import org.b3log.solo.util.Requests;
import org.json.JSONArray;
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
 * Plugin console request processing.
 *
 * @author <a href="http://cxy7.com">XyCai</a>
 * @version 1.0.0.2, Aug 9, 2012
 * @since 0.4.0
 */
@Controller
public class PageConsole {

	/**
	 * Logger.
	 */
	private static Logger logger = LoggerFactory.getLogger(PageConsole.class);

	/**
	 * User query service.
	 */
	@Autowired
	private UserQueryService userQueryService;

	/**
	 * Page query service.
	 */
	@Autowired
	private PageQueryService pageQueryService;

	/**
	 * Page management service.
	 */
	@Autowired
	private PageMgmtService pageMgmtService;

	/**
	 * Language service.
	 */
	@Autowired
	private LangPropsService langPropsService;

	/**
	 * Updates a page by the specified request.
	 * 
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
	 *            the specified http servlet request, for example,
	 * 
	 *            <pre>
	 * {
	 *     "page": {
	 *         "oId": "",
	 *         "pageTitle": "",
	 *         "pageContent": "",
	 *         "pageOrder": int,
	 *         "pageCommentCount": int,
	 *         "pagePermalink": "",
	 *         "pageCommentable": boolean,
	 *         "pageType": "",
	 *         "pageOpenTarget": ""
	 *     }
	 * }, see {@link org.b3log.solo.model.Page} for more details
	 *            </pre>
	 * 
	 * @param response
	 *            the specified http servlet response
	 * @param context
	 *            the specified http request context
	 * @throws Exception
	 *             exception
	 */
	@RequestMapping(value = "/console/page/", method = RequestMethod.PUT)
	public void updatePage(final HttpServletRequest request, final HttpServletResponse response, @RequestParam String body) throws Exception {
		if (!userQueryService.isAdminLoggedIn(request)) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
			return;
		}

		final JSONRenderer renderer = new JSONRenderer();
		final JSONObject ret = new JSONObject();
		try {
			body = URLDecoder.decode(body, "UTF-8");final JSONObject requestJSONObject = new JSONObject(body);

			pageMgmtService.updatePage(requestJSONObject);

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
	 * Removes a page by the specified request.
	 * 
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
	@RequestMapping(value = "/console/page/*", method = RequestMethod.DELETE)
	public void removePage(final HttpServletRequest request, final HttpServletResponse response) throws Exception {
		if (!userQueryService.isAdminLoggedIn(request)) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
			return;
		}

		final JSONRenderer renderer = new JSONRenderer();
		final JSONObject jsonObject = new JSONObject();
		renderer.setJSONObject(jsonObject);

		try {
			final String pageId = request.getRequestURI()
					.substring((Latkes.getContextPath() + "/console/page/").length());

			pageMgmtService.removePage(pageId);

			jsonObject.put(Keys.STATUS_CODE, true);
			jsonObject.put(Keys.MSG, langPropsService.get("removeSuccLabel"));
		} catch (final Exception e) {
			logger.error(e.getMessage(), e);

			jsonObject.put(Keys.STATUS_CODE, false);
			jsonObject.put(Keys.MSG, langPropsService.get("removeFailLabel"));

		}
		renderer.render(request, response);
	}

	/**
	 * Adds a page with the specified request.
	 * 
	 * <p>
	 * Renders the response with a json object, for example,
	 * 
	 * <pre>
	 * {
	 *     "sc": boolean,
	 *     "oId": "", // Generated page id
	 *     "msg": ""
	 * }
	 * </pre>
	 * </p>
	 * 
	 * @param context
	 *            the specified http request context
	 * @param request
	 *            the specified http servlet request, for example,
	 * 
	 *            <pre>
	 * {
	 *     "page": {
	 *         "pageTitle": "",
	 *         "pageContent": "",
	 *         "pagePermalink": "" // optional,
	 *         "pageCommentable": boolean,
	 *         "pageType": "",
	 *         "pageOpenTarget": ""
	 *     }
	 * }, see {@link org.b3log.solo.model.Page} for more details
	 *            </pre>
	 * 
	 * @param response
	 *            the specified http servlet response
	 * @throws Exception
	 *             exception
	 */
	@RequestMapping(value = "/console/page/", method = RequestMethod.POST)
	public void addPage(final HttpServletRequest request, final HttpServletResponse response, @RequestBody String body)
			throws Exception {
		if (!userQueryService.isAdminLoggedIn(request)) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
			return;
		}

		final JSONRenderer renderer = new JSONRenderer();
		final JSONObject ret = new JSONObject();

		try {
			body = URLDecoder.decode(body, "UTF-8");
			final JSONObject requestJSONObject = new JSONObject(body);

			final String pageId = pageMgmtService.addPage(requestJSONObject);

			ret.put(Keys.OBJECT_ID, pageId);
			ret.put(Keys.MSG, langPropsService.get("addSuccLabel"));
			ret.put(Keys.STATUS_CODE, true);

			renderer.setJSONObject(ret);
		} catch (final ServiceException e) { // May be permalink check exception
			logger.warn(e.getMessage(), e);

			final JSONObject jsonObject = QueryResults.defaultResult();

			renderer.setJSONObject(jsonObject);
			jsonObject.put(Keys.MSG, e.getMessage());
		}
		renderer.render(request, response);
	}

	/**
	 * Changes a page order by the specified page id and direction.
	 * 
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
	 *            the specified http servlet request, for example,
	 * 
	 *            <pre>
	 * {
	 *     "oId": "",
	 *     "direction": "" // "up"/"down"
	 * }
	 *            </pre>
	 * 
	 * @param response
	 *            the specified http servlet response
	 * @param context
	 *            the specified http request context
	 * @throws Exception
	 *             exception
	 */
	@RequestMapping(value = "/console/page/order/", method = RequestMethod.PUT)
	public void changeOrder(final HttpServletRequest request, final HttpServletResponse response, @RequestParam String body) throws Exception {
		if (!userQueryService.isAdminLoggedIn(request)) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
			return;
		}

		final JSONRenderer renderer = new JSONRenderer();
		final JSONObject ret = new JSONObject();
		try {
			body = URLDecoder.decode(body, "UTF-8");final JSONObject requestJSONObject = new JSONObject(body);
			final String linkId = requestJSONObject.getString(Keys.OBJECT_ID);
			final String direction = requestJSONObject.getString(Common.DIRECTION);

			pageMgmtService.changeOrder(linkId, direction);

			ret.put(Keys.STATUS_CODE, true);
			ret.put(Keys.MSG, langPropsService.get("updateSuccLabel"));

			renderer.setJSONObject(ret);
		} catch (final Exception e) {
			logger.error(e.getMessage(), e);

			final JSONObject jsonObject = QueryResults.defaultResult();

			renderer.setJSONObject(jsonObject);
			jsonObject.put(Keys.MSG, langPropsService.get("updateFailLabel"));
		}
		renderer.render(request, response);
	}

	/**
	 * Gets a page by the specified request.
	 * 
	 * <p>
	 * Renders the response with a json object, for example,
	 * 
	 * <pre>
	 * {
	 *     "sc": boolean
	 *     "page": {
	 *         "oId": "",
	 *         "pageTitle": "",
	 *         "pageContent": ""
	 *         "pageOrder": int,
	 *         "pagePermalink": "",
	 *         "pageCommentCount": int,
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
	@RequestMapping(value = "/console/page/*", method = RequestMethod.GET)
	public void getPage(final HttpServletRequest request, final HttpServletResponse response) throws Exception {
		if (!userQueryService.isLoggedIn(request, response)) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
			return;
		}

		final JSONRenderer renderer = new JSONRenderer();

		try {
			final String requestURI = request.getRequestURI();
			final String pageId = requestURI.substring((Latkes.getContextPath() + "/console/page/").length());

			final JSONObject result = pageQueryService.getPage(pageId);

			if (null == result) {
				renderer.setJSONObject(QueryResults.defaultResult());
				renderer.render(request, response);
				return;
			}

			renderer.setJSONObject(result);
			result.put(Keys.STATUS_CODE, true);
			result.put(Keys.MSG, langPropsService.get("getSuccLabel"));
		} catch (final Exception e) {
			logger.error(e.getMessage(), e);

			final JSONObject jsonObject = QueryResults.defaultResult();

			renderer.setJSONObject(jsonObject);
			jsonObject.put(Keys.MSG, langPropsService.get("getFailLabel"));
		}
		renderer.render(request, response);
	}

	/**
	 * Gets pages by the specified request.
	 * 
	 * <p>
	 * Renders the response with a json object, for example,
	 * 
	 * <pre>
	 * {
	 *     "pagination": {
	 *         "paginationPageCount": 100,
	 *         "paginationPageNums": [1, 2, 3, 4, 5]
	 *     },
	 *     "pages": [{
	 *         "oId": "",
	 *         "pageTitle": "",
	 *         "pageCommentCount": int,
	 *         "pageOrder": int,
	 *         "pagePermalink": ""
	 *      }, ....]
	 *     "sc": "GET_PAGES_SUCC"
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
	 * @see Requests#PAGINATION_PATH_PATTERN
	 */
	@RequestMapping(value = "/console/pages/*/*/*"/*
													 * Requests.
													 * PAGINATION_PATH_PATTERN
													 */, method = RequestMethod.GET)
	public void getPages(final HttpServletRequest request, final HttpServletResponse response) throws Exception {
		if (!userQueryService.isLoggedIn(request, response)) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
			return;
		}

		final JSONRenderer renderer = new JSONRenderer();
		try {
			final String requestURI = request.getRequestURI();
			final String path = requestURI.substring((Latkes.getContextPath() + "/console/pages/").length());

			final JSONObject requestJSONObject = Requests.buildPaginationRequest(path);

			final JSONObject result = pageQueryService.getPages(requestJSONObject);

			final JSONArray pages = result.optJSONArray(Page.PAGES);

			// Site-internal URLs process
			for (int i = 0; i < pages.length(); i++) {
				final JSONObject page = pages.getJSONObject(i);

				if ("page".equals(page.optString(Page.PAGE_TYPE))) {
					final String permalink = page.optString(Page.PAGE_PERMALINK);

					page.put(Page.PAGE_PERMALINK, Latkes.getServePath() + permalink);
				}
			}

			result.put(Keys.STATUS_CODE, true);

			renderer.setJSONObject(result);
		} catch (final Exception e) {
			logger.error(e.getMessage(), e);

			final JSONObject jsonObject = QueryResults.defaultResult();

			renderer.setJSONObject(jsonObject);
			jsonObject.put(Keys.MSG, langPropsService.get("getFailLabel"));
		}
		renderer.render(request, response);
	}
}
