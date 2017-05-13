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
package org.b3log.solo.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.b3log.solo.SoloConstant;
import org.b3log.solo.model.Pagination;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Request utilities.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @author <a href="mailto:dongxv.vang@gmail.com">Dongxu Wang</a>
 * @version 1.1.3.2, Jan 6, 2017
 * @see #PAGINATION_PATH_PATTERN
 */
public final class Requests {

	/**
	 * Logger.
	 */
	private static Logger logger = LoggerFactory.getLogger(Requests.class);

	/**
	 * The pagination path pattern.
	 * <p>
	 * The first star represents "the current page number", the second star
	 * represents "the page size", and the third star represents "the window
	 * size". Argument of each of these stars should be a number.
	 * </p>
	 * <p>
	 * For example, the request URI is "xxx/1/2/3", so the specified path is
	 * "1/2/3". The first number represents "the current page number", the
	 * second number represents "the page size", and the third number represents
	 * "the window size", all of these for pagination.
	 * </p>
	 */
	public static final String PAGINATION_PATH_PATTERN = "*/*/*";

	/**
	 * Default page size.
	 */
	private static final int DEFAULT_PAGE_SIZE = 15;

	/**
	 * Default window size.
	 */
	private static final int DEFAULT_WINDOW_SIZE = 20;

	/**
	 * HTTP header "User-Agent" pattern for mobile device requests.
	 */
	private static final Pattern MOBILE_USER_AGENT_PATTERN = Pattern.compile(
			"android.+mobile|avantgo|bada|blackberry|blazer|compal|elaine|fennec"
					+ "|hiptop|iemobile|ip(hone|od)|iris|kindle|lge|maemo|midp|mmp|opera m(ob|in)i"
					+ "|palm( os)?|phone|p(ixi|re)|plucker|pocket|psp|symbian|treo|up.(browser"
					+ "|link)|ucweb|vodafone|wap|webos|windows (ce|phone)|xda|xiino|htc|MQQBrowser",
			Pattern.CASE_INSENSITIVE);

	/**
	 * HTTP header "User-Agent" pattern for search engine bot requests.
	 */
	private static final Pattern SEARCH_ENGINE_BOT_USER_AGENT_PATTERN = Pattern.compile("spider|bot|fetcher|crawler"
			+ "|google|yahoo|sogou|youdao|Xianguo.com|RssBandit|JianKongBao Monitor|BAE Online Platform" + "|B3log",
			Pattern.CASE_INSENSITIVE);

	/**
	 * Cookie expiry of "visited".
	 */
	private static final int COOKIE_EXPIRY = 60 * 60 * 24; // 24 hours

