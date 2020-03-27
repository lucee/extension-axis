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
package org.lucee.extension.axis;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;
import java.util.Vector;

import javax.xml.namespace.QName;
import javax.xml.rpc.encoding.TypeMapping;

import org.apache.axis.Constants;
import org.apache.axis.types.Day;
import org.apache.axis.types.Duration;
import org.apache.axis.types.Entities;
import org.apache.axis.types.Entity;
import org.apache.axis.types.Language;
import org.apache.axis.types.Month;
import org.apache.axis.types.MonthDay;
import org.apache.axis.types.NCName;
import org.apache.axis.types.NMToken;
import org.apache.axis.types.NMTokens;
import org.apache.axis.types.Name;
import org.apache.axis.types.Token;
import org.apache.axis.types.URI;
import org.apache.axis.types.URI.MalformedURIException;
import org.apache.axis.types.Year;
import org.apache.axis.types.YearMonth;
import org.apache.axis.wsdl.symbolTable.TypeEntry;
import org.lucee.extension.axis.util.ClassUtil;
import org.lucee.extension.axis.util.HTTPUtil;
import org.lucee.extension.axis.util.Reflector;

import coldfusion.xml.rpc.QueryBean;
import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.loader.util.Util;
import lucee.runtime.Component;
import lucee.runtime.ComponentScope;
import lucee.runtime.PageContext;
import lucee.runtime.component.Property;
import lucee.runtime.exp.PageException;
import lucee.runtime.type.Array;
import lucee.runtime.type.Collection;
import lucee.runtime.type.Collection.Key;
import lucee.runtime.type.ObjectWrap;
import lucee.runtime.type.Query;
import lucee.runtime.type.QueryColumn;
import lucee.runtime.type.Struct;
import lucee.runtime.type.dt.DateTime;
import lucee.runtime.type.dt.TimeSpan;
import lucee.runtime.type.scope.Argument;
import lucee.runtime.util.Cast;
import lucee.runtime.util.Creation;
import lucee.runtime.util.Decision;
import lucee.runtime.util.Excepton;
import lucee.runtime.util.Strings;

/**
 * Axis Type Caster
 */
public final class Axis1Caster {

	private static final String POJO_CLASS = "lucee.runtime.type.Pojo";
	private static CFMLEngine engine;
	private static Cast caster;
	private static Strings strings;
	private static Excepton exp;
	private static Creation creator;
	private static Decision decision;

	static {
		engine = CFMLEngineFactory.getInstance();
		caster = engine.getCastUtil();
		strings = engine.getStringUtil();
		exp = engine.getExceptionUtil();
		creator = engine.getCreationUtil();
		decision = engine.getDecisionUtil();
	}

	/**
	 * cast a value to a Axis Compatible Type
	 * 
	 * @param type
	 * @param value
	 * @return Axis Compatible Type
	 * @throws PageException
	 */
	public static Object toAxisType(Axis1Handler handler, TypeMapping tm, TimeZone tz, TypeEntry typeEntry, QName type, Object value) throws PageException {
		return _toAxisType(handler, tm, tz, typeEntry, type, null, value, new HashSet<Object>());
	}

	public static Object toAxisType(Axis1Handler handler, TypeMapping tm, Object value, Class targetClass) throws PageException {
		return _toAxisType(handler, tm, null, null, null, targetClass, value, new HashSet<Object>());
	}

	/**
	 * cast a value to a Axis Compatible Type
	 * 
	 * @param type
	 * @param value
	 * @return Axis Compatible Type
	 * @throws PageException
	 */
	private static Object _toAxisType(Axis1Handler handler, TypeMapping tm, TimeZone tz, TypeEntry typeEntry, QName type, Class targetClass, Object value, Set<Object> done)
			throws PageException {

		// first make sure we have no wrapper
		if (value instanceof ObjectWrap) {
			value = ((ObjectWrap) value).getEmbededObject();
		}

		if (done.contains(value)) {
			return null;// TODO not sure what in this case is the best solution.
		}

		done.add(value);
		try {
			if (type != null) {

				// Array Of
				if (type.getLocalPart().startsWith("ArrayOf")) {
					return toArray(handler, tm, typeEntry, type, value, done);
				}

				// XSD
				for (int i = 0; i < Constants.URIS_SCHEMA_XSD.length; i++) {
					if (Constants.URIS_SCHEMA_XSD[i].equals(type.getNamespaceURI())) {
						return toAxisTypeXSD(handler, tm, tz, type.getLocalPart(), value, done);
					}
				}
				if (strings.startsWithIgnoreCase(type.getLocalPart(), "xsd_")) {
					return toAxisTypeXSD(handler, tm, tz, type.getLocalPart().substring(4), value, done);
				}

				// SOAP
				if (type.getNamespaceURI().indexOf("soap") != -1) {
					return toAxisTypeSoap(handler, tm, type.getLocalPart(), value, done);
				}
				if (strings.startsWithIgnoreCase(type.getLocalPart(), "soap_")) {
					return toAxisTypeSoap(handler, tm, type.getLocalPart().substring(5), value, done);
				}
			}
			return _toDefinedType(handler, tm, typeEntry, type, targetClass, value, done);

		}
		finally {
			done.remove(value);
		}
	}

