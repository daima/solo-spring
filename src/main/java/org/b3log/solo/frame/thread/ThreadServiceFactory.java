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
package org.b3log.solo.frame.thread;

import org.b3log.solo.Latkes;
import org.b3log.solo.RuntimeEnv;
import org.b3log.solo.frame.logging.Logger;

/**
 * Thread service factory.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 2.0.0.0, Jan 8, 2016
 */
public final class ThreadServiceFactory {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(ThreadServiceFactory.class.getName());

    /**
     * Thread service.
     */
    private static final ThreadService THREAD_SERVICE;

    static {
        LOGGER.info("Constructing Thread Service....");

        final RuntimeEnv runtimeEnv = Latkes.getRuntimeEnv();

        try {
            Class<ThreadService> serviceClass = null;

            switch (runtimeEnv) {
                case LOCAL:
                	serviceClass = (Class<ThreadService>) Class.forName("org.b3log.solo.frame.thread.local.LocalThreadService");
                	THREAD_SERVICE = serviceClass.newInstance();

                    break;
                default:
                    throw new RuntimeException("Latke runs in the hell.... Please set the enviornment correctly");
            }
        } catch (final Exception e) {
            throw new RuntimeException("Can not initialize Thread Service!", e);
        }

        LOGGER.info("Constructed Thread Service");
    }

    /**
     * Gets thread service.
     *
     * @return thread service
     */
    public static ThreadService getThreadService() {
        return THREAD_SERVICE;
    }

    /**
     * Private default constructor.
     */
    private ThreadServiceFactory() {
    }
}
