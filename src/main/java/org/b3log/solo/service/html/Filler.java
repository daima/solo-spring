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
package org.b3log.solo.service.html;

import static org.b3log.solo.model.Article.ARTICLE_CONTENT;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.b3log.solo.Keys;
import org.b3log.solo.Latkes;
import org.b3log.solo.SoloConstant;
import org.b3log.solo.dao.ArchiveDateDao;
import org.b3log.solo.dao.ArticleDao;
import org.b3log.solo.dao.CategoryDao;
import org.b3log.solo.dao.CommentDao;
import org.b3log.solo.dao.LinkDao;
import org.b3log.solo.dao.PageDao;
import org.b3log.solo.dao.TagDao;
import org.b3log.solo.dao.UserDao;
import org.b3log.solo.dao.repository.FilterOperator;
import org.b3log.solo.dao.repository.PropertyFilter;
import org.b3log.solo.dao.repository.Query;
import org.b3log.solo.dao.repository.RepositoryException;
import org.b3log.solo.dao.repository.SortDirection;
import org.b3log.solo.frame.event.Event;
import org.b3log.solo.frame.event.EventException;
import org.b3log.solo.frame.plugin.ViewLoadEventData;
import org.b3log.solo.model.ArchiveDate;
import org.b3log.solo.model.Article;
import org.b3log.solo.model.Comment;
import org.b3log.solo.model.Common;
import org.b3log.solo.model.Link;
import org.b3log.solo.model.Option;
import org.b3log.solo.model.Page;
import org.b3log.solo.model.Pagination;
import org.b3log.solo.model.Plugin;
import org.b3log.solo.model.Skin;
import org.b3log.solo.model.Statistic;
import org.b3log.solo.model.Tag;
import org.b3log.solo.model.User;
import org.b3log.solo.model.UserExt;
import org.b3log.solo.module.plugin.ViewLoadEventHandler;
import org.b3log.solo.module.util.Thumbnails;
import org.b3log.solo.service.ArticleQueryService;
import org.b3log.solo.service.LangPropsService;
import org.b3log.solo.service.OptionQueryService;
import org.b3log.solo.service.ServiceException;
import org.b3log.solo.service.StatisticQueryService;
import org.b3log.solo.service.TagQueryService;
import org.b3log.solo.service.UserQueryService;
import org.b3log.solo.service.UserService;
import org.b3log.solo.util.CollectionUtils;
import org.b3log.solo.util.Dates;
import org.b3log.solo.util.Locales;
import org.b3log.solo.util.Paginator;
import org.b3log.solo.util.PropsUtil;
import org.b3log.solo.util.Stopwatchs;
import org.b3log.solo.util.comparator.Comparators;
import org.b3log.solo.util.freemarker.Templates;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import freemarker.template.Template;

/**
 * Filler utilities.
 *
 * @author <a href="http://cxy7.com">XyCai</a>
 * 
 * @version 1.6.12.13, Apr 8, 2017
 * @since 0.3.1
 */
@Service
public class Filler {

	/**
	 * Logger.
	 */
	private static Logger logger = LoggerFactory.getLogger(Filler.class);
	/**
	 * {@code true} for published.
	 */
	private static final boolean PUBLISHED = true;
	/**
	 * Topbar utilities.
	 */
	@Autowired
	private TopBars topBars;
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
	 * Archive date repository.
	 */
	@Autowired
	private ArchiveDateDao archiveDateDao;
	/**
	 * Category repository.
	 */
	@Autowired
	private CategoryDao categoryDao;
	/**
	 * Tag repository.
	 */
	@Autowired
	private TagDao tagDao;
	/**
	 * Link repository.
	 */
	@Autowired
	private LinkDao linkDao;
	/**
	 * Page repository.
	 */
	@Autowired
	private PageDao pageDao;
	/**
	 * Statistic query service.
	 */
	@Autowired
	private StatisticQueryService statisticQueryService;
	/**
	 * User repository.
	 */
	@Autowired
	private UserDao userDao;
	/**
	 * Option query service..
	 */
	@Autowired
	private OptionQueryService optionQueryService;
	/**
	 * Article query service.
	 */
	@Autowired
	private ArticleQueryService articleQueryService;
	/**
	 * Tag query service.
	 */
	@Autowired
	private TagQueryService tagQueryService;
	/**
	 * User query service.
	 */
	@Autowired
	private UserQueryService userQueryService;
	/**
	 * Fill tag article..
	 */
	@Autowired
	private FillTagArticles fillTagArticles;
	@Autowired
	private UserService userService;

	/**
	 * Language service.
	 */
	@Autowired
	private LangPropsService langPropsService;
	@Autowired
	private ViewLoadEventHandler viewLoadEventHandler;

