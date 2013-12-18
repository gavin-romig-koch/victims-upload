package com.redhat.cloudconsole.api.persistence.entity;

public class Log {
	private int lineNo;
	private String line;
	
	public int getLineNo() {
		return lineNo;
	}
	public void setLineNo(int lineNo) {
		this.lineNo = lineNo;
	}
	public String getLine() {
		return line;
	}
	public void setLine(String line) {
		this.line = line;
	}
}