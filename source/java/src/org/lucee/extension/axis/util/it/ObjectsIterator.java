package org.lucee.extension.axis.util.it;

import java.util.Iterator;

import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.runtime.type.Collection.Key;
import lucee.runtime.type.Objects;

public class ObjectsIterator implements Iterator<Object> {

	private Iterator<Key> keys;
	private Objects objs;
	private CFMLEngine engine;

	public ObjectsIterator(Key[] keys, Objects objs) {
		this.keys = new KeyIterator(keys);
		this.objs = objs;
		this.engine = CFMLEngineFactory.getInstance();
	}

	public ObjectsIterator(Iterator<Key> keys, Objects objs) {
		this.keys = keys;
		this.objs = objs;
	}

	@Override
	public boolean hasNext() {
		return keys.hasNext();
	}

	@Override
	public Object next() {
		return objs.get(engine.getThreadPageContext(), engine.getCastUtil().toKey(keys.next(), null), null);
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("this operation is not suppored");
	}
}