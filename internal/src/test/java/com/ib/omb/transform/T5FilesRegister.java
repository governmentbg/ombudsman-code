package com.ib.omb.transform;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.persistence.Query;

import org.hibernate.jpa.QueryHints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.omb.system.OmbConstants;
import com.ib.system.db.JPA;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.DbErrorException;

/**
 * Прехвърля файловото съдържание на документите от регистъра
 *
 * @author belev
 */
public class T5FilesRegister {

	/**
	 * @author belev
	 */
	private class Dest2 extends Thread {
		Stream<Object[]>	stream;
		Exception			ex;

		@SuppressWarnings("unchecked")
		@Override
		public void run() { // file_id > 0 !!! по малките от нула ще са от старата система !!!
			try {
				StringBuilder sql = new StringBuilder();
				sql.append(" select dd.delo_id jid, d.doc_id, d.rn_pored, d.register_id, f.file_id, f.filename ");
				sql.append(" from files f ");
				sql.append(" inner join file_objects fo on fo.file_id = f.file_id ");
				sql.append(" inner join doc d on d.doc_id = fo.object_id and fo.object_code = ?1 ");
				sql.append(" inner join delo_doc dd on dd.input_doc_id = d.doc_id ");
				sql.append(" where f.file_id > 0 and f.content is null order by dd.delo_id desc ");

				this.stream = JPA.getUtil("dest").getEntityManager().createNativeQuery( //
					sql.toString()) //
					.setParameter(1, OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC) //
					.setHint(QueryHints.HINT_FETCH_SIZE, 500) //
					.getResultStream();
			} catch (Exception e) {
				this.ex = e;
			}
		}
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(T5FilesRegister.class);

	static Map<String, String> MIME = new HashMap<>();

