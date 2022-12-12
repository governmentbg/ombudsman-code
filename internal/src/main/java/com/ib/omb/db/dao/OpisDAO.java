package com.ib.omb.db.dao;

import static com.ib.system.utils.SearchUtils.asInteger;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.persistence.Query;

import com.ib.omb.system.OmbConstants;
import com.ib.system.ActiveUser;
import com.ib.system.SysConstants;
import com.ib.system.db.DialectConstructor;
import com.ib.system.db.JPA;
import com.ib.system.db.SelectMetadata;
import com.ib.system.db.dto.SystemJournal;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.utils.DateUtils;
import static com.ib.system.utils.SearchUtils.asString;

public class OpisDAO extends DocDAO {

	public OpisDAO(ActiveUser user) {
		super(user);
	}
	public static final String	andS	= " and ";
	public static final String	uperDv	= " UPPER(dvij.dvij_text) LIKE '%";
	public static final String	a17	= " A17 ";
	public static final String	selCount	= " select count(*) ";
	public static final String	uperDvij	= "UPPER(dvij.dvij_text) LIKE '%";
	
	
	public String getSqlStringForOpis () {
		
		String dialect = JPA.getUtil().getDbVendorName();
		String sql = " select dvij.id A0, dvij.doc_id  A1, "+DocDAO.formRnDocSelect("doc.", dialect)+" A2, doc.rn_prefix A3, doc.rn_pored A4, doc.doc_date A5, doc.doc_type A6, doc.doc_vid A7";
		       sql += ",dvij.dvij_date A8, dvij.dvij_method A9, dvij.code_ref A10, dvij.dvij_text A11, dvij.ekz_nomer A12, dvij.user_reg A13, doc.teh_nomer A14, doc.teh_date A15 ";
		       sql += " ,doc.REGISTER_ID A16, " + DialectConstructor.limitBigString(dialect, "doc.OTNOSNO", 300) + a17;
		       sql += " , dvij.return_date A18 ";
		       		       		
		return sql;
		
	}
	
	 
	/**
	 *  Избор на доументи за формиране на опис за предаване без зачисление
	 * @param metodPredList  - методи на предаване
	 * @param registr    - регистратура, собственик на документи
	 * @param dateFrom   - интервал на предаване - от
	 * @param dateTo     - до
	 * @param predNaList  - предадени на зададени с кодове от Адм. структура 
	 * @param textPredNa  - предадени на зададени с текст
	 * @param vidDocList  - видове документи
	 * @param user        - потребител, извършил предаването
	 * @param bezIdPredList  -  избрани id от doc_dvij, които се изключват при търсенето
	 * @return
	 */
	public SelectMetadata createSelectOpisBezZach (List<Integer> metodPredList, Integer registr, Date dateFrom, Date dateTo, List<Integer> predNaList, String textPredNa, List <Integer> vidDocList, Integer user,  List<Object[]> bezIdPredList )   {
		StringBuilder select = new StringBuilder();
		StringBuilder from = new StringBuilder();
		StringBuilder where = new StringBuilder();
		
		String dialect = JPA.getUtil().getDbVendorName();
		
		select.append(getSqlStringForOpis ());
		from.append(" from doc doc, doc_dvij dvij ");
		where.append(" where dvij.doc_id = doc.doc_id ");
		
		if (metodPredList != null && !metodPredList.isEmpty()) {
			String s = "";  
			for (int i = 0; i < metodPredList.size(); i++) {
		   		if (!s.isEmpty())  s+= ",";
	    		s += String.valueOf(metodPredList.get(i));
	    	}
			s = "(" + s + ") ";
			where.append(" and dvij.dvij_method in " + s);
			
		}
			
		
		
		if (registr != null) where.append(" and doc.registratura_id = " + registr);
		if (dateFrom != null) {
			where.append(" and dvij.dvij_date >= " + DialectConstructor.convertDateToSQLString(dialect, DateUtils.startDate(dateFrom)) + " "); 
		}
		if (dateTo != null) {
			where.append(" and dvij.dvij_date <= " + DialectConstructor.convertDateToSQLString(dialect, DateUtils.endDate(dateTo)) + " "); 
		}
		
			
		if (user != null) where.append(" and dvij.user_reg = "+ user);
		
		// зА ЗАДАДЕНИ ВИДОВЕ ДОКУМЕНТИ
		if (vidDocList != null && !vidDocList.isEmpty()) {
			String s = "";  
			for (int i = 0; i < vidDocList.size(); i++) {
		   		if (!s.isEmpty())  s+= ",";
	    		s += String.valueOf(vidDocList.get(i));
	    	}
			s = "(" + s + ") ";
			where.append(" and doc.doc_vid in " + s);
			
		}
		 
		// За предаване на зададени с код от Адм. структура и кореспонденти
		if (predNaList != null && !predNaList.isEmpty()) {
			// Зададени са избрани кодове на кого от адм. структура са предадени документи
		 	String s0 = "";  
		 	String s1 = "";
		 	String s = "";
		 	
		 // За предаване на зададен с текст
			  if (textPredNa != null && !textPredNa.trim().isEmpty()){
				  textPredNa = textPredNa.trim();
				  
				String str[] = textPredNa.split(" ");
	          	if (str != null && str.length > 1) { 
	          		s = "";
	          		int k = 0;
	          		for (int i = 0; i < str.length; i++) {
	          			if (str[i] == null)
	          				continue;
	          			else
	          				str[i] = str[i].trim();
	          			if (str[i].length() > 2) {
	          				k ++;
	          				if (k > 1) s += andS;
		            			s += uperDvij+str[i].toUpperCase().trim()+"%'";
	          			}	
	          		}
	          		if (s.length() > 0) {
	          			s = "(" + s + ")";
	          			s1 = new String(s);
	          		
	          		} else
	          			s1 = uperDv+textPredNa.toUpperCase().trim()+"%'";
	          		
	          	} else	
	          		s1 = uperDv+textPredNa.toUpperCase().trim()+"%'";
	          }
			  
			s = "";
			if (predNaList.size() <= 1000) {
				for (int i = 0; i < predNaList.size(); i++) {
		    		if (!s.isEmpty())  s+= ",";
		    		s += String.valueOf(predNaList.get(i));
		    	}
				s = "(" + s + ") ";
				s0 =" dvij.code_ref in " + s;
			} else {
				
//				s = "";
						
	         int j = 0;
	         int step = 1000;
	         
	         List <String>  strL = new ArrayList<> () ; 
			
			     while (j < predNaList.size())  {
			    	 
			    	 s = "";
			    	 int k = j + step;
					
			    	 if (k < predNaList.size()) { 
						for (int i = j; i < k; i++) {
				    		if (!s.isEmpty())  s+= ",";
				    		s += String.valueOf(predNaList.get(i));
				    	}
			    	 } else {
			    		 for (int i = j; i < predNaList.size(); i++) {
					    		if (!s.isEmpty())  s+= ",";
					    		s += String.valueOf(predNaList.get(i));
					     }
			    						    		 
			    	 }
			    	 strL.add(s);
			    	 j = k;
			     } 
			
			     if (!strL.isEmpty() ) {
			    	 s = "";
			    	 for (int i = 0; i < strL.size(); i++) {
			    		 if (!s.isEmpty())  s+= andS;
			    		 s += " dvij.code_ref in (" + strL.get(i) + ") ";
			    	 }
			    	s0 = "("+  s + ") "; 
			     }
			     
			}
			
			if (!s1.trim().isEmpty() ) {
				where.append(" and ((" + s0 + ") or ((dvij.code_ref is null or dvij.code_ref = 0) and (" + s1 + ")) ) ");
			} else where.append(andS+ s0);
	    	
		} else {  // Няма зададени кодове за предаден на и кореспонденти
		
		// За предаване само на зададен с текст
		  if (textPredNa != null && !textPredNa.trim().isEmpty()){
			  textPredNa = textPredNa.trim();
			  
			String str[] = textPredNa.split(" ");
          	if (str != null && str.length > 1) { 
          		String s = "";
          		int k = 0;
          		for (int i = 0; i < str.length; i++) {
          			if (str[i] == null)
          				continue;
          			else
          				str[i] = str[i].trim();
          			if (str[i].length() > 2) {
          				k ++;
          				if (k > 1) s += andS;
	            			s += uperDvij+str[i].toUpperCase().trim()+"%'";
          			}	
          		}
          		if (s.length() > 0) {
          			s = "(" + s + ")";
          			where.append(" and ((dvij.code_ref is null or dvij.code_ref = 0) and "+  s + ") " ); 
          		} else
          			where.append(" and  ((dvij.code_ref is null or dvij.code_ref = 0) and  UPPER(dvij.dvij_text) LIKE '%"+textPredNa.toUpperCase().trim()+"%') ");
          		
          	} else	
          		where.append(" and  ((dvij.code_ref is null or dvij.code_ref = 0) and  UPPER(dvij.dvij_text) LIKE '%"+textPredNa.toUpperCase().trim()+"%') ");
          }
		}
		
		// id_dvij, които се изключват	   
		   if (bezIdPredList != null && !bezIdPredList.isEmpty()) {
				// Зададени са избрани id от doc_dvij, които се изключват от търсенето 
			   String s0 = "";  
			 	String s = ""; 
			 	s = "";
				if (bezIdPredList.size() <= 1000) {
					for (int i = 0; i < bezIdPredList.size(); i++) {
						Object[] obj = bezIdPredList.get(i);
						Integer idDvij = asInteger(obj[0]);
			    		if (!s.isEmpty())  s+= ",";
			    		s += String.valueOf(idDvij);
			    	}
					s = "(" + s + ") ";
					s0 =" dvij.id not in " + s;
				} else {
					
//					s = "";
							
		         int j = 0;
		         int step = 1000;
		         
		         List <String>  strL = new ArrayList<> () ; 
				
				     while (j < bezIdPredList.size())  {
				    	 
				    	 s = "";
				    	 int k = j + step;
						
				    	 if (k < bezIdPredList.size()) { 
							for (int i = j; i < k; i++) {
								Object[] obj = bezIdPredList.get(i);
								Integer idDvij = asInteger(obj[0]);
					    		if (!s.isEmpty())  s+= ",";
					    		s += String.valueOf(idDvij);
					    	}
				    	 } else {
				    		 for (int i = j; i < bezIdPredList.size(); i++) {
				    			 Object[] obj = bezIdPredList.get(i);
									Integer idDvij = asInteger(obj[0]);
						    		if (!s.isEmpty())  s+= ",";
						    		s += String.valueOf(idDvij);
						     }
				    						    		 
				    	 }
				    	 strL.add(s);
				    	 j = k;
				     } 
				
				     if (!strL.isEmpty() ) {
				    	 s = "";
				    	 for (int i = 0; i < strL.size(); i++) {
				    		 if (!s.isEmpty())  s+= andS;
				    		 s += " dvij.id not in (" + strL.get(i) + ") ";
				    	 }
				    	s0 = "("+  s + ") "; 
				     }
				     
				}
				
				 where.append(andS+ s0);
		    	
			 	
		   } 
		   
		  
		SelectMetadata sm = new SelectMetadata();

		sm.setSqlCount(selCount + from + where);
		
		sm.setSql(select.toString() + from.toString() + where.toString());
		
		return sm;
		
	}
	
	
	
	
	/**
	 *  Търсене на  документи по id от DOC
	 * @param IdDocList
	 * @return
	 */
	
