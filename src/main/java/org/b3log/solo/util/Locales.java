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
package org.b3log.solo.util;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.b3log.solo.Keys;
import org.b3log.solo.Latkes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Locale utilities.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.1.1.2, Oct 27, 2016
 */
public final class Locales {

	/**
	 * Logger.
	 */
	private static Logger logger = LoggerFactory.getLogger(Locales.class);

	/**
	 * Thread local holder for locale.
	 */
	private static final ThreadLocal<Locale> LOCALE = new InheritableThreadLocal<>();

	/**
	 * Language start description in Accept-Language of request header.
	 */
	private static final int LANG_START = 0;

	/**
	 * Language end description in Accept-Language of request header.
	 */
	private static final int LANG_END = 2;

	/**
	 * Country start description in Accept-Language of request header.
	 */
	private static final int COUNTRY_START = 3;

	/**
	 * Country end description in Accept-Language of request header.
	 */
	private static final int COUNTRY_END = 5;

	/**
	 * Private default constructor.
	 */
	private Locales() {
	}

	/**
	 * Gets locale with the specified request.
	 *
	 * By the following steps:
	 * <ol>
	 * <li>Gets from session of the specified request</li>
	 * <li>Gets from the specified request header</li>
	 * <li>Using {@link Latkes#getLocale() server configuration}</li>
	 * </ol>
	 *
	 * @param request
	 *            the specified request
	 * @return locale
	 */
	public static Locale getLocale(final HttpServletRequest request) {
		Locale locale = null;

		// Gets from session
		final HttpSession session = request.getSession(false);

		if (session != null) {
			locale = (Locale) session.getAttribute(Keys.LOCALE);
		}

		if (null == locale) {
			// Gets from request header
			final String languageHeader = request.getHeader("Accept-Language");

			logger.debug("[Accept-Language={}]", languageHeader);

			String language = "zh";
			String country = "CN";

			if (!StringUtils.isBlank(languageHeader)) {
				language = getLanguage(languageHeader);
				country = getCountry(languageHeader);
			}

			locale = new Locale(language, country);

			if (!hasLocale(locale)) {
				// Uses default
				locale = Latkes.getLocale();
				logger.debug("Using the default locale[{}]", locale.toString());
			} else {
				logger.debug("Got locale[{}] from request.", locale.toString());
			}
		} else {
			logger.debug("Got locale[{}] from session.", locale.toString());
		}

		return locale;
	}

	/**
	 * Determines whether the server has the specified locale configuration or
	 * not.
	 *
	 * @param locale
	 *            the specified locale
	 * @return {@code true} if the server has the specified locale,
	 *         {@code false} otherwise
	 */
	public static boolean hasLocale(final Locale locale) {
		try {
			ResourceBundle.getBundle(Keys.LANGUAGE, locale);

			return true;
		} catch (final MissingResourceException e) {
			return false;
		}
	}

	/**
	 * Sets the specified locale into session of the specified request.
	 *
	 * <p>
	 * If no session of the specified request, do nothing.
	 * </p>
	 *
	 * @param request
	 *            the specified request
	 * @param locale
	 *            a new locale
	 */
	public static void setLocale(final HttpServletRequest request, final Locale locale) {
		final HttpSession session = request.getSession(false);

		if (null == session) {
			logger.warn("Ignores set locale caused by no session");

			return;
		}

		session.setAttribute(Keys.LOCALE, locale);
		logger.debug("Client[sessionId={}] sets locale to [{}]", new Object[] { session.getId(), locale.toString() });
	}

	/**
	 * Sets locale.
	 *
	 * @param locale
	 *            the specified locale
	 */
	public static void setLocale(final Locale locale) {
		LOCALE.set(locale);
	}

	/**
	 * Gets locale.
	 *
	 * @return locale
	 */
	public static Locale getLocale() {
		final Locale ret = LOCALE.get();
		if (null == ret) {
			return Latkes.getLocale();
		}

		return ret;
	}

	/**
	 * Gets country from the specified locale string.
	 *
	 * @param localeString
	 *            the specified locale string
	 * @return country, if the length of specified locale string less than 5,
	 *         returns ""
	 */
	public static String getCountry(final String localeString) {
		if (localeString.length() >= COUNTRY_END) {
			return localeString.substring(COUNTRY_START, COUNTRY_END);
		}

		return "";
	}

	/**
	 * Gets language from the specified locale string.
	 *
	 * @param localeString
	 *            the specified locale string
	 * @return language, if the length of specified locale string less than 2,
	 *         returns ""
	 */
	public static String getLanguage(final String localeString) {
		if (localeString.length() >= LANG_END) {
			return localeString.substring(LANG_START, LANG_END);
		}

		return "";
	}

	/**
	 * Gest a {@link java.util.Locale} with the specified locale string.
	 *
	 * @param localeString
	 *            the specified locale string
	 * @return locale
	 */
	public static Locale getLocale(final String localeString) {
		final String language = getLanguage(localeString);
		final String country = getCountry(localeString);

		// // XXX: variant
		return new Locale(language, country);
	}
}