	private static Object toArray(Axis1Handler handler, TypeMapping tm, TypeEntry typeEntry, QName type, Object value, Set<Object> done) throws PageException {
		if (type == null || !type.getLocalPart().startsWith("ArrayOf")) throw exp.createApplicationException("invalid call of the functionn toArray");

		// get component Type
		String tmp = type.getLocalPart().substring(7);
		QName componentType = null;

		// no arrayOf embeded anymore
		if (tmp.indexOf("ArrayOf") == -1 && typeEntry != null) {
			TypeEntry ref = typeEntry.getRefType();
			componentType = ref.getQName();
		}
		if (componentType == null) {
			if (tmp.startsWith("_tns1_")) tmp = tmp.substring(6);
			componentType = new QName(type.getNamespaceURI(), tmp);
		}
		Object[] objs = caster.toNativeArray(value);
		Object[] rtns;
		List<Object> list = new ArrayList<Object>();

		Class componentClass = null;
		Object v;
		for (int i = 0; i < objs.length; i++) {
			v = _toAxisType(handler, tm, null, typeEntry, componentType, null, objs[i], done);
			list.add(v);
			if (i == 0) {
				if (v != null) componentClass = v.getClass();
			}
			else {
				if (v == null || v.getClass() != componentClass) componentClass = null;
			}

		}

		if (componentClass != null) {
			componentClass = toAxisTypeClass(componentClass);
			rtns = (Object[]) java.lang.reflect.Array.newInstance(componentClass, objs.length);
		}
		else rtns = new Object[objs.length];

		return list.toArray(rtns);
	}

	private static Object toAxisTypeSoap(Axis1Handler handler, TypeMapping tm, String local, Object value, Set<Object> done) throws PageException {
		if (local.equals(Constants.SOAP_ARRAY.getLocalPart())) return toArrayList(handler, tm, value, done);
		if (local.equals(Constants.SOAP_ARRAY12.getLocalPart())) return toArrayList(handler, tm, value, done);
		if (local.equals(Constants.SOAP_ARRAY_ATTRS11.getLocalPart())) return toArrayList(handler, tm, value, done);
		if (local.equals(Constants.SOAP_ARRAY_ATTRS12.getLocalPart())) return toArrayList(handler, tm, value, done);
		if (local.equals(Constants.SOAP_BASE64.getLocalPart())) return caster.toBinary(value);
		if (local.equals(Constants.SOAP_BASE64BINARY.getLocalPart())) return caster.toBinary(value);
		if (local.equals(Constants.SOAP_BOOLEAN.getLocalPart())) return caster.toBoolean(value);
		if (local.equals(Constants.SOAP_BYTE.getLocalPart())) return caster.toByte(value);
		if (local.equals(Constants.SOAP_DECIMAL.getLocalPart())) return new BigDecimal(caster.toDoubleValue(value));
		if (local.equals(Constants.SOAP_DOUBLE.getLocalPart())) return caster.toDouble(value);
		if (local.equals(Constants.SOAP_FLOAT.getLocalPart())) return new Float(caster.toDoubleValue(value));
		if (local.equals(Constants.SOAP_INT.getLocalPart())) return caster.toInteger(value);
		if (local.equals(Constants.SOAP_INTEGER.getLocalPart())) return caster.toInteger(value);
		if (local.equals(Constants.SOAP_LONG.getLocalPart())) return caster.toLong(value);
		if (local.equals(Constants.SOAP_MAP.getLocalPart())) return toMap(handler, tm, value, done);
		if (local.equals(Constants.SOAP_SHORT.getLocalPart())) return caster.toShort(value);
		if (local.equals(Constants.SOAP_STRING.getLocalPart())) return caster.toString(value);
		if (local.equals(Constants.SOAP_VECTOR.getLocalPart())) return toVector(handler, tm, value, done);

		return _toDefinedType(handler, tm, null, null, null, value, done);

	}

