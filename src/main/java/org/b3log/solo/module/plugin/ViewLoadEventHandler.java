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
package org.b3log.solo.module.plugin;


import java.util.Map;
import java.util.Set;

import org.b3log.solo.Keys;
import org.b3log.solo.frame.event.Event;
import org.b3log.solo.frame.event.EventException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.b3log.solo.frame.plugin.ViewLoadEventData;
import org.b3log.solo.module.event.AbstractPlugin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * FreeMarker view load event handler.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.0.0.3, Aug 9, 2011
 */
@Component
public final class ViewLoadEventHandler {
	
	@Autowired
	private PluginManager pluginManager;
    /**
     * Logger.
     */
    private static Logger logger = LoggerFactory.getLogger(ViewLoadEventHandler.class);

    public String getEventType() {
        return Keys.FREEMARKER_ACTION;
    }

    public void action(final Event<ViewLoadEventData> event) throws EventException {
        final ViewLoadEventData data = event.getData();
        final String viewName = data.getViewName();
        final Map<String, Object> dataModel = data.getDataModel();
        
        final Set<AbstractPlugin> plugins = pluginManager.getPlugins(viewName);

        logger.debug( "Plugin count[{0}] of view[name={1}]", new Object[] {plugins.size(), viewName});
        for (final AbstractPlugin plugin : plugins) {
            switch (plugin.getStatus()) {
            case ENABLED:
                plugin.plug(dataModel);
                logger.debug( "Plugged[name={0}]", plugin.getName());
                break;

            case DISABLED:
                plugin.unplug();
                logger.debug( "Unplugged[name={0}]", plugin.getName());
                break;

            default:
                throw new AssertionError(
                    "Plugin state error, this is a bug! Please report " + "this bug (https://github.com/b3log/b3log-solo/issues/new)!");
            }
        }
    }
}
