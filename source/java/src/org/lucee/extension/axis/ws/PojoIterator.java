package org.lucee.extension.axis.ws;

import java.lang.reflect.Method;
import java.util.Iterator;

import org.lucee.extension.axis.util.Pair;
import org.lucee.extension.axis.util.Reflector;

import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.runtime.type.Collection;

public class PojoIterator implements Iterator<Pair<Collection.Key,Object>> {
	
	private static final Object[] EMPTY_ARG = new Object[]{}; 
	
	private Object pojo;
	private Method[] getters;
	private Class<? extends Object> clazz;
	private int index=-1;

	public PojoIterator(Object pojo) {
		this.pojo=pojo;
		this.clazz=pojo.getClass();
		getters = Reflector.getGetters(pojo.getClass());
	}
	
	public int size() {
		return getters.length;
	}

	@Override
	public boolean hasNext() {
		return (index+1)<getters.length;
	}

	@Override
	public Pair<Collection.Key, Object> next() {
		Method g = getters[++index];
		CFMLEngine en = CFMLEngineFactory.getInstance();
		try {
			
			return new Pair<Collection.Key, Object>(en.getCreationUtil().createKey(g.getName().substring(3)), g.invoke(pojo, EMPTY_ARG));
		}
		catch(Exception e) {
			throw en.getExceptionUtil().createPageRuntimeException(en.getCastUtil().toPageException(e));
		}
	}

	@Override
	public void remove() {
		throw new RuntimeException("method remove is not supported!");
	}

}