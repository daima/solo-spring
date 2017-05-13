package org.b3log.solo.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class MailMessage {

	/**
	 * From.
	 */
	private String from;

	/**
	 * Recipients.
	 */
	private Set<String> recipients = new HashSet<>();

	/**
	 * HTML body.
	 */
	private String htmlBody;

	/**
	 * Subject.
	 */
	private String subject;

	/**
	 * Gets the recipients.
	 * 
	 * @return recipients
	 */
	public Set<String> getRecipients() {
		return Collections.unmodifiableSet(recipients);
	}

	/**
	 * Adds the specified recipient.
	 * 
	 * @param recipient
	 *            the specified recipient
	 */
	public void addRecipient(final String recipient) {
		recipients.add(recipient);
	}

	/**
	 * Gets the HTML body.
	 * 
	 * @return HTML body
	 */
	public String getHtmlBody() {
		return htmlBody;
	}

	/**
	 * Sets the HTML body with the specified HTML body.
	 * 
	 * @param htmlBody
	 *            the specified HTML body
	 */
	public void setHtmlBody(final String htmlBody) {
		this.htmlBody = htmlBody;
	}

	/**
	 * Gets the from.
	 * 
	 * @return from
	 */
	public String getFrom() {
		return from;
	}

	/**
	 * Sets the from with the specified from.
	 * 
	 * @param from
	 *            the specified from
	 */
	public void setFrom(final String from) {
		this.from = from;
	}

	/**
	 * Gets the subject.
	 * 
	 * @return subject
	 */
	public String getSubject() {
		return subject;
	}

	/**
	 * Sets the subject with the specified subject.
	 * 
	 * @param subject
	 *            the specified subject
	 */
	public void setSubject(final String subject) {
		this.subject = subject;
	}
}
