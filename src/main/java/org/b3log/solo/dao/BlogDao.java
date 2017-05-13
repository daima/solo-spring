package org.b3log.solo.dao;

import java.util.List;
import java.util.Map;

import org.b3log.solo.dao.repository.Query;
import org.b3log.solo.dao.repository.RepositoryException;
import org.json.JSONObject;

public interface BlogDao {

	/**
	 * Adds the specified json object.
	 *
	 * @param jsonObject
	 *            the specified json object
	 * @return the generated object id
	 * @throws RepositoryException
	 *             repository exception
	 */
	String add(final JSONObject jsonObject) throws RepositoryException;

	/**
	 * Updates a certain json object by the specified id and the specified new
	 * json object.
	 *
	 * @param id
	 *            the specified id
	 * @param jsonObject
	 *            the specified new json object
	 * @throws RepositoryException
	 *             repository exception
	 */
	void update(final String id, final JSONObject jsonObject) throws RepositoryException;

	/**
	 * Removes a json object by the specified id.
	 *
	 * @param id
	 *            the specified id
	 * @throws RepositoryException
	 *             repository exception
	 */
	void remove(final String id) throws RepositoryException;

	/**
	 * Gets a json object by the specified id.
	 *
	 * @param id
	 *            the specified id
	 * @return a json object, returns {@code null} if not found
	 * @throws RepositoryException
	 *             repository exception
	 */
	JSONObject get(final String id) throws RepositoryException;

	/**
	 * Gets json objects by the specified ids.
	 *
	 * @param ids
	 *            the specified ids
	 * @return json objects matched in the specified ids
	 * @throws RepositoryException
	 *             repository exception
	 */
	Map<String, JSONObject> get(final Iterable<String> ids) throws RepositoryException;

	/**
	 * Determines a json object specified by the given id exists in this
	 * repository.
	 *
	 * @param id
	 *            the given id
	 * @return {@code true} if it exists, otherwise {@code false}
	 * @throws RepositoryException
	 *             repository exception
	 */
	boolean has(final String id) throws RepositoryException;

	/**
	 * Gets json objects by the specified query.
	 *
	 * <h4>Pagination</h4> If the "paginationPageCount" has been specified (not
	 * with {@code -1} or {@code null}) by caller (as the argument
	 * {@link Query#pageCount}), the value will be used in the returned value.
	 * In other words, the page count result will not be calculated by this
	 * interface, otherwise, the returned value pagination.paginationPageCount
	 * and pagination.paginationRecordCount will be calculated with query
	 * condition.
	 *
	 * <p>
	 * <b>Note</b>: The order of elements of the returned result list is decided
	 * by datastore implementation, excepts
	 * {@link Query#addSort(java.lang.String, org.b3log.solo.dao.repository.SortDirection)}
	 * be invoked.
	 * </p>
	 *
	 * @param query
	 *            the specified query
	 * @return for example,
	 * 
	 *         <pre>
	 * {
	 *     "pagination": {
	 *       "paginationPageCount": 10, // May be specified by the specified query.pageCount
	 *       "paginationRecordCount": "100" // If query.pageCount has been specified with not {@code -1} or {@code null}, this value will
	 *                                         be {@code 0} also
	 *     },
	 *     "rslts": [{
	 *         "oId": "...."
	 *     }, ....]
	 * }
	 *         </pre>
	 *
	 * @throws RepositoryException
	 *             repository exception
	 */
	JSONObject get(final Query query) throws RepositoryException;

	/**
	 * Gets json objects by the specified query statement.
	 *
	 * @param statement
	 *            the specified query statement
	 * @param params
	 *            the specified parameters
	 * @return a list of result, returns an empty list if not found
	 *
	 * @throws RepositoryException
	 *             repository exception
	 */
	List<JSONObject> select(final String statement, final Object... params) throws RepositoryException;

	/**
	 * Gets a list of json objects randomly with the specified fetch size.
	 *
	 * @param fetchSize
	 *            the specified fetch size
	 * @return a list of json objects, its size less or equal to the specified
	 *         fetch size, returns an empty list if not found
	 * @throws RepositoryException
	 *             repository exception
	 */
	List<JSONObject> getRandomly(final int fetchSize) throws RepositoryException;

	/**
	 * Gets the count of all json objects.
	 *
	 * @return count, returns {@code -1} if not available
	 * @throws RepositoryException
	 *             repository exception
	 */
	long count() throws RepositoryException;

	/**
	 * Gets the count of json objects by the specified query.
	 *
	 * @param query
	 *            the specified query
	 * @return count, returns {@code -1} if not available
	 * @throws RepositoryException
	 *             repository exception
	 */
	long count(final Query query) throws RepositoryException;

	/**
	 * Begins a transaction against the repository.
	 *
	 * <p>
	 * Callers are responsible for explicitly calling
	 * {@linkplain Transaction#commit()} or {@linkplain Transaction#rollback()}
	 * when they no longer need the {@code Transaction}. The {@code Transaction}
	 * returned by this call will be considered <i>the current transaction</i>
	 * until one of the following happens:
	 * <ol>
	 * <li>{@linkplain #beginTransaction()} is invoked from the same thread</li>
	 * <li>{@linkplain Transaction#commit()} is invoked on the
	 * {@code Transaction} returned by this method</li> Whether or not the
	 * commit returns successfully, the {@code Transaction} will no longer be
	 * <i>the current transaction</i>.
	 * <li>{@linkplain Transaction#rollback()} is invoked on the
	 * {@code Transaction} returned by this method</li> Whether or not the
	 * rollback returns successfully, the {@code Transaction} will no longer be
	 * <i>the current transaction</i>.
	 * </ol>
	 * </p>
	 *
	 * @return the transaction that was started.
	 */
	// Transaction beginTransaction();

	/**
	 * Whether the repository within a transaction.
	 *
	 * @return {@code true} if the repository within a transaction, returns
	 *         {@code false} otherwise
	 */
	// boolean hasTransactionBegun();

	/**
	 * Whether the repository is writable.
	 *
	 * @return {@code true} if it is writable, returns {@code false} otherwise
	 */
	boolean isWritable();

	/**
	 * Sets whether the repository is writable with the specified flag.
	 *
	 * @param writable
	 *            the specified flat, {@code true} for writable, {@code false}
	 *            otherwise
	 */
	void setWritable(final boolean writable);
}
