package org.lucee.extension.axis.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import lucee.loader.engine.CFMLEngineFactory;
import lucee.runtime.exp.PageException;

public class XMLUtil {

	public static final short UNDEFINED_NODE = -1;

	public static Element getRootElement(Node node) {
		Document doc = null;
		if (node instanceof Document) doc = (Document) node;
		else doc = node.getOwnerDocument();
		return doc.getDocumentElement();
	}

	public static synchronized Element getChildWithName(String name, Element el) {
		Element[] children = getChildElementsAsArray(el);
		for (int i = 0; i < children.length; i++) {
			if (name.equalsIgnoreCase(children[i].getNodeName())) return children[i];
		}
		return null;
	}

	public static Element[] getChildElementsAsArray(Node node) {
		ArrayList<Node> nodeList = getChildNodes(node, Node.ELEMENT_NODE, null);
		return nodeList.toArray(new Element[nodeList.size()]);
	}

	public static synchronized ArrayList<Node> getChildNodes(Node node, short type, String filter) {
		ArrayList<Node> rtn = new ArrayList<Node>();
		NodeList nodes = node.getChildNodes();
		int len = nodes.getLength();
		Node n;
		for (int i = 0; i < len; i++) {
			try {
				n = nodes.item(i);
				if (n != null && (type == UNDEFINED_NODE || n.getNodeType() == type)) {
					if (filter == null || filter.equals(n.getLocalName())) rtn.add(n);
				}
			}
			catch (Exception t) {
			}
		}
		return rtn;
	}

	public static Document getDocument(Node node) {
		if (node instanceof Document) return (Document) node;
		return node.getOwnerDocument();
	}

	public static List<Element> getChildElements(Element e) {
		List<Element> list = new ArrayList<Element>();
		NodeList nodes = e.getChildNodes();
		if (nodes != null) {
			int len = nodes.getLength();
			Node n;
			for (int i = 0; i < len; i++) {
				n = nodes.item(i);
				if (n instanceof Element) list.add((Element) n);
			}
		}
		return list;
	}

	public static Node toXMLStruct(Node node, boolean caseSensitive) throws PageException {
		try {
			Class<?> clazz = CFMLEngineFactory.getInstance().getClassUtil().loadClass("lucee.runtime.text.xml.XMLCaster");
			Method method = clazz.getMethod("toXMLStruct", new Class[] { Node.class, boolean.class });
			return (Node) method.invoke(null, new Object[] { node, caseSensitive });
		}
		catch (Exception e) {
			throw CFMLEngineFactory.getInstance().getCastUtil().toPageException(e);
		}
	}

	public static Element toRawElement(Object value, Element defaultValue) {
		try {
			Class<?> clazz = CFMLEngineFactory.getInstance().getClassUtil().loadClass("lucee.runtime.text.xml.XMLCaster");
			Method method = clazz.getMethod("toRawElement", new Class[] { Object.class, Element.class });
			return (Element) method.invoke(null, new Object[] { value, defaultValue });
		}
		catch (Exception e) {
			return defaultValue;
		}

	}

	public static final Document parse(InputSource xml, InputSource validator, boolean isHtml) throws PageException {
		// FUTURE use interface from loader
		try {
			Class<?> clazz = CFMLEngineFactory.getInstance().getClassUtil().loadClass("lucee.runtime.text.xml.XMLUtil");
			Method method = clazz.getMethod("parse", new Class[] { InputSource.class, InputSource.class, boolean.class });
			return (Document) method.invoke(null, new Object[] { xml, validator, isHtml });
		}
		catch (Exception e) {
			throw CFMLEngineFactory.getInstance().getCastUtil().toPageException(e);
		}
	}

	/*
	 * public static final Document parse(InputSource xml,InputSource validator, boolean isHtml) { //
	 * TODO Auto-generated method stub return null; }
	 */
}
