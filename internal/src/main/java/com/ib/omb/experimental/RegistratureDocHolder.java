package com.ib.omb.experimental;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**Да държи общи неща, които деловодителките от една регистратура трябва да виждат
 * @author krasi
 *
 */

public class RegistratureDocHolder {
	
	
	public RegistratureDocHolder() {
		super();
//		counterEmailMap.put("MamunBox", 11);
//		counterEmailMap.put("IndexBox", 11);
		
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(RegistratureDocHolder.class);
	
	
	//private Integer counterEmails = 0;
	
	private Map<String, Integer> counterEmailMap = new HashMap<String, Integer>();
	
	
	private Integer counterSEOS = 0;
	private Integer counterEDelivery = 0;
	
	private Integer counterNasochvane = 0;
	private Integer counterOfficial = 0;
	private Integer counterOtherReg = 0;
	private Integer counterForCompetence = 0;
	
	
	
	public boolean regHasChange = false;
	
	
//	public Integer getCounterEmails() {
//		return counterEmails;
//	}
	
	public Integer getCounterSEOS() {
		return counterSEOS;
	}
	
	public Integer getCounterEDelivery() {
		return counterEDelivery;
	}
	
	public Integer getCounterNasochvane() {
		return counterNasochvane;
	}
	
	public Integer getCounterOfficial() {
		return counterOfficial;
	}
	
	public Integer getCounterOtherReg() {
		return counterOtherReg;
	}
	
	public boolean setCounterOtherReg(Integer counterOtherReg) {
		boolean hasChange = false;
		if (this.counterOtherReg.intValue() != counterOtherReg.intValue()) {
			hasChange = true;
			regHasChange = regHasChange || hasChange;
		}
		this.counterOtherReg = counterOtherReg;
		return hasChange;
	}
	
	public boolean setCounterNasochvane(Integer counterNasochvane) {
		boolean hasChange = false;
		if (this.counterNasochvane.intValue() != counterNasochvane.intValue()) {
			hasChange = true;
			regHasChange = regHasChange || hasChange;
			
		}
		this.counterNasochvane = counterNasochvane;
		return hasChange;
	}
	
	public boolean setCounterEDelivery(Integer counterEDelivery) {
		boolean hasChange = false;
		
		LOGGER.debug(">>>>>> IN this.counterEDelivery = "+this.counterEDelivery);
		LOGGER.debug(">>>>>> IN counterEDelivery = "+counterEDelivery);
		
		if (this.counterEDelivery.intValue() != counterEDelivery.intValue()) {
			LOGGER.debug(this.counterEDelivery.intValue() + " <> " + counterEDelivery.intValue());
			hasChange = true;
			regHasChange = regHasChange || hasChange;
		}
		this.counterEDelivery = counterEDelivery;
		LOGGER.debug("<<<<< OUT this.counterEDelivery = "+this.counterEDelivery);
		return hasChange;
	}
	
	public boolean setCounterSEOS(Integer counterSEOS) {
		boolean hasChange = false;
		
		LOGGER.debug(">>>>>> IN this.counterSEOS = "+this.counterSEOS);
		LOGGER.debug(">>>>>> IN counterSEOS = "+counterSEOS);
		
		if (this.counterSEOS.intValue() != counterSEOS.intValue()) {
			LOGGER.debug(this.counterSEOS.intValue() + " <> " + counterSEOS.intValue());
			hasChange = true;
			regHasChange = regHasChange || hasChange;
		}
		this.counterSEOS = counterSEOS;
		LOGGER.debug("<<<<< OUT this.counterSEOS = "+this.counterSEOS);
		return hasChange;
	}
	
//	public boolean setCounterEmails(Integer counterEmails) {
//		boolean hasChange = false;
//		if (this.counterEmails.intValue() != counterEmails.intValue()) {
//			hasChange = true;
//			regHasChange = regHasChange || hasChange;
//		}
//		this.counterEmails = counterEmails;
//		return hasChange;
//	}
	
	public boolean setCounterOfficial(Integer counterOfficial) {
		boolean hasChange = false;
		if (this.counterOfficial.intValue() != counterOfficial.intValue()) {
			hasChange = true;
			regHasChange = regHasChange || hasChange;
		}
		this.counterOfficial = counterOfficial;
		return hasChange;
	}
	
	
	
	public Integer getCouterEmails(String mailBoxName) {
		Integer cnt = counterEmailMap.get(mailBoxName);
		if (cnt == null) {
			return -1;
		}else {
			return cnt;
		}
	}
	
	public boolean setCounterEmails(String mailBoxName, Integer counterEmails) {
		boolean hasChange = false;
		
		Integer cnt = getCouterEmails(mailBoxName);
		LOGGER.debug("********************************* IF " + cnt + " == " + counterEmails );
		if (cnt.intValue() != counterEmails.intValue()) {
			hasChange = true;
			regHasChange = regHasChange || hasChange;
		}
		counterEmailMap.put(mailBoxName, counterEmails);
		return hasChange;
	}
	
	
	
	public Set<String> getAllBoxes() {
		return counterEmailMap.keySet();
	}

	public Integer getCounterForCompetence() {
		return counterForCompetence;
	}

	public boolean setCounterForCompetence(Integer counterForCompetence) {
		boolean hasChange = false;
		if (this.counterForCompetence.intValue() != counterForCompetence.intValue()) {
			hasChange = true;
			regHasChange = regHasChange || hasChange;
			
		}
		this.counterForCompetence = counterForCompetence;
		return hasChange;
	}
	
	

}
