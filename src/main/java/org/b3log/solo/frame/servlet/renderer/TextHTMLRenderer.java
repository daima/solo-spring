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
package org.b3log.solo.frame.servlet.renderer;


import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.b3log.solo.frame.logging.Level;
import org.b3log.solo.frame.logging.Logger;


/**
 * HTML HTTP response renderer.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.0.0.0, Sep 11, 2011
 */
public final class TextHTMLRenderer extends AbstractHTTPResponseRenderer {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(TextHTMLRenderer.class.getName());

    /**
     * Content to render.
     */
    private String content;

    /**
     * Sets the content with the specified content.
     * 
     * @param content the specified content
     */
    public void setContent(final String content) {
        this.content = content;
    }

    @Override
    public void render(final HttpServletRequest request, final HttpServletResponse response) {
        try {
            response.setContentType("text/html");
            response.setCharacterEncoding("UTF-8");

            final PrintWriter writer = response.getWriter();

            writer.write(content);
            writer.close();
        } catch (final IOException e) {
            LOGGER.log(Level.ERROR, "Render failed", e);
        }
    }
}
