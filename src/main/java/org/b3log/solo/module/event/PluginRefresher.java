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
package org.b3log.solo.module.event;


import java.util.List;

import org.b3log.solo.frame.event.Event;
import org.b3log.solo.frame.event.EventException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.b3log.solo.frame.repository.Transaction;
import org.b3log.solo.dao.PluginDao;
import org.b3log.solo.module.plugin.PluginManager;
import org.b3log.solo.service.PluginMgmtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * This listener is responsible for refreshing plugin after every loaded.
 * 
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.0.0.1, Nov 28, 2011
 * @since 0.3.1
 */
@Component
public final class PluginRefresher {
	@Autowired
	private PluginDao pluginDao;
	@Autowired
	private PluginMgmtService pluginMgmtService;
    /**
     * Logger.
     */
    private static Logger logger = LoggerFactory.getLogger(PluginRefresher.class);

    public void action(final Event<List<AbstractPlugin>> event) throws EventException {
        final List<AbstractPlugin> plugins = event.getData();

        logger.debug( "Processing an event[type={0}, data={1}] in listener[className={2}]",
                event.getType(), plugins, PluginRefresher.class);
//        final Transaction transaction = pluginDao.beginTransaction();
        
        try {
            pluginMgmtService.refresh(plugins);
//            transaction.commit();
        } catch (final Exception e) {
//            if (transaction.isActive()) {
//                transaction.rollback();
//            }

            logger.error("Processing plugin loaded event error", e);
            throw new EventException(e);
        }
    }

    /**
     * Gets the event type {@linkplain PluginManager#PLUGIN_LOADED_EVENT}.
     * 
     * @return event type
     */
    
    public String getEventType() {
        return PluginManager.PLUGIN_LOADED_EVENT;
    }
}
