package org.b3log.solo;

import org.b3log.solo.util.PropsUtil;

public class SoloConstant {
	public static final String TMPLATE_PATH = PropsUtil.getString("basedir") + "/view";
	/**
	 * Solo version.
	 */
	public static final String VERSION = "2.0.0";
	public static final String JDBC_DEFAULTKEYNAME = "oId";
	public static final String LINE_SEPARATOR = PropsUtil.getString("line.separator");
}