	/**
	 * Fills articles in index.ftl.
	 *
	 * @param request
	 *            the specified HTTP servlet request
	 * @param dataModel
	 *            data model
	 * @param currentPageNum
	 *            current page number
	 * @param preference
	 *            the specified preference
	 * @throws ServiceException
	 *             service exception
	 */
	public void fillIndexArticles(final HttpServletRequest request, final Map<String, Object> dataModel,
			final int currentPageNum, final JSONObject preference) throws ServiceException {
		Stopwatchs.start("Fill Index Articles");

		try {
			final int pageSize = preference.getInt(Option.ID_C_ARTICLE_LIST_DISPLAY_COUNT);
			final int windowSize = preference.getInt(Option.ID_C_ARTICLE_LIST_PAGINATION_WINDOW_SIZE);

			final JSONObject statistic = statisticQueryService.getStatistic();
			final int publishedArticleCnt = statistic.getInt(Statistic.STATISTIC_PUBLISHED_ARTICLE_COUNT);
			final int pageCount = (int) Math.ceil((double) publishedArticleCnt / (double) pageSize);

			final Query query = new Query().setCurrentPageNum(currentPageNum).setPageSize(pageSize)
					.setPageCount(pageCount)
					.setFilter(new PropertyFilter(Article.ARTICLE_IS_PUBLISHED, FilterOperator.EQUAL, PUBLISHED));

			final Template template = Templates.getTemplate((String) request.getAttribute(Keys.TEMAPLTE_DIR_NAME),
					"index.ftl");

			boolean isArticles1 = false;

			if (null == template) {
				logger.debug("The skin dose not contain [index.ftl] template");
			} else // See https://github.com/b3log/solo/issues/179 for more
					// details
			if (Templates.hasExpression(template, "<#list articles1 as article>")) {
				isArticles1 = true;
				query.addSort(Article.ARTICLE_CREATE_DATE, SortDirection.DESCENDING);

				logger.trace("Query ${articles1} in index.ftl");
			} else { // <#list articles as article>
				query.addSort(Article.ARTICLE_PUT_TOP, SortDirection.DESCENDING);
				if (preference.getBoolean(Option.ID_C_ENABLE_ARTICLE_UPDATE_HINT)) {
					query.addSort(Article.ARTICLE_UPDATE_DATE, SortDirection.DESCENDING);
				} else {
					query.addSort(Article.ARTICLE_CREATE_DATE, SortDirection.DESCENDING);
				}
			}

			query.index(Article.ARTICLE_PERMALINK);

			final JSONObject result = articleDao.get(query);
			final List<Integer> pageNums = Paginator.paginate(currentPageNum, pageSize, pageCount, windowSize);

			if (0 != pageNums.size()) {
				dataModel.put(Pagination.PAGINATION_FIRST_PAGE_NUM, pageNums.get(0));
				dataModel.put(Pagination.PAGINATION_LAST_PAGE_NUM, pageNums.get(pageNums.size() - 1));
			}

			dataModel.put(Pagination.PAGINATION_PAGE_COUNT, pageCount);
			dataModel.put(Pagination.PAGINATION_PAGE_NUMS, pageNums);

			final List<JSONObject> articles = org.b3log.solo.util.CollectionUtils
					.jsonArrayToList(result.getJSONArray(Keys.RESULTS));

			final boolean hasMultipleUsers = userQueryService.hasMultipleUsers();

			if (hasMultipleUsers) {
				setArticlesExProperties(request, articles, preference);
			} else if (!articles.isEmpty()) {
				final JSONObject author = articleQueryService.getAuthor(articles.get(0));

				setArticlesExProperties(request, articles, author, preference);
			}

			if (!isArticles1) {
				dataModel.put(Article.ARTICLES, articles);
			} else {
				dataModel.put(Article.ARTICLES + "1", articles);
			}
		} catch (final JSONException e) {
			logger.error("Fills index articles failed", e);
			throw new ServiceException(e);
		} catch (final RepositoryException e) {
			logger.error("Fills index articles failed", e);
			throw new ServiceException(e);
		} finally {
			Stopwatchs.end();
		}
	}

	/**
	 * Fills links.
	 *
	 * @param dataModel
	 *            data model
	 * @throws ServiceException
	 *             service exception
	 */
	public void fillLinks(final Map<String, Object> dataModel) throws ServiceException {
		Stopwatchs.start("Fill Links");
		try {
			final Map<String, SortDirection> sorts = new HashMap<>();

			sorts.put(Link.LINK_ORDER, SortDirection.ASCENDING);
			final Query query = new Query().addSort(Link.LINK_ORDER, SortDirection.ASCENDING).setPageCount(1);
			final JSONObject linkResult = linkDao.get(query);
			final List<JSONObject> links = org.b3log.solo.util.CollectionUtils
					.jsonArrayToList(linkResult.getJSONArray(Keys.RESULTS));

			dataModel.put(Link.LINKS, links);
		} catch (final JSONException e) {
			logger.error("Fills links failed", e);
			throw new ServiceException(e);
		} catch (final RepositoryException e) {
			logger.error("Fills links failed", e);
			throw new ServiceException(e);
		} finally {
			Stopwatchs.end();
		}
		Stopwatchs.end();
	}

	/**
	 * Fills tags.
	 *
	 * @param dataModel
	 *            data model
	 * @throws ServiceException
	 *             service exception
	 */
	public void fillTags(final Map<String, Object> dataModel) throws ServiceException {
		Stopwatchs.start("Fill Tags");
		try {
			final List<JSONObject> tags = tagQueryService.getTags();

			tagQueryService.removeForUnpublishedArticles(tags);
			Collections.sort(tags, Comparators.TAG_REF_CNT_COMPARATOR);

			dataModel.put(Tag.TAGS, tags);
		} catch (final JSONException e) {
			logger.error("Fills tags failed", e);
			throw new ServiceException(e);
		} catch (final RepositoryException e) {
			logger.error("Fills tagss failed", e);
			throw new ServiceException(e);
		} finally {
			Stopwatchs.end();
		}

		Stopwatchs.end();
	}

