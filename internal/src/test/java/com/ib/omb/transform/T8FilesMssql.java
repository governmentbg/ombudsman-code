package com.ib.omb.transform;

import java.io.IOException;
import java.sql.SQLException;
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
 * Прехвърля файловото съдържание на документите от Стар Регистър жалби/Деловодна система
 *
 * @author belev
 */
public class T8FilesMssql {

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
				sql.append(" select f.file_id, f.filename, fo.object_id ");
				sql.append(" from files f ");
				sql.append(" inner join file_objects fo on fo.file_id = f.file_id and fo.object_code = ?1 ");
				sql.append(" where f.file_id < 0 and f.content is null ");
				sql.append(" order by fo.object_id ");

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

	private static final Logger LOGGER = LoggerFactory.getLogger(T8FilesMssql.class);

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

		MIME.put(".og1", "application/ogg");
		MIME.put(".shs", "application/octet-stream");
		MIME.put(".wbk", "application/msword");
	}

	/** */
	public T8FilesMssql() {
		super();
	}

	/**
	 * Методът
	 *
	 * @param sourceMssql
	 * @param dest
	 * @throws BaseException
	 * @throws SQLException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void transfer(JPA sourceMssql, JPA dest) throws BaseException, SQLException, IOException, InterruptedException {
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
		loadFilesContent(sourceMssql, dest);

		// край!
		dest.begin();
		dest.getEntityManager().createNativeQuery( //
			"update transfer_process set end_time = ?1 where clazz = ?2"). //
			setParameter(1, new Date()).setParameter(2, getClass().getSimpleName()) //
			.executeUpdate();
		dest.commit();
	}

	/** */
	void loadFilesContent(JPA sourceMssql, JPA dest) throws DbErrorException, SQLException, InterruptedException {
		LOGGER.info("");
		LOGGER.info("Start loading FilesContent from omb_mssql...");

		int updateCnt = 0;

		Query queryUpdate = dest.getEntityManager().createNativeQuery("update files set content_type = ?1, content = ?2 where file_id = ?3");

		Query queryContent = sourceMssql.getEntityManager().createNativeQuery("select ID, FileData from DocFiles where Doc_ID = ?1");

		Dest2 dest2 = new Dest2(); // тука се прави така че селекта да се пусне в отделна нишка, заради това че като има стрийм и
									// се опитам със същия ентитиманагер да направя комит се къса стрийма.
		dest2.start();
		dest2.join();
		if (dest2.ex != null) { // дало е нещо в селекта
			throw new DbErrorException(dest2.ex);
		}

		dest.begin();

		Iterator<Object[]> iter = dest2.stream.iterator();
		while (iter.hasNext()) {
			Object[] row = iter.next(); // f.file_id, f.filename, fo.object_id

			int docId = ((Number) row[2]).intValue();

			@SuppressWarnings("unchecked")
			List<Object[]> data = queryContent.setParameter(1, docId * -1).getResultList();
			if (data.isEmpty() || data.get(0)[1] == null) {
				LOGGER.warn("  ! DocFiles.Doc_ID=" + docId + " ! NULL FileData");

			} else {
				byte[] fileContent = (byte[]) data.get(0)[1];

				String contentType = extractContentType((String) row[1]);

				queryUpdate.setParameter(1, contentType); // content_type
				T0Start.setBinaryQueryParam(dest, false, queryUpdate, 2, fileContent); // content
				queryUpdate.setParameter(3, row[0]); // file_id
				queryUpdate.executeUpdate();

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
		LOGGER.info("End loading FilesContent from omb_mssql.");
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
