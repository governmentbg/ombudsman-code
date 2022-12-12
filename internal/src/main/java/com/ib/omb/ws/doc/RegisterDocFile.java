package com.ib.omb.ws.doc;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Файл на документ
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RegisterDocFile" //
	, propOrder = { "filename", "contentType", "binaryContent", "signed", "personalData", "official" })
public class RegisterDocFile implements Serializable {

	/**  */
	private static final long serialVersionUID = -3721846210058113058L;

	@XmlElement(name = "Filename")
	private String filename;

	@XmlElement(name = "ContentType")
	private String contentType;

	@XmlElement(name = "BinaryContent")
	private byte[] binaryContent;

	@XmlElement(name = "Signed")
	private Boolean signed;

	@XmlElement(name = "PersonalData")
	private Boolean personalData;

	@XmlElement(name = "Official")
	private Boolean official;

	/** */
	public RegisterDocFile() {
		super();
	}

	/** @return the binaryContent */
	public byte[] getBinaryContent() {
		return this.binaryContent;
	}

	/** @return the contentType */
	public String getContentType() {
		return this.contentType;
	}

	/** @return the filename */
	public String getFilename() {
		return this.filename;
	}

	/** @return the official */
	public Boolean getOfficial() {
		return this.official;
	}

	/** @return the personalData */
	public Boolean getPersonalData() {
		return this.personalData;
	}

	/** @return the signed */
	public Boolean getSigned() {
		return this.signed;
	}

	/** @param binaryContent the binaryContent to set */
	public void setBinaryContent(byte[] binaryContent) {
		this.binaryContent = binaryContent;
	}

	/** @param contentType the contentType to set */
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	/** @param filename the filename to set */
	public void setFilename(String filename) {
		this.filename = filename;
	}

	/** @param official the official to set */
	public void setOfficial(Boolean official) {
		this.official = official;
	}

	/** @param personalData the personalData to set */
	public void setPersonalData(Boolean personalData) {
		this.personalData = personalData;
	}

	/** @param signed the signed to set */
	public void setSigned(Boolean signed) {
		this.signed = signed;
	}
}