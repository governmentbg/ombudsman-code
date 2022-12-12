package com.ib.omb.export;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import com.ib.indexui.system.IndexUIbean;
import com.ib.system.exceptions.DbErrorException;
import com.itextpdf.io.source.ByteArrayOutputStream;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;

public class BaseExport extends  IndexUIbean  implements Serializable {
    
    /**
	 * 
	 */
	private static final long serialVersionUID = 3963401137061130953L;

	/** Стил 1 за таблицата като цяло */
    public static final  String styleTable11pt = "font-family: Arial; font-size: 11pt; PADDING-RIGHT: 2px;PADDING-LEFT: 2px; PADDING-TOP: 0px;PADDING-BOTTOM:2px;";
    
    /** Стил 2 за таблицата като цяло */
    public static final  String styleTable12pt  = "font-family: Arial; font-size: 12pt; text-align:justify; PADDING-RIGHT: 2px;PADDING-LEFT: 2px; PADDING-TOP: 0px;PADDING-BOTTOM:2px;";
    
    /** Стил за таблицата като цяло */
    public static final  String styleTable14pt  = "font-family: Arial; font-size: 14pt; text-align:justify; PADDING-RIGHT: 2px;PADDING-LEFT: 2px; PADDING-TOP: 0px;PADDING-BOTTOM:2px;";
    
    /** Стил 3 за таблицата като цяло */
    public static final  String styleTable16pt  = "font-family: Arial; font-size: 16pt; text-align:justify; PADDING-RIGHT: 2px;PADDING-LEFT: 2px; PADDING-TOP: 0px;PADDING-BOTTOM:2px;";
    
    /** Стил 4 за таблицата като цяло */
    public static final  String styleTable9pt = "font-family: Arial; font-size: 9pt; PADDING-RIGHT: 2px;PADDING-LEFT: 2px; PADDING-TOP: 0px;PADDING-BOTTOM:2px;";
    
    /** Стил за заглавието 9*/
    public static final  String styleHeader9 = "font-family: Arial; font-size: 9pt; font-weight: bold; ";
    
    /** Стил за заглавието 12*/
    public static final  String styleHeader12 = "font-family: Arial; font-size: 12pt; font-weight: bold; ";
    
    
    /** Стил за заглавието 18 */
    public static final  String styleHeader18 = "font-family: Arial; font-size:18pt; font-weight: bold; ";
    
   
    /** Стил за заглавието 20 */
    public static final  String styleHeader20 = "font-family: Arial; font-size:20pt; font-weight: bold; ";
    
    /** Стил за заглавието */
    public static final  String styleHeader12Under = "font-family: Arial; font-size: 12pt; font-weight: bold;  text-decoration:underline";
   
    /** Стил за антетката */
    public static final  String styleAnt =    "border-top: #808080 1px solid; border-left: #808080 1px solid; border-bottom: #808080 1px solid; font-weight: bold; background-color: #dcdcdc;  height:22px; "; 
    
    /** Стил за антетката без border top */
    public static final  String styleAntBezTop =    "border-left: #808080 1px solid; border-bottom: #808080 1px solid; font-weight: bold; background-color: #dcdcdc;  height:22px; "; 
 
    /** Стил за антетката  без border bottom */
    public static final  String styleAntBezBot =    "border-top: #808080 1px solid; border-left: #808080 1px solid; font-weight: bold; background-color: #dcdcdc;  height:22px; "; 
    
    /** Стил за антетката  - 7.5pt (10px)*/
    public static final  String styleAnt10B = "border-top: #808080 1px solid; border-left: #808080 1px solid; border-bottom: #808080 1px solid; font-weight: bold; background-color: #dcdcdc;  font-family: Arial; font-size: 7.5pt;";  
   
    /** Стил за антетката  - 10pt*/
    public static final  String styleAnt10ptB = "border-top: #808080 1px solid; border-left: #808080 1px solid; border-bottom: #808080 1px solid; font-weight: bold; background-color: #dcdcdc;  font-family: Arial; font-size: 10pt;";  
    
