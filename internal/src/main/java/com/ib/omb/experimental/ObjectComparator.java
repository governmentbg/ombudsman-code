package com.ib.omb.experimental;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ib.indexui.db.dto.AdmGroup;
import com.ib.indexui.system.Constants;
import com.ib.omb.db.dto.Delo;
import com.ib.omb.db.dto.Doc;
import com.ib.omb.db.dto.Task;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.system.ObjectsDifference;
import com.ib.system.SysConstants;
import com.ib.system.db.JPA;
import com.ib.system.db.JournalAttr;
import com.ib.system.db.dto.SystemJournal;
import com.ib.system.exceptions.DbErrorException;

public class ObjectComparator {
	
	private Date oldDate;
	private Date newDate;
	private SystemData sd;
	private Integer lang;
	
	
	public ObjectComparator(Date oldDate, Date newDate, SystemData sd, Integer lang ) {
		this.oldDate = oldDate;
		this.newDate = newDate;
		this.sd = sd;
		this.lang = lang;
		if (this.lang == null) {
			this.lang = SysConstants.CODE_DEFAULT_LANG;
		}
		
	}
	
//	private Difference createDifference() {
//		return new ObjectComparator(oldDate, newDate, sd, lang).new Difference();
//	}
	
	
//	class Difference {
//		
//		private String fieldName;
//		private String oldVal;
//		private String newVal;
//		private List<Difference> coplexDif = new ArrayList<Difference>();
//		private String error;
//		private Integer type = 2;
//		
//		
//		public String getError() {
//			return error;
//		}
//		public void setError(String error) {
//			this.error = error;
//		}
//		public String getFieldName() {
//			return fieldName;
//		}
//		public void setFieldName(String fieldName) {
//			this.fieldName = fieldName;
//		}
//		public String getOldVal() {
//			return oldVal;
//		}
//		public void setOldVal(String oldVal) {
//			this.oldVal = oldVal;
//		}
//		public String getNewVal() {
//			return newVal;
//		}
//		public void setNewVal(String newVal) {
//			this.newVal = newVal;
//		}
//		public List<Difference> getCoplexDif() {
//			return coplexDif;
//		}
//		public void setCoplexDif(List<Difference> coplexDif) {
//			this.coplexDif = coplexDif;
//		}
//		public Integer getType() {
//			return type;
//		}
//		public void setType(Integer type) {
//			this.type = type;
//		}
//		
//	}
	
	
	@SuppressWarnings("unchecked")
	public List<ObjectsDifference> compare(Object oldObj, Object newObj) {
		
		
		
		
		Map<String, Object> mFieldsOld = new HashMap<String,Object>();
		Map<String, Object> mFieldsNew = new HashMap<String,Object>();
		Field[] fieldsOld = null;
		Field[] fieldsNew = null;
		List<ObjectsDifference> diffs = new ArrayList<ObjectsDifference>();
		
		if (oldObj == null && newObj == null) {
			return diffs;
		}
		
		try {
			if (oldObj == null && newObj != null) {
				oldObj = newObj.getClass().newInstance();
			}
			
			if (oldObj != null && newObj == null) {
				newObj = oldObj.getClass().newInstance();
			}
		} catch (Exception e) {
			e.printStackTrace();
			return diffs;
		}
		
		
		try {
			fieldsOld = oldObj.getClass().getDeclaredFields();
			fieldsNew = newObj.getClass().getDeclaredFields();
			for (Field f : fieldsOld) {
				f.setAccessible(true);
				Object value = f.get(oldObj);
//				System.out.println(f.getName()  + "="+ value);
				mFieldsOld.put(f.getName(), value);
			}
			for (Field f : fieldsNew) {
				f.setAccessible(true);
				Object value = f.get(newObj);				
				//System.out.println(f.getName()  + "="+ value);
				mFieldsNew.put(f.getName(), value);
			}
			
			//Тук заповчаме същинската част
			for (Field f : fieldsOld) {
				
				String fName = getFieldName(f);
				String codeClassif = null;
				String fieldClassif = null;
				String maskDate = "dd.MM.yyyy";
				int codeObject = 0;
				
				Object valOld = mFieldsOld.get(fName);
				Object valNew = mFieldsNew.get(fName);
				
				Boolean isSimpleCollection = null;  //По долу приемеме, че ареисте са симпле колектион
				
				if (valOld != null && valOld.getClass().isArray()) {	
					isSimpleCollection = true;	
					  List<Object> list = new ArrayList<Object>();
					  Object[] arr = (Object[]) valOld;					  
				      for(Object tek: arr) {
				         list.add(tek);
				      }
				      valOld = list;
				}
				
				if (valNew != null && valNew.getClass().isArray()) {
					isSimpleCollection = true;
					List<Object> list = new ArrayList<Object>();
					  Object[] arr = (Object[]) valNew;					  
				      for(Object tek: arr) {
				         list.add(tek);
				      }
				      valNew = list;
				}
				
				
				//Сравняваме само тези, които са анотирани с @Journal
				//Ако махнем else-то ще са всички!!!
				if (f.isAnnotationPresent(JournalAttr.class)) {					
					JournalAttr attr= f.getAnnotation(JournalAttr.class);
					
					if (attr.isId().equalsIgnoreCase("true")) {
						continue;
					}
					
					fName = attr.defaultText();
					codeClassif = attr.classifID();
					codeObject = attr.codeObject();					
					maskDate = attr.dateMask();					
					fieldClassif = attr.classifField();
					
					if (valOld instanceof Date && valOld != null) {
						valOld = new SimpleDateFormat(maskDate).format((Date)valOld);						
					}
					
					if (valNew instanceof Date && valNew != null) {						
						valNew = new SimpleDateFormat(maskDate).format((Date)valNew);
					}
					
					
					if (fieldClassif != null && !fieldClassif.equals("none")) {
						String newCodeClassif = extractCodeClassif(oldObj, fieldClassif );
						if (newCodeClassif == null || newCodeClassif.equals("none") || "0".equals(newCodeClassif)) {
							newCodeClassif = extractCodeClassif(newObj, fieldClassif );
						}
						
						if (newCodeClassif != null) {
							codeClassif = newCodeClassif;
						}
					}
					
					
					if(valOld instanceof Collection) {
//						System.out.println("************** " + fName);
						if (isSimpleCollection == null) {
							ParameterizedType stringListType = (ParameterizedType) f.getGenericType();
						    Class<?> stringListClass = (Class<?>) stringListType.getActualTypeArguments()[0];
//						    System.out.println("!!!!!!!!!!!!!!!"+stringListClass+"=="+isWrappedStandard(stringListClass));
						}
					
					}
				}else {
					continue;
				}
				
				
				
				
				
				if (valOld instanceof Collection<?> || valNew instanceof Collection<?>) {
					
					if (isSimpleCollection == null) {
						ParameterizedType stringListType = (ParameterizedType) f.getGenericType();
				        Class<?> stringListClass = (Class<?>) stringListType.getActualTypeArguments()[0];
						if (isWrappedStandard(stringListClass)) {
							isSimpleCollection = true;							
						}else {
							isSimpleCollection = false;							
						}	
					}
					
					List<ObjectsDifference> compareCollectionTypes;
					ObjectsDifference diff = new ObjectsDifference();
					diff.setFieldName(fName);
					
					if (isSimpleCollection != null && isSimpleCollection.equals(true)) {
						compareCollectionTypes=  compareSimpleCollectionType((Collection<? extends Object>)valOld, (Collection<? extends Object>)valNew, fName,codeClassif, codeObject);
					}else {
						compareCollectionTypes = compareCollectionTypes((Collection<? extends Object>)valOld, (Collection<? extends Object>)valNew, fName,codeClassif, codeObject);
					}
					
					//Добавяме само ако сме открили някакви разлики.
					if (compareCollectionTypes!=null && !compareCollectionTypes.isEmpty() && compareCollectionTypes.size()>0) {
						diff.getCoplexDif().addAll(compareCollectionTypes);
						diffs.add(diff);
					}
					
				}else {
					//Не са колекция
					
					
					
					if ("".equals(valOld)) {
						valOld = null;
					}
					
					if ("".equals(valNew)) {
						valNew = null;
					}
					
					
				
					if (valOld == null && valNew == null) {
						//празни са --> еднакви са
						continue;
					}
					
					if (!isSimpleType(f)) {
						ObjectsDifference diff = new ObjectsDifference();
						diff.setFieldName(fName);						
						diff.getCoplexDif().addAll(compare(valOld, valNew));
						if (diff.getCoplexDif().size() > 0) {
							diffs.add(diff);
						}
					}else {
						if (valOld == null && valNew != null) {
							ObjectsDifference diff = new ObjectsDifference();
							diff.setFieldName(fName);
							diff.setOldVal("");
							diff.setNewVal(fromatVal(valNew, codeClassif, codeObject, newDate));
							diffs.add(diff);
						}else {
							if (valOld != null && valNew == null) {
								ObjectsDifference diff = new ObjectsDifference();
								diff.setFieldName(fName);
								diff.setNewVal("");
								diff.setOldVal(fromatVal(valOld, codeClassif,codeObject, oldDate));
								diffs.add(diff);
							}else {
								
								
								
								ObjectsDifference diff = compareSimpleNotNullTypes(valOld, valNew, codeClassif, codeObject);
								if (diff != null) {
									diff.setFieldName(fName);
									diffs.add(diff);
								}
							}
						}
					}
				}
				
				
			}
			
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			JPA.getUtil().closeConnection();
		}
		
		return diffs;
		
	}

	

	

	

