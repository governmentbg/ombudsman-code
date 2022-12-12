package com.ib.omb.experimental;

import static org.junit.Assert.fail;

import org.junit.Test;

import com.ib.omb.experimental.CheckNewData;
import com.ib.omb.experimental.GlobalHolder;
import com.ib.omb.experimental.RegistratureDocHolder;

public class TestHolders {
	
	@Test
	public void TestAllHolders() {
		
		GlobalHolder holder = new GlobalHolder(); //TODO Да се вземе от апликейшън-а
		
		
		try {
			
			CheckNewData cd = new CheckNewData();
			
			//Зареждаме СЕОС и ССЕВ
			cd.fillEgovData(holder);
			
			cd.fillDocData(holder);
			
			RegistratureDocHolder regHolder = holder.getRegInfo(1);
			
			System.out.println("EDelivery = " + regHolder.getCounterEDelivery());
			System.out.println("SEOS = " + regHolder.getCounterSEOS());
			//System.out.println("Mail = " + regHolder.getCounterEmails());
			System.out.println("Nasoch = " + regHolder.getCounterNasochvane());
			System.out.println("Official = " + regHolder.getCounterOfficial());
			System.out.println("Dvig = " + regHolder.getCounterOtherReg());
			
			
		}catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

}