	/**
	 * Fills most used categories.
	 *
	 * @param dataModel
	 *            data model
	 * @param preference
	 *            the specified preference
	 * @throws ServiceException
	 *             service exception
	 */
	public void fillMostUsedCategories(final Map<String, Object> dataModel, final JSONObject preference)
			throws ServiceException {
		Stopwatchs.start("Fill Most Used Categories");

		try {
			logger.debug("Filling most used categories....");
			final int mostUsedCategoryDisplayCnt = Integer.MAX_VALUE; // XXX:
																		// preference
																		// instead

			final List<JSONObject> categories = categoryDao.getMostUsedCategories(mostUsedCategoryDisplayCnt);

			dataModel.put(Common.MOST_USED_CATEGORIES, categories);
		} catch (final RepositoryException e) {
			logger.error("Fills most used categories failed", e);
			throw new ServiceException(e);
		} finally {
			Stopwatchs.end();
		}
	}

	/**
	 * Fills most used tags.
	 *
	 * @param dataModel
	 *            data model
	 * @param preference
	 *            the specified preference
	 * @throws ServiceException
	 *             service exception
	 */
	public void fillMostUsedTags(final Map<String, Object> dataModel, final JSONObject preference)
			throws ServiceException {
		Stopwatchs.start("Fill Most Used Tags");

		try {
			logger.debug("Filling most used tags....");
			final int mostUsedTagDisplayCnt = preference.getInt(Option.ID_C_MOST_USED_TAG_DISPLAY_CNT);

			final List<JSONObject> tags = tagDao.getMostUsedTags(mostUsedTagDisplayCnt);

			tagQueryService.removeForUnpublishedArticles(tags);

			dataModel.put(Common.MOST_USED_TAGS, tags);
		} catch (final JSONException e) {
			logger.error("Fills most used tags failed", e);
			throw new ServiceException(e);
		} catch (final RepositoryException e) {
			logger.error("Fills most used tags failed", e);
			throw new ServiceException(e);
		} finally {
			Stopwatchs.end();
		}
	}

	/**
	 * Fills archive dates.
	 *
	 * @param dataModel
	 *            data model
	 * @param preference
	 *            the specified preference
	 * @throws ServiceException
	 *             service exception
	 */
	public void fillArchiveDates(final Map<String, Object> dataModel, final JSONObject preference)
			throws ServiceException {
		Stopwatchs.start("Fill Archive Dates");

		try {
			logger.debug("Filling archive dates....");
			final List<JSONObject> archiveDates = archiveDateDao.getArchiveDates();
			final List<JSONObject> archiveDates2 = new ArrayList<>();

			dataModel.put(ArchiveDate.ARCHIVE_DATES, archiveDates2);

			if (archiveDates.isEmpty()) {
				return;
			}

			archiveDates2.add(archiveDates.get(0));

			if (1 < archiveDates.size()) { // XXX: Workaround, remove the
											// duplicated archive dates
				for (int i = 1; i < archiveDates.size(); i++) {
					final JSONObject archiveDate = archiveDates.get(i);

					final long time = archiveDate.getLong(ArchiveDate.ARCHIVE_TIME);
					final String dateString = DateFormatUtils.format(time, "yyyy/MM");

					final JSONObject last = archiveDates2.get(archiveDates2.size() - 1);
					final String lastDateString = DateFormatUtils.format(last.getLong(ArchiveDate.ARCHIVE_TIME),
							"yyyy/MM");

					if (!dateString.equals(lastDateString)) {
						archiveDates2.add(archiveDate);
					} else {
						logger.warn("Found a duplicated archive date [{}]", dateString);
					}
				}
			}

			final String localeString = preference.getString(Option.ID_C_LOCALE_STRING);
			final String language = Locales.getLanguage(localeString);

			for (final JSONObject archiveDate : archiveDates2) {
				final long time = archiveDate.getLong(ArchiveDate.ARCHIVE_TIME);
				final String dateString = DateFormatUtils.format(time, "yyyy/MM");
				final String[] dateStrings = dateString.split("/");
				final String year = dateStrings[0];
				final String month = dateStrings[1];

				archiveDate.put(ArchiveDate.ARCHIVE_DATE_YEAR, year);

				archiveDate.put(ArchiveDate.ARCHIVE_DATE_MONTH, month);
				if ("en".equals(language)) {
					final String monthName = Dates.EN_MONTHS.get(month);

					archiveDate.put(Common.MONTH_NAME, monthName);
				}
			}

			dataModel.put(ArchiveDate.ARCHIVE_DATES, archiveDates2);
		} catch (final JSONException e) {
			logger.error("Fills archive dates failed", e);
			throw new ServiceException(e);
		} catch (final RepositoryException e) {
			logger.error("Fills archive dates failed", e);
			throw new ServiceException(e);
		} finally {
			Stopwatchs.end();
		}
	}

