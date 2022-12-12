package com.ib.omb.print;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.system.Constants;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.beans.OpisBezZachList;
import com.ib.omb.db.dao.OpisDAO;
import com.ib.omb.export.BaseExport;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.system.BaseSystemData;
import com.ib.system.db.JPA;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.ObjectNotFoundException;
import static com.ib.system.utils.SearchUtils.asInteger;
import static com.ib.system.utils.SearchUtils.asString;
import static com.ib.system.utils.SearchUtils.asDate;

public class OpisiExport extends BaseExport {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 6212226311300117670L;
	private static final Logger LOGGER = LoggerFactory.getLogger(OpisiExport.class);
	private BaseSystemData sd = null;
	private boolean nastrWithEkz;    // true - ще има номера на екземпляри; false - няма номера на екземпляри
	private Integer lang = null;
	private Integer id_user = null;
	private String nameRegistrUser = "";
	private SimpleDateFormat sdf = new SimpleDateFormat ("dd.MM.yyyy");
	private boolean napravlYes = false;
	private Integer codeClassAdmS = Integer.valueOf(OmbConstants.CODE_CLASSIF_ADMIN_STR);
	
	private  HttpServletResponse response = null;
	private ServletOutputStream out = null;
    private OutputStreamWriter osw = null;
    private OutputStream os = null;
    private StringBuffer rowData = new StringBuffer(); 
     
    /** Стил за редовете от справката  */
    public static final  String styleRowBez = "  height:22px; ";
    public static final  String borderTop = "border-top: #808080 1px solid;";
   
    long lenRn = 0, lenDatD = 0, lenDatIzpr = 0, lenNEkz = 0, lenBrL = 0, lenDatBack = 0; 
    
    /** Стил за заглавието 12*/
    public static final  String styleHeader12 = "font-family: Arial; font-size: 12pt; ";
    
    public OpisiExport()  {
    	
			this.sd = getSystemData();
			this.lang = getCurrentLang();
			this.id_user = getUserData().getUserId();
			allSystemSettings();
		
	}
	
    /**
   	 *  общосистемни настройки
   	 */
   	private void allSystemSettings() {
   		try {
   			setNastrWithEkz(false);
   			String param1 = sd.getSettingsValue("delo.docWithExemplars"); // да се работи с екземпляри
   			if(Objects.equals(param1,String.valueOf(OmbConstants.CODE_ZNACHENIE_DA))) {
   				setNastrWithEkz(true);
   			}
   					
   		 } catch (DbErrorException e) {
   			 LOGGER.error("Грешка при извличане на системни настройки! ", e);
   			 setNastrWithEkz(false);
   //			 JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
   		}
   	}
    
    /**Методът изгражда html таблица, която се използва за експорт на данните - описи за документи или за предадени дела/преписки
     * @param ekzList - списък от обекти  за експорт
     * @param expType - вид на експорта (HTML или Word)
     * @param titleSpr - име на справката
     * @param titleSpr1 - второ име
     * @param vhParam -  1 - опис без зачисляване; 2 - опис за куриер тип опис;  3 - опис проверка зачислени;
     * @param  idRegistrUser - id на регистр. на потребителя
     * @param rnDocForOpis - регистр. номер на описа
     * @param datDocForOpis - дата на регистрация на описа
     * @throws IOException
     * @throws ObjectNotFoundException
     * @throws DbErrorException 
     */	
	