	public SelectMetadata createSelectOpisZach (List<Object[]> IdDocList )   {
		StringBuilder select = new StringBuilder();
		StringBuilder from = new StringBuilder();
		StringBuilder where = new StringBuilder();
		
		String dialect = JPA.getUtil().getDbVendorName();
				
			select.append("select distinct d.DOC_ID A0, null A1, "+DocDAO.formRnDocSelect("d.", dialect)+" A2, d.RN_PREFIX A3, d.RN_PORED A4, d.DOC_DATE A5, d.DOC_TYPE A6, d.DOC_VID A7 ");
			select.append(", " + DialectConstructor.convertDateToSQLString(dialect, new Date())  + " A8 ");  // Дата на предаване 
			select.append(", " + String.valueOf(OmbConstants.CODE_ZNACHENIE_PREDAVANE_NA_RAKA) + " A9 ");   // Предаване на ръка
			select.append(", null A10, ' ' A11, 1 A12, null A13 ");   // Предаден на (код), номер екз.(1), null
			select.append(", d.TEH_NOMER A14, d.TEH_DATE A15, d.REGISTER_ID A16 ");
			select.append(", " + DialectConstructor.limitBigString(dialect, "d.OTNOSNO", 300) + a17); // max 300!
			select.append(" , d.URGENT A18, d.CODE_REF_CORRESP A19 ");
			select.append(" , CASE WHEN d.DOC_TYPE = 1 THEN null ELSE "); // за входящите авторите не се теглят
			select.append(DialectConstructor.convertToDelimitedString(dialect, "dr.CODE_REF", "DOC_REFERENTS dr where dr.DOC_ID = d.DOC_ID and dr.ROLE_REF = 1", "dr.PORED"));
			select.append(" END A20 ");
			select.append(",  d.PROCESSED  A21 ");    // Ново поле
				
	
		from.append(" from doc d ");
		where.append(" where 1 = 1 ");
		
			   
		   if (IdDocList != null && !IdDocList.isEmpty()) {
				// Зададени са избрани id от doc_dvij, които се изключват от търсенето 
			   String s0 = "";  
//			 	String s1 = "";
			 	String s = ""; 
			 	s = "";
				if (IdDocList.size() <= 1000) {
					for (int i = 0; i < IdDocList.size(); i++) {
						Object[] obj = IdDocList.get(i);
						Integer idDvij = asInteger(obj[0]);
			    		if (!s.isEmpty())  s+= ",";
			    		s += String.valueOf(idDvij);
			    	}
					s = "(" + s + ") ";
					s0 =" d.DOC_ID  in " + s;
				} else {
					
//					s = "";
							
		         int j = 0;
		         int step = 1000;
		         
		         List <String>  strL = new ArrayList<> () ; 
				
				     while (j < IdDocList.size())  {
				    	 
				    	 s = "";
				    	 int k = j + step;
						
				    	 if (k < IdDocList.size()) { 
							for (int i = j; i < k; i++) {
								Object[] obj = IdDocList.get(i);
								Integer idDvij = asInteger(obj[0]);
					    		if (!s.isEmpty())  s+= ",";
					    		s += String.valueOf(idDvij);
					    	}
				    	 } else {
				    		 for (int i = j; i < IdDocList.size(); i++) {
				    			 Object[] obj = IdDocList.get(i);
									Integer idDvij = asInteger(obj[0]);
						    		if (!s.isEmpty())  s+= ",";
						    		s += String.valueOf(idDvij);
						     }
				    						    		 
				    	 }
				    	 strL.add(s);
				    	 j = k;
				     } 
				
				     if (!strL.isEmpty() ) {
				    	 s = "";
				    	 for (int i = 0; i < strL.size(); i++) {
				    		 if (!s.isEmpty())  s+= andS;
				    		 s += " d.DOC_ID.id in (" + strL.get(i) + ") ";
				    	 }
				    	s0 = "("+  s + ") "; 
				     }
				     
				}
				
				 where.append(andS+ s0);
		    	
			 	
		   } 
		   
		  
		SelectMetadata sm = new SelectMetadata();

		sm.setSqlCount(selCount + from + where);
		
		sm.setSql(select.toString() + from.toString() + where.toString());
		
		return sm;
		
	}
	
	
	/**
	 *  Търсене на предадени документи по id от DOC_DVIJ
	 * @param IdPredList
	 * @return
	 */
	
