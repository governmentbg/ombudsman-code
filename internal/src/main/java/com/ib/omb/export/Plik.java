package com.ib.omb.export;

//import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

//import org.apache.log4j.Logger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.system.exceptions.DbErrorException;
import com.lowagie.text.BadElementException;

//import com.ib.omb.system.SystemData;
//import com.indexbg.delo.db.DeloRegistraturi;
//import com.indexbg.delo.db.dao.DeloRegistraturiDAO;


/*import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;*/

/*import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;*/


import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
//import com.lowagie.text.Element;
import com.lowagie.text.Font;
//import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
//import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
//import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;


public class Plik {

    private static final Logger LOGGER = LoggerFactory.getLogger(Plik.class);
	private String docNumb = "";
	private Document document;
	//private Font f;
	/*private Font f2;
	private Font f2b;
	private Font f3b;
	private Font f4b;
	private Font fb;*/
	private Font myFont;

	private FramePlik frPlik;
	private int iLines = 0;
//	private Image imgR;
	private boolean isRegistered = false;

	private OutputStream os;
	private PdfPTable ptPlik;
	private String corespName = "";
	private String corespData = "";
	private String senderName = "";
	private String senderData = "";
		
	private final int colNumb       = 3;
    private final int pictureRows   = 3;
    private final int regRows       = 3;
    private final int rowRecip      = 4;
    
    private byte[] baR;


	private Plik(Font myFont, 
	            OutputStream os, 
	            FramePlik frPlik,
	            Long idUser
	            ) {
		this.frPlik = frPlik;
		this.os = os;
		this.myFont = myFont;
		float heightDt = frPlik.getHeight() - frPlik.getTm() - frPlik.getBm();
		float heightMm = pt2mm(heightDt);
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("heightMm/8 = " + heightMm / 8);
		}

		iLines = (int) Math.floor(heightMm / 9);
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("iLines = " + iLines);
		}
	}

	public Plik(Font myFont,
	            OutputStream fos,
                FramePlik frPlik2,
                long l,
                byte[] baR) {
	    this(myFont,
             fos,
             frPlik2,
             l);
	    this.baR = baR;
    }

	public String getDocNumb() {
		return docNumb;
	}

	public Font getMyFont() {
		return myFont;
	}

	public boolean getIsRegistered() {
		return isRegistered;
	}

	public OutputStream getOs() {
		return os;
	}

	public String getCorespData() {
		return corespData;
	}

	public String getCorespName() {
		return corespName;
	}

	public String getSenderData() {
		return senderData;
	}

	public String getSenderName() {
		return senderName;
	}

	private void init() throws DbErrorException{


		//f = new Font(myFont, 10);
			
		/*fb = new Font(myFont, 10, Font.BOLD);
		f2 = new Font(myFont, 12);
		f2b = new Font(myFont, 12, Font.BOLD);
		f3b = new Font(myFont, 14, Font.BOLD);
		f4b = new Font(myFont, 18, Font.BOLD);*/

		document = new Document(new Rectangle(frPlik.getWidth(),
				frPlik.getHeight()), frPlik.getLm(), frPlik.getRm(),
				frPlik.getTm(), frPlik.getBm());

		try {
			PdfWriter.getInstance(document, os);
		} catch (DocumentException e) {
			throw new DbErrorException(e.getMessage());
		}
		float[] widths = { 2f, 1f, 2f };
		ptPlik = new PdfPTable(widths);
		ptPlik.setWidthPercentage(100);
		
		document.open();
		
		

	}

	public void makePlik() throws DbErrorException {
		
		init();
		
		PdfPCell c = null;
		c = new PdfPCell();
		c.setRowspan(3);
		c.setBorder(0);
		Paragraph p = new Paragraph(senderName, myFont);
		c.addElement(p);
		p = new Paragraph(senderData, myFont);
		c.addElement(p);
		
		ptPlik.addCell(c);

		for (int i = 1; i <= pictureRows; i++) {
			for (int j = 2; j <= colNumb; j++) {
				c = new PdfPCell();
				c.setBorder(0);
				p = new Paragraph(i+","+j, myFont);
				p = new Paragraph(" ", myFont);
				c.addElement(p);
				ptPlik.addCell(c);
			}
		}

		for (int i = pictureRows+1; i <= iLines; i++) {
		    for (int j = 1; j <= colNumb; j++) {
		        if(i==pictureRows+1 && j==1) {
		            c = new PdfPCell();
		            c.setBorder(0);
		            c.setRowspan(regRows);
		            
		            if(isRegistered) {
		            	Image imgR;
		            	try {
							imgR = Image.getInstance(baR);
						} catch (BadElementException e) {
							throw new DbErrorException(e.getMessage());
						/*} catch (MalformedURLException e) {
							throw new DbErrorException(e.getMessage());*/
						} catch (IOException e) {
							throw new DbErrorException(e.getMessage());
						}
	                    
		                c = new PdfPCell(imgR,false);
		            }
		            else {
		                c = new PdfPCell();
		            }
		            
		            c.setBorder(0);
                    c.setRowspan(regRows);
		            ptPlik.addCell(c);
		            continue;
		        }
		        else
	            if(i < pictureRows+regRows+1 && j==1) {
	                continue;
	            }
	            else
                if(i == iLines-rowRecip+1 && j==colNumb) {
                    c = new PdfPCell();
                    c.setRowspan(rowRecip);
                    c.setBorder(0);
                    p = new Paragraph(corespName, myFont);
                    c.addElement(p);
                    p = new Paragraph(corespData, myFont);
                    c.addElement(p);
                    ptPlik.addCell(c);
                    continue;
                }
                else
                if(i > iLines-rowRecip+1 && j==colNumb) {
                    continue;
                }
		        if ((i == (iLines)) && (j == 1) && !isRegistered) {
		            c = new PdfPCell();
                    c.setBorder(0);
                    p = new Paragraph("Изх. No " + docNumb, myFont);
                    c.addElement(p);
                }
	            else {
	                c = new PdfPCell();
	                c.setBorder(0);
//	                p = new Paragraph(i+","+j, f);
	                p = new Paragraph(" ", myFont);
	                c.addElement(p);
	            }
		        ptPlik.addCell(c);    
		    }
		    
		}

		try {
			document.add(ptPlik);
		} catch (DocumentException e) {
			throw new DbErrorException(e.getMessage());
		}
		
		document.close();
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("plik.pdf ready");
		}
	}

	public float mm2pt(int mm) {
		//float r = 1 / 25.4f * 72 * mm;
		return  (float) 1 / 25.4f * 72 * mm;
	}

	public float pt2mm(float pt) {
//		float r = 25.4f / 72 * pt;
		return (float) 25.4f / 72 * pt;
	}

	public void setDocNumb(String docNumb) {
		this.docNumb = docNumb;
	}

	public void setMyFont(Font myFont) {
		this.myFont = myFont;
	}

	public void setIsRegistered(boolean isRegistered) {
		this.isRegistered = isRegistered;
	}

	public void setOs(OutputStream os) {
		this.os = os;
	}

	public void setCorespData(String corespData) {
		this.corespData = corespData;
	}

	public void setCorespName(String corespName) {
		this.corespName = corespName;
	}

	public void setSenderData(String senderData) {
		this.senderData = senderData;
	}
	
	public void setSenderName(String senderName) {
		this.senderName = senderName;
	}



}
