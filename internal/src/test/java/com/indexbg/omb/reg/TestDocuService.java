package com.indexbg.omb.reg;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;

import com.ib.system.utils.JAXBHelper;

/** */
public class TestDocuService {

	/** @param args */
	public static void main(String[] args) {
		try {
			String username = "zxc";
			String password = "123456";
			URL wsdl = new URL("http://192.168.0.105:8080/DocuWork/DocuService?wsdl"); // !!! да се смени при тест

			DocuService service = new DocuService(wsdl);

			RegisterDocRequestType request = new RegisterDocRequestType();

			request.setCorrespEikEgn("6401027465");
			request.setExternalCode("ЖАЛ");
			request.setAnnotation("анотация");

			RegisterDocFile docFile = new RegisterDocFile();

			String fileName = System.getProperty("user.dir") + "\\src\\test\\java\\com\\indexbg\\docu\\reg\\TestDocuService.java";
			File file = new File(fileName);

			docFile.setFilename(file.getName());
			docFile.setContentType(""); // има ли метод да го даде
			docFile.setBinaryContent(Files.readAllBytes(file.toPath()));
			docFile.setOfficial(true);
			docFile.setPersonalData(true);
			request.getRegisterDocFile().add(docFile);

			RegisterDocResponseType response = service.getDocuServicePort().registerDocument(request, username, password);

			System.out.println(JAXBHelper.objectToXml(response, true));

		} catch (DocuServiceFault_Exception e) {
			System.err.println("Message=" + e.getMessage());
			System.err.println("ErrorDetail=" + e.getFaultInfo().getErrorDetail());

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
