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
package org.b3log.solo.frame.servlet.handler;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.b3log.solo.frame.servlet.HTTPRequestContext;
import org.b3log.solo.frame.servlet.HttpControl;


/**
 * A handler interface for  {@link org.b3log.solo.frame.servlet.DispatcherServlet} to do the inner process.
 *
 * @author <a href="mailto:wmainlove@gmail.com">Love Yao</a>
 * @version 1.0.0.1, Sep 18, 2013
 */
public interface Handler {

    /**
     * Handle.
     * 
     * @param context     {@link HTTPRequestContext}
     * @param httpControl {@link HttpControl}
     * @throws Exception  {@link Exception}
     */
    void handle(final HttpServletRequest request, final HttpServletResponse response, final HttpControl httpControl) throws Exception;
}
