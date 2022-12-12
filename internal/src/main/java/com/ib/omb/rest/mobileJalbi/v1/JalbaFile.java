package com.ib.omb.rest.mobileJalbi.v1;

public class JalbaFile {
	private String filename;
	private String mime;
	private String content;

	private JalbaFile() { }

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public String getMime() {
		return mime;
	}

	public void setMime(String mime) {
		this.mime = mime;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}
	
}
