package com.baidu.ueditor.define;

import java.util.HashMap;
import java.util.Map;

public final class AppInfo {
	public static final int SUCCESS = 0;
	public static final int MAX_SIZE = 1;
	public static final int PERMISSION_DENIED = 2;
	public static final int FAILED_CREATE_FILE = 3;
	public static final int IO_ERROR = 4;
	public static final int NOT_MULTIPART_CONTENT = 5;
	public static final int PARSE_REQUEST_ERROR = 6;
	public static final int NOTFOUND_UPLOAD_DATA = 7;
	public static final int NOT_ALLOW_FILE_TYPE = 8;
	public static final int INVALID_ACTION = 101;
	public static final int CONFIG_ERROR = 102;
	public static final int PREVENT_HOST = 201;
	public static final int CONNECTION_ERROR = 202;
	public static final int REMOTE_FAIL = 203;
	public static final int NOT_DIRECTORY = 301;
	public static final int NOT_EXIST = 302;
	public static final int ILLEGAL = 401;
	public static Map<Integer, String> info = new HashMap<Integer, String>();
	static {
		info.put(SUCCESS, "SUCCESS");
		info.put(MAX_SIZE, "MAX_SIZE");
		info.put(PERMISSION_DENIED, "PERMISSION_DENIED");
		info.put(FAILED_CREATE_FILE, "FAILED_CREATE_FILE");
		info.put(IO_ERROR, "IO_ERROR");
		info.put(NOT_MULTIPART_CONTENT, "NOT_MULTIPART_CONTENT");
		info.put(PARSE_REQUEST_ERROR, "PARSE_REQUEST_ERROR");
		info.put(NOTFOUND_UPLOAD_DATA, "NOTFOUND_UPLOAD_DATA");
		info.put(NOT_ALLOW_FILE_TYPE, "NOT_ALLOW_FILE_TYPE");
		info.put(INVALID_ACTION, "INVALID_ACTION");
		info.put(CONFIG_ERROR, "CONFIG_ERROR");
		info.put(PREVENT_HOST, "PREVENT_HOST");
		info.put(CONNECTION_ERROR, "CONNECTION_ERROR");
		info.put(REMOTE_FAIL, "REMOTE_FAIL");
		info.put(NOT_DIRECTORY, "NOT_DIRECTORY");
		info.put(NOT_EXIST, "NOT_EXIST");
		info.put(ILLEGAL, "ILLEGAL");
	}

	public static String getStateInfo(int key) {
		return (String) info.get(Integer.valueOf(key));
	}
}