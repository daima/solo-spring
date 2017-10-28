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

/**
 * Transaction.
 *
 * @author <a href="http://cxy7.com">XyCai</a>
 * @version 1.0.0.2, Sep 11, 2011
 */
public interface Transaction {

	/**
	 * Gets the id of this transaction.
	 * 
	 * @return id
	 */
	String getId();

	/**
	 * Commits this transaction.
	 *
	 * <p>
	 * <b>Throws</b>:<br/>
	 * {@link java.lang.IllegalStateException} - if the transaction has already
	 * been committed, rolled back
	 * </p>
	 */
	void commit();

	/**
	 * Rolls back this transaction.
	 *
	 * <p>
	 * <b>Throws</b>:<br/>
	 * {@link java.lang.IllegalStateException} - if the transaction has already
	 * been committed, rolled back
	 * </p>
	 */
	void rollback();

	/**
	 * Determines whether this transaction is active.
	 *
	 * @return {@code true} if this transaction is active, {@code false}
	 *         otherwise
	 */
	boolean isActive();
}
