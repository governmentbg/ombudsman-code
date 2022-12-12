package com.ib.omb.utils;

import static org.junit.Assert.fail;

import java.util.Set;
import java.util.TreeSet;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.omb.system.UserData;
import com.ib.omb.utils.DocDostUtils;
import com.ib.system.db.JPA;
import com.ib.system.exceptions.BaseException;

public class TestDostUtils {

	private static SystemData sd;

	/** @throws Exception */
	@BeforeClass
	public static void setUp() throws Exception {
		sd = new SystemData();
	}

	@Test
	public void testDostChange() {

		try {

			Set<Integer> forDelete = new TreeSet<>();
			forDelete.add(11);
			forDelete.add(12);
			forDelete.add(13);

			Set<Integer> forAdd = new TreeSet<>();
			forAdd.add(11);
			forAdd.add(12);
			forAdd.add(13);

			JPA.getUtil().runInTransaction(() -> new DocDostUtils().addRemoveDocDost(103, OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC, 103, forDelete, forAdd, new SystemData()));
		} catch (BaseException e) {
			e.printStackTrace();
			fail();
		}
	}

	@Test
	public void testRecreateDost() {

		try {
			JPA.getUtil().runInTransaction(() -> new DocDostUtils().recreateUserAccessList(23, sd, new UserData(-1, "", "")));
		} catch (BaseException e) {
			e.printStackTrace();
			fail();
		}
	}

}
