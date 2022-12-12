package com.ib.omb.db.listeners;

import com.ib.omb.db.dto.Doc;
import com.ib.omb.db.dto.DocOcr;
import com.ib.omb.db.dto.FullTextIndexedEntity;
import com.ib.omb.search.DocFulltextSearch;
import com.ib.system.db.JPA;

import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;

import java.util.concurrent.CompletableFuture;

public class DocEntityListener{
	private static final Logger LOGGER = LoggerFactory.getLogger(DocEntityListener.class);
	@PostPersist
	@PostUpdate
	public void flushEntity(FullTextIndexedEntity entity) {

		DocFulltextSearch searchDao = new DocFulltextSearch();
		CompletableFuture<Object> completableFuture = CompletableFuture.supplyAsync(() -> {
			
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					LOGGER.error("Грешка при OCR на файлът",e);
					Thread.currentThread().interrupt();
				}
			
			searchDao.indexDocWithOcr(((Doc)entity).getId());

			return 0;
		});
		completableFuture.isDone();

	}

	@PostRemove
	public void removeDoc(Doc entity) {
		FullTextEntityManager ftem = Search.getFullTextEntityManager(JPA.getUtil().getEntityManager());
		ftem.purge(DocOcr.class,entity.getId());
	}

}
