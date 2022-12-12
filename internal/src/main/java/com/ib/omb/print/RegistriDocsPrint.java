package com.ib.omb.print;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
//import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
//import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFFooter;
import org.apache.poi.hssf.usermodel.HSSFPrintSetup;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.usermodel.HeaderFooter;
import org.apache.poi.ss.util.CellRangeAddress;
//import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.ss.util.RegionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
//import org.apache.poi.xssf.usermodel.XSSFFirstFooter;

import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.db.dao.OpisDAO;
import com.ib.omb.db.dao.RegisterDocsPrintDAO;
import com.ib.omb.db.dao.RegistraturaDAO;
import com.ib.omb.db.dto.Registratura;
import com.ib.omb.export.BaseExport;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.omb.system.UserData;
//import com.ib.indexui.utils.JSFUtils;
import com.ib.system.db.JPA;
import com.ib.system.exceptions.BaseException;
//import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.DbErrorException;
import static com.ib.system.utils.SearchUtils.asInteger;
import static com.ib.system.utils.SearchUtils.asString;
import static com.ib.omb.system.OmbConstants.CODE_CLASSIF_REGISTRI;
import static com.ib.system.utils.SearchUtils.asDate;


public class RegistriDocsPrint  extends BaseExport implements Serializable{
	
	/**
	 * 
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(OpisiExport.class);
	private static final long serialVersionUID = -253296390051131479L;
	private int lang;
	private int i =0;
	private String registraturaName="";
	private String registerName="";
	private String registerInterval="";
	private String paramEkz="";
	private String paramJourn="";
    private transient OutputStreamWriter osw;
    private SimpleDateFormat sDfp = new SimpleDateFormat("dd.MM.yyyy");
    private StringBuffer strB;
    private transient List<Object[]> ekzVlojeniList;
    private transient List<Object[]> destrDocsList;
    private transient List<Object[]> archDocsList;
    private transient List<Object[]> dvigDocsList;
    private transient List<Object[]> prilList;
    private transient List<Object[]> linkDocList;
    private transient List<Object[]> storeDocsList;
    //Excel POI
    private int typePr=0;
    private int rN; // Excel row N
    private int cN;	// Excel col N
    private transient HSSFWorkbook wb;
    private transient HSSFSheet sheet;
    private transient HSSFRow row; 
    private transient HSSFCell cell;
    private transient CellRangeAddress region;
    private transient HSSFCellStyle csBtArFb;
    private transient HSSFCellStyle csArFb;
    private transient HSSFCellStyle csAlFr;
    private transient HSSFCellStyle csBbAcFr;
    private transient HSSFCellStyle csAcFb;
    private transient HSSFCellStyle csBtAlFr;
    private transient HSSFCellStyle rsVAt;
    
    public static final  String BORDERTOP = "border-top: #808080 1px solid;";
    public static final  String RIGHTALIG = "right";
    public static final  String BLANKSP = "&nbsp;";
    
    public static final  String OFICIALL = "prRegDoc.oficS";
    public static final  String WORKL = "prRegDoc.workS";
    public static final  String EKZL = "docu.ekzN";
    public static final  String NADATE = "refDeleg.date";
    public static final  String VALIDL = "docDestruction.valid";
    public static final  String TDTR = "</td></tr>";
    public static final  String TR = "<tr>";
    public static final  String TRE = "</tr>";
    public static final  String BEGROWLINE = "<tr><td align=\"center\" colspan=\"16\" style=\"";
    public static final  String ENDROW = "\" />";
    public static final  String ROWSTYL = "<tr style=\"";
      
    /** Стил за редовете от справката - с бордер  */
    public static final String STYLEROWBEZTB =  styleRowBez + BORDERTOP;  // С черта отгоре
    
    /** Стил за редовете от справката - с бордер bold */
    public static final String STYLEROWBOLDBEZTB = styleRowBoldBez + BORDERTOP;  // С черта отгоре;
    
 
    