	private ObjectsDifference compareSimpleNotNullTypes(Object oldObj, Object newObj, String codeClassif, int codeObject) {
		if (!oldObj.getClass().equals(newObj.getClass())) {
			ObjectsDifference diff = new ObjectsDifference();
			diff.setError("Типовете са различни и не могат да бъдат сравнени !");
			return diff;
		}else {

			if (!oldObj.equals(newObj)) {						
				ObjectsDifference diff = new ObjectsDifference();		
				diff.setOldVal(fromatVal(oldObj, codeClassif, codeObject, oldDate));
				diff.setNewVal(fromatVal(newObj, codeClassif, codeObject, newDate));
				return diff;
			}else {
				return null;
			}
		}
		
		
		
	}
	
	private String fromatVal(Object o, String codeClassif, int codeObject, Date dat) {
		//System.out.println("CC=" + codeClassif);
		
		if (codeObject > 0) {
			String ident = null;
			try {				
				Integer id = Integer.parseInt(""+o);
				
				switch(codeObject) {
				  case OmbConstants.CODE_ZNACHENIE_JOURNAL_DELO:
				    Delo delo = JPA.getUtil().getEntityManager().find(Delo.class, id); // new DeloDAO(ActiveUser.DEFAULT).findById(id);
				    if (delo != null) {
				    	ident = delo.getIdentInfo();			    	
				    }
				    break;
				  case OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC:
					    Doc doc = JPA.getUtil().getEntityManager().find(Doc.class, id); // new DocDAO(ActiveUser.DEFAULT).findById(id);
					    if (doc != null) {
					    	ident =  doc.getIdentInfo();			    	
					    }
					    break;
				  case OmbConstants.CODE_ZNACHENIE_JOURNAL_TASK:
					    Task task = JPA.getUtil().getEntityManager().find(Task.class, id);
					    if (task != null) {
					    	ident =  task.getIdentInfo();			    	
					    }
					    break;
				  case Constants.CODE_ZNACHENIE_JOURNAL_GROUPUSER:
					    AdmGroup group = JPA.getUtil().getEntityManager().find(AdmGroup.class, id);
					    if (group != null) {
					    	ident =  group.getIdentInfo();			    	
					    }
					    break;
				  default:
					  break;
				}
				
				if (ident == null) {
					//Правим още един опит през журнала
					SystemJournal j = (SystemJournal) JPA.getUtil().getEntityManager().createQuery("from SystemJournal where codeObject =:co and idObject = :io and codeAction = :ca")
							.setParameter("co", codeObject)
							.setParameter("io", id)
							.setParameter("ca", SysConstants.CODE_DEIN_IZTRIVANE)
							.getSingleResult();
					if (j != null) {
						ident = j.getIdentObject();
					}
				}
				
				if (ident == null) {
					return "Id= " + id;
				}else {
					return ident + "(Id= " +id + ")";
				}
				
			} catch (Exception e) {
				return ident + " (Грешка при идентификация)";
			}

			
		}else {
			if (codeClassif == null || codeClassif.equalsIgnoreCase("none")) {
				return fromatSimpleVal(o);
			}else {
				return decodeVal(o,codeClassif, dat, lang);
			}
		}
		
		
	}
	
	
	private String fromatSimpleVal(Object o) {
		//TODO да форматираме стойностите	
		if (o instanceof  Date) {
			SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
			return sdf.format((Date)o);
		}
		
		return ""+o;
	}
	
