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
package org.b3log.solo.service;

import java.io.IOException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;

import javax.mail.Authenticator;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;

import org.apache.commons.lang3.StringUtils;
import org.b3log.solo.model.MailMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Implementation of the {@link MailService} interface.
 *
 * @author <a href="mailto:jiangzezhou1989@gmail.com">zezhou jiang</a>
 * @version 1.0.0.3, Sep 29, 2011
 */
@Service
public final class MailService {
	private static Logger logger = LoggerFactory.getLogger(MailService.class);

	public void send(final MailMessage message) throws IOException {
		// TODO: zezhou jiang, throws ioexception while send fails

		new Thread(() -> {
			try {
				new MailSender().sendMail(message);
			} catch (final Exception e) {
				logger.error("Sends mail failed", e);
			}
		}).start();
	}
}

class MailSender {

	/**
	 * Mail configurations.
	 *
	 * <ul>
	 * <li>mail.user</li>
	 * <li>mail.password</li>
	 * <li>mail.smtp.host</li>
	 * <li>mail.smtp.auth</li>
	 * <li>mail.smtp.port</li>
	 * <li>mail.smtp.starttls.enable</li>
	 * <li>mail.debug</li>
	 * <li>mail.smtp.socketFactory.class</li>
	 * <li>mail.smtp.socketFactory.fallback</li>
	 * <li>mail.smtp.socketFactory.port</li>
	 * </ul>
	 */
	private final ResourceBundle mailProperties = ResourceBundle.getBundle("mail");

	/**
	 * Create session based on the mail properties.
	 *
	 * @return session session from mail properties
	 */
	private Session getSession() {
		final Properties props = new Properties();

		props.setProperty("mail.smtp.host", mailProperties.getString("mail.smtp.host"));

		String auth = "true";
		if (mailProperties.containsKey("mail.smtp.auth")) {
			auth = mailProperties.getString("mail.smtp.auth");
		}
		props.setProperty("mail.smtp.auth", auth);

		props.setProperty("mail.smtp.port", mailProperties.getString("mail.smtp.port"));

		String starttls = "true";
		if (mailProperties.containsKey("mail.smtp.starttls.enable")) {
			starttls = mailProperties.getString("mail.smtp.starttls.enable");
		}
		props.put("mail.smtp.starttls.enable", starttls);

		props.put("mail.debug", mailProperties.getString("mail.debug"));
		props.put("mail.smtp.socketFactory.class", mailProperties.getString("mail.smtp.socketFactory.class"));
		props.put("mail.smtp.socketFactory.fallback", mailProperties.getString("mail.smtp.socketFactory.fallback"));
		props.put("mail.smtp.socketFactory.port", mailProperties.getString("mail.smtp.socketFactory.port"));

		return Session.getInstance(props, new SMTPAuthenticator());
	}

	/**
	 * Converts the specified message into a {@link javax.mail.Message
	 * javax.mail.Message}.
	 *
	 * @param message
	 *            the specified message
	 * @return a {@link javax.mail.internet.MimeMessage}
	 * @throws Exception
	 *             if converts error
	 */
	public javax.mail.Message convert2JavaMailMsg(final MailMessage message) throws Exception {
		if (message == null) {
			return null;
		}

		if (StringUtils.isBlank(message.getFrom())) {
			throw new MessagingException("Null from");
		}

		if (null == message.getRecipients() || message.getRecipients().isEmpty()) {
			throw new MessagingException("Null recipients");
		}

		final MimeMessage ret = new MimeMessage(getSession());

		ret.setFrom(new InternetAddress(message.getFrom()));
		final String subject = message.getSubject();

		ret.setSubject(MimeUtility.encodeText(subject != null ? subject : "", "UTF-8", "B"));
		final String htmlBody = message.getHtmlBody();

		ret.setContent(htmlBody != null ? htmlBody : "", "text/html;charset=UTF-8");
		ret.addRecipients(javax.mail.Message.RecipientType.TO, transformRecipients(message.getRecipients()));

		return ret;
	}

	/**
	 * Transport recipients to InternetAddress array.
	 *
	 * @param recipients
	 *            the set of all recipients
	 * @return InternetAddress array of all recipients internetAddress
	 * @throws MessagingException
	 *             messagingException from javax.mail
	 */
	private InternetAddress[] transformRecipients(final Set<String> recipients) throws MessagingException {
		if (recipients.isEmpty()) {
			throw new MessagingException("recipients of mail should not be empty");
		}

		final InternetAddress[] ret = new InternetAddress[recipients.size()];
		int i = 0;

		for (String recipient : recipients) {
			ret[i] = new InternetAddress(recipient);
			i++;
		}

		return ret;
	}

	/**
	 * Sends email.
	 *
	 * @param message
	 *            the specified message
	 * @throws Exception
	 *             message exception
	 */
	void sendMail(final MailMessage message) throws Exception {
		final javax.mail.Message msg = convert2JavaMailMsg(message);

		Transport.send(msg);
	}

	/**
	 * Inner class for Authenticator.
	 */
	private class SMTPAuthenticator extends Authenticator {

		@Override
		public PasswordAuthentication getPasswordAuthentication() {
			return new PasswordAuthentication(mailProperties.getString("mail.user"),
					mailProperties.getString("mail.password"));
		}
	}
}
