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
package org.b3log.solo.dao.repository;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.b3log.solo.SoloConstant;
import org.b3log.solo.util.CollectionUtils;
import org.b3log.solo.util.PropsUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Repository utilities.
 *
 * @author <a href="http://cxy7.com">XyCai</a>
 * @version 1.0.1.9, Apr 17, 2014
 */
public final class Repositories {

	/**
	 * Logger.
	 */
	private static Logger logger = LoggerFactory.getLogger(Repositories.class);

	/**
	 * Repository holder.
	 * 
	 * <p>
	 * &lt;repositoryName, {@link Repository repository}&gt;
	 * <p>
	 */
	private static final Map<String, Repository> REPOS_HOLDER = new ConcurrentHashMap<>();

	/**
	 * Repositories description (repository.json).
	 */
	private static JSONObject repositoriesDescription;

	/**
	 * Whether all repositories is writable.
	 */
	private static boolean repositoryiesWritable = true;
	// Repository name prefix
	private final static String tableNamePrefix = StringUtils.isNotBlank(PropsUtil.getString("jdbc.tablePrefix"))
			? PropsUtil.getString("jdbc.tablePrefix") + "_" : "";

	/**
	 * Whether all repositories is writable.
	 * 
	 * @return {@code true} if they are writable, returns {@code false}
	 *         otherwise
	 */
	public static boolean getReposirotiesWritable() {
		return repositoryiesWritable;
	}

	/**
	 * Sets all repositories whether is writable with the specified flag.
	 * 
	 * @param writable
	 *            the specified flat, {@code true} for writable, {@code false}
	 *            otherwise
	 */
	public static void setRepositoriesWritable(final boolean writable) {
		for (final Map.Entry<String, Repository> entry : REPOS_HOLDER.entrySet()) {
			final String repositoryName = entry.getKey();
			final Repository repository = entry.getValue();

			repository.setWritable(writable);

			logger.info("Sets repository[name={}] writable[{}]", new Object[] { repositoryName, writable });
		}

		repositoryiesWritable = writable;
	}

	/**
	 * Gets repository names.
	 * 
	 * @return repository names, for example,
	 * 
	 *         <pre>
	 * [
	 *     "repository1", "repository2", ....
	 * ]
	 *         </pre>
	 */
	public static JSONArray getRepositoryNames() {
		final JSONArray ret = new JSONArray();

		if (null == repositoriesDescription) {
			logger.info("Not found repository description[repository.json] file under classpath");

			return ret;
		}

		final JSONArray repositories = repositoriesDescription.optJSONArray("repositories");

		for (int i = 0; i < repositories.length(); i++) {
			final JSONObject repository = repositories.optJSONObject(i);

			ret.put(repository.optString("name"));
		}

		return ret;
	}

	/**
	 * Gets repositories description.
	 * 
	 * @return repositories description, returns {@code null} if not found or
	 *         parse the description failed
	 */
	public static JSONObject getRepositoriesDescription() {
		return repositoriesDescription;
	}

	static {
		loadRepositoryDescription();
	}

	/**
	 * Determines whether the specified json object can not be persisted (add or
	 * update) into an repository which specified by the given repository name.
	 * 
	 * <p>
	 * A valid json object to persist must match keys definitions (including
	 * type and length if had) in the repository description (repository.json)
	 * with the json object names itself.
	 * </p>
	 * 
	 * <p>
	 * The specified keys to ignore will be bypassed, regardless of matching
	 * keys definitions.
	 * </p>
	 * 
	 * @param repositoryName
	 *            the given repository name (maybe with table name prefix)
	 * @param jsonObject
	 *            the specified json object
	 * @param ignoredKeys
	 *            the specified keys to ignore
	 * @throws RepositoryException
	 *             if the specified json object can not be persisted
	 * @see Repository#add(org.json.JSONObject)
	 * @see Repository#update(java.lang.String, org.json.JSONObject)
	 */
	public static void check(final String repositoryName, final JSONObject jsonObject, final String... ignoredKeys)
			throws RepositoryException {
		if (null == jsonObject) {
			throw new RepositoryException("Null to persist to repository[" + repositoryName + "]");
		}

		final boolean needIgnoreKeys = null != ignoredKeys && 0 < ignoredKeys.length;

		final JSONArray names = jsonObject.names();
		final Set<Object> nameSet = CollectionUtils.jsonArrayToSet(names);

		final JSONArray keysDescription = getRepositoryKeysDescription(repositoryName);

		if (null == keysDescription) { // Not found repository description
			// Skips the checks
			return;
		}

		final Set<String> keySet = new HashSet<>();

		// Checks whether the specified json object has all keys defined,
		// and whether the type of its value is appropriate
		for (int i = 0; i < keysDescription.length(); i++) {
			final JSONObject keyDescription = keysDescription.optJSONObject(i);

			final String key = keyDescription.optString("name");

			keySet.add(key);
			if (needIgnoreKeys) {
				boolean contain = false;
				for (String k : ignoredKeys) {
					if (StringUtils.containsIgnoreCase(key, k))
						contain = true;
				}
				if (contain)
					continue;
			}

			if (!keyDescription.optBoolean("nullable") && !nameSet.contains(key)) {
				throw new RepositoryException("A json object to persist to repository[name=" + repositoryName
						+ "] does not contain a key[" + key + "]");
			}

			// TODO: 88250, type and length validation
			/*
			 * final String type = keyDescription.optString("type"); final
			 * Object value = jsonObject.opt(key);
			 * 
			 * if (("String".equals(type) && !(value instanceof String)) ||
			 * ("int".equals(type) && !(value instanceof Integer)) ||
			 * ("long".equals(type) && !(value instanceof Long)) ||
			 * ("double".equals(type) && !(value instanceof Double)) ||
			 * ("boolean".equals(type) && !(value instanceof Boolean))) {
			 * LOGGER.log(Level.WARNING,
			 * "A json object to persist to repository[name={}] has " +
			 * "a wrong value type[definedType={}, currentType={}] with key[" +
			 * key + "]", new Object[]{repositoryName, type, value.getClass()});
			 * 
			 * return true; }
			 */
		}

		// Checks whether the specified json object has an redundant (undefined)
		// key
		for (int i = 0; i < names.length(); i++) {
			final String name = names.optString(i);

			if (!keySet.contains(name)) {
				throw new RepositoryException("A json object to persist to repository[name=" + repositoryName
						+ "] contains an redundant key[" + name + "]");
			}
		}
	}