    /** Стил за заглавието вътре в редовете */
    public static final  String styleHeaderGrupa = "border-left: #808080 1px solid; font-family: Arial; font-size: 14px; font-weight: bold; border-right: #808080 1px solid; ";
    
    /** Стил за заглавието вътре в редовете с бордер отдолу */
    public static final  String styleHeaderGrupaB = "border-left: #808080 1px solid; border-bottom: #808080 1px solid; font-family: Arial; font-size: 14px; font-weight: bold; border-right: #808080 1px solid; ";
    
    /** Стил за заглавието вътре в редовете без бордер отдолу */
    public static final  String styleHeaderGrupaC = "border-left: #808080 1px solid; font-family: Arial; font-size: 14px; font-weight: bold; border-right: #808080 1px solid; ";
   
    /** Стил за редовете от справката - реда има бордер от всички страни (left, bottom)*/
    public static final  String styleRow = "border-left: #808080 1px solid; border-bottom: #808080 1px solid;  height:22px; ";
    
    /** Стил за редовете от справката - реда има бордер от всички страни (left, bottom)*/
    public static final  String styleRowBold = "border-left: #808080 1px solid; border-bottom: #808080 1px solid;  height:22px; font-weight: bold; ";
    
    /** Стил за редовете от справката - реда има бордер само отдолу */
    public static final  String styleRowBottom = "border-bottom: #808080 1px solid;  height:22px;  ";
    
    /** Стил за редовете от справката - реда има бордер в дясно */
    public static final String styleRightBorder =  " border-right: #808080 1px solid;  height:22px; ";
   
    /** Стил за редовете от справката - без бордер  */
    public static final String styleRowBez =  "height:22px; ";
    
    /** Стил за редовете от справката - без бордер bold */
    public static final String styleRowBoldBez =  "height:22px;font-weight: bold; ";
    
    
    
       
    /** Стил за ред с размер 10px*/
    public static final  String styleRow10  = "font-family: Arial; font-size: 7.5pt; "; //7.5pt
    
     /** Стил за ред с размер 10pt*/
    public static final  String styleRow10pt  = "font-family: Arial; font-size: 10pt; "; //10pt
    
     /** Стил за ред с размер 10pxbold  - без border */
    public static final  String styleRow10Bold  = "font-family: Arial; font-size: 7.5pt; font-weight: bold; "; //7.5pt
    
      /** Стил за ред с размер 9pxbold  - без border */
    public static final  String styleRow9Bold  = "font-family: Arial; font-size: 9pt; font-weight: bold; "; //9pt
    
       /** Стил за ред с размер 9pxbold  - без border */
    public static final  String styleRow9  = "font-family: Arial; font-size: 9pt;"; //9pt

    
      /** Стил за ред с размер 10ptbold  - без border */
    public static final  String styleRow10ptBold  = "font-family: Arial; font-size: 10pt; font-weight: bold; "; //10pt
    
    /** Стил за редовете от справката - реда има border-left border-bottom */
    public static final  String styleRow10B = "border-left: #808080 1px solid; border-bottom: #808080 1px solid;  font-family: Arial; font-size: 7.5pt; ";
    
    /** Стил за редовете от справката - реда има border-left border-bottom */
    public static final  String styleRow10ptB = "border-left: #808080 1px solid; border-bottom: #808080 1px solid;  font-family: Arial; font-size: 10pt; ";
    
    /** Стил 1 за редовете - bold*/
    public static final  String styleRow11Bold = "font-family: Arial; font-size: 11pt; font-weight: bold;";
    
    /** Стил 1 за редове - bold - underline*/ 
     public static final  String styleRow9UBold = "border-bottom: gray 1px solid;font-family: Arial; font-size: 9pt; font-weight: bold;";
     
    /** Стил 1 за редове  - underline*/ 
     public static final  String styleRow9U = "border-bottom: gray 1px solid;font-family: Arial; font-size: 9pt; ";
    
    /** Стил 1 за редовете */
    public static final  String styleRow11 = "font-family: Arial; font-size: 11pt; ";
    