	static {
		MIME.put(".bmp", "image/bmp");
		MIME.put(".csv", "text/csv");
		MIME.put(".doc", "application/msword");
		MIME.put(".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
		MIME.put(".exe", "application/x-msdownload");
		MIME.put(".gif", "image/gif");
		MIME.put(".htm", "text/html");
		MIME.put(".html", "text/html");
		MIME.put(".jpeg", "image/jpeg");
		MIME.put(".jpg", "image/jpeg");
		MIME.put(".m4a", "audio/m4a");
		MIME.put(".mp3", "audio/mpeg");
		MIME.put(".mp4", "video/mp4");
		MIME.put(".msg", "application/vnd.ms-outlook");
		MIME.put(".odt", "application/vnd.oasis.opendocument.text");
		MIME.put(".p7m", "application/pkcs7-mime");
		MIME.put(".p7s", "application/pkcs7-signature");
		MIME.put(".pdf", "application/pdf");
		MIME.put(".png", "image/png");
		MIME.put(".pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
		MIME.put(".rar", "application/x-rar-compressed");
		MIME.put(".rtf", "application/rtf");
		MIME.put(".tif", "image/tiff");
		MIME.put(".txt", "text/plain");
		MIME.put(".xls", "application/vnd.ms-excel");
		MIME.put(".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		MIME.put(".zip", "application/zip");
	}

	private String	path;		// \\10.29.0.20\buffer\NN\filingsystem\jfiles\
	private String	separator;	// \

	/**
	 * @param path
	 * @param separator
	 */
	public T5FilesRegister(String path, String separator) {
		this.path = path;
		this.separator = separator;
	}

	/**
	 * Методът
	 *
	 * @param dest
	 * @throws BaseException
	 * @throws SQLException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void transfer(JPA dest) throws BaseException, SQLException, IOException, InterruptedException {
		@SuppressWarnings("unchecked")
		List<Object[]> rows = dest.getEntityManager().createNativeQuery( //
			"select clazz, start_time, end_time from transfer_process where clazz = ?1") //
			.setParameter(1, getClass().getSimpleName()).getResultList();

		if (rows.isEmpty()) { // първо пускане
			dest.begin();
			dest.getEntityManager().createNativeQuery( //
				"insert into transfer_process (clazz, start_time) values (?1, ?2)") //
				.setParameter(1, getClass().getSimpleName()).setParameter(2, new Date()) //
				.executeUpdate();
			dest.commit();

		} else if (rows.get(0)[2] != null) {
			LOGGER.info("   ! finished !");
			return; // значи си е свършил работата
		}

		T0Start.wait(5);
		loadFilesContent(dest);

		// край!
		dest.begin();
		dest.getEntityManager().createNativeQuery( //
			"update transfer_process set end_time = ?1 where clazz = ?2"). //
			setParameter(1, new Date()).setParameter(2, getClass().getSimpleName()) //
			.executeUpdate();
		dest.commit();
	}

	/**
	 * @throws IOException
	 * @throws SQLException
	 * @throws InterruptedException
	 */
	void loadFilesContent(JPA dest) throws DbErrorException, IOException, SQLException, InterruptedException {
		LOGGER.info("");
		LOGGER.info("Start loading FilesContent from omb_register...");

		int updateCnt = 0;

		Query queryUpdate = dest.getEntityManager().createNativeQuery("update files set content_type = ?1, content = ?2 where file_id = ?3");

		Dest2 dest2 = new Dest2(); // тука се прави така че селекта да се пусне в отделна нишка, заради това че като има стрийм и
									// се опитам със същия ентитиманагер да направя комит се къса стрийма.
		dest2.start();
		dest2.join();
		if (dest2.ex != null) { // дало е нещо в селекта
			throw new DbErrorException(dest2.ex);
		}

		dest.begin();

		List<String> currentJidFiles = null;
		int currentJid = -1;
		String currentAbsolutePath = "";

		Iterator<Object[]> iter = dest2.stream.iterator();
		while (iter.hasNext()) {
			Object[] row = iter.next();

			int jid = ((Number) row[0]).intValue();

			if (currentJid != jid) { // нова жалба е на ред
				currentJidFiles = null;
				currentJid = jid; // важно

				File folder = new File(this.path + jid);
				currentAbsolutePath = folder.getAbsolutePath(); // важно

				String[] filenames = folder.list();
				if (filenames != null) {
					currentJidFiles = Arrays.asList(filenames); // важно
				}
			}

			String filename = (String) row[5];
			if (currentJidFiles != null && currentJidFiles.contains(filename)) {
				byte[] fileContent = Files.readAllBytes(new File(currentAbsolutePath + this.separator + filename).toPath());

				String contentType = extractContentType(filename);
				if (contentType == null) {
					LOGGER.warn("  ! files.file_Id=" + row[4] + " ! UNKNOWN MIME filename=" + filename);
				}
				queryUpdate.setParameter(1, contentType); // content_type
				T0Start.setBinaryQueryParam(dest, false, queryUpdate, 2, fileContent); // content
				queryUpdate.setParameter(3, row[4]); // file_id
				queryUpdate.executeUpdate();

			} else {
				// тези ги пропускаме
			}

			updateCnt++;

			if (updateCnt != 0 && updateCnt % 100 == 0) {
				dest.commit();
				dest.begin();
				LOGGER.info("  " + updateCnt);
			}
		}
		dest.commit(); // за последните които не са кратни на 100

		dest2.stream.close();

		LOGGER.info("  " + updateCnt);
		LOGGER.info("End loading FilesContent from omb_register.");
	}

	private String extractContentType(String filename) {
		if (filename == null) {
			return null;
		}
		filename = filename.trim();

		int index = filename.lastIndexOf('.');
		if (index == -1) {
			return null;
		}
		try {
			String ext = filename.substring(index, filename.length());

			return MIME.get(ext.toLowerCase());

		} catch (Exception e) {
			LOGGER.error("Грешка при определяне на MIME за файл: " + filename, e);
//			e.printStackTrace();
		}
		return null;
	}
}
