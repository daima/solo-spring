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

import java.net.URL;
import java.text.ParseException;
import java.util.Date;
import java.util.Set;

import javax.servlet.ServletContext;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.b3log.solo.Keys;
import org.b3log.solo.Latkes;
import org.b3log.solo.RuntimeDatabase;
import org.b3log.solo.RuntimeEnv;
import org.b3log.solo.SoloConstant;
import org.b3log.solo.dao.ArchiveDateArticleDao;
import org.b3log.solo.dao.ArchiveDateDao;
import org.b3log.solo.dao.ArticleDao;
import org.b3log.solo.dao.CommentDao;
import org.b3log.solo.dao.LinkDao;
import org.b3log.solo.dao.OptionDao;
import org.b3log.solo.dao.StatisticDao;
import org.b3log.solo.dao.TagArticleDao;
import org.b3log.solo.dao.TagDao;
import org.b3log.solo.dao.UserDao;
import org.b3log.solo.dao.repository.RepositoryException;
import org.b3log.solo.frame.urlfetch.HTTPRequest;
import org.b3log.solo.frame.urlfetch.URLFetchService;
import org.b3log.solo.frame.urlfetch.URLFetchServiceFactory;
import org.b3log.solo.model.ArchiveDate;
import org.b3log.solo.model.Article;
import org.b3log.solo.model.Comment;
import org.b3log.solo.model.Link;
import org.b3log.solo.model.Option;
import org.b3log.solo.model.Role;
import org.b3log.solo.model.Option.DefaultPreference;
import org.b3log.solo.model.Skin;
import org.b3log.solo.model.Statistic;
import org.b3log.solo.model.Tag;
import org.b3log.solo.model.User;
import org.b3log.solo.model.UserExt;
import org.b3log.solo.module.util.Comments;
import org.b3log.solo.module.util.Skins;
import org.b3log.solo.module.util.Thumbnails;
import org.b3log.solo.module.util.TimeZones;
import org.b3log.solo.util.Ids;
import org.b3log.solo.util.MD5;
import org.b3log.solo.util.PropsUtil;
import org.b3log.solo.util.freemarker.Templates;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.ContextLoader;

/**
 * Solo initialization service.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.5.2.11, Nov 8, 2016
 * @since 0.4.0
 */
@Service
public class InitService {

	/**
	 * Logger.
	 */
	private static Logger logger = LoggerFactory.getLogger(InitService.class);

	/**
	 * Statistic repository.
	 */
	@Autowired
	private StatisticDao statisticDao;

	/**
	 * Option repository.
	 */
	@Autowired
	private OptionDao optionRepository;

	/**
	 * User repository.
	 */
	@Autowired
	private UserDao userDao;

	/**
	 * Tag-Article repository.
	 */
	@Autowired
	private TagArticleDao tagArticleDao;

	/**
	 * Archive date repository.
	 */
	@Autowired
	private ArchiveDateDao archiveDateDao;

	/**
	 * Archive date-Article repository.
	 */
	@Autowired
	private ArchiveDateArticleDao archiveDateArticleDao;

	/**
	 * Tag repository.
	 */
	@Autowired
	private TagDao tagDao;

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
	 * Link repository.
	 */
	@Autowired
	private LinkDao linkDao;

	/**
	 * Maximum count of initialization.
	 */
	private static final int MAX_RETRIES_CNT = 3;

	/**
	 * Initialized time zone id.
	 */
	private static final String INIT_TIME_ZONE_ID = "Asia/Shanghai";

	/**
	 * Language service.
	 */
	@Autowired
	private LangPropsService langPropsService;

	/**
	 * Plugin manager.
	 */
	// @Autowired
	// private PluginManager pluginManager;

	/**
	 * Determines Solo had been initialized.
	 *
	 * @return {@code true} if it had been initialized, {@code false} otherwise
	 */
	// XXX: to find a better way (isInited)?
	public boolean isInited() {
		try {
			final JSONObject admin = userDao.getAdmin();

			return null != admin;
		} catch (final RepositoryException e) {
			logger.warn("Solo has not been initialized");
			return false;
		}
	}