	/**
	 * Gets log of the specified request.
	 *
	 * @param httpServletRequest
	 *            the specified request
	 * @return log
	 */
	public static String getLog(final HttpServletRequest httpServletRequest) {
		final String indents = "    ";
		final StringBuilder logBuilder = new StringBuilder("Request [").append(SoloConstant.LINE_SEPARATOR);

		logBuilder.append(indents).append("method=").append(httpServletRequest.getMethod()).append(",")
				.append(SoloConstant.LINE_SEPARATOR);
		logBuilder.append(indents).append("URL=").append(httpServletRequest.getRequestURL()).append(",")
				.append(SoloConstant.LINE_SEPARATOR);
		logBuilder.append(indents).append("contentType=").append(httpServletRequest.getContentType()).append(",")
				.append(SoloConstant.LINE_SEPARATOR);
		logBuilder.append(indents).append("characterEncoding=").append(httpServletRequest.getCharacterEncoding())
				.append(",").append(SoloConstant.LINE_SEPARATOR);
		logBuilder.append(indents).append("local=[").append(SoloConstant.LINE_SEPARATOR);
		logBuilder.append(indents).append(indents).append("addr=").append(httpServletRequest.getLocalAddr()).append(",")
				.append(SoloConstant.LINE_SEPARATOR);
		logBuilder.append(indents).append(indents).append("port=").append(httpServletRequest.getLocalPort()).append(",")
				.append(SoloConstant.LINE_SEPARATOR);
		logBuilder.append(indents).append(indents).append("name=").append(httpServletRequest.getLocalName())
				.append("],").append(SoloConstant.LINE_SEPARATOR);
		logBuilder.append(indents).append("remote=[").append(SoloConstant.LINE_SEPARATOR);
		logBuilder.append(indents).append(indents).append("addr=").append(getRemoteAddr(httpServletRequest)).append(",")
				.append(SoloConstant.LINE_SEPARATOR);
		logBuilder.append(indents).append(indents).append("port=").append(httpServletRequest.getRemotePort())
				.append(",").append(SoloConstant.LINE_SEPARATOR);
		logBuilder.append(indents).append(indents).append("host=").append(httpServletRequest.getRemoteHost())
				.append("],").append(SoloConstant.LINE_SEPARATOR);
		logBuilder.append(indents).append("headers=[").append(SoloConstant.LINE_SEPARATOR);

		final StringBuilder headerLogBuilder = new StringBuilder();
		final Enumeration<String> headerNames = httpServletRequest.getHeaderNames();

		while (headerNames.hasMoreElements()) {
			final String name = headerNames.nextElement();
			final String value = httpServletRequest.getHeader(name);

			headerLogBuilder.append(indents).append(indents).append(name).append("=").append(value);

			headerLogBuilder.append(SoloConstant.LINE_SEPARATOR);
		}
		headerLogBuilder.append(indents).append("]");

		logBuilder.append(headerLogBuilder.toString()).append(SoloConstant.LINE_SEPARATOR).append("]");

		return logBuilder.toString();
	}

	/**
	 * Gets the Internet Protocol (IP) address of the end-client that sent the
	 * specified request.
	 * <p>
	 * It will try to get HTTP head "X-forwarded-for" or "X-Real-IP" from the
	 * last proxy to get the request first, if not found, try to get it directly
	 * by {@link HttpServletRequest#getRemoteAddr()}.
	 * </p>
	 *
	 * @param request
	 *            the specified request
	 * @return the IP address of the end-client sent the specified request
	 */
	public static String getRemoteAddr(final HttpServletRequest request) {
		String ret = request.getHeader("X-forwarded-for");

		if (StringUtils.isBlank(ret)) {
			ret = request.getHeader("X-Real-IP");
		}

		if (StringUtils.isBlank(ret)) {
			return request.getRemoteAddr();
		}

		return ret.split(",")[0];
	}

	/**
	 * Mobile and normal skin toggle.
	 *
	 * @param request
	 *            the specified request
	 * @return {@code null} if not set cookie, returns value (mobile | $OTHER)
	 *         of the cookie named "btouch_switch_toggle"
	 */
	public static String mobileSwitchToggle(final HttpServletRequest request) {
		final Cookie[] cookies = request.getCookies();
		String ret = null;

		if (null == cookies || 0 == cookies.length) {
			return ret;
		}

		try {
			for (int i = 0; i < cookies.length; i++) {
				final Cookie cookie = cookies[i];

				if ("btouch_switch_toggle".equals(cookie.getName())) {
					ret = cookie.getValue();
				}
			}
		} catch (final Exception e) {
			logger.error("Parses cookie failed", e);
		}

		return ret;
	}

	/**
	 * Determines whether the specified request dose come from a search engine
	 * bot or not with its header "User-Agent".
	 *
	 * @param request
	 *            the specified request
	 * @return {@code true} if the specified request comes from a search engine
	 *         bot, returns {@code false} otherwise
	 */
	public static boolean searchEngineBotRequest(final HttpServletRequest request) {
		final String userAgent = request.getHeader("User-Agent");

		if (StringUtils.isBlank(userAgent)) {
			return false;
		}

		return SEARCH_ENGINE_BOT_USER_AGENT_PATTERN.matcher(userAgent).find();
	}

