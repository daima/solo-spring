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
package org.b3log.solo.dao.repository.jdbc.util;

import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.b3log.solo.dao.repository.Repositories;
import org.b3log.solo.dao.repository.RepositoryException;
import org.b3log.solo.dao.repository.jdbc.JdbcFactory;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * JdbcRepositories utilities.
 *
 * @author <a href="mailto:wmainlove@gmail.com">Love Yao</a>
 * @author <a href="http://cxy7.com">XyCai</a>
 * @version 1.2.0.2, Apr 8, 2014
 */
@Repository
public final class JdbcRepositories {

	/**
	 * Logger.
	 */
	private static Logger logger = LoggerFactory.getLogger(JdbcRepositories.class);
	@Autowired
	private JdbcTemplate jdbcTemplate;

	/**
	 * the String jsonType to JdbcType.
	 */
	// @SuppressWarnings("serial")
	// private static final Map<String, Integer> JSONTYPETOJDBCTYPEMAP =
	// new HashMap<String, Integer>() {
	// {
	//
	// put("int", Types.INTEGER);
	// put("long", Types.BIGINT);
	// put("String", Types.VARCHAR);
	// put("boolean", Types.BOOLEAN);
	// put("double", Types.DOUBLE);
	//
	// }
	// };
	/**
	 * /** to json "repositories".
	 */
	private static final String REPOSITORIES = "repositories";

	/**
	 * /** to json "name".
	 */
	private static final String NAME = "name";

	/**
	 * /** to json "keys".
	 */
	private static final String KEYS = "keys";

	/**
	 * /** to json "type".
	 */
	private static final String TYPE = "type";

	/**
	 * /** to json "nullable".
	 */
	private static final String NULLABLE = "nullable";

	/**
	 * /** to json "length".
	 */
	private static final String LENGTH = "length";

	/**
	 * ** to json "iskey".
	 */
	private static final String ISKEY = "iskey";

	/**
	 * the default key name.
	 */
	private static String defaultKeyName = "oId";

	/**
	 * Stores all repository filed definition in a Map.
	 *
	 * <p>
	 * key: the name of the repository value (or table name with prefix): list
	 * of all the FieldDefinition
	 * </p>
	 */
	private static Map<String, List<FieldDefinition>> repositoriesMap = null;

	/**
	 * Sets the default key name.
	 *
	 * @param keyName
	 *            the specified key name
	 */
	public static void setDefaultKeyName(final String keyName) {
		defaultKeyName = keyName;
	}

	/**
	 * Gets the default key name.
	 *
	 * @return default key name
	 */
	public static String getDefaultKeyName() {
		return defaultKeyName;
	}

	/**
	 * get the RepositoriesMap ,lazy load.
	 *
	 * @return Map<String, List<FieldDefinition>>
	 */
	public static Map<String, List<FieldDefinition>> getRepositoriesMap() {
		if (repositoriesMap == null) {
			try {
				initRepositoriesMap();
			} catch (final Exception e) {
				logger.error("initRepositoriesMap mistake " + e.getMessage(), e);
			}
		}

		return repositoriesMap;
	}

	/**
	 * init the repositoriesMap.
	 *
	 * @throws JSONException
	 *             JSONException
	 * @throws RepositoryException
	 *             RepositoryException
	 */
	private static void initRepositoriesMap() throws JSONException, RepositoryException {
		final JSONObject jsonObject = Repositories.getRepositoriesDescription();

		if (jsonObject == null) {
			logger.warn("the repository description[repository.json] miss");
			return;
		}

		jsonToRepositoriesMap(jsonObject);
	}

