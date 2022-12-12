package com.ib.omb.db.dao;

import java.util.List;

import javax.persistence.Query;

import com.ib.omb.db.dto.DocWSOptions;
import com.ib.system.ActiveUser;
import com.ib.system.db.AbstractDAO;
import com.ib.system.exceptions.DbErrorException;

public class DocWSOptionsDAO extends AbstractDAO<DocWSOptions> {
    public DocWSOptionsDAO(ActiveUser user) {
        super(DocWSOptions.class, user);
    }
    
    /**
     * Търсене на настройки за регистрация на документи през уеб услуга  по регистратура
     * 
     * @param registratura
     * @return
     * @throws DbErrorException
     */
    @SuppressWarnings("unchecked")
	public List<DocWSOptions> findByRegistraturaId(Integer registratura) throws DbErrorException {
    	
    	try {
			
    		StringBuilder sql = new StringBuilder();
			
    		sql.append(" select op from DocWSOptions op where op.docRegistratura = :registratura ");

			Query query = createQuery(sql.toString()).setParameter("registratura", registratura);
			
			return query.getResultList();

		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на групи служители/кореспонденти за регистратура!", e);
		}
    	
    }
}
