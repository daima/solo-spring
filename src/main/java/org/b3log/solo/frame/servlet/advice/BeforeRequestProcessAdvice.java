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
package org.b3log.solo.frame.servlet.advice;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.b3log.solo.frame.ioc.inject.Named;
import org.b3log.solo.frame.ioc.inject.Singleton;
import org.b3log.solo.frame.servlet.HTTPRequestContext;


/**
 * BeforeRequestProcessAdvice.
 * @author <a href="mailto:wmainlove@gmail.com">Love Yao</a>
 * @version 1.0.0.0, Sep 30, 2012
 */
@Named("LatkeBuiltInBeforeRequestProcessAdvice")
@Singleton
public class BeforeRequestProcessAdvice implements RequestProcessAdvice {

    /**
     * doAdvice.
     * @param context {@link HTTPRequestContext}
     * @param args the invoke method params and values.
     * @throws RequestProcessAdviceException the exception
     */
    public void doAdvice(final HttpServletRequest request, final HttpServletResponse response, final Map<String, Object> args) throws RequestProcessAdviceException {}
}
