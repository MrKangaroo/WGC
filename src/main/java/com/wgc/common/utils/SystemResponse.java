package com.wgc.common.utils;

import java.util.HashMap;
import java.util.Map;

public class SystemResponse extends HashMap<String, Object> {
	private static final long serialVersionUID = 1L;

	public SystemResponse() {
		put("code", 0);
		put("msg", "操作成功");
	}

	public static SystemResponse error() {
		return error(1, "操作失败");
	}

	public static SystemResponse error(String msg) {
		return error(500, msg);
	}

	public static SystemResponse error(int code, String msg) {
		SystemResponse systemResponse = new SystemResponse();
		systemResponse.put("code", code);
		systemResponse.put("msg", msg);
		return systemResponse;
	}

	public static SystemResponse ok(String msg) {
		SystemResponse systemResponse = new SystemResponse();
		systemResponse.put("msg", msg);
		return systemResponse;
	}

	public static SystemResponse ok(Map<String, Object> map) {
		SystemResponse systemResponse = new SystemResponse();
		systemResponse.putAll(map);
		return systemResponse;
	}

	public static SystemResponse ok() {
		return new SystemResponse();
	}

	@Override
	public SystemResponse put(String key, Object value) {
		super.put(key, value);
		return this;
	}
}
