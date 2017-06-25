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
package org.b3log.solo.frame.thread.local;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.b3log.solo.frame.thread.ThreadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Local thread service.
 *
 * @author <a href="http://cxy7.com">XyCai</a>
 * @version 1.1.1.2, Jan 23, 2017
 */
public final class LocalThreadService implements ThreadService {

	/**
	 * Logger.
	 */
	private static Logger logger = LoggerFactory.getLogger(LocalThreadService.class);

	/**
	 * Executor service.
	 */
	public static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(50);

	@Override
	public Thread createThreadForCurrentRequest(final Runnable runnable) {
		return Executors.defaultThreadFactory().newThread(runnable);
	}

	@Override
	public Future<?> submit(final Runnable runnable, final long millseconds) {
		final Object monitor = new Object();
		final Worker worker = new Worker(runnable, millseconds, monitor);

		synchronized (monitor) {
			EXECUTOR_SERVICE.execute(worker);

			try {
				monitor.wait();
			} catch (final Exception e) {
				logger.error("Wait failed", e);
			}
		}

		return worker.getFuture();
	}

	/**
	 * Worker.
	 */
	private static class Worker implements Runnable {

		/**
		 * Future.
		 */
		private Future<?> future;

		/**
		 * Runnable.
		 */
		private final Runnable runnable;

		/**
		 * Timeout.
		 */
		private final long timeout;

		/**
		 * Object.
		 */
		private Object monitor;

		/**
		 * Constructs a worker.
		 *
		 * @param runnable
		 *            the specified runnable
		 * @param timeout
		 *            the specified timeout
		 * @param monitor
		 *            the specified monitor
		 */
		Worker(final Runnable runnable, final long timeout, final Object monitor) {
			this.runnable = runnable;
			this.timeout = timeout;
			this.monitor = monitor;
		}

		/**
		 * Get the future.
		 *
		 * @return future
		 */
		public Future<?> getFuture() {
			return future;
		}

		@Override
		public void run() {
			synchronized (monitor) {
				try {
					future = EXECUTOR_SERVICE.submit(runnable);

					future.get(timeout, TimeUnit.MILLISECONDS);
				} catch (final Exception e) {
					logger.warn("Task executes failed [runnable=" + runnable.toString() + "]", e);

					future = null;
				}

				monitor.notify();
			}
		}
	}
}