    /** Стил за антетката  - 8pt*/
    public static final  String styleAnt8ptB = "border-top: #808080 1px solid; border-left: #808080 1px solid; border-bottom: #808080 1px solid; font-weight: bold; background-color: #dcdcdc;  font-family: Arial; font-size: 8pt;";  
   
    /** Стил за ред  - 8pt*/
    public static final  String styleRow8ptB =  "border-left: #808080 1px solid; border-bottom: #808080 1px solid;  font-family: Arial; font-size: 8pt; ";
    public static final  String styleRow8ptBez = " font-size: 8pt; ";
    
    
    /** Базов клас за експортите */
    public BaseExport() {
    }
    

    /**добавя колона в таблицата, със зададена ширина, стил , align, title......
     * @param rowData - буфера, където се натрупват данните 
     * @param dataField - данни за колоната
     * @param width_ - широчина на колоната - 
     * @param style_
     * @param align_ - left, right,center,justify 
     * @param valign_ - top, bottom, middle, baseline
     * @param rowspan_
     * @param colspan_
     * @param title_
     */
    protected void addNewColumn( StringBuffer rowData,
                                 Object dataField, 
                                 String width_, 
                                 String style_, 
                                 String align_, 
                                 String valign_, 
                                 String rowspan_, 
                                 String colspan_, 
                                 String title_){
                   
        if (valign_ == null){
            valign_ = "";
        }else{
            valign_ = "valign=\""+valign_+"\"";
        }
        
        if (title_==null){
            title_ = "";
        }else{
            title_ = "title=\""+title_+"\"";
        }
        
        if (rowspan_==null){
            rowspan_ = "";
        }else{
            rowspan_ = "rowspan=\""+rowspan_+"\";";
        }
        
        if (colspan_==null){
           colspan_ = "";
        }else{
           colspan_ = "colspan=\""+colspan_+"\";";
        }
        
        if (align_ == null){
           align_ = "left";
        }
        String style__ = "style=\"text-align:"+align_+"; ";
        if (style_== null){ 
            style_ = "";   
        }
        
        style__ +=  style_+ "\"";     
            
        if (width_== null){
          width_ = "";   
        }else{
           width_="width=\"" + width_+ "\"";   
        }  
        if (dataField == null || dataField.equals("")){
            rowData.append("<td  "+width_+ style__ + valign_ + title_ + rowspan_ + colspan_ + ">"+"&nbsp;"+"</td>");       
        }else {   
            rowData.append("<td "+width_+ style__ + valign_ + title_+ rowspan_ + colspan_ + ">"+dataField.toString().trim()+"</td>");  
        }
   }

 
    /**добавя нов ред, със зададен брой  колони и го праща за печат
     * @param osw
     * @param dataR  - данни за печат за всяка кол.
     * @param nCol - брoй колонки за печат
     * @param colspan_ - за всяка колона - colspan
     * @param style_ - стил за всички колони
     * @throws IOException
     */
    protected void addNewRow(OutputStreamWriter osw, String[] dataR, int nCol, String[] colspan_, String style_) throws IOException {
      if (style_== null){
         style_ = "";   
      }else{
          style_="style=\"" + style_+ "\"";  
      }
      
      String tmpColSpan="";
      StringBuffer rowData = null;             
      rowData = new StringBuffer("<tr>");
      
      for(int i=0; i<nCol; i++){
          if(colspan_ == null){
              tmpColSpan = "";
          }else{
              if (colspan_[i]==null){
                 tmpColSpan = "";
              }else{
            	  tmpColSpan= "colspan=\""+ colspan_[i] +"\";";
          	  }
          }
          if(dataR == null){
              rowData.append("<td " + tmpColSpan + ">" +  "&nbsp;</td>");
          }else{ 
             if(dataR[i]==null){
                  rowData.append("<td  " + tmpColSpan + ">" + "&nbsp;</td>");
             }else{
                  rowData.append("<td  valign='top' "+ style_ + tmpColSpan + ">" + dataR[i] +"</td>");
             }
          }
      }
      rowData.append("</tr>");        
      osw.write(rowData.toString());    
    } 


