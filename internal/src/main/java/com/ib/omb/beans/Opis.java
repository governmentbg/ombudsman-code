package com.ib.omb.beans;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.model.SelectItem;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.primefaces.component.export.PDFOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.customexporter.CustomExpPreProcess;
import com.ib.indexui.pagination.LazyDataModelSQL2Array;
import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.db.dao.DeloDAO;
import com.ib.omb.db.dao.DeloDeloDAO;
import com.ib.omb.db.dao.DeloDocDAO;
import com.ib.omb.db.dao.DocDAO;
import com.ib.omb.db.dao.OpisDAO;
import com.ib.omb.db.dto.Delo;
import com.ib.omb.db.dto.DeloDelo;
import com.ib.omb.db.dto.Doc;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.system.db.DialectConstructor;
import com.ib.system.db.JPA;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.DbErrorException;




/**
 * Показване на документите в том
 * 
 * @author ЛМ
 *
 */
@SuppressWarnings("cdi-ambiguous-dependency")
@Named
@ViewScoped
public class Opis   extends IndexUIbean  implements Serializable, Comparator<TempOpis>,Comparable<TempOpis>  {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2934740148410006322L;
	private static final Logger LOGGER = LoggerFactory.getLogger(Opis.class);
	public static final String  ERRDATABASEMSG = "general.errDataBaseMsg";
	public static final String	LABELS			= "labels";
	public static final String	BEANMSG			= "beanMessages";
	public static final String	OPISNOTOM		= "opisTom.noTom";
	public static final String	OPISNOMTOM		= "opis.nomerTom";
	public static final String	OPISFORMTOMNOM	= "opisForm:tomNomDoc";
	public static final String	FROMDELODELO	= " FROM delo_delo , delo ";
	
	
	private Delo delo;
	private transient DeloDAO		dao;
	
	private boolean obshtodostap=true;
	private boolean hasRazdeli=true;
	private boolean hasToms=true;
	
	
	private transient DeloDocDAO		daoDoc;
	private LazyDataModelSQL2Array deloDocList;
	
	private DeloDelo currentDeloDelo=new DeloDelo();
	private transient DeloDeloDAO		daoDeloDelo;
	private LazyDataModelSQL2Array deloDeloList;
	
	private Date decodeDate = new Date(); 
	private transient Object[] selectedDeloP=null;
	
	private List<Doc> docs;
	private TimeZone timeZone = TimeZone.getDefault();
	private String sp=" ";
	private Integer docId;
	private Integer tomId;
	private Integer zaTomId;

	
	private transient List<Object[]> opisList;
	private transient List<Object[]> prepList;
    private List<TempOpis>  opList;
	
	private transient List<Object[]> tomIdList ;
	private List <SelectItem>     	 tomList 	= new ArrayList<>();
	private boolean hasStranici=true;
         
	private HashMap<Integer, Integer> tomIds;
	private HashMap<Integer, Boolean> prepIds;
	private SystemData sd;	


	
	
	@PostConstruct
	void initData()  {
		LOGGER.debug("!!! PostConstruct Opis !!!");

		prepIds=new HashMap<>();
		dao = new DeloDAO(getUserData());
		daoDoc=new DeloDocDAO(getUserData());
		daoDeloDelo=new DeloDeloDAO(getUserData());
			
		String param = JSFUtils.getRequestParameter("idObj");
		docId = null;
		if (param != null && !param.isEmpty()){
			docId = Integer.valueOf(param);
			loadDelo(docId); // зарежда данните за документа
			if (!hasToms) {
//				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(BEANMSG, OPISNOTOM));					
//				return;
			}
		} 
		setTomIdList(new  ArrayList<>());
		try {
			sd = (SystemData) getSystemData();
			tomIdList= dao.GetToms(docId);
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при зареждане на томовете! ", e);				
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	

		}finally {
			 JPA.getUtil().closeConnection();
		}
		if (tomIdList.isEmpty()) {
			tomList.add(new SelectItem(1L,"Том 1"));
	//		System.out.println("tomIdList.isEmpty");
			//dolnoto е затворено по искане на МАлексиева DocuWork 0015112 Когато преписката е без томове, по подразбиране за номер на том да се слага 1.
//			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(BEANMSG, OPISNOTOM));					
//			return;
		}else {
		//	System.out.println("tomIdList.isNoEmpty");

			for (Object b :tomIdList) {
				if(b!=null ) {
					String a = b.toString();
					Long l=Long.parseLong(b.toString());
					
					tomList.add(new SelectItem(l,"Том "+a));
				}
			}
		}
		//dolnoto е затворено по искане на МАлексиева DocuWork 0015112 Когато преписката е без томове, по подразбиране за номер на том да се слага 1.
		//ako hasToms е true, па няма томове
//		if (!hasToms) {
//			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(BEANMSG, OPISNOTOM));					
//		}
	}
	private boolean forPrep=false;

	
	@SuppressWarnings("unchecked")
	private void loadPrep() {
		prepList=null;
		try { 
			String qq="select TOM_NOMER,DELO_ID,INPUT_DELO_ID "
					+ " from DELO_DELO ";
			Query query = createNativeQuery(qq);
			prepList= query.getResultList();
		} catch (Exception e) {
			JSFUtils.addErrorMessage(getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e);
			LOGGER.error(e.getMessage(), e);
		} finally {
				 JPA.getUtil().closeConnection();
		}
	}


	public void actionSearchTom(){
		setTomId(zaTomId);

	}

	
    public void onRowReorder() {
        opList=dobaviTekStr(opList);
    }
    
    public void sort() {
//      String a = event.getSortColumn().getColumnKey();
	  opList=dobaviTekStr(opList);

  }


