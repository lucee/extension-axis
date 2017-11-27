package org.lucee.extension.axis.util;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import lucee.commons.io.log.Log;
import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.runtime.Component;
import lucee.runtime.PageContext;
import lucee.runtime.component.Property;
import lucee.runtime.config.Config;
import lucee.runtime.dump.DumpData;
import lucee.runtime.dump.DumpProperties;
import lucee.runtime.exp.PageException;
import lucee.runtime.listener.ApplicationContext;
import lucee.runtime.net.proxy.ProxyData;
import lucee.runtime.type.Struct;
import lucee.runtime.util.Cast;

public class ClassUtil {
	
	private static CFMLEngine engine;
	private static Cast caster;
	private static lucee.runtime.util.ClassUtil classUtil;

	static {
		engine= CFMLEngineFactory.getInstance();
		caster =engine.getCastUtil();
		classUtil=engine.getClassUtil();
	}
	
	public static ClassLoader getRPCClassLoader(PageContext pc, boolean reload) throws PageException {
		try {
			Method m = pc.getClass().getMethod("getRPCClassLoader", new Class[]{boolean.class});
			return (ClassLoader) m.invoke(pc, new Object[]{reload});
		}
		catch (Exception e) {
			throw caster.toPageException(e);
		}
	}

	public static boolean getWSMaintainSession(ApplicationContext ac) throws PageException, RuntimeException {
		try {
			Method m = ac.getClass().getMethod("getWSMaintainSession", new Class[]{});
			return caster.toBooleanValue(m.invoke(ac, new Object[]{}));
			
		}
		catch (Exception e) {
			throw caster.toPageException(e);
		}
	}
	
	public static String createId(String wsdlUrl, String username,String password, ProxyData proxyData, String methodName,Object[] arguments, Struct namedArguments) throws PageException {
		String className="lucee.runtime.cache.tag.CacheHandlerCollectionImpl";
		try {
			Class<?> clazz = classUtil.loadClass(className);
			Method m = clazz.getMethod("createId", new Class[]{
					String.class, String.class, String.class, ProxyData.class, String.class ,Object[].class, Struct.class
			});
			return engine.getCastUtil().toString(m.invoke(null, new Object[]{
					wsdlUrl, username, password, proxyData, methodName, arguments, namedArguments
			}));	
		}
		catch (Exception e) {
			throw caster.toPageException(e);
		}
	}
	
	public static void argumentCollection(Struct values) throws PageException {
		String className="lucee.runtime.type.util.UDFUtil";
		try {
			Class<?> clazz = classUtil.loadClass(className);
			Method m = clazz.getMethod("argumentCollection", new Class[]{Struct.class});
			m.invoke(null, new Object[]{values});
			
		}
		catch (Exception e) {
			throw caster.toPageException(e);
		}
	}

	public static DumpData toDumpData(Object o, PageContext pageContext, int maxlevel, DumpProperties props) throws PageException {
		String className="lucee.runtime.dump.DumpUtil";
		try {
			Class<?> clazz = classUtil.loadClass(className);
			Method m = clazz.getMethod("toDumpData", new Class[]{Object.class, PageContext.class, int.class, DumpProperties.class});
			return (DumpData) m.invoke(null, new Object[]{o, pageContext, maxlevel, props});
			
		}
		catch (Exception e) {
			throw caster.toPageException(e);
		}
	}

	public static Class<?> getComponentPropertiesClass(Config config, String className, Object[] asmProperties,Class extendsClass) throws PageException {
		try{
			Class<?> compUtilClass = classUtil.loadClass("lucee.runtime.type.util.ComponentUtil");
			Class<?> asmPropArrayClass=getASMPropertyArrayClass();
			Method m = compUtilClass.getMethod("getComponentPropertiesClass", new Class[]{Config.class, String.class,asmPropArrayClass,Class.class});
			return (Class<?>) m.invoke(null, new Object[]{config,className,asmProperties,extendsClass});
			
		}
		catch(Exception e) {
			throw caster.toPageException(e);
		}
	}

	public static Class<?> getClientComponentPropertiesClass(PageContext pc, String className, Object[] asmProperties, Class extendsClass) throws PageException {
		try{
			Class<?> compUtilClass = classUtil.loadClass("lucee.runtime.type.util.ComponentUtil");
			Class<?> asmPropArrayClass=getASMPropertyArrayClass();
			Method m = compUtilClass.getMethod("getClientComponentPropertiesClass", new Class[]{PageContext.class, String.class,asmPropArrayClass,Class.class});
			return (Class<?>) m.invoke(null, new Object[]{pc,className,asmProperties,extendsClass});
			
		}
		catch(Exception e) {
			throw caster.toPageException(e);
		}
	}
	
