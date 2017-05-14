package com.baidu.ueditor;

public class Encoder {
	public static String toUnicode(String input) {
		StringBuilder builder = new StringBuilder();
		char[] chars = input.toCharArray();
		char[] arrayOfChar1;
		int j = (arrayOfChar1 = chars).length;
		for (int i = 0; i < j; i++) {
			char ch = arrayOfChar1[i];

			if (ch < 'Ā') {
				builder.append(ch);
			} else {
				builder.append("\\u" + Integer.toHexString(ch & 0xFFFF));
			}
		}

		return builder.toString();
	}
}