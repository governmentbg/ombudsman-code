package com.ib.omb.search;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.hibernate.query.NativeQuery;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.query.dsl.BooleanJunction;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.dsl.RangeMatchingContext;
import org.hibernate.search.query.dsl.RangeTerminationExcludable;
import org.hibernate.type.IntegerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ib.omb.db.dto.Delo;
import com.ib.omb.db.dto.Doc;
import com.ib.omb.db.dto.DocOcr;
import com.ib.omb.db.dto.Task;
import com.ib.omb.db.dto.TaskOcr;
import com.ib.system.db.JPA;
import com.ib.system.db.dto.FileObject;
import com.ib.system.db.dto.SystemJournal;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.DbErrorException;

public class DocFulltextSearch {
	private static final Logger LOGGER = LoggerFactory.getLogger(DocFulltextSearch.class);


	/**
	 *Търси документ в пълнотекстовия индекс
	 *
	 * @param filterCriteria - мап от полета и стойности, по които да се наложи филтър see {@link Doc.FilterFields}
	 * @param searchCriteria - мап от полета и стойности, по които да се търси пълнотекстово търсене see {@link Doc.FullTextFields}
	 *
	 * @return - List съдържащ {@link Doc} обекти, празен ако няма записи, които отговарят на условието, null ако няма условия за
	 *          за формиране на заявка към пълнотекстовия индекс
	 */
	public Set<Integer> searchFullText(Map<String,Object> filterCriteria, Map<String,String> searchCriteria) {
		FullTextEntityManager ftem = Search.getFullTextEntityManager(JPA.getUtil().getEntityManager());
		QueryBuilder qb = ftem.getSearchFactory().buildQueryBuilder().forEntity(DocOcr.class).get();

		BooleanJunction<BooleanJunction> booleanQuery = getBoolenSearch(searchCriteria, qb);
		if(filterCriteria!=null) {
			for (Map.Entry<String, Object> filterItem : filterCriteria.entrySet()) {
				Object value = filterItem.getValue();
				String key = filterItem.getKey();
				if (Collection.class.isAssignableFrom(value.getClass())) {
					BooleanJunction<BooleanJunction> orBool = qb.bool();
					for (Object collectionItem : (Collection) value) {
						orBool.should(qb.keyword().onField(key).matching(collectionItem).createQuery());
					}
					booleanQuery.must(orBool.createQuery());
				} else if (Date.class.isAssignableFrom(value.getClass())) {
					Date dateVal = (Date) value;
					if (dateVal != null) {
						String fieldName = key.replaceAll("Ot", "").replaceAll("Do", "");
						RangeMatchingContext rangeMatchingContext = qb.range().onField(fieldName);
						RangeTerminationExcludable executable = null;
						if (key.contains("Ot")) {
							executable = rangeMatchingContext.above(dateVal);
						} else {
							executable = rangeMatchingContext.below(dateVal);
						}
						booleanQuery.must(executable.createQuery());
					}
				} else {
					// default behaviour
					booleanQuery.must(qb.keyword().onField(key).matching(value).createQuery());
				}
			}
		}
		if (booleanQuery.isEmpty()) return null;
		//execute
		FullTextQuery persistenceQuery =
				ftem.createFullTextQuery(booleanQuery.createQuery(), DocOcr.class).setProjection("id");

		Sort sort = new Sort();
		sort.setSort(new SortField("document_id", SortField.Type.INT));
		persistenceQuery.setSort(sort);
		toQueryStringQuery(persistenceQuery);
		Set<Integer> result = (Set<Integer>) persistenceQuery.getResultStream().collect(Collectors.toSet());
		return result;
	}

