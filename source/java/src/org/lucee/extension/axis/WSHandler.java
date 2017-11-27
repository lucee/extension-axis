package org.lucee.extension.axis;

import lucee.runtime.PageContext;
import lucee.runtime.exp.PageException;
import lucee.runtime.net.proxy.ProxyData;

public interface WSHandler {

	public boolean isSOAPRequest();
	public void addSOAPResponseHeader(String namespace, String name, Object value, boolean mustUnderstand) throws PageException;
	public Object getSOAPRequestHeader(PageContext pc, String namespace, String name, boolean asXML) throws PageException;
	public String getTypeAsString();
	public Class<?> toWSTypeClass(Class<?> clazz);
	public WSServer getWSServer(PageContext pc) throws PageException;
	public WSClient getWSClient(String wsdlUrl, String username, String password, ProxyData proxyData) throws PageException;
}