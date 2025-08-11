package org.lucee.extension.axis.util;

import org.apache.axis.transport.http.AxisHttpSession;

import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.runtime.exp.PageException;

public class JakartaUtil {

	private static final boolean RESOLVE_WRAPPER = true;

	public static Object toJavaxHttpServletRequest(jakarta.servlet.http.HttpServletRequest jakRequest) throws PageException {
		if (RESOLVE_WRAPPER) {
			Object obj = jakRequest;

			while ("lucee.runtime.net.http.HTTPServletRequestWrap".equals(obj.getClass().getName())) {
				CFMLEngine eng = CFMLEngineFactory.getInstance();
				obj = eng.getClassUtil().callMethod(obj, eng.getCastUtil().toKey("getOriginalRequest"), new Object[] {});

			}
			if ("lucee.loader.servlet.javax.HttpServletRequestJakarta".equals(obj.getClass().getName())) {
				CFMLEngine eng = CFMLEngineFactory.getInstance();
				return eng.getClassUtil().callMethod(obj, eng.getCastUtil().toKey("getJavaxInstance"), new Object[] {});

			}
		}
		return new org.lucee.extension.axis.jakarta.HttpServletRequestJavax(jakRequest);
	}

	public static Object toJavaxHttpServletResponse(jakarta.servlet.http.HttpServletResponse jakResponse) throws PageException {
		if (RESOLVE_WRAPPER) {
			Object obj = jakResponse;

			while ("lucee.runtime.net.http.HttpServletResponseWrap".equals(obj.getClass().getName())) {
				CFMLEngine eng = CFMLEngineFactory.getInstance();
				obj = eng.getClassUtil().callMethod(obj, eng.getCastUtil().toKey("getResponse"), new Object[] {});

			}

			if ("lucee.loader.servlet.javax.HttpServletResponseJakarta".equals(obj.getClass().getName())) {
				CFMLEngine eng = CFMLEngineFactory.getInstance();
				return eng.getClassUtil().callMethod(obj, eng.getCastUtil().toKey("getJavaxInstance"), new Object[] {});

			}
		}
		return new org.lucee.extension.axis.jakarta.HttpServletResponseJavax(jakResponse);
	}

	public static Object toServletContext(jakarta.servlet.ServletContext context, Object objRequest) throws PageException {
		System.out.println(context.getClass().getName());

		if (objRequest instanceof javax.servlet.http.HttpServletRequest) {
			javax.servlet.http.HttpServletRequest jakRequest = (javax.servlet.http.HttpServletRequest) objRequest;
			return jakRequest.getServletContext();
		}

		if (RESOLVE_WRAPPER) {
			if ("lucee.loader.servlet.javax.ServletContextJakarta".equals(context.getClass().getName())) {
				CFMLEngine eng = CFMLEngineFactory.getInstance();
				return eng.getClassUtil().callMethod(context, eng.getCastUtil().toKey("getJavaxContext"), new Object[] {});

			}
		}
		return new org.lucee.extension.axis.jakarta.ServletContextJavax(context);
	}

	public static AxisHttpSession createAxisHttpSession(Object jaxRequest) {
		return new AxisHttpSession((javax.servlet.http.HttpServletRequest) jaxRequest);
	}

}