	/**
	 * Gets the keys description of an repository specified by the given
	 * repository name.
	 * 
	 * @param repositoryName
	 *            the given repository name (maybe with table name prefix)
	 * @return keys description, returns {@code null} if not found
	 */
	public static JSONArray getRepositoryKeysDescription(final String repositoryName) {
		if (StringUtils.isBlank(repositoryName)) {
			return null;
		}

		if (null == repositoriesDescription) {
			return null;
		}

		final JSONArray repositories = repositoriesDescription.optJSONArray("repositories");

		for (int i = 0; i < repositories.length(); i++) {
			final JSONObject repository = repositories.optJSONObject(i);

			if (repositoryName.equals(repository.optString("name"))
					|| (tableNamePrefix + repositoryName).equals(repository.optString("name"))) {
				return repository.optJSONArray("keys");
			}
		}

		throw new RuntimeException("Not found the repository[name=" + repositoryName
				+ "] description, please define it in repositories.json");
	}

	/**
	 * Gets the key names of an repository specified by the given repository
	 * name.
	 * 
	 * @param repositoryName
	 *            the given repository name
	 * @return a set of key names, returns an empty set if not found
	 */
	public static Set<String> getKeyNames(final String repositoryName) {
		if (StringUtils.isBlank(repositoryName)) {
			return Collections.emptySet();
		}

		if (null == repositoriesDescription) {
			return null;
		}

		final JSONArray repositories = repositoriesDescription.optJSONArray("repositories");
		JSONArray keys = null;

		for (int i = 0; i < repositories.length(); i++) {
			final JSONObject repository = repositories.optJSONObject(i);

			if (repositoryName.equals(repository.optString("name"))) {
				keys = repository.optJSONArray("keys");
			}
		}

		if (null == keys) {
			throw new RuntimeException("Not found the repository[name=" + repositoryName
					+ "] description, please define it in repositories.json");
		}

		final Set<String> ret = new HashSet<>();

		for (int i = 0; i < keys.length(); i++) {
			final JSONObject keyDescription = keys.optJSONObject(i);
			final String key = keyDescription.optString("name");

			ret.add(key);
		}

		return ret;
	}

	/**
	 * Gets a repository with the specified repository name.
	 * 
	 * @param repositoryName
	 *            the specified repository name
	 * @return repository, returns {@code null} if not found
	 */
	public static Repository getRepository(final String repositoryName) {
		return REPOS_HOLDER.get(repositoryName);
	}

	/**
	 * Adds the specified repository.
	 * 
	 * @param repository
	 *            the specified repository
	 */
	public static void addRepository(final Repository repository) {
		REPOS_HOLDER.put(repository.getTableName(), repository);
	}

	/**
	 * Loads repository description.
	 */
	private static void loadRepositoryDescription() {
		logger.info("Loading repository description....");

		final InputStream inputStream = AbstractRepository.class.getResourceAsStream("/repository.json");

		if (null == inputStream) {
			logger.info("Not found repository description[repository.json] file under classpath");
			return;
		}

		logger.info("Parsing repository description....");

		try {
			final String description = IOUtils.toString(inputStream);

			logger.debug("{}{}", new Object[] { SoloConstant.LINE_SEPARATOR, description });

			repositoriesDescription = new JSONObject(description);

			final JSONArray repositories = repositoriesDescription.optJSONArray("repositories");

			for (int i = 0; i < repositories.length(); i++) {
				final JSONObject repository = repositories.optJSONObject(i);

				repository.put("name", tableNamePrefix + repository.optString("name"));
			}
		} catch (final Exception e) {
			logger.error("Parses repository description failed", e);
		} finally {
			try {
				inputStream.close();
			} catch (final IOException e) {
				logger.error(e.getMessage(), e);
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * Private constructor.
	 */
	private Repositories() {
	}
}