	public void opisExport(List<Object[]> dvijList, int expType, String titleSpr,
			String titleSpr1, int vhParam, Integer idRegistrUser, String rnDocForOpis, Date datDocForOpis) 
	                   throws IOException, ObjectNotFoundException, DbErrorException {		

			
		String fileName = "";
		FacesContext ctx = FacesContext.getCurrentInstance();
		String contentType = null;
		FacesContext fContext = FacesContext.getCurrentInstance();		

				        
		switch ((int)vhParam) {
		    case 1:   // Опис без повторно зачисление 
			    fileName = "OpisBezZachExport";
				break;
		    case 2:	 // Опис куриер
				fileName = "OpisCurierExport";
				break;	
			case 3:   // Опис за проверка на зачислени документи
				fileName = "OpisProvZachExport";
				break;
					
			case 4:	  // Контролен списък
				fileName = "KontrSpisExport";
				break;
				
			case 5:    // Разносен опис със зачисляване
				fileName = "OpisWithZachExport";
				break;
			case 6: // Разносен опис за дела/преписки без зачисляване	
				fileName = "OpisDelaBezZachExport";
				break;
		}
		
		switch (expType) {
		case OmbConstants.EXPORT_HTML:
			contentType = "text/html; charset=UTF-8";
			fileName += ".html";
			break;

		case OmbConstants.EXPORT_WORD:
			contentType = "application/vnd.ms-word;  charset=UTF-8";
			fileName += ".doc";
			break;
	    case OmbConstants.EXPORT_EXCEL:
            contentType = "application/vnd.ms-excel; charset=UTF-8";
            fileName += ".xls";
            break;
            
		}
		
		
//		
//		this.nameRegistrUser = getNameRegistr(idRegistrUser);  // Име за регистратура на потребителя
//		
//		if (this.nameRegistrUser != null){
//	    	this.nameRegistrUser = getMessageResourceString("beanMessages", "general.registratura") +": "+ this.nameRegistrUser;
//		}else{
//			this.nameRegistrUser = getMessageResourceString("beanMessages", "general.registratura")+": ";
//		}
		
		
		// Дължини на колони
		
		// Дължини на колони
		if (vhParam == 2 ||  vhParam == 3) {
			// 5% 39% 20% 10%  20%
			this.lenRn=40;
			this.lenDatD=20;
			this.lenNEkz=10;
			this.lenDatIzpr=25;
			
		} else {
			if (vhParam == 4) {  // Контр. списък
				
			} else { 
				if (vhParam == 6) {  // Опис за дела/преписки без зачисление
					this.lenRn=30;
					this.lenDatD=10;
					this.lenBrL = 8;
					this.lenNEkz=6;
					this.lenDatIzpr=10;
					this.lenDatBack = 9;
					
				} else {
					
					//  vhParam= 1 - без зачисление ; vhParam = 5 - със зачисление
		  		   // 30% 10% 8% 8% 10% 
	//				this.lenRn=30;
	//				this.lenDatD=10;
	//				this.lenNEkz=8;
	//				this.lenBrL=8;
					
					this.lenRn=30;
					this.lenDatD=10;
					this.lenNEkz=8;
					this.lenDatIzpr=10;
					if (vhParam == 1) {
						this.lenDatBack = 10;
					}
				}	
			}	
			
		}
	
		this.response = (HttpServletResponse) fContext.getExternalContext().getResponse();
		this.out = response.getOutputStream();

		this.os = out;
		this.osw = new OutputStreamWriter(this.os, Charset.forName("UTF-8"));
        
		String tmpString = "";
		String titl = "";
		
			if (rnDocForOpis != null && rnDocForOpis.trim().length() > 0) {
				titl = getMessageResourceString("beanMessages", "deloEditExport.regN") + rnDocForOpis; 
				if (datDocForOpis != null){
					titl += "/" + this.sdf.format(datDocForOpis);
				}
			} else {
//				if (sekrDelovod)
//					titl = " Рег. No: ";
			}
			
		setResp (contentType, fileName, titl, null);
		pechLogo (null, null, null, null, rnDocForOpis, datDocForOpis, titleSpr, titleSpr1,  vhParam);
		
		// HTML  таблица с 6, 7 или 8 колони 
      
		//Антетка
        makeAntetkaFilter(vhParam);
       
        //съдържанието на справката
        if (vhParam == 4) {  // Контролен списък
 //       	makeBodyReportKontrSpis(dvijList,  vhParam); 
        }else{
        	if (vhParam == 6)
        		makeBodyReportFilterForDela (dvijList);
        	else	
        	   makeBodyReportFilter(dvijList,  vhParam);         
        }
        
        if (vhParam == 2 || vhParam == 3) {  // vhParam = 2 Куриер   vhParam = 3 - проверка
	        rowData = new StringBuffer("");   
	        rowData.append("<tr><td  colspan=\"4\">&nbsp;</td></tr>"); 
	        rowData.append("<tr><td  colspan=\"4\">&nbsp;</td></tr>");
	        rowData.append("<tr>");
	    	addNewColumn(rowData,sdf.format(new Date()), String.valueOf(this.lenRn + 5) + "%", styleHeader12, "left", null, null, "2", null);
	       	if (vhParam == 3) {  // За Проверка
	       		addNewColumn(rowData, getMessageResourceString("beanMessages", "opis.izgOpis")+"...........................................................", "56%", styleHeader12, "left", null, null, "4", null);
	       	 	       		  	     
	       		 String s = getNameLiceUser(this.id_user);
	       		if (s != null) {
		       	     rowData.append("</tr>"); 
		       		 osw.write(rowData.toString());
		       	     rowData.delete(0,rowData.length());
		       	     rowData.append("<tr>");
		       	      addNewColumn(rowData,"&nbsp;", String.valueOf(this.lenRn + 5) + "%", styleHeader12, "left", null, null, "2", null);
		           	  addNewColumn(rowData, 
		           			"&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" +
		           			  "/" + s + "/", "56%", "font-family: Arial; font-size: 6pt; ", "left", null, null, "4", null);
	       		}   	  
	       	} else	{  // Куриер
	       		addNewColumn(rowData, getMessageResourceString("beanMessages", "opis.podpCourier")+"...............................................", "55%", styleHeader12, "right", null, null, "3", null);
	       	}
	       	rowData.append("</tr>");       
	        osw.write(rowData.toString()); 
        } else {
//        	if (vhParam == 1 || vhParam == 5) {
//        	  		//  Опис без повторно зачисление или Опис със зачисление - не се отпечатва 'Изготвил описа'
//        		    rowData = new StringBuffer("");   
//        	        rowData.append("<tr><td  colspan=\"4\">&nbsp;</td></tr>"); 
//        	        rowData.append("<tr><td  colspan=\"4\">&nbsp;</td></tr>");
//        	        addNewColumn(rowData,sdf.format(new Date()), String.valueOf(this.lenRn + 5) + "%", styleHeader12, "left", null, null, "2", null);
//        	       	addNewColumn(rowData, "Изготвил  описа: ....................................................", "65%", styleHeader12, "right", null, null, "7", null);
//        	      	 rowData.append("</tr>");      
//        	        osw.write(rowData.toString()); 
//        	}        
        	
        }  
        
        
        tmpString = "</table></body></html>";

        
        try {
             osw.write(tmpString);
             osw.flush();
             osw.close();
             out.flush();            
             fContext.responseComplete();    
        } catch (IOException e) { 
        	if (LOGGER.isDebugEnabled()) {
        		LOGGER.debug(e.getMessage(),e);
        	}
        }       
       
        ctx.responseComplete();
        
       // Запис в журнала
        String nastrJournal = "";
		try {
			nastrJournal= sd.getSettingsValue("delo.journalExportOpis");
		} catch (DbErrorException e1) {
			// TODO Auto-generated catch block
			 LOGGER.error(e1.getMessage(), e1);
			e1.printStackTrace();
		}
		
		if ("1".equals(nastrJournal)  ) {
		
				String opisName = titleSpr; 
	            Integer count = dvijList.size();		// брой обекти в описа
	            OpisDAO opisDao = new OpisDAO(getUserData());
	
	        	try {
					JPA.getUtil().runInTransaction(()->opisDao.journalOpis(opisName,count));
				} catch (BaseException e) {
					// TODO Auto-generated catch block
					 LOGGER.error(e.getMessage(), e);
					e.printStackTrace();
				}
	      
		  }	
        
	}
	
	
	 /** Антетка на таблицата 
     */ 
	void makeAntetkaFilter(int vhPar) throws IOException {
        rowData = null;
        rowData = new StringBuffer("<tr>"); 
     

        if (vhPar == 4) {  // Контролен списък
        	// 4 колони - 5% 62% 25%  8%
            addNewColumn(rowData, "№", "5%", styleAnt8ptB,"center","top", null, null, null); 
 	        addNewColumn(rowData,getMessageResourceString("beanMessages", "opis.predNa"),String.valueOf(this.lenRn) + "%",styleAnt8ptB,"center","top",null,null,null);
 	        addNewColumn(rowData, getMessageResourceString("beanMessages", "opis.nomPredEkz"), String.valueOf(this.lenDatD) + "%", styleAnt8ptB,"center","top", null, null, null);
 	        addNewColumn(rowData,getMessageResourceString("beanMessages", "opis.brEkz"),String.valueOf(this.lenNEkz) + "%",styleAnt8ptB+styleRightBorder,"center","top",null,null,null);
        } else {
        
        //      vhPar == 1 || 5 - 	5% 30% 10% 8%  10% 13% 10% 14%   8 колони
         //     vhPar == 6 - 	5% 30% 10% 6%  8% 10% 11% 9% 11%   9 колони	
         //    	vhPar == 2 || 3 - 	5% 39% 20% 8%  20% 	         5 колони

       
	        addNewColumn(rowData, "№", "5%", styleAnt8ptB,"center","top", null, null, null); 
	        
	        if (vhPar == 6) {
	        	
	            addNewColumn(rowData,getMessageResourceString("beanMessages", "opis.regNomer"),String.valueOf(this.lenRn) + "%",styleAnt8ptB,"center","top",null,null,null);
	  	        addNewColumn(rowData, getMessageResourceString("beanMessages", "opis.datDelo"), String.valueOf(this.lenDatD) + "%", styleAnt8ptB,"center","top", null, null, null);
	  	        addNewColumn(rowData,getMessageResourceString("beanMessages", "opis.nomTom"),String.valueOf(this.lenNEkz) + "%",styleAnt8ptB,"center","top",null,null,null);
	  	       	addNewColumn(rowData,getMessageResourceString("beanMessages", "opis.maxBrL"),String.valueOf(this.lenBrL) + "%",styleAnt8ptB,"center","top",null,null,null);
	       
	        	
	        } else {
		        addNewColumn(rowData,getMessageResourceString("beanMessages", "opis.regNomer"),String.valueOf(this.lenRn) + "%",styleAnt8ptB,"center","top",null,null,null);
		        if (this.nastrWithEkz) {
		        	addNewColumn(rowData, getMessageResourceString("beanMessages", "opis.dateDoc"), String.valueOf(this.lenDatD) + "%", styleAnt8ptB,"center","top", null, null, null);
		            addNewColumn(rowData,getMessageResourceString("beanMessages", "opis.Ekz"),String.valueOf(this.lenNEkz) + "%",styleAnt8ptB,"center","top",null,null,null);
		        }  else {
		        	addNewColumn(rowData, getMessageResourceString("beanMessages", "opis.dateDoc"), String.valueOf(this.lenDatD +  this.lenNEkz) +  "%", styleAnt8ptB,"center","top", null, "2", null);
		        }
	        }
	        
	        if (vhPar == 2 || vhPar == 3) {   // Описи за куриер и за проверка
	        	addNewColumn(rowData,getMessageResourceString("beanMessages", "opis.datePred"),String.valueOf(this.lenDatIzpr) + "%",styleAnt8ptB+styleRightBorder,"center","top",null,null,null);
	       
	        } else {  // vhPar = 1  - без зачисление; vhPar = 5 - със зачисление; vhPar = 6 - опис за дела/преписки
	        	  addNewColumn(rowData,getMessageResourceString("beanMessages", "opis.datePred"),String.valueOf(this.lenDatIzpr) + "%",styleAnt8ptB,"center","top",null,null,null);
	        	
	           if (vhPar == 6) {
	        	   addNewColumn(rowData,getMessageResourceString("beanMessages", "opis.podPoluchil"),"11%",styleAnt8ptB,"center","top",null,null,null); 
		           addNewColumn(rowData,getMessageResourceString("beanMessages", "opis.dateBack"),"9%",styleAnt8ptB,"center","top",null,null,null); 
		           addNewColumn(rowData,getMessageResourceString("beanMessages", "opis.podpDelo"), "11%", styleAnt8ptB+styleRightBorder,"center","top", null, null, null);
	           } else {
	        		
		        	  addNewColumn(rowData,getMessageResourceString("beanMessages", "opis.podPoluchil"),"13%",styleAnt8ptB,"center","top",null,null,null); 
		              addNewColumn(rowData,getMessageResourceString("beanMessages", "opis.dateBack"),"10%",styleAnt8ptB,"center","top",null,null,null); 
		              addNewColumn(rowData,getMessageResourceString("beanMessages", "opis.podpDelo"), "14%", styleAnt8ptB+styleRightBorder,"center","top", null, null, null);
	           }       
	             
	        }
        }                            
        rowData.append("</tr>");
        osw.write(rowData.toString());
        if (vhPar == 1 || vhPar == 2 || vhPar == 5 || vhPar == 6) {   // Първи прадзен ред
        	 rowData.delete(0, rowData.length()) ;
        	 addNewColumn(rowData,"&nbsp;", String.valueOf(5 + this.lenRn + this.lenDatD + this.lenNEkz) + "%", borderTop,null,"top",null,"4",null); 
        	osw.write(rowData.toString());
        }	
    }     
	
