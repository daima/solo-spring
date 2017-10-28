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
package org.b3log.solo.frame.thread;

import java.util.concurrent.Future;

/**
 * Thread service.
 *
 * @author <a href="http://cxy7.com">XyCai</a>
 * @version 1.0.0.1, Jan 10, 2014
 */
public interface ThreadService {

	/**
	 * Creates a new thread that executes the specified runnable for the
	 * duration of the current request.
	 *
	 * @param runnable
	 *            the specified runnable
	 * @return a new thread
	 */
	Thread createThreadForCurrentRequest(final Runnable runnable);

	/**
	 * Submits the specified {@code Runnable} task for execution and returns a
	 * {@code Future} representing that task within the specified millseconds.
	 * 
	 * @param runnable
	 *            the specified runnable task
	 * @param millseconds
	 *            the specified millseconds timeout
	 * @return a {@code Future} representing pending completion of the task,
	 *         returns {@code null} if the task executes failed
	 */
	Future<?> submit(final Runnable runnable, final long millseconds);
}
