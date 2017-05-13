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
package org.b3log.solo.frame.urlfetch.local;

import org.springframework.web.bind.annotation.RequestMethod;

/**
 * UrlFetchHandlerFactory to get an UrlFetchHandler.
 *
 * @author <a href="mailto:wmainlove@gmail.com">Love Yao</a>
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.0.0.4, Oct 12, 2012
 */
final class UrlFetchHandlerFactory {

	/**
	 * 
	 * @param requestMethod
	 *            {@link HTTPRequestMethod}
	 * @return {@link UrlFetchCommonHandler}
	 */
	public static UrlFetchCommonHandler getFetchHandler(final RequestMethod requestMethod) {
		UrlFetchCommonHandler ret = null;

		/*
		 * now just Distinguish POST and the others.
		 */
		switch (requestMethod) {
		case POST:
			ret = new UrlFetchPostHandler();
			break;

		case PUT:
			ret = new UrlFetchPutHandler();
			break;

		default:
			ret = new UrlFetchCommonHandler();
			break;
		}

		return ret;
	}

	/**
	 * Private construction method.
	 */
	private UrlFetchHandlerFactory() {
	}
}
