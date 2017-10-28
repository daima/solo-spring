/*
 * Copyright (c) 2017, cxy7.com
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

import static org.b3log.solo.model.Skin.SKINS;
import static org.b3log.solo.model.Skin.SKIN_DIR_NAME;
import static org.b3log.solo.model.Skin.SKIN_NAME;
import static org.b3log.solo.module.util.Skins.getSkinDirNames;
import static org.b3log.solo.module.util.Skins.setDirectoryForTemplateLoading;

import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

import javax.servlet.ServletContext;

import org.b3log.solo.Latkes;
import org.b3log.solo.dao.OptionDao;
import org.b3log.solo.model.Option;
import org.b3log.solo.model.Skin;
import org.b3log.solo.module.util.Skins;
import org.b3log.solo.module.util.TimeZones;
import org.b3log.solo.util.Locales;
import org.b3log.solo.util.Stopwatchs;
import org.b3log.solo.util.freemarker.Templates;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.ContextLoader;

/**
 * Preference management service.
 *
 * @author <a href="http://cxy7.com">XyCai</a>
 * @version 1.3.2.11, Feb 18, 2017
 * @since 0.4.0
 */
@Service
public class PreferenceMgmtService {

	/**
	 * Logger.
	 */
	private static Logger logger = LoggerFactory.getLogger(PreferenceMgmtService.class);

	/**
	 * Preference query service.
	 */
	@Autowired
	private PreferenceQueryService preferenceQueryService;

	/**
	 * Option repository.
	 */
	@Autowired
	private OptionDao optionRepository;

	/**
	 * Language service.
	 */
	@Autowired
	private LangPropsService langPropsService;

	/**
	 * Loads skins for the specified preference and initializes templates
	 * loading.
	 *
	 * <p>
	 * If the skins directory has been changed, persists the change into
	 * preference.
	 * </p>
	 *
	 * @param preference
	 *            the specified preference
	 * @throws Exception
	 *             exception
	 */
	public void loadSkins(final JSONObject preference) throws Exception {
		Stopwatchs.start("Load Skins");

		logger.debug("Loading skins....");

		final Set<String> skinDirNames = getSkinDirNames();

		logger.debug("Loaded skins[dirNames={}]", skinDirNames);
		final JSONArray skinArray = new JSONArray();

		for (final String dirName : skinDirNames) {
			final JSONObject skin = new JSONObject();
			final String name = Latkes.getSkinName(dirName);

			if (null == name) {
				logger.warn("The directory[{}] does not contain any skin, ignored it", dirName);

				continue;
			}

			skin.put(SKIN_NAME, name);
			skin.put(SKIN_DIR_NAME, dirName);

			skinArray.put(skin);
		}

		final String currentSkinDirName = preference.optString(SKIN_DIR_NAME);
		final String skinName = preference.optString(SKIN_NAME);

		logger.debug("Current skin[name={}]", skinName);

		if (!skinDirNames.contains(currentSkinDirName)) {
			logger.warn("Configred skin[dirName={}] can not find, try to use " + "default skin[dirName="
					+ Option.DefaultPreference.DEFAULT_SKIN_DIR_NAME + "] instead.", currentSkinDirName);
			if (!skinDirNames.contains(Option.DefaultPreference.DEFAULT_SKIN_DIR_NAME)) {
				logger.error("Can not find skin[dirName=" + Option.DefaultPreference.DEFAULT_SKIN_DIR_NAME + "]");

				throw new IllegalStateException(
						"Can not find default skin[dirName=" + Option.DefaultPreference.DEFAULT_SKIN_DIR_NAME
								+ "], please redeploy your Solo and make sure contains this default skin!");
			}

			preference.put(SKIN_DIR_NAME, Option.DefaultPreference.DEFAULT_SKIN_DIR_NAME);
			preference.put(SKIN_NAME, Latkes.getSkinName(Option.DefaultPreference.DEFAULT_SKIN_DIR_NAME));

			updatePreference(preference);
		}

		final String skinsString = skinArray.toString();

		if (!skinsString.equals(preference.getString(SKINS))) {
			logger.debug("The skins directory has been changed, persists the change into preference");
			preference.put(SKINS, skinsString);
			updatePreference(preference);
		}

		setDirectoryForTemplateLoading(preference.getString(SKIN_DIR_NAME));

		final String localeString = preference.getString(Option.ID_C_LOCALE_STRING);

		if ("zh_CN".equals(localeString)) {
			TimeZones.setTimeZone("Asia/Shanghai");
		}

		logger.debug("Loaded skins....");

		Stopwatchs.end();
	}

