package org.lucee.extension.axis;

import java.io.StringReader;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.servlet.ServletContext;
import javax.xml.namespace.QName;
import javax.xml.rpc.encoding.TypeMapping;

import lucee.commons.io.log.Log;
import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.runtime.Component;
import lucee.runtime.ComponentScope;
import lucee.runtime.PageContext;
import lucee.runtime.component.Property;
import lucee.runtime.exp.PageException;
import lucee.runtime.listener.ApplicationContext;
import lucee.runtime.net.proxy.ProxyData;

import org.lucee.extension.axis.client.Axis1Client;

import lucee.runtime.type.Collection;
import lucee.runtime.type.Struct;
import lucee.runtime.util.Cast;

import org.lucee.extension.axis.server.Axis1Server;
import org.lucee.extension.axis.util.XMLUtil;
import org.apache.axis.AxisFault;
import org.apache.axis.MessageContext;
import org.apache.axis.client.Call;
import org.apache.axis.message.SOAPEnvelope;
import org.apache.axis.message.SOAPHeaderElement;
import org.apache.axis.wsdl.symbolTable.ElementDecl;
import org.apache.axis.wsdl.symbolTable.TypeEntry;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

public class Axis1Handler implements WSHandler {
	
	private static Cast caster;
	private static CFMLEngine engine;

	static {
		engine = CFMLEngineFactory.getInstance();
		caster=engine.getCastUtil();
	}

	@Override
	public boolean isSOAPRequest() {
		MessageContext context = MessageContext.getCurrentContext();
		return context != null && !context.isClient();
	}



	public void addSOAPResponseHeader(String namespace, String name,
			Object value, boolean mustUnderstand) throws PageException {
		try {
			MessageContext context = MessageContext.getCurrentContext();
			if (context == null || context.isClient())
				throw new AxisFault("not inside a Soap Request");

			SOAPEnvelope env = context.getResponseMessage().getSOAPEnvelope();
			SOAPHeaderElement header = toSOAPHeaderElement(namespace, name,
					value);
			header.setMustUnderstand(mustUnderstand);
			env.addHeader(header);
		} catch (AxisFault af) {
			throw caster.toPageException(af);
		}
	}

	public Object getSOAPRequestHeader(PageContext pc, String namespace,
			String name, boolean asXML) throws PageException {
		try {
			MessageContext context = MessageContext.getCurrentContext();
			if (context == null || context.isClient())
				throw new AxisFault("not inside a Soap Request");

			SOAPEnvelope env = context.getRequestMessage().getSOAPEnvelope();
			SOAPHeaderElement header = env.getHeaderByName(namespace, name);
			return toValue(header, asXML);
		} catch (Exception e) {
			throw caster.toPageException(e);
		}
	}

	public TypeEntry getContainedElement(TypeEntry type, String name,
			TypeEntry defaultValue) {
		if (type == null)
			return defaultValue;
		Vector v = type.getContainedElements();
		if (v != null) {
			Iterator it = v.iterator();
			ElementDecl ed;
			String tmp;
			while (it.hasNext()) {
				ed = (ElementDecl) it.next();
				if (ed.getQName() == null)
					continue;
				tmp = engine.getListUtil().last(ed.getQName()
						.getLocalPart(), ">",true);

				if (tmp.equalsIgnoreCase(name))
					return ed.getType();
			}
		}
		return defaultValue;
	}

	private static Object toValue(SOAPHeaderElement header, boolean asXML)
			throws Exception {
		if (header == null)
			return "";
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

	/*private static MessageContext getMessageContext(WSClient client) throws AxisFault, PageException {
		if (client != null) {
			Call call = (Call) ((Axis1Client)client).getLastCall();
			if (call == null)
				throw new AxisFault("web service was not invoked yet");
			return call.getMessageContext();
		}
		MessageContext context = MessageContext.getCurrentContext();
		if (context == null)
			throw new AxisFault("not inside a Soap Request");
		return context;
	}*/

	private static SOAPHeaderElement toSOAPHeaderElement(String namespace,
			String name, Object value) {
		
		Element el = XMLUtil.toRawElement(value, null);
		if (el != null)
			return new SOAPHeaderElement(el);
		return new SOAPHeaderElement(namespace, name, value);
	}

	@Override
	public String getTypeAsString() {
		return "Axis1";
	}

	@Override
	public Class<?> toWSTypeClass(Class<?> clazz) {
		try{
			return Axis1Caster.toAxisTypeClass(clazz);
		}
		catch(Exception e) {
			e.printStackTrace();
			return clazz;
		}
		
	}

	@Override
	public WSServer getWSServer(PageContext pc) throws PageException {
		try {
			return Axis1Server.getInstance(this,pc);
		}
		catch (AxisFault af) {
			throw caster.toPageException(af);
		}
	}

	@Override
	public WSClient getWSClient(String wsdlUrl, String username, String password, ProxyData proxyData) throws PageException {
		/*pc=ThreadLocalPageContext.get(pc);
		if(pc!=null) {
			Log l = pc.getConfig().getLog("application");
			ApplicationContext ac = pc.getApplicationContext();
			if(ac!=null) {
				if(ApplicationContext.WS_TYPE_JAX_WS==ac.getWSType()) {
					l.info("RPC","using JAX WS Client");
					return new JaxWSClient(wsdlUrl, username, password, proxyData);
				}
				if(ApplicationContext.WS_TYPE_CXF==ac.getWSType()) {
					l.info("RPC","using CXF Client");
					return new CXFClient(wsdlUrl, username, password, proxyData);
				}
			}
			l.info("RPC","using Axis 1 RPC Client");
		}*/
		return new org.lucee.extension.axis.client.Axis1Client(this,wsdlUrl,username,password,proxyData);
	}
}
