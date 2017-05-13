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
package org.b3log.solo.frame.urlfetch.local;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;

import org.b3log.solo.frame.urlfetch.HTTPRequest;

/**
 * PUT method handler.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.0.0.0, Oct 12, 2012
 */
class UrlFetchPutHandler extends UrlFetchCommonHandler {

	@Override
	protected void configConnection(final HttpURLConnection httpURLConnection, final HTTPRequest request)
			throws IOException {
		httpURLConnection.setDoOutput(true);
		httpURLConnection.setUseCaches(false);

		if (null != request.getPayload()) {
			final OutputStream outputStream = httpURLConnection.getOutputStream();

			outputStream.write(request.getPayload());

			outputStream.flush();
			outputStream.close();
		}

		// TODO: request.getPayloadMap()
	}
}
