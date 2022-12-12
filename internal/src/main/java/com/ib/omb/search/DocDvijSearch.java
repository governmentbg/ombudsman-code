package com.ib.omb.search;

import static com.ib.system.utils.SearchUtils.trimToNULL_Upper;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ib.omb.db.dao.DocDAO;
import com.ib.omb.system.UserData;
import com.ib.system.BaseUserData;
import com.ib.system.db.DialectConstructor;
import com.ib.system.db.JPA;
import com.ib.system.db.SelectMetadata;
import com.ib.system.utils.DateUtils;

/**
 * Търсене на движения на документи
 *
 * @author mamun
 */
public class DocDvijSearch extends SelectMetadata {

	private Integer registraturaId;
	
	private Date datPredavaneOt;
	private Date datPredavaneDo;
	
	private List<Integer> predaden;
	private String predadenAsString;
	
	private Integer[] nachin;
	private Integer regPredaden;
	private String email;
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
	
	
	private Date datDocOt;
	private Date datDocDo;
	
	private Integer vidDoc;
	private List<Integer>	docVidList;

	private String rnDoc;
	private boolean rnDocEQ = true;
	
	
	
	
	/**  */
	private static final long serialVersionUID = 6198203775464191186L;

	
	/**
	 * Винаги се търси в контекста на регистратура. Ако все пак се иска без регистратура да се подаде NULL в конструктора
	 *
	 * @param registraturaId
	 */
	public DocDvijSearch(Integer registraturaId) {
		this.registraturaId = registraturaId;
	}

