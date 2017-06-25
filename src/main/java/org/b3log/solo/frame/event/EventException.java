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
package org.b3log.solo.frame.event;

/**
 * Event exception.
 *
 * @author <a href="http://cxy7.com">XyCai</a>
 * @version 1.0.0.0, Aug 16, 2010
 */
public final class EventException extends Exception {

	/**
	 * Public default constructor.
	 */
	public EventException() {
		super("Event exception!");
	}

	/**
	 * Public constructor with {@link Throwable}.
	 *
	 * @param throwable
	 *            the specified throwable object
	 */
	public EventException(final Throwable throwable) {
		super(throwable);
	}

	/**
	 * Public constructor with message.
	 *
	 * @param msg
	 *            the specified message
	 */
	public EventException(final String msg) {
		super(msg);
	}
}
