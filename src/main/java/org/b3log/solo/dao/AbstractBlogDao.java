package org.b3log.solo.dao;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.b3log.solo.Keys;
import org.b3log.solo.frame.logging.Level;
import org.b3log.solo.frame.logging.Logger;
import org.b3log.solo.frame.model.Pagination;
import org.b3log.solo.frame.repository.Query;
import org.b3log.solo.frame.repository.Repositories;
import org.b3log.solo.frame.repository.RepositoryException;
import org.b3log.solo.frame.repository.Transaction;
import org.b3log.solo.frame.repository.jdbc.JdbcRepository;
import org.b3log.solo.util.Callstacks;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
@Component
public abstract class AbstractBlogDao extends JdbcRepository implements BlogDao {

	/**
	 * Logger.
	 */
	private static final Logger LOGGER = Logger.getLogger(AbstractBlogDao.class.getName());

	@Override
	public String add(final JSONObject jsonObject) throws RepositoryException {
		if (!isWritable() && !isInternalCall()) {
			throw new RepositoryException("The repository[name=" + getTableNamePostfix() + "] is not writable at present");
		}

		Repositories.check(getTableNamePostfix(), jsonObject, Keys.OBJECT_ID);

		return super.add(jsonObject);
	}

	@Override
	public void update(final String id, final JSONObject jsonObject) throws RepositoryException {
		if (!isWritable() && !isInternalCall()) {
			throw new RepositoryException("The repository[name=" + getTableNamePostfix() + "] is not writable at present");
		}

		Repositories.check(getTableName(), jsonObject, Keys.OBJECT_ID);

		super.update(id, jsonObject);
	}

	@Override
	public void remove(final String id) throws RepositoryException {
		if (!isWritable() && !isInternalCall()) {
			throw new RepositoryException("The repository[name=" + getTableNamePostfix() + "] is not writable at present");
		}

		super.remove(id);
	}

	@Override
	public JSONObject get(final String id) throws RepositoryException {
		try {
			return super.get(id);
		} catch (final RepositoryException e) {
			LOGGER.log(Level.WARN, "SQL exception[msg={0}]", e.getMessage());
			return null;
		}
	}

	@Override
	public Map<String, JSONObject> get(final Iterable<String> ids) throws RepositoryException {
		return super.get(ids);
	}

	@Override
	public boolean has(final String id) throws RepositoryException {
		return super.has(id);
	}

	@Override
	public JSONObject get(final Query query) throws RepositoryException {
		try {
			return super.get(query);
		} catch (final RepositoryException e) {
			LOGGER.log(Level.WARN, "SQL exception[msg={0}, repository={1}, query={2}]", e.getMessage(),
					super.getTableName(), query.toString());

			final JSONObject ret = new JSONObject();
			final JSONObject pagination = new JSONObject();

			ret.put(Pagination.PAGINATION, pagination);
			pagination.put(Pagination.PAGINATION_PAGE_COUNT, 0);
			final JSONArray results = new JSONArray();

			ret.put(Keys.RESULTS, results);

			return ret;
		}
	}

	@Override
	public List<JSONObject> select(final String statement, final Object... params) throws RepositoryException {
		try {
			return super.select(statement, params);
		} catch (final RepositoryException e) {
			LOGGER.log(Level.WARN, "SQL exception[msg={0}, repository={1}, statement={2}]", e.getMessage(),
					super.getTableName(), statement);

			return Collections.emptyList();
		}
	}

	@Override
	public List<JSONObject> getRandomly(final int fetchSize) throws RepositoryException {
		return super.getRandomly(fetchSize);
	}

	@Override
	public long count() throws RepositoryException {
		return super.count();
	}

	@Override
	public long count(final Query query) throws RepositoryException {
		return super.count(query);
	}

	/*@Override
	public Transaction beginTransaction() {
		return super.beginTransaction();
	}

	@Override
	public boolean hasTransactionBegun() {
		return super.hasTransactionBegun();
	}*/

	@Override
	public boolean isWritable() {
		return super.isWritable();
	}

	@Override
	public void setWritable(final boolean writable) {
		super.setWritable(writable);
	}

	/**
	 * Checks the current method is whether invoked as internal call.
	 *
	 * @return {@code true} if the current method is invoked as internal call,
	 *         return {@code false} otherwise
	 */
	private static boolean isInternalCall() {
		return Callstacks.isCaller("org.b3log.solo.remote.RepositoryAccessor", "*");
	}
}
