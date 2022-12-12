package com.ib.omb.db.listeners;

import javax.persistence.EntityManager;
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;

import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.omb.db.dto.FullTextIndexedEntity;
import com.ib.omb.db.dto.Task;
import com.ib.omb.db.dto.TaskOcr;
import com.ib.omb.search.DocFulltextSearch;
import com.ib.system.db.JPA;

import java.util.concurrent.CompletableFuture;

public class TaskEntityListener{
	private static final Logger LOGGER = LoggerFactory.getLogger(TaskEntityListener.class);
	@PostPersist
	@PostUpdate
	public void flushEntity(FullTextIndexedEntity entity) {
		DocFulltextSearch taskDao = new DocFulltextSearch();
		CompletableFuture<Object> future = CompletableFuture.supplyAsync(() -> {
			
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				LOGGER.error("Грешка при OCR на файлът",e);
				Thread.currentThread().interrupt();
			}
			
			taskDao.indexTaskWithOcr(((Task)entity).getId());
			return 0;
		});
	}

	@PostRemove
	public void removeTask(Task entity) {

		EntityManager entityManager = JPA.getUtil().getEntityManager();
		FullTextEntityManager ftem = Search.getFullTextEntityManager(entityManager);
		ftem.purge(TaskOcr.class,entity.getId());
	}
}