	public SelectMetadata createSelectOpisBezZach (List<Object[]> IdPredList )   {
		StringBuilder select = new StringBuilder();
		StringBuilder from = new StringBuilder();
		StringBuilder where = new StringBuilder();
		
//		String dialect = JPA.getUtil().getDbVendorName();
		
		select.append(getSqlStringForOpis ());
		from.append(" from doc doc, doc_dvij dvij ");
		where.append(" where dvij.doc_id = doc.doc_id ");
		
			   
		   if (IdPredList != null && !IdPredList.isEmpty()) {
				// Зададени са избрани id от doc_dvij, които се изключват от търсенето 
			   String s0 = "";  
//			 	String s1 = "";
			 	String s = ""; 
//			 	s = "";
				if (IdPredList.size() <= 1000) {
					for (int i = 0; i < IdPredList.size(); i++) {
						Object[] obj = IdPredList.get(i);
						Integer idDvij = asInteger(obj[0]);
			    		if (!s.isEmpty())  s+= ",";
			    		s += String.valueOf(idDvij);
			    	}
					s = "(" + s + ") ";
					s0 =" dvij.id  in " + s;
				} else {
					
//					s = "";
							
		         int j = 0;
		         int step = 1000;
		         
		         List <String>  strL = new ArrayList<> () ; 
				
				     while (j < IdPredList.size())  {
				    	 
				    	 s = "";
				    	 int k = j + step;
						
				    	 if (k < IdPredList.size()) { 
							for (int i = j; i < k; i++) {
								Object[] obj = IdPredList.get(i);
								Integer idDvij = asInteger(obj[0]);
					    		if (!s.isEmpty())  s+= ",";
					    		s += String.valueOf(idDvij);
					    	}
				    	 } else {
				    		 for (int i = j; i < IdPredList.size(); i++) {
				    			 Object[] obj = IdPredList.get(i);
									Integer idDvij = asInteger(obj[0]);
						    		if (!s.isEmpty())  s+= ",";
						    		s += String.valueOf(idDvij);
						     }
				    						    		 
				    	 }
				    	 strL.add(s);
				    	 j = k;
				     } 
				
				     if (!strL.isEmpty() ) {
				    	 s = "";
				    	 for (int i = 0; i < strL.size(); i++) {
				    		 if (!s.isEmpty())  s+= andS;
				    		 s += " dvij.id in (" + strL.get(i) + ") ";
				    	 }
				    	s0 = "("+  s + ") "; 
				     }
				     
				}
				
				 where.append(andS+ s0);
		    	
			 	
		   } 
		   
		  
		SelectMetadata sm = new SelectMetadata();

		sm.setSqlCount(selCount + from + where);
		
		sm.setSql(select.toString() + from.toString() + where.toString());
		
		return sm;
		
	}
	
	
	public List<Object[]> findDataWithSqlString(String sqlStr, String orderStr, boolean isDelo ) throws DbErrorException {
		if (sqlStr != null) {
			sqlStr = sqlStr.trim();
			if (sqlStr.isEmpty())  sqlStr = null;
		}
		if (sqlStr == null) return null; 
		if (orderStr != null) {
			orderStr = orderStr.trim();
			if (orderStr.isEmpty() ) orderStr = null;
		}
		
		if (orderStr != null)  sqlStr += " ORDER BY " + orderStr;
		
		try {
			

			StringBuilder sql = new StringBuilder();
			sql.append(sqlStr);

			@SuppressWarnings("unchecked")
			List<Object[]> result = createNativeQuery(sql.toString()).getResultList();
			
               return result;

		} catch (Exception e) {
			if (!isDelo)
			  throw new DbErrorException("Грешка при търсене на основни данни за предадени документи! - " + e.getLocalizedMessage(), e);
			else
			  throw new DbErrorException("Грешка при търсене на основни данни за предадени дела/преписки! - " + e.getLocalizedMessage(), e);	
		}
	}
	