	/**
	 * Fills most view count articles.
	 *
	 * @param dataModel
	 *            data model
	 * @param preference
	 *            the specified preference
	 * @throws ServiceException
	 *             service exception
	 */
	public void fillMostViewCountArticles(final Map<String, Object> dataModel, final JSONObject preference)
			throws ServiceException {
		Stopwatchs.start("Fill Most View Articles");
		try {
			logger.debug("Filling the most view count articles....");
			final int mostCommentArticleDisplayCnt = preference.getInt(Option.ID_C_MOST_VIEW_ARTICLE_DISPLAY_CNT);
			final List<JSONObject> mostViewCountArticles = articleDao
					.getMostViewCountArticles(mostCommentArticleDisplayCnt);

			dataModel.put(Common.MOST_VIEW_COUNT_ARTICLES, mostViewCountArticles);

		} catch (final Exception e) {
			logger.error("Fills most view count articles failed", e);
			throw new ServiceException(e);
		} finally {
			Stopwatchs.end();
		}
	}

	/**
	 * Fills most comments articles.
	 *
	 * @param dataModel
	 *            data model
	 * @param preference
	 *            the specified preference
	 * @throws ServiceException
	 *             service exception
	 */
	public void fillMostCommentArticles(final Map<String, Object> dataModel, final JSONObject preference)
			throws ServiceException {
		Stopwatchs.start("Fill Most CMMTs Articles");

		try {
			logger.debug("Filling most comment articles....");
			final int mostCommentArticleDisplayCnt = preference.getInt(Option.ID_C_MOST_COMMENT_ARTICLE_DISPLAY_CNT);
			final List<JSONObject> mostCommentArticles = articleDao
					.getMostCommentArticles(mostCommentArticleDisplayCnt);

			dataModel.put(Common.MOST_COMMENT_ARTICLES, mostCommentArticles);
		} catch (final Exception e) {
			logger.error("Fills most comment articles failed", e);
			throw new ServiceException(e);
		} finally {
			Stopwatchs.end();
		}
	}

	/**
	 * Fills post articles recently.
	 *
	 * @param dataModel
	 *            data model
	 * @param preference
	 *            the specified preference
	 * @throws ServiceException
	 *             service exception
	 */
	public void fillRecentArticles(final Map<String, Object> dataModel, final JSONObject preference)
			throws ServiceException {
		Stopwatchs.start("Fill Recent Articles");

		try {
			final int recentArticleDisplayCnt = preference.getInt(Option.ID_C_RECENT_ARTICLE_DISPLAY_CNT);

			final List<JSONObject> recentArticles = articleDao.getRecentArticles(recentArticleDisplayCnt);

			dataModel.put(Common.RECENT_ARTICLES, recentArticles);

		} catch (final JSONException e) {
			logger.error("Fills recent articles failed", e);
			throw new ServiceException(e);
		} catch (final RepositoryException e) {
			logger.error("Fills recent articles failed", e);
			throw new ServiceException(e);
		} finally {
			Stopwatchs.end();
		}
	}

	/**
	 * Fills post comments recently.
	 *
	 * @param dataModel
	 *            data model
	 * @param preference
	 *            the specified preference
	 * @throws ServiceException
	 *             service exception
	 */
	public void fillRecentComments(final Map<String, Object> dataModel, final JSONObject preference)
			throws ServiceException {
		Stopwatchs.start("Fill Recent Comments");
		try {
			logger.debug("Filling recent comments....");
			final int recentCommentDisplayCnt = preference.getInt(Option.ID_C_RECENT_COMMENT_DISPLAY_CNT);

			final List<JSONObject> recentComments = commentDao.getRecentComments(recentCommentDisplayCnt);

			for (final JSONObject comment : recentComments) {
				final String content = comment.getString(Comment.COMMENT_CONTENT);
				comment.put(Comment.COMMENT_CONTENT, content);
				comment.put(Comment.COMMENT_NAME, comment.getString(Comment.COMMENT_NAME));
				comment.put(Comment.COMMENT_URL, comment.getString(Comment.COMMENT_URL));

				comment.remove(Comment.COMMENT_EMAIL); // Erases email for
														// security reason
			}

			dataModel.put(Common.RECENT_COMMENTS, recentComments);

		} catch (final JSONException e) {
			logger.error("Fills recent comments failed", e);
			throw new ServiceException(e);
		} catch (final RepositoryException e) {
			logger.error("Fills recent comments failed", e);
			throw new ServiceException(e);
		} finally {
			Stopwatchs.end();
		}
	}