	/**
	 * Determines whether the specified request has been served.
	 * <p>
	 * A "served request" is a request a URI as former one. For example, if a
	 * client is request "/test", all requests from the client subsequent in 24
	 * hours will be treated as served requests, requested URIs save in client
	 * cookie (name: "visited").
	 * </p>
	 * <p>
	 * If the specified request has not been served, appends the request URI in
	 * client cookie.
	 * </p>
	 * <p>
	 * Sees this issue (https://github.com/b3log/b3log-solo/issues/44) for more
	 * details.
	 * </p>
	 *
	 * @param request
	 *            the specified request
	 * @param response
	 *            the specified response
	 * @return {@code true} if the specified request has been served, returns
	 *         {@code false} otherwise
	 */
	public static boolean hasBeenServed(final HttpServletRequest request, final HttpServletResponse response) {
		final Cookie[] cookies = request.getCookies();

		if (null == cookies || 0 == cookies.length) {
			return false;
		}

		Cookie cookie;
		boolean needToCreate = true;
		boolean needToAppend = true;
		JSONArray cookieJSONArray = null;

		try {
			for (int i = 0; i < cookies.length; i++) {
				cookie = cookies[i];

				if (!"visited".equals(cookie.getName())) {
					continue;
				}

				cookieJSONArray = new JSONArray(cookie.getValue());
				if (null == cookieJSONArray || 0 == cookieJSONArray.length()) {
					return false;
				}

				needToCreate = false;

				for (int j = 0; j < cookieJSONArray.length(); j++) {
					final String visitedURL = cookieJSONArray.optString(j);

					if (request.getRequestURI().equals(visitedURL)) {
						needToAppend = false;
						return true;
					}
				}
			}

			if (needToCreate) {
				final StringBuilder builder = new StringBuilder("[").append("\"").append(request.getRequestURI())
						.append("\"]");

				final Cookie c = new Cookie("visited", builder.toString());

				c.setMaxAge(COOKIE_EXPIRY);
				c.setPath("/");
				response.addCookie(c);
			} else if (needToAppend) {
				cookieJSONArray.put(request.getRequestURI());

				final Cookie c = new Cookie("visited", cookieJSONArray.toString());

				c.setMaxAge(COOKIE_EXPIRY);
				c.setPath("/");
				response.addCookie(c);
			}
		} catch (final Exception e) {
			logger.warn("Parses cookie failed, clears the cookie[name=visited]", e);

			final Cookie c = new Cookie("visited", null);

			c.setMaxAge(0);
			c.setPath("/");

			response.addCookie(c);
		}

		return false;
	}

	/**
	 * Determines whether the specified request dose come from mobile device or
	 * not with its header "User-Agent".
	 *
	 * @param request
	 *            the specified request
	 * @return {@code true} if the specified request comes from mobile device,
	 *         returns {@code false} otherwise
	 */
	public static boolean mobileRequest(final HttpServletRequest request) {
		final String userAgent = request.getHeader("User-Agent");

		if (StringUtils.isBlank(userAgent)) {
			return false;
		}

		return MOBILE_USER_AGENT_PATTERN.matcher(userAgent).find();
	}

	/**
	 * Builds pagination request with the specified path.
	 *
	 * @param path
	 *            the specified path, see {@link #PAGINATION_PATH_PATTERN} for
	 *            the details
	 * @return pagination request json object, for example,
	 * 
	 *         <pre>
	 * {
	 *     "paginationCurrentPageNum": int,
	 *     "paginationPageSize": int,
	 *     "paginationWindowSize": int
	 * }
	 *         </pre>
	 * 
	 * @see #PAGINATION_PATH_PATTERN
	 */
	public static JSONObject buildPaginationRequest(final String path) {
		final Integer currentPageNum = getCurrentPageNum(path);
		final Integer pageSize = getPageSize(path);
		final Integer windowSize = getWindowSize(path);

		final JSONObject ret = new JSONObject();

		ret.put(Pagination.PAGINATION_CURRENT_PAGE_NUM, currentPageNum);
		ret.put(Pagination.PAGINATION_PAGE_SIZE, pageSize);
		ret.put(Pagination.PAGINATION_WINDOW_SIZE, windowSize);

		return ret;
	}

