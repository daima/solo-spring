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
package org.b3log.solo.frame.image;

import java.util.List;

/**
 * Image service.
 *
 * @author <a href="http://cxy7.com">XyCai</a>
 * @version 1.0.0.1, Aug 16, 2011
 */
public interface ImageService {

	/**
	 * Makes an image with the specified data.
	 * 
	 * @param data
	 *            the specified data
	 * @return image
	 */
	Image makeImage(final byte[] data);

	/**
	 * Makes an image with the specified images from left to right respectively.
	 * 
	 * <p>
	 * Each of image of the specified images should have the same dimension.
	 * </p>
	 * 
	 * @param images
	 *            the specified images
	 * @return image
	 */
	Image makeImage(final List<Image> images);
}
