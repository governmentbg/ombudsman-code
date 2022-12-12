package com.ib.omb.rest.mobileJalbi;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonSyntaxException;
import com.ib.system.db.dto.Files;
import com.ib.system.utils.ValidationUtils;

/**
 * Класът се използва от рест услугите за мобилните приложения и уеб сайта.
 * 
 * @author n.kanev
 *
 */
public class RestUtils {
	
	private static final Gson gson = new Gson();

	public static Response returnError(Status errorCode, String message) {
		String errorJson = gson.toJson(Collections.singletonMap("error", message));
		return Response.serverError().entity(errorJson).build();
	}
	
	public static Response returnError(Status errorCode, List<String> message) {
		String errorJson = gson.toJson(Collections.singletonMap("error", message));
		return Response.serverError().entity(errorJson).build();
	}
	
	public static Response returnSuccess(String json) {
		return Response.ok().entity(json).build();
	}
	
	public static Response returnEmptyJson() {
		return Response.ok(gson.toJson(new JsonArray())).build();
	}
	
	public static void addCacheHeader(Response response, long seconds) {
		response.getHeaders().add("cache-control", "max-age=" + seconds);
	}
	
	public static String getMultipartFormValue(MultipartFormDataInput form, String key) throws IOException {
		
		Map<String, List<InputPart>> inputMap = form.getFormDataMap();
		List<InputPart> parts = inputMap.get(key);
		String value = null;
		
		if(parts != null) {
			InputPart part = parts.get(0);
			value = part.getBodyAsString();
		}		
		
		return value;
	}
	
	public static List<Files> getMultipartFormFiles(MultipartFormDataInput form, String key) throws IOException {
		Map<String, List<InputPart>> inputMap = form.getFormDataMap();
		List<InputPart> files = inputMap.get(key);
		List<Files> result = new ArrayList<>();
		Files multipartObject = null;
		
		if(files != null) {
			for(InputPart file : files) {
				multipartObject = new Files();
				
				String mime = file.getMediaType().getType() + "/" + file.getMediaType().getSubtype();
				InputStream inputStream = file.getBody(InputStream.class, null);
				byte[] value = IOUtils.toByteArray(inputStream);
				
				String contentDisposition = file.getHeaders().getFirst("Content-Disposition");
				String filenameParam = "filename";
				if(contentDisposition.indexOf("filename*") >= 0) filenameParam = "filename*";
				
				for(String fragment : contentDisposition.split(";")) {
					if(fragment.trim().startsWith(filenameParam)) {
						String filename = fragment.split("=")[1].trim();
						
						if(filenameParam.contains("*")) {
							filename = filename.split("\'\'")[1].trim();
							filename = URLDecoder.decode(filename, StandardCharsets.UTF_8.name());
						}
						
						filename = filename.replaceAll("\"","");
						multipartObject.setFilename(filename);
					}
				}
				
				multipartObject.setContentType(mime);
				multipartObject.setContent(value);
				result.add(multipartObject);
			}
		}		
		
		return result;
	}
	
	public static class MultipartFormValue {
		private String name;
		private String value;
		private String mime;
		
		public MultipartFormValue() {
			
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getValue() {
			return value;
		}
		public void setValue(String value) {
			this.value = value;
		}
		public String getMime() {
			return mime;
		}
		public void setMime(String mime) {
			this.mime = mime;
		}
		@Override
		public String toString() {
			return "MultipartFormValue [name=" + name + ", value=" + value + ", mime=" + mime + "]";
		}
	}
	
	public static Response validateRequest(String request, String parameterName, int validationType) throws ClassCastException {
		Gson gson = new Gson();
		Map<String, Object> map = gson.fromJson(request, HashMap.class);
		
		String value = (String) map.get(parameterName);
		
		if(value == null || value.trim().isEmpty()) {
			return RestUtils.returnEmptyJson();
		}
		
		boolean isValid = ValidationUtils.validateText(value.trim(), validationType);
		
		return RestUtils.returnSuccess(gson.toJson(Collections.singletonMap("valid", isValid)));
				
	}
	
	public static boolean validateParameter(String value, int validationType) {
		return ValidationUtils.validateText(value.trim(), validationType);
	}
}