	private static Object toAxisTypeXSD(Axis1Handler handler, TypeMapping tm, TimeZone tz, String local, Object value, Set<Object> done) throws PageException {
		// if(local.equals(Constants.XSD_ANY.getLocalPart())) return value;
		if (local.equalsIgnoreCase(Constants.XSD_ANYSIMPLETYPE.getLocalPart())) return caster.toString(value);
		if (local.equalsIgnoreCase(Constants.XSD_ANYURI.getLocalPart())) return toURI(value);
		if (local.equalsIgnoreCase(Constants.XSD_STRING.getLocalPart())) return caster.toString(value);
		if (local.equalsIgnoreCase(Constants.XSD_BASE64.getLocalPart())) return caster.toBinary(value);
		if (local.equalsIgnoreCase(Constants.XSD_BOOLEAN.getLocalPart())) return caster.toBoolean(value);
		if (local.equalsIgnoreCase(Constants.XSD_BYTE.getLocalPart())) return caster.toByte(value);
		if (local.equalsIgnoreCase(Constants.XSD_DATE.getLocalPart())) return caster.toDate(value, null);
		if (local.equalsIgnoreCase(Constants.XSD_DATETIME.getLocalPart())) return caster.toDate(value, null);
		if (local.equalsIgnoreCase(Constants.XSD_DAY.getLocalPart())) return toDay(value);
		if (local.equalsIgnoreCase(Constants.XSD_DECIMAL.getLocalPart())) return new BigDecimal(caster.toDoubleValue(value));
		if (local.equalsIgnoreCase(Constants.XSD_DOUBLE.getLocalPart())) return caster.toDouble(value);
		if (local.equalsIgnoreCase(Constants.XSD_DURATION.getLocalPart())) return toDuration(value);
		if (local.equalsIgnoreCase(Constants.XSD_ENTITIES.getLocalPart())) return toEntities(value);
		if (local.equalsIgnoreCase(Constants.XSD_ENTITY.getLocalPart())) return toEntity(value);
		if (local.equalsIgnoreCase(Constants.XSD_FLOAT.getLocalPart())) return new Float(caster.toDoubleValue(value));
		if (local.equalsIgnoreCase(Constants.XSD_HEXBIN.getLocalPart())) return caster.toBinary(value);
		if (local.equalsIgnoreCase(Constants.XSD_ID.getLocalPart())) return caster.toString(value);
		if (local.equalsIgnoreCase(Constants.XSD_IDREF.getLocalPart())) return caster.toString(value);
		if (local.equalsIgnoreCase(Constants.XSD_IDREFS.getLocalPart())) return caster.toString(value);
		if (local.equalsIgnoreCase(Constants.XSD_INT.getLocalPart())) return caster.toInteger(value);
		if (local.equalsIgnoreCase(Constants.XSD_INTEGER.getLocalPart())) return caster.toInteger(value);
		if (local.equalsIgnoreCase(Constants.XSD_LANGUAGE.getLocalPart())) return toLanguage(value);
		if (local.equalsIgnoreCase(Constants.XSD_LONG.getLocalPart())) return caster.toLong(value);
		if (local.equalsIgnoreCase(Constants.XSD_MONTH.getLocalPart())) return toMonth(value);
		if (local.equalsIgnoreCase(Constants.XSD_MONTHDAY.getLocalPart())) return toMonthDay(value);
		if (local.equalsIgnoreCase(Constants.XSD_NAME.getLocalPart())) return toName(value);
		if (local.equalsIgnoreCase(Constants.XSD_NCNAME.getLocalPart())) return toNCName(value);
		if (local.equalsIgnoreCase(Constants.XSD_NEGATIVEINTEGER.getLocalPart())) return caster.toInteger(value);
		if (local.equalsIgnoreCase(Constants.XSD_NMTOKEN.getLocalPart())) return toNMToken(value);
		if (local.equalsIgnoreCase(Constants.XSD_NMTOKENS.getLocalPart())) return toNMTokens(value);
		if (local.equalsIgnoreCase(Constants.XSD_NONNEGATIVEINTEGER.getLocalPart())) return caster.toInteger(value);
		if (local.equalsIgnoreCase(Constants.XSD_NONPOSITIVEINTEGER.getLocalPart())) return caster.toInteger(value);
		if (local.equalsIgnoreCase(Constants.XSD_NORMALIZEDSTRING.getLocalPart())) return caster.toString(value);
		if (local.equalsIgnoreCase(Constants.XSD_POSITIVEINTEGER.getLocalPart())) return caster.toInteger(value);
		if (local.equalsIgnoreCase(Constants.XSD_QNAME.getLocalPart())) return toQName(value);
		if (local.equalsIgnoreCase(Constants.XSD_SCHEMA.getLocalPart())) return toQName(value);
		if (local.equalsIgnoreCase(Constants.XSD_SHORT.getLocalPart())) return caster.toShort(value);
		if (local.equalsIgnoreCase(Constants.XSD_TIME.getLocalPart())) return caster.toDate(value, tz);
		if (local.equalsIgnoreCase(Constants.XSD_TIMEINSTANT1999.getLocalPart())) return caster.toDate(value, tz);
		if (local.equalsIgnoreCase(Constants.XSD_TIMEINSTANT2000.getLocalPart())) return caster.toDate(value, tz);
		if (local.equalsIgnoreCase(Constants.XSD_TOKEN.getLocalPart())) return toToken(value);
		if (local.equalsIgnoreCase(Constants.XSD_UNSIGNEDBYTE.getLocalPart())) return caster.toByte(value);
		if (local.equalsIgnoreCase(Constants.XSD_UNSIGNEDINT.getLocalPart())) return caster.toInteger(value);
		if (local.equalsIgnoreCase(Constants.XSD_UNSIGNEDLONG.getLocalPart())) return caster.toLong(value);
		if (local.equalsIgnoreCase(Constants.XSD_UNSIGNEDSHORT.getLocalPart())) return caster.toShort(value);
		if (local.equalsIgnoreCase(Constants.XSD_YEAR.getLocalPart())) return toYear(value);
		if (local.equalsIgnoreCase(Constants.XSD_YEARMONTH.getLocalPart())) return toYearMonth(value);
		return _toDefinedType(handler, tm, null, null, null, value, done);
	}

