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
package org.b3log.solo.controller.renderer;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.b3log.solo.SoloConstant;
import org.b3log.solo.frame.logging.Logger;
import org.b3log.solo.frame.servlet.renderer.freemarker.AbstractFreeMarkerRenderer;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;

/**
 * <a href="http://freemarker.org">FreeMarker</a> HTTP response renderer for administrator console and initialization
 * rendering.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.0.1.2, Nov 2, 2016
 * @since 0.4.1
 */
public final class ConsoleRenderer extends AbstractFreeMarkerRenderer {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(ConsoleRenderer.class.getName());

    /**
     * FreeMarker configuration.
     */
    public static final Configuration TEMPLATE_CFG;
    private static ServletContext servletContext;

    static {
        TEMPLATE_CFG = new Configuration();
        TEMPLATE_CFG.setDefaultEncoding("UTF-8");

    	try {
    		TEMPLATE_CFG.setDirectoryForTemplateLoading(new File(SoloConstant.TMPLATE_PATH));
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
//        TEMPLATE_CFG.setServletContextForTemplateLoading(servletContext, "/");
        TEMPLATE_CFG.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        TEMPLATE_CFG.setLogTemplateExceptions(false);
    }

    @Override
    protected Template getTemplate(final String templateDirName, final String templateName) {
        try {
            return TEMPLATE_CFG.getTemplate(templateName);
        } catch (final IOException e) {
            return null;
        }
    }

    @Override
    protected void beforeRender(final HttpServletRequest request, final HttpServletResponse response) throws Exception {
    	ServletContext servletContext = request.getServletContext();
    	if (ConsoleRenderer.servletContext == null) {
    		ConsoleRenderer.servletContext = servletContext;
    	}
    }

    @Override
    protected void afterRender(final HttpServletRequest request, final HttpServletResponse response) throws Exception {
    }
}
