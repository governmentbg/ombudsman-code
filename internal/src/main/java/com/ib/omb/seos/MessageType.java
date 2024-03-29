//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2012.02.14 at 10:15:38 AM EET 
//


package com.ib.omb.seos;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for MessageType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="MessageType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="MSG_DocumentRegistrationRequest"/>
 *     &lt;enumeration value="MSG_DocumentStatusResponse"/>
 *     &lt;enumeration value="MSG_DocumentStatusRequest"/>
 *     &lt;enumeration value="MSG_Error"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "MessageType")
@XmlEnum
public enum MessageType {

    @XmlEnumValue("MSG_DocumentRegistrationRequest")
    MSG_DOCUMENT_REGISTRATION_REQUEST("MSG_DocumentRegistrationRequest"),
    @XmlEnumValue("MSG_DocumentStatusResponse")
    MSG_DOCUMENT_STATUS_RESPONSE("MSG_DocumentStatusResponse"),
    @XmlEnumValue("MSG_DocumentStatusRequest")
    MSG_DOCUMENT_STATUS_REQUEST("MSG_DocumentStatusRequest"),
    @XmlEnumValue("MSG_Error")
    MSG_ERROR("MSG_Error");
    private final String value;

    MessageType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static MessageType fromValue(String v) {
        for (MessageType c: MessageType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