    /**добавя нов ред, със зададен брой  колони и връща получения стринг
     * @param dataR - данни за печат за всяка кол.
     * @param nCol - брoй колонки за печат
     * @param colspan_ - за всяка колона - colspan
     * @param style_ - стил за всички колони
     * @return получения стринг
     * @throws IOException
     */
    protected StringBuffer addNewRowToTbl(String[] dataR, int nCol, String[] colspan_, String style_) throws IOException {
          if (style_== null){
             style_ = "";   
          }else{
              style_="style=\"" + style_+ "\"";  
          } 
          String tmpColSpan="";
          StringBuffer rowData = null;             
          rowData = new StringBuffer("<tr>");
          
          for(int i=0; i<nCol; i++){
              if(colspan_ == null){
                  tmpColSpan = "";
              }else{
                  if (colspan_[i]==null){
                     tmpColSpan = "";
                  }else{
                     tmpColSpan= "colspan=\""+ colspan_[i] +"\";";
                  }
              }
              if(dataR == null){
                  rowData.append("<td " + tmpColSpan + ">" +  "</td>");
              }else{ 
                 if(dataR[i]==null){
                      rowData.append("<td  " + tmpColSpan + ">" + "</td>");
                 }else{
                      rowData.append("<td  valign='top' "+ style_ + tmpColSpan + ">" + dataR[i] +"</td>");
                 }
              }
          }
          rowData.append("</tr>");        
          return rowData;
    } 

    /**Връща датата като стринг на български (пример: "1 февруари 2008 г.")
     * @param date
     * @return
     */
    protected String bgDateString(Date date){
        String bgDate="";
        GregorianCalendar gc=new GregorianCalendar(); 
        gc.setTime(date);
        String  monthS = bgMonthString(gc.get(GregorianCalendar.MONTH));
        bgDate =  gc.get(GregorianCalendar.DAY_OF_MONTH)+" "+ monthS+" "+gc.get(GregorianCalendar.YEAR)+" г.";
        return bgDate;
    }
    
    /**Връща датата като стринг на български (пример: "петък, 1 февруари 2008 г.")
     * @param date
     * @return
     */
    protected String bgDateString_Day(Date date){
        String bgDate="";
        GregorianCalendar gc=new GregorianCalendar(); 
        gc.setTime(date);
        String  monthS = bgMonthString(gc.get(GregorianCalendar.MONTH));
        String dayOfWeekS =  bgDayOfWeekString( gc.get(GregorianCalendar.DAY_OF_WEEK));
        bgDate = dayOfWeekS+", "+ gc.get(GregorianCalendar.DAY_OF_MONTH)+" "+ monthS+" "+gc.get(GregorianCalendar.YEAR)+" г.";
        return bgDate;
    }


    /** Връща името на месеца
     * @param month
     * @return
     */
    protected String bgMonthString(int month){
        switch (month) {
            case GregorianCalendar.JANUARY:
               return getMessageResourceString("beanMessages", "baseExport.JANUARY");
            case GregorianCalendar.FEBRUARY:
               return getMessageResourceString("beanMessages", "baseExport.FEBRUARY");
            case GregorianCalendar.MARCH:
               return getMessageResourceString("beanMessages", "baseExport.MARCH");
            case GregorianCalendar.APRIL:
               return getMessageResourceString("beanMessages", "baseExport.APRIL"); 
            case GregorianCalendar.MAY:         
               return getMessageResourceString("beanMessages", "baseExport.MAY"); 
            case GregorianCalendar.JUNE:
               return getMessageResourceString("beanMessages", "baseExport.JUNE"); 
            case GregorianCalendar.JULY:
               return getMessageResourceString("beanMessages", "baseExport.JULY"); 
            case GregorianCalendar.AUGUST:
               return getMessageResourceString("beanMessages", "baseExport.AUGUST"); 
            case GregorianCalendar.SEPTEMBER:
               return getMessageResourceString("beanMessages", "baseExport.SEPTEMBER"); 
            case GregorianCalendar.OCTOBER:
               return getMessageResourceString("beanMessages", "baseExport.OCTOBER"); 
            case GregorianCalendar.NOVEMBER:
               return getMessageResourceString("beanMessages", "baseExport.NOVEMBER"); 
            case GregorianCalendar.DECEMBER:
               return getMessageResourceString("beanMessages", "baseExport.DECEMBER");                
            default:
              return "";
               
        }        
    }


