/*
 * Copyright (c) 2010-2017, b3log.org & hacpai.com
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
package org.b3log.solo.controller;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.IOUtils;
import org.b3log.solo.Latkes;
import org.b3log.solo.RuntimeEnv;
import org.b3log.solo.frame.image.Image;
import org.b3log.solo.frame.image.ImageService;
import org.b3log.solo.frame.image.ImageServiceFactory;
import org.b3log.solo.renderer.PNGRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Captcha processor.
 *
 * <p>
 * Checkout <a href="http://toy-code.googlecode.com/svn/trunk/CaptchaGenerator">
 * the sample captcha generator</a> for more details.
 * </p>
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.1.0.4, Oct 31, 2015
 * @since 0.3.1
 */
@Controller
public class CaptchaProcessor {

	/**
	 * Logger.
	 */
	private static Logger logger = LoggerFactory.getLogger(CaptchaProcessor.class);

	/**
	 * Images service.
	 */
	private static final ImageService IMAGE_SERVICE = ImageServiceFactory.getImageService();

	/**
	 * Key of captcha.
	 */
	public static final String CAPTCHA = "captcha";

	/**
	 * Captchas.
	 */
	private Image[] captchas;

	/**
	 * Count of static captchas.
	 */
	private static final int CAPTCHA_COUNT = 100;

	/**
	 * Gets captcha.
	 * 
	 * @param context
	 *            the specified context
	 */
	@RequestMapping(value = "/captcha.do", method = RequestMethod.GET)
	public void get(final HttpServletRequest request, final HttpServletResponse response) {
		final PNGRenderer renderer = new PNGRenderer();

		if (null == captchas) {
			loadCaptchas();
		}

		try {
			final Random random = new Random();
			final int index = random.nextInt(CAPTCHA_COUNT);
			final Image captchaImg = captchas[index];
			final String captcha = captchaImg.getName();

			final HttpSession httpSession = request.getSession(false);

			if (null != httpSession) {
				logger.debug("Captcha[{}] for session[id={}]", captcha, httpSession.getId());
				httpSession.setAttribute(CAPTCHA, captcha);
			}

			response.setHeader("Pragma", "no-cache");
			response.setHeader("Cache-Control", "no-cache");
			response.setDateHeader("Expires", 0);

			renderer.setImage(captchaImg);
		} catch (final Exception e) {
			logger.error(e.getMessage(), e);
		}
		renderer.render(request, response);
	}

	/**
	 * Loads captcha.
	 */
	private synchronized void loadCaptchas() {
		logger.debug("Loading captchas....");

		try {
			captchas = new Image[CAPTCHA_COUNT];

			ZipFile zipFile;

			if (RuntimeEnv.LOCAL == Latkes.getRuntimeEnv()) {
				final InputStream inputStream = CaptchaProcessor.class.getClassLoader()
						.getResourceAsStream("captcha_static.zip");
				final File file = File.createTempFile("b3log_captcha_static", null);
				final OutputStream outputStream = new FileOutputStream(file);

				IOUtils.copy(inputStream, outputStream);
				zipFile = new ZipFile(file);

				IOUtils.closeQuietly(inputStream);
				IOUtils.closeQuietly(outputStream);
			} else {
				final URL captchaURL = CaptchaProcessor.class.getClassLoader().getResource("captcha_static.zip");

				zipFile = new ZipFile(captchaURL.getFile());
			}

			final Enumeration<? extends ZipEntry> entries = zipFile.entries();

			int i = 0;

			while (entries.hasMoreElements()) {
				final ZipEntry entry = entries.nextElement();

				final BufferedInputStream bufferedInputStream = new BufferedInputStream(zipFile.getInputStream(entry));
				final byte[] captchaCharData = new byte[bufferedInputStream.available()];

				bufferedInputStream.read(captchaCharData);
				bufferedInputStream.close();

				final Image image = IMAGE_SERVICE.makeImage(captchaCharData);

				image.setName(entry.getName().substring(0, entry.getName().lastIndexOf('.')));

				captchas[i] = image;

				i++;
			}

			zipFile.close();
		} catch (final Exception e) {
			logger.error("Can not load captchs!");

			throw new IllegalStateException(e);
		}

		logger.debug("Loaded captch images");
	}
}
