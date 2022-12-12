package com.ib.omb.components;



import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.ib.indexui.db.dto.UniversalReport;
import com.ib.indexui.report.uni.SprAttributes;
import com.ib.indexui.report.uni.SprObject;
import com.ib.indexui.report.uni.SprValues;
import com.ib.omb.system.SystemData;
import com.ib.system.ObjectsDifference;
import com.ib.system.SysConstants;
import com.ib.system.db.JPA;
import com.ib.system.utils.JAXBHelper;

public class TestUserXML {
	
	
	
	
	public List<ObjectsDifference> testUserXML(){
		
		List<ObjectsDifference> diffs = new ArrayList<ObjectsDifference>();
		
		try {
			SystemData sd = new SystemData();
			Integer lang = SysConstants.CODE_DEFAULT_LANG;
			
			UniversalReport ur = (UniversalReport) JPA.getUtil().getEntityManager().createQuery("from UniversalReport where id = 601").getSingleResult();
			System.out.println(ur.getContentXml());
			
			SprObject spr = JAXBHelper.xmlToObject(SprObject.class, ur.getContentXml());
			
			
//			DocSearch dsEmpty = new DocSearch();		
//			
			
			diffs = convertSprObjectToDifferences2(spr,  sd, new Date()) ;
			for (ObjectsDifference diff : diffs) {
				System.out.println(diff.getFieldName());
			}
			
			
			
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();		
			
		}
		
		return diffs;
		
	}

	private List<ObjectsDifference> convertSprObjectToDifferences(SprObject spr, String path, SystemData sd, Date dat) {
		
		path = path + spr.getNameObject() + "\\";
		
		List<ObjectsDifference> diffs = new ArrayList<ObjectsDifference>();
		for (SprAttributes attr : spr.getAttributes()) {
			
			if (attr.getNameAttr() == null) {
				if (attr.getObject() != null) {
					//System.out.println(attr.getObject().getNameObject());
					diffs.addAll(convertSprObjectToDifferences(attr.getObject(), path, sd, dat));
				}
			}else {
				//System.out.println(attr.getNameAttr());
				ObjectsDifference diff = new ObjectsDifference();
				diff.setFieldName(path + attr.getNameAttr());
				if (attr.getValues().size() == 1) {
					diff.setOldVal(attr.getValues().get(0).toString(sd,dat));					
				}else {
					for (SprValues val : attr.getValues()) {
						ObjectsDifference diffVal = new ObjectsDifference();
						diffVal.setOldVal(val.toString(sd, dat));
						diff.getCoplexDif().add(diffVal);
					}
				}
				diffs.add(diff);
			}
			
			
		}
		
		
		
		return diffs;
	}
	
	private List<ObjectsDifference> convertSprObjectToDifferences2(SprObject spr, SystemData sd, Date dat) {
		
		
		
		List<ObjectsDifference> diffs = new ArrayList<ObjectsDifference>();
		for (SprAttributes attr : spr.getAttributes()) {
			
			if (attr.getNameAttr() == null) {				
				if (attr.getObject() != null) {
					
					ObjectsDifference odiff = new ObjectsDifference();
					odiff.setFieldName(attr.getObject().getNameObject());
					odiff.setCoplexDif(convertSprObjectToDifferences2(attr.getObject(), sd, dat));
					diffs.add(odiff);
				}
			}else {
				//System.out.println(attr.getNameAttr());
				ObjectsDifference diff = new ObjectsDifference();
				diff.setFieldName(attr.getNameAttr());
				if (attr.getValues().size() == 1) {
					diff.setOldVal(attr.getValues().get(0).toString(sd,dat));					
				}else {
					for (SprValues val : attr.getValues()) {
						ObjectsDifference diffVal = new ObjectsDifference();
						diffVal.setOldVal(val.toString(sd, dat));
						diff.getCoplexDif().add(diffVal);
					}
				}
				diffs.add(diff);
			}
			
			
		}
		
		
		
		return diffs;
	}
	
	

}