	private static ArrayList<Object> toArrayList(Axis1Handler handler, TypeMapping tm, Object value, Set<Object> done) throws PageException {
		Array arr = caster.toArray(value);
		ArrayList<Object> al = new ArrayList<Object>();
		int len = arr.size();
		Object o;
		for (int i = 0; i < len; i++) {
			o = arr.get(i + 1, null);
			al.add(i, _toAxisType(handler, tm, null, null, null, null, o, done));
		}
		return al;
	}

	private static Object[] toNativeArray(Axis1Handler handler, TypeMapping tm, Class targetClass, Object value, Set<Object> done) throws PageException {
		Object[] objs = caster.toNativeArray(value);
		Object[] rtns;

		Class<?> componentClass = null;
		if (targetClass != null) {
			componentClass = targetClass.getComponentType();
		}

		if (componentClass != null) {
			componentClass = toAxisTypeClass(componentClass);
			rtns = (Object[]) java.lang.reflect.Array.newInstance(componentClass, objs.length);
		}
		else rtns = new Object[objs.length];

		try {
			for (int i = 0; i < objs.length; i++) {
				rtns[i] = _toAxisType(handler, tm, null, null, null, componentClass, objs[i], done);
			}
		}
		// just in case something goes wrong with typed array
		catch (ArrayStoreException ase) {
			rtns = new Object[objs.length];
			for (int i = 0; i < objs.length; i++) {
				rtns[i] = _toAxisType(handler, tm, null, null, null, componentClass, objs[i], done);
			}
		}

		return rtns;
	}

	private static Vector<Object> toVector(Axis1Handler handler, TypeMapping tm, Object value, Set<Object> done) throws PageException {
		Array arr = caster.toArray(value);
		Vector<Object> v = new Vector<Object>();
		int len = arr.size();
		Object o;
		for (int i = 0; i < len; i++) {
			o = arr.get(i + 1, null);
			v.set(i, _toAxisType(handler, tm, null, null, null, null, o, done));
		}
		return v;
	}

	/*
	 * public static Component toComponent(PageContext pc, Object pojo, String compPath , Component
	 * defaultValue) { try { Component cfc = pc.loadComponent(compPath); Property[] props =
	 * cfc.getProperties(false, true, false, false); PojoIterator it=new PojoIterator(pojo); // only
	 * when the same amount of properties if(props.length==it.size()) { Map<Collection.Key, Property>
	 * propMap = toMap(props); Property p; Pair<Collection.Key,Object> pair; ComponentScope scope =
	 * cfc.getComponentScope(); while(it.hasNext()){ pair=it.next(); p=propMap.get(pair.getName());
	 * if(p==null) return defaultValue; Object val = null; try { val = caster.castTo(pc, p.getType(),
	 * pair.getValue(), false); } catch (PageException e) { }
	 * 
	 * // store in variables and this scope scope.setEL(pair.getName(), val); cfc.setEL(pair.getName(),
	 * val); } return cfc; } } catch (PageException e) {} return defaultValue; }
	 */

	private static Map<Collection.Key, Property> toMap(Property[] props) {
		Map<Collection.Key, Property> map = new HashMap<Collection.Key, Property>();
		for (int i = 0; i < props.length; i++) {
			map.put(creator.createKey(props[i].getName()), props[i]);
		}
		return map;
	}

