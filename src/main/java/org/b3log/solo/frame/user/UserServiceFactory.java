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
package org.b3log.solo.frame.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * User service factory.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 2.0.0.3, Jan 8, 2016
 */
@SuppressWarnings("unchecked")
public final class UserServiceFactory {

    /**
     * Logger.
     */
    private static Logger logger = LoggerFactory.getLogger(UserServiceFactory.class);

    /**
     * User service.
     */
    private static final UserService USER_SERVICE;

    static {
        logger.info("Constructing User Service....");

        try {
            Class<UserService> serviceClass = null;

            serviceClass = (Class<UserService>) Class.forName("org.b3log.solo.frame.user.local.LocalUserService");
            USER_SERVICE = serviceClass.newInstance();
        } catch (final Exception e) {
            throw new RuntimeException("Can not initialize User Service!", e);
        }

        logger.info("Constructed User Service");
    }

    /**
     * Gets user service (always be an instance of {@link org.b3log.solo.frame.user.local.LocalUserService}).
     *
     * @return user service
     */
    public static UserService getUserService() {
        return USER_SERVICE;
    }

    /**
     * Private default constructor.
     */
    private UserServiceFactory() {
    }
}
