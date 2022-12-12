package com.ib.omb.db.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.pagination.LazyDataModelSQL2Array;
import com.ib.omb.db.dao.DeloDAO;
import com.ib.omb.db.dao.DeloDeloDAO;
import com.ib.omb.db.dao.DeloDocDAO;
import com.ib.omb.db.dao.DocDAO;
import com.ib.omb.db.dao.DocProtocolDAO;
import com.ib.omb.db.dto.*;
import com.ib.omb.search.DocFulltextSearch;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.system.ActiveUser;
import com.ib.system.db.JPA;
import com.ib.system.db.SelectMetadata;
import com.ib.system.db.dto.SystemJournal;
import com.ib.system.exceptions.BaseException;
import com.ib.system.utils.SearchUtils;
import com.ib.system.utils.Serializator;

/**
 * Test class for {@link DocDAO}
 *
 * @author belev
 */
public class TestDocDAO {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestDocDAO.class);

	private static SystemData sd;

	private static DocDAO		dao;
	private static DeloDAO		deloDao;
	private static DeloDocDAO	deloDocDao;
	private static DeloDeloDAO	deloDeloDao;

	/**  */
	@BeforeClass
	public static void setUp() {
		dao = new DocDAO(ActiveUser.DEFAULT);
		deloDao = new DeloDAO(ActiveUser.DEFAULT);
		deloDocDao = new DeloDocDAO(ActiveUser.DEFAULT);
		deloDeloDao = new DeloDeloDAO(ActiveUser.DEFAULT);

		sd = new SystemData();
	}

	private Doc				doc;
	private DeloDoc			deloDoc;
	private Object[]		docData;
	private SystemJournal	journal;

	/**
	 * Test method for
	 * {@link DocProtocolDAO#createSelectDestructProtocol(Integer, Integer, Date, Date, Boolean, String, boolean, Date, Date, String, boolean)}.
	 */
	@Test
	public void testCreateSelectProtocolDestruction() {
		try {
			SelectMetadata sm = new DocProtocolDAO(1, ActiveUser.DEFAULT).createSelectDestructProtocol(1, 7, null, null, null, null, false, null, null, null, false, null, null);

			LazyDataModelSQL2Array lazy = new LazyDataModelSQL2Array(sm, "a0");
			List<Object[]> result = lazy.load(0, lazy.getRowCount(), null, null);

			for (Object[] row : result) {
				LOGGER.info(Arrays.toString(row));
			}
			LOGGER.info("{}", result.size());

		} catch (Exception e) {
			fail(e.getMessage());
			LOGGER.error(e.getMessage(), e);
		}
	}

	/** */
	@Test
	public void testFindById() {
		try {
			JPA.getUtil().runWithClose(() -> this.doc = dao.findById(209));
			if (this.doc != null) {
				assertNotNull(this.doc.getId());
			}
		} catch (BaseException e) {
			LOGGER.error(e.getMessage(), e);
			fail(e.getMessage());
		}
	}

	/** */
	@Test
	public void testFindDocData() {
		try {
			JPA.getUtil().runWithClose(() -> this.docData = dao.findDocData(104));
			if (this.docData != null) {
				assertNotNull(this.docData[0]);

				LOGGER.info(Arrays.toString(this.docData));
			}
		} catch (BaseException e) {
			LOGGER.error(e.getMessage(), e);
			fail(e.getMessage());
		}
	}

	/** */
	@Test
	public void testFindDocSettings() {
		try {
			Object[] result = dao.findDocSettings(1, 5, sd);
			LOGGER.info(Arrays.toString(result));

		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			fail(e.getMessage());
		}
	}

	/** */
