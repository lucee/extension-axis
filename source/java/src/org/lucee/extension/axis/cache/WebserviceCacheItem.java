package org.lucee.extension.axis.cache;

import java.io.Serializable;

import org.lucee.extension.axis.util.SimpleDumpData;
import org.lucee.extension.axis.util.StringUtil;

import lucee.runtime.PageContext;
import lucee.runtime.cache.tag.CacheItem;
import lucee.runtime.dump.DumpData;
import lucee.runtime.dump.DumpProperties;
import lucee.runtime.dump.DumpTable;
import lucee.runtime.dump.Dumpable;
import lucee.runtime.type.Collection;

public class WebserviceCacheItem implements CacheItem, Serializable, Dumpable {

	private static final long serialVersionUID = -8462614105941179140L;

	private Object data;
	private String url;
	private String methodName;
	private long executionTimeNS;

	public WebserviceCacheItem(Object data, String url, String methodName, long executionTimeNS) {
		this.data = data;
		this.url = url;
		this.methodName = methodName;
		this.executionTimeNS = executionTimeNS;
	}

	@Override
	public DumpData toDumpData(PageContext pageContext, int maxlevel, DumpProperties properties) {
		DumpTable table = new DumpTable("#669999", "#ccffff", "#000000");
		table.setTitle("WebserviceCacheEntry");
		table.appendRow(1, new SimpleDumpData("URL"), new SimpleDumpData(url));
		table.appendRow(1, new SimpleDumpData("Method Name"), new SimpleDumpData(methodName));

		return table;
	}

	@Override
	public String toString() {
		return data.toString();
	}

	@Override
	public String getHashFromValue() {
		return Long.toString(StringUtil.create64BitHash(data.toString()));
	}

	public Object getData() {
		return data;
	}

	@Override
	public String getName() {
		return url + "&method=" + methodName;
	}

	@Override
	public long getPayload() {
		return data instanceof Collection ? ((Collection) data).size() : 1;
	}

	@Override
	public String getMeta() {
		return url;
	}

	@Override
	public long getExecutionTime() {
		return executionTimeNS;
	}

}