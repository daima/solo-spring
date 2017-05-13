package org.b3log.solo.dao;

import java.util.List;

import org.b3log.solo.Keys;
import org.b3log.solo.dao.repository.FilterOperator;
import org.b3log.solo.dao.repository.PropertyFilter;
import org.b3log.solo.dao.repository.Query;
import org.b3log.solo.dao.repository.RepositoryException;
import org.b3log.solo.dao.repository.SortDirection;
import org.b3log.solo.model.Article;
import org.b3log.solo.model.Tag;
import org.b3log.solo.util.CollectionUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Repository;

@Repository
public class TagArticleDao extends AbstractBlogDao {

	@Override
	public String getTableNamePostfix() {
		return Tag.TAG + "_" + Article.ARTICLE;
	}

	public List<JSONObject> getByArticleId(final String articleId) throws RepositoryException {
		final Query query = new Query()
				.setFilter(new PropertyFilter(Article.ARTICLE + "_" + Keys.OBJECT_ID, FilterOperator.EQUAL, articleId))
				.setPageCount(1);

		final JSONObject result = get(query);
		final JSONArray array = result.optJSONArray(Keys.RESULTS);

		return CollectionUtils.jsonArrayToList(array);
	}

	public JSONObject getByTagId(final String tagId, final int currentPageNum, final int pageSize)
			throws RepositoryException {
		final Query query = new Query()
				.setFilter(new PropertyFilter(Tag.TAG + "_" + Keys.OBJECT_ID, FilterOperator.EQUAL, tagId))
				.addSort(Article.ARTICLE + "_" + Keys.OBJECT_ID, SortDirection.DESCENDING)
				.setCurrentPageNum(currentPageNum).setPageSize(pageSize).setPageCount(1);

		return get(query);
	}
}