	private String decodeVal(Object val, String codeClassif, Date dat, Integer lang) {
		
		if (dat == null) {
			dat = new Date();
		}
		
		Integer codeClassifInt = null;
		try {
			codeClassifInt = Integer.parseInt(codeClassif);
		} catch (NumberFormatException e1) {
			//Не би трябвало
		}
		
		if (! (val instanceof  Integer) || codeClassifInt == null ) {
			return fromatSimpleVal(val);
		}else {		
			
			try {
				Integer code  = (Integer)val;
				if (code == 0 && codeClassifInt > 0) {
					return "";
				}else {
					
					if (codeClassifInt == -1) {
						//Име на класификация
						return sd.getNameClassification(code, lang);
					}else {					
						return sd.decodeItem(codeClassifInt, code, lang, dat) + " ("+code+")";
					}
					
					
					
				}
				 
				
			} catch (DbErrorException e) {
				return "Db Грешка при разкодиране";
			}
		}
	}
	
	
	private String getFieldName(Field f) {
		//TODO да ползваме анотации
		return f.getName();
	}
	
	private Object findObjectId(Object obj) {

		if (obj == null) {
			return null;
		}
		
		if (isWrappedStandard(obj.getClass())) {
			return obj;
		}
		
		Field[] fields = obj.getClass().getDeclaredFields();
		for (Field f : fields) {
			if (f.isAnnotationPresent(JournalAttr.class)) {		
				f.setAccessible(true);
				JournalAttr attr= f.getAnnotation(JournalAttr.class);
				String isId = attr.isId();
				if (isId != null && isId.equalsIgnoreCase("true")) {
					try {
						//System.out.println("НАМЕРЕНО ИД = " + f.get(obj));
						return f.get(obj);
					} catch (Exception e) {
						return null;
					} 
				}
			}
		}
		
		return null;
		
	}
	