	public static Object toPojo(Axis1Handler handler, Object pojo, TypeMapping tm, TypeEntry typeEntry, QName type, Component comp, Set<Object> done) throws PageException {
		PageContext pc = engine.getThreadPageContext();
		try {
			return _toPojo(handler, pc, pojo, tm, typeEntry, type, comp, done);
		}
		catch (Exception e) {
			throw caster.toPageException(e);
		}
	}

	private static Object _toPojo(Axis1Handler handler, PageContext pc, Object pojo, TypeMapping tm, TypeEntry typeEntry, QName type, Component comp, Set<Object> done)
			throws PageException {// print.ds();System.exit(0);
		comp = ClassUtil.toComponentSpecificAccess(Component.ACCESS_PRIVATE, comp);
		ComponentScope scope = comp.getComponentScope();

		// create Pojo
		if (pojo == null) {
			try {
				pojo = engine.getClassUtil().loadInstance(engine.getTemplateUtil().getComponentPropertiesClass(pc, comp));
			}
			catch (Exception e) {
				throw caster.toPageException(e);
			}
		}

		// initialize Pojo
		Property[] props = comp.getProperties(false, true, false, false);
		_initPojo(handler, pc, typeEntry, type, pojo, props, scope, comp, tm, done);

		return pojo;
	}

	public static Object toPojo(Axis1Handler handler, Object pojo, TypeMapping tm, TypeEntry typeEntry, QName type, Struct sct, Set<Object> done) throws PageException {
		PageContext pc = engine.getThreadPageContext();
		try {
			return _toPojo(handler, pc, pojo, tm, typeEntry, type, sct, done);
		}
		catch (Exception e) {
			throw caster.toPageException(e);
		}
	}

	private static Object _toPojo(Axis1Handler handler, PageContext pc, Object pojo, TypeMapping tm, TypeEntry typeEntry, QName type, Struct sct, Set<Object> done)
			throws PageException {// print.ds();System.exit(0);
		if (pojo == null) {
			try {
				ClassLoader cl = pc.getConfig().getRPCClassLoader(false);
				pojo = engine.getClassUtil().loadInstance(ClassUtil.getStructPropertiesClass(pc, sct, cl));
			}
			catch (Exception e) {
				throw caster.toPageException(e);
			}
		}

		// initialize
		List<Property> props = new ArrayList<Property>();
		Iterator<Entry<Key, Object>> it = sct.entryIterator();
		Entry<Key, Object> e;
		Property p;
		while (it.hasNext()) {
			e = it.next();
			p = creator.createProperty(e.getKey().getString(), e.getValue() == null ? "any" : caster.toTypeName(e.getValue()));
			ClassUtil.setAccess(p, Component.ACCESS_PUBLIC);
			props.add(p);
		}

		_initPojo(handler, pc, typeEntry, type, pojo, props.toArray(new Property[props.size()]), sct, null, tm, done);

		return pojo;
	}

	private static void _initPojo(Axis1Handler handler, PageContext pc, TypeEntry typeEntry, QName type, Object pojo, Property[] props, Struct sct, Component comp, TypeMapping tm,
			Set<Object> done) throws PageException {
		Property p;
		Object v;
		Collection.Key k;
		// CFMLExpressionInterpreter interpreter = new CFMLExpressionInterpreter(false);

		for (int i = 0; i < props.length; i++) {
			p = props[i];
			k = caster.toKey(p.getName());
			// value
			v = sct.get(k, null);
			if (v == null && comp != null) v = comp.get(k, null);

			// default

			if (v != null) v = caster.castTo(pc, p.getType(), v, false);
			else {
				if (!Util.isEmpty(p.getDefault())) {
					try {
						v = caster.castTo(pc, p.getType(), p.getDefault(), false);

					}
					catch (PageException pe) {
						try {
							v = pc.evaluate(p.getDefault());
							v = caster.castTo(pc, p.getType(), v, false);
						}
						catch (PageException pe2) {
							throw exp.createExpressionException(
									"can not use default value [" + p.getDefault() + "] for property [" + p.getName() + "] with type [" + p.getType() + "]");
						}
					}
				}
			}

			// set or throw
			if (v == null) {
				if (p.isRequired()) throw exp.createExpressionException("required property [" + p.getName() + "] is not defined");
			}
			else {
				TypeEntry childTE = null;
				QName childT = null;
				if (typeEntry != null) {
					childTE = handler.getContainedElement(typeEntry, p.getName(), null);
					if (childTE != null) childT = childTE.getQName();

				}
				Reflector.callSetter(pojo, p.getName().toLowerCase(), _toAxisType(handler, tm, null, childTE, childT, null, v, done));
			}
		}
	}