	/**
	 * Updates the reply notification template with the specified reply
	 * notification template.
	 *
	 * @param replyNotificationTemplate
	 *            the specified reply notification template
	 * @throws ServiceException
	 *             service exception
	 */
	public void updateReplyNotificationTemplate(final JSONObject replyNotificationTemplate) throws ServiceException {
		// final Transaction transaction = optionRepository.beginTransaction();

		try {
			final JSONObject bodyOpt = optionRepository.get(Option.ID_C_REPLY_NOTI_TPL_BODY);
			bodyOpt.put(Option.OPTION_VALUE, replyNotificationTemplate.optString("body"));
			optionRepository.update(Option.ID_C_REPLY_NOTI_TPL_BODY, bodyOpt);

			final JSONObject subjectOpt = optionRepository.get(Option.ID_C_REPLY_NOTI_TPL_SUBJECT);
			subjectOpt.put(Option.OPTION_VALUE, replyNotificationTemplate.optString("subject"));
			optionRepository.update(Option.ID_C_REPLY_NOTI_TPL_SUBJECT, subjectOpt);

			// transaction.commit();
		} catch (final Exception e) {
			// if (transaction.isActive()) {
			// transaction.rollback();
			// }

			logger.error("Updates reply notification failed", e);
			throw new ServiceException(e);
		}
	}