	public List<Object[]> findDataWithSqlStringParams(String sqlStr, Map<String, Object> params,  String orderStr) throws DbErrorException {
		if (sqlStr != null) {
			sqlStr = sqlStr.trim();
			if (sqlStr.isEmpty())  sqlStr = null;
		}
		if (sqlStr == null) return null; 
		if (orderStr != null) {
			orderStr = orderStr.trim();
			if (orderStr.isEmpty() ) orderStr = null;
		}
		
		if (orderStr != null)  sqlStr += " ORDER BY " + orderStr;
		
		Query querySelect = null;
		querySelect = JPA.getUtil().getEntityManager().createNativeQuery(sqlStr);
		if (params != null && params.size() > 0) {
			for (Entry<String, Object> pair : params.entrySet()) {
				querySelect.setParameter(pair.getKey(), pair.getValue());
			}
		}  
		
		try {
		
			List<Object[]> result = new ArrayList<> ();
			result.addAll(querySelect.getResultList());
			
               return result;

		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на основни данни за предадени документи! - " + e.getLocalizedMessage(), e);
		}
	}
	
	/**
	 * Получаване на ОТНОСНО за документ
	 * @param idDoc  - id на документ
	 * @return
	 * @throws DbErrorException
	 */
	
	public String getOtnosnoFromDoc (Integer idDoc) throws DbErrorException {
		String dialect = JPA.getUtil().getDbVendorName();
		String col = " otnosno ";
		if (dialect.indexOf("ORACLE") != -1) {
			col = " TO_CHAR(otnosno) ";
		
		} 
//		else if (dialect.indexOf("INFORMIX") != -1) {
//			col = " otnosno ";
//			
//		} else if (dialect.indexOf("SQLSERVER") != -1) {
//			col = " otnosno ";
//
//		} else if (dialect.indexOf("POSTGRESQL") != -1) {
//			col = " otnosno ";
//		} else {
//			col = " otnosno ";
//		}
		
		String otn = null;
		try {
			  otn = asString( 
				createNativeQuery("select "+ col + " from DOC where DOC_ID = " + idDoc) //
					.getResultList().get(0));
			
	
		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на анотация за документ!" + "-" + e.getLocalizedMessage(), e);
		}
		
		if (otn != null) {
			otn = otn.trim();
			if (otn.isEmpty()) otn = null;
		}
		return otn;
		
	}
	
	
	// Опис за дела/преписки
    
