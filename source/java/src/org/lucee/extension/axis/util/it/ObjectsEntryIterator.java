package org.lucee.extension.axis.util.it;

import java.util.Iterator;
import java.util.Map.Entry;

import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.runtime.type.Collection.Key;
import lucee.runtime.type.Objects;

public class ObjectsEntryIterator implements Iterator<Entry<Key, Object>> {

	private Iterator<Key> keys;
	private Objects objs;
	private CFMLEngine engine;

	public ObjectsEntryIterator(Key[] keys, Objects objs) {
		this.engine = CFMLEngineFactory.getInstance();
		this.keys = new KeyIterator(keys);
		this.objs = objs;
	}

	public ObjectsEntryIterator(Iterator<Key> keys, Objects objs) {
		this.engine = CFMLEngineFactory.getInstance();
		this.keys = keys;
		this.objs = objs;
	}

	@Override
	public boolean hasNext() {
		return keys.hasNext();
	}

	@Override
	public Entry<Key, Object> next() {
		Key key = engine.getCastUtil().toKey(keys.next(), null);
		return new EntryImpl(objs, key);
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("this operation is not suppored");
	}

	public class EntryImpl implements Entry<Key, Object> {

		protected Key key;
		private Objects objcts;

		public EntryImpl(Objects objcts, Key key) {
			this.key = key;
			this.objcts = objcts;
		}

		@Override
		public Key getKey() {
			return key;
		}

		@Override
		public Object getValue() {
			return objcts.get(engine.getThreadPageContext(), key, null);
		}

		@Override
		public Object setValue(Object value) {
			return objcts.setEL(engine.getThreadPageContext(), key, value);
		}

	}
}