	public static boolean isWrappedStandard(Class<?> type) {
		if (type == String.class || type == Double.class || type == Float.class || type == Long.class ||
			    type == Integer.class || type == Short.class || type == Character.class ||
			    type == Byte.class || type == Boolean.class) {
			return true;
		}
		return false;
	}
	
//	@SuppressWarnings("unchecked")
	private List<ObjectsDifference> compareCollectionTypes(Collection<? extends Object> valOld, Collection<? extends Object> valNew, String parentName,String codeClassif, int codeObject) {
		
		List<ObjectsDifference> diffs = new ArrayList<ObjectsDifference>(); 

		List<Object> oldList = new ArrayList<Object>();
		List<Object> newList = new ArrayList<Object>();
		
		
		if (valOld != null) {
			oldList.addAll((Collection<? extends Object>) valOld);
			//System.out.println("---->" + oldList.size());
		}
		
		if (valNew != null) {
			newList.addAll((Collection<? extends Object>) valNew);
			//System.out.println("---->" + newList.size());
		}
		
		//Търсим променени стари
		for (Object oldObj : oldList) {
			Object idObj = findObjectId(oldObj);
			if (idObj != null) {				
				//Има ид-търсим го в новия списък
				
				String porName = parentName + "(id="+fromatVal(idObj, codeClassif, codeObject,  new Date())+")";
				
				Object newObj = null;
				for (Object tek : newList) {
					Object tekId = findObjectId(tek);
					if (tekId != null && tekId.equals(idObj)) {
						newObj = tek;
						break;
					}
				}
				if (newObj == null) {
					ObjectsDifference diff = new ObjectsDifference();
					diff.setFieldName(porName);
					try {
						newObj = oldObj.getClass().newInstance();
						diff.setCoplexDif(compare(oldObj,newObj));
					} catch (Exception e) {
						//Не би трябвало да се стига
					}
					diff.setType(3);
					diffs.add(diff);
				}else {
					ObjectsDifference diff = new ObjectsDifference();					
					diff.getCoplexDif().addAll(compare(oldObj, newObj));					
					if (diff.getCoplexDif().size() > 0) {
						diff.setFieldName(porName);
						diff.setType(2);
						diffs.add(diff);
					}
					
				}
			}
		}
		
		//Търсим добавени нови
		for (Object newObj : newList) {
			Object idObj = findObjectId(newObj);
			if (idObj != null) {				
				//Има ид-търсим го в стария списък
				
				String porName = parentName + "(id="+fromatVal(idObj, null, 0,null)+")";
				
				Object oldObj = null;
				for (Object tek : oldList) {
					Object tekId = findObjectId(tek);
					if (tekId != null && tekId.equals(idObj)) {
						oldObj = tek;
						break;
					}
				}
				if (oldObj == null) {
					ObjectsDifference diff = new ObjectsDifference();
					diff.setFieldName(porName);					
					diff.setType(1);	
					try {
						oldObj = newObj.getClass().newInstance();
						diff.setCoplexDif(compare(oldObj, newObj));
					} catch (Exception e) {
						//Не би трябвало да се стига
					}										
					diffs.add(diff);
				}
			}
		}
		
		
		
		return diffs;
	}


