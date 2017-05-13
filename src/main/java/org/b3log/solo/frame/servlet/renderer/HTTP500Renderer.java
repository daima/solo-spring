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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.b3log.solo.frame.logging.Level;
import org.b3log.solo.frame.logging.Logger;
import org.b3log.solo.frame.servlet.HTTPRequestContext;


/**
 * HTTP {@link HttpServletResponse#SC_INTERNAL_SERVER_ERROR status} renderer.
 * 
 * @author <a href="mailto:wmainlove@gmail.com">Love Yao</a>
 * @version 1.0.0.0, Sep 26, 2013
 */
public final class HTTP500Renderer extends AbstractHTTPResponseRenderer {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(HTTP500Renderer.class.getName());

    /**
     * the internal exception.
     */
    private Exception e;

    /**
     * the constructor.
     * 
     * @param e internal exception
     *            
     */
    public HTTP500Renderer(final Exception e) {
        this.e = e;
    }

    @Override
    public void render(final HttpServletRequest request, final HttpServletResponse response) {
        try {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (final IOException e) {
            LOGGER.log(Level.ERROR, "Renders 505 error", e);
        }
    }
}
