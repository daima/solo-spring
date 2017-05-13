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
package org.b3log.solo.frame.ioc.context;


/**
 * <p>Indicates a problem relating to context management.</p>
 * 
 * @author Pete Muir
 * @author Shane Bryzak
 */
public class ContextException extends RuntimeException
{
   
   private static final long serialVersionUID = -3599813072560026919L;

   public ContextException()
   {
      super();
   }
   
   public ContextException(String message)
   {
      super(message);
   }
   
   public ContextException(Throwable cause)
   {
      super(cause);
   }
   
   public ContextException(String message, Throwable cause)
   {
      super(message, cause);
   }
   
}
