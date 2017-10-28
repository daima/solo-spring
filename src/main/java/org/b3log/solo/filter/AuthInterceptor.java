package org.b3log.solo.filter;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.b3log.solo.model.Role;
import org.b3log.solo.model.User;
import org.b3log.solo.service.UserMgmtService;
import org.b3log.solo.service.UserQueryService;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Component
public class AuthInterceptor implements HandlerInterceptor {
	private static Logger logger = LoggerFactory.getLogger(AuthInterceptor.class);
	@Autowired
	private UserMgmtService userMgmtService;
	@Autowired
	private UserQueryService userQueryService;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		try {
			userMgmtService.tryLogInWithCookie(request, response);

			final JSONObject currentUser = userQueryService.getCurrentUser(request);

			if (null == currentUser) {
				logger.warn("The request has been forbidden");
				response.sendError(HttpServletResponse.SC_FORBIDDEN);
				return false;
			}

			final String userRole = currentUser.optString(User.USER_ROLE);

			if (Role.VISITOR_ROLE.equals(userRole)) {
				logger.warn("The request [Visitor] has been forbidden");
				response.sendError(HttpServletResponse.SC_FORBIDDEN);
				return false;
			}

		} catch (final IOException e) {
			logger.error("Auth filter failed", e);
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
		}
		return true;
	}
	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			ModelAndView modelAndView) throws Exception {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
			throws Exception {
		// TODO Auto-generated method stub
		
	}

}