	@SuppressWarnings("unchecked")
	public void actionSearch() throws DbErrorException{
		 if (zaTomId==null) {
				JSFUtils.addMessage(OPISFORMTOMNOM, FacesMessage.SEVERITY_ERROR,getMessageResourceString(LABELS, OPISNOMTOM));
			 return;
		 }
//			sd = (SystemData) getSystemData();

		 	List<Object[]> prepListTmp=new ArrayList<>();
			prepList=null;
			try { 
//				//Четем вида на преписката
//				String qq="select DELO_TYPE  from DELO WHERE DELO.DELO_ID=:DELO_ID";
//				Query queryType = createNativeQuery(qq).setParameter("DELO_ID", Integer.valueOf(docId));
//				BigDecimal deloType=   (BigDecimal) queryType.getSingleResult();
//				Integer deloTypInt=deloType.intValue();
//				int lang = getCurrentLang();
//				String deloTypeTxt=(String) sd.decodeItem(OmbConstants.CODE_CLASSIF_DELO_TYPE, deloTypInt, lang, null);
//				System.out.println("deloTypeTxt:"+deloTypeTxt);
//	
//				//Четем датата на преписката
//				 qq="select delo_date  from DELO WHERE DELO.DELO_ID=:DELO_ID";
//				queryType = createNativeQuery(qq).setParameter("DELO_ID", Integer.valueOf(docId));
//				Date deloDate=   (Date) queryType.getSingleResult();
//				SimpleDateFormat sdf=new SimpleDateFormat("dd.MM.yyyy");
//				String deloDateF = sdf.format(deloDate);
//				System.out.println("deloTypeTxt:"+deloDateF);
//				//Четем регистратурата
//				 qq="select REGISTRATURA  from REGISTRATURI,DELO WHERE delo.REGISTRATURA_ID=REGISTRATURI.REGISTRATURA_ID and  DELO.DELO_ID=:DELO_ID";
//				queryType = createNativeQuery(qq).setParameter("DELO_ID", Integer.valueOf(docId));
//				String registratura=   (String) queryType.getSingleResult();
//				System.out.println("registratura:"+registratura);
				
				
				String qq = "select DELO_ID,INPUT_DELO_ID "
						+ " from DELO_DELO WHERE TOM_NOMER=:tom";
				Query query = createNativeQuery(qq).setParameter("tom", zaTomId);
				prepListTmp= query.getResultList();
			} catch (Exception e) {
				JSFUtils.addErrorMessage(getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e);
				LOGGER.error(e.getMessage(), e);
			} finally {
					 JPA.getUtil().closeConnection();
			}
			prepList=new ArrayList<>();
			for (int i = 0; i < prepListTmp.size(); i++) {
				Object[] o = prepListTmp.get(i);
				Integer i0 = Integer.parseInt(String.valueOf(o[0]));
				Integer i1 = Integer.parseInt(String.valueOf(o[1]));
				Object[] cc=new Object[2];
				cc[0]=i0;cc[1]=i1;
				prepList.add(cc);
			}
			List<Object[]> prepIdList4=null;
			if (forPrep) {
				prepIdList4=OldFor(prepList);// До 4 нива на влагане
			}else {
				prepIdList4=NewAll(prepList);// Неограничено влагане
			}
	
			opisList = new ArrayList<>();
			opList = null;
			
			List<Object[]> tempDocList;
			if (prepIdList4==null) {// Няма подчинени преписки за указания том
				tempDocList=loadDoc(docId, zaTomId);
				opisList.addAll(tempDocList);
				opList = new ArrayList<>();
				opList =moveToOpList(opisList);
				opList=dobaviTekStr(opList);
				return;	
			}
			//зареждаме документите в тома
			tempDocList=loadDoc(docId,zaTomId);
			opisList.addAll(tempDocList);
			
			for (int i = 0; i < prepIdList4.size(); i++) {
				Object[] a = prepIdList4.get(i);
				if (a[0]==null) continue;
				for (int j = 0; j < a.length; j++) {
					Integer aj = Integer.parseInt(String.valueOf(a[j]));
					prepIds.put(aj, false);//Сетваме всички преписка че не са минали
					
				}
			}
			if (forPrep) {
				prepFor(prepIdList4);
			}else {
				prepAll(prepIdList4);
			}
		if (opisList.isEmpty()) {
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(BEANMSG, OPISNOTOM));					
		return;
		}
			opList = new ArrayList<>();
			opList = moveToOpList(opisList);
			opList =shellSortOpis((ArrayList<TempOpis>) opList);
			opList=dobaviTekStr(opList);
	}	

	public  List<TempOpis> shellSortOpis(List<TempOpis> lst){
		int h = 0;
		int  i;
		int  j;
		List<TempOpis> ttt=lst;
		while(2*(3*h+1)  <= ttt.size()) {
			h = 3*h+1;
		}
		while(h  >  0){
			for(i = h; i  <  ttt.size(); i++){
				TempOpis docx=ttt.get(i);
				for(j = i-h; j  >= 0; j = j-h){
					TempOpis docj=ttt.get(j);	
					Date dx= docx.getDataVl();
					Date dj= docj.getDataVl();
					if(dx.before(dj)){
						ttt.set(j+h, docj);	
					}else{
						break;
					}
					ttt.set(j, docx);		
				}
			}
			h = h/3;
		}
		return ttt;
	}

	public  List<Object[]> shellSort(List<Object[]> lst){
		int h = 0;
		int i;
		int j;
		List<Object[]> ttt=lst;
		while(2*(3*h+1)  <= ttt.size()) {
			h = 3*h+1;
		}
		while(h  >  0){
			for(i = h; i  <  ttt.size(); i++){
				Object[] docx=ttt.get(i);
				for(j = i-h; j  >= 0; j = j-h){
					Object[] docj=ttt.get(j);	
					Date dx=(Date) docx[5];
					Date dj=(Date) docj[5];
					if(dx.before(dj)){
						ttt.set(j+h, docj);	
					}else{
						break;
					}
					ttt.set(j, docx);		
				}
			}
			h = h/3;
		}
		return ttt;
	}

	private void prepAll(List<Object[]> prepIdList4) throws DbErrorException {
		for (int i = 0; i < prepIdList4.size(); i++) {
			Object[] a = prepIdList4.get(i);
			if (a[0]==null) continue;
			Integer ai = Integer.parseInt(String.valueOf(a[1]));
			Integer brStr=0;
			if (Boolean.FALSE.equals(prepIds.get(ai))) {	
				brStr=0;
				List<Object[]> tempPrepList = loadOnePrep(docId,ai,zaTomId);
				Integer brStrTmp = loadStrPrep(ai, zaTomId);
				if (brStrTmp!=null) {
					brStr+=brStrTmp;
				}
				List<Object[]> tempPrList=dobaviStr(tempPrepList,brStr);
				opisList.addAll(tempPrList);
				prepIds.put(ai, true);
			}
			brStr=0;
			Object[] w = null;
			for (int j = 2; j < a.length; j++) {
				Integer aj = Integer.parseInt(String.valueOf(a[j]));
				if (Boolean.FALSE.equals(prepIds.get(ai))) {	
					Integer brStrTmp = loadStrPrep(aj, zaTomId);
					if (brStrTmp!=null) {
						brStr+=brStrTmp;
					}
					for (Object[] object : opisList) {
						String dok=String.valueOf(object[1]);
						if (dok.equals("Док")) continue;
						Integer idPrep = Integer.parseInt(String.valueOf(object[0]));
						if (idPrep.equals(ai)) {
							w = object;break;
						}
					}
					prepIds.put(aj, true);	
				}
			}
			if(brStr>0) {
				List<Object[]> tempPList=new ArrayList<>();
				tempPList.add(w);
				opisList.remove(w);
				List<Object[]> tempPrList=dobaviStr(tempPList,brStr);
				opisList.addAll(tempPrList);
			}
		}
	}


	private void prepFor(List<Object[]> prepIdList4) throws DbErrorException {
		String iDta="";
		List<Object[]> tempPrepList=new ArrayList<>();

		for (int i = 0; i < prepIdList4.size(); i++) {
			Object[] a = prepIdList4.get(i);
			if (a[0]==null) continue;
			Integer ai = Integer.parseInt(String.valueOf(a[1]));
			int q = a.length;

			if (iDta.indexOf(String.valueOf(ai))<0) {
				if (q==2) {
					iDta+=" "+String.valueOf(ai);
					if( !prepIds.get(ai)) {
						Integer brStr=0;
						tempPrepList=loadOnePrep(docId,ai,zaTomId);
						Integer brStrTmp = loadStrPrep(ai, zaTomId);
						if (brStrTmp!=null) {
							brStr+=brStrTmp;
						}
						List<Object[]> tempPrList=dobaviStr(tempPrepList,brStr);
						opisList.addAll(tempPrList);
					//	System.out.println("11");
						prepIds.put(ai, true);
					 continue;
					
					}
				}
				if (q>2) {
					iDta+=" "+String.valueOf(ai);
					if( !prepIds.get(ai)) {
						Integer brStr=0;
						tempPrepList=loadOnePrep(docId,ai,zaTomId);
						for (int j = 0; j < a.length; j++) {
							Integer brStrTmp = loadStrPrep(Integer.parseInt(String.valueOf(a[j])), zaTomId);
							if (brStrTmp!=null) {
								brStr+=brStrTmp;
							}
						}
						List<Object[]> tempPrList=dobaviStr(tempPrepList,brStr);
						opisList.addAll(tempPrList);
					//	System.out.println("12");
						prepIds.put(ai, true);
						continue;
					}
				}
			}
			if (iDta.indexOf(String.valueOf(ai))>0) {
				Integer brStr=0;
				Object[] w = null;
				for (Object[] object : opisList) {
					Integer idPrep = Integer.parseInt(String.valueOf(object[0]));
					if (idPrep.equals(ai)) {
						w = object;break;
					}
				}
				for (int j = 2; j < a.length; j++) {
					if( !prepIds.get(ai)) {
						Integer brStrTmp = loadStrPrep(Integer.parseInt(String.valueOf(a[j])), zaTomId);
						if (brStrTmp!=null) {
							brStr+=brStrTmp;
						}
					}
				}
				List<Object[]> tempPList=new ArrayList<>();
				tempPList.add(w);
				opisList.remove(w);
				
				List<Object[]> tempPrList=dobaviStr(tempPList,brStr);
				opisList.addAll(tempPrList);
			}
		}
	}


	private List<Object[]> NewAll(List<Object[]> prepList2) {
		 List<Object[]> prepIdList=new ArrayList<>();
		 List<Object[]> prepIdList3=new ArrayList<>();
		 List<Object[]> prepIdList4=new ArrayList<>();
			for (int i = 0; i < prepList2.size(); i++) {
				Object[] c=new Object[8];
				c[0]=docId;
				Object[] o = prepList2.get(i);
				Integer ii = (Integer) o[0];
				if (o[0]==null) continue;
				if (docId.equals(ii)) {
					int j=1;
					Integer i2 = (Integer) o[1];
					c[j]=i2;j+=1;
					Integer i3=nextId(i2,prepList2);
					while ((i3!=null)){
						c[j]=i3;j+=1;
						i3 = nextId(i3,prepList2);
					}  ;
					if (i3==null) {
						Integer t0=(Integer) c[j-2];
						Integer t1=(Integer) c[j-1];
						for (int m = 0; m < prepList2.size(); m++) {
							Object[] oo = prepList2.get(m);
							Integer p0=(Integer) oo[0];
							Integer p1=(Integer) oo[1];
							if (p0.equals(t0)&&p1.equals(t1)) {
								prepList2.remove(m);break;
							}
						}
						prepIdList.add(c);
						i = -1;
					}
				}
			}
			for (int i = 0; i < prepIdList.size(); i++) {
				Object[] a = prepIdList.get(i);
				int j=0;
				for (int k2 = 0; k2 < 8; k2++) {
					if(a[k2]!=null) {
						j+=1;
					}
				}
				Object[] c=new Object[j];
				for (int m2 = 0; m2 < j; m2++) {
					c[m2]=a[m2];
				}
				prepIdList3.add(c);
			}
		return prepIdList3;
}


	private List<TempOpis> moveToOpList(List<Object[]> opisList2) {
		List<TempOpis>  opList1 = new ArrayList<>();
	
		for (int i = 0; i < opisList2.size(); i++) {
			Long id = null;
			String regNomer = null;
			Date dataReg = null;
			String otnosno = null;
			Long brLista = null;
			Date dataVl = null;
			String docTom = null;
			String tekStr= null;
			TempOpis tmp= new TempOpis(id, regNomer, docTom, dataReg, otnosno, brLista, dataVl, tekStr);
			Object[] op = opisList2.get(i);
			Object ii = op[0];
			tmp.setId(Long.valueOf(i));// вместо id слагам позицията
			tmp.setDocTom((String) op[1]);
			tmp.setRegNomer((String) op[2]);
			if (op[3] instanceof java.util.Date) {
				tmp.setDataReg((java.util.Date)op[3]);
		    }
			tmp.setOtnosno((String) op[4]);
			if ( op[6]!=null) {
				ii = op[6];
				tmp.setBrLista(Long.parseLong(ii .toString()));
			}
			if (op[5] instanceof java.util.Date) {
				tmp.setDataVl((java.util.Date) op[5]);
			}
			opList1.add(tmp);
		}
		return opList1;
	
	}


	private List<Object[]> OldFor(List<Object[]> prepListTmp) throws DbErrorException {
		 List<Object[]> prepIdList=new ArrayList<>();
		 List<Object[]> prepIdList3=new ArrayList<>();
		 List<Object[]> prepIdList4=new ArrayList<>();
			for (int i = 0; i < prepListTmp.size(); i++) {
				Object[] b=new Object[2];

				Object[] o = prepListTmp.get(i);
				 Integer a1 = (Integer) o[0];
				if (o[0]==null) continue;
				if (docId.equals(a1)) {
					b[0]=docId;b[1]=o[1];
					prepIdList.add(b);
				}
			}
			if (prepIdList.size()==0) {// Няма вложена преписка
				opisList = new ArrayList<>();
				opList = null;
				
				List<Object[]> tempDocList=new ArrayList<>();
				tempDocList=loadDoc(docId,zaTomId);
				opisList.addAll(tempDocList);
				opList=zarediOpList(opisList);
				opList=dobaviTekStr(opList);
				return null;
			}
			prepIdList3.addAll(prepIdList);
			for (Object[] objects : prepIdList) {
				Object a1 =objects[1];
				Integer ii = Integer.parseInt(String.valueOf(a1));
				for (int i = 0; i < prepListTmp.size(); i++) {
					Object[] b=new Object[3];
					Object[] o = prepListTmp.get(i);
					Object a2 = o[0];
					if (o[0]==null) continue;
					Integer ii2 = Integer.parseInt(String.valueOf(a2));
					if (ii2.equals(ii)) {
						b[0]=docId;b[1]=objects[1];b[2]=o[1];
						prepIdList3.remove(objects);
						prepIdList3.add(b);
					}
				}
			}
			
			prepIdList4.addAll(prepIdList3);
			for (Object[] objects : prepIdList3) {
				if (objects.length<3) continue;
				Object a1 =objects[2];
				Integer ii = Integer.parseInt(String.valueOf(a1));
				for (int i = 0; i < prepListTmp.size(); i++) {
					Object[] b=new Object[4];
					Object[] o = prepListTmp.get(i);
					Object a2 = o[0];
					if (o[0]==null) continue;
					Integer ii2 = Integer.parseInt(String.valueOf(a2));
					if (ii2.equals(ii)) {
						b[0]=docId;b[1]=objects[1];b[2]=objects[2];b[3]=o[1];
						prepIdList4.remove(objects);
						prepIdList4.add(b);
					}
				}
			}
		return prepIdList4;
	}


	private Integer nextId(Integer zaId2, List<Object[]> prepList2) {
		Integer tmp = null;
		for (int i = 0; i < prepList2.size(); i++) {
			Object[] o = prepList2.get(i);
			Object a1 = o[0];
			if (o[0]==null) continue;
			int ii = Integer.parseInt(String.valueOf(a1));
			if (zaId2.equals(ii)) {
				Object a2 = o[1];

				 tmp = Integer.parseInt(String.valueOf(a2));
				 break;
			}

		}
		return tmp;
	}


	private List<Object[]> dobaviStr(List<Object[]> tempPrepList, Integer brStrTmp) {
		Object[] a1 = new  Object[7];
		Object[] a2 = new  Object[7];
		 a2 = tempPrepList.get(0);
		a1[0] =a2[0];
		a1[1] =a2[1];
		a1[2] =a2[2];
		a1[3] =a2[3];
		a1[4] =a2[4];
		a1[5] =a2[5];
		if (a2[6]!=null) {
			a1[6] =Integer.parseInt(String.valueOf(a2[6]))+brStrTmp;
		}else {
			a1[6] =brStrTmp;
		}
		List<Object[]> tempPrList=new ArrayList<>();
		tempPrList.add(a1);
	return tempPrList;
}


	@SuppressWarnings("unchecked")
	private List<Object[]> loadOnePrep(Integer docId2, int ai, Integer zaTomId2) throws DbErrorException {
		List<Object[]> temp=new ArrayList<>();
		String dialect = JPA.getUtil().getDbVendorName();

		try {
			StringBuilder select = new StringBuilder();
			select.append(" SELECT DISTINCT delo.DELO_ID a0, 'Преписка'    а11, delo.RN_DELO a2, delo.DELO_DATE a3,");
			select.append(DialectConstructor.limitBigString(dialect, "delo.DELO_NAME", 300) +" a4,");
			select.append(" delo_delo.INPUT_DATE a5,null a6 ");
			select.append(FROMDELODELO);
			select.append(" WHERE delo.DELO_ID= delo_delo.INPUT_DELO_ID ");
			select.append(" and DELO_delo.DELO_id =:docId ");
//			select.append(" and delo_delo.TOM_NOMER=:tom ");//Премахнато на 04.08.2021 по искане на МАлексиева - да не се гледат томовете на вложените преписки
			select.append(" and delo.DELO_ID=:ai ");

			
//			String qq= " SELECT DISTINCT delo.DELO_ID a0, 'Преписка'    а11, delo.RN_DELO a2, delo.DELO_DATE a3,"
//			+ " DialectConstructor.limitBigString(dialect, \"delo.DELO_NAME\", 300) a4,"
////			+ " TO_CHAR(delo.DELO_NAME) a4,"
//			+ " delo_delo.INPUT_DATE a5,null a6 "
//			+ FROMDELODELO
//			+ " WHERE delo.DELO_ID= delo_delo.INPUT_DELO_ID "
//			+ " and DELO_delo.DELO_id =:docId "
//			+ " and delo_delo.TOM_NOMER=:tom "
//			+ " and delo.DELO_ID=:ai "
//			+ " ";
			String qq=select.toString();
//			Query query = createNativeQuery(qq).setParameter("docId", docId2).setParameter("tom", zaTomId2).setParameter("ai", ai);;//Премахнато на 04.08.2021 по искане на МАлексиева - да не се гледат томовете на вложените преписки

			Query query = createNativeQuery(qq).setParameter("docId", docId2).setParameter("ai", ai);;
			temp =  query.getResultList();			
		} catch (Exception e) {
			throw new DbErrorException("Грешка при извличане на документи за опис!", e);
		}
			return temp;
		}



	private List<TempOpis> zarediOpList(List<Object[]> opisList2) {
		if (opisList2.isEmpty()) {
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(BEANMSG, OPISNOTOM));					
			return null;
		}
		ArrayList opListTmp = new ArrayList<>();
	
		for (int i = 0; i < opisList2.size(); i++) {
			Long id = null;
			String regNomer = null;
			Date dataReg = null;
			String otnosno = null;
			Long brLista = null;
			Date dataVl = null;
			String docTom = null;
			String tekStr= null;
			TempOpis tmp= new TempOpis(id, regNomer, docTom, dataReg, otnosno, brLista, dataVl, tekStr);
			Object[] op = opisList2.get(i);
			Object ii = op[0];
			tmp.setId(Long.valueOf(i));// вместо id слагам позицията
			tmp.setDocTom((String) op[1]);
			tmp.setRegNomer((String) op[2]);
			if (op[3] instanceof java.util.Date) {
				tmp.setDataReg((java.util.Date)op[3]);
		    }
			tmp.setOtnosno((String) op[4]);
			if ( op[6]!=null) {
				ii = op[6];
				tmp.setBrLista(Long.parseLong(ii .toString()));
			}
			if (op[5] instanceof java.util.Date) {
				tmp.setDataVl((java.util.Date) op[5]);
			}
			opListTmp.add(tmp);
		}