	/**Сравняване на списъци от кодове и тяхното разкодиране (ако е подаден код на класификация
	 * @param valOld
	 * @param valNew
	 * @param fName
	 * @param codeClassif
	 * @return
	 */
	private List<ObjectsDifference> compareSimpleCollectionType(Collection<? extends Object> valOld,
			Collection<? extends Object> valNew, String fName, String codeClassif, int codeObject) {
		List<ObjectsDifference> diffs = new ArrayList<ObjectsDifference>(); 

		//1. ако в стярия няма нищо а в новия има
		if ( (valOld == null || valOld.isEmpty())
				&& valNew!=null && !valNew.isEmpty()) {
			for (Object object : valNew) {
				ObjectsDifference diff=new ObjectsDifference();
				diff.setOldVal(null);
				diff.setNewVal(codeClassif!=null?fromatVal(object,codeClassif, codeObject, newDate):(String)object);
				diffs.add(diff);
			}
			//2. ако в стария има а в новия всичко е махнато
		}else 	if ( (valNew == null || valNew.isEmpty())
				&& valOld!=null && !valOld.isEmpty()) {
			for (Object object : valOld) {
				ObjectsDifference diff=new ObjectsDifference();
				diff.setOldVal(codeClassif!=null?fromatVal(object,codeClassif,codeObject, newDate):(String)object);
				diff.setNewVal(null);
				diffs.add(diff);
			}
			//3.има разлики в новия и стария
		} else {
			//търсим тези които ги няма в ножия списък т.е. изтрити
			for (Object object : valOld) {
				ObjectsDifference diff=new ObjectsDifference();
				
				if (valNew != null && !valNew.contains(object)) {
					diff.setOldVal(codeClassif!=null?fromatVal(object,codeClassif,codeObject,oldDate):(String)object);
					diff.setNewVal(null);
					diffs.add(diff);
				}
			}
			//Търсим теси които ги има самов новия т.е. добавени
			for (Object object : valNew) {
				ObjectsDifference diff=new ObjectsDifference();
				
				if (valOld != null && !valOld.contains(object)) {
					diff.setOldVal(null);
					diff.setNewVal(codeClassif!=null?fromatVal(object,codeClassif,codeObject,newDate):(String)object);
					diffs.add(diff);
				}
			}
		}
		
			return diffs;
	}
	
	private String extractCodeClassif(Object obj, String fieldClassif) {
		
		Field[] fields = obj.getClass().getDeclaredFields();
		for (Field f : fields) {
			if (f.getName().equalsIgnoreCase(fieldClassif)) {
				f.setAccessible(true);
				try {
					Object val = f.get(obj);
					if (val == null) {
						return "none"; 
					}else {
						return "" + f.get(obj);
					}
				} catch (Exception e) {
					continue;
				}
			}
		}
		return null;
	}
	
	
	private boolean isSimpleType(Field f) {
		if (f == null || f.getType() == null || f.getType().isPrimitive()) {
			return true;
		}else {
			String typeName = "" + f.getType();
			if (typeName.contains("class java.")){
				return true;
			}else {
				return false;
			}
		}
	}
	
	
}
	


