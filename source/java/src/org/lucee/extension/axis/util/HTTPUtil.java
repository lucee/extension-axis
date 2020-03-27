package org.lucee.extension.axis.util;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import lucee.loader.util.Util;

public class HTTPUtil {
	public static String getRootPath(ServletContext sc) {
		if (sc == null) throw new RuntimeException("cannot determinate webcontext root, because the ServletContext is null");

		String root = sc.getRealPath("/");

		if (root == null) throw new RuntimeException("cannot determinate webcontext root, the ServletContext from class [" + sc.getClass().getName()
				+ "] is returning null for the method call sc.getRealPath(\"/\"), possibly due to configuration problem.");

		return root;
	}

	public static String getRequestURL(HttpServletRequest req, boolean includeQueryString) {

		StringBuffer sb = req.getRequestURL();
		int maxpos = sb.indexOf("/", 8);

		if (maxpos > -1) {

			if (req.isSecure()) {
				if (sb.substring(maxpos - 4, maxpos).equals(":443")) sb.delete(maxpos - 4, maxpos);
			}
			else {
				if (sb.substring(maxpos - 3, maxpos).equals(":80")) sb.delete(maxpos - 3, maxpos);
			}

			if (includeQueryString && !Util.isEmpty(req.getQueryString())) sb.append('?').append(req.getQueryString());
		}

		return sb.toString();
	}
}
