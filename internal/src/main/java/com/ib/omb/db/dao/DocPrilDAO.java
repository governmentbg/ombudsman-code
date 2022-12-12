package com.ib.omb.db.dao;

import java.util.HashMap;
import java.util.Map;

import com.ib.omb.db.dto.DocPril;
import com.ib.system.ActiveUser;
import com.ib.system.db.AbstractDAO;
import com.ib.system.db.SelectMetadata;

/**
 * DAO for {@link DocPril}
 *
 * @author belev
 */
public class DocPrilDAO extends AbstractDAO<DocPril> {

	/** @param user */
	public DocPrilDAO(ActiveUser user) {
		super(DocPril.class, user);
	}

	/**
	 * Търсене на приложения по докукемнта<br>
	 * Конструира селект за подаден docId, като изтегля само данните от вида:<br>
	 * [0]-DOC_PRIL.ID<br>
	 * [1]-DOC_ID<br>
	 * [2]-NOMER<br>
	 * [3]-MEDIA_TYPE<br>
	 * [4]-PRIL_NAME<br>
	 * [5]-PRIL_INFO<br>
	 * [6]-COUNT_SHEETS<br>
	 *
	 * @param docId
	 * @return
	 */
	public SelectMetadata createSelectPrilList(Integer docId) {
		Map<String, Object> params = new HashMap<>();

		String select = " select p.ID a0, p.DOC_ID a1, p.NOMER a2, p.MEDIA_TYPE a3, p.PRIL_NAME a4, p.PRIL_INFO a5, p.COUNT_SHEETS a6 ";
		String from = " from DOC_PRIL p ";
		String where = " where p.DOC_ID = :docId ";

		params.put("docId", docId);

		SelectMetadata sm = new SelectMetadata();

		sm.setSql(select + from + where);
		sm.setSqlCount(" select count(*) " + from + where);
		sm.setSqlParameters(params);

		return sm;
	}
}