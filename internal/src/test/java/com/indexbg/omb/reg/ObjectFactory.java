
package com.indexbg.omb.reg;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the com.indexbg.omb.reg package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _RegisterDocument_QNAME = new QName("http://reg.docu.indexbg.com/", "RegisterDocument");
    private final static QName _RegisterDocumentResponse_QNAME = new QName("http://reg.docu.indexbg.com/", "RegisterDocumentResponse");
    private final static QName _DocuServiceFault_QNAME = new QName("http://reg.docu.indexbg.com/", "DocuServiceFault");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.indexbg.omb.reg
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link RegisterDocument }
     * 
     */
    public RegisterDocument createRegisterDocument() {
        return new RegisterDocument();
    }

    /**
     * Create an instance of {@link RegisterDocumentResponse }
     * 
     */
    public RegisterDocumentResponse createRegisterDocumentResponse() {
        return new RegisterDocumentResponse();
    }

    /**
     * Create an instance of {@link DocuServiceFault }
     * 
     */
    public DocuServiceFault createDocuServiceFault() {
        return new DocuServiceFault();
    }

    /**
     * Create an instance of {@link RegisterDocRequestType }
     * 
     */
    public RegisterDocRequestType createRegisterDocRequestType() {
        return new RegisterDocRequestType();
    }

    /**
     * Create an instance of {@link RegisterDocFile }
     * 
     */
    public RegisterDocFile createRegisterDocFile() {
        return new RegisterDocFile();
    }

    /**
     * Create an instance of {@link RegisterDocResponseType }
     * 
     */
    public RegisterDocResponseType createRegisterDocResponseType() {
        return new RegisterDocResponseType();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RegisterDocument }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://reg.docu.indexbg.com/", name = "RegisterDocument")
    public JAXBElement<RegisterDocument> createRegisterDocument(RegisterDocument value) {
        return new JAXBElement<RegisterDocument>(_RegisterDocument_QNAME, RegisterDocument.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RegisterDocumentResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://reg.docu.indexbg.com/", name = "RegisterDocumentResponse")
    public JAXBElement<RegisterDocumentResponse> createRegisterDocumentResponse(RegisterDocumentResponse value) {
        return new JAXBElement<RegisterDocumentResponse>(_RegisterDocumentResponse_QNAME, RegisterDocumentResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DocuServiceFault }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://reg.docu.indexbg.com/", name = "DocuServiceFault")
    public JAXBElement<DocuServiceFault> createDocuServiceFault(DocuServiceFault value) {
        return new JAXBElement<DocuServiceFault>(_DocuServiceFault_QNAME, DocuServiceFault.class, null, value);
    }

}
