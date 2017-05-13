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
package org.b3log.solo.controller.console;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.b3log.solo.Keys;
import org.b3log.solo.Latkes;
import org.b3log.solo.model.Common;
import org.b3log.solo.module.util.QueryResults;
import org.b3log.solo.renderer.JSONRenderer;
import org.b3log.solo.service.LangPropsService;
import org.b3log.solo.service.LinkMgmtService;
import org.b3log.solo.service.LinkQueryService;
import org.b3log.solo.service.UserQueryService;
import org.b3log.solo.util.Requests;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Link console request processing.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.0.0.3, Aug 9, 2012
 * @since 0.4.0
 */
@Controller
public class LinkConsole {

	/**
	 * Logger.
	 */
	private static Logger logger = LoggerFactory.getLogger(LinkConsole.class);

	/**
	 * User query service.
	 */
	@Autowired
	private UserQueryService userQueryService;

	/**
	 * Link query service.
	 */
	@Autowired
	private LinkQueryService linkQueryService;

	/**
	 * Link management service.
	 */
	@Autowired
	private LinkMgmtService linkMgmtService;

	/**
	 * Language service.
	 */
	@Autowired
	private LangPropsService langPropsService;

	/**
	 * Removes a link by the specified request.
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
	@RequestMapping(value = "/console/link/*", method = RequestMethod.DELETE)
	public void removeLink(final HttpServletRequest request, final HttpServletResponse response) throws Exception {
		if (!userQueryService.isAdminLoggedIn(request)) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
			return;
		}

		final JSONRenderer renderer = new JSONRenderer();

		//

		final JSONObject jsonObject = new JSONObject();

		renderer.setJSONObject(jsonObject);

		try {
			final String linkId = request.getRequestURI()
					.substring((Latkes.getContextPath() + "/console/link/").length());

			linkMgmtService.removeLink(linkId);

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
	 * Updates a link by the specified request.
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
	 *     "link": {
	 *         "oId": "",
	 *         "linkTitle": "",
	 *         "linkAddress": "",
	 *         "linkDescription": ""
	 *     }
	 * }, see {@link org.b3log.solo.model.Link} for more details
	 *            </pre>
	 *
	 * @param context
	 *            the specified http request context
	 * @param response
	 *            the specified http servlet response
	 * @throws Exception
	 *             exception
	 */
	@RequestMapping(value = "/console/link/", method = RequestMethod.PUT)
	public void updateLink(final HttpServletRequest request, final HttpServletResponse response) throws Exception {
		if (!userQueryService.isAdminLoggedIn(request)) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
			return;
		}

		final JSONRenderer renderer = new JSONRenderer();

		final JSONObject ret = new JSONObject();