	/**
	 * Fills footer.ftl.
	 *
	 * @param request
	 *            the specified HTTP servlet request
	 * @param dataModel
	 *            data model
	 * @param preference
	 *            the specified preference
	 * @throws ServiceException
	 *             service exception
	 */
	public void fillBlogFooter(final HttpServletRequest request, final Map<String, Object> dataModel,
			final JSONObject preference) throws ServiceException {
		Stopwatchs.start("Fill Footer");
		try {
			logger.debug("Filling footer....");
			final String blogTitle = preference.getString(Option.ID_C_BLOG_TITLE);

			dataModel.put(Option.ID_C_BLOG_TITLE, blogTitle);
			dataModel.put("blogHost", Latkes.getServerHost() + ":" + Latkes.getServerPort());

			dataModel.put(Common.VERSION, SoloConstant.VERSION);
			dataModel.put(Common.STATIC_RESOURCE_VERSION, Latkes.getStaticResourceVersion());
			dataModel.put(Common.YEAR, String.valueOf(Calendar.getInstance().get(Calendar.YEAR)));

			String footerContent = "";
			final JSONObject opt = optionQueryService.getOptionById(Option.ID_C_FOOTER_CONTENT);
			if (null != opt) {
				footerContent = opt.optString(Option.OPTION_VALUE);
			}
			dataModel.put(Option.ID_C_FOOTER_CONTENT, footerContent);

			dataModel.put(Keys.Server.STATIC_SERVER, Latkes.getStaticServer());
			dataModel.put(Keys.Server.SERVER, Latkes.getServer());

			dataModel.put(Common.IS_INDEX, "/".equals(request.getRequestURI()));

			dataModel.put(User.USER_NAME, "");
			final JSONObject currentUser = userQueryService.getCurrentUser(request);
			if (null != currentUser) {
				final String userAvatar = currentUser.optString(UserExt.USER_AVATAR);
				if (!StringUtils.isBlank(userAvatar)) {
					dataModel.put(Common.GRAVATAR, userAvatar);
				} else {
					final String email = currentUser.optString(User.USER_EMAIL);
					final String gravatar = Thumbnails.getGravatarURL(email, "128");
					dataModel.put(Common.GRAVATAR, gravatar);
				}

				dataModel.put(User.USER_NAME, currentUser.optString(User.USER_NAME));
			}

			// Activates plugins
			try {
				final ViewLoadEventData data = new ViewLoadEventData();

				data.setViewName("footer.ftl");
				data.setDataModel(dataModel);
				viewLoadEventHandler.action(new Event<>(Keys.FREEMARKER_ACTION, data));
				if (StringUtils.isBlank((String) dataModel.get(Plugin.PLUGINS))) {
					// There is no plugin for this template, fill ${plugins}
					// with blank.
					dataModel.put(Plugin.PLUGINS, "");
				}
			} catch (final EventException e) {
				logger.warn("Event[FREEMARKER_ACTION] handle failed, ignores this exception for kernel health", e);
			}
		} catch (final JSONException e) {
			logger.error("Fills blog footer failed", e);
			throw new ServiceException(e);
		} finally {
			Stopwatchs.end();
		}
	}

	/**
	 * Fills header.ftl.
	 *
	 * @param request
	 *            the specified HTTP servlet request
	 * @param response
	 *            the specified HTTP servlet response
	 * @param dataModel
	 *            data model
	 * @param preference
	 *            the specified preference
	 * @throws ServiceException
	 *             service exception
	 */
	public void fillBlogHeader(final HttpServletRequest request, final HttpServletResponse response,
			final Map<String, Object> dataModel, final JSONObject preference) throws ServiceException {
		Stopwatchs.start("Fill Header");
		try {
			logger.debug("Filling header....");
			final String topBarHTML = topBars.getTopBarHTML(request, response);
			dataModel.put(Common.LOGIN_URL, userService.createLoginURL(Common.ADMIN_INDEX_URI));
			dataModel.put(Common.LOGOUT_URL, userService.createLogoutURL("/"));
			dataModel.put(Common.ONLINE_VISITOR_CNT, StatisticQueryService.getOnlineVisitorCount());

			dataModel.put(Common.TOP_BAR, topBarHTML);

			dataModel.put(Option.ID_C_ARTICLE_LIST_DISPLAY_COUNT,
					preference.getInt(Option.ID_C_ARTICLE_LIST_DISPLAY_COUNT));
			dataModel.put(Option.ID_C_ARTICLE_LIST_PAGINATION_WINDOW_SIZE,
					preference.getInt(Option.ID_C_ARTICLE_LIST_PAGINATION_WINDOW_SIZE));
			dataModel.put(Option.ID_C_LOCALE_STRING, preference.getString(Option.ID_C_LOCALE_STRING));
			dataModel.put(Option.ID_C_BLOG_TITLE, preference.getString(Option.ID_C_BLOG_TITLE));
			dataModel.put(Option.ID_C_BLOG_SUBTITLE, preference.getString(Option.ID_C_BLOG_SUBTITLE));
			dataModel.put(Option.ID_C_HTML_HEAD, preference.getString(Option.ID_C_HTML_HEAD));

			String metaKeywords = preference.getString(Option.ID_C_META_KEYWORDS);
			if (StringUtils.isBlank(metaKeywords)) {
				metaKeywords = "";
			}
			dataModel.put(Option.ID_C_META_KEYWORDS, metaKeywords);

			String metaDescription = preference.getString(Option.ID_C_META_DESCRIPTION);
			if (StringUtils.isBlank(metaDescription)) {
				metaDescription = "";
			}
			dataModel.put(Option.ID_C_META_DESCRIPTION, metaDescription);

			dataModel.put(Common.YEAR, String.valueOf(Calendar.getInstance().get(Calendar.YEAR)));
			dataModel.put(Common.IS_LOGGED_IN, null != userQueryService.getCurrentUser(request));
			dataModel.put(Common.FAVICON_API, PropsUtil.getString("faviconAPI"));

			final String noticeBoard = preference.getString(Option.ID_C_NOTICE_BOARD);

			dataModel.put(Option.ID_C_NOTICE_BOARD, noticeBoard);

			final Query query = new Query().setPageCount(1);
			final JSONObject result = userDao.get(query);
			final JSONArray users = result.getJSONArray(Keys.RESULTS);
			final List<JSONObject> userList = CollectionUtils.jsonArrayToList(users);
			dataModel.put(User.USERS, userList);

			final JSONObject admin = userDao.getAdmin();
			dataModel.put(Common.ADMIN_USER, admin);

			final String skinDirName = (String) request.getAttribute(Keys.TEMAPLTE_DIR_NAME);

			dataModel.put(Skin.SKIN_DIR_NAME, skinDirName);

			Keys.fillRuntime(dataModel);
			fillMinified(dataModel);
			fillPageNavigations(dataModel);
			fillStatistic(dataModel);
		} catch (final JSONException e) {
			logger.error("Fills blog header failed", e);
			throw new ServiceException(e);
		} catch (final RepositoryException e) {
			logger.error("Fills blog header failed", e);
			throw new ServiceException(e);
		} finally {
			Stopwatchs.end();
		}
	}

