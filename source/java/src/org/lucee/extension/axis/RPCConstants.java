package org.lucee.extension.axis;

import javax.xml.namespace.QName;

public final class RPCConstants {

	public static final String WEBSERVICE_NAMESPACE_URI = "http://rpc.xml.coldfusion";
	public static final QName COMPONENT = new QName(WEBSERVICE_NAMESPACE_URI, "Component");
	public static QName QUERY_QNAME = new QName(WEBSERVICE_NAMESPACE_URI, "QueryBean");
	public static QName ARRAY_QNAME = new QName(WEBSERVICE_NAMESPACE_URI, "Array");
	// private static QName componentQName=new QName("http://components.test.jm","address");
	// private static QName dateTimeQName=new QName("http://www.w3.org/2001/XMLSchema","dateTime");
	public static final QName STRING_QNAME = new QName("http://www.w3.org/2001/XMLSchema", "string");
}