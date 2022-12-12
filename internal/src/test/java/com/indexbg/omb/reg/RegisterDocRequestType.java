
package com.indexbg.omb.reg;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;


/**
 * <p>Java class for RegisterDocRequestType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="RegisterDocRequestType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="ExternalCode" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="Annotation" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="TehDocNumber" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="TehDocDate" type="{http://www.w3.org/2001/XMLSchema}dateTime" minOccurs="0"/&gt;
 *         &lt;element name="CorrespCountry" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="CorrespEikEgn" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="CorrespNames" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="CorrespEmail" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="CorrespPhone" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="CorrespAddress" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="RegisterDocFile" type="{http://reg.docu.indexbg.com/}RegisterDocFile" maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RegisterDocRequestType", propOrder = {
    "externalCode",
    "annotation",
    "tehDocNumber",
    "tehDocDate",
    "correspCountry",
    "correspEikEgn",
    "correspNames",
    "correspEmail",
    "correspPhone",
    "correspAddress",
    "registerDocFile"
})
public class RegisterDocRequestType {

    @XmlElement(name = "ExternalCode")
    protected String externalCode;
    @XmlElement(name = "Annotation")
    protected String annotation;
    @XmlElement(name = "TehDocNumber")
    protected String tehDocNumber;
    @XmlElement(name = "TehDocDate")
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar tehDocDate;
    @XmlElement(name = "CorrespCountry")
    protected String correspCountry;
    @XmlElement(name = "CorrespEikEgn")
    protected String correspEikEgn;
    @XmlElement(name = "CorrespNames")
    protected String correspNames;
    @XmlElement(name = "CorrespEmail")
    protected String correspEmail;
    @XmlElement(name = "CorrespPhone")
    protected String correspPhone;
    @XmlElement(name = "CorrespAddress")
    protected String correspAddress;
    @XmlElement(name = "RegisterDocFile")
    protected List<RegisterDocFile> registerDocFile;

    /**
     * Gets the value of the externalCode property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getExternalCode() {
        return externalCode;
    }

    /**
     * Sets the value of the externalCode property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setExternalCode(String value) {
        this.externalCode = value;
    }

    /**
     * Gets the value of the annotation property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAnnotation() {
        return annotation;
    }

    /**
     * Sets the value of the annotation property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAnnotation(String value) {
        this.annotation = value;
    }

    /**
     * Gets the value of the tehDocNumber property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTehDocNumber() {
        return tehDocNumber;
    }

    /**
     * Sets the value of the tehDocNumber property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTehDocNumber(String value) {
        this.tehDocNumber = value;
    }

    /**
     * Gets the value of the tehDocDate property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getTehDocDate() {
        return tehDocDate;
    }

    /**
     * Sets the value of the tehDocDate property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setTehDocDate(XMLGregorianCalendar value) {
        this.tehDocDate = value;
    }

    /**
     * Gets the value of the correspCountry property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCorrespCountry() {
        return correspCountry;
    }

    /**
     * Sets the value of the correspCountry property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCorrespCountry(String value) {
        this.correspCountry = value;
    }

    /**
     * Gets the value of the correspEikEgn property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCorrespEikEgn() {
        return correspEikEgn;
    }

    /**
     * Sets the value of the correspEikEgn property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCorrespEikEgn(String value) {
        this.correspEikEgn = value;
    }

    /**
     * Gets the value of the correspNames property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCorrespNames() {
        return correspNames;
    }

    /**
     * Sets the value of the correspNames property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCorrespNames(String value) {
        this.correspNames = value;
    }

    /**
     * Gets the value of the correspEmail property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCorrespEmail() {
        return correspEmail;
    }

    /**
     * Sets the value of the correspEmail property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCorrespEmail(String value) {
        this.correspEmail = value;
    }

    /**
     * Gets the value of the correspPhone property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCorrespPhone() {
        return correspPhone;
    }

    /**
     * Sets the value of the correspPhone property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCorrespPhone(String value) {
        this.correspPhone = value;
    }

    /**
     * Gets the value of the correspAddress property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCorrespAddress() {
        return correspAddress;
    }

    /**
     * Sets the value of the correspAddress property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCorrespAddress(String value) {
        this.correspAddress = value;
    }

    /**
     * Gets the value of the registerDocFile property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the registerDocFile property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getRegisterDocFile().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link RegisterDocFile }
     * 
     * 
     */
    public List<RegisterDocFile> getRegisterDocFile() {
        if (registerDocFile == null) {
            registerDocFile = new ArrayList<RegisterDocFile>();
        }
        return this.registerDocFile;
    }

}