    /** Връща деня от седмицата като текст 
     * @param day
     * @return
     */
    protected String bgDayOfWeekString(int day){
       
        switch (day) {
            case GregorianCalendar.SUNDAY:
               return getMessageResourceString("beanMessages", "baseExport.SUNDAY");
            case GregorianCalendar.MONDAY:
               return getMessageResourceString("beanMessages", "baseExport.MONDAY");
            case GregorianCalendar.TUESDAY:
               return getMessageResourceString("beanMessages", "baseExport.TUESDAY");
            case GregorianCalendar.WEDNESDAY:
               return getMessageResourceString("beanMessages", "baseExport.WEDNESDAY"); 
            case GregorianCalendar.THURSDAY:         
               return getMessageResourceString("beanMessages", "baseExport.THURSDAY"); 
            case GregorianCalendar.FRIDAY:
               return getMessageResourceString("beanMessages", "baseExport.FRIDAY"); 
            case GregorianCalendar.SATURDAY:
               return getMessageResourceString("beanMessages", "baseExport.SATURDAY");                
            default:
              return "";
        }        
    }
        

    /** Връща римските цифри до 30
     * Roman   I II V X XX L  C   CC  D   M 
     * Western 1 2 5 10 20 50 100 200 500 1000 
     * @param number
     * @return
     */
    protected String RomanNumberStyle(int number){
        switch (number) {
            case 1:
               return "I. ";
            case 2:
               return "II. ";
            case 3:
               return "III. ";
            case 4:
               return "IV. ";
            case 5:
               return "V. ";
            case 6:
               return "VI. ";
            case 7:
               return "VII. ";
            case 8:
               return "VIII. ";
            case 9:
               return "IX. ";
            case 10:
               return "X. ";
            case 11:
               return "XI. ";
            case 12:
               return "XII. ";
            case 13:
               return "XIII. ";
            case 14:
               return "XIV. ";
            case 15:
               return "XV. ";
            case 16:
               return "XVI. ";
            case 17:
               return "XVII. ";
            case 18:
               return "XVIII. ";
            case 19:
               return "XIX. ";
            case 20:
               return "XX. ";
            case 21:
               return "XXI. ";
            case 22:
               return "XXII. ";
            case 23:
               return "XXIII. ";
            case 24:
               return "XXIV. ";
            case 25:
               return "XXV. ";
            case 26:
               return "XXVI. ";
            case 27:
               return "XXVII. ";
            case 28:
               return "XXVIII. ";
            case 29:
               return "XXIX. ";
            case 30:
               return "XXX. ";   
            default:
              return "";
               
        }        
    }    
        
    
    /*
     * Връща датата като стринг - dd.MM.yyyy г. HH:mm:ss
     */
    public String changeDateChasLongtoString(Date Date_in){
        String StringDate ="";
        if(Date_in!=null){
            StringDate = new SimpleDateFormat("dd.MM.yyyy г. HH:mm:ss").format(Date_in);
        }
        return StringDate;
    }
    
    /*
     * Проверява за null и преобразува датата в стринг от вид: dd.MM.yyyy
     */
    public String changeDateLongtoString(Date Date_in){
       String StringDate ="";
       if(Date_in!=null){
           StringDate = new SimpleDateFormat("dd.MM.yyyy").format(Date_in);
       }
       return StringDate;
    }
    
    /*
     * Проверява за null и връща само годината
     */
    public String changeDateLongtoStringYear(Date Date_in){
       String StringDate ="";
       if(Date_in!=null){
           StringDate = new SimpleDateFormat("yyyy").format(Date_in);//+"г. ";
       }
       return StringDate;
    }
    
    
    /*
     * Връща само часът и минутите
     */
    public String changeChasLongtoString(Date Date_in){
       String StringDate ="";
       if(Date_in!=null){
           StringDate = new SimpleDateFormat("HH:mm").format(Date_in);
       }   
       return StringDate;
    }
    
  