	public RegistriDocsPrint()  {
		lang = getCurrentLang();
	    osw = null;
	    this.ekzVlojeniList = new ArrayList<>();
	    this.destrDocsList = new ArrayList<>();
	    this.archDocsList = new ArrayList<>();
	    this.dvigDocsList = new ArrayList<>();
	    this.prilList = new ArrayList<>();
	    this.linkDocList = new ArrayList<>();
	    this.storeDocsList = new ArrayList<>();
	    
	}
	
			
	public boolean registerDocsExport (List<Object[]> regDocList, Integer register, Date datOt, Date datDo, List<String> inclOpt, String contentType, String fileName) 
			throws DbErrorException  {	

		Integer registratura=getUserData(UserData.class).getRegistratura();
	
		try {
			Registratura r = new RegistraturaDAO(getUserData()).findById(registratura);
			
			paramEkz = getSystemData().getSettingsValue("delo.docWithExemplars"); // да се работи с екземпляри
			
			if (null!=r.getRegistratura() && !r.getRegistratura().isEmpty())
				registraturaName+=r.getRegistratura().trim();
			if (null!=r.getOrgName() && ! r.getOrgName().isEmpty())
				registraturaName+="/"+r.getOrgName().trim();
			
			if (null!=registraturaName && !"".equalsIgnoreCase(registraturaName))// Регистратура стринг
				registraturaName=getMessageResourceString(LABELS, "regData.registratura") +":"+  registraturaName; 
			
			
			registerName=getSystemData().decodeItem(CODE_CLASSIF_REGISTRI, register, lang, new Date());

			if (null!=registerName && !"".equalsIgnoreCase(registerName)) //Регистър
				registerName=getMessageResourceString(LABELS, "docu.register").toUpperCase() + " '"+registerName.trim()+"'";

			
			registerInterval+= getMessageResourceString(beanMessages, "prRegDoc.periodOt")+" "+sDfp.format(datOt) + " "+getMessageResourceString(LABELS, "refDeleg.do")+": "+ sDfp.format(datDo); 
			
		
		
		
		} catch (DbErrorException e) {
			throw new DbErrorException(getMessageResourceString(beanMessages, "prRegDoc.errorFindRegs"), e);

		}	

		strB = new StringBuffer();
		if (contentType.contains("excel")) {
			typePr=1;
		}else {
			typePr=0;
		}
		
		try
		  {
		
			FacesContext fContext = FacesContext.getCurrentInstance();	
			HttpServletResponse response = (HttpServletResponse) fContext.getExternalContext().getResponse();
			ServletOutputStream out = response.getOutputStream();
			response.setContentType(contentType);
			response.setHeader("Content-Disposition", "attachment;filename=\""+ fileName + "\"");
			
			if(typePr==0) {
				osw = new OutputStreamWriter(out, StandardCharsets.UTF_8);
				
			}
				
			generateHeadReport(contentType,fileName);
			generateBodyReport(regDocList, inclOpt);
			
			if(typePr==0) {
				strB.delete(0, strB.length());
				strB.append( BEGROWLINE+ styleRow11Bold +  BORDERTOP+"\"> "+ ""+TDTR) ;
				strB.append("<tr><td  align=\"left\">"+sDfp.format(new Date())+"</td>"+
					 		"<td align=\"right\" colspan=\"15\">"+ getMessageResourceString(beanMessages, "prRegDoc.izgotvil") +getUserData().getLiceNames()+TDTR);
				
				strB.append("</table></body></html>");
				osw.write(strB.toString());
				osw.flush();
				osw.close();

			}else if(typePr==1) {
				rN++;
        		row=sheet.createRow((short)rN);
        		cN=0;
        		createCell(rN, cN, 13, csBbAcFr,2);
				wb.write(out);
				wb.close();

			}
			
			
			out.flush();
			out.close();
			fContext.responseComplete();
			
			// Record in journal
			
			paramJourn = getSystemData().getSettingsValue("delo.journalExportOpis"); // да се журналира експорта
			
			if (null!=paramJourn && paramJourn.equals("1")) {
				try {
					String opisName = "Опис на регистри за документи: "+registraturaName+ " "+registerName+ " "+registerInterval;     // името на описа
		            
					Integer count = Integer.valueOf(i);    // брой обекти в описа
		            
		            OpisDAO opisDao = new OpisDAO(getUserData());
		            		
            		JPA.getUtil().runInTransaction(()->opisDao.journalOpis(opisName, count));		
		                
				} catch (BaseException e) {
		                        LOGGER.error(e.getMessage(), e);
		                        JSFUtils.addErrorMessage(getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e);
		        }
			}
		  
		  } catch (Exception e){
			  throw new DbErrorException(e.getMessage());
		  }	
		
		
		return true;
	}
	
	
	public void generateHeadReport(String contentType, String fName) throws DbErrorException  {
		
		try {
					
			if (typePr==0) {
				strB.delete(0, strB.length());
				strB.append(
					"<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">" +
					"<html xmlns=\"http://www.w3.org/1999/xhtml\">" +
					"<head>" +
					"<title>"+fName+"</title>"+
					"<meta http-equiv=\"Content-Type\" content=\" "+contentType+ ENDROW+
					"</head>" +
					"<body>"
					
				);
		
				osw.write(strB.toString());
				
				strB.delete(0, strB.length());
				strB.append("<br/>");
				strB.append("<table style=\""+ styleTable11pt+ "\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" width=\"100%\">" ) ;
				
				// Име на регистратура
				strB.append(BEGROWLINE+ styleRow11+"\">" + registraturaName +TDTR);
        		strB.append(BEGROWLINE+ styleRow11Bold +  BORDERTOP+"\">"+ ""+TDTR) ;
        		osw.write(strB.toString());
        		strB.delete(0, strB.length());
        		
        		// Заглавие
        		strB.append(ROWSTYL+ styleRowBez +ENDROW);
		        strB.append(ROWSTYL+ styleRowBez +ENDROW);
	        	strB.append("<tr><td  align=\"center\" colspan=\"16\" style=\""+ styleHeader12 +"\">" + registerName.trim()+TDTR);
	        	osw.write(strB.toString());
        		strB.delete(0, strB.length());
        		
        		//За период
        		strB.append("<tr><td  align=\"center\" colspan=\"16\" style=\""+ styleHeader12 +"\"> "+registerInterval+TDTR);
		        strB.append(ROWSTYL+ styleRowBez +ENDROW);
				strB.append(ROWSTYL+ styleRowBez +ENDROW);
				osw.write(strB.toString());
        		strB.delete(0, strB.length());
        		
			
			}else {// Excel
			
				wb = new HSSFWorkbook();
				sheet = wb.createSheet("Sheet1");
				
				sheet.getPrintSetup().setLandscape(true);
	    		sheet.getPrintSetup().setPaperSize(HSSFPrintSetup.A4_PAPERSIZE);
	    		
	    		sheet.getPrintSetup().setFitWidth((short)1);
	
	    		sheet.setMargin(Sheet.RightMargin, 0.3);// в inch
				sheet.setMargin(Sheet.LeftMargin, 0.3);
				sheet.setMargin(Sheet.TopMargin, 0.3);
				sheet.setMargin(Sheet.BottomMargin, 0.7);
				
				
	    		HSSFFooter footer = sheet.getFooter();
	    		footer.setLeft(HSSFFooter.fontSize((short)9)+HeaderFooter.date()+ " "+HeaderFooter.time());
	    		footer.setCenter(HSSFFooter.fontSize((short)9)+"Стр. "+HeaderFooter.page()+ "/"+HeaderFooter.numPages());
				footer.setRight(HSSFFooter.fontSize((short)9)+getMessageResourceString(beanMessages, "prRegDoc.izgotvil") +getUserData().getLiceNames());
				
				region =new CellRangeAddress((short)0,(short)0,(short)0,(short)0);
				
				HSSFFont font;
			    HSSFFont fontb;
				font= wb.createFont();
				font.setFontHeightInPoints((short)11);
				font.setFontName("Arial");
				font.setBold(false);
				fontb= wb.createFont();
				fontb.setFontHeightInPoints((short)11);
				fontb.setFontName("Arial");
				fontb.setBold(true);
				
				HSSFCellStyle csBtArFr;
				HSSFCellStyle csBbAlFr;
				HSSFCellStyle csAlFb;
				HSSFCellStyle csArFr;
				
				// Cell Styles
				csBtArFb = wb.createCellStyle();//Border top, Align Right, Font bold
				csBtArFb.setFont(fontb);
				csBtArFb.setBorderTop(BorderStyle.THIN);
				csBtArFb.setAlignment(HorizontalAlignment.RIGHT);
				csBtArFb.setVerticalAlignment(VerticalAlignment.TOP);
				
				
				csBtAlFr = wb.createCellStyle();//Border top, Align Left, Font reg
				csBtAlFr.setFont(font);
				csBtAlFr.setBorderTop(BorderStyle.THIN);
				csBtAlFr.setAlignment(HorizontalAlignment.LEFT);
				csBtAlFr.setVerticalAlignment(VerticalAlignment.TOP);
				
				csBtArFr = wb.createCellStyle();//Border top, Align Right, Font reg
				csBtArFr.setFont(font);
				csBtArFr.setBorderTop(BorderStyle.THIN);
				csBtArFr.setAlignment(HorizontalAlignment.RIGHT);
				csBtArFr.setVerticalAlignment(VerticalAlignment.TOP);
				
				
				csBbAlFr = wb.createCellStyle();//Border bot, Align Left, Font reg
				csBbAlFr.setFont(font);
				csBbAlFr.setBorderBottom(BorderStyle.THIN);
				csBbAlFr.setAlignment(HorizontalAlignment.LEFT);
				csBbAlFr.setVerticalAlignment(VerticalAlignment.TOP);
				
				csArFb = wb.createCellStyle();//Font bold, Align Right
				csArFb.setFont(fontb);
				csArFb.setAlignment(HorizontalAlignment.RIGHT);
				csArFb.setVerticalAlignment(VerticalAlignment.TOP);
				
				csAlFb = wb.createCellStyle();//Font bold,Align Left
				csAlFb.setFont(fontb);
				csAlFb.setAlignment(HorizontalAlignment.LEFT);
				csAlFb.setVerticalAlignment(VerticalAlignment.TOP);
				
				csArFr = wb.createCellStyle();//Font reg, Align Right
				csArFr.setFont(font);
				csArFr.setAlignment(HorizontalAlignment.RIGHT);
				csArFr.setVerticalAlignment(VerticalAlignment.TOP);
				
				csAlFr = wb.createCellStyle();//Font reg, Align Left
				csAlFr.setFont(font);
				csAlFr.setAlignment(HorizontalAlignment.LEFT);
				csAlFr.setWrapText(true);
				csAlFr.setVerticalAlignment(VerticalAlignment.TOP);
				
				csBbAcFr = wb.createCellStyle();//Border bot, Align Center, Font reg
				csBbAcFr.setFont(font);
				csBbAcFr.setBorderBottom(BorderStyle.THIN);
				csBbAcFr.setAlignment(HorizontalAlignment.CENTER);
				csBbAcFr.setWrapText(true);
				csBbAcFr.setVerticalAlignment(VerticalAlignment.TOP);
				
				csAcFb = wb.createCellStyle();//Align Center, Font bold
				csAcFb.setFont(fontb);
				csAcFb.setAlignment(HorizontalAlignment.CENTER);
				csAcFb.setVerticalAlignment(VerticalAlignment.TOP);
				
				//Row Style
				rsVAt = wb.createCellStyle();// Vertical align Top
				rsVAt.setVerticalAlignment(VerticalAlignment.TOP);
				
				// Име на регистратура
				rN=0;
        		row=sheet.createRow((short)rN);
        		cN=0;
        		createCell(rN, cN, 13, csBbAcFr,2);
        		cell.setCellValue(registraturaName);
        		
        		// Заглавие
        		rN=1;
        		row=sheet.createRow((short)rN);
        		rN=2;
        		row=sheet.createRow((short)rN);
        		row.setRowStyle(rsVAt);
        		cN=0;
           		createCell(rN, cN, 13, csAcFb, 0);
        		cell.setCellValue(registerName.trim());
        		
        		// За период
        		rN++;
        		row=sheet.createRow((short)rN);
        		row.setRowStyle(rsVAt);
        		cN=0;
        		createCell(rN, cN, 13, csAcFb, 0);
        		cell.setCellValue(registerInterval);
        		rN++;
        		row=sheet.createRow((short)rN);
	
			}


		} catch (IOException e) {
			throw new DbErrorException(e.getMessage());
		}		
	
	}
	
	

	
//	@SuppressWarnings("unchecked")
	public void generateBodyReport(List<Object[]> regDocList, List<String> inclOpt) throws DbErrorException {
		
		if (regDocList.isEmpty()) {
			if (typePr==0) {
				strB.append("<tr><td  align=\"center\" colspan=\"16\" style=\"font-weight: bold;\">"+getMessageResourceString(beanMessages, "opis.noResuls")+TDTR);
				strB.append(ROWSTYL+ styleRowBez +ENDROW);
				strB.delete(0, strB.length());
			}else{//Excel
        		rN++;
        		row=sheet.createRow((short)rN);
        		row.setRowStyle(rsVAt);
        		cN=0;
        		createCell(rN, cN, 13, csAcFb, 0);
        		cell.setCellValue(getMessageResourceString(beanMessages, "opis.noResuls"));
        		rN++;
        		row=sheet.createRow((short)rN);
        	}
			
			return;
		}
		
		String s=null;
		i=0;
		int n=0;
		
		try { 
		
		for (Object[] item: regDocList) {
			i++;
			
			// Първи ред за документ
			if (typePr==0) {
				strB.delete(0, strB.length());
				strB.append(TR);
	    		addNewColumn(strB, i, "3%", STYLEROWBOLDBEZTB, null, "top", null, null, null);
	    		addNewColumn(strB, getMessageResourceString(LABELS, "docDestruction.regNum")+":",  "6%", STYLEROWBOLDBEZTB, RIGHTALIG, "top", null, null, null);				
	   		
			}else{// Excel
	   			rN++;
        		row=sheet.createRow((short)rN);
        		row.setRowStyle(rsVAt);
        		cN=1;
        		createCell(rN, cN, 1, csBtAlFr, 1);
        		cell.setCellValue(i);
        		cN=2;
        		createCell(rN, cN, 2, csBtArFb, 1);
        		cell.setCellValue(getMessageResourceString(LABELS, "docDestruction.regNum")+":");
        		sheet.autoSizeColumn((short)cN, true);
        	}
			
	   		
	   		
	   		
	   		if (null!=item[1]) {
	   			s = asString(item[1]);  // Рег. номер
				if (null!=item[4])
					s += "/" + sDfp.format(asDate(item[4]));
							
			} else {
				if (null!=item[4]) //Дата рег.
					s = "/" + sDfp.format(asDate(item[4]));
				
			}
			
			if (s != null && !"".equals(s.trim())){
				if (typePr==0) {
					addNewColumn(strB, s, "42%", STYLEROWBEZTB, "left", "top", null, null, null);
				
				}else{// Excel
	        		cN=3;
	        		createCell(rN, cN, 5, csBtAlFr, 1);
	        		cell.setCellValue(s);
	        	}
			}else{
				if (typePr==0) {
					addNewColumn(strB, BLANKSP, "42%", STYLEROWBEZTB, "left", "top", null, null, null);
				
				}else{//Excel
	        		cN=3;
	        		createCell(rN, cN, 5, csBtAlFr,1);
	        		cell.setCellValue(" ");
	        	}
			}			
			
			if (typePr==0) {
				addNewColumn(strB, getMessageResourceString(beanMessages, "prRegDoc.brOrEkz")+":", "6%", STYLEROWBOLDBEZTB, RIGHTALIG, "top", null, null, null);
			}else{//Excel
        		cN=6;
        		createCell(rN, cN, 6, csBtArFb, 1);
        		cell.setCellValue(getMessageResourceString(beanMessages, "prRegDoc.brOrEkz")+":");
        		sheet.autoSizeColumn((short)cN, true);
        	}
			
			
			if (null!=item[21]){//Брой ориг.
				if (typePr==0) {
					addNewColumn(strB, asInteger(item[21]).toString(), "3%", STYLEROWBEZTB, "center", "top", null, null, null);	
				}else{
	        		cN=7;
	        		createCell(rN, cN, 7, csBtAlFr, 1);
	        		cell.setCellValue(asInteger(item[21]).toString());
	        	}
			
			}else{
				if (typePr==0) {
					addNewColumn(strB, BLANKSP, "3%", STYLEROWBEZTB, "left", "top", null, null, null);
				}else{
	        		cN=7;
	        		createCell(rN, cN, 7, csBtAlFr, 1);
	        		cell.setCellValue(" ");
	        	}
		    }
			if (typePr==0) {
				addNewColumn(strB, getMessageResourceString(beanMessages, "prRegDoc.brRazEkz")+":", "6%", STYLEROWBOLDBEZTB, RIGHTALIG, "top", null, null, null);
			}else{
        		cN=8;
        		createCell(rN, cN, 8, csBtArFb, 1);
        		cell.setCellValue(getMessageResourceString(beanMessages, "prRegDoc.brRazEkz")+":");
        		sheet.autoSizeColumn((short)cN, true);
        	}
		    
		    if (null!=item[20]){//Бр. размножени
		    	if (typePr==0) {
		    		addNewColumn(strB, asInteger(item[20]).toString(), "3%", STYLEROWBEZTB, "center", "top", null, null, null);
		    	}else{
	        		cN=9;
	        		createCell(rN, cN, 9, csBtAlFr, 1);
	        		cell.setCellValue(asInteger(item[20]).toString());
	        	}
		    	
		    }else{
		    	if (typePr==0) {
		    		addNewColumn(strB, BLANKSP, "3%", STYLEROWBEZTB, "left", "top", null, null, null);
		    	}else{
	        		cN=9;
	        		createCell(rN, cN, 9, csBtAlFr, 1);
	        		cell.setCellValue(" ");
//	        		sheet.autoSizeColumn((short)cN, true);
	        	}
		    }
		    // Брой приложения файлове
		    if (typePr==0) {
		    	addNewColumn(strB, getMessageResourceString(beanMessages, "prRegDoc.brAttFiles")+":", "6%", STYLEROWBOLDBEZTB, RIGHTALIG, "top", null, null, null);
		    }else{
        		cN=10;
//        		createCell(rN, cN, 10, HorizontalAlignment.RIGHT, 1, BorderStyle.THIN, true);
        		createCell(rN, cN, 10, csBtArFb, 1);
        		cell.setCellValue(getMessageResourceString(beanMessages, "prRegDoc.brAttFiles")+":");
        		sheet.autoSizeColumn((short)cN, true);
        	}
		    
		    if (null!=item[19]) {
		    	if(asInteger(item[19]).intValue()>0) {
		    		if (typePr==0) {
		    			addNewColumn(strB, asInteger(item[19]).toString(), "3%", STYLEROWBEZTB, "center", "top", null, null, null);
		    		}else{
		        		cN=11;
//		        		createCell(rN, cN, 11, HorizontalAlignment.LEFT, 1, BorderStyle.THIN, false);
		        		createCell(rN, cN, 11, csBtAlFr, 1);
		        		cell.setCellValue(asInteger(item[19]).toString());
//		        		sheet.autoSizeColumn((short)cN, true);
		        	}
		    	}else {
		    		if (typePr==0) {
		    			addNewColumn(strB, BLANKSP, "3%", STYLEROWBEZTB, "left", "top", null, null, null);	
		    		}else{
		        		cN=11;
//		        		createCell(rN, cN, 11, HorizontalAlignment.LEFT, 1, BorderStyle.THIN, false);
		        		createCell(rN, cN, 11, csBtAlFr, 1);
		        		cell.setCellValue(" ");
//		        		sheet.autoSizeColumn((short)cN, true);
		        	}
		    	}
		    } else{
		    	if (typePr==0) {
		    		addNewColumn(strB, BLANKSP, "3%", STYLEROWBEZTB, "left", "top", null, null, null);	
		    	}else{
	        		cN=11;
//	        		createCell(rN, cN, 11, HorizontalAlignment.LEFT, 1, BorderStyle.THIN, false);
	        		createCell(rN, cN, 11, csBtAlFr, 1);
	        		cell.setCellValue(" ");
//	        		sheet.autoSizeColumn((short)cN, true);
	        	}
		    }
		    
		    // Брой листа
		    if (typePr==0) {
		    	addNewColumn(strB, getMessageResourceString(beanMessages, "prRegDoc.brLists")+":", "6%", STYLEROWBOLDBEZTB, RIGHTALIG, "top", null, null, null);
		    }else{
        		cN=12;
//        		createCell(rN, cN, 12, HorizontalAlignment.RIGHT, 1, BorderStyle.THIN, true);
        		createCell(rN, cN, 12, csBtArFb, 1);
        		cell.setCellValue(getMessageResourceString(beanMessages, "prRegDoc.brLists")+":");
        		sheet.autoSizeColumn((short)cN, true);
        	}
		    
		    if (null!=item[22]){
		    	if (typePr==0) {
		    		addNewColumn(strB, asInteger(item[22]).toString(), "2%", STYLEROWBEZTB, "center", "top", null, null, null);
		    	}else{
	        		cN=13;
//	        		createCell(rN, cN, 13, HorizontalAlignment.LEFT, 1, BorderStyle.THIN, false);
	        		createCell(rN, cN, 13, csBtAlFr, 1);
	        		cell.setCellValue(asInteger(item[22]).toString());
//	        		sheet.autoSizeColumn((short)cN, true);
	        	}
		    	
		    }else{
		    	if (typePr==0) {
		    		addNewColumn(strB, BLANKSP, "2%", STYLEROWBEZTB, "left", "top", null, null, null);
		    	}else{
	        		cN=13;
//	        		createCell(rN, cN, 13, HorizontalAlignment.LEFT, 1, BorderStyle.THIN, false);
	        		createCell(rN, cN, 13, csBtAlFr, 1);
	        		cell.setCellValue(" ");
//	        		sheet.autoSizeColumn((short)cN, true);
	        	}
		    } 
		    /*// Общ Брой листа да се направи!!!!!- да се вземе от приложенията! - В системата няма приложения на този етап
		    addNewColumn(strB, "ОбщБрЛ", "7%", STYLEROWBOLDBEZTB, "right", "top", null, null, null);
		    if (null!=item[22]){
		    	addNewColumn(strB, asInteger(item[22]).toString(), "4%", styleRowBezTB, "center", "top", null, null, null);	
		    }else{
				addNewColumn(strB, "&nbsp;", "4%", styleRowBezTB, "left", "top", null, null, null);
		    } */
		    if (typePr==0) {	
		    	strB.append(TRE);
		    	osw.write(strB.toString());
		    }
	        
	        //2 ред Техен №/Идва от
         	s="";
         	if (null!=item[5] && asInteger(item[5]).intValue()==(OmbConstants.CODE_ZNACHENIE_DOC_TYPE_IN) && 
         		(null!=item[13] || null!=item[14] || null!=item[30]) ) {// Tip doc vhoden
         			if (typePr==0) {
	         			strB.delete(0, strB.length());
	        			strB.append(TR);
         			}else{
         				rN++;
                		row=sheet.createRow((short)rN);
                		row.setHeightInPoints(2 * sheet.getDefaultRowHeightInPoints());
        			}

	         		if (null!=item[13])// tehen nomer
	         			s=asString(item[13]);
	         		
	         		if (null!=item[14])// Date tehen nomer
	         			s+= "/ "+getMessageResourceString(LABELS, "docu.dateDoc")+": "+ sDfp.format(asDate(item[14]));
	         		
	         		if (!s.equals("")) {
	         			if (typePr==0) {
		         			addNewColumn(strB, getMessageResourceString(beanMessages, "prRegDoc.tehNom")+":", "9%", styleRowBoldBez, RIGHTALIG, "top", null, "2", null);
		         			addNewColumn(strB, s, "42%", styleRowBez, "left", "top", null, null, null);
	         			
	         			}else{
	                		cN=0;
	                		createCell(rN, cN, 2, csArFb, 0);
	                		cell.setCellValue(getMessageResourceString(beanMessages, "prRegDoc.tehNom")+":");
	                		
	                		cN=3;
	    	        		createCell(rN, cN, 5, csAlFr, 0);
	    	        		cell.setCellValue(s);

	                	}
	         		}else {
	         			if (typePr==0) {
	         				addNewColumn(strB, BLANKSP, "51%", styleRowBez, "left", "top", null, "3", null);
	         			}else{
	                		cN=0;
	                		createCell(rN, cN, 5, csAlFr, 0);
	                		cell.setCellValue("");
//	                		sheet.autoSizeColumn((short)cN, true);
	         			}
	         		}

	         		if (null!=item[30] && !asString(item[30]).equals("")) { // Idva ot
	         			s=asString(item[30]);
	         			if (null!=item[31] && !asString(item[31]).equals(""))
	         				s+= " "+getMessageResourceString(beanMessages, "prRegDoc.adress")+": "+asString(item[31]);
	         			if (null!=item[32] && !asString(item[32]).equals(""))
	         				s+= ", "+getMessageResourceString(beanMessages, "prRegDoc.nm")+" "+asString(item[32]);
	         			if (null!=item[33] && !asString(item[33]).equals(""))
	         				s+= ", "+getMessageResourceString(beanMessages, "prRegDoc.obl")+" "+asString(item[33]);
	         			if (null!=item[34] && !asString(item[34]).equals(""))
	         				s+= ", "+getMessageResourceString(beanMessages, "prRegDoc.obst")+" "+asString(item[34]);
	         			if (!s.equals("")) {
	         				if (typePr==0) {
		         				addNewColumn(strB, getMessageResourceString(LABELS, "docu.idvaOt")+": ", "6%", styleRowBoldBez, RIGHTALIG, "top", null, null, null);
		         				addNewColumn(strB, s, "43%", styleRowBez, "left", "top", null, "9", null);
	         				}else{
		                		cN=6;
		                		createCell(rN, cN, 6, csArFb, 0);
		                		cell.setCellValue(getMessageResourceString(LABELS, "docu.idvaOt")+":");
//		                		sheet.autoSizeColumn((short)cN, true);
		                		
		                		cN=7;
		    	        		createCell(rN, cN, 13,csAlFr, 0);
		    	        		cell.setCellValue(s);
//		    	        		sheet.autoSizeColumn((short)cN, true);
		                	}
	         				
	         			}
	         			
	      			
	         		}

	         		
	         		if (typePr==0) {
	         			strB.append(TRE);
	         			osw.write(strB.toString());
	         		}
         		
//         		}
         	    
             }
	
         	//3 ред - вид документ и тип документ
//         	s="";
         	if (typePr==0) {
	         	strB.delete(0,strB.length());
	         	strB.append(TR);
         	}else{
 				rN++;
        		row=sheet.createRow((short)rN);
        		row.setHeightInPoints(2 * sheet.getDefaultRowHeightInPoints());
			}
         	
         	
         // Вид документ
         	if (typePr==0) {
         		addNewColumn(strB, getMessageResourceString(LABELS, "docu.vid")+": ", "9%", styleRowBoldBez, RIGHTALIG, "top", null, "2", null);
         	}else{
        		cN=0;
        		createCell(rN, cN, 2, csArFb, 0);
        		cell.setCellValue(getMessageResourceString(LABELS, "docu.vid")+":");
 //       		sheet.autoSizeColumn((short)cN, true);
         	}
         	
         	if (null!=item[29]){
         		if (typePr==0) {
         			addNewColumn(strB, asString(item[29]), "42%", styleRowBez, "left", "top", null, null, null);
		    	
         		}else{
             		cN=3;
	        		createCell(rN, cN, 5, csAlFr, 0);
	        		cell.setCellValue(asString(item[29]));
//	        		sheet.autoSizeColumn((short)cN, true);
            	}

		    }else{
		    	if (typePr==0) {
		    		addNewColumn(strB, BLANKSP, "42%", styleRowBez, "left", "top", null, null, null);
		    	}else{
             		cN=3;
	        		createCell(rN, cN, 5, csAlFr, 0);
	        		cell.setCellValue(" ");
//	        		sheet.autoSizeColumn((short)cN, true);
            	}
		    } 
		    
		 // Тип документ
         	if (typePr==0) {
         		addNewColumn(strB,getMessageResourceString(LABELS, "docu.type")+": ", "3%", styleRowBoldBez, RIGHTALIG, "top", null, null, null);
         	}else{
        		cN=6;
        		createCell(rN, cN, 6, csArFb, 0);
        		cell.setCellValue(getMessageResourceString(LABELS, "docu.type")+":");
//        		sheet.autoSizeColumn((short)cN, true);
        	}
      	
         	if (null!=item[28]){
         		if (typePr==0) {
         			addNewColumn(strB, asString(item[28]), "9%", styleRowBez, "left", "top", null, "2", null);
         		}else{
	        		cN=7;
	        		createCell(rN, cN, 9,csAlFr, 0);
	        		cell.setCellValue(asString(item[28]));
//	        		sheet.autoSizeColumn((short)cN, true);
		    	}
		    }else{
		    	if (typePr==0) {
		    		addNewColumn(strB, BLANKSP, "9%", styleRowBez, "left", "top", null, "2", null);
		    	}else{
	        		cN=7;
	        		createCell(rN, cN, 9,csAlFr, 0);
	        		cell.setCellValue(" ");
//	        		sheet.autoSizeColumn((short)cN, true);
		    	}
		    } 

		    s=""; // ako e raboten tarsim ofic.document
		    if (null!=item[23] && null!=item[5] && asInteger(item[5]).intValue() == OmbConstants.CODE_ZNACHENIE_DOC_TYPE_WRK ) { // Рег. номер на официалния документ, който е създаден от работния
		    	if (null!=item[35]) {
					s = asString(item[35]);  // Рег. номер OficDoc
					if (null!=item[36])
						s += "/" + sDfp.format(asDate(item[36]));
				} else {
					if (null!=item[36]) //Дата рег.
						s = "/" + sDfp.format(asDate(item[36]));
				}
		    }
  
		    if (!"".equals(s.trim()) && (null!=item[5] && asInteger(item[5]).intValue()==(OmbConstants.CODE_ZNACHENIE_DOC_TYPE_WRK))  ) {
		    	if (typePr==0) {	
		    		addNewColumn(strB, getMessageResourceString(LABELS, "docu.officDoc")+": ", "6%", styleRowBoldBez, RIGHTALIG, "top", null, "1", null);
					addNewColumn(strB, s, "23%", styleRowBez, "left", "top", null, "5", null);
					
		    	}else{
	        		cN=10;
	        		createCell(rN, cN, 10,csArFb, 0);
	        		cell.setCellValue(getMessageResourceString(LABELS, "docu.officDoc")+": ");
	        		
	        		cN=11;
	        		createCell(rN, cN, 13,csAlFr, 0);
	        		cell.setCellValue(s);
		        		
			    }

			}
								
		    
		    if (typePr==0) {
		    	strB.append(TRE); 
		    	osw.write(strB.toString());
		    }
		 
           //4 ред -  - Кореспондент
          	s=""; 
		    if(null!=item[5] && asInteger(item[5]).intValue()!=(OmbConstants.CODE_ZNACHENIE_DOC_TYPE_IN) && null!=item[30] && !asString(item[30]).equals("")) {
		    	if (typePr==0) {
		          	strB.delete(0,strB.length());
		          	strB.append(TR);
		          	addNewColumn(strB, getMessageResourceString(LABELS, "docu.corespondent")+":", "9%", styleRowBoldBez, RIGHTALIG, "top", null,"2", null);
		    	}else{
	 				rN++;
	        		row=sheet.createRow((short)rN);
	        		cN=0;
	        		createCell(rN, cN, 2, csArFb, 0);
	        		cell.setCellValue(getMessageResourceString(LABELS, "docu.corespondent")+":");
				}
	          		
				
				s=asString(item[30]);
     			if (null!=item[31] && !asString(item[31]).equals(""))
     				s+= " "+getMessageResourceString(beanMessages, "prRegDoc.adress")+": "+asString(item[31]);
     			if (null!=item[32] && !asString(item[32]).equals(""))
     				s+= ", "+getMessageResourceString(beanMessages, "prRegDoc.nm")+" "+asString(item[32]);
     			if (null!=item[33] && !asString(item[33]).equals(""))
     				s+= ", "+getMessageResourceString(beanMessages, "prRegDoc.obl")+" "+asString(item[33]);
     			if (null!=item[34] && !asString(item[34]).equals(""))
     				s+=", "+getMessageResourceString(beanMessages, "prRegDoc.obst")+" "+asString(item[34]);
     			if (!s.equals("")) {
     				if (typePr==0) {
     					addNewColumn(strB, s, "89%", styleRowBez, null, "top", null, "13", null);
     				}else{
    	        		cN=3;
    	        		createCell(rN, cN, 13,csAlFr, 0);
    	        		cell.setCellValue(s);
//    	        		sheet.autoSizeColumn((short)cN, true);
    		    	}
     			}
				
				
				if (typePr==0) {
					strB.append(TRE);
					osw.write(strB.toString());
				}
           	     
            }  
	        
		   // 5 ред - Avtori
		  
           if (null!=item[12] && ! asString(item[12]).equals("")) {
	        	 s = asString(item[12]);
	        	 if (!"".equals(s.trim())) {
	        		s=((SystemData)getSystemData()).decodeItems(OmbConstants.CODE_CLASSIF_ADMIN_STR_REPORTS, s, lang, new Date());
	        		if (typePr==0) {
		        		strB.delete(0,strB.length());
		         		strB.append(TR);
		         		addNewColumn(strB, getMessageResourceString(beanMessages, "prRegDoc.avtor_i")+":", "9%", styleRowBoldBez, RIGHTALIG, "top", null, "2", null);
		        		addNewColumn(strB, s, "60%", styleRowBez, "left", "top", null, "7", null);
	        		}else{
		 				rN++;
		        		row=sheet.createRow((short)rN);
		        		cN=0;
		        		createCell(rN, cN, 2, csArFb, 0);
		        		cell.setCellValue(getMessageResourceString(beanMessages, "prRegDoc.avtor_i")+":");
		        		cN=3;
    	        		createCell(rN, cN, 13,csAlFr, 0);
    	        		cell.setCellValue(s);
					}
				   		              
					
					if (typePr==0) {
						strB.append(TRE); 
						osw.write(strB.toString());
					}
	        	 }    
		    }  
           
           // 6 ред - Относно
		   if (inclOpt.contains("1") && null!=item[7] && ! asString(item[7]).equals("")) {
			   s=asString(item[7]).trim();
			   if (!"".equals(s.trim())) {
				   if (typePr==0) {
					   strB.delete(0,strB.length());
					   strB.append(TR);
					   addNewColumn(strB, getMessageResourceString(beanMessages, "opis.anotExport")+":", "9%", styleRowBoldBez, RIGHTALIG, "top", null,"2", null);
					   addNewColumn(strB, s, "89%", styleRowBez, "left", "top", null, "13", null);
				   }else{
		 				rN++;
		        		row=sheet.createRow((short)rN);
		        		row.setHeightInPoints(2 * sheet.getDefaultRowHeightInPoints());
		        		cN=0;
		        		createCell(rN, cN, 2, csArFb, 0);
		        		cell.setCellValue(getMessageResourceString(beanMessages, "opis.anotExport")+":");
		        		cN=3;
	   	        		createCell(rN, cN, 13,csAlFr, 0);
	   	        		cell.setCellValue(s);
					}

				   
				   if (typePr==0) {
					   strB.append(TRE);
					   osw.write(strB.toString());
				   }
			   } 
           }
		   
		   
		   // 7 ред допълн. информация 
		   if (inclOpt.contains("4") && null!=item[25] && ! asString(item[25]).equals("")) {
			   s=asString(item[25]).trim();
			   if (!"".equals(s.trim())) {
				   if (typePr==0) {
					   strB.delete(0,strB.length());
					   strB.append(TR);
					   addNewColumn(strB, getMessageResourceString(LABELS, "task.dopInfo")+":", "9%", styleRowBoldBez, RIGHTALIG, "top", null,"2", null);
					   addNewColumn(strB, s, "89%", styleRowBez, "left", "top", null, "13", null);
				   }else{
		 				rN++;
		        		row=sheet.createRow((short)rN);
		        		row.setHeightInPoints(2 * sheet.getDefaultRowHeightInPoints());
		        		cN=0;
		        		createCell(rN, cN, 2, csArFb, 0);
		        		cell.setCellValue(getMessageResourceString(LABELS, "task.dopInfo")+":");
		        		cN=3;
	   	        		createCell(rN, cN, 13,csAlFr, 0);
	   	        		cell.setCellValue(s);
					}
				   

				   
				   if (typePr==0) {
					   strB.append(TRE); 
					   osw.write(strB.toString());
				   }
			   } 
           }
		             
		   // 8 ред Получаване 
		   if (null!=item[0] && (null != item[40] || null != item[41] || null != item[39])) {
			  
			  s=getMessageResourceString(LABELS, "docu.received")+":";
			  if (typePr==0) {
				  strB.delete(0,strB.length());
				  strB.append(TR);
				  addNewColumn(strB, s, "9%", styleRowBoldBez, RIGHTALIG, "top", null, "2", null);
			  }else{
 				rN++;
        		row=sheet.createRow((short)rN);
        		row.setHeightInPoints(2 * sheet.getDefaultRowHeightInPoints());
        		cN=0;
        		createCell(rN, cN, 2, csArFb, 0);
        		cell.setCellValue(s);
			  }

			  
			  s="";
			 			  
			  if(null!=item[39])
				  s+=" "+getMessageResourceString(LABELS, "dvijenie.nachin")+": "+getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_DVIJ_METHOD, asInteger(item[39]), Integer.valueOf(lang), new Date());
			  
			 
			  if (null!=item[40]) {	
				  if (! s.isEmpty())
					  s+=", ";
				  s+=getMessageResourceString(LABELS, NADATE)+": "+sDfp.format(asDate(item[40]));
			  }
			 	  
			  if (null!=item[41]) {
				  if (! s.isEmpty())
					  s+=", ";		
				  s+=getMessageResourceString(LABELS, "task.dopInfo")+": "+asString(item[41]);
			  }
			  
			  if (s != null && !"".equals(s.trim())) {
				 if (typePr==0) { 
					 addNewColumn(strB, s, "89%", styleRowBez, null, "top", null, "13", null);
				  
				 }else{
   	        		cN=3;
   	        		createCell(rN, cN, 13,csAlFr, 0);
   	        		cell.setCellValue(s);
   		    	  }  
				   
			  }

			  
			  if (typePr==0) {
				  strB.append(TRE); 
				  osw.write(strB.toString());
			  }
	
		   }
		
		   
		   
		   // 9 ред Предоставяне		   
		   
		   if (null!=item[0] && (null != item[42] || null != item[43] || null != item[45])) {
			  s=getMessageResourceString(LABELS, "docu.delivery")+":";
			  if (typePr==0) {
				  strB.delete(0,strB.length());
				  strB.append(TR);
				  addNewColumn(strB, s, "9%", styleRowBoldBez, RIGHTALIG, "top", null, "2", null);
			  }else{
 				rN++;
        		row=sheet.createRow((short)rN);
        		row.setHeightInPoints(2 * sheet.getDefaultRowHeightInPoints());
        		cN=0;
        		createCell(rN, cN, 2, csArFb, 0);
        		cell.setCellValue(s);
			  }
			  
			  s="";
			 			  
			  if(null!=item[42])
				  s+=" "+getMessageResourceString(LABELS, "dvijenie.nachin")+": "+getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_DVIJ_METHOD, asInteger(item[42]), Integer.valueOf(lang), new Date());
			  
			  if (null!=item[43]) {	
				  if (! s.isEmpty())
					  s+=", ";		
				  
				  s+=getMessageResourceString(LABELS, "docu.answerDate")+": "+sDfp.format(asDate(item[43]));
			  }
			  
			 			  
			  if (null!=item[45]) {	
				  if (! s.isEmpty())
					  s+=", ";		
				  
				  s+=getMessageResourceString(LABELS, "docList.irregular")+": "+asString(item[45]);
			  }
			  
			  if (s != null && !"".equals(s.trim())) {
				  if (typePr==0) {
					  addNewColumn(strB, s, "89%", styleRowBez, null, "top", null, "13", null);
				  }else{
   	        		cN=3;
   	        		createCell(rN, cN, 13,csAlFr, 0);
   	        		cell.setCellValue(s);
   		    	  }  
				   
			  }

			  
			  if (typePr==0) {
				  strB.append(TRE); 
				  osw.write(strB.toString());
			  }
	
		   }
		   
		   
		   // 10 ред Валидност.
		   
		   if (null!=item[26]) { // Валидност + Дата
			  
			   if (asInteger(item[26]).intValue()==(OmbConstants.CODE_CLASSIF_DOC_VALID_DESTRUCT)) {// Унищожен док-т
			    		
	    		getDestrDocs(asInteger(item[0]));
	    		if (null!=this.destrDocsList && ! this.destrDocsList.isEmpty()) {
				   n = 0;
				  
				   for (Object[]  ddItem: this.destrDocsList) {
					   s = "";
					   if(null!=ddItem[1]) {
						  if (typePr==0) { 
							  strB.delete(0,strB.length());
							  strB.append(TR);
						  }else{
			 				rN++;
			        		row=sheet.createRow((short)rN);
			        		row.setHeightInPoints(2 * sheet.getDefaultRowHeightInPoints());
						  }
						  
						  
						  if (n==0) { 
							  s+=getMessageResourceString(LABELS, VALIDL)+":";
						  }else {
							  s+=" ";
						  }
						  
						  if (typePr==0) {
							  addNewColumn(strB, s, "9%", styleRowBoldBez, RIGHTALIG, "top", null, "2", null);
						  }else{
			        		cN=0;
			        		createCell(rN, cN, 2, csArFb, 0);
			        		cell.setCellValue(s);
//			        		sheet.autoSizeColumn((short)cN, true);
						  }
						  
						  s="";
						  if (null!=item[37])
							  s += "-"+asString(item[37]);  // Валидност
						  
						  if(null!=ddItem[5] && asInteger(ddItem[5]).intValue() == OmbConstants.CODE_ZNACHENIE_DOC_TYPE_WRK) {
							  s += " "+getMessageResourceString(beanMessages, WORKL)+" ";
						  }else {
							  s += " "+getMessageResourceString(beanMessages, OFICIALL)+" "; 
						  }  
						  if (null!=ddItem[11])
							  s += asString(ddItem[11]); // Вид
						
						  if (null!=ddItem[1])
							  s += " "+asString(ddItem[1]);
						 									 
						  if(null!=ddItem[2])
							  s += "/" + sDfp.format(asDate(ddItem[2]));
						 
						  if(null!=ddItem[0] && (null!=paramEkz && paramEkz.equals("1")))
							  s += " "+getMessageResourceString(LABELS, EKZL)+" " + asString(ddItem[0]);

						  if(null!=ddItem[7] && asInteger(ddItem[7]).intValue() == OmbConstants.CODE_CLASSIF_DOC_VALID_DESTRUCT ) {
							  s += " ("+getMessageResourceString(beanMessages, "prRegDoc.destr");
							  if(null!=ddItem[8]) 
								  s += " "+getMessageResourceString(beanMessages, "prRegDoc.on") +" "+ sDfp.format(asDate(ddItem[8]));
							  s += ")";
						  }
						  
						  ///
						  if (null!=ddItem[3]) {
							  if(null!=ddItem[6] && asInteger(ddItem[6]).intValue() == OmbConstants.CODE_ZNACHENIE_DOC_TYPE_WRK) {
								 s += ", "+getMessageResourceString(beanMessages, WORKL)+" ";
							  }else {
								 s += ", "+getMessageResourceString(beanMessages, OFICIALL)+" "; 
							  } 
							  if (null!=ddItem[12])
								  s += asString(ddItem[12]); // Вид
							  
							  if (null!=ddItem[3])
								  s += " "+asString(ddItem[3]);
								 									 
							  if(null!=ddItem[4])
								  s += "/" + sDfp.format(asDate(ddItem[4]));
							  
							  if(null!=ddItem[9] && asInteger(ddItem[9]).intValue() == OmbConstants.CODE_CLASSIF_DOC_VALID_DESTRUCT ) {
								  s += " ("+getMessageResourceString(beanMessages, "prRegDoc.destr");
							  
							  if(null!=ddItem[10]) 
								  s += " "+getMessageResourceString(beanMessages, "prRegDoc.on") +" "+ sDfp.format(asDate(ddItem[10]));
							  
							  s += ")";
							  }
						  }
						  
						  if (s != null && !"".equals(s.trim())) {
							  if (typePr==0) {
								  addNewColumn(strB, s, "89%", styleRowBez, null, "top", null, "13", null);
							  }else{
			   	        		cN=3;
			   	        		createCell(rN, cN, 13,csAlFr, 0);
			   	        		cell.setCellValue(s);
			   		    	  } 
						  }

						  if (typePr==0) {
							  strB.append(TRE);   
							  osw.write(strB.toString());
						  }

					   	}
						
					   	n=1;  
					   }

	    			}else {	
	    				if (typePr==0) {
	    					strB.delete(0,strB.length());
		 				   	strB.append(TR);
		 				   	addNewColumn(strB, getMessageResourceString(LABELS, VALIDL)+":" , "9%", styleRowBoldBez, RIGHTALIG, "top", null, "2", null);
	    				}else{
	 				   		rN++;
			        		row=sheet.createRow((short)rN);
			        		row.setHeightInPoints(2 * sheet.getDefaultRowHeightInPoints());
			        		cN=0;
			        		createCell(rN, cN, 2, csArFb, 0);
			        		cell.setCellValue(getMessageResourceString(LABELS, VALIDL)+":");
						 }
	 				   	
	 				   	
	    				s="";
	 				   	if (null!=item[37])
	 				   		s = asString(item[37]);  // Валидност
		    			
	 				   	if (null!=item[38])
	 				   		s += "/" + sDfp.format(asDate(item[38]));
				    	
		    			if (s != null && !"".equals(s.trim())) {
		    				if (typePr==0) {
		    					addNewColumn(strB, s, "23%", styleRowBez, "left", "top", null, "5", null);
		    				}else{
			   	        		cN=3;
			   	        		createCell(rN, cN, 7,csAlFr, 0);
			   	        		cell.setCellValue(s);

			   		    	 } 
		    			}
						
		    			
		    			if (typePr==0) {
		    				strB.append(TRE); 
		    				osw.write(strB.toString());
		    			}
		    		}
			    	
		    	} else if(asInteger(item[26]).intValue()==(OmbConstants.CODE_CLASSIF_DOC_VALID_ARCH)) {
		    		getArchDocs(asInteger(item[0]));
		    		if (null!=this.archDocsList && ! this.archDocsList.isEmpty()) {
		    			n = 0;

						   for (Object[]  daItem: this.archDocsList) {
							   s = "";
							   String drn="";
							   if(null!=daItem[0]) {
								  if (typePr==0) {
									 strB.delete(0,strB.length());
									 strB.append(TR);
								  }else{
									 rN++;
									 row=sheet.createRow((short)rN);
									 row.setHeightInPoints(2 * sheet.getDefaultRowHeightInPoints());
								  }
					  
								  if (n==0) { 
									  s+=getMessageResourceString(LABELS, VALIDL)+":" ;
								  }else {
									  s+=" ";
								  }
								  if (typePr==0) {
									  addNewColumn(strB, s, "9%", styleRowBoldBez, RIGHTALIG, "top", null, "2", null);
								  }else{
									  cN=0;
									  createCell(rN, cN, 2, csArFb, 0);
									  cell.setCellValue(s);

								  }
								  
								  
								  s="";
								  if (null!=item[37])
									   s += "-"+asString(item[37]);  // Валидност
								  
								  if(null!=daItem[1] && asInteger(daItem[1]).intValue() == OmbConstants.CODE_ZNACHENIE_DOC_TYPE_WRK) {
									  s+=", "+getMessageResourceString(beanMessages, WORKL)+" ";
								  }else {
									  s+=", "+getMessageResourceString(beanMessages, OFICIALL)+" "; 
								  }  
								  if (null!=daItem[7])
									  s+=asString(daItem[7]); // Вид
								
								  if (null!=daItem[3]) // Рег № раб.прот.
									  s+=" "+asString(daItem[3]);
								 									 
								  if(null!=daItem[4]) // Дата
									  s += "/" + sDfp.format(asDate(daItem[4]));
								 
								  if(null!=daItem[5]) 
									  s += " "+getMessageResourceString(LABELS, "opis.tomNomer").toLowerCase() +" " + asInteger(daItem[5]);

								 
								  if(null!=daItem[6] && (null!=paramEkz && paramEkz.equals("1"))) 
									  s += " "+getMessageResourceString(LABELS, EKZL) +" "+ asInteger(daItem[6]);
					  
								  ///
									  if (null!=daItem[9]) {
										  if(asInteger(daItem[9]).intValue() == OmbConstants.CODE_ZNACHENIE_DOC_TYPE_WRK) {
											 s+=", "+getMessageResourceString(beanMessages, WORKL)+" ";
										  }else {
											 s+=", "+getMessageResourceString(beanMessages, OFICIALL)+" "; 
										  } 
									  }
									  if (null!=daItem[8])
										  s+=asString(daItem[8]); // Вид
									  
									  if (null!=daItem[11]) // Рег.№
											 s+=" "+asString(daItem[11]);
										 									 
									  if(null!=daItem[12])
									   	 s += "/" + sDfp.format(asDate(daItem[12]));
									  
									  
									  if(null!=daItem[13]) // Чрез дело regNom
										  drn+=asString(daItem[13]);
									  if(null!=daItem[14]) //Date delo
										  drn+= "/" + sDfp.format(asDate(daItem[14]));
									  if(!drn.isEmpty()) {  
										  s += ", "+ getMessageResourceString(beanMessages, "printReg.pril2DocArhL")+" "; 
										if(null!=daItem[17])
											s += asString(daItem[17])+" "+ drn + ", "+getMessageResourceString(beanMessages, "printReg.pril2DocArh"); 
									  }
									  
									  if(null!=daItem[15]) // включено в протокола на дата
										  s += " "+getMessageResourceString(LABELS, NADATE)+" "+ sDfp.format(asDate(daItem[15])); 
									  
									  
									  /*if (null!=daItem[2] || null!=daItem[3] || null!=daItem[4] || null!=daItem[5] || null!=daItem[6]) {
										  if(null!=daItem[2]) {
											  s += " "+getMessageResourceString(beanMessages, "prRegDoc.archNo")+" ";
											  s += asString(daItem[2]);
											  s+=")";
										  }
										  if(null!=daItem[1]) {
											  s += " "+getMessageResourceString(LABELS, "opis.tomNomer").toLowerCase()+" ";
											  s += asInteger(daItem[1]);
											  s+=")";
										  }
										  
										  s += " ("+getMessageResourceString(beanMessages, "prRegDoc.save");
										  if(null!=daItem[3]) {
											  s += " "+getMessageResourceString(beanMessages, "prRegDoc.room")+" ";
											  s += asString(daItem[3]);
											  s+=")";
										  }
										  if(null!=daItem[4]) {
											  s += " "+getMessageResourceString(beanMessages, "prRegDoc.shkaf")+" ";
											  s += asString(daItem[4]);
											  s+=")";
										  }
										  
										  if(null!=daItem[5]) {
											  s += " "+getMessageResourceString(beanMessages, "prRegDoc.stelag") +" ";
											  s += asString(daItem[5]);
											  s+=")";
										  }
										  
										  if(null!=daItem[6]) {
											  s += " "+getMessageResourceString(beanMessages, "prRegDoc.box") +" ";
											  s += asString(daItem[6]);
											  s+=")";
										  }
										  s += ")";
									  }*/
										 
									  
							//	  }
								  
								  
								  
								 if (s != null && !"".equals(s.trim())) {
									  if (typePr==0) {
										  addNewColumn(strB, s, "89%", styleRowBez, null, "top", null, "13", null);
									  }else{
					   	        		cN=3;
					   	        		createCell(rN, cN, 13,csAlFr, 0);
					   	        		cell.setCellValue(s);
	
									  }   
									   
								  }

								  
								  if (typePr==0) {
									  strB.append(TRE);
									  osw.write(strB.toString());
								  }

							   	}
								
							   	n=1;  
						   }
		    			
		    			
		    		}

		    	
		    	} else {
		    		if (typePr==0) {	
				    	strB.delete(0,strB.length());
						strB.append(TR);
						addNewColumn(strB, getMessageResourceString(LABELS, VALIDL)+":" , "9%", styleRowBoldBez, RIGHTALIG, "top", null, "2", null);
		    		}else{
		 				rN++;
		        		row=sheet.createRow((short)rN);
		        		row.setHeightInPoints(2 * sheet.getDefaultRowHeightInPoints());
		        		cN=0;
		        		createCell(rN, cN, 2, csArFb, 0);
		        		cell.setCellValue(getMessageResourceString(LABELS, VALIDL)+":" );
		    		}
				   
				   s="";
				   if (null!=item[37])
					   s = asString(item[37]);  // Валидност
				   
				   if (null!=item[38])
					   s += "/" + sDfp.format(asDate(item[38]));
			    	
				   if (s != null && !"".equals(s.trim())) {
					   if (typePr==0) {
						   	addNewColumn(strB, s, "23%", styleRowBez, "left", "top", null, "5", null);
					   }else{
		   	        		cN=3;
		   	        		createCell(rN, cN, 7,csAlFr, 0);
		   	        		cell.setCellValue(s);
//		   	        		sheet.autoSizeColumn((short)cN, true);
		   		    	}  
				   
				   }
	
				   
				   if (typePr==0) {
					   strB.append(TRE); 
					   osw.write(strB.toString());
				   }
			    		
			   }
		   
		   
		   }
		   
		   
		   
		// Съхраняване на вложени документи
		   if (null!=item[0]) {
		   	   getStoreDocs(asInteger(item[0]));
			   if (null!=this.storeDocsList && ! this.storeDocsList.isEmpty()) {
				   n = 1;
				  
				   for (Object[]  stItem: this.storeDocsList) {
					   s = "";
					   if (typePr==0) {
						   strB.delete(0,strB.length());
						   strB.append(TR);
					   }else{
						   rN++;
						   row=sheet.createRow((short)rN);
						   row.setHeightInPoints(2 * sheet.getDefaultRowHeightInPoints());
						}
						  
						  
						  if (n==1) { 
							  s+=getMessageResourceString(LABELS, "delo.sahranqvane")+": ";
						  }else {
							  s+=" ";
						  }
						  if (typePr==0) {
							  addNewColumn(strB, s, "9%", styleRowBoldBez, RIGHTALIG, "top", null, "2", null);
						  
						  }else{
			        		cN=0;
			        		createCell(rN, cN, 2, csArFb, 0);
			        		cell.setCellValue(s);
						  }
						  
						  
						  s="";
						  s+=n+".";
						  
						  if (null!=stItem[2])	
							  s+=" "+getMessageResourceString(LABELS, "deloStorage.arhNo")+": "+asString(stItem[2]);
						  
						  if (null!=stItem[13])	// Тип дело
							  s+=" "+getMessageResourceString(LABELS, "docu.vlojen") +" "+asString(stItem[13]);
						  
						  if (null!=stItem[10])	//Дело № и дата
							  s+=" "+asString(stItem[10]);
						  
						  if (null!=stItem[11])	
							  s+="/"+sDfp.format(asDate(stItem[11]));
						  
						  if (null!=stItem[1])	
							  s+=", "+getMessageResourceString(LABELS, "deloDvij.tom")+" № "+asInteger(stItem[1]);
						  
						  if (null!=stItem[3])	
							  s+=", "+getMessageResourceString(LABELS, "deloList.room")+" "+asString(stItem[3]);
						  
						  if (null!=stItem[4])	
							  s+=", "+getMessageResourceString(LABELS, "deloList.shkaf")+" № "+asString(stItem[4]);
						  
						  if (null!=stItem[5])	
							  s+=", "+getMessageResourceString(LABELS, "deloList.stillage")+" № "+asString(stItem[5]);
						  
						  if (null!=stItem[6])	
							  s+=", "+getMessageResourceString(LABELS, "deloList.box")+" № "+asString(stItem[6]);
						  
						  if (s != null && !"".equals(s.trim())) {
							  if (typePr==0) {
								  addNewColumn(strB, s, "89%", styleRowBez, null, "top", null, "13", null);
							  
							  }else{
			   	        		cN=3;
			   	        		createCell(rN, cN, 13,csAlFr, 0);
			   	        		cell.setCellValue(s);
			   		    	  }  
							   
						  }
						  
						  
						  if (typePr==0) {
							  strB.append(TRE); 
							  osw.write(strB.toString());
						  }

						  n++;
					   
				   }
			   }
		   }
		   
		    	
		   // 11 Ред Статус
		   if (null!=item[0] && (null != item[47] || null != item[48])) {
			   s=getMessageResourceString(LABELS, "prRegDoc.statusObr")+":";
			   if (typePr==0) {
				  strB.delete(0,strB.length());
				  strB.append(TR);
				  addNewColumn(strB, s, "9%", styleRowBoldBez, RIGHTALIG, "top", null, "2", null);
			   }else{
				  rN++;
				  row=sheet.createRow((short)rN);
				  row.setHeightInPoints(2 * sheet.getDefaultRowHeightInPoints());
				  cN=0;
				  createCell(rN, cN, 2, csArFb, 0);
				  cell.setCellValue(s);
			   }
				  
				  s="";
				 			  
				  if(null!=item[48])
					  s+=" "+asString(item[48]); 
				 
				  if (null!=item[47]) {	
					  if (! s.isEmpty())
						  s+=", ";
					  s+=getMessageResourceString(LABELS, "docu.statusDate")+": "+sDfp.format(asDate(item[47]));
				  }
				 	  
				  
				  
				  if (s != null && !"".equals(s.trim())) {
					  if (typePr==0) { 
						addNewColumn(strB, s, "89%", styleRowBez, null, "top", null, "13", null);
					  
					  }else{
	   	        		cN=3;
	   	        		createCell(rN, cN, 13,csAlFr, 0);
	   	        		cell.setCellValue(s);
	   		    	  }  
					   
				  }

				  
				  if (typePr==0) {
					  strB.append(TRE); 
					  osw.write(strB.toString());
				  }
		
			   }
		   
		   
		   
		   
			// 12 ред Приложения
		   if (inclOpt.contains("5") && null!=item[0]) {
			   getAllDocPril(asInteger(item[0]));
			   
			   if (null!=this.prilList && ! this.prilList.isEmpty()) {
				   n = 1;
				   for (Object[] pril: this.prilList) {
					  s = "";
					  if (typePr==0) {
						  strB.delete(0,strB.length());
						  strB.append(TR);
					  }else{
		 				rN++;
		        		row=sheet.createRow((short)rN);
		        		row.setHeightInPoints(2 * sheet.getDefaultRowHeightInPoints());
					  }
					  
					  
					  if (n==1) { 
						  s+=getMessageResourceString(LABELS, "docu.prilojTab")+": ";
					  }else {
						  s+=" ";
					  }
					  if (typePr==0) {
						  addNewColumn(strB, s, "9%", styleRowBoldBez, RIGHTALIG, "top", null, "2", null);
					  
					  }else{
		        		cN=0;
		        		createCell(rN, cN, 2, csArFb, 0);
		        		cell.setCellValue(s);
					  }
					  
					  
					  s="";
					  s+="№: ";
					  
					  if(null!=pril[2])
						  s+=asString(pril[2]);
					  
					  if (null!=pril[4])	
						  s+=" "+getMessageResourceString(LABELS, "docu.nameDelo")+": "+asString(pril[4]);
					  
					  if (null!=pril[7])	
						  s+=", "+getMessageResourceString(LABELS, "prilojenia.nositel")+": "+asString(pril[7]);
					  
					  if (null!=pril[6])	
						  s+=", "+getMessageResourceString(LABELS, "prilojenia.brLista")+": "+asInteger(pril[6]);
					  
					  if (null!=pril[5])	
						  s+=", "+getMessageResourceString(LABELS, "prRegDoc.dopInfo")+": "+asString(pril[5]);
					  
					  
					  if (s != null && !"".equals(s.trim())) {
						  if (typePr==0) {
							  addNewColumn(strB, s, "89%", styleRowBez, null, "top", null, "13", null);
						  
						  }else{
		   	        		cN=3;
		   	        		createCell(rN, cN, 13,csAlFr, 0);
		   	        		cell.setCellValue(s);
		   		    	  }  
						   
					  }
					  
					  
					  if (typePr==0) {
						  strB.append(TRE); 
						  osw.write(strB.toString());
					  }

					  n++;
						  
				}
			}

		   }
		   
		   
		  
		   if (null!=item[0]) {
			   // 13 ред вложени док-ти
			   getEkzVlojList(asInteger(item[0]), "A2");
			   
			   if (null!=this.ekzVlojeniList && !this.ekzVlojeniList.isEmpty()) {
				   n = 0;
				   
				   for (Object[]  vlItem: this.ekzVlojeniList) {
					   s = "";
					   if(null!=vlItem[2]) {
						   if (typePr==0) {
							  strB.delete(0,strB.length());
							  strB.append(TR);
						   }else{
							  rN++;
							  row=sheet.createRow((short)rN);

						   }
						  
						  if (n==0) {
							  s+=getMessageResourceString(beanMessages, "prRegDoc.incl")+":";
						  }else {
							  s+=" ";
						  }
						  
						  if (typePr==0) {
							  addNewColumn(strB, s, "9%", styleRowBoldBez + "font-weight: bold; ", RIGHTALIG, "top", null, "2", null);
						 
						  }else{
			        		cN=0;
			        		createCell(rN, cN, 2, csArFb, 0);
			        		cell.setCellValue(s);

						  }
						  
						  s=" - ";
						  if(null!=vlItem[10])
							  s+=getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_DELO_TYPE, asInteger(vlItem[10]), lang, new Date());

						  if(null!=vlItem[6])
							  s+=" "+asString(vlItem[6]);
						  
						  if(null!=vlItem[7])
							  s += "/" + sDfp.format(asDate(vlItem[7]));
					   
						  if(null!=vlItem[3]) 
							  s +=", "+getMessageResourceString(LABELS, "opis.tomNomer").toLowerCase()+" " +asString(vlItem[3]);
						   
						  if(null!=vlItem[1])
							  s +=" "+getMessageResourceString(LABELS, NADATE).toLowerCase()+" " +sDfp.format(asDate(vlItem[1]));
						  
						  if (null!=paramEkz && paramEkz.equals("1"))
							  s+=", "+getMessageResourceString(LABELS, EKZL)+" " +asString(vlItem[2]);
						   
						  if (typePr==0) {
							  addNewColumn(strB, s, "89%", styleRowBez, null, "top", null, "13", null);
						  
						  }else{
		   	        		cN=3;
		   	        		createCell(rN, cN, 13,csAlFr, 0);
		   	        		cell.setCellValue(s);
//		   	        		sheet.autoSizeColumn((short)cN, true);
		   		    	  }  
		  
						  
						  if (typePr==0) {
							  strB.append(TRE); 
							  osw.write(strB.toString());
						  }
					   	}
						 n++;  
					 }
				  
		   		}
		   	}
		   
 
		   