	/**
	 * Initializes Solo.
	 *
	 * <p>
	 * Initializes the followings in sequence:
	 * <ol>
	 * <li>Statistic</li>
	 * <li>Preference</li>
	 * <li>Administrator</li>
	 * </ol>
	 * </p>
	 *
	 * <p>
	 * We will try to initialize Solo 3 times at most.
	 * </p>
	 *
	 * <p>
	 * Posts "Hello World!" article and its comment while Solo initialized.
	 * </p>
	 *
	 * @param requestJSONObject
	 *            the specified request json object, for example,
	 * 
	 *            <pre>
	 * {
	 *     "userName": "",
	 *     "userEmail": "",
	 *     "userPassword": "", // Unhashed
	 * }
	 *            </pre>
	 *
	 * @throws ServiceException
	 *             service exception
	 */
	public void init(final JSONObject requestJSONObject) throws ServiceException {
		if (isInited()) {
			return;
		}

		final RuntimeEnv runtimeEnv = Latkes.getRuntimeEnv();

		if (RuntimeEnv.LOCAL == runtimeEnv) {
			logger.info("Solo is running on [" + runtimeEnv + "] environment, database [{0}], creates " + "all tables",
					Latkes.getRuntimeDatabase());

			if (RuntimeDatabase.H2 == Latkes.getRuntimeDatabase()) {
				String dataDir = PropsUtil.getString("jdbc.URL");
				dataDir = dataDir.replace("~", System.getProperty("user.home"));
				logger.info("YOUR DATA will be stored in directory [" + dataDir + "], "
						+ "please pay more attention to it~");
			}

			// final List<CreateTableResult> createTableResults =
			// JdbcRepositories.initAllTables();
			// for (final CreateTableResult createTableResult :
			// createTableResults) {
			// logger.debug( "Create table result[tableName={0},
			// isSuccess={1}]",
			// createTableResult.getName(), createTableResult.isSuccess());
			// }
		}

		int retries = MAX_RETRIES_CNT;

		while (true) {
			// final Transaction transaction = userDao.beginTransaction();

			try {
				final JSONObject statistic = statisticDao.get(Statistic.STATISTIC);

				if (null == statistic) {
					initStatistic();
					initPreference(requestJSONObject);
					initReplyNotificationTemplate();
					initAdmin(requestJSONObject);
					initLink();
				}

				// transaction.commit();

				break;
			} catch (final Exception e) {
				if (0 == retries) {
					logger.error("Initialize Solo error", e);
					throw new ServiceException("Initailize Solo error: " + e.getMessage());
				}

				// Allow retry to occur
				--retries;
				logger.warn("Retrying to init Solo[retries={0}]", retries);
			} finally {
				// if (transaction.isActive()) {
				// transaction.rollback();
				// }
			}
		}

		// final Transaction transaction = userDao.beginTransaction();

		try {
			helloWorld();
			// transaction.commit();
		} catch (final Exception e) {
			// if (transaction.isActive()) {
			// transaction.rollback();
			// }

			logger.error("Hello World error?!", e);
		}

		try {
			final URLFetchService urlFetchService = URLFetchServiceFactory.getURLFetchService();

			final HTTPRequest req = new HTTPRequest();
			req.setURL(new URL(Latkes.getServePath() + "/blog/symphony/user"));
			urlFetchService.fetch(req);
		} catch (final Exception e) {
			logger.trace("Sync account failed");
		}

		// pluginManager.load();
	}

	/**
	 * Publishes the first article "Hello World" and the first comment with the
	 * specified locale.
	 *
	 * @throws Exception
	 *             exception
	 */
	private void helloWorld() throws Exception {
		final JSONObject article = new JSONObject();

		article.put(Article.ARTICLE_TITLE, langPropsService.get("helloWorld.title"));
		final String content = langPropsService.get("helloWorld.content");

		article.put(Article.ARTICLE_ABSTRACT, content);
		article.put(Article.ARTICLE_CONTENT, content);
		article.put(Article.ARTICLE_TAGS_REF, "Solo");
		article.put(Article.ARTICLE_PERMALINK, "/hello-solo");
		article.put(Article.ARTICLE_IS_PUBLISHED, true);
		article.put(Article.ARTICLE_HAD_BEEN_PUBLISHED, true);
		article.put(Article.ARTICLE_SIGN_ID, "1");
		article.put(Article.ARTICLE_COMMENT_COUNT, 1);
		article.put(Article.ARTICLE_VIEW_COUNT, 0);
		final Date date = new Date();

		final JSONObject admin = userDao.getAdmin();
		final String authorEmail = admin.optString(User.USER_EMAIL);

		article.put(Article.ARTICLE_CREATE_DATE, date);
		article.put(Article.ARTICLE_UPDATE_DATE, date);
		article.put(Article.ARTICLE_PUT_TOP, false);
		article.put(Article.ARTICLE_RANDOM_DOUBLE, Math.random());
		article.put(Article.ARTICLE_AUTHOR_EMAIL, authorEmail);
		article.put(Article.ARTICLE_COMMENTABLE, true);
		article.put(Article.ARTICLE_VIEW_PWD, "");
		article.put(Article.ARTICLE_EDITOR_TYPE, DefaultPreference.DEFAULT_EDITOR_TYPE);

		final String articleId = addHelloWorldArticle(article);

		final JSONObject comment = new JSONObject();

		comment.put(Keys.OBJECT_ID, articleId);
		comment.put(Comment.COMMENT_NAME, "Daniel");
		comment.put(Comment.COMMENT_EMAIL, "dl88250@gmail.com");
		comment.put(Comment.COMMENT_URL, "https://hacpai.com/member/88250");
		comment.put(Comment.COMMENT_CONTENT, langPropsService.get("helloWorld.comment.content"));
		comment.put(Comment.COMMENT_ORIGINAL_COMMENT_ID, "");
		comment.put(Comment.COMMENT_ORIGINAL_COMMENT_NAME, "");
		comment.put(Comment.COMMENT_THUMBNAIL_URL, Thumbnails.GRAVATAR + "59a5e8209c780307dbe9c9ba728073f5??s=60&r=G");
		comment.put(Comment.COMMENT_DATE, date);
		comment.put(Comment.COMMENT_ON_ID, articleId);
		comment.put(Comment.COMMENT_ON_TYPE, Article.ARTICLE);
		final String commentId = Ids.genTimeMillisId();

		comment.put(Keys.OBJECT_ID, commentId);
		final String commentSharpURL = Comments.getCommentSharpURLForArticle(article, commentId);

		comment.put(Comment.COMMENT_SHARP_URL, commentSharpURL);

		commentDao.add(comment);

		logger.info("Hello World!");
	}