	/**
	 * analysis json data structure to java Map structure.
	 *
	 * @param jsonObject
	 *            json Model
	 * @throws JSONException
	 *             JSONException
	 */
	private static void jsonToRepositoriesMap(final JSONObject jsonObject) throws JSONException {
		repositoriesMap = new HashMap<>();

		final JSONArray repositoritArray = jsonObject.getJSONArray(REPOSITORIES);

		JSONObject repositoryObject;
		JSONObject fieldDefinitionObject;

		for (int i = 0; i < repositoritArray.length(); i++) {
			repositoryObject = repositoritArray.getJSONObject(i);
			final String repositoryName = repositoryObject.getString(NAME);

			final List<FieldDefinition> fieldDefinitions = new ArrayList<>();

			repositoriesMap.put(repositoryName, fieldDefinitions);

			final JSONArray keysJsonArray = repositoryObject.getJSONArray(KEYS);

			FieldDefinition definition;

			for (int j = 0; j < keysJsonArray.length(); j++) {
				fieldDefinitionObject = keysJsonArray.getJSONObject(j);
				definition = fillFieldDefinitionData(fieldDefinitionObject);
				fieldDefinitions.add(definition);
			}
		}
	}

	/**
	 * fillFieldDefinitionData.
	 *
	 * @param fieldDefinitionObject
	 *            josn model
	 * @return {@link FieldDefinition}
	 * @throws JSONException
	 *             JSONException
	 */
	private static FieldDefinition fillFieldDefinitionData(final JSONObject fieldDefinitionObject)
			throws JSONException {
		final FieldDefinition fieldDefinition = new FieldDefinition();

		fieldDefinition.setName(fieldDefinitionObject.getString(NAME));

		// final Integer type =
		// JSONTYPETOJDBCTYPEMAP
		// .get(fieldDefinitionObject.getString(TYPE));
		// if (type == null) {
		// LOGGER.severe("the type [" + fieldDefinitionObject.getString(TYPE)
		// + "] no mapping defined now!!!!");
		// throw new RuntimeException("the type ["
		// + fieldDefinitionObject.getString(TYPE)
		// + "] no mapping defined now!!!!");
		// }
		fieldDefinition.setType(fieldDefinitionObject.getString(TYPE));
		fieldDefinition.setNullable(fieldDefinitionObject.optBoolean(NULLABLE));
		fieldDefinition.setLength(fieldDefinitionObject.optInt(LENGTH));
		fieldDefinition.setIsKey(fieldDefinitionObject.optBoolean(ISKEY));

		/**
		 * the default key name is 'old'.
		 */
		if (defaultKeyName.equals(fieldDefinition.getName())) {
			fieldDefinition.setIsKey(true);
		}

		return fieldDefinition;

	}

	/**
	 * createTableResult model for view to show.
	 *
	 */
	public static class CreateTableResult {

		/**
		 * table name.
		 */
		private String name;

		/**
		 * isCreate success.
		 */
		private boolean isSuccess;

		/**
		 *
		 * @return name
		 */
		public String getName() {
			return name;
		}

		/**
		 *
		 * @param name
		 *            tableName
		 */
		public void setName(final String name) {
			this.name = name;
		}

		/**
		 *
		 * @return isSuccess
		 */
		public boolean isSuccess() {
			return isSuccess;
		}

		/**
		 *
		 * @param isSuccess
		 *            isSuccess
		 */
		public void setSuccess(final boolean isSuccess) {
			this.isSuccess = isSuccess;
		}

		/**
		 * constructor.
		 *
		 * @param name
		 *            table
		 * @param isSuccess
		 *            isSuccess
		 */
		public CreateTableResult(final String name, final boolean isSuccess) {
			super();
			this.name = name;
			this.isSuccess = isSuccess;
		}
	}

	/**
	 * Initializes all tables from repository.json.
	 *
	 * @return List<CreateTableResult>
	 */
	public static List<CreateTableResult> initAllTables() {
		final List<CreateTableResult> ret = new ArrayList<>();
		final Map<String, List<FieldDefinition>> map = getRepositoriesMap();

		boolean isSuccess = false;

		for (final String tableName : map.keySet()) {
			try {
				isSuccess = JdbcFactory.createJdbcFactory().createTable(tableName, map.get(tableName));
			} catch (final SQLException e) {
				logger.error("createTable[" + tableName + "] error", e);
			}

			ret.add(new CreateTableResult(tableName, isSuccess));
		}

		return ret;
	}

