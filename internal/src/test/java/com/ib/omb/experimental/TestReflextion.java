package com.ib.omb.experimental;

import static org.junit.Assert.fail;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.junit.Test;

import com.ib.omb.db.dto.Doc;
import com.ib.omb.db.dto.DocReferent;
import com.ib.omb.experimental.ObjectComparator;
import com.ib.omb.system.SystemData;
import com.ib.system.ObjectsDifference;

public class TestReflextion {

	@Test
	public void test()  {
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
			SimpleDateFormat sdf1 = new SimpleDateFormat("dd.MM.yyyy hh:mm:ss");
			SystemData sd = new SystemData();
			Date dat = new Date();
			
			Doc doc = new Doc();
			doc.setRnDoc("94-00-102");
			doc.setDocDate(dat);
			doc.setDocVid(1);
			doc.setReceiveDate(new Date());
			List<DocReferent> refs = new ArrayList<DocReferent>();
			
			
			DocReferent ref = new DocReferent();
			ref.setId(10);
			ref.setCodeRef(1226);
			ref.setEventDate(sdf.parse("31.12.2020"));			
			refs.add(ref);
			
			DocReferent refDel = new DocReferent();
			refDel.setId(1);
			refDel.setCodeRef(1224);
			refDel.setEventDate(sdf.parse("31.12.2020"));			
			refs.add(refDel);
			
			
			doc.setReferentsAgreed(refs );
			
			Calendar cal = new GregorianCalendar();
			cal.setTime(dat);
			cal.add(Calendar.SECOND, cal.get(Calendar.SECOND)+11);
			dat = cal.getTime();
			
			Doc doc1 = new Doc();
			doc1.setDocType(222);
			doc1.setRnDoc("94-00-103");
			doc1.setDocDate(dat);
			doc1.setDocVid(2);
			doc1.setReceiveDate(sdf.parse("01.01.2021"));
			List<DocReferent> refs1 = new ArrayList<DocReferent>();
			
			DocReferent ref1 = new DocReferent();
			ref1.setId(10);
			ref1.setCodeRef(1225);
			ref1.setEventDate(sdf.parse("01.01.2021"));			
			refs1.add(ref1);
			
			DocReferent refNew = new DocReferent();
			refNew.setId(100);
			refNew.setCodeRef(1288);
			refNew.setEventDate(sdf.parse("01.01.2021"));			
			refs1.add(refNew);
			
			
			doc1.setReferentsAgreed(refs1 );
			
			List<ObjectsDifference> result = new ObjectComparator(dat,dat, sd, null).compare(doc, doc1);
			
//			Referent r1 = new Referent();
//			Referent r2 = new Referent();
//			
//			r1.setFzlEgn("7609093441");
//			
//			ReferentAddress addr = new ReferentAddress();
//			addr.setAddrText("Pirin 5");
//			
//			r2.setAddress(addr);
//			
//			
//			List<ObjectsDifference> result = new ObjectComparator(dat,dat, sd, null).compare(r1, r2);
			
			
			
			printDiff(result, null);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		
		
		
		
	}
	
	
	
	public static void printDiff (List<ObjectsDifference> result, String space) {
		
		if (space == null) {
			space = "\t";
		}
		
		for (ObjectsDifference diff : result) {
			if (diff == null) {
				//не би трябвало да стане но ...
				continue;
			}
			
			String type = "";
			if (diff.getType() == 3) {
				type = "ИЗТРИВАНЕ";
			}else {
				if (diff.getType() == 2) {
					type = "ПРОМЯНА";
				}else {
					if (diff.getType() == 1) {
						type = "ДОБАВЯНЕ";
					}
				}
			}
			
			
			if (diff.getError() != null) {
				System.out.println(diff.getFieldName() + "\tERROR: " + diff.getError());
			}else {
				
				if (diff.getCoplexDif().size() > 0) {
					//Списък е и има промени
					System.out.println(space + diff.getFieldName() + "\t" + type);
					printDiff(diff.getCoplexDif(), "\t"+space);
				}else {
					
					if (diff.getType() == 3) {
						//Изтриване
						System.out.println(space + diff.getFieldName() + "\t" + type);
					}else {
						if (diff.getType() == 1) {
							//ДОБАВЯНЕ
							System.out.println(space + diff.getFieldName() + "\t" + type);
						}else {
							System.out.println(space + diff.getFieldName() + ":\t" + diff.getOldVal() + " --> " + diff.getNewVal());
						}
					}
					
					
				}
			}
			
		}
	}

}