	/**
	 * Използва се от основния екран за търсене на задачи <br>
	 * На база входните параметри подготвя селект за получаване на резултат от вида: <br>
	 * [0]-RN_DOC<br>
	 * [1]-DOC_DATE<br>
	 * [2]-DOC_VID<br>
	 * [3]-OTNOSNO<br>
	 * [4]-DVIJ_DATE<br>
	 * [5]-DVIJ_METHOD<br>
	 * [6]-DVIJ_TEXT<br>
	 * [7]-DVIJ_EMAIL<br>
	 * [8]-DVIJ_INFO<br>
	 * [9]-STATUS	 * 
	 * [10]-STATUS_DATE<br>
	 * [11]-STATUS_TEXT<br>
	 * [12]-RETURN_TO_DATE<br>
	 * [13]-RETURN_DATE<br>
	 * [14]-RETURN_METHOD<br>
	 * [15]-RETURN_TEXT_REF<br>
	 * [16]-RETURN_INFO<br>
	 * [17]-FOR_REG_ID<br>
	 * [18]-EKZ_NOMER<br>
	 * [19]-DOC_ID
     * [20]-ID
	 * [21]-CODE_REF
	 *
	 * @param userData това е този които изпълнява търсенето
	 * @param viewMode <code>true</code>-разглеждане, <code>false</code>-актуализация
	 */
	public void buildQueryDvijList(BaseUserData userData) {
				

		Map<String, Object> params = new HashMap<>();

		StringBuilder select = new StringBuilder();
		StringBuilder from = new StringBuilder();
		StringBuilder where = new StringBuilder();
		
		String dialect = JPA.getUtil().getDbVendorName();
	//	System.out.println("DbVendorName "+dialect);
		
		select.append("select distinct "+DocDAO.formRnDocSelect("d.", dialect)+" a01, d.DOC_DATE a02, d.DOC_VID a03,");
		select.append(DialectConstructor.limitBigString(dialect, "d.OTNOSNO", 300) + " a04 "); // max 300!
		select.append(",dd.DVIJ_DATE a05, dd.DVIJ_METHOD a06, DVIJ_TEXT a07, dd.DVIJ_EMAIL a08, dd.DVIJ_INFO a09, dd.STATUS a10, dd.STATUS_DATE a11,  ")
		.append(DialectConstructor.limitBigString(dialect,"dd.STATUS_TEXT",3990)+" a12,").append(" RETURN_TO_DATE a13, RETURN_DATE a14, RETURN_METHOD a15, RETURN_TEXT_REF a16, RETURN_INFO a17, dd.FOR_REG_ID   a18, EKZ_NOMER a19, d.DOC_ID a20  ");
		select.append(" , dd.ID a21, dd.CODE_REF a22 ");
		from.append(" from doc_dvij dd join doc d on dd.DOC_ID = d.DOC_ID ");
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
		if (datDocOt != null) {
			datDocOt = DateUtils.startDate(datDocOt);
			where.append(" and  d.DOC_DATE >= :datDocOt ");
			params.put("datDocOt", datDocOt);
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
		if (datDocDo != null) {
			datDocDo = DateUtils.endDate(datDocDo);
			where.append(" and  d.DOC_DATE <= :datDocDo ");
			params.put("datDocDo", datDocDo);
		}
		
		
		String t = trimToNULL_Upper(this.rnDoc);
		if (t != null) {
			if (this.rnDocEQ) { // пълно съвпадение case insensitive
				where.append(" and upper(d.RN_DOC) = :rnDoc ");
				params.put("rnDoc", t);

			} else {
				where.append(" and upper(d.RN_DOC) like :rnDoc ");
				params.put("rnDoc", "%" + t + "%");
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
		
		
		t = trimToNULL_Upper(this.email);
		if (t != null) {			
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
		
		
		
		if (this.regPredaden != null) {
			where.append(" and dd.FOR_REG_ID = :regPredaden ");
			params.put("regPredaden", this.regPredaden);
		}
		
		
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
		
		
		
		
		if (this.vidDoc != null) {
			where.append(" and d.DOC_VID = :vidDoc ");
			params.put("vidDoc", this.vidDoc);
		}
		if (this.docVidList != null && !this.docVidList.isEmpty()) {
			where.append(" and d.DOC_VID in (:docVidList) ");
			params.put("docVidList", this.docVidList);
		}

		t = trimToNULL_Upper(this.varnatAsString);
		if (t != null) {			
			where.append(" and upper(dd.RETURN_TEXT_REF) like :varnatAsString ");
			params.put("varnatAsString", "%" + t + "%");			
		}
		
		// така ще се приложи налагането на достъп, което се използва в справката за документи
		new DocSearch(this.registraturaId).addAccessRules(where, from, params, (UserData) userData, true);
		
		
		setSqlCount(" select count(distinct dd.ID) " + from.toString() + where.toString()); // на този етап бройката е готова

		
		String queryStr=select.toString() + from.toString() + where.toString();
		setSql(queryStr);
	//	System.out.println("SQL String: "+ queryStr);

		setSqlParameters(params);
	}

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

	

	public String getPredadenAsString() {
		return predadenAsString;
	}

	public void setPredadenAsString(String predadenAsString) {
		this.predadenAsString = predadenAsString;
	}

	
	public Integer getRegPredaden() {
		return regPredaden;
	}

	public void setRegPredaden(Integer regPredaden) {
		this.regPredaden = regPredaden;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
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

	

	public String getVarnatAsString() {
		return varnatAsString;
	}

	public void setVarnatAsString(String varnatAsString) {
		this.varnatAsString = varnatAsString;
	}

	public Date getDatDocOt() {
		return datDocOt;
	}

	public void setDatDocOt(Date datDocOt) {
		this.datDocOt = datDocOt;
	}

	public Date getDatDocDo() {
		return datDocDo;
	}

	public void setDatDocDo(Date datDocDo) {
		this.datDocDo = datDocDo;
	}

	public Integer getVidDoc() {
		return vidDoc;
	}

	public void setVidDoc(Integer vidDoc) {
		this.vidDoc = vidDoc;
	}

	public String getRnDoc() {
		return rnDoc;
	}

	public void setRnDoc(String rnDoc) {
		this.rnDoc = rnDoc;
	}

	public boolean isRnDocEQ() {
		return rnDocEQ;
	}

	public void setRnDocEQ(boolean rnDocEQ) {
		this.rnDocEQ = rnDocEQ;
	}

	public List<Integer> getPredaden() {
		return predaden;
	}

	public void setPredaden(List<Integer> predaden) {
		this.predaden = predaden;
	}

	

	public Integer[] getNachin() {
		return nachin;
	}

	public void setNachin(Integer[] nachin) {
		this.nachin = nachin;
	}

	public Integer[] getStatus() {
		return status;
	}

	public void setStatus(Integer[] status) {
		this.status = status;
	}

	public Integer[] getNachinVr() {
		return nachinVr;
	}

	public void setNachinVr(Integer[] nachinVr) {
		this.nachinVr = nachinVr;
	}

	public List<Integer> getVarnat() {
		return varnat;
	}

	public void setVarnat(List<Integer> varnat) {
		this.varnat = varnat;
	}

	public List<Integer> getDocVidList() {
		return this.docVidList;
	}
	public void setDocVidList(List<Integer> docVidList) {
		this.docVidList = docVidList;
	}
	
}
