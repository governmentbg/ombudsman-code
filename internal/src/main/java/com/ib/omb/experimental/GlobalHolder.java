package com.ib.omb.experimental;

import java.util.HashMap;
import java.util.Map.Entry;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import java.util.Set;

/**
 * Държи нещата необходими за частта Деловодство от работния плот.
 * Дреме в ApplicationScope-to дефиниран в faces-config
 * 
 * @author vassil
 *
 */

public class GlobalHolder {
	
	
	private HashMap<Integer, RegistratureDocHolder> regMap = new HashMap<Integer, RegistratureDocHolder>();

	public HashMap<Integer, RegistratureDocHolder> getRegMap() {
		return regMap;
	}

	public RegistratureDocHolder getRegInfo(Integer registratureId) {
		
		RegistratureDocHolder regHolder = regMap.get(registratureId);
		if (regHolder == null) {
			regHolder = new RegistratureDocHolder();
		}
		return regHolder;
	}

	public void setRegInfo(Integer registratureId , RegistratureDocHolder docHolder) {
		regMap.put(registratureId,docHolder );
	}
	
	
	public void clearChangeFlag() {
		
		Set<Entry<Integer, RegistratureDocHolder>> all = regMap.entrySet();
		for (Entry<Integer, RegistratureDocHolder> entry : all) {
			entry.getValue().regHasChange = false;
		}
		
	}

}
