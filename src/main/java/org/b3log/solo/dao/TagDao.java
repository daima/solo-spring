package org.b3log.solo.dao;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.b3log.solo.Keys;
import org.b3log.solo.dao.repository.FilterOperator;
import org.b3log.solo.dao.repository.PropertyFilter;
import org.b3log.solo.dao.repository.Query;
import org.b3log.solo.dao.repository.RepositoryException;
import org.b3log.solo.dao.repository.SortDirection;
import org.b3log.solo.model.Tag;
import org.b3log.solo.util.CollectionUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class TagDao extends AbstractBlogDao {
	/**
	 * Tag-Article relation repository.
	 */
	@Autowired
	private TagArticleDao tagArticleDao;

	@Override
	public String getTableNamePostfix() {
		return Tag.TAG;
	}

	public JSONObject getByTitle(final String tagTitle) throws RepositoryException {
		final Query query = new Query().setFilter(new PropertyFilter(Tag.TAG_TITLE, FilterOperator.EQUAL, tagTitle))
				.setPageCount(1);

		final JSONObject result = get(query);
		final JSONArray array = result.optJSONArray(Keys.RESULTS);

		if (0 == array.length()) {
			return null;
		}

		return array.optJSONObject(0);
	}

	public List<JSONObject> getMostUsedTags(final int num) throws RepositoryException {
		final Query query = new Query().addSort(Tag.TAG_PUBLISHED_REFERENCE_COUNT, SortDirection.DESCENDING)
				.setCurrentPageNum(1).setPageSize(num).setPageCount(1);

		final JSONObject result = get(query);
		final JSONArray array = result.optJSONArray(Keys.RESULTS);

		List<JSONObject> tagJoList = CollectionUtils.jsonArrayToList(array);
		sortJSONTagList(tagJoList);

		return tagJoList;
	}

	public List<JSONObject> getByArticleId(final String articleId) throws RepositoryException {
		final List<JSONObject> ret = new ArrayList<>();

		final List<JSONObject> tagArticleRelations = tagArticleDao.getByArticleId(articleId);

		for (final JSONObject tagArticleRelation : tagArticleRelations) {
			final String tagId = tagArticleRelation.optString(Tag.TAG + "_" + Keys.OBJECT_ID);
			final JSONObject tag = get(tagId);

			ret.add(tag);
		}

		return ret;
	}

	/**
	 * Sets tag article repository with the specified tag article repository.
	 *
	 * @param tagArticleDao
	 *            the specified tag article repository
	 */
	public void setTagArticleDao(final TagArticleDao tagArticleDao) {
		this.tagArticleDao = tagArticleDao;
	}

	private void sortJSONTagList(final List<JSONObject> tagJoList) throws RepositoryException {
		Collections.sort(tagJoList, (o1, o2) -> {
			try {
				return Collator.getInstance(java.util.Locale.CHINA).compare(o1.getString(Tag.TAG_TITLE),
						o2.getString(Tag.TAG_TITLE));
			} catch (final JSONException e) {
				throw new RuntimeException(e);
			}
		});
	}
}