	/**
	 * Adds the specified "Hello World" article.
	 *
	 * @param article
	 *            the specified "Hello World" article
	 * @return generated article id
	 * @throws RepositoryException
	 *             repository exception
	 */
	private String addHelloWorldArticle(final JSONObject article) throws RepositoryException {
		final String ret = Ids.genTimeMillisId();

		try {
			article.put(Keys.OBJECT_ID, ret);

			// Step 1: Add tags
			final String tagsString = article.optString(Article.ARTICLE_TAGS_REF);
			final String[] tagTitles = tagsString.split(",");
			final JSONArray tags = tag(tagTitles, article);

			// Step 2: Add tag-article relations
			addTagArticleRelation(tags, article);
			// Step 3: Inc blog article and comment count statictis
			final JSONObject statistic = statisticDao.get(Statistic.STATISTIC);

			statistic.put(Statistic.STATISTIC_BLOG_ARTICLE_COUNT, 1);
			statistic.put(Statistic.STATISTIC_PUBLISHED_ARTICLE_COUNT, 1);
			statistic.put(Statistic.STATISTIC_PUBLISHED_BLOG_COMMENT_COUNT, 1);
			statistic.put(Statistic.STATISTIC_BLOG_COMMENT_COUNT, 1);
			statisticDao.update(Statistic.STATISTIC, statistic);
			// Step 4: Add archive date-article relations
			archiveDate(article);
			// Step 5: Add article
			articleDao.add(article);
			// Step 6: Update admin user for article statistic
			final JSONObject admin = userDao.getAdmin();

			admin.put(UserExt.USER_ARTICLE_COUNT, 1);
			admin.put(UserExt.USER_PUBLISHED_ARTICLE_COUNT, 1);
			userDao.update(admin.optString(Keys.OBJECT_ID), admin);
		} catch (final RepositoryException e) {
			logger.error("Adds an article failed", e);

			throw new RepositoryException(e);
		}

		return ret;
	}

	/**
	 * Archive the create date with the specified article.
	 *
	 * @param article
	 *            the specified article, for example,
	 * 
	 *            <pre>
	 * {
	 *     ....,
	 *     "oId": "",
	 *     "articleCreateDate": java.util.Date,
	 *     ....
	 * }
	 *            </pre>
	 *
	 * @throws RepositoryException
	 *             repository exception
	 */
	public void archiveDate(final JSONObject article) throws RepositoryException {
		final Date createDate = (Date) article.opt(Article.ARTICLE_CREATE_DATE);
		final String createDateString = DateFormatUtils.format(createDate, "yyyy/MM");
		final JSONObject archiveDate = new JSONObject();

		try {
			archiveDate.put(ArchiveDate.ARCHIVE_TIME,
					DateUtils.parseDate(createDateString, new String[] { "yyyy/MM" }).getTime());
			archiveDate.put(ArchiveDate.ARCHIVE_DATE_ARTICLE_COUNT, 1);
			archiveDate.put(ArchiveDate.ARCHIVE_DATE_PUBLISHED_ARTICLE_COUNT, 1);

			archiveDateDao.add(archiveDate);
		} catch (final ParseException e) {
			logger.error(e.getMessage(), e);
			throw new RepositoryException(e);
		}

		final JSONObject archiveDateArticleRelation = new JSONObject();

		archiveDateArticleRelation.put(ArchiveDate.ARCHIVE_DATE + "_" + Keys.OBJECT_ID,
				archiveDate.optString(Keys.OBJECT_ID));
		archiveDateArticleRelation.put(Article.ARTICLE + "_" + Keys.OBJECT_ID, article.optString(Keys.OBJECT_ID));

		archiveDateArticleDao.add(archiveDateArticleRelation);
	}

	/**
	 * Adds relation of the specified tags and article.
	 *
	 * @param tags
	 *            the specified tags
	 * @param article
	 *            the specified article
	 * @throws RepositoryException
	 *             repository exception
	 */
	private void addTagArticleRelation(final JSONArray tags, final JSONObject article) throws RepositoryException {
		for (int i = 0; i < tags.length(); i++) {
			final JSONObject tag = tags.optJSONObject(i);
			final JSONObject tagArticleRelation = new JSONObject();

			tagArticleRelation.put(Tag.TAG + "_" + Keys.OBJECT_ID, tag.optString(Keys.OBJECT_ID));
			tagArticleRelation.put(Article.ARTICLE + "_" + Keys.OBJECT_ID, article.optString(Keys.OBJECT_ID));

			tagArticleDao.add(tagArticleRelation);
		}
	}

