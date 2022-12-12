package com.indexbg.omb.reg;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

/**
 * This class was generated by Apache CXF 3.2.0
 * 2021-11-23T11:27:26.125+02:00
 * Generated source version: 3.2.0
 * 
 */
@WebService(targetNamespace = "http://reg.docu.indexbg.com/", name = "DocuServiceSOAP")
@XmlSeeAlso({ObjectFactory.class})
public interface DocuServiceSOAP {

    @WebMethod(operationName = "RegisterDocument")
    @RequestWrapper(localName = "RegisterDocument", targetNamespace = "http://reg.docu.indexbg.com/", className = "com.indexbg.omb.reg.RegisterDocument")
    @ResponseWrapper(localName = "RegisterDocumentResponse", targetNamespace = "http://reg.docu.indexbg.com/", className = "com.indexbg.omb.reg.RegisterDocumentResponse")
    @WebResult(name = "return", targetNamespace = "")
    public com.indexbg.omb.reg.RegisterDocResponseType registerDocument(
        @WebParam(name = "RegisterDocRequestType", targetNamespace = "")
        com.indexbg.omb.reg.RegisterDocRequestType registerDocRequestType,
        @WebParam(name = "Username", targetNamespace = "")
        java.lang.String username,
        @WebParam(name = "Password", targetNamespace = "")
        java.lang.String password
    ) throws DocuServiceFault_Exception;
}
