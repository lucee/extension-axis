/**
 * Copyright (c) 2014, the Railo Company Ltd.
 * Copyright (c) 2015, Lucee Assosication Switzerland
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
 */
package org.lucee.extension.axis.client;

import java.io.StringReader;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;
import java.util.Vector;

import javax.wsdl.Binding;
import javax.wsdl.Operation;
import javax.wsdl.Port;
import javax.xml.namespace.QName;
import javax.xml.rpc.ServiceException;
import javax.xml.rpc.encoding.TypeMapping;
import javax.xml.soap.SOAPHeaderElement;

import org.apache.axis.AxisFault;
import org.apache.axis.EngineConfiguration;
import org.apache.axis.MessageContext;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.configuration.EngineConfigurationFactoryFinder;
import org.apache.axis.configuration.SimpleProvider;
import org.apache.axis.message.SOAPEnvelope;
import org.apache.axis.transport.http.CommonsHTTPSender;
import org.apache.axis.wsdl.gen.Parser;
import org.apache.axis.wsdl.symbolTable.BindingEntry;
import org.apache.axis.wsdl.symbolTable.DefinedType;
import org.apache.axis.wsdl.symbolTable.ElementDecl;
import org.apache.axis.wsdl.symbolTable.Parameter;
import org.apache.axis.wsdl.symbolTable.Parameters;
import org.apache.axis.wsdl.symbolTable.ServiceEntry;
import org.apache.axis.wsdl.symbolTable.SymTabEntry;
import org.apache.axis.wsdl.symbolTable.SymbolTable;
import org.apache.axis.wsdl.symbolTable.TypeEntry;
import org.apache.axis.wsdl.toJava.Utils;
import org.lucee.extension.axis.Axis1Caster;
import org.lucee.extension.axis.Axis1Handler;
import org.lucee.extension.axis.TypeMappingUtil;
import org.lucee.extension.axis.WSClient;
import org.lucee.extension.axis.WSHandler;
import org.lucee.extension.axis.cache.WebserviceCacheItem;
import org.lucee.extension.axis.util.ClassUtil;
import org.lucee.extension.axis.util.SimpleDumpData;
import org.lucee.extension.axis.util.StringUtil;
import org.lucee.extension.axis.util.WSUtil;
import org.lucee.extension.axis.util.XMLUtil;
import org.lucee.extension.axis.util.it.KeyAsStringIterator;
import org.lucee.extension.axis.util.it.KeyIterator;
import org.lucee.extension.axis.util.it.ObjectsEntryIterator;
import org.lucee.extension.axis.util.it.ObjectsIterator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;

import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.loader.util.Util;
import lucee.runtime.PageContext;
import lucee.runtime.cache.tag.CacheHandler;
import lucee.runtime.cache.tag.CacheItem;
import lucee.runtime.config.Config;
import lucee.runtime.dump.DumpData;
import lucee.runtime.dump.DumpProperties;
import lucee.runtime.dump.DumpTable;
import lucee.runtime.exp.PageException;
import lucee.runtime.listener.ApplicationContext;
import lucee.runtime.net.proxy.ProxyData;
import lucee.runtime.type.Collection;
import lucee.runtime.type.Collection.Key;
import lucee.runtime.type.Struct;
import lucee.runtime.type.dt.DateTime;
import lucee.runtime.util.Cast;
import lucee.runtime.util.Creation;
import lucee.runtime.util.Excepton;

/**
 * Wrapper for a Webservice
 */
public final class Axis1Client implements WSClient {

	private static final long serialVersionUID = 1L;
	private static final Object[] OBJECT_EMPTY = new Object[0];

	private Parser parser = new Parser();
	// private Map properties=new HashTable();
	private String wsdlUrl;
	private ProxyData proxyData;
	private String username;
	private String password;
	private Call last;
	private List<SOAPHeaderElement> headers;
	private boolean wsdlExecuted;
	private Axis1Handler handler;
	private static CFMLEngine _engine;
	private static Cast caster;
	private static Creation creator;
	private static Excepton exception;

	static {
		EngineConfiguration engine = EngineConfigurationFactoryFinder.newFactory().getClientEngineConfig();
		SimpleProvider provider = new SimpleProvider(engine);
		provider.deployTransport("http", new CommonsHTTPSender());

		_engine = CFMLEngineFactory.getInstance();
		caster = _engine.getCastUtil();
		creator = _engine.getCreationUtil();
		exception = _engine.getExceptionUtil();

	}

	/**
	 * @param wsdlUrl
	 * @param username
	 * @param password
	 * @throws PageException
	 */
	public Axis1Client(Axis1Handler handler, String wsdlUrl, String username, String password) throws PageException {
		this(handler, wsdlUrl, username, password, null);
	}

