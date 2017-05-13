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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMethod;
import org.b3log.solo.frame.thread.ThreadService;
import org.b3log.solo.frame.thread.ThreadServiceFactory;
import org.b3log.solo.frame.urlfetch.HTTPRequest;
import org.b3log.solo.frame.urlfetch.HTTPResponse;
import org.b3log.solo.frame.urlfetch.URLFetchService;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

/**
 * Local URL fetch service.
 *
 * @author <a href="mailto:wmainlove@gmail.com">Love Yao</a>
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.1.1.4, Jan 23, 2017
 */
public final class LocalURLFetchService implements URLFetchService {

    /**
     * Logger.
     */
    private static Logger logger = LoggerFactory.getLogger(LocalURLFetchService.class);

    /**
     * Timeout for async fetch.
     */
    private static final long ASYNC_TIME_OUT = 30000;

    /**
     * Thread service.
     */
    private ThreadService threadService = ThreadServiceFactory.getThreadService();

    @Override
    public HTTPResponse fetch(final HTTPRequest request) throws IOException {
        final RequestMethod requestMethod = request.getRequestMethod();

        if (requestMethod == null) {
            throw new IOException("RequestMethod  for URLFetch should not be null");
        }

        return UrlFetchHandlerFactory.getFetchHandler(requestMethod).doFetch(request);
    }

    @Override
    public Future<?> fetchAsync(final HTTPRequest request) {
        final FutureTask<HTTPResponse> futureTask = new FetchTask(new Callable<HTTPResponse>() {
            @Override
            public HTTPResponse call() throws Exception {
                logger.debug( "Fetch async, request=[" + request.toString() + "]");

                return fetch(request);
            }
        }, request);


        threadService.submit(futureTask, ASYNC_TIME_OUT);

        return futureTask;
    }

    /**
     * URL fetch task.
     *
     * @author <a href="http://88250.b3log.org">Liang Ding</a>
     * @version 1.0.0.0, Jan 23, 2017
     */
    private static class FetchTask extends FutureTask<HTTPResponse> {

        /**
         * Request.
         */
        private HTTPRequest request;

        /**
         * Constructs a fetch task with the specified callable and request.
         *
         * @param callable the specified callable
         * @param request  the specified request
         */
        FetchTask(final Callable<HTTPResponse> callable, final HTTPRequest request) {
            super(callable);

            this.request = request;
        }

        @Override
        public String toString() {
            return "URL Fetch [request=" + request.toString() + "]";
        }
    }
}