	public String getSqlStringForOpisDelo () {
		
		String sql = " select dvij.id A0, dvij.delo_id  A1, delo.rn_delo A2, delo.rn_prefix A3, delo.rn_pored A4, delo.delo_date A5, delo.delo_type A6, null A7";
		       sql += ",dvij.dvij_date A8, dvij.dvij_method A9, dvij.code_ref A10, dvij.dvij_text A11, dvij.tom_nomer A12, dvij.user_reg A13, null A14, null A15 ";
		       sql += " ,delo.max_br_sheets A16, " + DialectConstructor.limitBigString(JPA.getUtil().getDbVendorName(), "delo.DELO_NAME", 300) + a17;
		       sql += " ,dvij.return_date A18 "; 	       		
		return sql;
		
	}
	
	 
	/**
	 *  Избор на дела/преписки за формиране на разносен опис за предаване на дела/преписки  без зачисление
	 * @param metodPredList  - методи на предаване
	 * @param registr    - регистратура, собственик на документи
	 * @param dateFrom   - интервал на предаване - от
	 * @param dateTo     - до
	 * @param predNaList  - предадени на зададени с кодове от Адм. структура 
	 * @param textPredNa  - предадени на зададени с текст
	 * @param tipDeloList  - тип на дело
	 * @param user        - потребител, извършил предаването
	 * @param bezIdPredList  -  избрани id от doc_dvij, които се изключват при търсенето
	 * @return
	 */
	public SelectMetadata createSelectOpisDeloBezZach (List<Integer> metodPredList, Integer registr, Date dateFrom, Date dateTo, List<Integer> predNaList, String textPredNa, List <Integer> tipDeloList, Integer user,  List<Object[]> bezIdPredList )   {
		StringBuilder select = new StringBuilder();
		StringBuilder from = new StringBuilder();
		StringBuilder where = new StringBuilder();
		
		String dialect = JPA.getUtil().getDbVendorName();
		
		select.append(getSqlStringForOpisDelo ());
		from.append(" from delo delo, delo_dvij dvij ");
		where.append(" where dvij.delo_id = delo.delo_id ");
		
		if (metodPredList != null && !metodPredList.isEmpty()) {
			String s = "";  
			for (int i = 0; i < metodPredList.size(); i++) {
		   		if (!s.isEmpty())  s+= ",";
	    		s += String.valueOf(metodPredList.get(i));
	    	}
			s = "(" + s + ") ";
			where.append(" and dvij.dvij_method in " + s);
			
		}
			
		
		
		if (registr != null) where.append(" and delo.registratura_id = " + registr);
		if (dateFrom != null) {
			where.append(" and dvij.dvij_date >= " + DialectConstructor.convertDateToSQLString(dialect, DateUtils.startDate(dateFrom)) + " "); 
		}
		if (dateTo != null) {
			where.append(" and dvij.dvij_date <= " + DialectConstructor.convertDateToSQLString(dialect, DateUtils.endDate(dateTo)) + " "); 
		}
		
			
		if (user != null) where.append(" and dvij.user_reg = "+ user);
		
		// зА ЗАДАДЕНИ ВИДОВЕ ДОКУМЕНТИ
		if (tipDeloList != null && !tipDeloList.isEmpty()) {
			String s = "";  
			for (int i = 0; i < tipDeloList.size(); i++) {
		   		if (!s.isEmpty())  s+= ",";
	    		s += String.valueOf(tipDeloList.get(i));
	    	}
			s = "(" + s + ") ";
			where.append(" and delo.delo_type in " + s);
			
		}
		 
		// За предаване на зададени с код от Адм. структура и кореспонденти
		if (predNaList != null && !predNaList.isEmpty()) {
			// Зададени са избрани кодове на кого от адм. структура са предадени документи
		 	String s0 = "";  
		 	String s1 = "";
		 	String s = "";
		 	
		 // За предаване на зададен с текст
			  if (textPredNa != null && !textPredNa.trim().isEmpty()){
				  textPredNa = textPredNa.trim();
				  
				String str[] = textPredNa.split(" ");
	          	if (str != null && str.length > 1) { 
	          		s = "";
	          		int k = 0;
	          		for (int i = 0; i < str.length; i++) {
	          			if (str[i] == null)
	          				continue;
	          			else
	          				str[i] = str[i].trim();
	          			if (str[i].length() > 2) {
	          				k ++;
	          				if (k > 1) s += andS;
		            			s += uperDvij+str[i].toUpperCase().trim()+"%'";
	          			}	
	          		}
	          		if (s.length() > 0) {
	          			s = "(" + s + ")";
	          			s1 = new String(s);
	          		
	          		} else
	          			s1 = uperDv+textPredNa.toUpperCase().trim()+"%'";
	          		
	          	} else	
	          		s1 = uperDv+textPredNa.toUpperCase().trim()+"%'";
	          }
			  
			s = "";
			if (predNaList.size() <= 1000) {
				for (int i = 0; i < predNaList.size(); i++) {
		    		if (!s.isEmpty())  s+= ",";
		    		s += String.valueOf(predNaList.get(i));
		    	}
				s = "(" + s + ") ";
				s0 =" dvij.code_ref in " + s;
			} else {
				
				s = "";
						
	         int j = 0;
	         int step = 1000;
	         
	         List <String>  strL = new ArrayList<> () ; 
			
			     while (j < predNaList.size())  {
			    	 
			    	 s = "";
			    	 int k = j + step;
					
			    	 if (k < predNaList.size()) { 
						for (int i = j; i < k; i++) {
				    		if (!s.isEmpty())  s+= ",";
				    		s += String.valueOf(predNaList.get(i));
				    	}
			    	 } else {
			    		 for (int i = j; i < predNaList.size(); i++) {
					    		if (!s.isEmpty())  s+= ",";
					    		s += String.valueOf(predNaList.get(i));
					     }
			    						    		 
			    	 }
			    	 strL.add(s);
			    	 j = k;
			     } 
			
			     if (!strL.isEmpty() ) {
			    	 s = "";
			    	 for (int i = 0; i < strL.size(); i++) {
			    		 if (!s.isEmpty())  s+= andS;
			    		 s += " dvij.code_ref in (" + strL.get(i) + ") ";
			    	 }
			    	s0 = "("+  s + ") "; 
			     }
			     
			}
			
			if (!s1.trim().isEmpty() ) {
				where.append(" and ((" + s0 + ") or ((dvij.code_ref is null or dvij.code_ref = 0) and (" + s1 + ")) ) ");
			} else where.append(andS+ s0);
	    	
		} else {  // Няма зададени кодове за предаден на и кореспонденти
		
		// За предаване само на зададен с текст
		  if (textPredNa != null && !textPredNa.trim().isEmpty()){
			  textPredNa = textPredNa.trim();
			  
			String str[] = textPredNa.split(" ");
          	if (str != null && str.length > 1) { 
          		String s = "";
          		int k = 0;
          		for (int i = 0; i < str.length; i++) {
          			if (str[i] == null)
          				continue;
          			else
          				str[i] = str[i].trim();
          			if (str[i].length() > 2) {
          				k ++;
          				if (k > 1) s += andS;
	            			s += uperDvij+str[i].toUpperCase().trim()+"%'";
          			}	
          		}
          		if (s.length() > 0) {
          			s = "(" + s + ")";
          			where.append(" and ((dvij.code_ref is null or dvij.code_ref = 0) and "+  s + ") " ); 
          		} else
          			where.append(" and  ((dvij.code_ref is null or dvij.code_ref = 0) and  UPPER(dvij.dvij_text) LIKE '%"+textPredNa.toUpperCase().trim()+"%') ");
          		
          	} else	
          		where.append(" and  ((dvij.code_ref is null or dvij.code_ref = 0) and  UPPER(dvij.dvij_text) LIKE '%"+textPredNa.toUpperCase().trim()+"%') ");
          }
		}
		
		// id_dvij, които се изключват	   
		   if (bezIdPredList != null && !bezIdPredList.isEmpty()) {
				// Зададени са избрани id от doc_dvij, които се изключват от търсенето 
			   String s0 = "";  
			 	String s = ""; 
			 	s = "";
				if (bezIdPredList.size() <= 1000) {
					for (int i = 0; i < bezIdPredList.size(); i++) {
						Object[] obj = bezIdPredList.get(i);
						Integer idDvij = asInteger(obj[0]);
			    		if (!s.isEmpty())  s+= ",";
			    		s += String.valueOf(idDvij);
			    	}
					s = "(" + s + ") ";
					s0 =" dvij.id not in " + s;
				} else {
							
		         int j = 0;
		         int step = 1000;
		         
		         List <String>  strL = new ArrayList<> () ; 
				
				     while (j < bezIdPredList.size())  {
				    	 
				    	 s = "";
				    	 int k = j + step;
						
				    	 if (k < bezIdPredList.size()) { 
							for (int i = j; i < k; i++) {
								Object[] obj = bezIdPredList.get(i);
								Integer idDvij = asInteger(obj[0]);
					    		if (!s.isEmpty())  s+= ",";
					    		s += String.valueOf(idDvij);
					    	}
				    	 } else {
				    		 for (int i = j; i < bezIdPredList.size(); i++) {
				    			 Object[] obj = bezIdPredList.get(i);
									Integer idDvij = asInteger(obj[0]);
						    		if (!s.isEmpty())  s+= ",";
						    		s += String.valueOf(idDvij);
						     }
				    						    		 
				    	 }
				    	 strL.add(s);
				    	 j = k;
				     } 
				
				     if (!strL.isEmpty() ) {
				    	 s = "";
				    	 for (int i = 0; i < strL.size(); i++) {
				    		 if (!s.isEmpty())  s+= andS;
				    		 s += " dvij.id not in (" + strL.get(i) + ") ";
				    	 }
				    	s0 = "("+  s + ") "; 
				     }
				     
				}
				
				 where.append(andS+ s0);
		    	
			 	
		   } 
		   
		  
		SelectMetadata sm = new SelectMetadata();

		sm.setSqlCount(selCount + from + where);
		
		sm.setSql(select.toString() + from.toString() + where.toString());
		
		return sm;
		
	}
	