//		opListTmp=dobaviTekStr(opList);
	return opListTmp;
	}



	private Integer loadStrPrep(Object idPrep, Object tom) {

		String qq=  " SELECT SUM(CASE WHEN DOC.COUNT_SHEETS IS NULL THEN 1 ELSE DOC.COUNT_SHEETS"
				+ " + ( SELECT "//Ново за да взема бр стр от приложенията
				+ " CASE " //Ново за да взема бр стр от приложенията
				+ " WHEN SUM(DOC_PRIL.COUNT_SHEETS) IS NULL " //Ново за да взема бр стр от приложенията
				+ " THEN 0 " //Ново за да взема бр стр от приложенията
				+ " ELSE SUM(DOC_PRIL.COUNT_SHEETS) " //Ново за да взема бр стр от приложенията
				+ " END  " //Ново за да взема бр стр от приложенията
				+ "  FROM DOC_PRIL " //Ново за да взема бр стр от приложенията
				+ "  WHERE DOC_PRIL.DOC_ID=DOC.DOC_id ) " //Ново за да взема бр стр от приложенията
				+ " END ) "
				+ " FROM delo_DOC  ,doc ,delo "
		+ " where DOC.DOC_id  =delo_DOC.INPUT_DOC_ID AND delo_DOC.DELO_id=DELO.DELO_id "
//		+ " AND DELO_DOC.TOM_NOMER=:tom	 " //Премахнато на 04.08.2021 по искане на МАлексиева - да не се гледат томовете на вложените преписки
		+ " and DELO.DELO_id =:docId "
//		+ "   ( SELECT DISTINCT delo.DELO_ID a0 "
//		+ FROMDELODELO
//		+ " WHERE delo.DELO_ID= delo_delo.INPUT_DELO_ID AND DELO_delo.DELO_id=:docId "
//		+ " AND delo_delo.TOM_NOMER=:tom ) "
		+ " ";

//		
//		String qq=  " SELECT SUM( DOC.COUNT_SHEETS) FROM delo_DOC  ,doc ,delo "
//		+ " where DOC.DOC_id  =delo_DOC.INPUT_DOC_ID AND delo_DOC.DELO_id=DELO.DELO_id AND DELO_DOC.TOM_NOMER=:tom	 "
//		+ " and DELO.DELO_id in "
//		+ "   ( SELECT DISTINCT delo.DELO_ID a0 "
//		+ FROMDELODELO
//		+ " WHERE delo.DELO_ID= delo_delo.INPUT_DELO_ID AND DELO_delo.DELO_id=:docId "
//		+ " AND delo_delo.TOM_NOMER=:tom ) "
//		+ " ";

		//Query query = createNativeQuery(qq).setParameter("docId", idPrep).setParameter("tom", tom);//Премахнато на 04.08.2021 по искане на МАлексиева - да не се гледат томовете на вложените преписки
	//	System.out.println("qq2:"+qq);
		Query query = createNativeQuery(qq).setParameter("docId", idPrep);
		BigDecimal temp1 =  (BigDecimal) query.getSingleResult();
		Integer temp =0;
		if (temp1!=null) {
			temp = Integer.valueOf(temp1.intValue());
		}
		return  temp;
	}


	@SuppressWarnings("unchecked")
	private List<Object[]> loadPrep(Integer docId2, Integer zaTomId2) throws DbErrorException {

		List<Object[]> temp=new ArrayList<>();
		try {
			String qq= " SELECT DISTINCT delo.DELO_ID a0, 'Преписка'    а11, delo.RN_DELO a2, delo.DELO_DATE a3, "
				+ " DialectConstructor.limitBigString(dialect, \"delo.DELO_NAME\", 300) a4,"
//				+ "TO_CHAR(delo.DELO_NAME) a4,"
			+ " delo_delo.INPUT_DATE a5 "
			+ ",  CASE "
			+ " WHEN "
				+ " ( SELECT SUM( DOC.COUNT_SHEETS) FROM delo_DOC  ,doc ,delo "
				+ " where DOC.DOC_id  =delo_DOC.INPUT_DOC_ID AND delo_DOC.DELO_id=DELO.DELO_id AND DELO_DOC.TOM_NOMER=:tom"
				+ " and DELO.DELO_id in "
				+ "   ( SELECT DISTINCT delo.DELO_ID a0 "
				+ FROMDELODELO
				+ " WHERE delo.DELO_ID= delo_delo.INPUT_DELO_ID AND DELO_delo.DELO_id=:docId "
				+ " AND delo_delo.TOM_NOMER=:tom )"
				+ " ) IS NULL "
			+ " THEN 1 "
			+ " ELSE "
			+ " ( SELECT SUM( DOC.COUNT_SHEETS) FROM delo_DOC  ,doc ,delo "
			+ " where DOC.DOC_id  =delo_DOC.INPUT_DOC_ID AND delo_DOC.DELO_id=DELO.DELO_id AND DELO_DOC.TOM_NOMER=:tom	 "
			+ " and DELO.DELO_id in "
			+ "   ( SELECT DISTINCT delo.DELO_ID a0 "
			+ FROMDELODELO
			+ " WHERE delo.DELO_ID= delo_delo.INPUT_DELO_ID AND DELO_delo.DELO_id=:docId "
			+ " AND delo_delo.TOM_NOMER=:tom ) )"
			+ " END a6 "
			+ FROMDELODELO
			+ " WHERE delo.DELO_ID= delo_delo.INPUT_DELO_ID "
			+ " and DELO_delo.DELO_id =:docId "
			+ " and delo_delo.TOM_NOMER=:tom "
			+ " ";

			Query query = createNativeQuery(qq).setParameter("docId", docId2).setParameter("tom", zaTomId2);
			temp = query.getResultList();			
		} catch (Exception e) {
			throw new DbErrorException("Грешка при извличане на документи за опис!", e);
		}
		
	return temp;
}


	@SuppressWarnings("unchecked")
	private List<Object[]> loadDoc(Integer docId2, Integer zaTomId2) throws DbErrorException {
		List<Object[]> temp=new ArrayList<>();
		String dialect = JPA.getUtil().getDbVendorName();

		try { 
			
			StringBuilder select = new StringBuilder();
			select.append("select DOC.DOC_id a0,'Док' a11, "+DocDAO.formRnDocSelect("DOC.", dialect)+" a2, doc.doc_date a3,");
			select.append(DialectConstructor.limitBigString(dialect, "DOC.OTNOSNO", 300) +" a4,");
			select.append(" delo_DOC.INPUT_DATE a5 ");
			select.append(" , CASE ");
			select.append(" WHEN DOC.COUNT_SHEETS is null ");
			select.append(" THEN 1 ");
			select.append(" ELSE ");
//			select.append(" DOC.COUNT_SHEETS ");//STAROTO
			select.append(" DOC.COUNT_SHEETS + (SELECT CASE WHEN sum(DOC_PRIL.COUNT_SHEETS) IS NULL THEN 0 ELSE sum(DOC_PRIL.COUNT_SHEETS) END FROM DOC_PRIL WHERE DOC_PRIL.DOC_ID=DOC.DOC_id) ");//NOVOTO
			select.append(" end  a6 ");
			select.append(" from delo_DOC  ,doc ,delo  ");//STAROTO
//			select.append(" from delo_DOC  ,doc ,delo , DOC_PRIL ");//NOVOTO
			select.append(" where DOC.DOC_id  =delo_DOC.INPUT_DOC_ID ");
			select.append(" AND delo_DOC.DELO_id=DELO.DELO_id ");
			select.append(" AND delo_DOC.DELO_id=DELO.DELO_id ");
			select.append(" and DELO.DELO_id =:docId ");
			select.append(" and DELO_DOC.TOM_NOMER=:tom ");
//			select.append(" AND DOC.DOC_id =DOC_PRIL.DOC_id ");//NOVOTO
			
//			String qq="select DOC.DOC_id a0,'Док' a11, DOC.rn_doc a2, doc.doc_date a3,"
//					+ " DialectConstructor.limitBigString(dialect, \"DOC.OTNOSNO\", 300) a4,"
////					+ " TO_CHAR(doc.OTNOSNO) a4,"
//					+ " delo_DOC.INPUT_DATE a5 "
////					+ " ,DOC.COUNT_SHEETS     a6 "
//					+ " , CASE "
//					+ " WHEN DOC.COUNT_SHEETS is null "
//					+ " THEN 1 "
//					+ " ELSE "
//					+ " DOC.COUNT_SHEETS "
//					+ " end  a6 "
//					+ " from delo_DOC  ,doc ,delo  "
//					+ " where DOC.DOC_id  =delo_DOC.INPUT_DOC_ID "
//					+ " AND delo_DOC.DELO_id=DELO.DELO_id "
////					+ " and DOC.COUNT_SHEETS is not null " //тозиред е щото бр листи в док не е зад
//					+ " and DELO.DELO_id =:docId "
//					+ " and DELO_DOC.TOM_NOMER=:tom ";
			String qq=select.toString();
//			System.out.println("qq:"+qq);
			Query query = createNativeQuery(qq).setParameter("docId", docId2).setParameter("tom", zaTomId2);
			temp = query.getResultList();			
		} catch (Exception e) {
			throw new DbErrorException("Грешка при извличане на документи за опис!", e);
		}
		return temp;
}


	


	private List<TempOpis> dobaviTekStr(List<TempOpis> opList2) {
		if (opList2==null ) {
			JSFUtils.addMessage(OPISFORMTOMNOM, FacesMessage.SEVERITY_ERROR,getMessageResourceString(LABELS, OPISNOMTOM));
			return null;
		}
		  long nachalo=1;
		  long krai=0;
		  List<TempOpis> opiList = new ArrayList<>();
		  for (int i = 0; i < opList2.size(); i++) {
			  TempOpis dok=opList2.get(i);
			  if (dok.getBrLista()==null||dok.getBrLista()==0) {
				  dok.setTekStr( "-");

			  }else {
				  krai=nachalo+dok.getBrLista()-1;
				  dok.setTekStr(nachalo + "-" + krai);
				  nachalo=krai+1;
				  
			  }
			  opiList.add(dok);
			
		  }
		return opiList;

		
	}


	/**
	 * премахва избраните критерии за търсене
	 */
	public void actionClear() {
		opList.clear();
		opisList.clear();
		zaTomId=null;
		
	}
	public void actionStranicirane() {
		if (opList==null ) {
			JSFUtils.addMessage(OPISFORMTOMNOM, FacesMessage.SEVERITY_ERROR,getMessageResourceString(LABELS, OPISNOMTOM));
			return;
		}
		opList=dobaviTekStr(opList);
		hasStranici=true;
		
	}

	
	/**
	 * Създава {@link Query} по подадения String
	 *
	 * @param queryString
	 * @return Query
	 */
	public final Query createNativeQuery(String queryString) {
		return getEntityManager().createNativeQuery(queryString);
	}

	/**
	 * Създава {@link Query} по подадения String
	 *
	 * @param queryString
	 * @return Query
	 */
	public final Query createQuery(String queryString) {
		return getEntityManager().createQuery(queryString);
	}

	/**
	 * @return entityManager instance
	 */
	protected final EntityManager getEntityManager() {
		return JPA.getUtil(this.unitName).getEntityManager();
	}
	/**
	 * Ако по някакъв начин е зададено друго име, то тогава даото ще работи с него в методите за обръщение към БД. Иначе ще си
	 * взима дефолтното, което е описана в JPA-то.
	 */
	private String unitName;

	/**
	 * @return the unitName
	 */
	protected final String getUnitName() {
		return this.unitName;
	}


	
	/**
	 *  Зарежда данните на преписка/дело за редактиране
	 *   
	 * @param docId
	 * @return
	 */
	private boolean loadDelo(Integer deloId) {
		boolean bb = false;
		
		
		if(deloId != null){			
			try {

				JPA.getUtil().runWithClose(() -> delo = this.dao.findById(deloId));
				if (delo.getWithTom()==1) {
					this.hasToms = true;				
				} else {
					this.hasToms = false;
				}
				if (delo.getWithSection()==1) {
					this.hasRazdeli = true;
				} else {
					this.hasRazdeli = false;
				}
				 		
			} catch (BaseException e) {				
				JSFUtils.addErrorMessage(getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e);
				LOGGER.error(e.getMessage(), e);
			}
	
		}
		return bb;
	}
	
	public Date getToday() {
		return new Date();
	}
	
	protected DeloDAO getDao() {
		return dao;
	}

	public Delo getDelo() {
		return delo;
	}

	public void setDelo(Delo delo) {
		this.delo = delo;
	}

	public boolean isObshtodostap() {
		return obshtodostap;
	}


	public void setObshtodostap(boolean obshtodostap) {
		this.obshtodostap = obshtodostap;
	}


	public boolean isHasToms() {
		return hasToms;
	}


	public void setHasToms(boolean hasToms) {
		this.hasToms = hasToms;
	}


	public boolean isHasRazdeli() {
		return hasRazdeli;
	}


	public void setHasRazdeli(boolean hasRazdeli) {
		this.hasRazdeli = hasRazdeli;
	}

	protected DeloDocDAO getDaoDoc() {
		return daoDoc;
	}


	public LazyDataModelSQL2Array getDeloDocList() {
		return deloDocList;
	}


	public void setDeloDocList(LazyDataModelSQL2Array deloDocList) {
		this.deloDocList = deloDocList;
	}


	public DeloDelo getCurrentDeloDelo() {
		return currentDeloDelo;
	}


	public void setCurrentDeloDelo(DeloDelo currentDeloDelo) {
		this.currentDeloDelo = currentDeloDelo;
	}


	protected DeloDeloDAO getDaoDeloDelo() {
		return daoDeloDelo;
	}

	public LazyDataModelSQL2Array getDeloDeloList() {
		return deloDeloList;
	}


	public void setDeloDeloList(LazyDataModelSQL2Array deloDeloList) {
		this.deloDeloList = deloDeloList;
	}


	public Date getDecodeDate() {
		return decodeDate;
	}


	public void setDecodeDate(Date decodeDate) {
		this.decodeDate = decodeDate;
	}


	public Object[] getSelectedDeloP() {
		return selectedDeloP;
	}


	public List<Doc> getDocs() {
		return docs;
	}

	public void setDocs(List<Doc> docs) {
		this.docs = docs;
	}
	@Override
	public TimeZone getTimeZone() {
		return timeZone;
	}


	public void setTimeZone(TimeZone timeZone) {
		this.timeZone = timeZone;
	}

	public String getSp() {
		return sp;
	}

	public void setSp(String sp) {
		this.sp = sp;
	}

	public Integer getDocId() {
		return docId;
	}

	public void setDocId(Integer docId) {
		this.docId = docId;
	}


	public Integer getTomId() {
		return tomId;
	}


	public void setTomId(Integer tomId) {
		this.tomId = tomId;
	}


	public List<Object[]> getOpisList() {
		return opisList;
	}


	public void setOpisList(List<Object[]> opisList) {
		this.opisList = opisList;
	}


	public class Sortbyname implements Comparator<TempOpis>
	{
	    // Used for sorting in ascending order of
	    // roll name
	    public int compare(TempOpis a, TempOpis b)
	    {
	        return a.regNomer.compareTo(b.regNomer);
	    }
	}
	
	
