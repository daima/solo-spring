/*
 * Copyright (c) 2009-2016, b3log.org & hacpai.com
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
package org.b3log.solo.frame.repository.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

import org.b3log.solo.frame.repository.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 *
 * JdbcTransaction.
 *
 * @author <a href="mailto:wmainlove@gmail.com">Love Yao</a>
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.0.0.2, Mar 6, 2013
 */
@Repository
public final class JdbcTransaction implements Transaction {

	/**
	 * Connection.
	 */
	// private Connection connection;

	/**
	 * Is active.
	 */
	private boolean isActive;
	private Connection connection;
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Override
	public String getId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void commit() {
		boolean ifSuccess = false;

		try {
			connection.commit();
			ifSuccess = true;
		} catch (final SQLException e) {
			throw new RuntimeException("commit mistake", e);
		}

		if (ifSuccess) {
			dispose();
		}
	}

	@Override
	public void rollback() {
		try {
			connection.rollback();
		} catch (final SQLException e) {
			throw new RuntimeException("rollback mistake", e);
		} finally {
			dispose();
		}
	}

	/**
	 * setActive.
	 * 
	 * @param isActive
	 *            isActive
	 */
	public void setActive(final boolean isActive) {
		this.isActive = isActive;
	}

	@Override
	public boolean isActive() {
		return isActive;
	}

	/**
	 * close the connection.
	 */
	public void dispose() {
		try {
			connection.close();

			JdbcRepository.TX.set(null);
		} catch (final SQLException e) {
			throw new RuntimeException("close connection", e);
		} finally {
			isActive = false;
			connection = null;
		}
	}

	/**
	 * getConnection.
	 * 
	 * @return {@link Connection}
	 */
	public Connection getConnection() throws SQLException {
		if (connection == null) {
			connection = jdbcTemplate.getDataSource().getConnection();
			connection.setAutoCommit(false);
			isActive = true;
		}
		return connection;
	}
}