	/**
	 * Tags the specified article with the specified tag titles.
	 *
	 * @param tagTitles
	 *            the specified tag titles
	 * @param article
	 *            the specified article
	 * @return an array of tags
	 * @throws RepositoryException
	 *             repository exception
	 */
	private JSONArray tag(final String[] tagTitles, final JSONObject article) throws RepositoryException {
		final JSONArray ret = new JSONArray();

		for (int i = 0; i < tagTitles.length; i++) {
			final String tagTitle = tagTitles[i].trim();
			final JSONObject tag = new JSONObject();

			logger.trace("Found a new tag[title={0}] in article[title={1}]", tagTitle,
					article.optString(Article.ARTICLE_TITLE));
			tag.put(Tag.TAG_TITLE, tagTitle);
			tag.put(Tag.TAG_REFERENCE_COUNT, 1);
			tag.put(Tag.TAG_PUBLISHED_REFERENCE_COUNT, 1);

			final String tagId = tagDao.add(tag);

			tag.put(Keys.OBJECT_ID, tagId);

			ret.put(tag);
		}

		return ret;
	}

	/**
	 * Initializes administrator with the specified request json object, and
	 * then logins it.
	 *
	 * @param requestJSONObject
	 *            the specified request json object, for example,
	 * 
	 *            <pre>
	 * {
	 *     "userName": "",
	 *     "userEmail": "",
	 *     "userPassowrd": "" // Unhashed
	 * }
	 *            </pre>
	 *
	 * @throws Exception
	 *             exception
	 */
	private void initAdmin(final JSONObject requestJSONObject) throws Exception {
		logger.debug("Initializing admin....");
		final JSONObject admin = new JSONObject();

		admin.put(User.USER_NAME, requestJSONObject.getString(User.USER_NAME));
		admin.put(User.USER_EMAIL, requestJSONObject.getString(User.USER_EMAIL));
		admin.put(User.USER_URL, Latkes.getServePath());
		admin.put(User.USER_ROLE, Role.ADMIN_ROLE);
		admin.put(User.USER_PASSWORD, MD5.hash(requestJSONObject.getString(User.USER_PASSWORD)));
		admin.put(UserExt.USER_ARTICLE_COUNT, 0);
		admin.put(UserExt.USER_PUBLISHED_ARTICLE_COUNT, 0);
		admin.put(UserExt.USER_AVATAR, Thumbnails.getGravatarURL(requestJSONObject.getString(User.USER_EMAIL), "128"));

		userDao.add(admin);

		logger.debug("Initialized admin");
	}

	/**
	 * Initializes link.
	 *
	 * @throws Exception
	 *             exception
	 */
	private void initLink() throws Exception {
		final JSONObject link = new JSONObject();

		link.put(Link.LINK_TITLE, "黑客派");
		link.put(Link.LINK_ADDRESS, "https://hacpai.com");
		link.put(Link.LINK_DESCRIPTION, "黑客与画家的社区");

		final int maxOrder = linkDao.getMaxOrder();

		link.put(Link.LINK_ORDER, maxOrder + 1);
		linkDao.add(link);
	}

	/**
	 * Initializes statistic.
	 *
	 * @throws RepositoryException
	 *             repository exception
	 * @throws JSONException
	 *             json exception
	 */
	private void initStatistic() throws RepositoryException, JSONException {
		logger.debug("Initializing statistic....");
		final JSONObject statistic = new JSONObject();

		statistic.put(Keys.OBJECT_ID, Statistic.STATISTIC);
		statistic.put(Statistic.STATISTIC_BLOG_ARTICLE_COUNT, 0);
		statistic.put(Statistic.STATISTIC_PUBLISHED_ARTICLE_COUNT, 0);
		statistic.put(Statistic.STATISTIC_BLOG_VIEW_COUNT, 0);
		statistic.put(Statistic.STATISTIC_BLOG_COMMENT_COUNT, 0);
		statistic.put(Statistic.STATISTIC_PUBLISHED_BLOG_COMMENT_COUNT, 0);
		statisticDao.add(statistic);

		logger.debug("Initialized statistic");
	}

	/**
	 * Initializes reply notification template.
	 *
	 * @throws Exception
	 *             exception
	 */
	private void initReplyNotificationTemplate() throws Exception {
		logger.debug("Initializing reply notification template");

		final JSONObject replyNotificationTemplate = new JSONObject(
				DefaultPreference.DEFAULT_REPLY_NOTIFICATION_TEMPLATE);

		replyNotificationTemplate.put(Keys.OBJECT_ID, "replyNotificationTemplate");

		final JSONObject subjectOpt = new JSONObject();
		subjectOpt.put(Keys.OBJECT_ID, Option.ID_C_REPLY_NOTI_TPL_SUBJECT);
		subjectOpt.put(Option.OPTION_CATEGORY, Option.CATEGORY_C_PREFERENCE);
		subjectOpt.put(Option.OPTION_VALUE, replyNotificationTemplate.optString("subject"));
		optionRepository.add(subjectOpt);

		final JSONObject bodyOpt = new JSONObject();
		bodyOpt.put(Keys.OBJECT_ID, Option.ID_C_REPLY_NOTI_TPL_BODY);
		bodyOpt.put(Option.OPTION_CATEGORY, Option.CATEGORY_C_PREFERENCE);
		bodyOpt.put(Option.OPTION_VALUE, replyNotificationTemplate.optString("body"));
		optionRepository.add(bodyOpt);

		logger.debug("Initialized reply notification template");
	}