//	
//	public class TempOpis implements Serializable{
//    	/**
//		 * 
//		 */
//		private static final long serialVersionUID = 1L;
//		public Long   id;
//    	public String docTom;
// 		public String regNomer;
//    	public Date   dataReg;
//    	public String otnosno;
//    	public Long   brLista;
//    	public Date   dataVl;
//    	public String tekStr;
//    	
//    	
//		public String getTekStr() {
//			return tekStr;
//		}
//
//		public void setTekStr(String tekStr) {
//			this.tekStr = tekStr;
//		}
//
//		public TempOpis(Long id, String docTom, String regNomer, Date dataReg,String otnosno,
//				Long brLista,Date dataVl, String tekStr) {
//			this.id		 = id;
//			this.docTom  = docTom;
//			this.regNomer= regNomer;
//			this.dataReg = dataReg;
//			this.otnosno = otnosno;
//			this.brLista = brLista;
//			this.dataVl  = dataVl;
//			this.tekStr  = tekStr;
//		}
//
//		public Long getId() {
//			return id;
//		}
//		public void setId(Long id) {
//			this.id = id;
//		}
//	   	public String getDocTom() {
//				return docTom;
//		}
//
//		public void setDocTom(String docTom) {
//			this.docTom = docTom;
//		}
//
//		public String getRegNomer() {
//			return regNomer;
//		}
//		public void setRegNomer(String regNomer) {
//			this.regNomer = regNomer;
//		}
//		public Date getDataReg() {
//			return dataReg;
//		}
//		public void setDataReg(Date dataReg) {
//			this.dataReg = dataReg;
//		}
//		public String getOtnosno() {
//			return otnosno;
//		}
//		public void setOtnosno(String otnosno) {
//			this.otnosno = otnosno;
//		}
//		public Long getBrLista() {
//			return brLista;
//		}
//		public void setBrLista(Long brLista) {
//			this.brLista = brLista;
//		}
//		public Date getDataVl() {
//			return dataVl;
//		}
//		public void setDataVl(Date dataVl) {
//			this.dataVl = dataVl;
//		}
//
//		public int compareTo(TempOpis o) {
//			// TODO Auto-generated method stub
//			return 0;
//		}
//
//		public int compare(TempOpis arg0, TempOpis arg1) {
//			// TODO Auto-generated method stub
//			return 0;
//		}
//   	
//
//	}




	public List<TempOpis> getOpList() {
		return opList;
	}


	public void setOpList(List<TempOpis> opList) {
		this.opList = opList;
	}


	public Integer getZaTomId() {
		return zaTomId;
	}


	public void setZaTomId(Integer zaTomId) {
		this.zaTomId = zaTomId;
	}

