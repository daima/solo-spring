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
package org.b3log.solo.frame.mail.local;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.b3log.solo.frame.mail.MailService;
import org.b3log.solo.frame.mail.MailServiceFactory;

/**
 * Implementation of the {@link MailService} interface.
 * 
 * @author <a href="mailto:jiangzezhou1989@gmail.com">zezhou jiang</a>
 * @version 1.0.0.3, Sep 29, 2011
 */
public final class LocalMailService implements MailService {
	private static Logger logger = LoggerFactory.getLogger(MailServiceFactory.class);

	@Override
	public void send(final Message message) throws IOException {
		// TODO: zezhou jiang, throws ioexception while send fails

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					new MailSender().sendMail(message);
				} catch (final Exception e) {
					logger.error("Sends mail failed", e);
				}
			}
		}).start();
	}
}
