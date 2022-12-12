package com.ib.omb.ws.doc;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 * Изход на услугата за деловодна регистрация
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RegisterDocResponseType" //
	, propOrder = { "docNumber", "docDate", "docTypeCode", "docTypeText", "docVidCode", "docVidText", "registraturaId", "registraturaText", "otnosno" })
public class RegisterDocResponseType implements Serializable {

	/**  */
	private static final long serialVersionUID = 480593610110427921L;

	@XmlElement(name = "DocNumber")
	private String docNumber;

	@XmlElement(name = "DocDate")
	@XmlSchemaType(name = "dateTime")
	private XMLGregorianCalendar docDate;

	@XmlElement(name = "DocTypeCode")
	private Integer docTypeCode;

	@XmlElement(name = "DocTypeText")
	private String docTypeText;

	@XmlElement(name = "DocVidCode")
	private Integer docVidCode;

	@XmlElement(name = "DocVidText")
	private String docVidText;

	@XmlElement(name = "RegistraturaId")
	private Integer registraturaId;

	@XmlElement(name = "RegistraturaText")
	private String registraturaText;

	@XmlElement(name = "Otnosno")
	private String otnosno;

	/** */
	public RegisterDocResponseType() {
		super();
	}

	/** @return the docDate */
	public XMLGregorianCalendar getDocDate() {
		return this.docDate;
	}

	/** @return the docNumber */
	public String getDocNumber() {
		return this.docNumber;
	}

	/** @return the docTypeCode */
	public Integer getDocTypeCode() {
		return this.docTypeCode;
	}

	/** @return the docTypeText */
	public String getDocTypeText() {
		return this.docTypeText;
	}

	/** @return the docVidCode */
	public Integer getDocVidCode() {
		return this.docVidCode;
	}

	/** @return the docVidText */
	public String getDocVidText() {
		return this.docVidText;
	}

	/** @return the otnosno */
	public String getOtnosno() {
		return this.otnosno;
	}

	/** @return the registraturaId */
	public Integer getRegistraturaId() {
		return this.registraturaId;
	}

	/** @return the registraturaText */
	public String getRegistraturaText() {
		return this.registraturaText;
	}

	/** @param docDate the docDate to set */
	public void setDocDate(XMLGregorianCalendar docDate) {
		this.docDate = docDate;
	}

	/** @param docNumber the docNumber to set */
	public void setDocNumber(String docNumber) {
		this.docNumber = docNumber;
	}

	/** @param docTypeCode the docTypeCode to set */
	public void setDocTypeCode(Integer docTypeCode) {
		this.docTypeCode = docTypeCode;
	}

	/** @param docTypeText the docTypeText to set */
	public void setDocTypeText(String docTypeText) {
		this.docTypeText = docTypeText;
	}

	/** @param docVidCode the docVidCode to set */
	public void setDocVidCode(Integer docVidCode) {
		this.docVidCode = docVidCode;
	}

	/** @param docVidText the docVidText to set */
	public void setDocVidText(String docVidText) {
		this.docVidText = docVidText;
	}

	/** @param otnosno the otnosno to set */
	public void setOtnosno(String otnosno) {
		this.otnosno = otnosno;
	}

	/** @param registraturaId the registraturaId to set */
	public void setRegistraturaId(Integer registraturaId) {
		this.registraturaId = registraturaId;
	}

	/** @param registraturaText the registraturaText to set */
	public void setRegistraturaText(String registraturaText) {
		this.registraturaText = registraturaText;
	}
}