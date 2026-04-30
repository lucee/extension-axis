/**
 *
 * Copyright (c) 2014, the Railo Company Ltd. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.  If not, see <http://www.gnu.org/licenses/>.
 *
 **/
package org.lucee.extension.axis.server;

import java.util.concurrent.ConcurrentHashMap;

import org.apache.axis.AxisFault;
import org.apache.axis.MessageContext;
import org.apache.axis.constants.Scope;
import org.apache.axis.handlers.BasicHandler;
import org.apache.axis.handlers.soap.SOAPService;
import org.apache.axis.providers.java.JavaProvider;
import org.apache.axis.providers.java.RPCProvider;
import org.lucee.extension.axis.util.RefBooleanImpl;

import lucee.commons.lang.types.RefBoolean;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.runtime.Component;

/**
 * Handle Component as Webservice.
 *
 * Performance fix: caches the SOAPService and proxy class to avoid expensive
 * getJavaAccessClass() reflection on every request. On cache hit, the proxy
 * class is registered in the current AxisEngine's ClassCache so Axis can
 * resolve it without the original request-scoped classloader. Per-key locking
 * prevents thundering herd on cold start.
 */
public final class ComponentHandler extends BasicHandler {

	private static final long serialVersionUID = -3000170039354443399L;

	private static final ConcurrentHashMap<String, CachedService> serviceCache = new ConcurrentHashMap<String, CachedService>();

	private static final ConcurrentHashMap<String, Object> lockMap = new ConcurrentHashMap<String, Object>();

	@Override
	public void invoke(MessageContext msgContext) throws AxisFault {
		try {
			setupService(msgContext);
		}
		catch (Exception e) {
			throw AxisFault.makeFault(CFMLEngineFactory.getInstance().getCastUtil().toPageException(e));
		}
	}

	@Override
	public void generateWSDL(MessageContext msgContext) throws AxisFault {
		try {
			setupService(msgContext);
		}
		catch (Exception e) {
			throw AxisFault.makeFault(CFMLEngineFactory.getInstance().getCastUtil().toPageException(e));
		}
	}

	/**
	 * Handle all the work necessary to set up the "proxy" RPC service surrounding
	 * the component as the MessageContext's active service.
	 *
	 * On cache hit, skips the expensive getJavaAccessClass() entirely. The proxy
	 * class is registered in the current AxisEngine's ClassCache so that Axis's
	 * RPCProvider can find it without depending on the original classloader.
	 */
	protected void setupService(MessageContext msgContext) throws Exception {
		Component cfc = (Component) msgContext.getProperty(Constants.COMPONENT);
		String cacheKey = cfc.getPageSource().getDisplayPath();

		// 1. Check cache FIRST -- skip all reflection on cache hit
		CachedService cached = serviceCache.get(cacheKey);
		if (cached != null) {
			msgContext.getAxisEngine().getClassCache().registerClass(cached.className, cached.clazz);
			msgContext.setClassLoader(cached.classLoader);
			cached.rpc.setEngine(msgContext.getAxisEngine());
			msgContext.setService(cached.rpc);
			return;
		}

		// 2. Cache miss -- synchronize per component to prevent thundering herd
		synchronized (getLock(cacheKey)) {
			// Double-check after acquiring lock
			cached = serviceCache.get(cacheKey);
			if (cached != null) {
				msgContext.getAxisEngine().getClassCache().registerClass(cached.className, cached.clazz);
				msgContext.setClassLoader(cached.classLoader);
				cached.rpc.setEngine(msgContext.getAxisEngine());
				msgContext.setService(cached.rpc);
				return;
			}

			// Do the expensive reflection (only one thread per component pays this cost)
			RefBoolean isnew = new RefBooleanImpl(false);
			Class clazz = cfc.getJavaAccessClass(
					CFMLEngineFactory.getInstance().getThreadPageContext(),
					isnew, false, true, true, true);
			String clazzName = clazz.getName();
			ClassLoader classLoader = clazz.getClassLoader();

			// Set classloader BEFORE getInitializedServiceDesc - it uses
			// msgContext.getClassLoader() to resolve the proxy class
			msgContext.setClassLoader(classLoader);

			SOAPService rpc = new SOAPService(new RPCProvider());
			rpc.setName(clazzName);
			rpc.setOption(JavaProvider.OPTION_CLASSNAME, clazzName);
			rpc.setEngine(msgContext.getAxisEngine());
			rpc.setOption(JavaProvider.OPTION_ALLOWEDMETHODS, "*");
			rpc.setOption(JavaProvider.OPTION_SCOPE, Scope.REQUEST.getName());
			rpc.getInitializedServiceDesc(msgContext);

			serviceCache.put(cacheKey, new CachedService(classLoader, rpc, clazz, clazzName));

			msgContext.setService(rpc);
		}
	}

	private static Object getLock(String key) {
		return lockMap.computeIfAbsent(key, k -> new Object());
	}

	static class CachedService {
		final ClassLoader classLoader;
		final SOAPService rpc;
		final Class clazz;
		final String className;

		CachedService(ClassLoader classLoader, SOAPService rpc, Class clazz, String className) {
			this.classLoader = classLoader;
			this.rpc = rpc;
			this.clazz = clazz;
			this.className = className;
		}
	}
}