		// 14 ред Свързани документи
		   if (inclOpt.contains("6") && null!=item[0]) {
			   getAllDocLinks(asInteger(item[0]));
			   
			   if (null!=this.linkDocList && ! this.linkDocList.isEmpty()) {
				   n = 1;
				   for (Object[] linkD: this.linkDocList) {
					  s = "";
					  if (typePr==0) {
						  strB.delete(0,strB.length());
						  strB.append(TR);
					  }else{
		 				rN++;
		        		row=sheet.createRow((short)rN);
		        		row.setHeightInPoints(2 * sheet.getDefaultRowHeightInPoints());
					  }
					  
					  
					  if (n==1) { 
						  s+=getMessageResourceString(LABELS, "printReg.pril2DocLinkD")+": ";
					  }else {
						  s+=" ";
					  }
					  if (typePr==0) {
						  addNewColumn(strB, s, "9%", styleRowBoldBez, RIGHTALIG, "top", null, "2", null);
					  
					  }else{
		        		cN=0;
		        		createCell(rN, cN, 2, csArFb, 0);
		        		cell.setCellValue(s);
					  }
					  
					  
					  s="";
					  s+=n+".";
					  
					  if (null!=linkD[6])	// Vid link
						  s+=" "+getMessageResourceString(LABELS, "docDoc.relType")+": "+asString(linkD[6]);
					  
					  if(null!=linkD[3] || null!=linkD[4]) {// Reg #
						  s+=" "+getMessageResourceString(LABELS, "opis.regN")+": ";
						  
						  if(null!=linkD[3])
							  s+=" "+asString(linkD[3]);
									  
						  if(null!=linkD[4])
							  s += "/" + sDfp.format(asDate(linkD[4]));
					  }
  
					  if (null!=linkD[5])	// Otnosno
						  s+=", "+getMessageResourceString(LABELS, "docu.otnosno")+": "+asString(linkD[5]);

					  if (s != null && !"".equals(s.trim())) {
						  if (typePr==0) {
							  addNewColumn(strB, s, "89%", styleRowBez, null, "top", null, "13", null);
						  
						  }else{
		   	        		cN=3;
		   	        		createCell(rN, cN, 13,csAlFr, 0);
		   	        		cell.setCellValue(s);
		   		    	  }  
						   
					  }
					  
					  
					  if (typePr==0) {
						  strB.append(TRE); 
						  osw.write(strB.toString());
					  }
						  

					  n++;
						  
				}
			}

		   }
		   
		   
   
