package com.ib.omb.search;

import static com.ib.system.utils.SearchUtils.trimToNULL_Upper;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ib.omb.system.UserData;
import com.ib.system.BaseUserData;
import com.ib.system.db.DialectConstructor;
import com.ib.system.db.JPA;
import com.ib.system.db.SelectMetadata;
import com.ib.system.utils.DateUtils;

/**  @author yonchev 
 * За справката за дело/преписка
 */
public class DeloDvijSearch extends SelectMetadata {

	private static final long serialVersionUID = -4574083549819423701L;
	

	private Integer registraturaId;
	
	private Date datPredavaneOt;
	private Date datPredavaneDo;
	
	private List<Integer> predaden;
	private String predadenAsString;
	
	private Integer[] nachin;
	private Integer regPredaden;
	private Integer tome;
	private String dopInfo;
	
	
	private Date datStatusOt;
	private Date datStatusDo;
	
	private Integer[] status;
	private String dopInfoStatus;
	
	private Date datSrokOt;
	private Date datSrokDo;
	
	
	private Integer[] nachinVr;
	private String dopInfoVr;
	
	
	private Date datVrastaneOt;
	private Date datVrastaneDo;
	
	private List<Integer> varnat;
	private String varnatAsString;
	
	
	private Date datPrepOt;
	private Date datPrepDo;
	
	private Integer typePrep;
	
	private String rnPrep;
	private boolean rnPrepEQ = true;
	