	/**
	 * Fills minified directory and file postfix for static JavaScript, CSS.
	 *
	 * @param dataModel
	 *            the specified data model
	 */
	public void fillMinified(final Map<String, Object> dataModel) {
		switch (Latkes.getRuntimeMode()) {
		case DEVELOPMENT:
			dataModel.put(Common.MINI_POSTFIX, "");
			break;

		case PRODUCTION:
			dataModel.put(Common.MINI_POSTFIX, Common.MINI_POSTFIX_VALUE);
			break;

		default:
			throw new AssertionError();
		}
	}

	/**
	 * Fills side.ftl.
	 *
	 * @param request
	 *            the specified HTTP servlet request
	 * @param dataModel
	 *            data model
	 * @param preference
	 *            the specified preference
	 * @throws ServiceException
	 *             service exception
	 */
	public void fillSide(final HttpServletRequest request, final Map<String, Object> dataModel,
			final JSONObject preference) throws ServiceException {
		Stopwatchs.start("Fill Side");
		try {
			logger.debug("Filling side....");

			String dir = (String) request.getAttribute(Keys.TEMAPLTE_DIR_NAME);
			Template template = Templates.getTemplate(dir, "side.ftl");

			if (null == template) {
				logger.debug("The skin dose not contain [side.ftl] template");
				template = Templates.getTemplate(dir, "index.ftl");
				if (null == template) {
					logger.debug("The skin dose not contain [index.ftl] template");
					return;
				}
			}

			dataModel.put("fillTagArticles", fillTagArticles);

			if (Templates.hasExpression(template, "<#list recentArticles as article>")) {
				fillRecentArticles(dataModel, preference);
			}

			if (Templates.hasExpression(template, "<#list links as link>")) {
				fillLinks(dataModel);
			}

			if (Templates.hasExpression(template, "<#list recentComments as comment>")) {
				fillRecentComments(dataModel, preference);
			}

			if (Templates.hasExpression(template, "<#list mostUsedCategories as category>")) {
				fillMostUsedCategories(dataModel, preference);
			}

			if (Templates.hasExpression(template, "<#list mostUsedTags as tag>")) {
				fillMostUsedTags(dataModel, preference);
			}

			if (Templates.hasExpression(template, "<#list mostCommentArticles as article>")) {
				fillMostCommentArticles(dataModel, preference);
			}

			if (Templates.hasExpression(template, "<#list mostViewCountArticles as article>")) {
				fillMostViewCountArticles(dataModel, preference);
			}

			if (Templates.hasExpression(template, "<#list archiveDates as archiveDate>")) {
				fillArchiveDates(dataModel, preference);
			}

		} catch (final ServiceException e) {
			logger.error("Fills side failed", e);
			throw new ServiceException(e);
		} finally {
			Stopwatchs.end();
		}
	}

	/**
	 * Fills the specified template.
	 *
	 * @param request
	 *            the specified HTTP servlet request
	 * @param template
	 *            the specified template
	 * @param dataModel
	 *            data model
	 * @param preference
	 *            the specified preference
	 * @throws ServiceException
	 *             service exception
	 */
	public void fillUserTemplate(final HttpServletRequest request, final Template template,
			final Map<String, Object> dataModel, final JSONObject preference) throws ServiceException {
		Stopwatchs.start("Fill User Template[name=" + template.getName() + "]");
		try {
			logger.debug("Filling user template[name{}]", template.getName());

			if (Templates.hasExpression(template, "<#list links as link>")) {
				fillLinks(dataModel);
			}

			if (Templates.hasExpression(template, "<#list tags as tag>")) {
				fillTags(dataModel);
			}

			if (Templates.hasExpression(template, "<#list recentComments as comment>")) {
				fillRecentComments(dataModel, preference);
			}

			if (Templates.hasExpression(template, "<#list mostUsedCategories as category>")) {
				fillMostUsedCategories(dataModel, preference);
			}

			if (Templates.hasExpression(template, "<#list mostUsedTags as tag>")) {
				fillMostUsedTags(dataModel, preference);
			}

			if (Templates.hasExpression(template, "<#list mostCommentArticles as article>")) {
				fillMostCommentArticles(dataModel, preference);
			}

			if (Templates.hasExpression(template, "<#list mostViewCountArticles as article>")) {
				fillMostViewCountArticles(dataModel, preference);
			}

			if (Templates.hasExpression(template, "<#list archiveDates as archiveDate>")) {
				fillArchiveDates(dataModel, preference);
			}

			if (Templates.hasExpression(template, "<#include \"side.ftl\"/>")) {
				fillSide(request, dataModel, preference);
			}

			final String noticeBoard = preference.getString(Option.ID_C_NOTICE_BOARD);

			dataModel.put(Option.ID_C_NOTICE_BOARD, noticeBoard);
		} catch (final JSONException e) {
			logger.error("Fills user template failed", e);
			throw new ServiceException(e);
		} finally {
			Stopwatchs.end();
		}
	}

