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


import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.b3log.solo.frame.logging.Level;
import org.b3log.solo.frame.logging.Logger;


/**
 *  servlet forward renderer.
 * 
 * @author <a href="mailto:wmainlove@gmail.com">Love Yao</a>
 * @version 1.0.0.0, Sep 26, 2013
 */
public class StaticFileRenderer extends AbstractHTTPResponseRenderer {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(StaticFileRenderer.class.getName());
    
    /**
     * Request dispatcher.
     */
    private RequestDispatcher requestDispatcher;

    /**
     * requestDispatcher holder.
     * @param requestDispatcher requestDispatcher
     */
    public StaticFileRenderer(final RequestDispatcher requestDispatcher) {
        this.requestDispatcher = requestDispatcher;
    }

    @Override
    public void render(final HttpServletRequest request, final HttpServletResponse response) {

        try {
            requestDispatcher.forward(request, response);
        } catch (final Exception e) {
            LOGGER.log(Level.ERROR, "servlet forward error", e);
            throw new RuntimeException(e);
        }

    }

}