	 /**
		 * Съдържание на справката - param= 1 - описи без зачисляване; param=2 - опис за куриер; param=5 - опис със зачисляване
		 *  @throws DbErrorException 
		 */
		void makeBodyReportFilter(List<Object[]> dvijList,  int vhPar ) throws IOException, ObjectNotFoundException, DbErrorException {
			int size = dvijList.size();
			rowData = null;			

			if (size == 0) {
				rowData = new StringBuffer("");
				rowData.append("<tr>");
				addNewColumn(rowData, getMessageResourceString("beanMessages", "opis.noResults"), "60%", "color:red", "center", null, null, "3", null);
				rowData.append("</tr>");
				osw.write(rowData.toString());
			
			}else {
				String style1red = styleRow8ptBez ;
				if (vhPar != 3)   style1red +=  borderTop;
				
				long poluch_t = -1;
				String polT = "";
				String namePoluch = "";
				int porNom = 0;
				
				for (int i = 0; i < size; i++) {	
				
					rowData = new StringBuffer("<tr>");
					
					Object[] obj = dvijList.get(i);
					
					Integer poluch = asInteger(obj[10]);
					String poluchT = asString(obj[11]);
					if (poluchT != null) {
						poluchT = poluchT.trim();
						if (poluchT.isEmpty()) poluchT = null;
					}
					if (poluchT == null && poluch != null) {
					
						poluchT = this.sd.decodeItem(OmbConstants.CODE_CLASSIF_ADMIN_STR, poluch, this.lang, new Date());
						poluchT = poluchT.trim();
						if (poluchT != null) {
							poluchT = poluchT.trim();
							if (poluchT.isEmpty()) poluchT = null;
						}
					}	
					
					Date dateIzpr = asDate(obj[8]);
					Date dateBack = null;
					if (vhPar == 1 )   // Опис за документи без зачисляване
						dateBack = asDate(obj[18]);
					
//					
					
//					if ((poluch != null && poluch.longValue() > 0 && poluch.longValue() != poluch_t) || 
//							((poluch == null || poluch.longValue() <= 0) && poluchT != null && poluchT.compareTo(polT) != 0))  {
					if (poluchT != null && poluchT.compareTo(polT) != 0) {
						// Смяна на получател
						
	//					if (poluch_t > 0 || !polT.equalsIgnoreCase("")) {
						if (!polT.equalsIgnoreCase("")) {
							// Първо празен ред с подчертаване 
							rowData.delete(0, rowData.length()) ;
						
							if (vhPar == 3) { // Опис за проверка
								rowData.append("<tr>");
								addNewColumn(rowData, "&nbsp;", "100%", borderTop, null, "top", null, "6", null);
								 rowData.append("</tr>"); 
					          			 // Има общ подпис накрая            
//								// Ред подпис
//							     rowData.append("<tr>");
//								addNewColumn(rowData, "&nbsp;", "100%", styleRowBez, null, "top", null, "6", null);
//								 rowData.append("</tr>"); 
//					          
//								rowData.append("<tr>");
//								addNewColumn(rowData, "&nbsp;", "44%", styleRowBez, null, "top", null, "2", null);
//								addNewColumn(rowData, getMessageResourceString("beanMessages", "ekspedPismo.podpis"), "56%", styleRowBez, null, "top", null, "4", null);
//								 rowData.append("</tr>"); 
////								rowData.append("<tr>");
////								addNewColumn(rowData, "&nbsp;", "100%", styleRowBez, null, "top", null, "6", null);
////								 rowData.append("</tr>"); 
							} else	{
						        if (vhPar != 2) {    
									rowData.append("<tr>");
								    addNewColumn(rowData, "&nbsp;", "100%", borderTop, null, "top", null, "8", null);
								    rowData.append("</tr>"); 
						        } else {    // За опис за куриер (vhPar = 2) има завършване на предишен документ с подчертаване за отделяне на редовете за него (за всеки документ е само един ред)  
						        	rowData.append("<tr>");
								    addNewColumn(rowData, "&nbsp;", "100%", borderTop, null, "top", null, "5", null);
								    rowData.append("</tr>"); 
						        }
								
							    if (vhPar == 1 || vhPar == 5) {
							    	// Ред подпис
//							    	rowData.append("<tr>");
//							    	 addNewColumn(rowData, "&nbsp;", "100%", styleRowBez, null, "top", null, "9", null);
//							    	 rowData.append("</tr>"); 
							    	 rowData.append("<tr>");
							    		addNewColumn(rowData, "&nbsp;", "63%", styleRowBez, null, "top", null, "5", null);
										addNewColumn(rowData, getMessageResourceString("beanMessages", "opis.podpis") + " " + "...............................................", "37%", styleRowBez, null, "top", null, "3", null);
							    	 rowData.append("</tr>"); 
//							    	 rowData.append("<tr>");
//							    	 addNewColumn(rowData, "&nbsp;", "100%", styleRowBez, null, "top", null, "9", null);
//							    	 rowData.append("</tr>"); 
							    }
							}    
						
				             osw.write(rowData.toString()); 
					
						}
						
						porNom = 0;
						 // Име на новия получател
//						if (poluch != null && poluch.longValue() > 0){
//							namePoluch = sd.getSystemClassifItem(this.lang, Long.valueOf(Constants.CODE_SYSCLASS_ADM_STRUCTURE), poluch);
//						}else{
//							namePoluch =  poluchT;
//						}
						
						namePoluch =  poluchT;
						
	//					if (poluch_t > 0 || !polT.equalsIgnoreCase("")) {
						if (!polT.equalsIgnoreCase("") && vhPar != 2) {
							// Един празен ред
							    rowData.delete(0, rowData.length()) ;
							    rowData.append("<tr><td  colspan=\"6\">&nbsp;</td></tr>"); 

						        osw.write(rowData.toString()); 
							
						}
						
						rowData.delete(0, rowData.length()) ;
						rowData.append("<tr>");
						if (vhPar == 3){  // Опис за проверка
							addNewColumn(rowData, getMessageResourceString("beanMessages", "opis.docZachNa") + " " + namePoluch + " " , "100%", styleHeader12, "center", null, null, "6", null);
						}else{
							if (vhPar == 2)       // Опис за куриер
								 addNewColumn(rowData, getMessageResourceString("beanMessages", "opis.docPredNa") + " " + namePoluch + " " , "100%", styleHeader12 , "center", null, null, "5", null);
							else	
							    addNewColumn(rowData, getMessageResourceString("beanMessages", "opis.docPredNa") + " " + namePoluch + " " , "100%", styleHeader12, "center", null, null, "8", null);
						}
						rowData.append("</tr>"); 
						osw.write(rowData.toString()); 
						
		//				if (vhPar != 2) {           // За опис за куриер може и да няма нужда от празен ред сле името но получателя 
							rowData.delete(0, rowData.length()) ;
							 rowData.append("<tr><td  colspan=\"6\">&nbsp;</td></tr>"); 
							 osw.write(rowData.toString()); 
	    //				}
						
						 if (poluch != null && poluch.longValue() > 0){
							 poluch_t = poluch.longValue();
						 }
						 
						   polT = new String(poluchT);   // Проверява се само за смяна на текст - при код за получател винаги го има текста от разкодиране
						
					}
					

					String nameCl = null;
					
					String s = "";
					
					
					// Първи ред
					rowData.delete(0, rowData.length()) ;
					rowData.append("<tr>");
					porNom++;
					
					addNewColumn(rowData, String.valueOf(porNom), "5%", style1red, null, "top", null, null, null);
					String rn = asString(obj[2]);    // rn_doc
					Date dDoc = asDate (obj[5]);     // date_doc
					String nomEkz = asString(obj[12]);  // номер екз
					Integer tipDoc = asInteger(obj[6]);   //  Тип документ
					String tehNom = asString(obj[14]);  // Техен номер (за входящи) 
					Date tehDat = asDate(obj[15]);
					if (tehNom != null) {
						tehNom = tehNom.trim();
					} else tehNom = "";
					if (tehDat != null) tehNom = tehNom + "/" + sdf.format(tehDat);
					if (tehNom.isEmpty()) tehNom = null;
					
					
							
					if (rn != null){
						s = rn;
						if (dDoc != null){
							s += "/" + 	sdf.format(dDoc);
						}
					} else{
						s = "";
					}
					
					addNewColumn(rowData, s, String.valueOf(this.lenRn) + "%", style1red, null, "top", null, null, null);
					if (this.nastrWithEkz) {
						if (dDoc != null)
							addNewColumn(rowData, sdf.format(dDoc) , String.valueOf(this.lenDatD) + "%", style1red, "center", "top", null, null, null);
						else
							addNewColumn(rowData, " " , String.valueOf(this.lenDatD) + "%", style1red, "center", "top", null, null, null);
									
						addNewColumn(rowData, nomEkz, String.valueOf(this.lenNEkz) + "%", style1red, "center","top", null, null, null);
				   } else {
					   if (dDoc != null)
							addNewColumn(rowData, sdf.format(dDoc) , String.valueOf(this.lenDatD + this.lenNEkz) + "%", style1red, "center", "top", null, "2", null);
						else
							addNewColumn(rowData, " " , String.valueOf(this.lenDatD + this.lenNEkz) + "%", style1red, "center", "top", null, null, null);
				   }	
					if (dateIzpr != null)
						addNewColumn(rowData, sdf.format(dateIzpr) , String.valueOf(this.lenDatIzpr) + "%", style1red, "center", "top", null, null, null);
					else
						addNewColumn(rowData, " " , String.valueOf(this.lenDatIzpr) + "%", style1red, "center", "top", null, null, null);
					
					if (vhPar !=2 ) {     // Допълване с подчертаване
						if (vhPar == 1) {
							if (dateBack == null)
								addNewColumn(rowData, " " , "37%", style1red, "center", "top", null, "3", null);   // Празни колони
							else {
							  addNewColumn(rowData, " " , "13%", style1red, "center", "top", null, "1", null);
							  addNewColumn(rowData, sdf.format(dateBack) , String.valueOf(this.lenDatBack) + "%", style1red, "center", "top", null, "1", null);
							  addNewColumn(rowData, " " , "14%", style1red, "center", "top", null, "1", null);
							}  
						} else
					       addNewColumn(rowData, " " , "37%", style1red, "center", "top", null, "3", null);   // Празни колони
					}  
					
					//	addNewColumn(rowData, checkLong(itemD.getBrStrAll()), String.valueOf(this.lenBrL) + "%", style1red , "center","top", null, null, null);
//					if (vhPar == 3 || vhPar == 2){
//						addNewColumn(rowData, sdf.format(dateIzpr), String.valueOf(this.lenDatIzpr) + "%",style1red, "center", "top", null, null, null);
//					}else {
//						s = getMessageResourceString("beanMessages", "opis.tipDoc");
//						nameCl = sd.getSystemClassifItem(this.lang, Long.valueOf(Constants.CODE_SYSCLASS_TIP_DOC), itemD.getTipDoc());
//						if (nameCl != null){
//							s += " " + nameCl;
//						}
////						  if (itemD.getTehNom() != null && !itemD.getTehNom().trim().equalsIgnoreCase("")) {
////			  				  // Допълнително техен номер
////								s += getMessageResourceString("beanMessages", "printOpisZachExport.techNom");
////								s +=  itemD.getTehNom();
////								if (itemD.getTehDat() != null){
////									s += "/" + sdf.format(itemD.getTehDat());
////								}
////			  			   }
//						
//						addNewColumn(rowData, s,String.valueOf((100 - (5 + this.lenRn + this.lenDatD + this.lenNEkz + this.lenBrL)))  + "%", style1red , null, "top", null, "4", null);
//					}
					
//					if (vhPar != 3) {
//						addNewColumn(rowData, "...............", "9%",  styleRow8ptBez  + borderTop , "center", "top", null, "1", null);
//						addNewColumn(rowData, ".........................", "11%",  styleRow8ptBez  + borderTop, "center", "top", null, "1", null);
//						addNewColumn(rowData, "...............", "9%",  styleRow8ptBez  + borderTop, "center", "top", null, "1", null);
//					}	
									
					rowData.append("</tr>"); 
					osw.write(rowData.toString());
					
					// Втори ред
					
					if (vhPar != 2) {                    // Опис за куриер има само първия ред
							// Втори ред 
							rowData.delete(0, rowData.length()) ;
							rowData.append("<tr>");
							
							addNewColumn(rowData, "", "5%", styleRow8ptBez, null, "top", null, null, null);
//							if (vhPar == 3 || vhPar == 1) {
//								s = getMessageResourceString("beanMessages", "deloExport.tipDoc")+": ";
//								nameCl = sd.getSystemClassifItem(this.lang, Long.valueOf(Constants.CODE_SYSCLASS_TIP_DOC), itemD.getTipDoc());
//								if (nameCl != null){
//									s += " " + nameCl;
//								}
//								addNewColumn(rowData, s, String.valueOf(this.lenRn) + "%", styleRow8ptBez , null, "top", null, null, null);
//								
//								s = getMessageResourceString("beanMessages", "general.vidDoc"); 
//								nameCl = sd.getSystemClassifItem(this.lang, Long.valueOf(Constants.CODE_SYSCLASS_VID_DOC), itemD.getVidDoc());
//								if (nameCl != null){
//									s += " " + nameCl;
//								}
//								addNewColumn(rowData, s, String.valueOf((100 - (5 + this.lenRn))) + "%", styleRow8ptBez,  null, "top", null, "4", null);
//									
//							} else {
//							   // Вид документ
//										
//								s = getMessageResourceString("beanMessages", "printOpisZachExport.vidDoc"); 
//								nameCl = sd.getSystemClassifItem(this.lang, Long.valueOf(Constants.CODE_SYSCLASS_VID_DOC), itemD.getVidDoc());
//								if (nameCl != null){
//									s += " " + nameCl;
//								}
//								addNewColumn(rowData, s, String.valueOf(this.lenRn + this.lenDatD + this.lenNEkz + this.lenBrL) + "%", styleRow8ptBez,  "left", "top", null, "4", null);
//									
//							}   
							
							if (vhPar == 1 || vhPar == 5) {
								s = getMessageResourceString("labels", "register.tipDoc")+": ";
								s = "<b>" + s + "</b>" ;
								nameCl = asString(obj[obj.length-2]);   // Тип документ
								if (nameCl != null){
									s += " " + nameCl;
								}
			//					addNewColumn(rowData, s, String.valueOf(this.lenRn) + "%", styleRow8ptBez , null, "top", null, null, null);
								
								if (tehNom != null && tipDoc.intValue() == OmbConstants.CODE_ZNACHENIE_DOC_TYPE_IN) {
									
									s += "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" + "<b>" + getMessageResourceString("beanMessages", "opis.tehNom") + "</b>";
									s += " " + tehNom;
//									addNewColumn(rowData, s, String.valueOf(this.lenDatD + lenNEkz) + "%", styleRow8ptBez , null, "top", null, "2", null);
//								} else
//									addNewColumn(rowData, " ", String.valueOf(this.lenDatD + lenNEkz) + "%", styleRow8ptBez , null, "top", null, "2", null);
								}
								
								addNewColumn(rowData, s, String.valueOf(this.lenRn + this.lenDatD + lenNEkz) + "%", styleRow8ptBez , null, "top", null, "3", null);
								
								s = getMessageResourceString("labels", "docVidSetting.typeDoc")+": "; 
								s = "<b>" + s + "</b>";
								nameCl = asString(obj[obj.length-1]);   // Вид документ
								if (nameCl != null){
									s += " " + nameCl;
								}
						//		addNewColumn(rowData, s, String.valueOf((100 - (5 + this.lenRn))) + "%", styleRow8ptBez,  null, "top", null, "4", null);
								addNewColumn(rowData, s, "37%", styleRow8ptBez,  null, "top", null, "4", null);
							} else {
								
							}
							
							  rowData.append("</tr>"); 
				              osw.write(rowData.toString()); 
				              
				            // Трети ред - направления - ако има
							 s = "";
//				              if (napravlYes && itemD.getNaprav() != null) {
//				            	  if (vhPar == 3 || vhPar == 1) {
//					            	  nameCl = sd.getSystemClassifItem(this.lang, Long.valueOf(Constants.CODE_SYSCLASS_MAILIN_CHANEL), itemD.getNaprav());
//					            	  s = getMessageResourceString("beanMessages", "deloExport.napravlenie");
//					            	  if (nameCl != null){
//					            		  s += nameCl;
//					            	  }
//					            	  rowData.delete(0, rowData.length()) ;
//					  				  rowData.append("<tr>"); 
//					  				  addNewColumn(rowData, "", "5%", styleRow8ptBez, null, "top", null, null, null);
//					  				  if (vhPar == 1){
//					  					  addNewColumn(rowData, s, "59%", styleRow8ptBez, null, "top", null, "2", null);
//					  				  }else{	  
//					  					  addNewColumn(rowData, s, "56%", styleRow8ptBez, null, "top", null, "4", null);
//					  				  }
//					  				if (itemD.getTehNom() != null && !itemD.getTehNom().trim().equalsIgnoreCase("")) {
//					  				  // Допълнително техен номер
//										s = getMessageResourceString("beanMessages", "printOpisZachExport.techNomer");
//										s +=  itemD.getTehNom();
//										if (itemD.getTehDat() != null){
//											s += "/" + sdf.format(itemD.getTehDat());
//										}
//										addNewColumn(rowData, s, "36%", styleRow8ptBez, null, "top", null, "3", null);	
//				
//					  				}
//					  			     rowData.append("</tr>"); 
//					                 osw.write(rowData.toString()); 
//				            	  }  else {
//				            		  nameCl = sd.getSystemClassifItem(this.lang, Long.valueOf(Constants.CODE_SYSCLASS_MAILIN_CHANEL), itemD.getNaprav());
//					            	  s = getMessageResourceString("beanMessages", "deloExport.napravlenie");
//					            	  if (nameCl != null){
//					            		  s += nameCl;
//					            	  }
//				            	  }
//				              }
//				              if (vhPar != 3 && vhPar != 2) {
//				            	  // Трети ред -  направление и подписи
//				            	  rowData.delete(0, rowData.length()) ;
//				  				  rowData.append("<tr>");
//				  				 addNewColumn(rowData, "", "5%", styleRow8ptBez, null, "top", null, null, null);
//				  				 addNewColumn(rowData, s, String.valueOf(this.lenRn + this.lenDatD + this.lenNEkz + this.lenBrL) + "%", styleRow8ptBez,  null, "top", null, "4", null);
//				  			    
//				  				addNewColumn(rowData, sdf.format(dateIzpr), String.valueOf(this.lenDatIzpr) + "%", styleRow8ptBez, "center", "top", null, null, null);
//				  			
//			//	  				addNewColumn(rowData, "...........................................................", "29%",  styleRow8ptBez , "center", "top", null, "3", null);
//				  				addNewColumn(rowData, "..............................", "9%",  styleRow8ptBez  , "center", "top", null, "1", null);
//								addNewColumn(rowData, "....................................", "11%",  styleRow8ptBez, "center", "top", null, "1", null);
//								addNewColumn(rowData, "..............................", "9%",  styleRow8ptBez , "center", "top", null, "1", null);
//				  				 
//				  				 
//				  				  rowData.append("</tr>"); 
//				                  osw.write(rowData.toString()); 
//				              }
//					
					
		              // Четвърти ред - формира се само за описа за проверка
//		              if (vhPar == 3) {
//		            	if (statusE != null && statusE.longValue() == Constants.CODE_ZNACHENIE_EKZ_DADEN) {
//		            		  rowData.delete(0, rowData.length()) ;
//			  				  rowData.append("<tr>");
//		            		// Екземплярът е в друга регистратура
//		            		addNewColumn(rowData, "", "5%", styleRow8ptBez, null, "top", null, null, null);
//		  					s = "ЕКЗЕМПЛЯРЪТ Е В ДРУГА РЕГИСТРАТУРА";
//		  					addNewColumn(rowData, s, "75%", styleRow8ptBez, null, "top", null, "4", null);
//		  				  rowData.append("</tr>"); 
//			              osw.write(rowData.toString());
//		  				} 
//		              } else {
//		            	  if (sekrDelovod == true && (itemD.getSekr() != null && itemD.getSekr() > Constants.CODE_ZNACHENIE_QVNO)) {
//			            	  if (vhPar == 1) {
//			            		  rowData.delete(0, rowData.length()) ;
//				  				  rowData.append("<tr>");
//				  				  s = getMessageResourceString("beanMessages", "printOpisZachExport.nivClassif");
//				  				  nameCl = sd.getSystemClassifItem(this.lang, Long.valueOf(Constants.CODE_SYSCLASS_NIVO_SECR), itemD.getSekr());
//				  				  if (nameCl != null){
//									s += " " + nameCl;
//				  				  }
//				  				  addNewColumn(rowData, "", "5%", styleRow8ptBez, null, "top", null, null, null);
//				            	  addNewColumn(rowData, s, "56%", styleRow8ptBez, "left", "top", null, "4", null);
//								  rowData.append("</tr>"); 
//					              osw.write(rowData.toString());
//			            	  }    
//			              }
		            	  
		            	  
//		              }
				                
		            	  // Пети ред - относно - само за явно деловодство
		            	   // Четвърти ред  - относно - само за явно деловодство
	
	//				       if (sekrDelovod == false ) {
							 
							 try {
								 if (vhPar == 5)   // За опис със зачисляване
							      s =  new OpisDAO(getUserData()).getOtnosnoFromDoc(asInteger(obj[0]));    // Относно по idDoc
								 else if (vhPar == 1)   // vhPar -- 1 - опис без зачисляване 
							      s =  new OpisDAO(getUserData()).getOtnosnoFromDoc(asInteger(obj[1]));    // Относно по idDoc
								 else s = null;
							 } catch (DbErrorException e) {
								 throw new DbErrorException (e.getLocalizedMessage());
							 }
							 
							   if (s == null)  s = "";
				        	   	rowData.delete(0, rowData.length()) ;
				        	   	rowData.append("<tr>");
				        	   	addNewColumn(rowData, "", "5%", styleRow8ptBez, null, "top", null, null, null);
				      //  		addNewColumn(rowData, getMessageResourceString("beanMessages", "opis.anotExport")+": ", String.valueOf(this.lenRn) + "%", styleRow8ptBez , null, "top", null, null, null);
				        		addNewColumn(rowData, "<b>" + getMessageResourceString("beanMessages", "opis.anotExport")+": " + "</b>" + s, "95%", styleRow8ptBez,  "left", "top", null, "7", null);
				           	    rowData.append("</tr>"); 
				                 osw.write(rowData.toString());
	//				       }
				           
					}          // Край за проверката vhPar != 2
										  	
	        
				 }       // Край за for
					
	           }      // Край за else за size > 0
				
						
					//  Накрая - първо празен ред с подчертаване
				rowData.delete(0, rowData.length()) ;
				
					if (vhPar == 3) { // Опис за проверка
						//Първо  празен ред с подчертаване
						rowData.append("<tr>");
						addNewColumn(rowData, "&nbsp;", "100%", borderTop, null, "top", null, "6", null);
						 rowData.append("</tr>"); 
			          			             
//						// Ред подпис
//					     rowData.append("<tr>");
//						addNewColumn(rowData, "&nbsp;", "100%", styleRowBez, null, "top", null, "6", null);
//						 rowData.append("</tr>"); 
//			          
//						rowData.append("<tr>");
//						addNewColumn(rowData, "&nbsp;", "44%", styleRowBez, null, "top", null, "2", null);
//						addNewColumn(rowData, getMessageResourceString("beanMessages", "opis.podpis"), "56%", styleRowBez, null, null, null, "4", null);
//						 rowData.append("</tr>"); 
////						rowData.append("<tr>");
////						addNewColumn(rowData, "&nbsp;", "100%", styleRowBez, null, "top", null, "6", null);
////						 rowData.append("</tr>"); 
					} else	{
						if (vhPar != 2) {                      
						//Първо  празен ред с подчертаване
							rowData.append("<tr>");
						    addNewColumn(rowData, "&nbsp;", "100%", borderTop, null, "top", null, "8", null);
						    rowData.append("</tr>"); 
						}  else {
							
					        	rowData.append("<tr>");
							    addNewColumn(rowData, "&nbsp;", "100%", borderTop, null, "top", null, "5", null);
							    rowData.append("</tr>"); 
					       
						}
						
					    if (vhPar == 1 || vhPar== 5 ) {
					    	// Ред подпис
//					    	rowData.append("<tr>");
//					    	 addNewColumn(rowData, "&nbsp;", "100%", styleRowBez, null, "top", null, "9", null);
//					    	 rowData.append("</tr>"); 
					    	 rowData.append("<tr>");
					    	    addNewColumn(rowData, "&nbsp;", "63%", styleRowBez, null, "top", null, "5", null);
								addNewColumn(rowData, getMessageResourceString("beanMessages", "opis.podpis") + " " + "...............................................", "37%", styleRowBez, null, "top", null, "3", null);
					    	 rowData.append("</tr>"); 
//					    	 rowData.append("<tr>");
//					    	 addNewColumn(rowData, "&nbsp;", "100%", styleRowBez, null, "top", null, "9", null);
//					    	 rowData.append("</tr>"); 
					    }
					}    
				
		             osw.write(rowData.toString()); 
						
	    }
		
		
		 /**
		 * Съдържание на справката - param= 6 - опис за дела/преписки  без зачисляване; 
		 *  @throws DbErrorException 
		 */
		void makeBodyReportFilterForDela(List<Object[]> dvijList ) throws IOException, ObjectNotFoundException, DbErrorException {
			int size = dvijList.size();
			rowData = null;			

			if (size == 0) {
				rowData = new StringBuffer("");
				rowData.append("<tr>");
				addNewColumn(rowData, getMessageResourceString("beanMessages", "opis.noResults"), "60%", "color:red", "center", null, null, "3", null);
				rowData.append("</tr>");
				osw.write(rowData.toString());
			
			}else {
				String style1red = styleRow8ptBez ;
				style1red +=  borderTop;
				
				long poluch_t = -1;
				String polT = "";
				String namePoluch = "";
				int porNom = 0;
				
				for (int i = 0; i < size; i++) {	
				
					rowData = new StringBuffer("<tr>");
					
					Object[] obj = dvijList.get(i);
					
					Integer poluch = asInteger(obj[10]);
					String poluchT = asString(obj[11]);
					if (poluchT != null) {
						poluchT = poluchT.trim();
						if (poluchT.isEmpty()) poluchT = null;
					}
					if (poluchT == null && poluch != null) {
					
						poluchT = this.sd.decodeItem(OmbConstants.CODE_CLASSIF_ADMIN_STR, poluch, this.lang, new Date());
						poluchT = poluchT.trim();
						if (poluchT != null) {
							poluchT = poluchT.trim();
							if (poluchT.isEmpty()) poluchT = null;
						}
					}	
					
					Date dateIzpr = asDate(obj[8]);
					Date dateBack = asDate(obj[18]);
					
//					
					
//					if ((poluch != null && poluch.longValue() > 0 && poluch.longValue() != poluch_t) || 
//							((poluch == null || poluch.longValue() <= 0) && poluchT != null && poluchT.compareTo(polT) != 0))  {
					if (poluchT != null && poluchT.compareTo(polT) != 0) {
						// Смяна на получател
						
	//					if (poluch_t > 0 || !polT.equalsIgnoreCase("")) {
						if (!polT.equalsIgnoreCase("")) {
												
							       rowData.delete(0, rowData.length()) ;
							     
							   	// Първо празен ред с подчертаване 
							   	rowData.append("<tr>");
							    addNewColumn(rowData, "&nbsp;", "100%", borderTop, null, "top", null, "9", null);
							    rowData.append("</tr>"); 
										
							    	// Ред подпис
//							    
							       rowData.append("<tr>");
						    	    addNewColumn(rowData, "&nbsp;", "69%", styleRowBez, null, "top", null, "6", null);
									addNewColumn(rowData, getMessageResourceString("beanMessages", "opis.podpis") + " " + "...............................................", "31%", styleRowBez, null, "top", null, "3", null);
						    	   rowData.append("</tr>"); 
//							    	 
												
				             osw.write(rowData.toString()); 
					
						}
						
						porNom = 0;
						 // Име на новия получател
//						if (poluch != null && poluch.longValue() > 0){
//							namePoluch = sd.getSystemClassifItem(this.lang, Long.valueOf(Constants.CODE_SYSCLASS_ADM_STRUCTURE), poluch);
//						}else{
//							namePoluch =  poluchT;
//						}
						
						namePoluch =  poluchT;
						
	//					if (poluch_t > 0 || !polT.equalsIgnoreCase("")) {
						if (!polT.equalsIgnoreCase("")) {
							// Два празни реда
							    rowData.delete(0, rowData.length()) ;
							    rowData.append("<tr><td  colspan=\"6\">&nbsp;</td></tr>"); 
//						        rowData.append("<tr><td  colspan=\"6\">&nbsp;</td></tr>");
						        osw.write(rowData.toString()); 
							
						}
						
						rowData.delete(0, rowData.length()) ;
						rowData.append("<tr>");
					
					     addNewColumn(rowData, getMessageResourceString("beanMessages", "opis.prepPredNa") + " " + namePoluch + " " , "100%", styleHeader12, "center", null, null, "9", null);
						
						rowData.append("</tr>"); 
						osw.write(rowData.toString()); 
						
						// празен ред след името но получателя
					
							rowData.delete(0, rowData.length()) ;
							 rowData.append("<tr><td  colspan=\"6\">&nbsp;</td></tr>"); 
							 osw.write(rowData.toString()); 
						
						
						 if (poluch != null && poluch.longValue() > 0){
							 poluch_t = poluch.longValue();
						 }
						 
						   polT = new String(poluchT);   // Проверява се само за смяна на текст - при код за получател винаги го има текста от разкодиране
						
					}
					

					String nameCl = null;
					
					String s = "";
					
					
					// Първи ред
					rowData.delete(0, rowData.length()) ;
					rowData.append("<tr>");
					porNom++;
					
					addNewColumn(rowData, String.valueOf(porNom), "5%", style1red, null, "top", null, null, null);
					String rn = asString(obj[2]);    // rn_delo
					Date dDelo = asDate (obj[5]);     // date_delo
					String nomTom = asString(obj[12]);  // номер tom
					String brL =  asString(obj[16]);   // Макс. брой листа
					Integer tipDelo = asInteger(obj[6]);   //  Тип дело
										
							
					if (rn != null){
						s = rn;
						if (dDelo != null){
							s += "/" + 	sdf.format(dDelo);
						}
					} else{
						s = "";
					}
					
					addNewColumn(rowData, s, String.valueOf(this.lenRn) + "%", style1red, null, "top", null, null, null);
					if (dDelo != null)
						addNewColumn(rowData, sdf.format(dDelo) , String.valueOf(this.lenDatD) + "%", style1red, "center", "top", null, null, null);
					else
						addNewColumn(rowData, " " , String.valueOf(this.lenDatD) + "%", style1red, "center", "top", null, null, null);
									
					addNewColumn(rowData, nomTom, String.valueOf(this.lenNEkz) + "%", style1red, "center","top", null, null, null);
					addNewColumn(rowData, brL, String.valueOf(this.lenBrL) + "%", style1red, "center","top", null, null, null);
					
					if (dateIzpr != null)
						addNewColumn(rowData, sdf.format(dateIzpr) , String.valueOf(this.lenDatIzpr) + "%", style1red, "center", "top", null, null, null);
					else
						addNewColumn(rowData, " " , String.valueOf(this.lenDatIzpr) + "%", style1red, "center", "top", null, null, null);
					
					     // Допълване с подчертаване
					
					if (dateBack == null)
					      addNewColumn(rowData, " " , "31%", style1red, "center", "top", null, "3", null);   // Празни колони
					  else {
						  addNewColumn(rowData, " " , "11%", style1red, "center", "top", null, "1", null);
						  addNewColumn(rowData, sdf.format(dateBack) , String.valueOf(this.lenDatBack) + "%", style1red, "center", "top", null, "1", null);
						  addNewColumn(rowData, " " , "11%", style1red, "center", "top", null, "1", null);
					  }
					
													
					rowData.append("</tr>"); 
					osw.write(rowData.toString());
					
							// Втори ред 
							rowData.delete(0, rowData.length()) ;
							rowData.append("<tr>");
							
							addNewColumn(rowData, "", "5%", styleRow8ptBez, null, "top", null, null, null);
//							
								s = getMessageResourceString("labels", "opis.tipDelo")+": ";
								s = "<b>" + s + "</b>" ;
								nameCl = asString(obj[obj.length-1]);   // Тип Дело
								if (nameCl != null){
									s += " " + nameCl;
								}
		
								
								addNewColumn(rowData, s, String.valueOf(this.lenRn ) + "%", styleRow8ptBez , null, "top", null, "1", null);
								
								 try {
										
								      s =  new OpisDAO(getUserData()).getNaimFromDelo(asInteger(obj[1]));    // Наименование по idDelo
									
								 } catch (DbErrorException e) {
									 throw new DbErrorException (e.getLocalizedMessage());
								 }
								 
								   if (s == null)  s = "";
					        	   
					           		addNewColumn(rowData, "<b>" + getMessageResourceString("labels", "docu.nameDelo")+": " + "</b>" + s, "65%", styleRow8ptBez,  "left", "top", null, "7", null);
					           	    
														
							  rowData.append("</tr>"); 
				              osw.write(rowData.toString()); 
								 
//							 try {
//								
//							      s =  new OpisDAO(getUserData()).getNaimFromDelo(asInteger(obj[1]));    // Наименование по idDelo
//								
//							 } catch (DbErrorException e) {
//								 throw new DbErrorException (e.getLocalizedMessage());
//							 }
//							 
//							   if (s == null)  s = "";
//				        	   	rowData.delete(0, rowData.length()) ;
//				        	   	rowData.append("<tr>");
//				        	   	addNewColumn(rowData, "", "5%", styleRow8ptBez, null, "top", null, null, null);
//				           		addNewColumn(rowData, "<b>" + getMessageResourceString("labels", "docu.nameDelo")+": " + "</b>" + s, "95%", styleRow8ptBez,  "left", "top", null, "8", null);
//				           	    rowData.append("</tr>"); 
//				                 osw.write(rowData.toString());
				           
				
	        
				 }       // Край за for
					
	           }      // Край за else за size > 0
				
						
					//  Накрая - първо празен ред с подчертаване
				          rowData.delete(0, rowData.length()) ;
				
					    
						//Първо  празен ред с подчертаване
							rowData.append("<tr>");
						    addNewColumn(rowData, "&nbsp;", "100%", borderTop, null, "top", null, "9", null);
						    rowData.append("</tr>"); 
					 
						
					    	// Ред подпис

					    	 rowData.append("<tr>");
					    	    addNewColumn(rowData, "&nbsp;", "69%", styleRowBez, null, "top", null, "6", null);
								addNewColumn(rowData, getMessageResourceString("beanMessages", "opis.podpis") + " " + "...............................................", "31%", styleRowBez, null, "top", null, "3", null);
					    	 rowData.append("</tr>"); 

				
		             osw.write(rowData.toString()); 
						
	    }

	
	
