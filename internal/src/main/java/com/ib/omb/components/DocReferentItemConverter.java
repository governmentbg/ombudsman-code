package com.ib.omb.components;
import java.util.Date;
import java.util.StringTokenizer;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.omb.db.dto.DocReferent;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.utils.DateUtils;

@FacesConverter("docReferentItemConverter")
public class DocReferentItemConverter implements Converter<Object> {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(DocReferentItemConverter.class);
	
	@Override
	public Object getAsObject(FacesContext context, UIComponent component, String value) {
		if(value!=null){
		//	System.out.println("getAsObject-> "+value);
			StringTokenizer drItem = new StringTokenizer(value, "@@");
			
			DocReferent dr = new DocReferent();
			
			String codeRef = drItem.nextToken();
			dr.setCodeRef(Integer.parseInt(codeRef)); 
			
			String tekst = drItem.nextToken();
			dr.setTekst(tekst);	
			
			if(drItem.countTokens() > 2) {// изборът идва от модалния...
				String id = drItem.nextToken();
				if(id.equals("null")) {
					dr.setId(null);
				}else {
					dr.setId(Integer.parseInt(id));
				}
				
				String docId = drItem.nextToken();
				if(docId.equals("null")) {
					dr.setDocId(null);
				}else {
					dr.setDocId(Integer.parseInt(docId));
				}
				
				String roleRef = drItem.nextToken();
				if(roleRef.equals("null")) {
					dr.setRoleRef(null);
				}else {
					dr.setRoleRef(Integer.parseInt(roleRef));
				}
				
				String agree = drItem.nextToken();
				if(agree.equals("null")) {
					dr.setEndOpinion(null);
				}else {
					dr.setEndOpinion(Integer.parseInt(agree));
				}
				
				String comments = drItem.nextToken();
				if(comments.equals("null")) {
					dr.setComments(null);	
				}else {
					dr.setComments(comments);
				}
				
				String eventDate = drItem.nextToken();
				if(eventDate.equals("null")) {
					dr.setEventDate(null);
				}else {
					dr.setEventDate(DateUtils.parseFull(eventDate));
				}
				
				String userReg = drItem.nextToken();
				if(userReg.equals("null")) {
					dr.setUserReg(null);	
				}else {
					dr.setUserReg(Integer.parseInt(userReg));	
				}
				
				String dateReg = drItem.nextToken();
				if(dateReg.equals("null")) {
					dr.setDateReg(new Date());
				}else {
					dr.setDateReg(DateUtils.parseFull(dateReg));
				}
			}
			return dr;
		}
		return null;
	}

	@Override
	public String getAsString(FacesContext context, UIComponent component, Object object) {
		if(object != null) {			
			if(object instanceof SystemClassif ){
				SystemClassif scItem = (SystemClassif) object;
				
				LOGGER.debug("getAsString->{}", scItem.getCode());
				
	            return scItem.getCode()+"@@"+scItem.getTekst(); 
	            
			} else if(object instanceof DocReferent ){
				DocReferent drItem = (DocReferent) object;
				
			    String evDate = null;
			    if(drItem.getEventDate() != null){
			    	evDate = DateUtils.printDateFull(drItem.getEventDate());
			    }
			    String regDate = null;
			    if(drItem.getDateReg() != null){
			    	regDate =  DateUtils.printDateFull(drItem.getDateReg());
			    }

			    LOGGER.debug("getAsString->{}", drItem.getCodeRef());
			    
	            return 		drItem.getCodeRef()+"@@"+drItem.getTekst()+"@@"+drItem.getId()+"@@"+drItem.getDocId()+"@@"+
	            					  drItem.getRoleRef()+"@@"+drItem.getEndOpinion()+"@@"+
	            					  (drItem.getComments()!=null&&drItem.getComments().length()==0?null:drItem.getComments())+"@@"+
	            					  evDate+"@@"+drItem.getUserReg()+"@@"+regDate; 
	            
			} else if(object instanceof String ){
				return (String) object;
			} else {
				return null;
			}
        }
        else {
            return null;
        }
	}
}