	/**
	 * Gets the request page number from the specified path.
	 *
	 * @param path
	 *            the specified path, see {@link #PAGINATION_PATH_PATTERN} for
	 *            the details
	 * @return page number, returns {@code 1} if the specified request URI can
	 *         not convert to an number
	 * @see #PAGINATION_PATH_PATTERN
	 */
	public static int getCurrentPageNum(final String path) {
		logger.trace("Getting current page number[path={0}]", path);

		if (StringUtils.isBlank(path) || path.equals("/")) {
			return 1;
		}

		final String currentPageNumber = path.split("/")[0];

		if (!NumberUtils.isDigits(currentPageNumber)) {
			return 1;
		}

		return Integer.valueOf(currentPageNumber);
	}

	/**
	 * Gets the request page size from the specified path.
	 *
	 * @param path
	 *            the specified path, see {@link #PAGINATION_PATH_PATTERN} for
	 *            the details
	 * @return page number, returns {@value #DEFAULT_PAGE_SIZE} if the specified
	 *         request URI can not convert to an number
	 * @see #PAGINATION_PATH_PATTERN
	 */
	public static int getPageSize(final String path) {
		logger.trace("Page number[string={0}]", path);

		if (StringUtils.isBlank(path)) {
			return DEFAULT_PAGE_SIZE;
		}

		final String[] parts = path.split("/");

		if (1 >= parts.length) {
			return DEFAULT_PAGE_SIZE;
		}

		final String pageSize = parts[1];

		if (!NumberUtils.isDigits(pageSize)) {
			return DEFAULT_PAGE_SIZE;
		}

		return Integer.valueOf(pageSize);
	}

	/**
	 * Gets the request window size from the specified path.
	 *
	 * @param path
	 *            the specified path, see {@link #PAGINATION_PATH_PATTERN} for
	 *            the details
	 * @return page number, returns {@value #DEFAULT_WINDOW_SIZE} if the
	 *         specified request URI can not convert to an number
	 * @see #PAGINATION_PATH_PATTERN
	 */
	public static int getWindowSize(final String path) {
		logger.trace("Page number[string={0}]", path);

		if (StringUtils.isBlank(path)) {
			return DEFAULT_WINDOW_SIZE;
		}

		final String[] parts = path.split("/");

		if (2 >= parts.length) {
			return DEFAULT_WINDOW_SIZE;
		}

		final String windowSize = parts[2];

		if (!NumberUtils.isDigits(windowSize)) {
			return DEFAULT_WINDOW_SIZE;
		}

		return Integer.valueOf(windowSize);
	}

	/**
	 * Gets the request json object with the specified request.
	 *
	 * @param request
	 *            the specified request
	 * @param response
	 *            the specified response, sets its content type with
	 *            "application/json"
	 * @return a json object
	 * @throws ServletException
	 *             servlet exception
	 * @throws IOException
	 *             io exception
	 */
	public static JSONObject parseRequestJSONObject(final HttpServletRequest request,
			final HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("application/json");

		final StringBuilder sb = new StringBuilder();
		BufferedReader reader;

		final String errMsg = "Can not parse request[requestURI=" + request.getRequestURI() + ", method="
				+ request.getMethod() + "], returns an empty json object";

		try {
			try {
				reader = request.getReader();
			} catch (final IllegalStateException illegalStateException) {
				reader = new BufferedReader(new InputStreamReader(request.getInputStream()));
			}

			String line = reader.readLine();

			while (null != line) {
				sb.append(line);
				line = reader.readLine();
			}
			reader.close();

			String tmp = sb.toString();

			if (StringUtils.isBlank(tmp)) {
				tmp = "{}";
			}

			return new JSONObject(tmp);
		} catch (final Exception ex) {
			logger.error(errMsg, ex);

			return new JSONObject();
		}
	}

	/**
	 * Private default constructor.
	 */
	private Requests() {
	}
}