	public Axis1Client(Axis1Handler handler, String wsdlUrl, String username, String password, ProxyData proxyData) {
		this.handler = handler;
		if (!Util.isEmpty(username)) {
			if (password == null) password = "";
			parser.setUsername(username);
			parser.setPassword(password);
			// parser.setTimeout(1000);
			this.username = username;
			this.password = password;

		}
		this.proxyData = proxyData;
		this.wsdlUrl = wsdlUrl;

	}

	public Object callWithNamedValues(PageContext pc, String methodName, Struct arguments) throws PageException {
		try {
			if (hasCachedWithin(pc)) return _callCachedWithin(pc, pc.getConfig(), methodName, arguments, null);
			return _call(pc, pc.getConfig(), methodName, arguments, null);
		}
		catch (Exception e) {
			throw caster.toPageException(e);
		}
	}

	@Override
	public Object callWithNamedValues(Config config, Collection.Key methodName, Struct arguments) throws PageException {
		try {
			return (_call(null, config, methodName.getString(), arguments, null));
		}
		catch (Exception e) {
			throw caster.toPageException(e);
		}
	}

	public Object call(PageContext pc, String methodName, Object[] arguments) throws PageException {
		try {
			if (hasCachedWithin(pc)) return _callCachedWithin(pc, null, methodName, null, arguments);
			return _call(pc, pc.getConfig(), methodName, null, arguments);
		}
		catch (Exception e) {
			throw caster.toPageException(e);
		}
	}

	@Override
	public Object callWithNamedValues(PageContext pc, Collection.Key methodName, Struct args) throws PageException {
		return callWithNamedValues(pc, methodName.getString(), args);
	}

	@Override
	public Object call(PageContext pc, Collection.Key methodName, Object[] arguments) throws PageException {
		return call(pc, methodName.getString(), arguments);
	}

	private boolean hasCachedWithin(PageContext pc) {
		return pc.getCachedWithin(Config.CACHEDWITHIN_WEBSERVICE) != null;
	}

	private Object getCachedWithin(PageContext pc) {
		// if(this.properties.cachedWithin!=null) return this.properties.cachedWithin;
		return pc.getCachedWithin(Config.CACHEDWITHIN_WEBSERVICE);
	}

	private Object _callCachedWithin(PageContext pc, Config secondChanceConfig, String methodName, Struct namedArguments, Object[] arguments)
			throws PageException, RemoteException, ServiceException {

		// no pc no cache!
		if (pc == null) return _call(pc, secondChanceConfig, methodName, namedArguments, arguments);

		Object cachedWithin = getCachedWithin(pc);
		String cacheId = ClassUtil.createId(wsdlUrl, username, password, proxyData, methodName, arguments, namedArguments);
		CacheHandler cacheHandler = pc.getConfig().getCacheHandlerCollection(Config.CACHE_TYPE_WEBSERVICE, null).getInstanceMatchingObject(cachedWithin, null);
		CacheItem cacheItem = get(cacheHandler, pc, cacheId, cachedWithin);
		if (cacheItem == null) cacheItem = cacheHandler.get(pc, cacheId);

		if (cacheItem instanceof WebserviceCacheItem) {
			WebserviceCacheItem entry = (WebserviceCacheItem) cacheItem;
			return entry.getData();
		}

		// cached item not found, process and cache result if needed
		long start = System.nanoTime();
		Object rtn = _call(pc, secondChanceConfig, methodName, namedArguments, arguments);

		if (cacheHandler != null) cacheHandler.set(pc, cacheId, cachedWithin, new WebserviceCacheItem(rtn, wsdlUrl, methodName, System.nanoTime() - start));

		return rtn;
	}

