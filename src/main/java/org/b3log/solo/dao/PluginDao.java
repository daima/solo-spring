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
package org.b3log.solo.dao;

import org.b3log.solo.model.Plugin;
import org.springframework.stereotype.Repository;

/**
 * Plugin repository.
 *
 * @author <a href="http://cxy7.com">XyCai</a>
 * @version 1.0.0.0, Jul 21, 2011
 * @since 0.3.1
 */
@Repository
public class PluginDao extends AbstractBlogDao {

	@Override
	public String getTableNamePostfix() {
		return Plugin.PLUGIN;
	}

}
