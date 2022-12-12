package com.ib.omb.ws;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.ws.WebFault;

/**
 * Грешка при изпълнението на услугата
 */
@WebFault
@XmlType(name = "DocuServiceFault", propOrder = { "message", "errorDetail" }, namespace = "http://reg.docu.indexbg.com/")
public class DocuServiceFault extends Exception {

	/**  */
	private static final long serialVersionUID = -1063562318737772190L;

	@XmlElement(name = "ErrorDetail")
	private String errorDetail;

	/**
	 * @param message
	 * @param errorDetail
	 */
	public DocuServiceFault(String message, String errorDetail) {
		super(message);

		this.errorDetail = errorDetail;
	}

	/** @return the errorDetail */
	public String getErrorDetail() {
		return this.errorDetail;
	}

	/** @param errorDetail the errorDetail to set */
	public void setErrorDetail(String errorDetail) {
		this.errorDetail = errorDetail;
	}

	/**  */
	@Override
	public String toString() {
		return "DocuServiceFault [message=" + getMessage() + ", errorDetail=" + this.errorDetail + "]";
	}
}
