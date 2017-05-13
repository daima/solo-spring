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
package org.b3log.solo.util;

import org.apache.commons.lang3.StringUtils;
import org.b3log.solo.SoloConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Call stack utilities.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.0.0.4, Apr 11, 2012
 */
public final class Callstacks {

	/**
	 * Logger.
	 */
	private static Logger logger = LoggerFactory.getLogger(Callstacks.class);

	/**
	 * Checks the current method is whether invoked by a caller specified by the
	 * given class name and method name.
	 * 
	 * @param className
	 *            the given class name
	 * @param methodName
	 *            the given method name, "*" for matching all methods
	 * @return {@code true} if it is invoked by the specified caller, returns
	 *         {@code false} otherwise
	 */
	public static boolean isCaller(final String className, final String methodName) {
		final Throwable throwable = new Throwable();
		final StackTraceElement[] stackElements = throwable.getStackTrace();

		if (null == stackElements) {
			logger.warn("Empty call stack");

			return false;
		}

		final boolean matchAllMethod = "*".equals(methodName);

		for (int i = 1; i < stackElements.length; i++) {
			if (stackElements[i].getClassName().equals(className)) {
				return matchAllMethod ? true : stackElements[i].getMethodName().equals(methodName);
			}
		}

		return false;
	}

	/**
	 * Prints call stack with the specified logging level.
	 * 
	 * @param logLevel
	 *            the specified logging level
	 * @param carePackages
	 *            the specified packages to print, for example,
	 *            ["org.b3log.latke", "org.b3log.solo"], {@code null} to care
	 *            nothing
	 * @param exceptablePackages
	 *            the specified packages to skip, for example, ["com.sun",
	 *            "java.io", "org.b3log.solo.filter"], {@code null} to skip
	 *            nothing
	 */
	public static void printCallstack(final String[] carePackages, final String[] exceptablePackages) {
		final Throwable throwable = new Throwable();
		final StackTraceElement[] stackElements = throwable.getStackTrace();

		if (null == stackElements) {
			logger.warn("Empty call stack");

			return;
		}

		final StringBuilder stackBuilder = new StringBuilder("CallStack [").append(SoloConstant.LINE_SEPARATOR);

		for (int i = 1; i < stackElements.length; i++) {
			final String stackElemClassName = stackElements[i].getClassName();

			if (!StringUtils.startsWithAny(stackElemClassName, carePackages)
					|| StringUtils.startsWithAny(stackElemClassName, exceptablePackages)) {
				continue;
			}
			stackBuilder.append("    [className=").append(stackElements[i].getClassName()).append(", fileName=")
					.append(stackElements[i].getFileName()).append(", lineNumber=")
					.append(stackElements[i].getLineNumber()).append(", methodName=")
					.append(stackElements[i].getMethodName()).append(']').append(SoloConstant.LINE_SEPARATOR);
		}
		stackBuilder.append("], full depth [").append(stackElements.length).append("]");

		logger.info(stackBuilder.toString());
	}

	/**
	 * Private constructor.
	 */
	private Callstacks() {
	}
}
