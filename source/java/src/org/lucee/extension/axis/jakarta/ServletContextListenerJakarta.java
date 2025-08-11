package org.lucee.extension.axis.jakarta;

import java.util.EventListener;

import javax.servlet.ServletContext;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

public class ServletContextListenerJakarta implements ServletContextListener {

	private ServletContext sc;
	private javax.servlet.ServletContextListener listener;

	public static <T extends EventListener> jakarta.servlet.ServletContextListener getinstance(javax.servlet.ServletContext sc, T t) {
		if (t instanceof jakarta.servlet.ServletContextListener) return (jakarta.servlet.ServletContextListener) t;

		return new ServletContextListenerJakarta(sc, (javax.servlet.ServletContextListener) t);
	}

	public ServletContextListenerJakarta(javax.servlet.ServletContext sc, javax.servlet.ServletContextListener listener) {
		this.sc = sc;
		this.listener = listener;
	}

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		listener.contextInitialized(new ServletContextEventImpl(sc, sce));
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		listener.contextDestroyed(new ServletContextEventImpl(sc, sce));
	}

	private static class ServletContextEventImpl extends javax.servlet.ServletContextEvent {

		private static final long serialVersionUID = -6301294044223510600L;
		private ServletContext src;
		private ServletContextEvent sce;

		public ServletContextEventImpl(ServletContext source, ServletContextEvent sce) {
			super(source);
			this.src = source;
			this.sce = sce;
		}

		@Override
		public ServletContext getServletContext() {
			return this.src;
		}

		@Override
		public Object getSource() {
			return src;
		}

		@Override
		public String toString() {
			return sce.toString();
		}

	}

}
