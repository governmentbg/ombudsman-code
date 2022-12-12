package com.ib.omb.ws.doc;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 * Вход на услугата за деловодна регистрация
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RegisterDocRequestType" //
	, propOrder = { "externalCode", "annotation", "tehDocNumber", "tehDocDate" //
		, "correspCountry", "correspEikEgn", "correspNames", "correspEmail", "correspPhone", "correspAddress" //
		, "registerDocFiles" })
public class RegisterDocRequestType implements Serializable {

	/**  */
	private static final long serialVersionUID = -6274660313838865242L;

	@XmlElement(name = "ExternalCode")
	private String externalCode;

	@XmlElement(name = "Annotation")
	private String annotation;

	@XmlElement(name = "TehDocNumber")
	private String tehDocNumber;

	@XmlElement(name = "TehDocDate")
	@XmlSchemaType(name = "dateTime")
	private XMLGregorianCalendar tehDocDate;

	@XmlElement(name = "CorrespCountry")
	private String correspCountry; // двубуквен код и ако няма ще се смята че е BG

	@XmlElement(name = "CorrespEikEgn")
	private String correspEikEgn;

	@XmlElement(name = "CorrespNames")
	private String correspNames;

	@XmlElement(name = "CorrespEmail")
	private String correspEmail;

	@XmlElement(name = "CorrespPhone")
	private String correspPhone;

	@XmlElement(name = "CorrespAddress")
	private String correspAddress;

	@XmlElement(name = "RegisterDocFile")
	private List<RegisterDocFile> registerDocFiles;

	/** */
	public RegisterDocRequestType() {
		super();
	}

	/** @return the annotation */
	public String getAnnotation() {
		return this.annotation;
	}

	/** @return the correspAddress */
	public String getCorrespAddress() {
		return this.correspAddress;
	}

	/** @return the correspCountry */
	public String getCorrespCountry() {
		return this.correspCountry;
	}

	/** @return the correspEikEgn */
	public String getCorrespEikEgn() {
		return this.correspEikEgn;
	}

	/** @return the correspEmail */
	public String getCorrespEmail() {
		return this.correspEmail;
	}

	/** @return the correspNames */
	public String getCorrespNames() {
		return this.correspNames;
	}

	/** @return the correspPhone */
	public String getCorrespPhone() {
		return this.correspPhone;
	}

	/** @return the externalCode */
	public String getExternalCode() {
		return this.externalCode;
	}

	/** @return the registerDocFiles */
	public List<RegisterDocFile> getRegisterDocFiles() {
		if (this.registerDocFiles == null) {
			this.registerDocFiles = new ArrayList<>();
		}
		return this.registerDocFiles;
	}

	/** @return the tehDocDate */
	public XMLGregorianCalendar getTehDocDate() {
		return this.tehDocDate;
	}

	/** @return the tehDocNumber */
	public String getTehDocNumber() {
		return this.tehDocNumber;
	}

	/** @param annotation the annotation to set */
	public void setAnnotation(String annotation) {
		this.annotation = annotation;
	}

	/** @param correspAddress the correspAddress to set */
	public void setCorrespAddress(String correspAddress) {
		this.correspAddress = correspAddress;
	}

	/** @param correspCountry the correspCountry to set */
	public void setCorrespCountry(String correspCountry) {
		this.correspCountry = correspCountry;
	}

	/** @param correspEikEgn the correspEikEgn to set */
	public void setCorrespEikEgn(String correspEikEgn) {
		this.correspEikEgn = correspEikEgn;
	}

	/** @param correspEmail the correspEmail to set */
	public void setCorrespEmail(String correspEmail) {
		this.correspEmail = correspEmail;
	}

	/** @param correspNames the correspNames to set */
	public void setCorrespNames(String correspNames) {
		this.correspNames = correspNames;
	}

	/** @param correspPhone the correspPhone to set */
	public void setCorrespPhone(String correspPhone) {
		this.correspPhone = correspPhone;
	}

	/** @param externalCode the externalCode to set */
	public void setExternalCode(String externalCode) {
		this.externalCode = externalCode;
	}

	/** @param registerDocFiles the registerDocFiles to set */
	public void setRegisterDocFiles(List<RegisterDocFile> registerDocFiles) {
		this.registerDocFiles = registerDocFiles;
	}

	/** @param tehDocDate the tehDocDate to set */
	public void setTehDocDate(XMLGregorianCalendar tehDocDate) {
		this.tehDocDate = tehDocDate;
	}

	/** @param tehDocNumber the tehDocNumber to set */
	public void setTehDocNumber(String tehDocNumber) {
		this.tehDocNumber = tehDocNumber;
	}
}