	/**
	 * Винаги се търси в контекста на регистратура. Ако все пак се иска без регистратура да се подаде NULL в конструктора
	 *
	 * @param registraturaId
	 */
	public DeloDvijSearch(Integer registraturaId) {
		this.registraturaId = registraturaId;
	}
	
	
	/**
	 * Използва се от основния екран за търсене на задачи <br>
	 * На база входните параметри подготвя селект за получаване на резултат от вида: <br>
	 * [0]-RN_DELO<br>
	 * [1]-DELO_DATE<br>
	 * [2]-DOC_VID<br>
	 * [3]-OTNOSNO<br>
	 * [4]-DVIJ_DATE<br>
	 * [5]-DVIJ_METHOD<br>
	 * [6]-DVIJ_TEXT<br>	 * 
	 * [7]-DVIJ_INFO<br>
	 * [8]-STATUS	 * 
	 * [9]-STATUS_DATE<br>
	 * [10]-STATUS_TEXT<br>
	 * [11]-RETURN_TO_DATE<br>
	 * [12]-RETURN_DATE<br>
	 * [13]-RETURN_METHOD<br>
	 * [14]-RETURN_TEXT_REF<br>
	 * [15]-RETURN_INFO<br>
	 * [16]-TOM_NOMER<br>
	 * [17]-DELO_ID<br>
	 * [18]-ID<br>
	 * [19]-CODE_REF<br>
	 *
	 * @param userData това е този които изпълнява търсенето
	 * @param viewMode <code>true</code>-разглеждане, <code>false</code>-актуализация
	 */
	public void buildQueryDvijPrepList(BaseUserData userData) {
				

		Map<String, Object> params = new HashMap<>();

		StringBuilder select = new StringBuilder();
		StringBuilder from = new StringBuilder();
		StringBuilder where = new StringBuilder();
		
		String dialect = JPA.getUtil().getDbVendorName();
		
		select.append("select distinct d.RN_DELO a01, d.DELO_DATE a02, d.DELO_TYPE a03,");
		select.append(DialectConstructor.limitBigString(dialect, "d.DELO_NAME", 300) + " a04 "); // max 300!
		select.append(",dd.DVIJ_DATE a05, dd.DVIJ_METHOD a06, DVIJ_TEXT a07, dd.DVIJ_INFO a08, dd.STATUS a09, dd.STATUS_DATE a10, dd.STATUS_TEXT a11, RETURN_TO_DATE a12, RETURN_DATE a13, RETURN_METHOD a14, RETURN_TEXT_REF a15, RETURN_INFO a16,dd.TOM_NOMER a17, d.DELO_ID as a18 ");
		select.append(" , dd.ID a19,  dd.CODE_REF a20 ");
		from.append(" from delo_dvij dd join delo d on dd.DELO_ID = d.DELO_ID ");
		where.append(" where d.REGISTRATURA_ID = :REGID ");
		params.put("REGID", registraturaId);
		
		
		if (datPredavaneOt != null) {
			datPredavaneOt = DateUtils.startDate(datPredavaneOt);
			where.append(" and dd.DVIJ_DATE >= :datPredavaneOt ");
			params.put("datPredavaneOt", datPredavaneOt);
		}
		if (datStatusOt != null) {
			datStatusOt = DateUtils.startDate(datStatusOt);
			where.append(" and  dd.STATUS_DATE >= :datStatusOt ");
			params.put("datStatusOt", datStatusOt);
		}
		if (datSrokOt != null) {
			datSrokOt = DateUtils.startDate(datSrokOt);
			where.append(" and  dd.RETURN_TO_DATE >= :datSrokOt ");
			params.put("datSrokOt", datSrokOt);
		}
		if (datVrastaneOt != null) {
			datVrastaneOt = DateUtils.startDate(datVrastaneOt);
			where.append(" and  dd.RETURN_DATE >= :datVrastaneOt ");
			params.put("datVrastaneOt", datVrastaneOt);
		}
		if (datPrepOt != null) {
			datPrepOt = DateUtils.startDate(datPrepOt);
			where.append(" and  d.DELO_DATE >= :datDocOt ");
			params.put("datDocOt", datPrepOt);
		}

		
		if (datPredavaneDo != null) {
			datPredavaneDo = DateUtils.endDate(datPredavaneDo);
			where.append(" and  dd.DVIJ_DATE <= :datPredavaneDo ");
			params.put("datPredavaneDo", datPredavaneDo);
		}
		if (datStatusDo != null) {
			datStatusDo = DateUtils.endDate(datStatusDo);
			where.append(" and  dd.STATUS_DATE <= :datStatusDo ");
			params.put("datStatusDo", datStatusDo);
		}
		if (datSrokDo != null) {
			datSrokDo = DateUtils.endDate(datSrokDo);
			where.append(" and  dd.RETURN_TO_DATE <= :datSrokDo ");
			params.put("datSrokDo", datSrokDo);
		}
		if (datVrastaneDo != null) {
			datVrastaneDo = DateUtils.endDate(datVrastaneDo);
			where.append(" and  dd.RETURN_DATE <= :datVrastaneDo ");
			params.put("datVrastaneDo", datVrastaneDo);
		}
		if (datPrepDo != null) {
			datPrepDo = DateUtils.endDate(datPrepDo);
			where.append(" and  d.DELO_DATE <= :datDocDo ");
			params.put("datDocDo", datPrepDo);
		}
		
		
		String t = trimToNULL_Upper(this.rnPrep);
		if (t != null) {
			if (this.rnPrepEQ) { // пълно съвпадение case insensitive
				where.append(" and upper(d.RN_DELO) = :rnDelo ");
				params.put("rnDelo", t);

			} else {
				where.append(" and upper(d.RN_DELO) like :rnDelo ");
				params.put("rnDelo", "%" + t + "%");
			}
		}
		
		
		if (this.predaden != null  && this.predaden.size() > 0) {
			if (this.predaden.size() == 1) {
				where.append(" and dd.CODE_REF = :predaden ");
				params.put("predaden", this.predaden.get(0));
			}else {
				where.append(" and dd.CODE_REF in :predaden ");
				params.put("predaden", this.predaden);
			}
			
		}
		
		
		t = trimToNULL_Upper(this.predadenAsString);
		if (t != null) {			
			where.append(" and upper(dd.DVIJ_TEXT) like :predadenAsString ");
			params.put("predadenAsString", "%" + t + "%");			
		}
		
		
		if (this.tome != null) {			
			where.append(" and upper(dd.DVIJ_EMAIL) like :email ");
			params.put("email", "%" + t + "%");			
		}
		
		t = trimToNULL_Upper(this.dopInfo);
		if (t != null) {			
			where.append(" and upper(dd.DVIJ_INFO) like :dopInfo ");
			params.put("dopInfo", "%" + t + "%");			
		}
				
				
		if (this.nachin != null  && this.nachin.length > 0) {
			if (this.nachin.length == 1) {
				where.append(" and dd.DVIJ_METHOD = :nachin ");
				params.put("nachin", this.nachin[0]);
			}else {
				where.append(" and dd.DVIJ_METHOD in :nachin ");
				params.put("nachin",  Arrays.asList( this.nachin));
			}
			
		}
		
		
		
//		if (this.regPredaden != null) {
//			where.append(" and dd.FOR_REG_ID = :regPredaden ");
//			params.put("regPredaden", this.regPredaden);
//		}
//		
		
		if (this.status != null  && this.status.length > 0) {
			if (this.status.length == 1) {
				where.append(" and dd.STATUS = :status ");
				params.put("status", this.status[0]);
			}else {
				where.append(" and dd.STATUS in :status ");
				params.put("status", Arrays.asList(this.status));
			}
			
		}
		
		
		t = trimToNULL_Upper(this.dopInfoStatus);
		if (t != null) {			
			where.append(" and upper(dd.STATUS_TEXT) like :dopInfoStatus ");
			params.put("dopInfoStatus", "%" + t + "%");			
		}
		
			
		if (this.nachinVr != null  && this.nachinVr.length > 0) {
			if (this.nachinVr.length == 1) {
				where.append(" and dd.RETURN_METHOD = :nachinVr ");
				params.put("nachinVr", this.nachinVr[0]);
			}else {
				where.append(" and dd.RETURN_METHOD in :nachinVr ");
				params.put("nachinVr",  Arrays.asList(this.nachinVr));
			}
			
		}
		
		
		t = trimToNULL_Upper(this.dopInfoVr);
		if (t != null) {			
			where.append(" and upper(dd.RETURN_INFO) like :dopInfoVr ");
			params.put("dopInfoVr", "%" + t + "%");			
		}

		
		if (this.varnat != null  && this.varnat.size() > 0) {
			if (this.varnat.size() == 1) {
				where.append(" and dd.RETURN_CODE_REF = :varnat ");
				params.put("varnat", this.varnat.get(0));
			}else {
				where.append(" and dd.RETURN_CODE_REF in :varnat ");
				params.put("varnat", this.varnat);
			}
			
		}
		
		
		if (this.typePrep != null) {
			where.append(" and d.DELO_TYPE = :typePrep ");
			params.put("typePrep", this.typePrep);
		}
		
		t = trimToNULL_Upper(this.varnatAsString);
		if (t != null) {			
			where.append(" and upper(dd.RETURN_TEXT_REF) like :varnatAsString ");
			params.put("varnatAsString", "%" + t + "%");			
		}
		
		// така ще се приложи налагането на достъп, което се използва в справката за документи
		new DeloSearch(this.registraturaId).addAccessRules(where, from, params, (UserData) userData, true);

		
		
		setSqlCount(" select count(distinct dd.ID) " + from.toString() + where.toString()); // на този етап бройката е готова

		
		String queryStr=select.toString() + from.toString() + where.toString();
		setSql(queryStr);
		System.out.println("SQL String: "+ queryStr);

		setSqlParameters(params);
	}
	
	
	/*============================================================================== Getters& Setters=======================================*/

