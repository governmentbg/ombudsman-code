package com.ib.omb.utils;

import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.junit.Test;

import com.ib.omb.system.UserData;
import com.ib.omb.utils.DashboardUtils;
import com.ib.system.exceptions.DbErrorException;

public class TestDashBoardUtils {
	
	@Test
	public void testTaskSection() {
		
		HashMap<Long, String> map = new HashMap<Long, String>();
		try {
			new DashboardUtils().calculateTasksSection(-1, map, true, true, true,7,Arrays.asList(new Integer(1),new Integer(2)));
			
			//Принт мап
			Iterator<Entry<Long, String>> it = map.entrySet().iterator();
			while (it.hasNext()) {
				Entry<Long, String> entry = it.next();
				System.out.println(entry.getKey() + " --> " + entry.getValue());
			}
			
			
		} catch (DbErrorException e) {			
			e.printStackTrace();
			fail();
		}
	}
	
	@Test
	public void testDockSection() {
		
		HashMap<Long, String> map = new HashMap<Long, String>();
		try {
			UserData ud = new UserData(-1, "", "");
			ud.setRegistratura(1);
			new DashboardUtils().calculateDocSection(ud, map,Arrays.asList(new Integer(1),new Integer(2)));
			
			//ssss
			
			//Принт мап
			Iterator<Entry<Long, String>> it = map.entrySet().iterator();
			while (it.hasNext()) {
				Entry<Long, String> entry = it.next();
				System.out.println(entry.getKey() + " --> " + entry.getValue());
			}
			
			
		} catch (DbErrorException e) {			
			e.printStackTrace();
			fail();
		}
	}
	

}