	private void toQueryStringQuery(FullTextQuery persistenceQuery) {

		//Мръсен хак направен с цел да се подмени simple_query_string с query_string
		Class<? extends FullTextQuery> cls = persistenceQuery.getClass();
		try {
			Field fieldSearchQuery = cls.getDeclaredField("hSearchQuery");
			fieldSearchQuery.setAccessible(true);
			Object searchQuery = fieldSearchQuery.get(persistenceQuery);
			Field fieldPayload = searchQuery.getClass().getDeclaredField("rawSearchPayload");
			fieldPayload.setAccessible(true);
			com.google.gson.JsonObject payload = (JsonObject) fieldPayload.get(searchQuery);
			String payloadstring = payload.toString();
			payloadstring = payloadstring.replaceAll("simple_query_string","query_string");
			JsonObject newValue = new JsonParser().parse(payloadstring).getAsJsonObject();
			fieldPayload.set(searchQuery,newValue);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	/**
	 *Търси задачи в пълнотекстовия индекс
	 *
	 * @param filterCriteria - мап от полета и стойности, по които да се наложи филтър see {@link Task.FilterFields}
	 * @param searchCriteria - мап от полета и стойности, по които да се търси пълнотекстово търсене see {@link Task.FullTextFields}
	 *
	 * @return - List съдържащ {@link Task} обекти, празен ако няма записи, които отговарят на условието, null ако няма условия за
	 *          за формиране на заявка към пълнотекстовия индекс
	 */
	public Set<Integer> searchTaskFullText(Map<String,Object> filterCriteria, Map<String,String> searchCriteria) {
		FullTextEntityManager ftem = Search.getFullTextEntityManager(JPA.getUtil().getEntityManager());
		QueryBuilder qb = ftem.getSearchFactory().buildQueryBuilder().forEntity(TaskOcr.class).get();

		BooleanJunction<BooleanJunction> booleanQuery = getBoolenSearch(searchCriteria, qb);
		if(filterCriteria!=null) {
			for (Map.Entry<String, Object> filterItem : filterCriteria.entrySet()) {
				Object value = filterItem.getValue();
				String key = filterItem.getKey();
				if (Collection.class.isAssignableFrom(value.getClass())) {
					BooleanJunction<BooleanJunction> orBool = qb.bool();
					for (Object collectionItem : (Collection) value) {
						orBool.should(qb.keyword().onField(key).matching(collectionItem).createQuery());
					}
					booleanQuery.must(orBool.createQuery());
				} else if (Date.class.isAssignableFrom(value.getClass())) {
					Date dateVal = (Date) value;
					if (dateVal != null) {
						String fieldName = key.replaceAll("From", "").replaceAll("To", "");
						RangeMatchingContext rangeMatchingContext = qb.range().onField(fieldName);
						RangeTerminationExcludable executable = null;
						if (key.contains("From")) {
							executable = rangeMatchingContext.above(dateVal);
						} else {
							executable = rangeMatchingContext.below(dateVal);
						}
						booleanQuery.must(executable.createQuery());
					}
				} else {
					// default behaviour
					booleanQuery.must(qb.keyword().onField(key).matching(value).createQuery());

				}
			}
		}
		if (booleanQuery.isEmpty()) return null;
		//execute
		FullTextQuery persistenceQuery =
				ftem.createFullTextQuery(booleanQuery.createQuery(), TaskOcr.class).setProjection("id");
		toQueryStringQuery(persistenceQuery);
		List<Objects[]> result = persistenceQuery.getResultList();
		return unwrapToIntegerCollection(result);
	}

	
	
	/**
	 *Търси дела в пълнотекстовия индекс
	 *
	 * @param filterCriteria - мап от полета и стойности, по които да се наложи филтър see {@link Delo.FilterFields}
	 * @param searchCriteria - мап от полета и стойности, по които да се търси пълнотекстово търсене see {@link Delo.FullTextFields}
	 *
	 * @return - List съдържащ {@link Delo} обекти, празен ако няма записи, които отговарят на условието, null ако няма условия за
	 *          за формиране на заявка към пълнотекстовия индекс
	 */
	public Set<Integer> searchDeloFullText(Map<String,Object> filterCriteria, Map<String,String> searchCriteria) {
		FullTextEntityManager ftem = Search.getFullTextEntityManager(JPA.getUtil().getEntityManager());
		QueryBuilder qb = ftem.getSearchFactory().buildQueryBuilder().forEntity(Delo.class).get();

		BooleanJunction<BooleanJunction> booleanQuery = getBoolenSearch(searchCriteria, qb);
		if(filterCriteria!=null) {
			for (Map.Entry<String, Object> filterItem : filterCriteria.entrySet()) {
				Object value = filterItem.getValue();
				String key = filterItem.getKey();
				if (Collection.class.isAssignableFrom(value.getClass())) {
					BooleanJunction<BooleanJunction> orBool = qb.bool();
					for (Object collectionItem : (Collection) value) {
						orBool.should(qb.keyword().onField(key).matching(collectionItem).createQuery());
					}
					booleanQuery.must(orBool.createQuery());
				} else if (Date.class.isAssignableFrom(value.getClass())) {
					Date dateVal = (Date) value;
					if (dateVal != null) {
						String fieldName = key.replaceAll("From", "").replaceAll("To", "");
						RangeMatchingContext rangeMatchingContext = qb.range().onField(fieldName);
						RangeTerminationExcludable executable = null;
						if (key.contains("From")) {
							executable = rangeMatchingContext.above(dateVal);
						} else {
							executable = rangeMatchingContext.below(dateVal);
						}
						booleanQuery.must(executable.createQuery());
					}
				} else {
					// default behaviour
					booleanQuery.must(qb.keyword().onField(key).matching(value).createQuery());

				}
			}
		}
		if (booleanQuery.isEmpty()) return null;
		//execute
		FullTextQuery persistenceQuery =
				ftem.createFullTextQuery(booleanQuery.createQuery(), Delo.class).setProjection("id");
		toQueryStringQuery(persistenceQuery);
		List<Objects[]> result = persistenceQuery.getResultList();
		return unwrapToIntegerCollection(result);
	}
	
	
	
	/**
	 * Премахва опаковката Object[] от списък с List<Object[]>, като очаква елементите на Object да са Integer
	 * @param objectsArray
	 * @return
	 */
	private Set<Integer> unwrapToIntegerCollection(List<Objects[]> objectsArray) {
		Set<Integer> result = new HashSet<Integer>();
		for (Object[] obj:objectsArray) {
			result.add((Integer) obj[0]);
		}
		return result;
	}

	public List<Doc> suggestions(String query, Map<String, String> searchCriteria){
		FullTextEntityManager ftem = Search.getFullTextEntityManager(JPA.getUtil().getEntityManager());
		QueryBuilder qb = ftem.getSearchFactory().buildQueryBuilder().forEntity(Doc.class).get();

		BooleanJunction<BooleanJunction> booleanQuery = getBoolenSearch(searchCriteria, qb);
		FullTextQuery persistenceQuery =
				ftem.createFullTextQuery(booleanQuery.createQuery(), Doc.class);
		persistenceQuery.setMaxResults(20);
		toQueryStringQuery(persistenceQuery);
		List<Doc> result = persistenceQuery.getResultList();
		return result;
	}

	/**
	 *  Връща пълнотекстовата част от Boolean Query-to
	 * @param searchCriteria
	 * @param qb
	 * @return
	 */
	private BooleanJunction<BooleanJunction> getBoolenSearch(Map<String, String> searchCriteria, QueryBuilder qb) {
		BooleanJunction<BooleanJunction> booleanQuery = qb.bool();
		if (searchCriteria != null){
			for (Map.Entry<String, String> searchItem : searchCriteria.entrySet()) {
				booleanQuery.must(qb.simpleQueryString().onField(searchItem.getKey()).withAndAsDefaultOperator().matching(searchItem.getValue()).createQuery());
			}
		}
		return booleanQuery;
	}
	
	public List<Object[]> findRelatedJournalItemsByDate(Date from, Date to, String code) throws DbErrorException {
		try {

			StringBuilder sql = new StringBuilder();
			sql.append("select distinct j.ID_OBJECT obj_id,j.code_object obj_code , j.date_action from  system_journal j\r\n"
					+ "where j.CODE_OBJECT in (:code) -- doc \r\n"
					+ "and j.CODE_ACTION in (1,2,3) -- zapis i korekcia 3 e iztrivane\r\n"
					+ "and j.DATE_ACTION >=  :from\r\n" 
					+ "and j.DATE_ACTION<= :to\r\n"
					+ "order by j.DATE_ACTION desc");

			Query nativeQuery = JPA.getUtil().getEntityManager().createNativeQuery(sql.toString());
			nativeQuery.setParameter("from", from);
			nativeQuery.setParameter("to", to);
			nativeQuery.setParameter("code", code);
			NativeQuery hibernateQuery = nativeQuery.unwrap(NativeQuery.class);
			hibernateQuery.addScalar("obj_id", IntegerType.INSTANCE);
			hibernateQuery.addScalar("obj_code", IntegerType.INSTANCE);
			
			@SuppressWarnings("unchecked")
			List<Object[]> result = nativeQuery.getResultList();

			if (result.size() == 0) {
				LOGGER.error("NO DOCUMENTS WERE FOUND!");
			}

			return result;

		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на основни данни за документ!", e);
		}
	}

	public void indexDocWithOcr(Integer id) {
		try {
			JPA.getUtil().runInTransaction(()->{
				EntityManager entityManager = JPA.getUtil().getEntityManager();
				FullTextEntityManager em = Search.getFullTextEntityManager(entityManager);
				DocOcr doc = entityManager.find(DocOcr.class, id);
				em.index(doc);
			});
		} catch (BaseException e) {
			e.printStackTrace();
		}
	}

	public void indexTaskWithOcr(Integer id) {
		try {
			JPA.getUtil().runInTransaction(()->{
				EntityManager entityManager = JPA.getUtil().getEntityManager();
				FullTextEntityManager em = Search.getFullTextEntityManager(entityManager);
				TaskOcr doc = entityManager.find(TaskOcr.class, id);
				em.index(doc);
			});
		} catch (BaseException e) {
			e.printStackTrace();
		}
	}

	public Collection<? extends Object[]> findRelatedObjectsToFilesAndJournal(Date from,Date to, String code) throws DbErrorException {
		try {
			StringBuilder sql = new StringBuilder();
			sql.append("select distinct fo.OBJECT_ID obj_id,fo.OBJECT_CODE obj_code from \n" +
					"FILE_OBJECTS fo \n" +
					"inner join system_journal j on j.ID_OBJECT=fo.ID and j.CODE_OBJECT = 14 and j.CODE_ACTION in (1,2)\n" +
					"where j.DATE_ACTION >=  :from and j.DATE_ACTION<= :to and fo.OBJECT_CODE = :code");

			Query nativeQuery = JPA.getUtil().getEntityManager().createNativeQuery(sql.toString());
			nativeQuery.setParameter("from", from);
			nativeQuery.setParameter("to", to);
			nativeQuery.setParameter("code", code);

			NativeQuery hibernateQuery = nativeQuery.unwrap(NativeQuery.class);
			hibernateQuery.addScalar("obj_id", IntegerType.INSTANCE);
			hibernateQuery.addScalar("obj_code", IntegerType.INSTANCE);

			@SuppressWarnings("unchecked")
			List<Object[]> result = nativeQuery.getResultList();

			if (result.size() == 0) {
				LOGGER.error("NO DOCUMENTS WERE FOUND!");
			}

			return result;

		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на основни данни за документ!", e);
		}
	}

	public Collection<? extends Object[]> findRelatedObjectsToDeletedFilesAndJournal(Date from, Date to, String code) throws DbErrorException {
		try {

			StringBuilder sql = new StringBuilder();
			sql.append("select j.id obj_id, j.CODE_OBJECT obj_code from system_journal j \n" +
					"where j.DATE_ACTION >=  :from " +
					"and j.DATE_ACTION<= :to  " +
					"and j.CODE_ACTION =3 and j.CODE_OBJECT = 14");

			EntityManager entityManager = JPA.getUtil().getEntityManager();
			Query nativeQuery = entityManager.createNativeQuery(sql.toString());
			nativeQuery.setParameter("from", from);
			nativeQuery.setParameter("to", to);


			NativeQuery hibernateQuery = nativeQuery.unwrap(NativeQuery.class);
			hibernateQuery.addScalar("obj_id", IntegerType.INSTANCE);
			hibernateQuery.addScalar("obj_code", IntegerType.INSTANCE);

			@SuppressWarnings("unchecked")
			List<Object[]> result = nativeQuery.getResultList();

			if (result.size() == 0) {
				LOGGER.error("NO DOCUMENTS WERE FOUND!");
				return result;
			}
			Set<Object[]> fileObjects = new HashSet<>();

			for (Object[] item:result) {
				SystemJournal j = entityManager.find(SystemJournal.class, item[0]);
				FileObject fileObject = (FileObject) new ObjectInputStream(new ByteArrayInputStream(j.getObjectContent())).readObject();
				if(code.equals(fileObject.getObjectCode().toString())){
					fileObjects.add(new Object[]{fileObject.getObjectId(),code});
				}
			}

			return fileObjects;

		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на основни данни за документ!", e);
		}
	}
}
