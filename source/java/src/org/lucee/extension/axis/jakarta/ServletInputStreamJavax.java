package org.lucee.extension.axis.jakarta;

import java.io.IOException;

import javax.servlet.ServletInputStream;

public class ServletInputStreamJavax extends ServletInputStream {

	private jakarta.servlet.ServletInputStream inputStream;

	public ServletInputStreamJavax(jakarta.servlet.ServletInputStream inputStream) {
		if (inputStream == null) throw new NullPointerException();
		this.inputStream = inputStream;
	}

	@Override
	public int read() throws IOException {
		return inputStream.read();
	}

	public boolean isFinished() {
		return inputStream.isFinished();
	}

	public boolean isReady() {
		return inputStream.isReady();
	}

	/*
	 * @Override public void setReadListener(javax.servlet.ReadListener readListener) {
	 * inputStream.setReadListener(new jakarta.servlet.ReadListener() {
	 * 
	 * @Override public void onDataAvailable() throws IOException { readListener.onDataAvailable(); }
	 * 
	 * @Override public void onAllDataRead() throws IOException { readListener.onAllDataRead(); }
	 * 
	 * @Override public void onError(Throwable t) { readListener.onError(t); } }); }
	 */
}