		try {
			final JSONObject requestJSONObject = Requests.parseRequestJSONObject(request, response);

			linkMgmtService.updateLink(requestJSONObject);

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
	 * Changes a link order by the specified link id and direction.
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
	@RequestMapping(value = "/console/link/order/", method = RequestMethod.PUT)
	public void changeOrder(final HttpServletRequest request, final HttpServletResponse response) throws Exception {
		if (!userQueryService.isAdminLoggedIn(request)) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
			return;
		}

		final JSONRenderer renderer = new JSONRenderer();

		final JSONObject ret = new JSONObject();

		try {
			final JSONObject requestJSONObject = Requests.parseRequestJSONObject(request, response);
			final String linkId = requestJSONObject.getString(Keys.OBJECT_ID);
			final String direction = requestJSONObject.getString(Common.DIRECTION);

			linkMgmtService.changeOrder(linkId, direction);

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
	 * Adds a link with the specified request.
	 *
	 * <p>
	 * Renders the response with a json object, for example,
	 *
	 * <pre>
	 * {
	 *     "sc": boolean,
	 *     "oId": "", // Generated link id
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
	 *     "link": {
	 *         "linkTitle": "",
	 *         "linkAddress": "",
	 *         "linkDescription": ""
	 *     }
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
	@RequestMapping(value = "/console/link/", method = RequestMethod.POST)
	public void addLink(final HttpServletRequest request, final HttpServletResponse response) throws Exception {
		if (!userQueryService.isAdminLoggedIn(request)) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
			return;
		}

		final JSONRenderer renderer = new JSONRenderer();

		final JSONObject ret = new JSONObject();

		try {
			final JSONObject requestJSONObject = Requests.parseRequestJSONObject(request, response);

			final String linkId = linkMgmtService.addLink(requestJSONObject);

			ret.put(Keys.OBJECT_ID, linkId);
			ret.put(Keys.MSG, langPropsService.get("addSuccLabel"));
			ret.put(Keys.STATUS_CODE, true);

			renderer.setJSONObject(ret);
		} catch (final Exception e) {
			logger.error(e.getMessage(), e);

			final JSONObject jsonObject = QueryResults.defaultResult();

			renderer.setJSONObject(jsonObject);
			jsonObject.put(Keys.MSG, langPropsService.get("addFailLabel"));
		}
		renderer.render(request, response);
	}

	/**
	 * Gets links by the specified request.
	 *
	 * <p>
	 * The request URI contains the pagination arguments. For example, the
	 * request URI is /console/links/1/10/20, means the current page is 1, the
	 * page size is 10, and the window size is 20.
	 * </p>
	 *
	 * <p>
	 * Renders the response with a json object, for example,
	 *
	 * <pre>
	 * {
	 *     "sc": boolean,
	 *     "pagination": {
	 *         "paginationPageCount": 100,
	 *         "paginationPageNums": [1, 2, 3, 4, 5]
	 *     },
	 *     "links": [{
	 *         "oId": "",
	 *         "linkTitle": "",
	 *         "linkAddress": "",
	 *         "linkDescription": ""
	 *      }, ....]
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
	@RequestMapping(value = "/console/links/*/*/*"/*
													 * Requests.
													 * PAGINATION_PATH_PATTERN
													 */, method = RequestMethod.GET)
	public void getLinks(final HttpServletRequest request, final HttpServletResponse response) throws Exception {
		if (!userQueryService.isLoggedIn(request, response)) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
			return;
		}

		final JSONRenderer renderer = new JSONRenderer();
		try {
			final String requestURI = request.getRequestURI();
			final String path = requestURI.substring((Latkes.getContextPath() + "/console/links/").length());

			final JSONObject requestJSONObject = Requests.buildPaginationRequest(path);

			final JSONObject result = linkQueryService.getLinks(requestJSONObject);

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

	/**
	 * Gets the file with the specified request.
	 *
	 * <p>
	 * Renders the response with a json object, for example,
	 *
	 * <pre>
	 * {
	 *     "sc": boolean,
	 *     "link": {
	 *         "oId": "",
	 *         "linkTitle": "",
	 *         "linkAddress": "",
	 *         "linkDescription": ""
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
	@RequestMapping(value = "/console/link/*", method = RequestMethod.GET)
	public void getLink(final HttpServletRequest request, final HttpServletResponse response) throws Exception {
		if (!userQueryService.isLoggedIn(request, response)) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
			return;
		}

		final JSONRenderer renderer = new JSONRenderer();

		try {
			final String requestURI = request.getRequestURI();
			final String linkId = requestURI.substring((Latkes.getContextPath() + "/console/link/").length());

			final JSONObject result = linkQueryService.getLink(linkId);

			if (null == result) {
				renderer.setJSONObject(QueryResults.defaultResult());
				renderer.render(request, response);
				return;
			}

			renderer.setJSONObject(result);
			result.put(Keys.STATUS_CODE, true);
		} catch (final Exception e) {
			logger.error(e.getMessage(), e);

			final JSONObject jsonObject = QueryResults.defaultResult();

			renderer.setJSONObject(jsonObject);
			jsonObject.put(Keys.MSG, langPropsService.get("getFailLabel"));
		}
		renderer.render(request, response);
	}
}
