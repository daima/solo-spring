package com.baidu.ueditor.define;

import java.util.HashMap;
import java.util.Map;

public final class ActionMap {
	public static final Map<String, Integer> mapping = new HashMap<String, Integer>();
	public static final int CONFIG = 0;
	public static final int UPLOAD_IMAGE = 1;
	public static final int UPLOAD_SCRAWL = 2;
	public static final int UPLOAD_VIDEO = 3;
	public static final int UPLOAD_FILE = 4;
	public static final int CATCH_IMAGE = 5;
	public static final int LIST_FILE = 6;
	public static final int LIST_IMAGE = 7;
	static {
		mapping.put("config".toLowerCase(), CONFIG);
		mapping.put("UPLOADIMAGE".toLowerCase(), UPLOAD_IMAGE);
		mapping.put("UPLOADSCRAWL".toLowerCase(), UPLOAD_SCRAWL);
		mapping.put("UPLOADVIDEO".toLowerCase(), UPLOAD_VIDEO);
		mapping.put("UPLOADFILE".toLowerCase(), UPLOAD_FILE);
		mapping.put("CATCHIMAGE".toLowerCase(), CATCH_IMAGE);
		mapping.put("LISTFILE".toLowerCase(), LIST_FILE);
		mapping.put("LISTIMAGE".toLowerCase(), LIST_IMAGE);
	}

	public static int getType(String key) {
		return ((Integer) mapping.get(key)).intValue();
	}
}