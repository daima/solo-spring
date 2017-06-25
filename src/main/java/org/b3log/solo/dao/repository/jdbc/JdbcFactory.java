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
package org.b3log.solo.dao.repository.jdbc;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.b3log.solo.RuntimeDatabase;
import org.b3log.solo.dao.repository.jdbc.util.FieldDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * JDBC Factory.
 *
 * @author <a href="mailto:wmainlove@gmail.com">Love Yao</a>
 * @version 1.0.0.1, Mar 6, 2014
 */
public final class JdbcFactory implements JdbcDatabase {

	/**
	 * Logger.
	 */
	private static Logger logger = LoggerFactory.getLogger(JdbcRepository.class);

	/**
	 * the holder of the databaseSolution.
	 */
	private AbstractJdbcDatabaseSolution databaseSolution;

	/**
	 * the singleton of jdbcfactory.
	 */
	private static JdbcFactory jdbcFactory;

	/**
	 * All JdbcDatabaseSolution class names.
	 */
	@SuppressWarnings("serial")
	private static Map<RuntimeDatabase, String> jdbcDatabaseSolutionMap = new HashMap<RuntimeDatabase, String>() {
		{
			put(RuntimeDatabase.MYSQL, "org.b3log.solo.frame.repository.mysql.MysqlJdbcDatabaseSolution");
			put(RuntimeDatabase.H2, "org.b3log.solo.frame.repository.h2.H2JdbcDatabaseSolution");
			put(RuntimeDatabase.MSSQL, "org.b3log.solo.frame.repository.sqlserver.SQLServerJdbcDatabaseSolution");
		}
	};

	@Override
	public boolean createTable(final String tableName, final List<FieldDefinition> fieldDefinitions)
			throws SQLException {
		return databaseSolution.createTable(tableName, fieldDefinitions);
	}

	@Override
	public boolean clearTable(final String tableName, final boolean ifdrop) throws SQLException {
		return databaseSolution.clearTable(tableName, ifdrop);
	}

	/**
	 * singleton way to get jdbcFactory.
	 * 
	 * @return JdbcFactory jdbcFactory.
	 */
	public static synchronized JdbcFactory createJdbcFactory() {
		if (jdbcFactory == null) {
			jdbcFactory = new JdbcFactory();
		}
		return jdbcFactory;
	}

	/**
	 * Private constructor.
	 */
	private JdbcFactory() {

		/**
		 * Latkes.getRuntimeDatabase();
		 */
		// final String databaseSolutionClassName =
		// jdbcDatabaseSolutionMap.get(Latkes.getRuntimeDatabase());
		final String databaseSolutionClassName = "org.b3log.solo.dao.MysqlJdbcDatabaseSolution";

		try {
			databaseSolution = (AbstractJdbcDatabaseSolution) Class.forName(databaseSolutionClassName).newInstance();
		} catch (final Exception e) {
			logger.error("init the [" + databaseSolutionClassName + "]JdbcDatabaseSolution instance wrong", e);
		}
	}

	@Override
	public String queryPage(final int start, final int end, final String selectSql, final String filterSql,
			final String orderBySql, final String tableName) {
		return databaseSolution.queryPage(start, end, selectSql, filterSql, orderBySql, tableName);
	}

	@Override
	public String getRandomlySql(final String tableName, final int fetchSize) {
		return databaseSolution.getRandomlySql(tableName, fetchSize);
	}
}
