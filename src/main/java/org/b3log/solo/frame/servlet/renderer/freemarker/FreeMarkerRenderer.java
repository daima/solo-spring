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
package org.b3log.solo.frame.servlet.renderer.freemarker;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * <a href="http://freemarker.org">FreeMarker</a> HTTP response 
 * renderer.
 * 
 * <p>Do nothing after render.</p>
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.0.0.1, Oct 2, 2011
 */
public final class FreeMarkerRenderer extends AbstractFreeMarkerRenderer {

    @Override
    protected void beforeRender(final HttpServletRequest request, final HttpServletResponse response) throws Exception {}

    @Override
    protected void afterRender(final HttpServletRequest request, final HttpServletResponse response) throws Exception {}
}
