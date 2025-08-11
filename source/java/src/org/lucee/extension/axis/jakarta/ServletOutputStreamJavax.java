package org.lucee.extension.axis.jakarta;

import java.io.IOException;

import javax.servlet.ServletOutputStream;

public class ServletOutputStreamJavax extends ServletOutputStream {

	private jakarta.servlet.ServletOutputStream outputStream;

	public ServletOutputStreamJavax(jakarta.servlet.ServletOutputStream outputStream) {
		if (outputStream == null) throw new NullPointerException();
		this.outputStream = outputStream;
	}

	@Override
	public void write(int b) throws IOException {
		outputStream.write(b);
	}

	public boolean isReady() {
		return outputStream.isReady();
	}

	/*
	 * public void setWriteListener(javax.servlet.WriteListener writeListener) {
	 * outputStream.setWriteListener(new jakarta.servlet.WriteListener() {
	 * 
	 * @Override public void onWritePossible() throws IOException { writeListener.onWritePossible(); }
	 * 
	 * @Override public void onError(Throwable t) { writeListener.onError(t); } }); }
	 */
}