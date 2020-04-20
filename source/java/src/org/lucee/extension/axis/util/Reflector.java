package org.lucee.extension.axis.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import lucee.loader.engine.CFMLEngineFactory;
import lucee.runtime.exp.PageException;

public class Reflector {

	public static Method[] getGetters(Class clazz) {
		Method[] methods = clazz.getMethods();
		List<Method> list = new ArrayList<Method>();
		for (int i = 0; i < methods.length; i++) {
			if (isGetter(methods[i])) list.add(methods[i]);
		}
		if (list.size() == 0) return new Method[0];
		return list.toArray(new Method[list.size()]);
	}

	public static boolean isGetter(Method method) {
		if (method.getParameterTypes().length > 0) return false;
		if (method.getReturnType() == void.class) return false;
		if (!method.getName().startsWith("get") && !method.getName().startsWith("is")) return false;
		if (method.getDeclaringClass() == Object.class) return false;
		return true;
	}

	public static boolean isSetter(Method method) {
		if (method.getParameterTypes().length != 1) return false;
		if (method.getReturnType() != void.class) return false;
		if (!method.getName().startsWith("set")) return false;
		if (method.getDeclaringClass() == Object.class) return false;
		return true;
	}

	/**
	 * to invoke a setter Method of a Object
	 * 
	 * @param obj Object to invoke method from
	 * @param prop Name of the Method without get
	 * @param value Value to set to the Method
	 * @throws PageException
	 */
	public static void callSetter(Object obj, String prop, Object value) throws PageException {
		try {
			getSetter(obj, prop, value).invoke(obj, new Object[] { value });
		}
		catch (Exception e) {
			throw CFMLEngineFactory.getInstance().getCastUtil().toPageException(e);
		}
	}

	/**
	 * to invoke a setter Method of a Object
	 * 
	 * @param obj Object to invoke method from
	 * @param prop Name of the Method without get
	 * @param value Value to set to the Method
	 * @return MethodInstance
	 * @throws NoSuchMethodException
	 * @throws PageException
	 */
	private static Method getSetter(Object obj, String prop, Object value) throws NoSuchMethodException {
		prop = "set" + CFMLEngineFactory.getInstance().getStringUtil().ucFirst(prop);
		List<Method> matches = getMethods(obj.getClass(), prop, 1, false);
		Class<? extends Object> vClass = value.getClass();
		Class<? extends Object> pvClass = toPrimitiveType(vClass, null);
		Class<?> param;
		for (Method m: matches) {
			param = m.getParameters()[0].getType();
			if (param.getName().equals(vClass.getName()) || (null != pvClass && param.getName().equals(pvClass.getName()))) return m;
		}
		throw new NoSuchMethodException("no method " + prop + "(" + vClass + ") found in class " + obj.getClass().getName());
	}

	private static List<Method> getMethods(Class<? extends Object> clazz, String name, int argCount, boolean hasReturn) {
		List<Method> matches = new ArrayList<>();
		Method[] methods = clazz.getMethods();
		Class<?> rt;
		// exact match
		for (Method m: methods) {
			rt = m.getReturnType();
			if (m.getParameterCount() == argCount && (hasReturn ? rt != void.class : rt == void.class) && m.getName().equals(name)) matches.add(m);
		}
		// similar match
		for (Method m: methods) {
			rt = m.getReturnType();
			if (m.getParameterCount() == argCount && (hasReturn ? rt != void.class : rt == void.class) && !m.getName().equals(name) && m.getName().equalsIgnoreCase(name))
				matches.add(m);
		}

		return matches;
	}

	private static Class toPrimitiveType(Class<? extends Object> vClass, Class defaultValue) {
		if (vClass == Boolean.class) return boolean.class;
		if (vClass == Byte.class) return byte.class;
		if (vClass == Short.class) return short.class;
		if (vClass == Integer.class) return int.class;
		if (vClass == Long.class) return long.class;
		if (vClass == Float.class) return float.class;
		if (vClass == Double.class) return double.class;
		if (vClass == Character.class) return char.class;
		return defaultValue;
	}
}