	private static Class<?> getASMPropertyArrayClass() throws IOException {
		Class<?> asmPropArrayClass;
		try{
			asmPropArrayClass = classUtil.loadClass("[lucee.transformer.bytecode.util.ASMProperty;");
		}
		catch(IOException ioe) { // should not happen
			Class<?> tmp = classUtil.loadClass("lucee.transformer.bytecode.util.ASMProperty");
			asmPropArrayClass = Array.newInstance(tmp, 0).getClass();
		}
		return asmPropArrayClass;
	}
	
	
	public static Class getStructPropertiesClass(PageContext pc,Struct sct, ClassLoader cl) throws PageException {
		try {	
			Class<?> cu = classUtil.loadClass("lucee.runtime.type.util.ComponentUtil");
			Class<?> pcl = classUtil.loadClass("lucee.commons.lang.PhysicalClassLoader");
			
			Method m = cu.getMethod("getStructPropertiesClass", new Class[]{PageContext.class, Struct.class, pcl});
			return (Class<?>) m.invoke(null, new Object[]{pc,sct,cl});	
		}
		catch (Exception e) {
			throw caster.toPageException(e);
		}
	}

	public static void setAccess(Property p, int access) {
		try {
			Method m = p.getClass().getMethod("setAccess", new Class[]{int.class});
			m.invoke(p, new Object[]{access});
		}
		catch (Exception e) {e.printStackTrace();}
	}

	public static Component toComponentSpecificAccess(int access, Component component) throws PageException {
		try {
			Class<?> clazz = classUtil.loadClass("lucee.runtime.ComponentSpecificAccess");
			Method m = clazz.getMethod("toComponentSpecificAccess", new Class[]{int.class, Component.class});
			return (Component) m.invoke(null, new Object[]{access,component});
		}
		catch (Exception e) {
			throw caster.toPageException(e);
		}
	}

	public static Object createASMProperty(Class type,String name) throws PageException {
		try {
			Class<?> clazz = classUtil.loadClass("lucee.transformer.bytecode.util.ASMPropertyImpl");
			Constructor<?> c = clazz.getConstructor(new Class[]{Class.class, String.class});
			return c.newInstance(new Object[]{type,name});
		} 
		catch (Exception e) {
			throw caster.toPageException(e);
		}
	}

	public static Object[] toASMPropertyArray(List properties) throws PageException {
		try {
			Class<?> asmPropClass = classUtil.loadClass("lucee.transformer.bytecode.util.ASMProperty");
			
			// create array
			Object array = Array.newInstance(asmPropClass, properties.size());
			
			// fill array
			Iterator it = properties.iterator();
			int index=0;
			while(it.hasNext()) {
				Array.set(array, index++, it.next());
			}
			return (Object[]) array;
			
		} 
		catch (Exception e) {
			throw caster.toPageException(e);
		}
	}

	public static Logger getLogger(Log logger) throws PageException { // this method is from interface LogAdapter
		try {
			Method m = logger.getClass().getMethod("getLogger", new Class[0]);
			return (Logger) m.invoke(logger, new Object[0]);
			
		}
		catch (Exception e) {
			throw caster.toPageException(e);
		}
	}

	public static Class cfTypeToClass(String type) throws PageException {
		try {
			Class<?> clazz = classUtil.loadClass("lucee.runtime.op.Caster");
			Method m = clazz.getMethod("cfTypeToClass", new Class[]{String.class});
			return (Class) m.invoke(null, new Object[]{type});
		}
		catch (Exception e) {
			throw caster.toPageException(e);
		}
	}

	public static Component toComponent(PageContext pc, Object pojo, String compPath , Component defaultValue) {
		try {
			Class<?> casterClass = classUtil.loadClass("lucee.runtime.op.Caster");
			Class<?> pojoClass = classUtil.loadClass("lucee.runtime.type.Pojo");
			
			Method m = casterClass.getMethod("toComponent", new Class[]{PageContext.class,pojoClass,String.class,Component.class});
			return (Component) m.invoke(null, new Object[]{pc,pojo,compPath,defaultValue});
		}
		catch (Exception e) {
			PageException pe = caster.toPageException(e);
			throw engine.getExceptionUtil().createPageRuntimeException(pe);
		}
	}
}