	/**
	 * Initializes preference.
	 *
	 * @param requestJSONObject
	 *            the specified json object
	 * @throws Exception
	 *             exception
	 */
	private void initPreference(final JSONObject requestJSONObject) throws Exception {
		logger.debug("Initializing preference....");

		final JSONObject noticeBoardOpt = new JSONObject();
		noticeBoardOpt.put(Keys.OBJECT_ID, Option.ID_C_NOTICE_BOARD);
		noticeBoardOpt.put(Option.OPTION_CATEGORY, Option.CATEGORY_C_PREFERENCE);
		noticeBoardOpt.put(Option.OPTION_VALUE, DefaultPreference.DEFAULT_NOTICE_BOARD);
		optionRepository.add(noticeBoardOpt);

		final JSONObject metaDescriptionOpt = new JSONObject();
		metaDescriptionOpt.put(Keys.OBJECT_ID, Option.ID_C_META_DESCRIPTION);
		metaDescriptionOpt.put(Option.OPTION_CATEGORY, Option.CATEGORY_C_PREFERENCE);
		metaDescriptionOpt.put(Option.OPTION_VALUE, DefaultPreference.DEFAULT_META_DESCRIPTION);
		optionRepository.add(metaDescriptionOpt);

		final JSONObject metaKeywordsOpt = new JSONObject();
		metaKeywordsOpt.put(Keys.OBJECT_ID, Option.ID_C_META_KEYWORDS);
		metaKeywordsOpt.put(Option.OPTION_CATEGORY, Option.CATEGORY_C_PREFERENCE);
		metaKeywordsOpt.put(Option.OPTION_VALUE, DefaultPreference.DEFAULT_META_KEYWORDS);
		optionRepository.add(metaKeywordsOpt);

		final JSONObject htmlHeadOpt = new JSONObject();
		htmlHeadOpt.put(Keys.OBJECT_ID, Option.ID_C_HTML_HEAD);
		htmlHeadOpt.put(Option.OPTION_CATEGORY, Option.CATEGORY_C_PREFERENCE);
		htmlHeadOpt.put(Option.OPTION_VALUE, DefaultPreference.DEFAULT_HTML_HEAD);
		optionRepository.add(htmlHeadOpt);

		final JSONObject relevantArticlesDisplayCountOpt = new JSONObject();
		relevantArticlesDisplayCountOpt.put(Keys.OBJECT_ID, Option.ID_C_RELEVANT_ARTICLES_DISPLAY_CNT);
		relevantArticlesDisplayCountOpt.put(Option.OPTION_CATEGORY, Option.CATEGORY_C_PREFERENCE);
		relevantArticlesDisplayCountOpt.put(Option.OPTION_VALUE,
				DefaultPreference.DEFAULT_RELEVANT_ARTICLES_DISPLAY_COUNT);
		optionRepository.add(relevantArticlesDisplayCountOpt);

		final JSONObject randomArticlesDisplayCountOpt = new JSONObject();
		randomArticlesDisplayCountOpt.put(Keys.OBJECT_ID, Option.ID_C_RANDOM_ARTICLES_DISPLAY_CNT);
		randomArticlesDisplayCountOpt.put(Option.OPTION_CATEGORY, Option.CATEGORY_C_PREFERENCE);
		randomArticlesDisplayCountOpt.put(Option.OPTION_VALUE, DefaultPreference.DEFAULT_RANDOM_ARTICLES_DISPLAY_COUNT);
		optionRepository.add(randomArticlesDisplayCountOpt);

		final JSONObject externalRelevantArticlesDisplayCountOpt = new JSONObject();
		externalRelevantArticlesDisplayCountOpt.put(Keys.OBJECT_ID, Option.ID_C_EXTERNAL_RELEVANT_ARTICLES_DISPLAY_CNT);
		externalRelevantArticlesDisplayCountOpt.put(Option.OPTION_CATEGORY, Option.CATEGORY_C_PREFERENCE);
		externalRelevantArticlesDisplayCountOpt.put(Option.OPTION_VALUE,
				DefaultPreference.DEFAULT_EXTERNAL_RELEVANT_ARTICLES_DISPLAY_COUNT);
		optionRepository.add(externalRelevantArticlesDisplayCountOpt);

		final JSONObject mostViewArticleDisplayCountOpt = new JSONObject();
		mostViewArticleDisplayCountOpt.put(Keys.OBJECT_ID, Option.ID_C_MOST_VIEW_ARTICLE_DISPLAY_CNT);
		mostViewArticleDisplayCountOpt.put(Option.OPTION_CATEGORY, Option.CATEGORY_C_PREFERENCE);
		mostViewArticleDisplayCountOpt.put(Option.OPTION_VALUE,
				DefaultPreference.DEFAULT_MOST_VIEW_ARTICLES_DISPLAY_COUNT);
		optionRepository.add(mostViewArticleDisplayCountOpt);

		final JSONObject articleListDisplayCountOpt = new JSONObject();
		articleListDisplayCountOpt.put(Keys.OBJECT_ID, Option.ID_C_ARTICLE_LIST_DISPLAY_COUNT);
		articleListDisplayCountOpt.put(Option.OPTION_CATEGORY, Option.CATEGORY_C_PREFERENCE);
		articleListDisplayCountOpt.put(Option.OPTION_VALUE, DefaultPreference.DEFAULT_ARTICLE_LIST_DISPLAY_COUNT);
		optionRepository.add(articleListDisplayCountOpt);

		final JSONObject articleListPaginationWindowSizeOpt = new JSONObject();
		articleListPaginationWindowSizeOpt.put(Keys.OBJECT_ID, Option.ID_C_ARTICLE_LIST_PAGINATION_WINDOW_SIZE);
		articleListPaginationWindowSizeOpt.put(Option.OPTION_CATEGORY, Option.CATEGORY_C_PREFERENCE);
		articleListPaginationWindowSizeOpt.put(Option.OPTION_VALUE,
				DefaultPreference.DEFAULT_ARTICLE_LIST_PAGINATION_WINDOW_SIZE);
		optionRepository.add(articleListPaginationWindowSizeOpt);

		final JSONObject mostUsedTagDisplayCountOpt = new JSONObject();
		mostUsedTagDisplayCountOpt.put(Keys.OBJECT_ID, Option.ID_C_MOST_USED_TAG_DISPLAY_CNT);
		mostUsedTagDisplayCountOpt.put(Option.OPTION_CATEGORY, Option.CATEGORY_C_PREFERENCE);
		mostUsedTagDisplayCountOpt.put(Option.OPTION_VALUE, DefaultPreference.DEFAULT_MOST_USED_TAG_DISPLAY_COUNT);
		optionRepository.add(mostUsedTagDisplayCountOpt);

		final JSONObject mostCommentArticleDisplayCountOpt = new JSONObject();
		mostCommentArticleDisplayCountOpt.put(Keys.OBJECT_ID, Option.ID_C_MOST_COMMENT_ARTICLE_DISPLAY_CNT);
		mostCommentArticleDisplayCountOpt.put(Option.OPTION_CATEGORY, Option.CATEGORY_C_PREFERENCE);
		mostCommentArticleDisplayCountOpt.put(Option.OPTION_VALUE,
				DefaultPreference.DEFAULT_MOST_COMMENT_ARTICLE_DISPLAY_COUNT);
		optionRepository.add(mostCommentArticleDisplayCountOpt);

		final JSONObject recentArticleDisplayCountOpt = new JSONObject();
		recentArticleDisplayCountOpt.put(Keys.OBJECT_ID, Option.ID_C_RECENT_ARTICLE_DISPLAY_CNT);
		recentArticleDisplayCountOpt.put(Option.OPTION_CATEGORY, Option.CATEGORY_C_PREFERENCE);
		recentArticleDisplayCountOpt.put(Option.OPTION_VALUE, DefaultPreference.DEFAULT_RECENT_ARTICLE_DISPLAY_COUNT);
		optionRepository.add(recentArticleDisplayCountOpt);

		final JSONObject recentCommentDisplayCountOpt = new JSONObject();
		recentCommentDisplayCountOpt.put(Keys.OBJECT_ID, Option.ID_C_RECENT_COMMENT_DISPLAY_CNT);
		recentCommentDisplayCountOpt.put(Option.OPTION_CATEGORY, Option.CATEGORY_C_PREFERENCE);
		recentCommentDisplayCountOpt.put(Option.OPTION_VALUE, DefaultPreference.DEFAULT_RECENT_COMMENT_DISPLAY_COUNT);
		optionRepository.add(recentCommentDisplayCountOpt);

		final JSONObject blogTitleOpt = new JSONObject();
		blogTitleOpt.put(Keys.OBJECT_ID, Option.ID_C_BLOG_TITLE);
		blogTitleOpt.put(Option.OPTION_CATEGORY, Option.CATEGORY_C_PREFERENCE);
		blogTitleOpt.put(Option.OPTION_VALUE, DefaultPreference.DEFAULT_BLOG_TITLE);
		optionRepository.add(blogTitleOpt);

		final JSONObject blogSubtitleOpt = new JSONObject();
		blogSubtitleOpt.put(Keys.OBJECT_ID, Option.ID_C_BLOG_SUBTITLE);
		blogSubtitleOpt.put(Option.OPTION_CATEGORY, Option.CATEGORY_C_PREFERENCE);
		blogSubtitleOpt.put(Option.OPTION_VALUE, DefaultPreference.DEFAULT_BLOG_SUBTITLE);
		optionRepository.add(blogSubtitleOpt);

		final JSONObject adminEmailOpt = new JSONObject();
		adminEmailOpt.put(Keys.OBJECT_ID, Option.ID_C_ADMIN_EMAIL);
		adminEmailOpt.put(Option.OPTION_CATEGORY, Option.CATEGORY_C_PREFERENCE);
		adminEmailOpt.put(Option.OPTION_VALUE, requestJSONObject.getString(User.USER_EMAIL));
		optionRepository.add(adminEmailOpt);

		final JSONObject localeStringOpt = new JSONObject();
		localeStringOpt.put(Keys.OBJECT_ID, Option.ID_C_LOCALE_STRING);
		localeStringOpt.put(Option.OPTION_CATEGORY, Option.CATEGORY_C_PREFERENCE);
		localeStringOpt.put(Option.OPTION_VALUE, DefaultPreference.DEFAULT_LANGUAGE);
		optionRepository.add(localeStringOpt);

		final JSONObject enableArticleUpdateHintOpt = new JSONObject();
		enableArticleUpdateHintOpt.put(Keys.OBJECT_ID, Option.ID_C_ENABLE_ARTICLE_UPDATE_HINT);
		enableArticleUpdateHintOpt.put(Option.OPTION_CATEGORY, Option.CATEGORY_C_PREFERENCE);
		enableArticleUpdateHintOpt.put(Option.OPTION_VALUE, DefaultPreference.DEFAULT_ENABLE_ARTICLE_UPDATE_HINT);
		optionRepository.add(enableArticleUpdateHintOpt);

		final JSONObject signsOpt = new JSONObject();
		signsOpt.put(Keys.OBJECT_ID, Option.ID_C_SIGNS);
		signsOpt.put(Option.OPTION_CATEGORY, Option.CATEGORY_C_PREFERENCE);
		signsOpt.put(Option.OPTION_VALUE, DefaultPreference.DEFAULT_SIGNS);
		optionRepository.add(signsOpt);

		final JSONObject timeZoneIdOpt = new JSONObject();
		timeZoneIdOpt.put(Keys.OBJECT_ID, Option.ID_C_TIME_ZONE_ID);
		timeZoneIdOpt.put(Option.OPTION_CATEGORY, Option.CATEGORY_C_PREFERENCE);
		timeZoneIdOpt.put(Option.OPTION_VALUE, DefaultPreference.DEFAULT_TIME_ZONE);
		optionRepository.add(timeZoneIdOpt);

		final JSONObject allowVisitDraftViaPermalinkOpt = new JSONObject();
		allowVisitDraftViaPermalinkOpt.put(Keys.OBJECT_ID, Option.ID_C_ALLOW_VISIT_DRAFT_VIA_PERMALINK);
		allowVisitDraftViaPermalinkOpt.put(Option.OPTION_CATEGORY, Option.CATEGORY_C_PREFERENCE);
		allowVisitDraftViaPermalinkOpt.put(Option.OPTION_VALUE,
				DefaultPreference.DEFAULT_ALLOW_VISIT_DRAFT_VIA_PERMALINK);
		optionRepository.add(allowVisitDraftViaPermalinkOpt);

		final JSONObject allowRegisterOpt = new JSONObject();
		allowRegisterOpt.put(Keys.OBJECT_ID, Option.ID_C_ALLOW_REGISTER);
		allowRegisterOpt.put(Option.OPTION_CATEGORY, Option.CATEGORY_C_PREFERENCE);
		allowRegisterOpt.put(Option.OPTION_VALUE, DefaultPreference.DEFAULT_ALLOW_REGISTER);
		optionRepository.add(allowRegisterOpt);

		final JSONObject commentableOpt = new JSONObject();
		commentableOpt.put(Keys.OBJECT_ID, Option.ID_C_COMMENTABLE);
		commentableOpt.put(Option.OPTION_CATEGORY, Option.CATEGORY_C_PREFERENCE);
		commentableOpt.put(Option.OPTION_VALUE, DefaultPreference.DEFAULT_COMMENTABLE);
		optionRepository.add(commentableOpt);

		final JSONObject versionOpt = new JSONObject();
		versionOpt.put(Keys.OBJECT_ID, Option.ID_C_VERSION);
		versionOpt.put(Option.OPTION_CATEGORY, Option.CATEGORY_C_PREFERENCE);
		versionOpt.put(Option.OPTION_VALUE, SoloConstant.VERSION);
		optionRepository.add(versionOpt);

		final JSONObject articleListStyleOpt = new JSONObject();
		articleListStyleOpt.put(Keys.OBJECT_ID, Option.ID_C_ARTICLE_LIST_STYLE);
		articleListStyleOpt.put(Option.OPTION_CATEGORY, Option.CATEGORY_C_PREFERENCE);
		articleListStyleOpt.put(Option.OPTION_VALUE, DefaultPreference.DEFAULT_ARTICLE_LIST_STYLE);
		optionRepository.add(articleListStyleOpt);

		final JSONObject keyOfSoloOpt = new JSONObject();
		keyOfSoloOpt.put(Keys.OBJECT_ID, Option.ID_C_KEY_OF_SOLO);
		keyOfSoloOpt.put(Option.OPTION_CATEGORY, Option.CATEGORY_C_PREFERENCE);
		keyOfSoloOpt.put(Option.OPTION_VALUE, Ids.genTimeMillisId());
		optionRepository.add(keyOfSoloOpt);

		final JSONObject feedOutputModeOpt = new JSONObject();
		feedOutputModeOpt.put(Keys.OBJECT_ID, Option.ID_C_FEED_OUTPUT_MODE);
		feedOutputModeOpt.put(Option.OPTION_CATEGORY, Option.CATEGORY_C_PREFERENCE);
		feedOutputModeOpt.put(Option.OPTION_VALUE, DefaultPreference.DEFAULT_FEED_OUTPUT_MODE);
		optionRepository.add(feedOutputModeOpt);

		final JSONObject feedOutputCntOpt = new JSONObject();
		feedOutputCntOpt.put(Keys.OBJECT_ID, Option.ID_C_FEED_OUTPUT_CNT);
		feedOutputCntOpt.put(Option.OPTION_CATEGORY, Option.CATEGORY_C_PREFERENCE);
		feedOutputCntOpt.put(Option.OPTION_VALUE, DefaultPreference.DEFAULT_FEED_OUTPUT_CNT);
		optionRepository.add(feedOutputCntOpt);

		final JSONObject editorTypeOpt = new JSONObject();
		editorTypeOpt.put(Keys.OBJECT_ID, Option.ID_C_EDITOR_TYPE);
		editorTypeOpt.put(Option.OPTION_CATEGORY, Option.CATEGORY_C_PREFERENCE);
		editorTypeOpt.put(Option.OPTION_VALUE, DefaultPreference.DEFAULT_EDITOR_TYPE);
		optionRepository.add(editorTypeOpt);

		final JSONObject footerContentOpt = new JSONObject();
		footerContentOpt.put(Keys.OBJECT_ID, Option.ID_C_FOOTER_CONTENT);
		footerContentOpt.put(Option.OPTION_CATEGORY, Option.CATEGORY_C_PREFERENCE);
		footerContentOpt.put(Option.OPTION_VALUE, DefaultPreference.DEFAULT_FOOTER_CONTENT);
		optionRepository.add(footerContentOpt);

		final String skinDirName = DefaultPreference.DEFAULT_SKIN_DIR_NAME;
		final JSONObject skinDirNameOpt = new JSONObject();
		skinDirNameOpt.put(Keys.OBJECT_ID, Option.ID_C_SKIN_DIR_NAME);
		skinDirNameOpt.put(Option.OPTION_CATEGORY, Option.CATEGORY_C_PREFERENCE);
		skinDirNameOpt.put(Option.OPTION_VALUE, skinDirName);
		optionRepository.add(skinDirNameOpt);

		final String skinName = Latkes.getSkinName(skinDirName);
		final JSONObject skinNameOpt = new JSONObject();
		skinNameOpt.put(Keys.OBJECT_ID, Option.ID_C_SKIN_NAME);
		skinNameOpt.put(Option.OPTION_CATEGORY, Option.CATEGORY_C_PREFERENCE);
		skinNameOpt.put(Option.OPTION_VALUE, skinName);
		optionRepository.add(skinNameOpt);

		final Set<String> skinDirNames = Skins.getSkinDirNames();
		final JSONArray skinArray = new JSONArray();
		for (final String dirName : skinDirNames) {
			final JSONObject skin = new JSONObject();
			skinArray.put(skin);

			final String name = Latkes.getSkinName(dirName);
			skin.put(Skin.SKIN_NAME, name);
			skin.put(Skin.SKIN_DIR_NAME, dirName);
		}

		final JSONObject skinsOpt = new JSONObject();
		skinsOpt.put(Keys.OBJECT_ID, Option.ID_C_SKINS);
		skinsOpt.put(Option.OPTION_CATEGORY, Option.CATEGORY_C_PREFERENCE);
		skinsOpt.put(Option.OPTION_VALUE, skinArray.toString());
		optionRepository.add(skinsOpt);

		final ServletContext servletContext = ContextLoader.getCurrentWebApplicationContext().getServletContext();

		Templates.MAIN_CFG.setServletContextForTemplateLoading(servletContext, "/skins/" + skinDirName);

		TimeZones.setTimeZone(INIT_TIME_ZONE_ID);

		logger.debug("Initialized preference");
	}