	public void setResp (String contT, String fileN, String title, String titleInterval) {
	    this.response.setContentType(contT);
        this.response.setHeader("Content-Disposition", 
                           "attachment;filename=\"" + fileN + "\"");
        String s = title;
       
        if (titleInterval != null){
        	s += " " + titleInterval;
        }
        rowData.delete(0, rowData.length());
        rowData.append( 
            "<html><Head> <meta http-equiv=\"Content-Type\" " + "content=\"" + 
            contT + "\"></meta> " + 
            "<TITLE> " + s + " </TITLE> </HEAD> <BODY>" +
            "<style> td { mso-number-format:\"\\@\";} </style>"
        );    
       	       
        try {
        	osw.write(rowData.toString());
        } catch (IOException e) {
        	JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString("beanMessages", "opis.ioErrorWrite") + "-" + e.getLocalizedMessage(),e.getMessage());

            LOGGER.error(e.getMessage(),e);
         }	
	
    }

     public void pechLogo (String antetka, String antetka1, String adr, String infSyst, String regNomForma,  Date dateForma, String zagl, String zagl1,  int vhPar) {

	
	    // Таблицата е с 4, 5, 6 или 8 колони
        int brCol = 8;
        if (vhPar == 6)
        	brCol = 9;
        else if (vhPar == 3){
           brCol = 6;  // Опис за проверка на зачислени документи
        }else{ 
        	if (vhPar == 4){
        		brCol = 4;   // Контролен списък
        	} else if (vhPar == 2) brCol = 5;
        	
        	
        }
        
		// Явна регистратура - само заглавие
		rowData.delete(0, rowData.length()); 
		 rowData.append("<br/>  <br/>");
        rowData.append(
         "<table align=\"center\" style=\""+ styleTable11pt+ "\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" width=\"100%\">" + "<tr>") ;
//        if (this.nameRegistrUser != null && !this.nameRegistrUser.trim().equalsIgnoreCase("")) {
//        	rowData.append( "<td align=\"left\" colspan=\"" + String.valueOf(brCol) + "\" style=\""+ styleRow10+"\"> " +  this.nameRegistrUser +" </td>" +
//        	"<td align=\"right\" colspan=\"" + String.valueOf(brCol) + "\" style=\""+ styleRow10+"\"> "+ this.sdf.format(new Date())+" </td></tr>") ;
//        } else {	
              if (vhPar != 1 && vhPar != 5 && vhPar != 3 && vhPar != 2)  { // Дата
            	  rowData.append(  "<td align=\"right\" colspan=\"" + String.valueOf(brCol) + "\" style=\""+ styleRow10+"\"> "+ this.sdf.format(new Date())+" </td></tr>" );
              }
//        }	
        
  //      if (sekrDelovod && regNomForma != null && !regNomForma.trim().equalsIgnoreCase("")) {
            if (regNomForma != null && !regNomForma.trim().equalsIgnoreCase("")) {   
        	// Регистрационен номер на формата
        	rowData.append( "<tr/> ");
        	String s = getMessageResourceString("beanMessages", "printOpisZachExport.regNomer")+":" + regNomForma;
        	if (dateForma != null){
        		s += "/" + this.sdf.format(dateForma);
        	}
        	rowData.append( "<td align=\"left\" colspan=\"" + String.valueOf(brCol) + "\" style=\""+ styleRow10+"\"> " + 
        			s +" </td> </tr> ");
        	   
            rowData.append("<tr><td  colspan=\"6\">&nbsp;</td></tr>"); 
            rowData.append("<tr><td  colspan=\"6\">&nbsp;</td></tr>"); 
        }	
        
         if (zagl != null && !zagl.trim().isEmpty())
           rowData.append("<tr><td  align=\"center\" colspan=\"" + String.valueOf(brCol) + "\" style=\""+ styleHeader12 +"\"> "+zagl+" </td></tr>");
         else  rowData.append("<tr><td  align=\"center\" colspan=\"" + String.valueOf(brCol) + "\" style=\""+ styleHeader12 +"\"> "+"&nbsp;"+" </td></tr>");
         
         if (zagl1 != null && !zagl1.trim().isEmpty())
          rowData.append("<tr><td  align=\"center\" colspan=\"" + String.valueOf(brCol) + "\" style=\""+ styleHeader12 +"\"> "+zagl1+" </td></tr>");
         rowData.append("<tr><td> &nbsp; </td></tr>" );
        try {
        	osw.write(rowData.toString());
        } catch (IOException e) {
        	JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString("beanMessages", "opis.ioErrorWrite") + "-" + e.getLocalizedMessage(),e.getMessage());

             LOGGER.error(e.getMessage(),e);
         }	
				
   }
     
     /**
      *  Име на служител/служба, свързани с текущия потребител
      * @param idUs - id на текущия потребител 
      * @return
      * @throws DbErrorException
      */
     public String getNameLiceUser (Integer idUs)  {
    	 
    	 String liceName = null;
     
// 		HibernateUtil.currentSession();
// 		AdmUsersDAO usersDAO = new AdmUsersDAO(this.id_user, this.sd);
// 		AdmUsers user = null;
// 		Long idLiceUser = null;

// 		try {
// 		
// 			user =  usersDAO.findById(idUs);
// 			if (user != null) {
//                  idLiceUser = user.getId_lice();
// 			}
// 			if (idLiceUser == null) {
// 				return null;
// 			}
// 			// зарежда името на служителя/службата, ако има такова
// 			liceName = this.getSystemData().getClassAttr(this.codeClassAdmS, idLiceUser, new Date(), this.lang);
// 			if (liceName == null || liceName.trim().equalsIgnoreCase("")){ 
//                return null;
//             }
// 		
// 		} catch (DbErrorException e) {
// 			LOGGER.error("Грешка при работа с базата данни!!!", e);
//// 			addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString("beanMessages", "general.errDataBaseMsg"),e.getHtmlDetail());
//// 			  throw new DbErrorException("Грешка при извличане данни за потребител!", 
////                       e);
// 			return null;
// 		} finally {
// 			HibernateUtil.closeSession();
// 		}
// 			
 		return liceName;
 	}

	public boolean isNastrWithEkz() {
		return nastrWithEkz;
	}

	public void setNastrWithEkz(boolean nastrWithEkz) {
		this.nastrWithEkz = nastrWithEkz;
	}

}
