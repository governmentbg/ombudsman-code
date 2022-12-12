package com.ib.omb.db.dao;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ib.omb.db.dao.DocDAO;
import com.ib.omb.db.dto.Doc;
import com.ib.omb.system.SystemData;
import com.ib.system.ActiveUser;
import com.ib.system.db.JPA;

/**
 * @author belev
 */
public class TestNomerGen {

	private static SystemData sd;

	/** @throws Exception */
	@BeforeClass
	public static void setUp() throws Exception {
		sd = new SystemData();
	}

	/**  */
	@Test
	public void testGenRnDocByRegister() {
		try {

			Doc doc = new Doc();

			doc.setRegistraturaId(1);
			doc.setRegisterId(1);

			new DocDAO(ActiveUser.DEFAULT).generateRnDoc(doc, sd);

			System.out.println(doc.getRnDoc());

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			JPA.getUtil().closeConnection();
		}
	}

	/**  */
	@Test
	public void testGenRnDocByVidDoc() {
		try {

			Doc doc = new Doc();

			doc.setRegisterId(2);
			doc.setRegistraturaId(1);
			doc.setDocVid(4);

			new DocDAO(ActiveUser.DEFAULT).generateRnDoc(doc, sd);

			System.out.println(doc.getRnDoc());

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			JPA.getUtil().closeConnection();
		}
	}
}