	/**
	 * Fills page navigations.
	 *
	 * @param dataModel
	 *            data model
	 * @throws ServiceException
	 *             service exception
	 */
	private void fillPageNavigations(final Map<String, Object> dataModel) throws ServiceException {
		Stopwatchs.start("Fill Navigations");
		try {
			logger.debug("Filling page navigations....");
			final List<JSONObject> pages = pageDao.getPages();

			for (final JSONObject page : pages) {
				if ("page".equals(page.optString(Page.PAGE_TYPE))) {
					final String permalink = page.optString(Page.PAGE_PERMALINK);

					page.put(Page.PAGE_PERMALINK, Latkes.getServePath() + permalink);
				}
			}

			dataModel.put(Common.PAGE_NAVIGATIONS, pages);
		} catch (final RepositoryException e) {
			logger.error("Fills page navigations failed", e);
			throw new ServiceException(e);
		} finally {
			Stopwatchs.end();
		}
	}

	/**
	 * Fills statistic.
	 *
	 * @param dataModel
	 *            data model
	 * @throws ServiceException
	 *             service exception
	 */
	private void fillStatistic(final Map<String, Object> dataModel) throws ServiceException {
		Stopwatchs.start("Fill Statistic");
		try {
			logger.debug("Filling statistic....");
			final JSONObject statistic = statisticQueryService.getStatistic();

			dataModel.put(Statistic.STATISTIC, statistic);
		} catch (final ServiceException e) {
			logger.error("Fills statistic failed", e);
			throw new ServiceException(e);
		} finally {
			Stopwatchs.end();
		}
	}

	/**
	 * Sets some extra properties into the specified article with the specified
	 * author and preference, performs content and abstract editor processing.
	 * <p>
	 * <p>
	 * Article ext properties:
	 * 
	 * <pre>
	 * {
	 *     ....,
	 *     "authorName": "",
	 *     "authorId": "",
	 *     "authorThumbnailURL": "",
	 *     "hasUpdated": boolean
	 * }
	 * </pre>
	 * </p>
	 *
	 * @param request
	 *            the specified HTTP servlet request
	 * @param article
	 *            the specified article
	 * @param author
	 *            the specified author
	 * @param preference
	 *            the specified preference
	 * @throws ServiceException
	 *             service exception
	 * @see #setArticlesExProperties(HttpServletRequest, List, JSONObject,
	 *      JSONObject)
	 */
	private void setArticleExProperties(final HttpServletRequest request, final JSONObject article,
			final JSONObject author, final JSONObject preference) throws ServiceException {
		try {
			final String authorName = author.getString(User.USER_NAME);

			article.put(Common.AUTHOR_NAME, authorName);
			final String authorId = author.getString(Keys.OBJECT_ID);

			article.put(Common.AUTHOR_ID, authorId);

			final String userAvatar = author.optString(UserExt.USER_AVATAR);
			if (!StringUtils.isBlank(userAvatar)) {
				article.put(Common.AUTHOR_THUMBNAIL_URL, userAvatar);
			} else {
				final String thumbnailURL = Thumbnails.getGravatarURL(author.optString(User.USER_EMAIL), "128");
				article.put(Common.AUTHOR_THUMBNAIL_URL, thumbnailURL);
			}

			if (preference.getBoolean(Option.ID_C_ENABLE_ARTICLE_UPDATE_HINT)) {
				article.put(Common.HAS_UPDATED, articleQueryService.hasUpdated(article));
			} else {
				article.put(Common.HAS_UPDATED, false);
			}

			if (articleQueryService.needViewPwd(request, article)) {
				final String content = langPropsService.get("articleContentPwd");

				article.put(ARTICLE_CONTENT, content);
			}

			processArticleAbstract(preference, article);

			articleQueryService.markdown(article);
		} catch (final Exception e) {
			logger.error("Sets article extra properties failed", e);
			throw new ServiceException(e);
		}
	}

