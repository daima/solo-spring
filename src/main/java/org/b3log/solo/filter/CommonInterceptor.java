package org.b3log.solo.filter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.b3log.solo.Keys;
import org.b3log.solo.model.Option;
import org.b3log.solo.module.util.Skins;
import org.b3log.solo.service.PreferenceQueryService;
import org.b3log.solo.service.ServiceException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

public class CommonInterceptor implements HandlerInterceptor {
	private static Logger logger = LoggerFactory.getLogger(CommonInterceptor.class);
	@Autowired
	private PreferenceQueryService preferenceQueryService;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
//		request.setAttribute(Keys.HttpRequest.START_TIME_MILLIS, System.currentTimeMillis());
		try {
			JSONObject preference = preferenceQueryService.getPreference();
			// https://github.com/b3log/solo/issues/12060
			String specifiedSkin = Skins.getSkinDirName(request);
			if (null != specifiedSkin) {
				if ("default".equals(specifiedSkin)) {
					specifiedSkin = preference.optString(Option.ID_C_SKIN_DIR_NAME);
				}
			} else {
				specifiedSkin = preference.optString(Option.ID_C_SKIN_DIR_NAME);
			}
			request.setAttribute(Keys.TEMAPLTE_DIR_NAME, specifiedSkin);
		} catch (ServiceException e1) {
			e1.printStackTrace();
		}
		final String requestURI = request.getRequestURI();

		String ip = request.getRemoteAddr();
		int port = request.getRemotePort();

		logger.info("ip:{}, port:{}, request uri:{}", ip, port, requestURI);
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