	private static QueryBean toQueryBean(Axis1Handler handler, TypeMapping tm, Object value, Set<Object> done) throws PageException {
		Query query = caster.toQuery(value);
		int recordcount = query.getRecordcount();
		String[] columnList = query.getColumns();
		QueryColumn[] columns = new QueryColumn[columnList.length];
		Object[][] data = new Object[recordcount][columnList.length];

		for (int i = 0; i < columnList.length; i++) {
			columns[i] = query.getColumn(columnList[i]);
		}

		int row;
		for (row = 1; row <= recordcount; row++) {
			for (int i = 0; i < columns.length; i++) {
				data[row - 1][i] = _toAxisType(handler, tm, null, null, null, null, columns[i].get(row, null), done);
			}
		}

		QueryBean qb = new QueryBean();
		qb.setColumnList(columnList);
		qb.setData(data);
		return qb;

	}

	private static Map<String, Object> toMap(Axis1Handler handler, TypeMapping tm, Object value, Set<Object> done) throws PageException {
		Struct src = caster.toStruct(value);

		HashMap<String, Object> trg = new HashMap<String, Object>();
		Iterator<Entry<Key, Object>> it = src.entryIterator();
		Entry<Key, Object> e;
		while (it.hasNext()) {
			e = it.next();
			trg.put(e.getKey().getString(), _toAxisType(handler, tm, null, null, null, null, e.getValue(), done));

		}
		return trg;

	}

	private static Object _toDefinedType(Axis1Handler handler, TypeMapping tm, TypeEntry typeEntry, QName type, Class targetClass, Object value, Set<Object> done)
			throws PageException {

		// Date
		if (value instanceof Date) {// not set to decision.isDate(value)
			return new Date(((Date) value).getTime());
		}

		Class clazz = type == null ? null : ((org.apache.axis.encoding.TypeMapping) tm).getClassForQName(type);
		// Pojo
		if (clazz != null && engine.getClassUtil().isInstaneOf(clazz, POJO_CLASS)) {
			Object pojo;
			try {
				pojo = engine.getClassUtil().loadInstance(clazz);
			}
			catch (Exception e) {
				throw caster.toPageException(e);
			}
			// Struct
			if (decision.isStruct(value)) {

				if (value instanceof Component) return toPojo(handler, pojo, tm, typeEntry, type, (Component) value, done);
				return toPojo(handler, pojo, tm, typeEntry, type, caster.toStruct(value), done);
			}
		}

		// No Mapping found

		// Array
		if (decision.isArray(value) && !(value instanceof Argument)) {
			if (value instanceof byte[]) return value;
			return toNativeArray(handler, tm, targetClass, value, done);
		}
		// Struct
		if (decision.isStruct(value)) {
			if (value instanceof Component) {
				Object pojo = toPojo(handler, null, tm, null, null, (Component) value, done);
				try {
					if (type == null || type.getLocalPart().equals("anyType")) {
						type = new QName(getRequestDefaultNameSpace(), pojo.getClass().getName());
						// type= new QName(getRequestNameSpace(),pojo.getClass().getName());
						// print.ds("missing type for "+pojo.getClass().getName());
					}
					TypeMappingUtil.registerBeanTypeMapping(tm, pojo.getClass(), type);

				}
				catch (Exception fault) {
					throw caster.toPageException(fault);
				}
				return pojo;
			}
			/*
			 * if(type!=null && !type.getLocalPart().equals("anyType")) { Object pojo=
			 * toPojo(null,tm,caster.toStruct(value),targetClass,done);
			 * 
			 * //Map<String, Object> map = toMap(tm,value,targetClass,done);
			 * //TypeMappingUtil.registerMapTypeMapping(tm, map.getClass(), type);
			 * TypeMappingUtil.registerBeanTypeMapping(tm, pojo.getClass(), type); return pojo; }
			 */
			return toMap(handler, tm, value, done);

		}
		// Query
		if (decision.isQuery(value)) return toQueryBean(handler, tm, value, done);
		// Other
		return value;
	}

	public static Class toAxisTypeClass(Class clazz) {
		if (clazz.isArray()) {
			return engine.getClassUtil().toArrayClass(toAxisTypeClass(clazz.getComponentType()));
		}

		if (Query.class == clazz) return QueryBean.class;
		if (Array.class == clazz) return Object[].class;
		if (Struct.class == clazz) return Map.class;
		// if(Struct[].class==clazz) return Map[].class;
		// if(Query[].class==clazz) return QueryBean[].class;

		return clazz;
	}

