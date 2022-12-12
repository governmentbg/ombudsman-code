package com.ib.omb.rest.mobileJalbi;

import java.util.Date;

import com.ib.omb.system.OmbConstants;
import com.ib.system.SysConstants;

/**
 * 
 * @author n.kanev
 *
 */
public class RestParseUtils {
	
	/**
	 * 
	 * @param s    a {@code String} containing the {@code int} representation to be parsed
     * @return     the integer value represented by the argument in decimal or {@code null} if the string is null or empty
     * 
     * @exception  NumberFormatException  if the string does not contain a parsable integer
	 */
	public static Integer parseInteger(String s) {
		if(s == null || s.trim().isEmpty()) {
			return null;
		}
		
		return Integer.parseInt(s.trim());
	}
	
	/**
	 * 
	 * @param s    a {@code String} containing the {@code Long} representation to be parsed
     * @return     the long value represented by the argument in decimal or {@code null} if the string is null or empty
     * 
     * @exception  NumberFormatException  if the string does not contain a parsable long
	 */
	public static Long parseLong(String s) {
		if(s == null || s.trim().isEmpty()) {
			return null;
		}
		
		return Long.parseLong(s.trim());
	}
	
	/**
	 * 
	 * @param s		a {@code String} containing the value to be parsed
     * @return
     * <ul>
     * 		<li>{@code true} if s is "true"</li>
     * 		<li>{@code false} if s is "false"</li>
     * 		<li>{@code null} if s is null or not "true"/"false"</li>
     * </ul>
	 */
	public static Boolean parseBoolean(String s) {
		if(s == null || s.trim().isEmpty()) {
			return null;
		}
		
		if(s.trim().equalsIgnoreCase("true")) return Boolean.TRUE;
		else if(s.trim().equalsIgnoreCase("false")) return Boolean.FALSE;
		else return null;
	}
	
	/**
	 * 
	 * @param s		a {@code String} containing the value to be parsed
     * @return
     * <ul>
     * 		<li>{@link OmbConstants#CODE_ZNACHENIE_DA} if s is "true"</li>
     * 		<li>{@link OmbConstants#CODE_ZNACHENIE_NE} if s is "false"</li>
     * 		<li>{@code null} if s is null or not "true"/"false"</li>
     * </ul>
	 */
	public static Integer parseBooleanToYesNo(String s) {
		if(s == null || s.trim().isEmpty()) {
			return null;
		}
		
		if(s.trim().equalsIgnoreCase("true")) return OmbConstants.CODE_ZNACHENIE_DA;
		else if(s.trim().equalsIgnoreCase("false")) return OmbConstants.CODE_ZNACHENIE_NE;
		else return null;
	}
	
	/**
	 * 
	 * @param s		a {@code String} containing milliseconds as long to be parsed
	 * @return
	 * <ul>
	 * 		<li>the date represented by the milliseconds</li>
	 * 		<li>{@code null} if s is null or empty</li>
	 * </ul>
	 * 
	 * @exception  NumberFormatException  if the string does not contain a parsable long.
	 */
	public static Date parseStringMillisToDate(String s) {
		if(s == null || s.trim().isEmpty()) {
			return null;
		}
		
		return new Date(parseLong(s));
	}
	
	/**
	 * 
	 * @param s a string
	 * @return true if s is null or an empty string (length is 0), false otherwise
	 */
	public static boolean isStringNullOrEmpty(String s) {
		return s == null || s.trim().isEmpty();
	}
}