	/**
	 *  Търсене на предадени дела/преписки по id от DELO_DVIJ
	 * @param IdPredList
	 * @return
	 */
	
	public SelectMetadata createSelectOpisDeloBezZach (List<Object[]> IdPredList )   {
		StringBuilder select = new StringBuilder();
		StringBuilder from = new StringBuilder();
		StringBuilder where = new StringBuilder();
		
		
		select.append(getSqlStringForOpisDelo ());
		from.append(" from delo delo, delo_dvij dvij ");
		where.append(" where dvij.delo_id = delo.delo_id ");
		
			   
		   if (IdPredList != null && !IdPredList.isEmpty()) {
				// Зададени са избрани id от doc_dvij, които се изключват от търсенето 
			   String s0 = "";  
			 	String s1 = "";
			 	String s = ""; 
			 	s = "";
				if (IdPredList.size() <= 1000) {
					for (int i = 0; i < IdPredList.size(); i++) {
						Object[] obj = IdPredList.get(i);
						Integer idDvij = asInteger(obj[0]);
			    		if (!s.isEmpty())  s+= ",";
			    		s += String.valueOf(idDvij);
			    	}
					s = "(" + s + ") ";
					s0 =" dvij.id  in " + s;
				} else {
					
		         int j = 0;
		         int step = 1000;
		         
		         List <String>  strL = new ArrayList<> () ; 
				
				     while (j < IdPredList.size())  {
				    	 
				    	 s = "";
				    	 int k = j + step;
						
				    	 if (k < IdPredList.size()) { 
							for (int i = j; i < k; i++) {
								Object[] obj = IdPredList.get(i);
								Integer idDvij = asInteger(obj[0]);
					    		if (!s.isEmpty())  s+= ",";
					    		s += String.valueOf(idDvij);
					    	}
				    	 } else {
				    		 for (int i = j; i < IdPredList.size(); i++) {
				    			 Object[] obj = IdPredList.get(i);
									Integer idDvij = asInteger(obj[0]);
						    		if (!s.isEmpty())  s+= ",";
						    		s += String.valueOf(idDvij);
						     }
				    						    		 
				    	 }
				    	 strL.add(s);
				    	 j = k;
				     } 
				
				     if (!strL.isEmpty() ) {
				    	 s = "";
				    	 for (int i = 0; i < strL.size(); i++) {
				    		 if (!s.isEmpty())  s+= andS;
				    		 s += " dvij.id in (" + strL.get(i) + ") ";
				    	 }
				    	s0 = "("+  s + ") "; 
				     }
				     
				}
				
				 where.append(andS+ s0);
		    	
			 	
		   } 
		   
		  
		SelectMetadata sm = new SelectMetadata();

		sm.setSqlCount(selCount + from + where);
		
		sm.setSql(select.toString() + from.toString() + where.toString());
		
		return sm;
		
	}
	
