package org.lucee.extension.axis.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.runtime.net.proxy.ProxyData;
import lucee.runtime.type.Collection;
import lucee.runtime.type.Struct;
import lucee.runtime.type.Collection.Key;

public class StringUtil {
	private static final long[] byteTable = createLookupTable();
	private static final long HSTART = 0xBB40E64DA205B064L;
	private static final long HMULT = 7664345821815920749L;

	public static final char CACHE_DEL = ';';
	public static final char CACHE_DEL2 = ':';
	

	public static String create64BitHashAsString(CharSequence cs) {
		return Long.toString(create64BitHash(cs), Character.MAX_RADIX);
	}
	
	public static long create64BitHash(CharSequence cs) {
		long h = HSTART;
		final long hmult = HMULT;
		final long[] ht = byteTable;
		final int len = cs.length();
		for (int i = 0; i < len; i++) {
			char ch = cs.charAt(i);
			h = (h * hmult) ^ ht[ch & 0xff];
			h = (h * hmult) ^ ht[(ch >>> 8) & 0xff];
		}
		if(h<0)
			return 0-h;
		return h;
	}
	
	private static final long[] createLookupTable() {
		long[] _byteTable = new long[256];
		long h = 0x544B2FBACAAF1684L;
		for (int i = 0; i < 256; i++) {
			for (int j = 0; j < 31; j++) {
				h = (h >>> 7) ^ h;
				h = (h << 11) ^ h;
				h = (h >>> 10) ^ h;
			}
			_byteTable[i] = h;
		}
		return _byteTable;
	}
	

	public static String[] keysAsString(Collection coll) {
		if(coll==null) return new String[0];
		Iterator<Key> it = coll.keyIterator();
		List<String> rtn=new ArrayList<String>();
		if(it!=null)while(it.hasNext()){
			rtn.add(it.next().getString());
		}
		return rtn.toArray(new String[rtn.size()]);
	}

}