/******************************** EXPORTS **********************************/
	
	/**
	 * за експорт в excel - добавя заглавие и дата на изготвяне на справката и др. - 
	 */
	public void postProcessXLS(Object document) {
		String journal="";
		String title  ="";
		Object[] dopInfo=dopInfoReport();

		title = getMessageResourceString(LABELS, "opis.reportTitle");		  
    	new CustomExpPreProcess().postProcessXLS(document, title, dopInfoReport(), null, null);		
		//Запис в Журнала
		try {
			journal = sd.getSettingsValue("delo.journalExportOpis");
		} catch (DbErrorException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} // да се работи с екземпляри
		if ("1".equals(journal)) {
				String opisName = title+" "+dopInfo[0]; 
	            Integer count = opList.size();		// брой обекти в описа
	            OpisDAO opisDao = new OpisDAO(getUserData());
	
	        	try {
					JPA.getUtil().runInTransaction(()->opisDao.journalOpis(opisName,count));
				} catch (BaseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}	
   
	}

	/**
	 * за експорт в pdf - добавя заглавие и дата на изготвяне на справката - 
	 */
	public void preProcessPDF(Object document)  {
		
		String journal="";
		String title  ="";
		Object[] dopInfo=dopInfoReport();
		
		try{
			
			title = getMessageResourceString(LABELS, "opis.reportTitle");		
			new CustomExpPreProcess().preProcessPDF(document, title, dopInfoReport(), null, null);		
						
		} catch (UnsupportedEncodingException e) {
			LOGGER.error(e.getMessage(),e);			
		} catch (IOException e) {
			LOGGER.error(e.getMessage(),e);			
		} 
		//Запис в Журнала
		try {
			journal = sd.getSettingsValue("delo.journalExportOpis");
		} catch (DbErrorException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} // да се работи с екземпляри
		if ("1".equals(journal)) {
				String opisName = title+" "+dopInfo[0]; 
	            Integer count = opList.size();		// брой обекти в описа
	            OpisDAO opisDao = new OpisDAO(getUserData());
	
	        	try {
					JPA.getUtil().runInTransaction(()->opisDao.journalOpis(opisName,count));
				} catch (BaseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}	

		
	}	
	
	/**
	 * за експорт в pdf 
	 * @return
	 */
	public PDFOptions pdfOptions() {
		PDFOptions pdfOpt = new CustomExpPreProcess().pdfOptions(null, null, null);
        return pdfOpt;
	}
	
	
	/**
	 * за експорт в excel - добавя заглавие и дата на изготвяне на справката и др. 
	 */
	public void postProcessXLSInclCorresp(Object document) {
		
		String title = getMessageResourceString(LABELS, "opis.reportTitle");		  
    	new CustomExpPreProcess().postProcessXLS(document, title, dopInfoReport(), null, null);		
    	
	}

	/**
	 * за експорт в pdf - добавя заглавие и дата на изготвяне на справката - за кореспондентите в групата
	 */
	public void preProcessPDFInclCorresp(Object document)  {
		try{
			
			String title = getMessageResourceString(LABELS, "regGrInclCorresp.reportTitle");		
			new CustomExpPreProcess().preProcessPDF(document, title, dopInfoReport(), null, null);		
						
		} catch (UnsupportedEncodingException e) {
			LOGGER.error(e.getMessage(),e);			
		} catch (IOException e) {
			LOGGER.error(e.getMessage(),e);			
		} 
	}

	/**
	 * подзаглавие за екпсорта - дали да остане???
	 */
	public Object[] dopInfoReport() {
		String deloTypeTxt 	= null;
		String deloDateF 	= null;
		String registratura = null;
		Integer deloTypInt=null;
		String dialect = JPA.getUtil().getDbVendorName();
		try { 
			//Четем вида на преписката
			String qq="select DELO_TYPE  from DELO WHERE DELO.DELO_ID=:DELO_ID";
			Query queryType = createNativeQuery(qq).setParameter("DELO_ID", docId);
			if (dialect.contains("POSTGRESQL")) {
				BigInteger bi = (BigInteger) queryType.getSingleResult();
				BigDecimal deloType=new BigDecimal(bi);
				deloTypInt=deloType.intValue();
			}

			if (dialect.contains("ORACLE")) {
				BigDecimal deloType=   (BigDecimal) queryType.getSingleResult();
				deloTypInt=deloType.intValue();
			}

			int lang = getCurrentLang();
			deloTypeTxt=(String) sd.decodeItem(OmbConstants.CODE_CLASSIF_DELO_TYPE, deloTypInt, lang, null);
			//Четем датата на преписката
			 qq="select delo_date  from DELO WHERE DELO.DELO_ID=:DELO_ID";
			queryType = createNativeQuery(qq).setParameter("DELO_ID", docId);
			Date deloDate=   (Date) queryType.getSingleResult();
			SimpleDateFormat sdf=new SimpleDateFormat("dd.MM.yyyy");
			deloDateF = sdf.format(deloDate);
		//	System.out.println("deloTypeTxt:"+deloDateF);
			//Четем регистратурата
			 qq="select REGISTRATURA  from REGISTRATURI,DELO WHERE delo.REGISTRATURA_ID=REGISTRATURI.REGISTRATURA_ID and  DELO.DELO_ID=:DELO_ID";
			queryType = createNativeQuery(qq).setParameter("DELO_ID", docId);
			registratura=   (String) queryType.getSingleResult();
		} catch (Exception e) {
			JSFUtils.addErrorMessage(getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e);
			LOGGER.error(e.getMessage(), e);
		} finally {
				 JPA.getUtil().closeConnection();
		}
		
		Object[] dopInf = null;
		dopInf = new Object[2];
		dopInf[0] = "N:"+zaTomId+" на "+deloTypeTxt+" "+this.delo.getRnDelo()+"/"+deloDateF+" на регистратура "+registratura;
		return dopInf;
	}

	
	
	public List<Object[]> getTomIdList() {
		return tomIdList;
	}


	public void setTomIdList(List<Object[]> tomIdList) {
		this.tomIdList = tomIdList;
	}


	public List <SelectItem> getTomList() {
		return tomList;
	}


	public void setTomList(List <SelectItem> tomList) {
		this.tomList = tomList;
	}


	@Override
	public int compareTo(TempOpis arg0) {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	public int compare(TempOpis o1, TempOpis o2) {
		// TODO Auto-generated method stub
		return 0;
	}


	public boolean isHasStranici() {
		return hasStranici;
	}


	public void setHasStranici(boolean hasStranici) {
		this.hasStranici = hasStranici;
	}


	public List<Object[]> getPrepList() {
		return prepList;
	}


	public void setPrepList(List<Object[]> prepList) {
		this.prepList = prepList;
	}


	public HashMap<Integer, Integer> getTomIds() {
		return tomIds;
	}


	public void setTomIds(HashMap<Integer, Integer> tomIds) {
		this.tomIds = tomIds;
	}


	public HashMap<Integer, Boolean> getPrepIds() {
		return prepIds;
	}


	public void setPrepIds(HashMap<Integer, Boolean> prepIds) {
		this.prepIds = prepIds;
	}


	public SystemData getSd() {
		return sd;
	}


	public void setSd(SystemData sd) {
		this.sd = sd;
	}


}