	/**
	 * Получаване на НАИМЕНОВАНИЕ за дела/преписки
	 * @param idDelo  - id на дело
	 * @return
	 * @throws DbErrorException
	 */
	
	public String getNaimFromDelo (Integer idDelo) throws DbErrorException {
		String dialect = JPA.getUtil().getDbVendorName();
		String col = " delo_name ";
		if (dialect.indexOf("ORACLE") != -1) {
			col = " TO_CHAR(delo_name) ";
		
		} 
		
		String otn = null;
		try {
			  otn = asString( 
				createNativeQuery("select "+ col + " from DELO where DELO_ID = " + idDelo) //
					.getResultList().get(0));
			
	
		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на наименование за дело/преписка!" + "-" + e.getLocalizedMessage(), e);
		}
		
		if (otn != null) {
			otn = otn.trim();
			if (otn.isEmpty()) otn = null;
		}
		return otn;
		
	}
	
	/**
	 * @param opisName
	 * @param count
	 * @throws DbErrorException
	 */
	public void journalOpis(String opisName, Integer count) throws DbErrorException {
		String ident = opisName + ", бр. " + count + ".";
		SystemJournal journal = new SystemJournal(OmbConstants.CODE_ZNACHENIE_JOURNAL_OPISDOCDELO, null, ident);

		journal.setCodeAction(SysConstants.CODE_DEIN_EXPORT);
		journal.setDateAction(new Date());
		journal.setIdUser(getUserId());

		saveAudit(journal);
	}
}