	   //15 red Движение документи.
		if (inclOpt.contains("3")) {
		   getDvigDocs(asInteger(item[0]));
		   if (null!=this.dvigDocsList && ! this.dvigDocsList.isEmpty()) {
			   n = 1;
			  
			   for (Object[]  dvItem: this.dvigDocsList) {
				  s = "";
				  if (typePr==0) {
					  strB.delete(0,strB.length());
					  strB.append(TR);
				  }else{
	 				rN++;
	        		row=sheet.createRow((short)rN);
	        		row.setHeightInPoints(2 * sheet.getDefaultRowHeightInPoints());
				  }
				  
				  
				  if (n==1) { 
					  s+=getMessageResourceString(beanMessages, "prRegDoc.movent")+":";
				  }else {
					  s+=" ";
				  }
				  if (typePr==0) {
				 	addNewColumn(strB, s, "9%", styleRowBoldBez, RIGHTALIG, "top", null, "2", null);
				  
				  }else{
	        		cN=0;
	        		createCell(rN, cN, 2, csArFb, 0);
	        		cell.setCellValue(s);
	//				        	sheet.autoSizeColumn((short)cN, true);
				  }
				  
				  
				  s="";
				
				  s+=n+".";
				  if (null!=dvItem[1] && (null!=paramEkz && paramEkz.equals("1")))	
					  s+=" "+getMessageResourceString(LABELS, EKZL)+asInteger(dvItem[1]);
				  if (null!=dvItem[2]) {// Дата предаване
					  s+=" "+getMessageResourceString(LABELS, "dvijenie.predadenNa")+": ";
					  if (null!=dvItem[4])	
						  s+=asString(dvItem[4]);
					  if (null!=dvItem[5])
						  s+=" email "+asString(dvItem[5]);
					  
					 
					  if(null!=dvItem[2]) {// data predavane
						  s +=" "+getMessageResourceString(LABELS, NADATE).toLowerCase()+" " +sDfp.format(asDate(dvItem[2]));
						  if(null!=dvItem[7]) // da se varne do
						  	 s +=", "+getMessageResourceString(beanMessages, "prRegDoc.returnDat")+": " +sDfp.format(asDate(dvItem[7]));
					  }
					  
					  if(null!=dvItem[6])// inf-ja predavane
						  s+=", "+getMessageResourceString(beanMessages, "prRegDoc.infPred")+": "+asString(dvItem[6]);
					  
					  /*if(null!=dvItem[15])// nachin predavane
						  s+=", начин предав. "+asString(dvItem[15]);*/
					  
					  if(null!=dvItem[3])// nachin predavane
						  s+=", "+getMessageResourceString(beanMessages, "prRegDoc.mothodPred")+": "+getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_DVIJ_METHOD, asInteger(dvItem[3]), Integer.valueOf(lang), new Date()); 
			
					  
					  if(null!=dvItem[15])// status predavane
						  s+=", "+getMessageResourceString(LABELS, "docu.taskH2")+": "+asString(dvItem[15]);
					  
					  if(null!=dvItem[9])// data status
					  		s +=" "+getMessageResourceString(LABELS, NADATE).toLowerCase()+" " +sDfp.format(asDate(dvItem[9]));
					  
					  if(null!=dvItem[10])// info status
						  s+=", "+getMessageResourceString(beanMessages, "prRegDoc.statusInf")+": "+asString(dvItem[10]);
				  
				  }
				  if (null!=dvItem[11]) {// Връщане дата
					  if (s != null && !"".equals(s.trim()))
						  s+=";";
					  s+=" "+getMessageResourceString(LABELS, "dvijenie.varnat")+": ";
					  /*if (null!=dvItem[4])	
						  s+=asString(dvItem[4]);*/
	
					  if(null!=dvItem[11]) // data връщате
						  	 s +=" "+getMessageResourceString(LABELS, NADATE).toLowerCase()+" " +sDfp.format(asDate(dvItem[11]));
					  
					  if(null!=dvItem[13])// inf-ja връщане
						  s+=", "+getMessageResourceString(beanMessages, "prRegDoc.infRet")+": "+asString(dvItem[13]);
					  
					  /*if(null!=dvItem[17])// nachin връщане
						  s+=", начин връщ. "+asString(dvItem[17]); */
					  
					  if(null!=dvItem[12])// nachin връщане
						  s+=", "+getMessageResourceString(beanMessages, "prRegDoc.retMethod")+": "+getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_DVIJ_METHOD, asInteger(dvItem[12]), Integer.valueOf(lang), new Date());
					  				  
				  }
	
				  if (s != null && !"".equals(s.trim())) {
					  if (typePr==0) {
						  addNewColumn(strB, s, "89%", styleRowBez, null, "top", null, "13", null);
					  
					  }else{
	   	        		cN=3;
	   	        		createCell(rN, cN, 13,csAlFr, 0);
	   	        		cell.setCellValue(s);
	   		    	  }  
					   
				  }
	
				   
				   
				  if (typePr==0) {
					  strB.append(TRE);
					  osw.write(strB.toString());
				  }
	
				   
				   n++;
				   
			   }
						
				   
			}
   
		  }
		   		
			
		}

		} catch (Exception e) {
			throw new DbErrorException(e.getMessage());
		} 
	
	}
	

	/**
	 * @return the osw
	 */
	public OutputStreamWriter getOsw() {
		return osw;
	}

	/**
	 * @param osw the osw to set
	 */
	public void setOsw(OutputStreamWriter osw) {
		this.osw = osw;
	}
	
	
	
	public void getAllDocPril(Integer idDoc)  throws DbErrorException{
		try {
			JPA.getUtil().runWithClose(() -> this.prilList = new RegisterDocsPrintDAO(getUserData()).selPrilDocs(idDoc));

		} catch (Exception e) {
			throw new DbErrorException(e.getMessage());
		}
		
		
	}
	
	
	
	public void getAllDocLinks(Integer idDoc)  throws DbErrorException{
		try {
			JPA.getUtil().runWithClose(() -> this.linkDocList = new RegisterDocsPrintDAO(getUserData()).selLinkDocs(idDoc));

		} catch (Exception e) {
			throw new DbErrorException(e.getMessage());
		}
		
		
	}
	
	public void getEkzVlojList (Integer idDoc, String sortCol) throws DbErrorException {
		
    	try {
			JPA.getUtil().runWithClose(() -> this.ekzVlojeniList = new RegisterDocsPrintDAO(getUserData()).selVlojEkz(idDoc, sortCol));
			
		} catch (Exception e) {
			throw new DbErrorException(e.getMessage());
		}
	
	}
	
	public void getDestrDocs (Integer idDoc)  throws DbErrorException {
		
    	try {
			JPA.getUtil().runWithClose(() -> this.destrDocsList = new RegisterDocsPrintDAO(getUserData()).selDestrDocs(idDoc));
			
		} catch (Exception e) {
			throw new DbErrorException(e.getMessage());
		}
	
	}
	
	public void getDvigDocs (Integer idDoc) throws DbErrorException {
		
    	try {
			JPA.getUtil().runWithClose(() -> this.dvigDocsList = new RegisterDocsPrintDAO(getUserData()).selDvigDocs(idDoc));
			
		} catch (Exception e) {
			throw new DbErrorException(e.getMessage());
		}
	
	}

	
	public void getArchDocs (Integer idDoc) throws DbErrorException{
		
    	try {
			JPA.getUtil().runWithClose(() -> this.archDocsList = new RegisterDocsPrintDAO(getUserData()).selArhivDocs(idDoc));
			
		} catch (Exception e) {
			throw new DbErrorException(e.getMessage());
		}
	
	}
	
	
	
	public void getStoreDocs (Integer idDoc)  throws DbErrorException{
		
    	try {
			JPA.getUtil().runWithClose(() -> this.storeDocsList = new RegisterDocsPrintDAO(getUserData()).selStoredDocs(idDoc));
			
		} catch (Exception e) {
			throw new DbErrorException(e.getMessage());
		}
	
	}
	