	private static Object toURI(Object value) throws PageException {
		if (value instanceof URI) return value;
		if (value instanceof java.net.URI) return value;
		try {
			return new URI(caster.toString(value));
		}
		catch (MalformedURIException e) {
			throw caster.toPageException(e);
		}
	}

	private static Token toToken(Object value) throws PageException {
		if (value instanceof Token) return (Token) value;
		return new Token(caster.toString(value));
	}

	private static QName toQName(Object value) throws PageException {
		if (value instanceof QName) return (QName) value;
		return new QName(caster.toString(value));
	}

	private static NMTokens toNMTokens(Object value) throws PageException {
		if (value instanceof NMTokens) return (NMTokens) value;
		return new NMTokens(caster.toString(value));
	}

	private static NMToken toNMToken(Object value) throws PageException {
		if (value instanceof NMToken) return (NMToken) value;
		return new NMToken(caster.toString(value));
	}

	private static NCName toNCName(Object value) throws PageException {
		if (value instanceof NCName) return (NCName) value;
		return new NCName(caster.toString(value));
	}

	private static Name toName(Object value) throws PageException {
		if (value instanceof Name) return (Name) value;
		return new Name(caster.toString(value));
	}

	private static Language toLanguage(Object value) throws PageException {
		if (value instanceof Language) return (Language) value;
		return new Language(caster.toString(value));
	}

	private static Entities toEntities(Object value) throws PageException {
		if (value instanceof Entities) return (Entities) value;
		return new Entities(caster.toString(value));
	}

	private static Entity toEntity(Object value) throws PageException {
		if (value instanceof Entity) return (Entity) value;
		return new Entity(caster.toString(value));
	}

	private static Day toDay(Object value) throws PageException {
		if (value instanceof Day) return (Day) value;
		if (decision.isDate(value, false)) {
			return new Day(caster.toDate(value, null).getDate());
		}

		try {
			return new Day(caster.toIntValue(value));
		}
		catch (Exception e) {
			try {
				return new Day(caster.toString(value));
			}
			catch (NumberFormatException nfe) {
				throw caster.toPageException(nfe);
			}
			catch (PageException ee) {
				throw ee;
			}
		}
	}

	private static Year toYear(Object value) throws PageException {
		if (value instanceof Year) return (Year) value;
		if (decision.isDate(value, false)) {
			return new Year(caster.toDate(value, null).getYear());
		}
		try {
			return new Year(caster.toIntValue(value));
		}
		catch (Exception e) {
			try {
				return new Year(caster.toString(value));
			}
			catch (NumberFormatException nfe) {
				throw caster.toPageException(nfe);
			}
			catch (PageException ee) {
				throw ee;
			}
		}
	}

	private static Month toMonth(Object value) throws PageException {
		if (value instanceof Month) return (Month) value;
		if (decision.isDate(value, false)) {
			return new Month(caster.toDate(value, null).getMonth());
		}
		try {
			return new Month(caster.toIntValue(value));
		}
		catch (Exception e) {
			try {
				return new Month(caster.toString(value));
			}
			catch (NumberFormatException nfe) {
				throw caster.toPageException(nfe);
			}
			catch (PageException ee) {
				throw ee;
			}
		}
	}

	private static YearMonth toYearMonth(Object value) throws PageException {
		if (value instanceof YearMonth) return (YearMonth) value;
		if (decision.isDate(value, false)) {
			DateTime dt = caster.toDate(value, null);
			return new YearMonth(dt.getYear(), dt.getMonth());
		}

		try {
			return new YearMonth(caster.toString(value));
		}
		catch (NumberFormatException nfe) {
			throw caster.toPageException(nfe);
		}
		catch (PageException ee) {
			throw ee;
		}
	}

	private static MonthDay toMonthDay(Object value) throws PageException {
		if (value instanceof MonthDay) return (MonthDay) value;
		if (decision.isDate(value, false)) {
			DateTime dt = caster.toDate(value, null);
			return new MonthDay(dt.getMonth(), dt.getDate());
		}

		try {
			return new MonthDay(caster.toString(value));
		}
		catch (NumberFormatException nfe) {
			throw caster.toPageException(nfe);
		}
		catch (PageException ee) {
			throw ee;
		}
	}

	private static Duration toDuration(Object value) throws PageException, IllegalArgumentException {
		if (value instanceof Duration) return (Duration) value;
		try {
			TimeSpan ts = caster.toTimespan(value);
			return new Duration(true, 0, 0, ts.getDay(), ts.getHour(), ts.getMinute(), ts.getSecond());
		}
		catch (PageException e) {
			return new Duration(caster.toString(value));
		}
	}

	public static Object toLuceeType(PageContext pc, Object value) throws PageException {
		return toLuceeType(pc, null, value);
	}

