package com.ib.omb.search;

import java.util.*;

import javax.faces.application.FacesMessage;
import javax.persistence.EntityManager;

import com.ib.system.db.TrackableEntity;
import com.ib.system.exceptions.BaseException;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.db.dao.DocDAO;
import com.ib.omb.db.dto.*;
import com.ib.system.ActiveUser;
import com.ib.system.db.JPA;

/**
 * Нишка за индексиране
 *
 * @author l.varbanov
 */
public class IndexThread extends Thread implements Runnable {

	private static final Logger LOGGER = LoggerFactory.getLogger(IndexThread.class);
	private Date dateFrom;
	private Date dateTo;
	private String indexName;

	public IndexThread(String name) {
		this.indexName = name;
	}

	public IndexThread(Date from, Date to, String name) {
		this.dateFrom = from;
		this.dateTo = to;
		this.indexName = name;
	}

	@Override
	public void run() {
		EntityManager em = JPA.getUtil().getEntityManager();
		FullTextEntityManager ftem = Search.getFullTextEntityManager(em);
			if (dateFrom == null && dateTo == null) {
				try {
					if (indexName.equals("Документ")) {
						ftem.createIndexer(DocOcr.class).startAndWait();
					} else if (indexName.equals("Задача")) {
						ftem.createIndexer(TaskOcr.class).startAndWait();
					} else if (indexName.equals("Преписка/Дело")) {
						ftem.createIndexer(Delo.class).startAndWait();
					}
				} catch (InterruptedException e) {
					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, "Грешка при изпълнение на индексиране",
							e.getMessage());
					LOGGER.error("Грешка при изпълнение на индексиране", e);
					Thread.currentThread().interrupt();
				}
			} else {
				try {
					JPA.getUtil().runInTransaction(() -> {
						DocFulltextSearch fulltextDao = new DocFulltextSearch();
						Set<Object[]> docs = new HashSet<>();
						Date dateFromLocal=null,dateToLocal=null;
						if(dateTo==null) {
							dateToLocal = GregorianCalendar.getInstance().getTime();
							dateFromLocal = dateFrom;
						}
						if(dateFrom==null){
							dateFromLocal = GregorianCalendar.getInstance().getTime();
							dateToLocal = dateTo;
						}
						if(dateFrom!=null && dateTo!=null){
							dateFromLocal=dateFrom;
							dateToLocal = dateTo;
						}
						String code = "";

						if (indexName.equals("Документ")) {
							code = "51";
						} else if (indexName.equals("Задача")) {
							code = "59";
						} else if (indexName.equals("Преписка/Дело")) {
							code = "53";
						}
						// Oбират се променените документи от посочения период и се зареждат един по
						// един
						docs.addAll(fulltextDao.findRelatedJournalItemsByDate(dateFromLocal, dateToLocal, code));
						if(code.equals("51")||code.equals("59")) {
							docs.addAll(fulltextDao.findRelatedObjectsToFilesAndJournal(dateFromLocal, dateToLocal, code));
							docs.addAll(fulltextDao.findRelatedObjectsToDeletedFilesAndJournal(dateFromLocal, dateToLocal, code));
						}

						int count = 0;

						for (Object[] object : docs) {
							try {
								Thread.sleep(10);
							} catch (InterruptedException e) {
								LOGGER.error("Грешка при изпълнение на индексиране", e);
								Thread.currentThread().interrupt();

							}
							int obj_id = (int) object[0];

							TrackableEntity fromDb = null;
							switch (Integer.valueOf(code).intValue()) {
								case 51:
									//търсим като релационно ентити
									fromDb = em.find(Doc.class, obj_id);
									if (fromDb == null) {
										//ако го няма го изтриваме и от индекса
										ftem.purge(DocOcr.class, obj_id);
									} else {
										//ако го има го добавяме в индекса
										fromDb = em.find(DocOcr.class, obj_id);
									}
									break;
								case 59:
									fromDb = em.find(Task.class, obj_id);
									if (fromDb == null) {
										ftem.purge(TaskOcr.class, obj_id);
									} else {
										fromDb = em.find(TaskOcr.class, obj_id);
									}
									break;
								case 53:
									fromDb = em.find(Delo.class, obj_id);
									if (fromDb == null) {
										ftem.purge(Delo.class, obj_id);
									}
									break;

							}
							if(fromDb!=null) {
								ftem.index(fromDb);
								em.getTransaction().commit();
								em.getTransaction().begin();
							}
							count++;

						}
						LOGGER.info("File indexed: {}", count);
					});
				} catch (BaseException e) {
					LOGGER.error("Грешка при индексиране в транзакция",e);
				}
			}

	}

	// <<<<<<<<<GETTERS AND SETTERS>>>>>>>>>

	public Date getDateFrom() {
		return dateFrom;
	}

	public void setDateFrom(Date dateFrom) {
		this.dateFrom = dateFrom;
	}

	public Date getDateTo() {
		return dateTo;
	}

	public void setDateTo(Date dateTo) {
		this.dateTo = dateTo;
	}

	public String getIndexName() {
		return indexName;
	}

	public void setIndexName(String indexName) {
		this.indexName = indexName;
	}

}