//	@Test
	public void testFullCycle() {

		this.doc = new Doc();

		this.doc.setRegistraturaId(1);
		this.doc.setRegisterId(1);
		this.doc.setOtnosno("TestRef");

		this.doc.setDocType(2);
		this.doc.setDocVid(1);
		this.doc.setDocDate(new Date());
		this.doc.setFreeAccess(1);

		this.doc.setReferentsAgreed(new ArrayList<>());
		this.doc.setReferentsAuthor(new ArrayList<>());
		this.doc.setReferentsSigned(new ArrayList<>());

		DocReferent author = new DocReferent(-1, OmbConstants.CODE_ZNACHENIE_DOC_REF_ROLE_AUTHOR);
		this.doc.getReferentsAuthor().add(author);

		DocReferent sagl = new DocReferent(-1, OmbConstants.CODE_ZNACHENIE_DOC_REF_ROLE_AGREED);
		this.doc.getReferentsAgreed().add(sagl);

		LOGGER.info("-------------------- Добавяме документ с референти и го записваме ! --------------------");
		try {
			JPA.getUtil().runInTransaction(() -> this.doc = dao.save(this.doc, false, null, null, sd));
		} catch (BaseException e) {
			LOGGER.error(e.getMessage(), e);
			fail(e.getMessage());
		}

		LOGGER.info("-------------------- Зареждане и корекции ! --------------------");
		try {
			JPA.getUtil().runWithClose(() -> this.doc = dao.findById(this.doc.getId()));

			// Един добавен
			DocReferent podpis = new DocReferent(-1, OmbConstants.CODE_ZNACHENIE_DOC_REF_ROLE_SIGNED);
			this.doc.getReferentsSigned().add(podpis);

			// Един коригиран
			this.doc.getReferentsAuthor().get(0).setComments("alabala");

			// Един изтрит
			this.doc.getReferentsAgreed().remove(0);

			JPA.getUtil().runInTransaction(() -> this.doc = dao.save(this.doc, false, null, null, sd));

		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			fail(e.getMessage());
		}

		LOGGER.info("-------------------- Още веднъж само запис --------------------");
		try {
			JPA.getUtil().runInTransaction(() -> this.doc = dao.save(this.doc, false, null, null, sd));

		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			fail(e.getMessage());
		}

		LOGGER.info("-------------------- Изтриване на документ  --------------------");
		try {
			JPA.getUtil().runWithClose(() -> this.doc = dao.findById(this.doc.getId()));
			JPA.getUtil().runInTransaction(() -> dao.delete(this.doc));

		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			fail(e.getMessage());
		}

	}

	/** */
//	@Test
	public void testGenerateRnDoc() {
		try {
			this.doc = new Doc();
			this.doc.setRegistraturaId(1);
			this.doc.setRegisterId(1);
			this.doc.setDocVid(5);

			JPA.getUtil().runWithClose(() -> dao.generateRnDoc(this.doc, sd));

			assertNotNull(this.doc.getRnDoc());
			assertNotNull(this.doc.getRnPored());

			LOGGER.info("rnDoc={}", this.doc.getRnDoc());
			LOGGER.info("rnPrefix={}", this.doc.getRnPrefix());
			LOGGER.info("rnPored={}", this.doc.getRnPored());

		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			fail(e.getMessage());
		}
	}

	/** */
//	@Test
	public void testJournalDoc() {
		try {
			JPA.getUtil().runWithClose(() -> this.journal = JPA.getUtil().getEntityManager().find(SystemJournal.class, 274));
		} catch (BaseException e) {
			LOGGER.error(e.getMessage(), e);
			fail(e.getMessage());
		}

		if (this.journal != null) {

			try {
				this.doc = (Doc) Serializator.deSerializeFromBArray(this.journal.getObjectContent());

				LOGGER.info("{}", this.doc);

			} catch (ClassNotFoundException | IOException e) {
				LOGGER.error(e.getMessage(), e);
			}
		}
	}

//	@Test
	public void testSearchFullText() throws InterruptedException {
		HashMap<String, Object> filterCriteria = new HashMap<>();
		Map<String, String> searchCriteria = new HashMap<>();
		reindexAll();
		DocFulltextSearch d = new DocFulltextSearch();
		Set<Integer> result = null;
//		result = dao.searchFullText(null, null);
//		Assert.assertNull(result);
//
//		filterCriteria.put(Doc.FilterFields.codeRefCorresp.name(),-2);
//		result = dao.searchFullText(filterCriteria, searchCriteria);
//		Assert.assertNotNull( "Резултата е null", result);
//		Assert.assertTrue("Резултата не е празен List",result.isEmpty());

		filterCriteria.put(DocOcr.FilterFields.codeRefCorresp.name(), 152);
		searchCriteria.put(DocOcr.FullTextFields.docInfo.name(), "забележка");
		searchCriteria.put(DocOcr.FullTextFields.otnosno.name(), "нова");
		filterCriteria.put(DocOcr.FilterFields.docDateOt.name(), Date.from(LocalDate.of(2020, 2, 14).atStartOfDay(TimeZone.getDefault().toZoneId()).toInstant()));
		filterCriteria.put(DocOcr.FilterFields.docDateDo.name(), new Date());

		List<Integer> list = new ArrayList<>();
		list.add(1);
		list.add(3);
		list.add(5);
		filterCriteria.put(DocOcr.FilterFields.docVid.name(), list);
		result = d.searchFullText(filterCriteria, searchCriteria);
		Assert.assertNotNull("Резултата е null", result);
		Assert.assertFalse("Резултата е празен List", result.isEmpty());

		searchCriteria.put(DocOcr.FullTextFields.otnosno.name(), "стара");
		result = d.searchFullText(filterCriteria, searchCriteria);
		Assert.assertNotNull("Резултата е null", result);
		Assert.assertTrue("Резултата не е празен List", result.isEmpty());

	}

	private void reindexAll() throws InterruptedException {
		FullTextEntityManager ftem = Search.getFullTextEntityManager(JPA.getUtil().getEntityManager());
		ftem.createIndexer().startAndWait();
	}

}