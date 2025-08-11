package org.lucee.extension.axis.jakarta;

public interface Jakarta {
	public Object getJakartaInstance();

	public static jakarta.servlet.http.Cookie toCookie(javax.servlet.http.Cookie cookie) {

		if (cookie instanceof CookieJavax) {
			return (jakarta.servlet.http.Cookie) ((CookieJavax) cookie).getJakartaInstance();
		}

		jakarta.servlet.http.Cookie jakartaCookie = new jakarta.servlet.http.Cookie(cookie.getName(), cookie.getValue());
		jakartaCookie.setMaxAge(cookie.getMaxAge());
		jakartaCookie.setSecure(cookie.getSecure());
		jakartaCookie.setPath(cookie.getPath());
		jakartaCookie.setHttpOnly(cookie.isHttpOnly());
		if (cookie.getDomain() != null) {
			jakartaCookie.setDomain(cookie.getDomain());
		}
		return jakartaCookie;
	}
}