	public Integer getRegistraturaId() {
		return registraturaId;
	}

	public void setRegistraturaId(Integer registraturaId) {
		this.registraturaId = registraturaId;
	}

	public Date getDatPredavaneOt() {
		return datPredavaneOt;
	}

	public void setDatPredavaneOt(Date datPredavaneOt) {
		this.datPredavaneOt = datPredavaneOt;
	}

	public Date getDatPredavaneDo() {
		return datPredavaneDo;
	}

	public void setDatPredavaneDo(Date datPredavaneDo) {
		this.datPredavaneDo = datPredavaneDo;
	}

	public List<Integer> getPredaden() {
		return predaden;
	}

	public void setPredaden(List<Integer> predaden) {
		this.predaden = predaden;
	}

	public String getPredadenAsString() {
		return predadenAsString;
	}

	public void setPredadenAsString(String predadenAsString) {
		this.predadenAsString = predadenAsString;
	}

	public Integer[] getNachin() {
		return nachin;
	}

	public void setNachin(Integer[] nachin) {
		this.nachin = nachin;
	}

	public Integer getRegPredaden() {
		return regPredaden;
	}

	public void setRegPredaden(Integer regPredaden) {
		this.regPredaden = regPredaden;
	}

	public Integer getTome() {
		return tome;
	}

