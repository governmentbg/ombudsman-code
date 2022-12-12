package com.ib.omb.ws;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;

import com.ib.omb.ws.doc.RegisterDocRequestType;
import com.ib.omb.ws.doc.RegisterDocResponseType;

/**
 * Interface defines methods to be exposed through SOAP
 *
 * @author ilukov
 */
//@WebService(name = "DocuServiceSOAP", targetNamespace = "http://reg.docu.indexbg.com/")
public interface DocuService {

	/**
	 * Деловодна регистрация
	 *
	 * @param request
	 * @param username
	 * @param password
	 * @return
	 * @throws DocuServiceFault
	 */
	@WebMethod(operationName = "RegisterDocument")
	RegisterDocResponseType registerDocument( //
		@WebParam(name = "RegisterDocRequestType") RegisterDocRequestType request, //
		@WebParam(name = "Username") String username, @WebParam(name = "Password") String password) //
		throws DocuServiceFault;
}