	/**
	 * Sets archive date article repository with the specified archive date
	 * article repository.
	 *
	 * @param archiveDateArticleDao
	 *            the specified archive date article repository
	 */
	public void setArchiveDateArticleRepository(final ArchiveDateArticleDao archiveDateArticleDao) {
		this.archiveDateArticleDao = archiveDateArticleDao;
	}

	/**
	 * Sets archive date repository with the specified archive date repository.
	 *
	 * @param archiveDateDao
	 *            the specified archive date repository
	 */
	public void setArchiveDateRepository(final ArchiveDateDao archiveDateDao) {
		this.archiveDateDao = archiveDateDao;
	}

	/**
	 * Sets the article repository with the specified article repository.
	 *
	 * @param articleDao
	 *            the specified article repository
	 */
	public void setArticleRepository(final ArticleDao articleDao) {
		this.articleDao = articleDao;
	}

	/**
	 * Sets the user repository with the specified user repository.
	 *
	 * @param userDao
	 *            the specified user repository
	 */
	public void setUserRepository(final UserDao userDao) {
		this.userDao = userDao;
	}

	/**
	 * Sets the statistic repository with the specified statistic repository.
	 *
	 * @param statisticDao
	 *            the specified statistic repository
	 */
	public void setStatisticRepository(final StatisticDao statisticDao) {
		this.statisticDao = statisticDao;
	}

	/**
	 * Sets the tag repository with the specified tag repository.
	 *
	 * @param tagDao
	 *            the specified tag repository
	 */
	public void setTagRepository(final TagDao tagDao) {
		this.tagDao = tagDao;
	}

	/**
	 * Sets the tag article repository with the specified tag article
	 * repository.
	 *
	 * @param tagArticleDao
	 *            the specified tag article repository
	 */
	public void setTagArticleRepository(final TagArticleDao tagArticleDao) {
		this.tagArticleDao = tagArticleDao;
	}

	/**
	 * Sets the comment repository with the specified comment repository.
	 *
	 * @param commentDao
	 *            the specified comment repository
	 */
	public void setCommentRepository(final CommentDao commentDao) {
		this.commentDao = commentDao;
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

	/**
	 * Sets the plugin manager with the specified plugin manager.
	 *
	 * @param pluginManager
	 *            the specified plugin manager
	 */
	// public void setPluginManager(final PluginManager pluginManager) {
	// this.pluginManager = pluginManager;
	// }
}