	public void setTome(Integer tome) {
		this.tome = tome;
	}

	public String getDopInfo() {
		return dopInfo;
	}

	public void setDopInfo(String dopInfo) {
		this.dopInfo = dopInfo;
	}

	public Date getDatStatusOt() {
		return datStatusOt;
	}

	public void setDatStatusOt(Date datStatusOt) {
		this.datStatusOt = datStatusOt;
	}

	public Date getDatStatusDo() {
		return datStatusDo;
	}

	public void setDatStatusDo(Date datStatusDo) {
		this.datStatusDo = datStatusDo;
	}

	public Integer[] getStatus() {
		return status;
	}

	public void setStatus(Integer[] status) {
		this.status = status;
	}

	public String getDopInfoStatus() {
		return dopInfoStatus;
	}

	public void setDopInfoStatus(String dopInfoStatus) {
		this.dopInfoStatus = dopInfoStatus;
	}

	public Date getDatSrokOt() {
		return datSrokOt;
	}

	public void setDatSrokOt(Date datSrokOt) {
		this.datSrokOt = datSrokOt;
	}

	public Date getDatSrokDo() {
		return datSrokDo;
	}

	public void setDatSrokDo(Date datSrokDo) {
		this.datSrokDo = datSrokDo;
	}

	public Integer[] getNachinVr() {
		return nachinVr;
	}

	public void setNachinVr(Integer[] nachinVr) {
		this.nachinVr = nachinVr;
	}

	public String getDopInfoVr() {
		return dopInfoVr;
	}

	public void setDopInfoVr(String dopInfoVr) {
		this.dopInfoVr = dopInfoVr;
	}

	public Date getDatVrastaneOt() {
		return datVrastaneOt;
	}

	public void setDatVrastaneOt(Date datVrastaneOt) {
		this.datVrastaneOt = datVrastaneOt;
	}

	public Date getDatVrastaneDo() {
		return datVrastaneDo;
	}

	public void setDatVrastaneDo(Date datVrastaneDo) {
		this.datVrastaneDo = datVrastaneDo;
	}

	public List<Integer> getVarnat() {
		return varnat;
	}

	public void setVarnat(List<Integer> varnat) {
		this.varnat = varnat;
	}

	public String getVarnatAsString() {
		return varnatAsString;
	}

	public void setVarnatAsString(String varnatAsString) {
		this.varnatAsString = varnatAsString;
	}

	public Date getDatPrepOt() {
		return datPrepOt;
	}

	public void setDatPrepOt(Date datPrepOt) {
		this.datPrepOt = datPrepOt;
	}

	public Date getDatPrepDo() {
		return datPrepDo;
	}

	public void setDatPrepDo(Date datPrepDo) {
		this.datPrepDo = datPrepDo;
	}

	public Integer getTypePrep() {
		return typePrep;
	}

	public void setTypePrep(Integer typePrep) {
		this.typePrep = typePrep;
	}

	public String getRnPrep() {
		return rnPrep;
	}

	public void setRnPrep(String rnPrep) {
		this.rnPrep = rnPrep;
	}

	public boolean isRnPrepEQ() {
		return rnPrepEQ;
	}

	public void setRnPrepEQ(boolean rnPrepEQ) {
		this.rnPrepEQ = rnPrepEQ;
	}

}
