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
package org.b3log.solo.frame.mail;

import org.b3log.solo.Latkes;
import org.b3log.solo.RuntimeEnv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mail service factory.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 2.0.1.1, Jan 8, 2016
 */
@SuppressWarnings("unchecked")
public final class MailServiceFactory {

	/**
	 * Logger.
	 */
	private static Logger logger = LoggerFactory.getLogger(MailServiceFactory.class);

	/**
	 * Mail service.
	 */
	private static final MailService MAIL_SERVICE;

	static {
		logger.info("Constructing Mail Service....");

		final RuntimeEnv runtimeEnv = Latkes.getRuntimeEnv();

		try {
			Class<MailService> mailServiceClass;

			switch (runtimeEnv) {
			case LOCAL:
				mailServiceClass = (Class<MailService>) Class
						.forName("org.b3log.solo.frame.mail.local.LocalMailService");
				MAIL_SERVICE = mailServiceClass.newInstance();

				break;
			default:
				throw new RuntimeException("Latke runs in the hell.... Please set the enviornment correctly");
			}
		} catch (final Exception e) {
			throw new RuntimeException("Can not initialize Mail Service!", e);
		}

		logger.info("Constructed Mail Service");
	}

	/**
	 * Gets mail service.
	 *
	 * @return mail service
	 */
	public static MailService getMailService() {
		return MAIL_SERVICE;
	}

	/**
	 * Private default constructor.
	 */
	private MailServiceFactory() {
	}
}
