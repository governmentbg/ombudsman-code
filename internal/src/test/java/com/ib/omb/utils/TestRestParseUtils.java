package com.ib.omb.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.junit.Test;

import com.ib.omb.rest.mobileJalbi.RestParseUtils;
import com.ib.omb.system.OmbConstants;

public class TestRestParseUtils {
	
	@Test
	public void testParseInteger() {
		assertEquals(123456, (long) RestParseUtils.parseInteger("123456"));
		assertEquals(123456, (long) RestParseUtils.parseInteger(" 123456   "));
	}
	
	@Test
	public void testParseIntegerNull() {
		assertNull(RestParseUtils.parseInteger(null));
		assertNull(RestParseUtils.parseInteger(""));
		assertNull(RestParseUtils.parseInteger("    "));
	}
	
	@Test(expected = NumberFormatException.class)
	public void testParseIntegerExceptionOverlow() {
		RestParseUtils.parseInteger("123456789123");
	}
	
	@Test(expected = NumberFormatException.class)
	public void testParseIntegerExceptionInvalid() {
		RestParseUtils.parseInteger("12x3");
	}
	
	@Test(expected = NumberFormatException.class)
	public void testParseIntegerExceptionNull() {
		RestParseUtils.parseInteger("null");
	}
	
	@Test
	public void testParseLong() {
		assertEquals(1234567891234L, (long) RestParseUtils.parseLong("1234567891234"));
		assertEquals(1234567891234L, (long) RestParseUtils.parseLong(" 1234567891234   "));
	}	
	
	@Test
	public void testParseLongNull() {
		assertNull(RestParseUtils.parseLong(null));
		assertNull(RestParseUtils.parseLong(""));
		assertNull(RestParseUtils.parseLong("    "));
	}
	
	@Test(expected = NumberFormatException.class)
	public void testParseLongException1() {
		RestParseUtils.parseLong("x");
	}
	
	@Test(expected = NumberFormatException.class)
	public void testParseLongException2() {
		RestParseUtils.parseLong("null");
	}
	
	@Test
	public void testParseYesNoNull() {
		assertNull(RestParseUtils.parseBooleanToYesNo(null));
		assertNull(RestParseUtils.parseBooleanToYesNo(""));
		assertNull(RestParseUtils.parseBooleanToYesNo("    "));
	}
	
	@Test
	public void testParseYes() {
		assertEquals((long) OmbConstants.CODE_ZNACHENIE_DA, (long) RestParseUtils.parseBooleanToYesNo("true"));
		assertEquals((long) OmbConstants.CODE_ZNACHENIE_DA, (long) RestParseUtils.parseBooleanToYesNo(" true   "));
		assertEquals((long) OmbConstants.CODE_ZNACHENIE_DA, (long) RestParseUtils.parseBooleanToYesNo("tRue"));
		assertEquals((long) OmbConstants.CODE_ZNACHENIE_DA, (long) RestParseUtils.parseBooleanToYesNo(" TrUe "));
	}
	
	@Test
	public void testParseNo() {
		assertEquals((long) OmbConstants.CODE_ZNACHENIE_NE, (long) RestParseUtils.parseBooleanToYesNo("false"));
		assertEquals((long) OmbConstants.CODE_ZNACHENIE_NE, (long) RestParseUtils.parseBooleanToYesNo(" false   "));
		assertEquals((long) OmbConstants.CODE_ZNACHENIE_NE, (long) RestParseUtils.parseBooleanToYesNo("fAlSe"));
		assertEquals((long) OmbConstants.CODE_ZNACHENIE_NE, (long) RestParseUtils.parseBooleanToYesNo(" FaLsE "));
	}
	
	@Test
	public void testParseBooleanTrue() {
		assertTrue(RestParseUtils.parseBoolean("true"));
		assertTrue(RestParseUtils.parseBoolean(" true   "));
		assertTrue(RestParseUtils.parseBoolean("tRuE"));
		assertTrue(RestParseUtils.parseBoolean("   TrUe "));
	}
	
	@Test
	public void testParseBooleanFalse() {
		assertFalse(RestParseUtils.parseBoolean("false"));
		assertFalse(RestParseUtils.parseBoolean(" false   "));
		assertFalse(RestParseUtils.parseBoolean("fAlSe"));
		assertFalse(RestParseUtils.parseBoolean(" FaLsE  "));
	}
	
	@Test
	public void testParseBooleanNull() {
		assertNull(RestParseUtils.parseBoolean(null));
		assertNull(RestParseUtils.parseBoolean(""));
		assertNull(RestParseUtils.parseBoolean("    "));
		assertNull(RestParseUtils.parseBoolean(" fanta "));
	}
	
	@Test
	public void testParseMillisToDate() {
		Date currentDate = new Date();
		long currentTime = currentDate.getTime();
		String currentTimeString = Long.toString(currentTime);
		
		assertEquals(currentDate, RestParseUtils.parseStringMillisToDate(currentTimeString));
		assertEquals(currentDate, RestParseUtils.parseStringMillisToDate("    " + currentTimeString + " "));
	}
	
	@Test
	public void testParseMillisToDateNull() {
		assertNull(RestParseUtils.parseStringMillisToDate(null));
		assertNull(RestParseUtils.parseStringMillisToDate(""));
		assertNull(RestParseUtils.parseStringMillisToDate("    "));
	}
	
	@Test
	public void testIsStringNullOrEmpty() {
		assertTrue(RestParseUtils.isStringNullOrEmpty(""));
		assertTrue(RestParseUtils.isStringNullOrEmpty("    "));
		assertTrue(RestParseUtils.isStringNullOrEmpty(null));
		assertFalse(RestParseUtils.isStringNullOrEmpty("asd"));
		assertFalse(RestParseUtils.isStringNullOrEmpty("  asd "));
	}

}
