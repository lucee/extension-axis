package org.lucee.extension.axis.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import lucee.loader.engine.CFMLEngineFactory;
import lucee.runtime.exp.PageException;

public class Reflector {
	
	public static Method[] getGetters(Class clazz) {
		Method[] methods = clazz.getMethods();
		List<Method> list=new ArrayList<Method>();
		for(int i=0;i<methods.length;i++) {
			if(isGetter(methods[i])) list.add(methods[i]);
		}
		if(list.size()==0) return new Method[0];
		return (Method[]) list.toArray(new Method[list.size()]);
	}
	
	public static boolean isGetter(Method method) {
		if(method.getParameterTypes().length>0) return false;
		if(method.getReturnType()==void.class) return false;
		if(!method.getName().startsWith("get") && !method.getName().startsWith("is")) return false;
		if(method.getDeclaringClass()==Object.class) return false;
		return true;
	}

	public static boolean isSetter(Method method) {
		if(method.getParameterTypes().length!=1) return false;
		if(method.getReturnType()!=void.class) return false;
		if(!method.getName().startsWith("set")) return false;
		if(method.getDeclaringClass()==Object.class) return false;
		return true;
	}

	/**
	 * to invoke a setter Method of a Object
	 * @param obj Object to invoke method from
	 * @param prop Name of the Method without get
	 * @param value Value to set to the Method
	 * @throws PageException
	 */
	public static void callSetter(Object obj, String prop,Object value) throws PageException {
		try {
		    getSetter(obj, prop, value).invoke(obj,new Object[]{value});
		}
		catch (Exception e) {
			throw CFMLEngineFactory.getInstance().getCastUtil().toPageException(e);
		}
	}
	
    /**
     * to invoke a setter Method of a Object
     * @param obj Object to invoke method from
     * @param prop Name of the Method without get
     * @param value Value to set to the Method
     * @return MethodInstance
     * @throws NoSuchMethodException
     * @throws PageException
     */
    private static Method getSetter(Object obj, String prop,Object value) throws NoSuchMethodException {
            prop="set"+CFMLEngineFactory.getInstance().getStringUtil().ucFirst(prop);
            Method m = obj.getClass().getMethod(prop, new Class[]{value.getClass()}); //getMethodInstance(obj,obj.getClass(),prop,new Object[]{value});
            
            if(m.getReturnType()!=void.class)
                throw new NoSuchMethodException("invalid return Type, method ["+m.getName()+"] must have return type void, now ["+m.getReturnType().getName()+"]");
            return m;
    }
}
