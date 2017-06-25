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
package org.b3log.solo.frame.urlfetch;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Encapsulation of a single HTTP request that is made via the
 * {@link URLFetchService}.
 *
 * @author <a href="http://cxy7.com">XyCai</a>
 * @version 1.1.1.1, Jul 7, 2015
 */
public final class HTTPRequest {

	/**
	 * URL.
	 */
	private URL url;

	/**
	 * Payload.
	 */
	private byte[] payload;

	/**
	 * HTTP headers.
	 */
	private List<HTTPHeader> headers = new ArrayList<>();

	/**
	 * Adds the specified HTTP header.
	 *
	 * @param header
	 *            the specified HTTP header
	 */
	public void addHeader(final HTTPHeader header) {
		headers.add(header);
	}

	/**
	 * Gets HTTP headers.
	 *
	 * @return HTTP headers
	 */
	public List<HTTPHeader> getHeaders() {
		return Collections.unmodifiableList(headers);
	}

	/**
	 * Gets the payload ({@link RequestMethod#POST POST} data body).
	 *
	 * <p>
	 * Certain HTTP methods ({@linkplain RequestMethod#GET GET}) will NOT have
	 * any payload, and this method will return {@code null}.
	 * </p>
	 *
	 * @return payload
	 */
	public byte[] getPayload() {
		return payload;
	}

	/**
	 * Sets the payload with the specified payload.
	 *
	 * <p>
	 * This method should NOT be called for certain HTTP methods (e.g.
	 * {@link RequestMethod#GET GET}).
	 * </p>
	 *
	 * @param payload
	 *            the specified payload
	 */
	public void setPayload(final byte[] payload) {
		this.payload = payload;
	}

	/**
	 * Request method.
	 */
	private RequestMethod requestMethod = RequestMethod.GET;

	/**
	 * Gets the request method.
	 *
	 * @return request method
	 */
	public RequestMethod getRequestMethod() {
		return requestMethod;
	}

	/**
	 * Sets the request method with the specified request method.
	 *
	 * @param requestMethod
	 *            the specified request method
	 */
	public void setRequestMethod(final RequestMethod requestMethod) {
		this.requestMethod = requestMethod;
	}

	/**
	 * Gets the request URL.
	 *
	 * @return request URL
	 */
	public URL getURL() {
		return url;
	}

	/**
	 * Sets the request URL with the specified URL.
	 *
	 * @param url
	 *            the specified URL
	 */
	public void setURL(final URL url) {
		this.url = url;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
	}
}