	private CacheItem get(CacheHandler cacheHandler, PageContext pc, String cacheId, Object cachePolicy) {
		if (cacheHandler == null) return null;
		try {
			Method m = cacheHandler.getClass().getMethod("get", new Class[] { PageContext.class, String.class, Object.class });
			Object o = m.invoke(cacheHandler, new Object[] { pc, cacheId, cachePolicy });
			if (o instanceof CacheItem) return (CacheItem) o;
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	// public CacheItem get(PageContext pc, String cacheId, Object cachePolicy) throws PageException;

	private Object _call(PageContext pc, Config secondChanceConfig, String methodName, Struct namedArguments, Object[] arguments)
			throws PageException, ServiceException, RemoteException {

		ApplicationContext acs = pc.getApplicationContext();
		javax.wsdl.Service service = getWSDLService();

		Service axisService = new Service(parser, service.getQName());
		axisService.setMaintainSession(ClassUtil.getWSMaintainSession(acs));

		TypeMappingUtil.registerDefaults(axisService.getTypeMappingRegistry());
		Port port = WSUtil.getSoapPort(service);

		Binding binding = port.getBinding();

		SymbolTable symbolTable = parser.getSymbolTable();
		BindingEntry bEntry = symbolTable.getBindingEntry(binding.getQName());

		// get matching operation/method
		Iterator<Entry<Operation, Parameters>> itr = bEntry.getParameters().entrySet().iterator();
		Operation operation = null;
		Entry<Operation, Parameters> e;
		Parameters parameters = null;
		while (itr.hasNext()) {
			e = itr.next();
			if (e.getKey().getName().equalsIgnoreCase(methodName)) {
				operation = e.getKey();
				parameters = e.getValue();
				break;
			}
		}

		// no operation found!
		if (operation == null || parameters == null) {
			// get array of existing methods
			Set<Operation> set = bEntry.getParameters().keySet();
			Iterator<Operation> it = set.iterator();
			Collection.Key[] keys = new Collection.Key[set.size()];
			int index = 0;
			while (it.hasNext()) {
				keys[index++] = creator.createKey(it.next().getName());
			}

			throw _engine.getExceptionUtil().createExpressionException(
					_engine.getExceptionUtil().similarKeyMessage(keys, methodName, "method/operation", "methods/operations", null, true) + " Webservice: " + wsdlUrl);
		}
		org.apache.axis.client.Call call = (Call) axisService.createCall(QName.valueOf(port.getName()), QName.valueOf(operation.getName()));
		if (!Util.isEmpty(username, true)) {
			call.setUsername(username);
			call.setPassword(password);
		}

		org.apache.axis.encoding.TypeMapping tm = call.getTypeMapping();
		Vector<String> inNames = new Vector<String>();
		Vector<Parameter> inTypes = new Vector<Parameter>();
		Vector<String> outNames = new Vector<String>();
		Vector<Parameter> outTypes = new Vector<Parameter>();
		Parameter p = null;
		for (int j = 0; j < parameters.list.size(); j++) {
			p = (Parameter) parameters.list.get(j);

			map(pc, symbolTable, secondChanceConfig, tm, p.getType());
			switch (p.getMode()) {
			case Parameter.IN:
				inNames.add(p.getQName().getLocalPart());
				inTypes.add(p);
				break;
			case Parameter.OUT:
				outNames.add(p.getQName().getLocalPart());
				outTypes.add(p);
				break;
			case Parameter.INOUT:
				inNames.add(p.getQName().getLocalPart());
				inTypes.add(p);
				outNames.add(p.getQName().getLocalPart());
				outTypes.add(p);
				break;
			}
		}

		// set output type
		if (parameters.returnParam != null) {
			QName rtnQName = parameters.returnParam.getQName();
			// TypeEntry rtnType = parameters.returnParam.getType();

			map(pc, symbolTable, secondChanceConfig, tm, parameters.returnParam.getType());
			outNames.add(rtnQName.getLocalPart());
			outTypes.add(parameters.returnParam);

		}

		// get timezone
		TimeZone tz = _engine.getThreadTimeZone();

		// check arguments
		Object[] inputs = new Object[inNames.size()];

		if (arguments != null) {
			if (inNames.size() != arguments.length)
				throw exception.createExpressionException("Invalid arguments count for operation " + methodName + " (" + arguments.length + " instead of " + inNames.size() + ")");

			for (int pos = 0; pos < inNames.size(); pos++) {
				p = inTypes.get(pos);
				inputs[pos] = getArgumentData(tm, tz, p, arguments[pos]);
			}
		}
		else {
			ClassUtil.argumentCollection(namedArguments);
			if (inNames.size() != namedArguments.size()) throw exception
					.createExpressionException("Invalid arguments count for operation " + methodName + " (" + namedArguments.size() + " instead of " + inNames.size() + ")");

			Object arg;
			for (int pos = 0; pos < inNames.size(); pos++) {
				p = inTypes.get(pos);
				arg = namedArguments.get(creator.createKey(p.getName()), null);

				if (arg == null) {
					throw exception.createExpressionException("Invalid arguments for operation " + methodName,
							getErrorDetailForArguments(inNames.toArray(new String[inNames.size()]), StringUtil.keysAsString(namedArguments)));
				}
				inputs[pos] = getArgumentData(tm, tz, p, arg);
			}
		}
		Object ret = null;

		// add header
		if (headers != null && !headers.isEmpty()) {
			Iterator<SOAPHeaderElement> it = headers.iterator();
			while (it.hasNext()) {
				call.addHeader((org.apache.axis.message.SOAPHeaderElement) it.next());
			}
		}

		try {
			ret = invoke(call, inputs);
		}
		catch (AxisFault af) {
			boolean rethrow = true;
			Throwable cause = af.getCause();
			if (cause != null) {
				/*
				 * // first check if that missing type is around String[] notFound=new
				 * String[]{"could not find deserializer for type","No deserializer for"}; int index;
				 * if(msg!=null)for(int i=0; i<notFound.length;i++) {
				 * if((index=msg.indexOf(notFound[i]))==-1)continue;;
				 * 
				 * String raw=msg.substring(index+notFound[i].length()+1).trim(); QName qn = QName.valueOf(raw);
				 * print.e(qn.getLocalPart()); print.e(qn.getNamespaceURI()); Type type = symbolTable.getType(qn);
				 * if(type!=null) { map(pc,secondChanceConfig,call.getTypeMapping(),type); ret =
				 * invoke(call,inputs); rethrow=false; } }
				 */

				// get the missing types from the SOAP Body, if possible
				String msg = cause.getMessage();
				// if(StringUtil.indexOfIgnoreCase(msg, "deserializer")!=-1) {
				try {
					InputSource is = new InputSource(new StringReader(call.getResponseMessage().getSOAPPartAsString()));
					Document doc = XMLUtil.parse(is, null, false);
					// Element body = XMLUtility.getChildWithName("soapenv:Body", doc.getDocumentElement());
					Element body = XMLUtil.getChildWithName("soapenv:Body", doc.getDocumentElement());

					Vector types = SOAPUtil.getTypes(body, symbolTable);
					map(pc, symbolTable, secondChanceConfig, (org.apache.axis.encoding.TypeMapping) (axisService.getTypeMappingRegistry().getDefaultTypeMapping()), types);
					ret = invoke(call, inputs);
					rethrow = false;

				}
				catch (Exception ee) {}
				// }
			}
			if (rethrow) throw af;
		}

		last = call;
		if (outNames.size() <= 1) return Axis1Caster.toLuceeType(null, ret);
		// getParamData((org.apache.axis.client.Call)call,parameters.returnParam,ret);
		Map outputs = call.getOutputParams();

		Struct sct = creator.createStruct();
		for (int pos = 0; pos < outNames.size(); pos++) {
			String name = outNames.get(pos);
			// print.ln(name);
			Object value = outputs.get(name);
			if (value == null && pos == 0) {
				sct.setEL(name, Axis1Caster.toLuceeType(null, ret));
			}
			else {
				sct.setEL(name, Axis1Caster.toLuceeType(null, value));
			}
		}
		return sct;
	}

	private Object invoke(Call call, Object[] inputs) throws RemoteException {
		if (proxyData != null && !Util.isEmpty(proxyData.getServer(), true)) {
			try {
				// Proxy.start(proxyData); TODO FUTURE if a proxy impl exist enable this
				return call.invoke(inputs);

			}
			finally {
				// Proxy.end();
			}
		}
		return call.invoke(inputs);
	}

	private void map(PageContext pc, SymbolTable symbolTable, Config secondChanceConfig, org.apache.axis.encoding.TypeMapping tm, Vector types) throws PageException {
		Iterator it = types.iterator();
		while (it.hasNext()) {
			map(pc, symbolTable, secondChanceConfig, tm, (TypeEntry) it.next());
		}
	}

	private Class map(PageContext pc, SymbolTable symbolTable, Config secondChanceConfig, org.apache.axis.encoding.TypeMapping tm, TypeEntry type) throws PageException {
		// print.e("MAP");
		// print.e(type.getQName());

		TypeEntry ref = type.getRefType();
		if (ref != null && ref != type) {
			map(pc, symbolTable, secondChanceConfig, tm, ref);
		}

		// Simple Type
		if (type.getContainedElements() == null) return null;

		// is class already registered!
		// Class clazz=tm.getClassForQName(type.getQName());
		// if(clazz!=null && clazz.getName().equals(getClientClassName(type))) return clazz;

		Class clazz = mapComplex(pc, symbolTable, secondChanceConfig, tm, type);

		// TODO make a better impl; this is not the fastest way to make sure all pojos use the same
		// classloader
		if (clazz != null && getClassLoader(pc, secondChanceConfig) != clazz.getClassLoader()) {
			clazz = mapComplex(pc, symbolTable, secondChanceConfig, tm, type);
		}
		return clazz;

	}

	private Class mapComplex(PageContext pc, SymbolTable symbolTable, Config secondChanceConfig, org.apache.axis.encoding.TypeMapping tm, TypeEntry type) throws PageException {
		TypeEntry ref = type.getRefType();
		if (ref == null) return _mapComplex(pc, symbolTable, secondChanceConfig, tm, type);

		// Array
		if (ref.getContainedElements() == null) return null;
		Class clazz = mapComplex(pc, symbolTable, secondChanceConfig, tm, ref);
		if (clazz == null) return null;

		Class arr = _engine.getClassUtil().toArrayClass(clazz);
		TypeMappingUtil.registerBeanTypeMapping(tm, arr, type.getQName());
		return arr;
	}

	private Class _mapComplex(PageContext pc, SymbolTable symbolTable, Config secondChanceConfig, org.apache.axis.encoding.TypeMapping tm, TypeEntry type) throws PageException {

		// extension
		Class ex = null;
		if (type instanceof DefinedType) {
			DefinedType dt = (DefinedType) type;
			TypeEntry exType = dt.getComplexTypeExtensionBase(symbolTable);
			if (exType != null) ex = map(pc, symbolTable, secondChanceConfig, tm, exType);
		}

		Vector children = type.getContainedElements();
		List properties = new ArrayList();
		if (children != null) {
			Iterator it = children.iterator();
			ElementDecl el;
			Class clazz;
			TypeEntry t;
			String name;
			while (it.hasNext()) {
				clazz = null;
				el = (ElementDecl) it.next();
				t = el.getType();
				Vector els = t.getContainedElements();

				// again handle children
				if (els != null) {
					clazz = mapComplex(pc, symbolTable, secondChanceConfig, tm, t);
				}
				name = _engine.getListUtil().last(el.getQName().getLocalPart(), ">", true);

				if (clazz == null) clazz = tm.getClassForQName(t.getQName());
				if (clazz == null) clazz = Object.class;

				properties.add(ClassUtil.createASMProperty(clazz, name));
			}
		}
		Object[] props = ClassUtil.toASMPropertyArray(properties);
		String clientClassName = getClientClassName(type, props);
		Class pojo;
		if (pc == null) pojo = ClassUtil.getComponentPropertiesClass(secondChanceConfig, clientClassName, props, ex);
		else pojo = ClassUtil.getClientComponentPropertiesClass(pc, clientClassName, props, ex);

		TypeMappingUtil.registerBeanTypeMapping(tm, pojo, type.getQName());

		return pojo;
	}

	private ClassLoader getClassLoader(PageContext pc, Config secondChanceConfig) {
		ClassLoader cl = null;
		try {
			if (pc == null) cl = secondChanceConfig.getRPCClassLoader(false);
			else cl = ClassUtil.getRPCClassLoader(pc, false);
		}
		catch (Exception e) {
			e.printStackTrace();
			/* MUST report the exception or throw it */}
		return cl;
	}

	private String getClientClassName(TypeEntry type, Object[] asmProperties) {
		// properties
		StringBuilder sb = new StringBuilder();
		if (asmProperties != null) for (int i = 0; i < asmProperties.length; i++) {
			sb.append(asmProperties[i].toString()).append(';');
		}

		String className = _engine.getStringUtil().toVariableName(type.getQName().getLocalPart(), true, true);
		String url = urlToClass(wsdlUrl);
		String ns = type.getQName().getNamespaceURI();
		String prefix = Long.toString(StringUtil.create64BitHash(sb.append(url).append(':').append(ns)), Character.MAX_RADIX);
		char c = prefix.charAt(0);
		if (c >= '0' && c <= '9') prefix = "a" + prefix;
		return prefix + "." + className;
	}

	private static String urlToClass(String wsdlUrl) {

		StringBuffer sb = new StringBuffer();
		try {
			URL url = new URL(wsdlUrl);

			// protocol
			if ("http".equalsIgnoreCase(url.getProtocol())) {}
			else {
				sb.append(toClassName(url.getProtocol(), false));
				sb.append('.');
			}

			// host
			sb.append(toClassName(url.getHost(), true));

			// port
			if (url.getPort() > 0 && url.getPort() != 80) {
				sb.append(".p");
				sb.append(url.getPort());
			}

			// path
			if (!Util.isEmpty(url.getPath())) {
				sb.append('.');
				sb.append(toClassName(url.getPath(), false));
			}

			// query
			if (!Util.isEmpty(url.getQuery()) && !"wsdl".equals(url.getQuery())) {
				sb.append('.');
				sb.append(toClassName(url.getQuery(), false));
			}

			return sb.toString();
		}
		catch (MalformedURLException e) {
			return _engine.getStringUtil().toVariableName(wsdlUrl, true, false);
		}
	}

	private static String toClassName(String raw, boolean reverse) {
		raw = raw.trim();
		if (raw.endsWith("/")) raw = raw.substring(0, raw.length() - 1);
		StringBuffer sb = new StringBuffer();
		String[] arr = null;
		try {
			arr = _engine.getListUtil().toStringArray(_engine.getListUtil().toArray(raw, "./&="));
		}
		catch (PageException e) {}
		String el;
		for (int i = 0; i < arr.length; i++) {
			el = arr[i].trim();
			if (el.length() == 0) continue;
			if (reverse) {
				if (sb.length() > 0) sb.insert(0, '.');
				sb.insert(0, lcFirst(_engine.getStringUtil().toVariableName(arr[i], true, false)));

			}
			else {
				if (sb.length() > 0) sb.append('.');
				sb.append(lcFirst(_engine.getStringUtil().toVariableName(arr[i], true, false)));
			}
		}
		return sb.toString();
	}

	private static String lcFirst(String str) {
		if (str == null) return null;
		else if (str.length() <= 1) return str.toLowerCase();
		else {
			return str.substring(0, 1).toLowerCase() + str.substring(1);
		}
	}

	private String getErrorDetailForArguments(String[] names, String[] argKeys) {
		String name;
		boolean found;

		for (int i = 0; i < names.length; i++) {
			name = names[i];
			found = false;
			for (int y = 0; y < argKeys.length; y++) {
				if (name.equalsIgnoreCase(argKeys[y])) found = true;
			}
			if (!found) {
				if (names.length > 1) return "missing argument with name [" + name + "], needed argument are [" + _engine.getListUtil().toList(names, ", ") + "]";
				return "missing argument with name [" + name + "]";
			}
		}
		return "";
	}

	/**
	 * returns the WSDL Service for this Object
	 * 
	 * @return
	 * @return WSDL Service
	 * @throws RPCException
	 * @throws PageException
	 */

	public Port getPort() throws PageException {
		return WSUtil.getSoapPort(getWSDLService());
	}

	public javax.wsdl.Service getWSDLService() throws PageException {
		if (!wsdlExecuted) {
			try {
				parser.run(wsdlUrl);
				wsdlExecuted = true;
			}
			catch (Exception e) {
				throw caster.toPageException(e);
			}
		}

		SymTabEntry symTabEntry = null;
		Map.Entry entry = null;
		Vector v = null;
		Iterator<Map.Entry> it = parser.getSymbolTable().getHashMap().entrySet().iterator();

		while (it.hasNext()) {
			entry = it.next();
			v = (Vector) entry.getValue();
			for (int i = 0; i < v.size(); i++) {
				if (!(org.apache.axis.wsdl.symbolTable.ServiceEntry.class).isInstance(v.elementAt(i))) continue;
				symTabEntry = (SymTabEntry) v.elementAt(i);
				// break;
			}

		}

		if (symTabEntry == null) throw exception.createExpressionException("Can't locate service entry in WSDL");
		return ((ServiceEntry) symTabEntry).getService();
	}

	private Object getArgumentData(TypeMapping tm, TimeZone tz, Parameter p, Object arg) throws PageException {
		// print.e("ArgumentData");

		QName paramType = Utils.getXSIType(p);

		Object res = Axis1Caster.toAxisType(handler, tm, tz, p.getType(), paramType, arg);
		// print.e(res);
		return res;

	}

	@Override
	public Object get(PageContext pc, Collection.Key key) throws PageException {
		return call(pc, "get" + key.getString(), OBJECT_EMPTY);
	}

	@Override
	public Object get(PageContext pc, Collection.Key key, Object defaultValue) {
		try {
			return call(pc, "get" + CFMLEngineFactory.getInstance().getStringUtil().ucFirst(key.getString()), OBJECT_EMPTY);
		}
		catch (PageException e) {
			return defaultValue;
		}
	}

	@Override
	public Object set(PageContext pc, Collection.Key propertyName, Object value) throws PageException {
		return call(pc, "set" + propertyName.getString(), new Object[] { value });
	}

	@Override
	public Object setEL(PageContext pc, Collection.Key propertyName, Object value) {
		try {
			return call(pc, "set" + propertyName.getString(), new Object[] { value });
		}
		catch (PageException e) {
			return null;
		}
	}

	public boolean isInitalized() {
		return true;
	}

	@Override
	public DumpData toDumpData(PageContext pageContext, int maxlevel, DumpProperties dp) {
		try {
			return _toDumpData(pageContext, maxlevel, dp);
		}
		catch (Exception e) {
			DumpData dd;
			try {
				dd = ClassUtil.toDumpData(e, pageContext, maxlevel, dp);
			}
			catch (PageException e1) {
				dd = new SimpleDumpData(e.getMessage());
			}

			DumpTable table = new DumpTable("webservice", "#99cccc", "#ccffff", "#000000");
			table.appendRow(1, new SimpleDumpData("webservice"), new SimpleDumpData(wsdlUrl));
			table.appendRow(1, new SimpleDumpData("error"), dd);

			return table;
		}
	}

	private DumpData _toDumpData(PageContext pageContext, int maxlevel, DumpProperties dp) throws PageException {

		DumpTable functions = new DumpTable("webservice", "#99cccc", "#ccffff", "#000000");
		functions.setTitle("Web Service (Axis 1)");
		if (dp.getMetainfo()) functions.setComment(wsdlUrl);
		// DumpTable functions = new DumpTable("#ccccff","#cccc00","#000000");

		Port port = getPort();
		Binding binding = port.getBinding();

		// Parameters parameters = null;
		// Parameter p = null;
		SymbolTable symbolTable = parser.getSymbolTable();
		BindingEntry bEntry = symbolTable.getBindingEntry(binding.getQName());
		Iterator itr = bEntry.getParameters().keySet().iterator();
		Operation tmpOp = null;
		// Operation operation = null;
		while (itr.hasNext()) {
			tmpOp = (Operation) itr.next();
			Element el = tmpOp.getDocumentationElement();
			StringBuffer doc = new StringBuffer();
			if (el != null) {
				ArrayList<Node> children = XMLUtil.getChildNodes(el, Node.TEXT_NODE, null);
				Iterator<Node> it = children.iterator();
				Node n;
				Text text;
				while (it.hasNext()) {
					n = it.next();
					if (n instanceof Text) {
						text = (Text) n;
						doc.append(text.getData());
					}
				}
			}
			// parameters = (Parameters)bEntry.getParameters().get(tmpOp);
			functions.appendRow(1, new SimpleDumpData(tmpOp.getName()), _toHTMLOperation(tmpOp.getName(), doc.toString(), (Parameters) bEntry.getParameters().get(tmpOp)));
		}

		// box.appendRow(1,new SimpleDumpData(""),functions);
		return functions;
	}

	private DumpData _toHTMLOperation(String title, String doc, Parameters parameters) {
		DumpTable table = new DumpTable("#99cccc", "#ccffff", "#000000");
		table.setTitle(title);
		if (doc.length() > 0) table.setComment(doc);

		DumpTable attributes = new DumpTable("#99cccc", "#ccffff", "#000000");
		String returns = "void";
		attributes.appendRow(3, new SimpleDumpData("name"), new SimpleDumpData("type"));

		for (int j = 0; j < parameters.list.size(); j++) {
			Parameter p = (Parameter) parameters.list.get(j);

			QName paramType = org.apache.axis.wsdl.toJava.Utils.getXSIType(p);
			String strType = paramType.getLocalPart();

			switch (p.getMode()) {
			case Parameter.IN:
				attributes.appendRow(0, new SimpleDumpData(p.getName()), new SimpleDumpData(toLuceeType(strType)));
				break;
			case Parameter.OUT:
				returns = toLuceeType(strType);
				break;
			case Parameter.INOUT:
				attributes.appendRow(0, new SimpleDumpData(p.getName()), new SimpleDumpData(toLuceeType(strType)));
				returns = toLuceeType(strType);

				break;
			}
		}
		Parameter rtn = parameters.returnParam;
		if (rtn != null) {
			QName paramType = org.apache.axis.wsdl.toJava.Utils.getXSIType(rtn);
			String strType = paramType.getLocalPart();
			returns = toLuceeType(strType);
		}
		table.appendRow(1, new SimpleDumpData("arguments"), attributes);
		table.appendRow(1, new SimpleDumpData("return type"), new SimpleDumpData(returns));

		return table;

	}

	private String toLuceeType(String strType) {
		strType = strType.toLowerCase();
		if (strType.startsWith("array")) strType = "array";
		else if (strType.equals("map")) strType = "struct";
		else if (strType.startsWith("query")) strType = "query";
		else if (strType.equals("double")) strType = "numeric";
		else if (strType.startsWith("any")) strType = "any";
		else if (strType.equals("date")) strType = "date";
		return strType;
	}

	@Override
	public String castToString() throws PageException {
		throw exception.createExpressionException("can't cast Webservice to a string");
	}

	@Override
	public String castToString(String defaultValue) {
		return defaultValue;
	}

	@Override
	public boolean castToBooleanValue() throws PageException {
		throw exception.createExpressionException("can't cast Webservice to a boolean");
	}

	@Override
	public Boolean castToBoolean(Boolean defaultValue) {
		return defaultValue;
	}

	@Override
	public double castToDoubleValue() throws PageException {
		throw exception.createExpressionException("can't cast Webservice to a number");
	}

	@Override
	public double castToDoubleValue(double defaultValue) {
		return defaultValue;
	}

	@Override
	public DateTime castToDateTime() throws PageException {
		throw exception.createExpressionException("can't cast Webservice to a Date Object");
	}

	@Override
	public DateTime castToDateTime(DateTime defaultValue) {
		return defaultValue;
	}

	@Override
	public int compareTo(boolean b) throws PageException {
		throw exception.createExpressionException("can't compare Webservice Object with a boolean value");
	}

	@Override
	public int compareTo(DateTime dt) throws PageException {
		throw exception.createExpressionException("can't compare Webservice Object with a DateTime Object");
	}

	@Override
	public int compareTo(double d) throws PageException {
		throw exception.createExpressionException("can't compare Webservice Object with a numeric value");
	}

	@Override
	public int compareTo(String str) throws PageException {
		throw exception.createExpressionException("can't compare Webservice Object with a String");
	}

	@Override
	public Iterator<Collection.Key> keyIterator() {
		List<Collection.Key> list = new ArrayList<Collection.Key>();
		Port port = null;
		try {
			port = getPort();
		}
		catch (Exception e) {
			return new KeyIterator(new Collection.Key[0]);
		}

		Binding binding = port.getBinding();

		SymbolTable symbolTable = parser.getSymbolTable();
		BindingEntry bEntry = symbolTable.getBindingEntry(binding.getQName());
		Iterator itr = bEntry.getParameters().keySet().iterator();
		Operation tmpOp = null;
		// Operation operation = null;
		while (itr.hasNext()) {
			tmpOp = (Operation) itr.next();
			// Parameters p = (Parameters)bEntry.getParameters().get(tmpOp);
			list.add(creator.createKey(tmpOp.getName()));

		}
		return new KeyIterator(list.toArray(new Collection.Key[list.size()]));
	}

	@Override
	public Iterator<String> keysAsStringIterator() {
		return new KeyAsStringIterator(keyIterator());
	}

	@Override
	public Iterator<Object> valueIterator() {
		return new ObjectsIterator(keyIterator(), this);
	}

	@Override
	public Iterator<Entry<Key, Object>> entryIterator() {
		return new ObjectsEntryIterator(keyIterator(), this);
	}

	public Call getLastCall() {
		return last;
	}

	@Override
	public void addHeader(SOAPHeaderElement header) {
		if (headers == null) headers = new ArrayList<SOAPHeaderElement>();
		headers.add(header);
	}

	@Override
	public void addSOAPRequestHeader(String namespace, String name, Object value, boolean mustUnderstand) throws PageException {
		SOAPHeaderElement header = toSOAPHeaderElement(namespace, name, value);
		header.setMustUnderstand(mustUnderstand);
		addHeader(header);
	}

	@Override
	public Node getSOAPRequest() throws PageException {
		try {
			MessageContext context = getMessageContext();
			SOAPEnvelope env = context.getRequestMessage().getSOAPEnvelope();
			return XMLUtil.toXMLStruct(env.getAsDocument(), true);
		}
		catch (Exception e) {
			throw caster.toPageException(e);
		}
	}

	@Override
	public Node getSOAPResponse() throws PageException {
		try {
			Call call = getLastCall();
			if (call == null) throw new AxisFault("web service was not invoked yet");
			SOAPEnvelope env = call.getResponseMessage().getSOAPEnvelope();
			return XMLUtil.toXMLStruct(env.getAsDocument(), true);
		}
		catch (Exception e) {
			throw caster.toPageException(e);
		}
	}

	@Override
	public Object getSOAPResponseHeader(PageContext pc, String namespace, String name, boolean asXML) throws PageException {
		try {
			MessageContext context = getMessageContext();
			SOAPEnvelope env = context.getResponseMessage().getSOAPEnvelope();
			org.apache.axis.message.SOAPHeaderElement header = env.getHeaderByName(namespace, name);
			return toValue(header, asXML);
		}
		catch (Exception e) {
			throw caster.toPageException(e);
		}
	}

	private static SOAPHeaderElement toSOAPHeaderElement(String namespace, String name, Object value) {

		Element el = XMLUtil.toRawElement(value, null);
		if (el != null) return new org.apache.axis.message.SOAPHeaderElement(el);
		return new org.apache.axis.message.SOAPHeaderElement(namespace, name, value);
	}

	private MessageContext getMessageContext() throws AxisFault, PageException {

		Call call = getLastCall();
		if (call == null) throw new AxisFault("web service was not invoked yet");
		return call.getMessageContext();
	}

	private static Object toValue(org.apache.axis.message.SOAPHeaderElement header, boolean asXML) throws Exception {
		if (header == null) return "";
		if (asXML) {
			String strXML = header.toString();
			InputSource is = new InputSource(new StringReader(strXML.trim()));
			return XMLUtil.toXMLStruct(XMLUtil.parse(is, null, false), true);
		}

		Object value = header.getObjectValue();
		if (value == null) {
			value = header.getObjectValue(String.class);
		}
		return value;
	}

	@Override
	public WSHandler getWSHandler() {
		return handler;
	}
}