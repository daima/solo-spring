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
package org.b3log.solo.service;

import org.b3log.solo.SoloConstant;
import org.b3log.solo.dao.ArticleDao;
import org.b3log.solo.dao.OptionDao;
import org.b3log.solo.dao.UserDao;
import org.b3log.solo.frame.mail.MailService;
import org.b3log.solo.frame.mail.MailServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Upgrade service.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @author <a href="mailto:dongxu.wang@acm.org">Dongxu Wang</a>
 * @version 1.2.0.11, Apr 10, 2017
 * @since 1.2.0
 */
@Service
public class UpgradeService {

	/**
	 * Logger.
	 */
	private static Logger logger = LoggerFactory.getLogger(UpgradeService.class);
	/**
	 * Step for article updating.
	 */
	private static final int STEP = 50;
	/**
	 * Mail Service.
	 */
	private static final MailService MAIL_SVC = MailServiceFactory.getMailService();
	/**
	 * Old version.
	 */
	private static final String FROM_VER = "1.9.0";
	/**
	 * New version.
	 */
	private static final String TO_VER = SoloConstant.VERSION;
	/**
	 * Article repository.
	 */
	@Autowired
	private ArticleDao articleDao;
	/**
	 * User repository.
	 */
	@Autowired
	private UserDao userDao;
	/**
	 * Option repository.
	 */
	@Autowired
	private OptionDao optionRepository;
	/**
	 * Preference Query Service.
	 */
	@Autowired
	private PreferenceQueryService preferenceQueryService;
	/**
	 * Language service.
	 */
	@Autowired
	private LangPropsService langPropsService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	/**
	 * Upgrades if need.
	 */
	public void upgrade() {
		// try {
		// return;
		// final JSONObject preference = preferenceQueryService.getPreference();
		// if (null == preference) {
		// return;
		// }
		//
		// final String currentVer = preference.getString(Option.ID_C_VERSION);
		// if (SoloConstant.VERSION.equals(currentVer)) {
		// return;
		// }
		//
		// if (FROM_VER.equals(currentVer)) {
		// perform();
		//
		// return;
		// }
		//
		// logger.warn("Attempt to skip more than one version to upgrade.
		// Expected: {0}; Actually: {1}", FROM_VER, currentVer);
		//
		// if (!sent) {
		// notifyUserByEmail();
		//
		// sent = true;
		// }
		// } catch (final Exception e) {
		// logger.error(e.getMessage(), e);
		// logger.error(
		// "Upgrade failed [" + e.getMessage() + "], please contact the Solo
		// developers or reports this "
		// + "issue directly (<a
		// href='https://github.com/b3log/solo/issues/new'>"
		// + "https://github.com/b3log/solo/issues/new</a>) ");
		// }
	}
}
