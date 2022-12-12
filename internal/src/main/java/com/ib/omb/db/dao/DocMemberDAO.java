package com.ib.omb.db.dao;

import java.util.List;

import com.ib.omb.db.dto.DocMember;
import com.ib.system.ActiveUser;
import com.ib.system.db.AbstractDAO;
import com.ib.system.exceptions.DbErrorException;

/**
 * DAO for {@link DocMember}
 *
 * @author belev
 */
public class DocMemberDAO extends AbstractDAO<DocMember> {

	/** @param user */
	public DocMemberDAO(ActiveUser user) {
		super(DocMember.class, user);
	}

	/**
	 * @param docId
	 * @return
	 * @throws DbErrorException
	 */
	@SuppressWarnings("unchecked")
	public List<DocMember> findByDoc(Integer docId) throws DbErrorException {
		try {
			List<DocMember> members = createQuery("select m from DocMember m where m.docId = ?1") //
				.setParameter(1, docId).getResultList();
			return members;
		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на участници по документа!", e);
		}
	}
}