/*
	*//**
	 * Този метод създава cell Excel + styles
	 *
	 * @param rowN ред №
	 * @param colB кол.№
	 * @param colE крайна кол№ for marging cell и bording   
	 * @param halign HorizontalAlignment тип
	 * @param bord BorderStyle тип  
	 * @param boolean fo - bold font
	 * @param int bordPl - Бордер место(0-без 1-горе,2-долу,3-ляво,4-дясно)
	 * @return
	 *//*
	public void createCell(int rowN, int colB, int colE, HorizontalAlignment halign, int bordPl, BorderStyle bord, boolean fo) {
		
		cell=row.createCell((short)colB);
		
		if (colE>colB) {//Merging
			region.setFirstRow((short)rN);
			region.setLastRow((short)rN);
			region.setFirstColumn((short)colB);
			region.setLastColumn((short)colE);
			sheet.addMergedRegion(region);
		}
		
		
		cellStyle = wb.createCellStyle();
		
		
		// Стилове
		if (null!=halign) //Alignment
			cellStyle.setAlignment(halign);
		
//		font = wb.createFont();
		if (fo) {//Bold
			cellStyle.setFont(fontb);
			//font.setBold(true);
		}else {
			cellStyle.setFont(font);
			//font.setBold(false);
		}
		//cellStyle.setFont(font);
	
		if (bordPl>0 && null!=bord) {
			switch(bordPl) {
				case 1:
					if (colE>colB)
						RegionUtil.setBorderTop(bord, region, sheet);
					cellStyle.setBorderTop(bord);
				break;
				case 2:
					if (colE>colB)
						RegionUtil.setBorderBottom(bord, region, sheet);
					cellStyle.setBorderBottom(bord);
				break;
				case 3:
					if (colE>colB)
						RegionUtil.setBorderLeft(bord, region, sheet);
					cellStyle.setBorderLeft(bord);
				break;
				case 4:
					if (colE>colB) 
						RegionUtil.setBorderRight(bord, region, sheet);
					cellStyle.setBorderRight(bord);
				break;
				case 0: 
				break;
			}
		}
		
//		cell=row.createCell(colB);
		if (null!=cellStyle)
//		   	sheet.getRow(rowN).getCell(colB).setCellStyle(cellStyle); 
			cell.setCellStyle(cellStyle);
	
	}
	
	
*/	

	/**
	 * Този метод създава cell Excel + styles
	 *
	 * @param rowN ред №
	 * @param colB нач. кол.№ for marging cell и bording
	 * @param colE крайна кол.№ for marging cell и bording   
	 * @param cs HSSFCellStyle тип  
	 * @param int bordPl - Бордер место(0-без 1-горе,2-долу,3-ляво,4-дясно)
	 * @return
	 */
	public void createCell(int rowN, int colB, int colE, HSSFCellStyle cs, int bordPl) {
		
		cell=row.createCell((short)colB);
		
		
		if (colE>colB) {//Merging
			region.setFirstRow((short)rN);
			region.setLastRow((short)rN);
			region.setFirstColumn((short)colB);
			region.setLastColumn((short)colE);
			sheet.addMergedRegion(region);
		}
		
		
		
		if (bordPl>0 && colE>colB) {
			switch(bordPl) {
				case 1:
//					if (colE>colB)
						RegionUtil.setBorderTop(BorderStyle.THIN, region, sheet);
//					cellStyle.setBorderTop(bord);
				break;
				case 2:
//					if (colE>colB)
						RegionUtil.setBorderBottom(BorderStyle.THIN, region, sheet);
//					cellStyle.setBorderBottom(bord);
				break;
				case 3:
//					if (colE>colB)
						RegionUtil.setBorderLeft(BorderStyle.THIN, region, sheet);
//					cellStyle.setBorderLeft(bord);
				break;
				case 4:
//					if (colE>colB) 
						RegionUtil.setBorderRight(BorderStyle.THIN, region, sheet);
//					cellStyle.setBorderRight(bord);
				break;
				
				case 0: 
				break;
				
				default:
				break;
			}
		}
		

		if (null!=cs)
			cell.setCellStyle(cs);
		
//		sheet.autoSizeColumn((short)colB, true);
	
	}
	
}
