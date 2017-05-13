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

import java.io.IOException;
import java.sql.Connection;
import java.sql.Statement;

import org.b3log.solo.Keys;
import org.b3log.solo.SoloConstant;
import org.b3log.solo.dao.ArticleDao;
import org.b3log.solo.dao.CommentDao;
import org.b3log.solo.dao.OptionDao;
import org.b3log.solo.dao.UserDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.b3log.solo.frame.mail.MailService;
import org.b3log.solo.frame.mail.MailServiceFactory;
import org.b3log.solo.frame.model.User;
import org.b3log.solo.frame.repository.Query;
import org.b3log.solo.frame.repository.Transaction;
import org.b3log.solo.frame.repository.jdbc.JdbcRepository;
import org.b3log.solo.frame.service.ServiceException;
import org.b3log.solo.model.Article;
import org.b3log.solo.model.Option;
import org.b3log.solo.model.UserExt;
import org.b3log.solo.module.util.Thumbnails;
import org.b3log.solo.util.PropsUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
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
     * Whether the email has been sent.
     */
    private static boolean sent = false;
    /**
     * Article repository.
     */
    @Autowired
    private ArticleDao articleDao;
    /**
     * Comment repository.
     */
    @Autowired
    private CommentDao commentDao;
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
//        try {
//        	return;
//            final JSONObject preference = preferenceQueryService.getPreference();
//            if (null == preference) {
//                return;
//            }
//
//            final String currentVer = preference.getString(Option.ID_C_VERSION);
//            if (SoloConstant.VERSION.equals(currentVer)) {
//                return;
//            }
//
//            if (FROM_VER.equals(currentVer)) {
//                perform();
//
//                return;
//            }
//
//            logger.warn("Attempt to skip more than one version to upgrade. Expected: {0}; Actually: {1}", FROM_VER, currentVer);
//
//            if (!sent) {
//                notifyUserByEmail();
//
//                sent = true;
//            }
//        } catch (final Exception e) {
//            logger.error(e.getMessage(), e);
//            logger.error(
//                    "Upgrade failed [" + e.getMessage() + "], please contact the Solo developers or reports this "
//                            + "issue directly (<a href='https://github.com/b3log/solo/issues/new'>"
//                            + "https://github.com/b3log/solo/issues/new</a>) ");
//        }
    }

    /**
     * Performs upgrade.
     *
     * @throws Exception upgrade fails
     */
    private void perform() throws Exception {
        logger.info("Upgrading from version [{0}] to version [{1}]....", FROM_VER, TO_VER);

//        Transaction transaction = null;

        try {
            final Connection connection = jdbcTemplate.getDataSource().getConnection();
            final Statement statement = connection.createStatement();

            final String tablePrefix = PropsUtil.getString("jdbc.tablePrefix") + "_";
            statement.execute("CREATE TABLE `" + tablePrefix + "category` (\n" +
                    "  `oId` varchar(19) NOT NULL,\n" +
                    "  `categoryTitle` varchar(64) NOT NULL,\n" +
                    "  `categoryURI` varchar(32) NOT NULL,\n" +
                    "  `categoryDescription` text NOT NULL,\n" +
                    "  `categoryOrder` int(11) NOT NULL,\n" +
                    "  `categoryTagCnt` int(11) NOT NULL,\n" +
                    "  PRIMARY KEY (`oId`)\n" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8;");
            statement.execute("CREATE TABLE `" + tablePrefix + "category_tag` (\n" +
                    "  `oId` varchar(19) NOT NULL,\n" +
                    "  `category_oId` varchar(19) NOT NULL,\n" +
                    "  `tag_oId` varchar(19) NOT NULL,\n" +
                    "  PRIMARY KEY (`oId`)\n" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8;");
            statement.close();
            connection.commit();
            connection.close();

//            transaction = optionRepository.beginTransaction();

            final JSONObject versionOpt = optionRepository.get(Option.ID_C_VERSION);
            versionOpt.put(Option.OPTION_VALUE, TO_VER);
            optionRepository.update(Option.ID_C_VERSION, versionOpt);

//            transaction.commit();

            logger.info("Updated preference");
        } catch (final Exception e) {
//            if (null != transaction && transaction.isActive()) {
//                transaction.rollback();
//            }

            logger.error("Upgrade failed!", e);
            throw new Exception("Upgrade failed from version [" + FROM_VER + "] to version [" + TO_VER + ']');
        }

        logger.info("Upgraded from version [{0}] to version [{1}] successfully :-)", FROM_VER, TO_VER);
    }

    /**
     * Upgrades users.
     * <p>
     * Password hashing.
     * </p>
     *
     * @throws Exception exception
     */
    private void upgradeUsers() throws Exception {
        final JSONArray users = userDao.get(new Query()).getJSONArray(Keys.RESULTS);

        for (int i = 0; i < users.length(); i++) {
            final JSONObject user = users.getJSONObject(i);
            final String email = user.optString(User.USER_EMAIL);

            user.put(UserExt.USER_AVATAR, Thumbnails.getGravatarURL(email, "128"));

            userDao.update(user.optString(Keys.OBJECT_ID), user);

            logger.info("Updated user[email={0}]", email);
        }
    }

    /**
     * Upgrades articles.
     *
     * @throws Exception exception
     */
    private void upgradeArticles() throws Exception {
        logger.info("Adds a property [articleEditorType] to each of articles");

        final JSONArray articles = articleDao.get(new Query()).getJSONArray(Keys.RESULTS);

        if (articles.length() <= 0) {
            logger.trace("No articles");
            return;
        }

//        Transaction transaction = null;

        try {
            for (int i = 0; i < articles.length(); i++) {
//                if (0 == i % STEP || !transaction.isActive()) {
////                    transaction = userDao.beginTransaction();
//                }

                final JSONObject article = articles.getJSONObject(i);

                final String articleId = article.optString(Keys.OBJECT_ID);

                logger.info("Found an article[id={0}]", articleId);
                article.put(Article.ARTICLE_EDITOR_TYPE, "tinyMCE");

                articleDao.update(article.getString(Keys.OBJECT_ID), article);

                if (0 == i % STEP) {
//                    transaction.commit();
                    logger.trace("Updated some articles");
                }
            }

//            if (transaction.isActive()) {
//                transaction.commit();
//            }

            logger.trace("Updated all articles");
        } catch (final Exception e) {
//            if (transaction.isActive()) {
//                transaction.rollback();
//            }

            throw e;
        }
    }

    /**
     * Send an email to the user who upgrades Solo with a discontinuous version.
     *
     * @throws ServiceException ServiceException
     * @throws JSONException    JSONException
     * @throws IOException      IOException
     */
    private void notifyUserByEmail() throws ServiceException, JSONException, IOException {
        final String adminEmail = preferenceQueryService.getPreference().getString(Option.ID_C_ADMIN_EMAIL);
        final MailService.Message message = new MailService.Message();

        message.setFrom(adminEmail);
        message.addRecipient(adminEmail);
        message.setSubject(langPropsService.get("skipVersionMailSubject"));
        message.setHtmlBody(langPropsService.get("skipVersionMailBody"));

        MAIL_SVC.send(message);

        logger.info("Send an email to the user who upgrades Solo with a discontinuous version.");
    }
}
