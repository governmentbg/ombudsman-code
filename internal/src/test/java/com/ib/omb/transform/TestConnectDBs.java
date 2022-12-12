package com.ib.omb.transform;

import com.ib.system.db.JPA;

/**
 * @author belev
 */
public class TestConnectDBs {

	/** @param args */
	public static void main(String[] args) {

		JPA dest = null;
		try {
			dest = JPA.getUtil("dest");

			Object count = dest.getEntityManager().createNativeQuery("select count(*) from doc").getResultList().get(0);

			System.out.println("SUCCESS.dest count.doc=" + count);

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (dest != null) {
				dest.closeConnection();
			}
		}

		JPA sourceEdsd = null;
		try {
			sourceEdsd = JPA.getUtil("sourceEdsd");

			Object count = sourceEdsd.getEntityManager().createNativeQuery("select count(*) from documents").getResultList().get(0);

			System.out.println("SUCCESS.sourceEdsd count.documents=" + count);

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (sourceEdsd != null) {
				sourceEdsd.closeConnection();
			}
		}

		JPA sourceReg = null;
		try {
			sourceReg = JPA.getUtil("sourceReg");

			Object count = sourceReg.getEntityManager().createNativeQuery("select count(*) from jalbi").getResultList().get(0);

			System.out.println("SUCCESS.sourceReg count.jalbi=" + count);

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (sourceReg != null) {
				sourceReg.closeConnection();
			}
		}
		
		JPA sourceMssql = null;
		try {
			sourceMssql = JPA.getUtil("sourceMssql");

			Object count = sourceMssql.getEntityManager().createNativeQuery("select count(*) from Document").getResultList().get(0);

			System.out.println("SUCCESS.sourceMssql count.Document=" + count);

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (sourceMssql != null) {
				sourceMssql.closeConnection();
			}
		}
	}
}
