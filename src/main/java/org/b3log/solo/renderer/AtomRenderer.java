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
package org.b3log.solo.renderer;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Atom HTTP response renderer.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.0.0.0, Sep 12, 2011
 */
public final class AtomRenderer extends AbstractHTTPResponseRenderer {

	/**
	 * Logger.
	 */
	private static Logger logger = LoggerFactory.getLogger(AtomRenderer.class);

	/**
	 * Content to render.
	 */
	private String content;

	/**
	 * Sets the content with the specified content.
	 * 
	 * @param content
	 *            the specified content
	 */
	public void setContent(final String content) {
		this.content = content;
	}

	@Override
	public void render(final HttpServletRequest request, final HttpServletResponse response) {
		try {
			response.setContentType("application/atom+xml");
			response.setCharacterEncoding("UTF-8");

			final PrintWriter writer = response.getWriter();

			writer.write(content);
			writer.close();
		} catch (final IOException e) {
			logger.error("Render failed", e);
		}
	}
}