	public static Object toLuceeType(PageContext pc, String customType, Object value) throws PageException {
		if (pc == null) pc = engine.getThreadPageContext();
		if (pc != null && value != null && engine.getClassUtil().isInstaneOf(value.getClass(), POJO_CLASS)) {
			if (!Util.isEmpty(customType)) {
				Component cfc = ClassUtil.toComponent(pc, value, customType, null);
				if (cfc != null) return cfc;
			}
		}
		if (value instanceof Date || value instanceof Calendar) {// do not change to caster.isDate
			return caster.toDate(value, null);
		}
		if (value instanceof Object[]) {
			Object[] arr = (Object[]) value;
			if (arr != null && arr.length > 0) {
				boolean allTheSame = true;
				// byte
				if (arr[0] instanceof Byte) {
					for (int i = 1; i < arr.length; i++) {
						if (!(arr[i] instanceof Byte)) {
							allTheSame = false;
							break;
						}
					}
					if (allTheSame) {
						byte[] bytes = new byte[arr.length];
						for (int i = 0; i < arr.length; i++) {
							bytes[i] = caster.toByteValue(arr[i]);
						}
						return bytes;
					}
				}
			}
		}
		if (value instanceof Byte[]) {
			Byte[] arr = (Byte[]) value;
			if (arr != null && arr.length > 0) {
				byte[] bytes = new byte[arr.length];
				for (int i = 0; i < arr.length; i++) {
					bytes[i] = arr[i].byteValue();
				}
				return bytes;
			}
		}
		if (value instanceof byte[]) {
			return value;
		}
		if (decision.isArray(value)) {

			Array a = caster.toArray(value);
			int len = a.size();
			Object o;
			String ct;
			for (int i = 1; i <= len; i++) {
				o = a.get(i, null);
				if (o != null) {
					ct = customType != null && customType.endsWith("[]") ? customType.substring(0, customType.length() - 2) : null;
					a.setEL(i, toLuceeType(pc, ct, o));
				}
			}
			return a;
		}
		if (value instanceof Map) {
			Struct sct = creator.createStruct();
			Iterator it = ((Map) value).entrySet().iterator();
			Map.Entry entry;
			while (it.hasNext()) {
				entry = (Entry) it.next();
				sct.setEL(caster.toString(entry.getKey()), toLuceeType(pc, null, entry.getValue()));
			}
			return sct;

			// return StructUtil.copyToStruct((Map)value);
		}
		if (isQueryBean(value)) {
			QueryBean qb = (QueryBean) value;
			String[] strColumns = qb.getColumnList();
			Object[][] data = qb.getData();
			int recorcount = data.length;
			Query qry = creator.createQuery(strColumns, recorcount, "QueryBean");
			QueryColumn[] columns = new QueryColumn[strColumns.length];
			for (int i = 0; i < columns.length; i++) {
				columns[i] = qry.getColumn(strColumns[i]);
			}

			int row;
			for (row = 1; row <= recorcount; row++) {
				for (int i = 0; i < columns.length; i++) {
					columns[i].set(row, toLuceeType(pc, null, data[row - 1][i]));
				}
			}
			return qry;
		}
		if (decision.isQuery(value)) {
			Query q = caster.toQuery(value);
			int recorcount = q.getRecordcount();
			String[] strColumns = q.getColumns();

			QueryColumn col;
			int row;
			for (int i = 0; i < strColumns.length; i++) {
				col = q.getColumn(strColumns[i]);
				for (row = 1; row <= recorcount; row++) {
					col.set(row, toLuceeType(pc, null, col.get(row, null)));
				}
			}
			return q;
		}
		return value;
	}

	private static boolean isQueryBean(Object value) {
		return (value instanceof QueryBean);
	}

	public static QName toComponentType(QName qName, QName defaultValue) {
		String lp = qName.getLocalPart();
		String uri = qName.getNamespaceURI();
		if (lp.startsWith("ArrayOf")) return new QName(uri, lp.substring(7));
		return defaultValue;
	}

	public static String getRequestNameSpace() {
		String rawURL = HTTPUtil.getRequestURL(engine.getThreadPageContext().getHttpServletRequest(), false);
		String urlPath = "";
		try {
			urlPath = new java.net.URL(rawURL).getPath();
		}
		catch (MalformedURLException e) {}
		String pathWithoutContext = urlPath.replaceFirst("/[^/]*", "");

		return RPCConstants.WEBSERVICE_NAMESPACE_URI + pathWithoutContext.toLowerCase();
	}

	public static String getRequestDefaultNameSpace() {
		return RPCConstants.WEBSERVICE_NAMESPACE_URI;
	}

}