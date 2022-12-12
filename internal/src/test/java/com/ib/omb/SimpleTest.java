package com.ib.omb;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;

import javax.xml.bind.JAXBException;

import com.ib.omb.db.dto.Doc;
import com.ib.omb.db.dto.Referent;
import com.ib.omb.search.DocSearch;
import com.ib.omb.system.UserData;
import com.ib.system.utils.JAXBHelper;


public class SimpleTest {

	public static void main(String[] args) {
		
		
		try {
			DocSearch search = new DocSearch();
			search.setRnDoc("alabala");
			
			
			ArrayList<Integer> vids = new ArrayList<Integer>();
			vids.add(1);
			vids.add(2);
			search.setDocVidList(vids);
			search.buildQueryDocList(new UserData(1, "", ""), true);
			
			String xml = JAXBHelper.objectToXml(search, true);
			System.out.println(xml);
			
			
		} catch (Exception e) {			
			e.printStackTrace();
		}
		
		
	}
		
		
		
		
	
	

}


