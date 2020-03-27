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

import java.util.Map;
import java.util.WeakHashMap;

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
import lucee.runtime.config.Config;

/**
 * Handle Component as Webservice
 */
public final class ComponentHandler extends BasicHandler {

	private static final long serialVersionUID = -3000170039354443399L;

	private static Map<String, Pair> soapServices = new WeakHashMap<String, Pair>();

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
	 * handle all the work necessary set up the "proxy" RPC service surrounding it as the
	 * MessageContext's active service.
	 *
	 */
	protected void setupService(MessageContext msgContext) throws Exception {
		RefBoolean isnew = new RefBooleanImpl(false);
		Component cfc = (Component) msgContext.getProperty(Constants.COMPONENT);
		Config c = CFMLEngineFactory.getInstance().getThreadConfig();
		// ((ConfigImpl) ThreadLocalPageContext.getConfig(pc)).getWSHandler().getWSServer(pc).doGet(pc,
		// pc.getHttpServletRequest(), pc.getHttpServletResponse(), component);

		Class clazz = cfc.getJavaAccessClass(CFMLEngineFactory.getInstance().getThreadPageContext(), isnew, false, true, true, true);
		String clazzName = clazz.getName();

		ClassLoader classLoader = clazz.getClassLoader();
		Pair pair;
		SOAPService rpc = null;
		if (!isnew.toBooleanValue() && (pair = soapServices.get(clazzName)) != null) {
			if (classLoader == pair.classloader) rpc = pair.rpc;
		}
		// else classLoader = clazz.getClassLoader();

		// print.out("cl:"+classLoader);
		msgContext.setClassLoader(classLoader);

		if (rpc == null) {
			rpc = new SOAPService(new RPCProvider());
			rpc.setName(clazzName);
			rpc.setOption(JavaProvider.OPTION_CLASSNAME, clazzName);
			rpc.setEngine(msgContext.getAxisEngine());

			rpc.setOption(JavaProvider.OPTION_ALLOWEDMETHODS, "*");
			rpc.setOption(JavaProvider.OPTION_SCOPE, Scope.REQUEST.getName());
			rpc.getInitializedServiceDesc(msgContext);
			soapServices.put(clazzName, new Pair(classLoader, rpc));
		}

		rpc.setEngine(msgContext.getAxisEngine());
		rpc.init(); // ??
		msgContext.setService(rpc);

	}

	class Pair {
		private ClassLoader classloader;
		private SOAPService rpc;

		public Pair(ClassLoader classloader, SOAPService rpc) {
			this.classloader = classloader;
			this.rpc = rpc;
		}
	}
}