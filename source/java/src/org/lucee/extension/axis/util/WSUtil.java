package org.lucee.extension.axis.util;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.wsdl.Message;
import javax.wsdl.Port;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.xml.namespace.QName;

import lucee.loader.engine.CFMLEngineFactory;
import lucee.runtime.exp.PageException;

public class WSUtil {
	public static Port getSoapPort(javax.wsdl.Service service) throws PageException {
		String name = null;
		Port port = null;
		List list = null;
		Map ports = service.getPorts();
		Iterator it;
		Iterator<Port> itr = ports.values().iterator();
		Object v;
		while(itr.hasNext()) {
			port = itr.next();
			
			list=port.getExtensibilityElements();
			if(list != null) {
				it = list.iterator();
				while(it.hasNext()) {
					v=it.next();
					if(v instanceof SOAPAddress) {
						return port;
					}
				}

			}
		}
		throw CFMLEngineFactory.getInstance().getExceptionUtil().createExpressionException
		("Can't locate port entry for service " + service.getQName().toString() + " WSDL");
	}

	public static Message getMessageByLocalName(Map<QName, Message> messages, String name) {
		Iterator<Entry<QName,Message>> it = messages.entrySet().iterator();
		Entry<QName,Message> e;
		while(it.hasNext()){
       	e = it.next();
       	//print.e(e.getKey().getLocalPart()+":"+name);
       	if(e.getKey().getLocalPart().equals(name)) return e.getValue();
       }
       return null;
	}
}