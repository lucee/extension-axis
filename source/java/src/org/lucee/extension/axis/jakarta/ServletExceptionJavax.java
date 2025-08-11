package org.lucee.extension.axis.jakarta;

import javax.servlet.ServletException;

public class ServletExceptionJavax extends ServletException implements Jakarta {
	private static final long serialVersionUID = -109825224091435598L;
	private jakarta.servlet.ServletException exp;

	public ServletExceptionJavax(jakarta.servlet.ServletException exp) {
		super(exp);
		if (exp == null) throw new NullPointerException();
		this.exp = exp;
	}

	@Override
	public Object getJakartaInstance() {
		return exp;
	}

}