    /*
     * проверява дали подадения стринг е null или ""
     * и ако да - връща " "
     * ако не е празен - замества  "\\n" с "<br>" 
     * Това ни трябваше някъде при печат на текстове като относно и доп. информ.
     */
    public String checkString(String checkEl){
    
        if ( checkEl==null || checkEl.trim().equals("")){
            checkEl=" ";
        }else{
            checkEl=checkEl.replace("\\n","<br>");
        }
        return  checkEl;
    }
    
//    public String checkStringP(String pref, String checkEl){
//    
//        if ( checkEl==null || checkEl.trim().equals(""))
//            checkEl=" ";
//        else
//            checkEl=pref+" "+ checkEl.replace("\\n","<br>");
//        return  checkEl;
//    }
    
    /*
     * Ако е е подаден null - връща " ";
     */
    public String checkLong(Long checkEl){
        String checkElTemp;
        if ( checkEl==null ){
            checkElTemp=" ";
        }else{
            checkElTemp= ""+checkEl;
        }
        return  checkElTemp;
    }
    
    
    

	/*
	 * Отпечатва плик с адреса на кореспондент
	 * Извикава се от разглеждане на кореспонденти и от екземпляри 
	 */
    public String printPlikCorespondent(String rPlik, String corespName, String  corespData, String senderName, String senderData, String  rnDoc, boolean isRegistered) throws DbErrorException{
        try {
		    
        	FacesContext ctx = FacesContext.getCurrentInstance();
		    HttpServletResponse response = (HttpServletResponse) ctx.getExternalContext().getResponse();
	        ServletContext context_ = (ServletContext)FacesContext.getCurrentInstance().getExternalContext().getContext();
	        
//	        byte[] bytes = FileUtils.InputStreamToByteArray( Thread.currentThread().getContextClassLoader().getResourceAsStream("/META-INF/resources/fonts/arialuni.ttf")); 
// 			BaseFont myFont = BaseFont.createFont("arial.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, bytes, null);
             
	        Font myFont = FontFactory.getFont("Arial Unicode MS", "windows-1251");
	        
	        FileInputStream  fis = new FileInputStream(context_.getRealPath("")+"/resources/images/"+"R4.jpg");
	        int size = fis.available();
	        byte[] baR = new byte[size];
	        fis.read(baR);
	        fis.close();
	        
	        String fileName = "Plik.pdf";
	        response.setContentType("application/x-download");
	        response.setHeader("Content-Disposition",
	                           "attachment;filename=" + fileName);
	        ServletOutputStream os = null;
	       
	            os = response.getOutputStream();
	        
	        
	        String[] sa = null;
	        FramePlik frPlik = null;
	        if(rPlik != null){
	        	sa =  rPlik.split(",");  //hmPlikRazmer.get(lFrPlik); //param
	        	frPlik = new FramePlik(Integer.valueOf(sa[0]),Integer.valueOf(sa[1]));
	        } else {
	        	frPlik = new FramePlik();
	        }
	        
	        ByteArrayOutputStream bos = new ByteArrayOutputStream();
	        Plik plik = null;
	        
	 //       SystemData sd = (SystemData) getSystemData();
	        
	       
            plik = new Plik(myFont, 
                            bos, 
                            frPlik,
                            getUserData().getUserId(),
                            baR);
	        
	        plik.setSenderName(senderName); //param
	        plik.setSenderData(senderData); //param
	        
	        plik.setCorespName(corespName);
	        plik.setCorespData(corespData);
	        
	        plik.setIsRegistered(isRegistered); // param
	        plik.setDocNumb(rnDoc); //param
	
	        plik.makePlik();
	        byte[] ba = bos.toByteArray();
	        os.write(ba);
	        os.flush();
	        os.close();
	        FacesContext.getCurrentInstance().responseComplete(); 
	        
        } catch (DocumentException e) {
        	throw new DbErrorException(e.getMessage());
        
        }catch (IOException e) {
        	throw new DbErrorException(e.getMessage());
        }
        
        
                
	    return null;
	}
    
    
    
   
   
}
