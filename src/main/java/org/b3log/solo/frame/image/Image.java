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
package org.b3log.solo.frame.image;


/**
 * Image.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.0.0.1, Sep 18, 2012
 */
public final class Image {
    
    /**
     * Name.
     */
    private String name;

    /**
     * Data.
     */
    private byte[] data;
    
    /**
     * Gets the name.
     * 
     * @return name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Sets the name with the specified name.
     * 
     * @param name the specified name
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Gets the data.
     * 
     * @return data
     */
    public byte[] getData() {
        return data;
    }

    /**
     * Sets the data with the specified data.
     * 
     * @param data the specified data
     */
    public void setData(final byte[] data) {
        this.data = data;
    }
}
