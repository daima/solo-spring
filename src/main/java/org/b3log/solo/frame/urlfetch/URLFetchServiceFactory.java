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
package org.b3log.solo.frame.urlfetch;

import org.b3log.solo.Latkes;
import org.b3log.solo.RuntimeEnv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * URL fetch service factory.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 2.0.0.5, Jan 8, 2016
 */
@SuppressWarnings("unchecked")
public final class URLFetchServiceFactory {

    /**
     * Logger.
     */
    private static Logger logger = LoggerFactory.getLogger(URLFetchServiceFactory.class);

    /**
     * URL fetch service.
     */
    private static final URLFetchService URL_FETCH_SERVICE;

    static {
        logger.info("Constructing URL Fetch Service....");

        final RuntimeEnv runtimeEnv = Latkes.getRuntimeEnv();

        try {
            Class<URLFetchService> serviceClass;

            switch (runtimeEnv) {
                case LOCAL:
                	serviceClass = (Class<URLFetchService>) Class.forName("org.b3log.solo.frame.urlfetch.local.LocalURLFetchService");
                	URL_FETCH_SERVICE = serviceClass.newInstance();

                    break;
                default:
                    throw new RuntimeException("Latke runs in the hell.... Please set the enviornment correctly");
            }
        } catch (final Exception e) {
            throw new RuntimeException("Can not initialize URL Fetch Service!", e);
        }

        logger.info("Constructed URL Fetch Service");
    }

    /**
     * Gets URL fetch service.
     *
     * @return URL fetch service
     */
    public static URLFetchService getURLFetchService() {
        return URL_FETCH_SERVICE;
    }

    /**
     * Private default constructor.
     */
    private URLFetchServiceFactory() {
    }
}
