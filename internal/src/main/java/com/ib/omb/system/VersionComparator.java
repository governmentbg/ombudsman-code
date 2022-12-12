package com.ib.omb.system;

import java.util.Comparator;

public class VersionComparator implements Comparator<String> {

	@Override
	public int compare(String s1, String s2) {
		if (s1 == null && s2 == null) {
			return 0;
		}
		
		if (s1 == null) {
			return 1;
		}
		
		if (s2 == null) {
			return -1;
		}
		
		String[] s1Arr = s1.split("\\.");
		String[] s2Arr = s2.split("\\.");
		
		//проверка да игнорираме разширението като string
		String last = s1Arr[s1Arr.length-1];		
		try {
			Integer.parseInt(last);
		} catch (NumberFormatException e) {
			s1Arr[s1Arr.length-1] = "0";
		} 
		last = s2Arr[s2Arr.length-1];
		try {
			Integer.parseInt(last);
		} catch (NumberFormatException e) {
			s2Arr[s2Arr.length-1] = "0";
		} 
		
		
		
		int compIndex = s1Arr.length;
		if (s2Arr.length < compIndex) {
			compIndex = s1Arr.length;
		}
		
		for (int i = 0; i < compIndex; i++) {
			String part1 = s1Arr[i];
			String part2 = s2Arr[i];
			
			
			Integer pInt1 = null;
			try {
				pInt1 = Integer.parseInt(part1);
			} catch (NumberFormatException e) {
				//остава си NULL;
			} 
			
			Integer pInt2 = null;
			try {
				pInt2 = Integer.parseInt(part2);
			} catch (NumberFormatException e) {
				//остава си NULL;
			} 
			
			if (pInt1 == null || pInt2 == null) {
				//return part1.compareTo(part2);
				Object[] mix1 = splitNumber(part1);
				Object[] mix2 = splitNumber(part2);
				
				if (mix1[0].equals(mix2[0])) {
					int c = ((String)mix1[1]).compareTo((String)mix2[1]);
					if (c != 0) {		
						return c;
					}else {
						continue;
					}
				}else {
					return ((Integer)mix1[0]).compareTo((Integer)mix2[0]);
				}
				
				
			}
			
//			if (pInt1 == null) {
//				return 1;
//			}
//			
//			if (pInt2 == null) {
//				return -1;
//			}
			
			if (! pInt1.equals(pInt2)) {
				return pInt1 - pInt2;
			}
			
		}
		
		return s1Arr.length - s2Arr.length;
		
		
	}
	
	
	private Object[] splitNumber(String part) {
		String numPart = "";
		String rest = "";
		boolean stillNum = true;
		for (int i = 0; i < part.length(); i++) {
			String tek = part.substring(i,i+1);
			if (stillNum && "0123456789".contains(tek)) {
				numPart+=tek;
			}else {
				stillNum = false;
				rest += tek;
				if (numPart.isEmpty()) {
					numPart = "9999";
				}
			}
			
		}
		
		Integer num = 0;
		try {
			num = Integer.parseInt(numPart);
		} catch (NumberFormatException e) {
			//Не би било възможно да се стигне до тук ....
		}
		
		Object[] res = {num,rest};
		//System.out.println("---------------> " + num + "|" + rest);
		return res;
		
		
	}

}
