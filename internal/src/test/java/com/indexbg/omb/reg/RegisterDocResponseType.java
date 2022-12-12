
package com.indexbg.omb.reg;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;


/**
 * <p>Java class for RegisterDocResponseType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="RegisterDocResponseType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="DocNumber" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="DocDate" type="{http://www.w3.org/2001/XMLSchema}dateTime" minOccurs="0"/&gt;
 *         &lt;element name="DocTypeCode" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/&gt;
 *         &lt;element name="DocTypeText" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="DocVidCode" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/&gt;
 *         &lt;element name="DocVidText" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="RegistraturaId" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/&gt;
 *         &lt;element name="RegistraturaText" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="Otnosno" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RegisterDocResponseType", propOrder = {
    "docNumber",
    "docDate",
    "docTypeCode",
    "docTypeText",
    "docVidCode",
    "docVidText",
    "registraturaId",
    "registraturaText",
    "otnosno"
})
public class RegisterDocResponseType {

    @XmlElement(name = "DocNumber")
    protected String docNumber;
    @XmlElement(name = "DocDate")
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar docDate;
    @XmlElement(name = "DocTypeCode")
    protected Integer docTypeCode;
    @XmlElement(name = "DocTypeText")
    protected String docTypeText;
    @XmlElement(name = "DocVidCode")
    protected Integer docVidCode;
    @XmlElement(name = "DocVidText")
    protected String docVidText;
    @XmlElement(name = "RegistraturaId")
    protected Integer registraturaId;
    @XmlElement(name = "RegistraturaText")
    protected String registraturaText;
    @XmlElement(name = "Otnosno")
    protected String otnosno;

    /**
     * Gets the value of the docNumber property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDocNumber() {
        return docNumber;
    }

    /**
     * Sets the value of the docNumber property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDocNumber(String value) {
        this.docNumber = value;
    }

    /**
     * Gets the value of the docDate property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getDocDate() {
        return docDate;
    }

    /**
     * Sets the value of the docDate property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setDocDate(XMLGregorianCalendar value) {
        this.docDate = value;
    }

    /**
     * Gets the value of the docTypeCode property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getDocTypeCode() {
        return docTypeCode;
    }

    /**
     * Sets the value of the docTypeCode property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setDocTypeCode(Integer value) {
        this.docTypeCode = value;
    }

    /**
     * Gets the value of the docTypeText property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDocTypeText() {
        return docTypeText;
    }

    /**
     * Sets the value of the docTypeText property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDocTypeText(String value) {
        this.docTypeText = value;
    }

    /**
     * Gets the value of the docVidCode property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getDocVidCode() {
        return docVidCode;
    }

    /**
     * Sets the value of the docVidCode property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setDocVidCode(Integer value) {
        this.docVidCode = value;
    }

    /**
     * Gets the value of the docVidText property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDocVidText() {
        return docVidText;
    }

    /**
     * Sets the value of the docVidText property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDocVidText(String value) {
        this.docVidText = value;
    }

    /**
     * Gets the value of the registraturaId property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getRegistraturaId() {
        return registraturaId;
    }

    /**
     * Sets the value of the registraturaId property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setRegistraturaId(Integer value) {
        this.registraturaId = value;
    }

    /**
     * Gets the value of the registraturaText property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRegistraturaText() {
        return registraturaText;
    }

    /**
     * Sets the value of the registraturaText property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRegistraturaText(String value) {
        this.registraturaText = value;
    }

    /**
     * Gets the value of the otnosno property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOtnosno() {
        return otnosno;
    }

    /**
     * Sets the value of the otnosno property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOtnosno(String value) {
        this.otnosno = value;
    }

}
