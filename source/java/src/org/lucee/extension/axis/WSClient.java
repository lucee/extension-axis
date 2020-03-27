package org.lucee.extension.axis;

import javax.xml.soap.SOAPHeaderElement;

import lucee.runtime.PageContext;
import lucee.runtime.config.Config;
import lucee.runtime.exp.PageException;
import lucee.runtime.type.Collection;
import lucee.runtime.type.Iteratorable;
import lucee.runtime.type.Objects;
import lucee.runtime.type.Struct;

import org.w3c.dom.Node;

public interface WSClient extends Objects, Iteratorable {

	public void addHeader(SOAPHeaderElement header) throws PageException;

	public Object callWithNamedValues(Config config, Collection.Key methodName, Struct arguments) throws PageException;

	public void addSOAPRequestHeader(String namespace, String name, Object value, boolean mustUnderstand) throws PageException;

	public Node getSOAPRequest() throws PageException;

	public Node getSOAPResponse() throws PageException;

	public Object getSOAPResponseHeader(PageContext pc, String namespace, String name, boolean asXML) throws PageException;

	public WSHandler getWSHandler();
}