	/**
	 * Updates the preference with the specified preference.
	 *
	 * @param preference
	 *            the specified preference
	 * @throws ServiceException
	 *             service exception
	 */
	public void updatePreference(final JSONObject preference) throws ServiceException {
		final Iterator<String> keys = preference.keys();

		while (keys.hasNext()) {
			final String key = keys.next();

			if (preference.isNull(key)) {
				throw new ServiceException("A value is null of preference[key=" + key + "]");
			}
		}

		// final Transaction transaction = optionRepository.beginTransaction();

		try {
			final String skinDirName = preference.getString(Skin.SKIN_DIR_NAME);
			final String skinName = Latkes.getSkinName(skinDirName);

			preference.put(Skin.SKIN_NAME, skinName);
			final Set<String> skinDirNames = Skins.getSkinDirNames();
			final JSONArray skinArray = new JSONArray();

			for (final String dirName : skinDirNames) {
				final JSONObject skin = new JSONObject();

				skinArray.put(skin);

				final String name = Latkes.getSkinName(dirName);

				skin.put(Skin.SKIN_NAME, name);
				skin.put(Skin.SKIN_DIR_NAME, dirName);
			}

			preference.put(Skin.SKINS, skinArray.toString());

			final String timeZoneId = preference.getString(Option.ID_C_TIME_ZONE_ID);
			TimeZones.setTimeZone(timeZoneId);

			preference.put(Option.ID_C_SIGNS, preference.get(Option.ID_C_SIGNS).toString());

			final JSONObject oldPreference = preferenceQueryService.getPreference();
			final String adminEmail = oldPreference.getString(Option.ID_C_ADMIN_EMAIL);
			preference.put(Option.ID_C_ADMIN_EMAIL, adminEmail);

			final String version = oldPreference.optString(Option.ID_C_VERSION);
			preference.put(Option.ID_C_VERSION, version);

			final String localeString = preference.getString(Option.ID_C_LOCALE_STRING);
			logger.debug("Current locale[string={}]", localeString);
			Latkes.setLocale(new Locale(Locales.getLanguage(localeString), Locales.getCountry(localeString)));

			final JSONObject adminEmailOpt = optionRepository.get(Option.ID_C_ADMIN_EMAIL);
			adminEmailOpt.put(Option.OPTION_VALUE, adminEmail);
			optionRepository.update(Option.ID_C_ADMIN_EMAIL, adminEmailOpt);

			final JSONObject allowVisitDraftViaPermalinkOpt = optionRepository
					.get(Option.ID_C_ALLOW_VISIT_DRAFT_VIA_PERMALINK);
			allowVisitDraftViaPermalinkOpt.put(Option.OPTION_VALUE,
					preference.optString(Option.ID_C_ALLOW_VISIT_DRAFT_VIA_PERMALINK));
			optionRepository.update(Option.ID_C_ALLOW_VISIT_DRAFT_VIA_PERMALINK, allowVisitDraftViaPermalinkOpt);

			final JSONObject allowRegisterOpt = optionRepository.get(Option.ID_C_ALLOW_REGISTER);
			allowRegisterOpt.put(Option.OPTION_VALUE, preference.optString(Option.ID_C_ALLOW_REGISTER));
			optionRepository.update(Option.ID_C_ALLOW_REGISTER, allowRegisterOpt);

			final JSONObject articleListDisplayCountOpt = optionRepository.get(Option.ID_C_ARTICLE_LIST_DISPLAY_COUNT);
			articleListDisplayCountOpt.put(Option.OPTION_VALUE,
					preference.optString(Option.ID_C_ARTICLE_LIST_DISPLAY_COUNT));
			optionRepository.update(Option.ID_C_ARTICLE_LIST_DISPLAY_COUNT, articleListDisplayCountOpt);

			final JSONObject articleListPaginationWindowSizeOpt = optionRepository
					.get(Option.ID_C_ARTICLE_LIST_PAGINATION_WINDOW_SIZE);
			articleListPaginationWindowSizeOpt.put(Option.OPTION_VALUE,
					preference.optString(Option.ID_C_ARTICLE_LIST_PAGINATION_WINDOW_SIZE));
			optionRepository.update(Option.ID_C_ARTICLE_LIST_PAGINATION_WINDOW_SIZE,
					articleListPaginationWindowSizeOpt);

			final JSONObject articleListStyleOpt = optionRepository.get(Option.ID_C_ARTICLE_LIST_STYLE);
			articleListStyleOpt.put(Option.OPTION_VALUE, preference.optString(Option.ID_C_ARTICLE_LIST_STYLE));
			optionRepository.update(Option.ID_C_ARTICLE_LIST_STYLE, articleListStyleOpt);

			final JSONObject blogSubtitleOpt = optionRepository.get(Option.ID_C_BLOG_SUBTITLE);
			blogSubtitleOpt.put(Option.OPTION_VALUE, preference.optString(Option.ID_C_BLOG_SUBTITLE));
			optionRepository.update(Option.ID_C_BLOG_SUBTITLE, blogSubtitleOpt);

			final JSONObject blogTitleOpt = optionRepository.get(Option.ID_C_BLOG_TITLE);
			blogTitleOpt.put(Option.OPTION_VALUE, preference.optString(Option.ID_C_BLOG_TITLE));
			optionRepository.update(Option.ID_C_BLOG_TITLE, blogTitleOpt);

			final JSONObject commentableOpt = optionRepository.get(Option.ID_C_COMMENTABLE);
			commentableOpt.put(Option.OPTION_VALUE, preference.optString(Option.ID_C_COMMENTABLE));
			optionRepository.update(Option.ID_C_COMMENTABLE, commentableOpt);

			final JSONObject editorTypeOpt = optionRepository.get(Option.ID_C_EDITOR_TYPE);
			editorTypeOpt.put(Option.OPTION_VALUE, preference.optString(Option.ID_C_EDITOR_TYPE));
			optionRepository.update(Option.ID_C_EDITOR_TYPE, editorTypeOpt);

			final JSONObject enableArticleUpdateHintOpt = optionRepository.get(Option.ID_C_ENABLE_ARTICLE_UPDATE_HINT);
			enableArticleUpdateHintOpt.put(Option.OPTION_VALUE,
					preference.optString(Option.ID_C_ENABLE_ARTICLE_UPDATE_HINT));
			optionRepository.update(Option.ID_C_ENABLE_ARTICLE_UPDATE_HINT, enableArticleUpdateHintOpt);

			final JSONObject externalRelevantArticlesDisplayCountOpt = optionRepository
					.get(Option.ID_C_EXTERNAL_RELEVANT_ARTICLES_DISPLAY_CNT);
			externalRelevantArticlesDisplayCountOpt.put(Option.OPTION_VALUE,
					preference.optString(Option.ID_C_EXTERNAL_RELEVANT_ARTICLES_DISPLAY_CNT));
			optionRepository.update(Option.ID_C_EXTERNAL_RELEVANT_ARTICLES_DISPLAY_CNT,
					externalRelevantArticlesDisplayCountOpt);

			final JSONObject feedOutputCntOpt = optionRepository.get(Option.ID_C_FEED_OUTPUT_CNT);
			feedOutputCntOpt.put(Option.OPTION_VALUE, preference.optString(Option.ID_C_FEED_OUTPUT_CNT));
			optionRepository.update(Option.ID_C_FEED_OUTPUT_CNT, feedOutputCntOpt);

			final JSONObject feedOutputModeOpt = optionRepository.get(Option.ID_C_FEED_OUTPUT_MODE);
			feedOutputModeOpt.put(Option.OPTION_VALUE, preference.optString(Option.ID_C_FEED_OUTPUT_MODE));
			optionRepository.update(Option.ID_C_FEED_OUTPUT_MODE, feedOutputModeOpt);

			final JSONObject footerContentOpt = optionRepository.get(Option.ID_C_FOOTER_CONTENT);
			footerContentOpt.put(Option.OPTION_VALUE, preference.optString(Option.ID_C_FOOTER_CONTENT));
			optionRepository.update(Option.ID_C_FOOTER_CONTENT, footerContentOpt);

			final JSONObject htmlHeadOpt = optionRepository.get(Option.ID_C_HTML_HEAD);
			htmlHeadOpt.put(Option.OPTION_VALUE, preference.optString(Option.ID_C_HTML_HEAD));
			optionRepository.update(Option.ID_C_HTML_HEAD, htmlHeadOpt);

			final JSONObject keyOfSoloOpt = optionRepository.get(Option.ID_C_KEY_OF_SOLO);
			keyOfSoloOpt.put(Option.OPTION_VALUE, preference.optString(Option.ID_C_KEY_OF_SOLO));
			optionRepository.update(Option.ID_C_KEY_OF_SOLO, keyOfSoloOpt);

			final JSONObject localeStringOpt = optionRepository.get(Option.ID_C_LOCALE_STRING);
			localeStringOpt.put(Option.OPTION_VALUE, preference.optString(Option.ID_C_LOCALE_STRING));
			optionRepository.update(Option.ID_C_LOCALE_STRING, localeStringOpt);

			final JSONObject metaDescriptionOpt = optionRepository.get(Option.ID_C_META_DESCRIPTION);
			metaDescriptionOpt.put(Option.OPTION_VALUE, preference.optString(Option.ID_C_META_DESCRIPTION));
			optionRepository.update(Option.ID_C_META_DESCRIPTION, metaDescriptionOpt);

			final JSONObject metaKeywordsOpt = optionRepository.get(Option.ID_C_META_KEYWORDS);
			metaKeywordsOpt.put(Option.OPTION_VALUE, preference.optString(Option.ID_C_META_KEYWORDS));
			optionRepository.update(Option.ID_C_META_KEYWORDS, metaKeywordsOpt);

			final JSONObject mostCommentArticleDisplayCountOpt = optionRepository
					.get(Option.ID_C_MOST_COMMENT_ARTICLE_DISPLAY_CNT);
			mostCommentArticleDisplayCountOpt.put(Option.OPTION_VALUE,
					preference.optString(Option.ID_C_MOST_COMMENT_ARTICLE_DISPLAY_CNT));
			optionRepository.update(Option.ID_C_MOST_COMMENT_ARTICLE_DISPLAY_CNT, mostCommentArticleDisplayCountOpt);

			final JSONObject mostUsedTagDisplayCountOpt = optionRepository.get(Option.ID_C_MOST_USED_TAG_DISPLAY_CNT);
			mostUsedTagDisplayCountOpt.put(Option.OPTION_VALUE,
					preference.optString(Option.ID_C_MOST_USED_TAG_DISPLAY_CNT));
			optionRepository.update(Option.ID_C_MOST_USED_TAG_DISPLAY_CNT, mostUsedTagDisplayCountOpt);

			final JSONObject mostViewArticleDisplayCountOpt = optionRepository
					.get(Option.ID_C_MOST_VIEW_ARTICLE_DISPLAY_CNT);
			mostViewArticleDisplayCountOpt.put(Option.OPTION_VALUE,
					preference.optString(Option.ID_C_MOST_VIEW_ARTICLE_DISPLAY_CNT));
			optionRepository.update(Option.ID_C_MOST_VIEW_ARTICLE_DISPLAY_CNT, mostViewArticleDisplayCountOpt);

			final JSONObject noticeBoardOpt = optionRepository.get(Option.ID_C_NOTICE_BOARD);
			noticeBoardOpt.put(Option.OPTION_VALUE, preference.optString(Option.ID_C_NOTICE_BOARD));
			optionRepository.update(Option.ID_C_NOTICE_BOARD, noticeBoardOpt);

			final JSONObject randomArticlesDisplayCountOpt = optionRepository
					.get(Option.ID_C_RANDOM_ARTICLES_DISPLAY_CNT);
			randomArticlesDisplayCountOpt.put(Option.OPTION_VALUE,
					preference.optString(Option.ID_C_RANDOM_ARTICLES_DISPLAY_CNT));
			optionRepository.update(Option.ID_C_RANDOM_ARTICLES_DISPLAY_CNT, randomArticlesDisplayCountOpt);

			final JSONObject recentArticleDisplayCountOpt = optionRepository
					.get(Option.ID_C_RECENT_ARTICLE_DISPLAY_CNT);
			recentArticleDisplayCountOpt.put(Option.OPTION_VALUE,
					preference.optString(Option.ID_C_RECENT_ARTICLE_DISPLAY_CNT));
			optionRepository.update(Option.ID_C_RECENT_ARTICLE_DISPLAY_CNT, recentArticleDisplayCountOpt);

			final JSONObject recentCommentDisplayCountOpt = optionRepository
					.get(Option.ID_C_RECENT_COMMENT_DISPLAY_CNT);
			recentCommentDisplayCountOpt.put(Option.OPTION_VALUE,
					preference.optString(Option.ID_C_RECENT_COMMENT_DISPLAY_CNT));
			optionRepository.update(Option.ID_C_RECENT_COMMENT_DISPLAY_CNT, recentCommentDisplayCountOpt);

			final JSONObject relevantArticlesDisplayCountOpt = optionRepository
					.get(Option.ID_C_RELEVANT_ARTICLES_DISPLAY_CNT);
			relevantArticlesDisplayCountOpt.put(Option.OPTION_VALUE,
					preference.optString(Option.ID_C_RELEVANT_ARTICLES_DISPLAY_CNT));
			optionRepository.update(Option.ID_C_RELEVANT_ARTICLES_DISPLAY_CNT, relevantArticlesDisplayCountOpt);

			final JSONObject signsOpt = optionRepository.get(Option.ID_C_SIGNS);
			signsOpt.put(Option.OPTION_VALUE, preference.optString(Option.ID_C_SIGNS));
			optionRepository.update(Option.ID_C_SIGNS, signsOpt);

			final JSONObject skinDirNameOpt = optionRepository.get(Option.ID_C_SKIN_DIR_NAME);
			skinDirNameOpt.put(Option.OPTION_VALUE, preference.optString(Option.ID_C_SKIN_DIR_NAME));
			optionRepository.update(Option.ID_C_SKIN_DIR_NAME, skinDirNameOpt);

			final JSONObject skinNameOpt = optionRepository.get(Option.ID_C_SKIN_NAME);
			skinNameOpt.put(Option.OPTION_VALUE, preference.optString(Option.ID_C_SKIN_NAME));
			optionRepository.update(Option.ID_C_SKIN_NAME, skinNameOpt);

			final JSONObject skinsOpt = optionRepository.get(Option.ID_C_SKINS);
			skinsOpt.put(Option.OPTION_VALUE, preference.optString(Option.ID_C_SKINS));
			optionRepository.update(Option.ID_C_SKINS, skinsOpt);

			final JSONObject timeZoneIdOpt = optionRepository.get(Option.ID_C_TIME_ZONE_ID);
			timeZoneIdOpt.put(Option.OPTION_VALUE, preference.optString(Option.ID_C_TIME_ZONE_ID));
			optionRepository.update(Option.ID_C_TIME_ZONE_ID, timeZoneIdOpt);

			final JSONObject versionOpt = optionRepository.get(Option.ID_C_VERSION);
			versionOpt.put(Option.OPTION_VALUE, preference.optString(Option.ID_C_VERSION));
			optionRepository.update(Option.ID_C_VERSION, versionOpt);

			// transaction.commit();

			final ServletContext servletContext = ContextLoader.getCurrentWebApplicationContext().getServletContext();

			Templates.MAIN_CFG.setServletContextForTemplateLoading(servletContext, "/skins/" + skinDirName);
		} catch (final Exception e) {
			// if (transaction.isActive()) {
			// transaction.rollback();
			// }

			logger.error("Updates preference failed", e);
			throw new ServiceException(langPropsService.get("updateFailLabel"));
		}

		logger.debug("Updates preference successfully");
	}

	/**
	 * Sets the language service with the specified language service.
	 *
	 * @param langPropsService
	 *            the specified language service
	 */
	public void setLangPropsService(final LangPropsService langPropsService) {
		this.langPropsService = langPropsService;
	}
}
