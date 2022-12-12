package com.ib.omb.ocr;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Properties;

import com.aspose.words.Document;
import com.github.jaiimageio.impl.plugins.tiff.TIFFImageReader;
import com.github.jaiimageio.impl.plugins.tiff.TIFFImageReaderSpi;
import com.ib.indexui.system.IndexUIbean;
import com.ib.omb.system.SystemData;
import com.ib.system.BaseSystemData;
import com.ib.system.db.dto.Files;
import com.ib.system.db.dto.SystemOption;
import com.ib.system.exceptions.DbErrorException;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.util.PdfGsUtilities;
import net.sourceforge.tess4j.util.PdfUtilities;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;

public class OCRUtils extends IndexUIbean{
	private static boolean isConfigured = false;
	private static boolean isTesseractEnabled = false;
	private static String tessdata="./";
	private static String lang="bul";
	static ThreadLocal<Tesseract> tesseractInstance= new ThreadLocal<>();

	public static void configure(BaseSystemData systemData){
		isConfigured = true;
		try {
			isTesseractEnabled = "true".equals(systemData.getSettingsValue("general.tesseract.enabled")) ? true : false;
			tessdata = systemData.getSettingsValue("general.tesseract.path");
			lang = systemData.getSettingsValue("general.tesseract.language");
		} catch (Throwable e) {
			isTesseractEnabled=false;
		}

	}
	private Tesseract getTesseract() throws IOException {
		if(tesseractInstance.get()!=null) return tesseractInstance.get();
		//тeзи редobe ca, за да накараме tess4j да зареди tiff библиотеките иначе номера с imagebuffer не сработва
		//-->
		tesseractInstance.set(new Tesseract()); ;
		TIFFImageReaderSpi spi=new TIFFImageReaderSpi();
		ImageReader imageReader1 = spi.createReaderInstance();
		ImageIO.scanForPlugins();
		Arrays.asList(ImageIO.getReaderFormatNames()).stream().forEach((n) -> System.out.println(n));
		tesseractInstance.get().setDatapath(tessdata);
		tesseractInstance.get().setLanguage(lang);

		//<--
		return tesseractInstance.get();
	}

	public String process(Files f) throws Exception {
		String extractedText = "";
		String filename = f.getFilename();
		String fileExtension = filename.substring(filename.lastIndexOf(".") + 1);

		if (("application/msword".equals(f.getContentType())
				|| "application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(f.getContentType())
				|| ("doc".equals(fileExtension) || "docx".equals(fileExtension)))
				|| (("text/rtf".equals(fileExtension)) || ("rtf".equals(fileExtension)))) {
			extractedText = processAsDoc(f);
		} else if (null != fileExtension && ("jpg".equalsIgnoreCase(fileExtension)
				|| "jpeg".equalsIgnoreCase(fileExtension) || "gif".equalsIgnoreCase(fileExtension)
				|| "tiff".equalsIgnoreCase(fileExtension) || "tif".equalsIgnoreCase(fileExtension)
				|| "bmp".equalsIgnoreCase(fileExtension) || "png".equalsIgnoreCase(fileExtension))) {

				extractedText = doRealOCR(f);

		} else if ("application/pdf".equals(f.getContentType()) || "pdf".equalsIgnoreCase(fileExtension)) {
			extractedText = processAsPdf(f);
		} else {
			throw new Exception("Форматът на файла " + f.getFilename() + " не се поддържа за OCR!");
		}
		return extractedText;
	}

	private String processAsPdf(Files f) throws Exception {

		InputStream input = objectToInputStream(f);
		String text = null;
		try {
			text = getPdfTxt(input);
		} catch (Exception e) {

		}

		if (null == text || text.length()<2) {
			text = doRealOCR(f);
		}
		return text;
	}

	private String getPdfTxt(InputStream content) throws Exception {
		PdfReader pr = null;
		try {
			pr = new PdfReader(content);
			PdfDocument document = new PdfDocument(pr);
			String text = "";

			for (int i = 0; i < document.getNumberOfPages(); i++) {
				text += PdfTextExtractor.getTextFromPage(document.getPage(i + 1)) + "\n";
			}
			return text;
		} catch (Exception e) {
			throw new Exception("Възникна грешка в метода 'getPdfTxt'", e);
		}
	}

	public String doRealOCR(Files f) throws Exception {
		if(!isTesseractEnabled) return null;

		String text = null;
		Tesseract instance = getTesseract();

		String filename = f.getFilename();

		Properties configs = new Properties();
		configs.put("dpi", 300);
		try {
			// изпълняване на самото ocr-ване
			ByteArrayInputStream bais = new ByteArrayInputStream(f.getContent());
			text = instance.doOCR(ImageIO.read(bais));
		} catch (Throwable e) {
			//може да е PDF, а те не могат да се OCR-ват без да са записани във файл
			String fileExtension = filename.substring(filename.lastIndexOf(".") + 1);
			File file = null;
			try {
				if ("pdf".equalsIgnoreCase(fileExtension)) {
					file = File.createTempFile("temp-ocr", "." + fileExtension);
					writeBytesToFile(file.getAbsolutePath(), f.getContent()); // записване на съдържанието във временния файл
					text = instance.doOCR(file);
				}
			} catch (Exception ioException) {
				throw e;
			} finally {
				try {
					if(file!=null) {
						file.delete();
					}
				} catch (Exception exception) {
					//нищо
				}
			}
		}

		return text;
	}


	public static void writeBytesToFile(String fileName, byte[] fileBArray) {

		FileOutputStream fileOuputStream = null;

		try {
			fileOuputStream = new FileOutputStream(fileName);
			fileOuputStream.write(fileBArray);

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (fileOuputStream != null) {
				try {
					fileOuputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static InputStream objectToInputStream(Files object) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(object);
		oos.flush();
		oos.close();
		InputStream is = new ByteArrayInputStream(baos.toByteArray());
		return is;
	}

	private String processAsDoc(Files f) throws Exception {
		String text = null;
		try {
			Document doc = new Document(new ByteArrayInputStream(f.getContent()));
			text = doc.getText();
			return text;
		} catch (Exception e) {
			throw new Exception("Възникна грешка в метода 'processAsDoc'", e);
		}
	}
}
