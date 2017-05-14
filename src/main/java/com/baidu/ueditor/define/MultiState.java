package com.baidu.ueditor.define;

import com.baidu.ueditor.Encoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MultiState implements State {
	private boolean state = false;
	private String info = null;
	private Map<String, Long> intMap = new HashMap();
	private Map<String, String> infoMap = new HashMap();
	private List<String> stateList = new ArrayList();

	public MultiState(boolean state) {
		this.state = state;
	}

	public MultiState(boolean state, String info) {
		this.state = state;
		this.info = info;
	}

	public MultiState(boolean state, int infoKey) {
		this.state = state;
		this.info = AppInfo.getStateInfo(infoKey);
	}

	public boolean isSuccess() {
		return this.state;
	}

	public void addState(State state) {
		this.stateList.add(state.toJSONString());
	}

	public void putInfo(String name, String val) {
		this.infoMap.put(name, val);
	}

	public String toJSONString() {
		String stateVal = isSuccess() ? AppInfo.getStateInfo(0) : this.info;

		StringBuilder builder = new StringBuilder();

		builder.append("{\"state\": \"" + stateVal + "\"");

		Iterator<String> iterator = this.intMap.keySet().iterator();

		while (iterator.hasNext()) {
			stateVal = (String) iterator.next();

			builder.append(",\"" + stateVal + "\": " + this.intMap.get(stateVal));
		}

		iterator = this.infoMap.keySet().iterator();

		while (iterator.hasNext()) {
			stateVal = (String) iterator.next();

			builder.append(",\"" + stateVal + "\": \"" + (String) this.infoMap.get(stateVal) + "\"");
		}

		builder.append(", list: [");

		iterator = this.stateList.iterator();

		while (iterator.hasNext()) {
			builder.append((String) iterator.next() + ",");
		}

		if (this.stateList.size() > 0) {
			builder.deleteCharAt(builder.length() - 1);
		}

		builder.append(" ]}");

		return Encoder.toUnicode(builder.toString());
	}

	public void putInfo(String name, long val) {
		this.intMap.put(name, Long.valueOf(val));
	}
}