	/**
	 * Generates repository.json from databases.
	 *
	 * @param tableNames
	 *            the specified table names for generation
	 * @param destPath
	 *            the specified path of repository.json file to generate
	 */
	public void initRepositoryJSON(final Set<String> tableNames, final String destPath) {
		Connection connection;
		FileWriter writer = null;

		try {
			connection = jdbcTemplate.getDataSource().getConnection();

			final DatabaseMetaData databaseMetaData = connection.getMetaData();
			final ResultSet resultSet = databaseMetaData.getTables(null, "%", "%", new String[] { "TABLE" });

			final JSONObject repositoryJSON = new JSONObject();
			final JSONArray repositories = new JSONArray();

			repositoryJSON.put("repositories", repositories);

			while (resultSet.next()) {
				final String tableName = resultSet.getString("TABLE_NAME");

				if (!tableNames.contains(tableName)) {
					continue;
				}

				final JSONObject repository = new JSONObject();

				repositories.put(repository);

				repository.put("name", tableName);
				final JSONArray keys = new JSONArray();

				repository.put("keys", keys);

				final ResultSet rs = databaseMetaData.getColumns(null, "%", tableName, "%");

				while (rs.next()) {
					final String columnName = rs.getString("COLUMN_NAME");
					final String remarks = rs.getString("REMARKS");
					final int dataType = rs.getInt("DATA_TYPE");
					final int length = rs.getInt("COLUMN_SIZE");
					final int nullable = rs.getInt("NULLABLE");

					final JSONObject key = new JSONObject();

					keys.put(key);

					key.put("name", columnName);
					if (!StringUtils.isBlank(remarks)) {
						key.put("description", remarks);
					}
					key.put("nullable", 0 == nullable ? false : true);
					switch (dataType) {
					case Types.CHAR:
					case Types.LONGNVARCHAR:
					case Types.LONGVARCHAR:
					case Types.NCHAR:
					case Types.NVARCHAR:
					case Types.VARCHAR:
						key.put("type", "String");
						break;

					case Types.BIGINT:
					case Types.INTEGER:
					case Types.SMALLINT:
					case Types.TINYINT:
						key.put("type", "int");
						break;

					case Types.DATE:
						key.put("type", "Date");
						break;

					case Types.TIME:
					case Types.TIMESTAMP:
						key.put("type", "Datetime");
						break;

					case Types.DECIMAL:
					case Types.NUMERIC:
						key.put("type", "Decimal");
						key.put("precision", rs.getInt("DECIMAL_DIGITS"));
						break;

					case Types.BIT:
						key.put("type", "Bit");
						break;

					case Types.CLOB:
						key.put("type", "Clob");
						break;

					case Types.BLOB:
						key.put("type", "Blob");
						break;

					default:
						throw new IllegalStateException("Unsupported type [" + dataType + ']');
					}
					key.put("length", length);

				}
			}

			final File file = new File(destPath);

			FileUtils.deleteQuietly(file);

			writer = new FileWriter(file);

			final String content = repositoryJSON.toString(Integer.valueOf("4"));

			IOUtils.write(content, writer);
		} catch (final Exception e) {
			logger.error("Init repository.json failed", e);
		} finally {
			IOUtils.closeQuietly(writer);
		}
	}

	/**
	 * set the repositoriesMap.
	 *
	 * @param repositoriesMap
	 *            repositoriesMap
	 */
	public static void setRepositoriesMap(final Map<String, List<FieldDefinition>> repositoriesMap) {
		JdbcRepositories.repositoriesMap = repositoriesMap;
	}

	/**
	 * Private constructor.
	 */
	private JdbcRepositories() {
	}
}
