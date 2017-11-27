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

import javax.xml.rpc.encoding.TypeMapping;

import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.runtime.Component;
import lucee.runtime.PageContext;
import lucee.runtime.exp.PageException;

import org.lucee.extension.axis.Axis1Caster;
import org.lucee.extension.axis.Axis1Handler;
import org.lucee.extension.axis.TypeMappingUtil;
import org.lucee.extension.axis.WSHandler;
import org.lucee.extension.axis.WSServer;
import org.lucee.extension.axis.util.ClassUtil;

import lucee.runtime.type.Collection.Key;
import lucee.runtime.type.FunctionArgument;
import lucee.runtime.type.UDF;
import lucee.runtime.util.Cast;
import lucee.runtime.util.Excepton;

import org.apache.axis.AxisFault;
import org.apache.axis.MessageContext;

 final class ComponentController {

	private static ThreadLocal<Component> component = new ThreadLocal<Component>();
	private static ThreadLocal<PageContext> pagecontext = new ThreadLocal<PageContext>();
	private static ThreadLocal<MessageContext> messageContext = new ThreadLocal<MessageContext>();
	private static Cast caster;
	private static Excepton exp;
	
	static {
		CFMLEngine engine = CFMLEngineFactory.getInstance();
		caster=engine.getCastUtil();
		exp = engine.getExceptionUtil();
	}

	/**
	 * invokes thread local component
	 * 
	 * @param name
	 * @param args
	 * @return
	 * @throws AxisFault
	 * @throws PageException
	 */
	public static Object invoke(Axis1Server server, String name, Object[] args) throws AxisFault {
		try {
			return _invoke(server,name, args);
		} catch (Exception e) {
			throw AxisFault.makeFault((caster.toPageException(e)));
		}
	}

	public static Object _invoke(Axis1Server server, String name, Object[] args)
			throws PageException {
		Key key = caster.toKey(name);
		Component c = component.get();
		PageContext p = pagecontext.get();
		MessageContext mc = messageContext.get();
		if (c == null)
			throw exp.createApplicationException("missing component");
		if (p == null)
			throw exp.createApplicationException("missing pagecontext");
		Object o=c.get(p, key, null);
		UDF udf = o instanceof UDF?(UDF)o:null;
		FunctionArgument[] fa = null;
		if (udf != null)
			fa = udf.getFunctionArguments();

		for (int i = 0; i < args.length; i++) {
			if (fa != null && i < fa.length
					&& fa[i].getType() == -1/*CFTypes.TYPE_UNKNOW*/) {
				args[i] = Axis1Caster.toLuceeType(p, fa[i].getTypeAsString(),
						args[i]);
			} else
				args[i] = Axis1Caster.toLuceeType(p, args[i]);
		}

		// return type
		String rtnType = udf != null ? udf.getReturnTypeAsString() : "any";

		Object rtn = c.call(p, key, args);

		// cast return value to Axis type
		try {
			//WSServer server = WSHandler.getInstance().getWSServer(p);
			TypeMapping tm = mc != null ? mc.getTypeMapping() : TypeMappingUtil
					.getServerTypeMapping(((Axis1Server)server).getEngine()
							.getTypeMappingRegistry());
			rtn = caster.castTo(p, rtnType, rtn, false);
			Class<?> clazz = ClassUtil.cfTypeToClass(rtnType);
			return Axis1Caster.toAxisType((Axis1Handler) server.getWSHandler(),tm, rtn,
					clazz.getComponentType() != null ? clazz : null);
		} catch (Exception e) {
			throw caster.toPageException(e);
		}
	}

	/**
	 * removes PageContext and Component sets component and pageContext to
	 * invoke
	 * 
	 * @param p
	 * @param c
	 */
	public static void set(PageContext p, Component c) {
		pagecontext.set(p);
		component.set(c);
	}

	public static void set(MessageContext mc) {
		messageContext.set(mc);
	}

	/**
	 * 
	 */
	public static void release() {
		pagecontext.set(null);
		component.set(null);
		messageContext.set(null);
	}
}