	/**
	 * Sets some extra properties into the specified article with the specified
	 * preference, performs content and abstract editor processing.
	 * <p>
	 * <p>
	 * Article ext properties:
	 * 
	 * <pre>
	 * {
	 *     ....,
	 *     "authorName": "",
	 *     "authorId": "",
	 *     "authorThumbnailURL": "",
	 *     "hasUpdated": boolean
	 * }
	 * </pre>
	 * </p>
	 *
	 * @param request
	 *            the specified HTTP servlet request
	 * @param article
	 *            the specified article
	 * @param preference
	 *            the specified preference
	 * @throws ServiceException
	 *             service exception
	 * @see #setArticlesExProperties(HttpServletRequest, List, JSONObject)
	 */
	private void setArticleExProperties(final HttpServletRequest request, final JSONObject article,
			final JSONObject preference) throws ServiceException {
		try {
			final JSONObject author = articleQueryService.getAuthor(article);
			final String authorName = author.getString(User.USER_NAME);

			article.put(Common.AUTHOR_NAME, authorName);
			final String authorId = author.getString(Keys.OBJECT_ID);

			article.put(Common.AUTHOR_ID, authorId);

			final String userAvatar = author.optString(UserExt.USER_AVATAR);
			if (!StringUtils.isBlank(userAvatar)) {
				article.put(Common.AUTHOR_THUMBNAIL_URL, userAvatar);
			} else {
				final String thumbnailURL = Thumbnails.getGravatarURL(author.optString(User.USER_EMAIL), "128");
				article.put(Common.AUTHOR_THUMBNAIL_URL, thumbnailURL);
			}

			if (preference.getBoolean(Option.ID_C_ENABLE_ARTICLE_UPDATE_HINT)) {
				article.put(Common.HAS_UPDATED, articleQueryService.hasUpdated(article));
			} else {
				article.put(Common.HAS_UPDATED, false);
			}

			if (articleQueryService.needViewPwd(request, article)) {
				final String content = langPropsService.get("articleContentPwd");

				article.put(ARTICLE_CONTENT, content);
			}

			processArticleAbstract(preference, article);

			articleQueryService.markdown(article);
		} catch (final Exception e) {
			logger.error("Sets article extra properties failed", e);
			throw new ServiceException(e);
		}
	}

	/**
	 * Sets some extra properties into the specified article with the specified
	 * author and preference.
	 * <p>
	 * <p>
	 * The batch version of method
	 * {@linkplain #setArticleExProperties(HttpServletRequest, JSONObject, JSONObject, JSONObject)}.
	 * </p>
	 * <p>
	 * <p>
	 * Article ext properties:
	 * 
	 * <pre>
	 * {
	 *     ....,
	 *     "authorName": "",
	 *     "authorId": "",
	 *     "hasUpdated": boolean
	 * }
	 * </pre>
	 * </p>
	 *
	 * @param request
	 *            the specified HTTP servlet request
	 * @param articles
	 *            the specified articles
	 * @param author
	 *            the specified author
	 * @param preference
	 *            the specified preference
	 * @throws ServiceException
	 *             service exception
	 * @see #setArticleExProperties(HttpServletRequest, JSONObject, JSONObject,
	 *      JSONObject)
	 */
	public void setArticlesExProperties(final HttpServletRequest request, final List<JSONObject> articles,
			final JSONObject author, final JSONObject preference) throws ServiceException {
		for (final JSONObject article : articles) {
			setArticleExProperties(request, article, author, preference);
		}
	}

	/**
	 * Sets some extra properties into the specified article with the specified
	 * preference.
	 * <p>
	 * <p>
	 * The batch version of method
	 * {@linkplain #setArticleExProperties(HttpServletRequest, JSONObject, JSONObject)}.
	 * </p>
	 * <p>
	 * <p>
	 * Article ext properties:
	 * 
	 * <pre>
	 * {
	 *     ....,
	 *     "authorName": "",
	 *     "authorId": "",
	 *     "hasUpdated": boolean
	 * }
	 * </pre>
	 * </p>
	 *
	 * @param request
	 *            the specified HTTP servlet request
	 * @param articles
	 *            the specified articles
	 * @param preference
	 *            the specified preference
	 * @throws ServiceException
	 *             service exception
	 * @see #setArticleExProperties(HttpServletRequest, JSONObject, JSONObject)
	 */
	public void setArticlesExProperties(final HttpServletRequest request, final List<JSONObject> articles,
			final JSONObject preference) throws ServiceException {
		for (final JSONObject article : articles) {
			setArticleExProperties(request, article, preference);
		}
	}

	/**
	 * Processes the abstract of the specified article with the specified
	 * preference.
	 * <p>
	 * <p>
	 * <ul>
	 * <li>If the abstract is {@code null}, sets it with ""</li>
	 * <li>If user configured preference "titleOnly", sets the abstract with
	 * ""</li>
	 * <li>If user configured preference "titleAndContent", sets the abstract
	 * with the content of the article</li>
	 * </ul>
	 * </p>
	 *
	 * @param preference
	 *            the specified preference
	 * @param article
	 *            the specified article
	 */
	private void processArticleAbstract(final JSONObject preference, final JSONObject article) {
		final String articleAbstract = article.optString(Article.ARTICLE_ABSTRACT, null);

		if (null == articleAbstract) {
			article.put(Article.ARTICLE_ABSTRACT, "");
		}

		final String articleListStyle = preference.optString(Option.ID_C_ARTICLE_LIST_STYLE);

		if ("titleOnly".equals(articleListStyle)) {
			article.put(Article.ARTICLE_ABSTRACT, "");
		} else if ("titleAndContent".equals(articleListStyle)) {
			article.put(Article.ARTICLE_ABSTRACT, article.optString(Article.ARTICLE_CONTENT));
		}
	}
}
