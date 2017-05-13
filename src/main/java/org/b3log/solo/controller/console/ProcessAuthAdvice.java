package org.b3log.solo.controller.console;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.b3log.solo.frame.servlet.advice.BeforeRequestProcessAdvice;
import org.b3log.solo.frame.servlet.advice.RequestProcessAdviceException;
import org.b3log.solo.frame.servlet.advice.RequestReturnAdviceException;
import org.b3log.solo.service.UserQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 *  The common auth check before advice for admin console.
 * 
 * @author <a href="mailto:wmainlove@gmail.com">Love Yao</a>
 * @version 1.0.0.1, Jal 18, 2013
 */
@Component
public class ProcessAuthAdvice extends BeforeRequestProcessAdvice {
	@Autowired
	private UserQueryService userQueryService;
    @Override
    public void doAdvice(HttpServletRequest request, HttpServletResponse response, final Map<String, Object> args) throws RequestProcessAdviceException {
        if (!userQueryService.isLoggedIn(request, response)) {
            try {
            	response.sendError(HttpServletResponse.SC_FORBIDDEN);
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
            throw new RequestReturnAdviceException(null);
        }

    }

}
