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
package org.b3log.solo.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.b3log.solo.renderer.DoNothingRenderer;
import org.b3log.solo.service.StatisticMgmtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Statistics processor.
 *
 * <p>
 * Statistics of Solo:
 *
 * <ul>
 * <li>{@link #viewCounter(org.b3log.solo.frame.servlet.HTTPRequestContext)
 * Blog/Article view counting}</li>
 * </ul>
 * <p>
 *
 * @author <a href="http://cxy7.com">XyCai</a>
 * @version 1.0.2.0, Oct 12, 2013
 * @since 0.4.0
 */
@Controller
public class StatProcessor {

	/**
	 * Logger.
	 */
	private static Logger logger = LoggerFactory.getLogger(StatProcessor.class);

	/**
	 * Online visitor count refresher.
	 * 
	 * @param context
	 *            the specified context
	 */
	@RequestMapping(value = "/console/stat/onlineVisitorRefresh", method = RequestMethod.GET)
	public void onlineVisitorCountRefresher(final HttpServletRequest request, final HttpServletResponse response) {
		StatisticMgmtService.removeExpiredOnlineVisitor();
		new DoNothingRenderer().render(request, response);
	}
}
