package com.ib.omb.db.dto;

import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlTransient;

import com.ib.indexui.system.Constants;
import com.ib.omb.system.OmbConstants;
import com.ib.system.SysConstants;
import com.ib.system.db.JournalAttr;
import com.ib.system.db.PersistentEntity;

/**
 * Жалбата
 *
 * @author belev
 */
@Entity
@Table(name = "doc_jalba")
public class DocJalba implements PersistentEntity {

	/**  */
	private static final long serialVersionUID = 9057886179627978234L;

	@Id
	@Column(name = "doc_id", unique = true, nullable = false)
	private Integer id; // съвпада с ID от таблица DOC

	@JournalAttr(label = "jbpType", defaultText = "Жалбоподател", classifID = "" + OmbConstants.CODE_CLASSIF_REF_TYPE)
	@Column(name = "jbp_type")
	private Integer jbpType;

	@JournalAttr(label = "jbpGrj", defaultText = "Гражданство", classifID = "" + Constants.CODE_CLASSIF_COUNTRIES)
	@Column(name = "jbp_grj")
	private Integer jbpGrj;

	@JournalAttr(label = "jbpName", defaultText = "Имена/Наименование")
	@Column(name = "jbp_name")
	private String jbpName;

	@JournalAttr(label = "jbpEgn", defaultText = "ЕГН")
	@Column(name = "jbp_egn")
	private String jbpEgn;

	@JournalAttr(label = "jbpLnc", defaultText = "ЛНЧ")
	@Column(name = "jbp_lnc")
	private String jbpLnc;

	@JournalAttr(label = "jbpPol", defaultText = "Пол", classifID = "" + OmbConstants.CODE_CLASSIF_POL)
	@Column(name = "jbp_pol")
	private Integer jbpPol;

	@JournalAttr(label = "jbpAge", defaultText = "Възраст")
	@Column(name = "jbp_age")
	private Integer jbpAge;

	@JournalAttr(label = "jbpEik", defaultText = "ЕИК")
	@Column(name = "jbp_eik")
	private String jbpEik;

	@JournalAttr(label = "jbpEkatte", defaultText = "Населено място", classifID = "" + SysConstants.CODE_CLASSIF_EKATTE)
	@Column(name = "jbp_ekatte")
	private Integer jbpEkatte;

	@JournalAttr(label = "jbpPost", defaultText = "Пощенски код")
	@Column(name = "jbp_post")
	private String jbpPost;

	@JournalAttr(label = "jbpAddr", defaultText = "Адрес")
	@Column(name = "jbp_addr")
	private String jbpAddr;

	@JournalAttr(label = "jbpPhone", defaultText = "Телефон")
	@Column(name = "jbp_phone")
	private String jbpPhone;

	@JournalAttr(label = "jbpEmail", defaultText = "е-мейл")
	@Column(name = "jbp_email")
	private String jbpEmail;

	@JournalAttr(label = "jbpHidden", defaultText = "Запазена самоличност", classifID = "" + SysConstants.CODE_CLASSIF_DANE)
	@Column(name = "jbp_hidden")
	private Integer jbpHidden;

	@JournalAttr(label = "dateNar", defaultText = "Дата на извършване на нарушението")
	@Column(name = "date_nar")
	private Date dateNar;

	@JournalAttr(label = "subjectNar", defaultText = "Орган/лице, срещу което се подава жалба")
	@Column(name = "subject_nar")
	private String subjectNar;

	@JournalAttr(label = "codeNar", defaultText = "Наименование на нарушител", classifID = "" + Constants.CODE_CLASSIF_REFERENTS)
	@Column(name = "code_nar")
	private Integer codeNar;

	@JournalAttr(label = "zasPrava", defaultText = "Засегнати права", classifID = "" + OmbConstants.CODE_CLASSIF_ZAS_PRAVA)
	@Column(name = "zas_prava")
	private Integer zasPrava;

	@JournalAttr(label = "vidOpl", defaultText = "Вид оплакване", classifID = "" + OmbConstants.CODE_CLASSIF_VID_OPL)
	@Column(name = "vid_opl")
	private Integer vidOpl;

	@JournalAttr(label = "jalbaText", defaultText = "Описание от жалбоподателя")
	@Column(name = "jalba_text")
	private String jalbaText;

	@JournalAttr(label = "requestText", defaultText = "Конкретно искане")
	@Column(name = "request_text")
	private String requestText;

	@JournalAttr(label = "registComment", defaultText = "Коментар при завеждане")
	@Column(name = "regist_comment")
	private String registComment;

	@JournalAttr(label = "codeZveno", defaultText = "Звено, към което е разпределена жалбата", classifID = "" + Constants.CODE_CLASSIF_ADMIN_STR)
	@Column(name = "code_zveno")
	private Integer codeZveno;

	@JournalAttr(label = "codeExpert", defaultText = "Водещ експерт", classifID = "" + Constants.CODE_CLASSIF_ADMIN_STR)
	@Column(name = "code_expert")
	private Integer codeExpert;

	@Transient
	@JournalAttr(label = "dopExperts", defaultText = "Допълнителен експерт", classifID = "" + Constants.CODE_CLASSIF_ADMIN_STR)
	private List<Integer>			dopExpertCodes;
	@Transient
	private transient List<Integer>	dbDopExpertCodes;

	@JournalAttr(label = "sast", defaultText = "Състояние", classifID = "" + OmbConstants.CODE_CLASSIF_JALBA_SAST)
	@Column(name = "sast")
	private Integer sast;

	@JournalAttr(label = "sastDate", defaultText = "Дата на състоянието", dateMask = "dd.MM.yyyy HH:mm:ss")
	@Column(name = "sast_date")
	private Date sastDate;

	@JournalAttr(label = "srok", defaultText = "Срок за разглеждане")
	@Column(name = "srok")
	private Date srok;

	@JournalAttr(label = "dopust", defaultText = "Допустимост", classifID = "" + OmbConstants.CODE_CLASSIF_DOPUST)
	@Column(name = "dopust")
	private Integer dopust;

	@JournalAttr(label = "osnNedopust", defaultText = "Основание за недопустимост", classifID = "" + OmbConstants.CODE_CLASSIF_OSN_NEDOPUST)
	@Column(name = "osn_nedopust")
	private Integer osnNedopust;

	@JournalAttr(label = "publicVisible", defaultText = "Видима в публичния регистър", classifID = "" + SysConstants.CODE_CLASSIF_DANE)
	@Column(name = "public_visible")
	private Integer publicVisible;

	@JournalAttr(label = "finMethod", defaultText = "Начин на финализиране", classifID = "" + OmbConstants.CODE_CLASSIF_JALBA_FIN)
	@Column(name = "fin_method")
	private Integer finMethod;

	@JournalAttr(label = "jalbaResultList", defaultText = "Резултат")
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	@JoinColumn(name = "doc_id", referencedColumnName = "doc_id", nullable = false)
	private List<DocJalbaResult> jalbaResultList;

	@Column(name = "submit_method")
	@JournalAttr(label = "submitMethod", defaultText = "Начин на подаване", classifID = "" + OmbConstants.CODE_CLASSIF_SUBMIT_METHOD)
	private Integer submitMethod;

	@Column(name = "submit_date")
	@JournalAttr(label = "submitDate", defaultText = "Дата на подаване")
	private Date submitDate;

	@JournalAttr(label = "corruption", defaultText = "Свързана с корупция", classifID = "" + SysConstants.CODE_CLASSIF_DANE)
	@Column(name = "corruption")
	private Integer corruption;

	@JournalAttr(label = "instCheck", defaultText = "Описаният проблем разглеждан ли е от други институции", classifID = "" + SysConstants.CODE_CLASSIF_DANE)
	@Column(name = "inst_check")
	private Integer instCheck;

	@JournalAttr(label = "instNames", defaultText = "Институции, разглеждали проблема")
	@Column(name = "inst_names")
	private String instNames;

	@Column(name = "user_last_mod")
	private Integer userLastMod;

	@Column(name = "date_last_mod")
	private Date dateLastMod;

	@Transient
	private Integer dbSast;

	@Transient
	private Integer	dbCodeExpert;
	@Transient
	private Integer	dbCodeZveno;

	@Transient
	private Boolean deloStatusChanged; // когато статуса на жалбата влияе на статуса на преписката, ще се направи true

	/** при вдигане на флага, ще се приключват и породените жалби */
	@Transient
	private Boolean completePorodeni;

	/**  */
	public DocJalba() {
		super();
	}

	/** @return the codeExpert */
	public Integer getCodeExpert() {
		return this.codeExpert;
	}

	/** */
	@Override
	public Integer getCodeMainObject() {
		return null; // запис в контекста на документ
	}

	/** @return the codeNar */
	public Integer getCodeNar() {
		return this.codeNar;
	}

	/** @return the codeZveno */
	public Integer getCodeZveno() {
		return this.codeZveno;
	}

	/** @return the completePorodeni */
	public Boolean getCompletePorodeni() {
		return this.completePorodeni;
	}

	/** @return the corruption */
	public Integer getCorruption() {
		return this.corruption;
	}

	/** @return the dateLastMod */
	public Date getDateLastMod() {
		return this.dateLastMod;
	}

	/** @return the dateNar */
	public Date getDateNar() {
		return this.dateNar;
	}

	/** @return the dbCodeExpert */
	public Integer getDbCodeExpert() {
		return this.dbCodeExpert;
	}

	/** @return the dbCodeZveno */
	public Integer getDbCodeZveno() {
		return this.dbCodeZveno;
	}

	/** @return the dbDopExpertCodes */
	@XmlTransient
	public List<Integer> getDbDopExpertCodes() {
		return this.dbDopExpertCodes;
	}

	/** @return the dbSast */
	public Integer getDbSast() {
		return this.dbSast;
	}

	/** @return the deloStatusChanged */
	public Boolean getDeloStatusChanged() {
		return this.deloStatusChanged;
	}

	/** @return the dopExpertCodes */
	public List<Integer> getDopExpertCodes() {
		return this.dopExpertCodes;
	}

	/** @return the dopust */
	public Integer getDopust() {
		return this.dopust;
	}

	/** @return the finMethod */
	public Integer getFinMethod() {
		return this.finMethod;
	}

	/** */
	@Override
	public Integer getId() {
		return this.id;
	}

	/** @return the instCheck */
	public Integer getInstCheck() {
		return this.instCheck;
	}

	/** @return the instNames */
	public String getInstNames() {
		return this.instNames;
	}

	/** @return the jalbaResultList */
	public List<DocJalbaResult> getJalbaResultList() {
		return this.jalbaResultList;
	}

	/** @return the jalbaText */
	public String getJalbaText() {
		return this.jalbaText;
	}

	/** @return the jbpAddr */
	public String getJbpAddr() {
		return this.jbpAddr;
	}

	/** @return the jbpAge */
	public Integer getJbpAge() {
		return this.jbpAge;
	}

	/** @return the jbpEgn */
	public String getJbpEgn() {
		return this.jbpEgn;
	}

	/** @return the jbpEik */
	public String getJbpEik() {
		return this.jbpEik;
	}

	/** @return the jbpEkatte */
	public Integer getJbpEkatte() {
		return this.jbpEkatte;
	}

	/** @return the jbpEmail */
	public String getJbpEmail() {
		return this.jbpEmail;
	}

	/** @return the jbpGrj */
	public Integer getJbpGrj() {
		return this.jbpGrj;
	}

	/** @return the jbpHidden */
	public Integer getJbpHidden() {
		return this.jbpHidden;
	}

	/** @return the jbpLnc */
	public String getJbpLnc() {
		return this.jbpLnc;
	}

	/** @return the jbpName */
	public String getJbpName() {
		return this.jbpName;
	}

	/** @return the jbpPhone */
	public String getJbpPhone() {
		return this.jbpPhone;
	}

	/** @return the jbpPol */
	public Integer getJbpPol() {
		return this.jbpPol;
	}

	/** @return the jbpPost */
	public String getJbpPost() {
		return this.jbpPost;
	}

	/** @return the jbpType */
	public Integer getJbpType() {
		return this.jbpType;
	}

	/** @return the osnNedopust */
	public Integer getOsnNedopust() {
		return this.osnNedopust;
	}

	/** @return the publicVisible */
	public Integer getPublicVisible() {
		return this.publicVisible;
	}

	/** @return the registComment */
	public String getRegistComment() {
		return this.registComment;
	}

	/** @return the requestText */
	public String getRequestText() {
		return this.requestText;
	}

	/** @return the sast */
	public Integer getSast() {
		return this.sast;
	}

	/** @return the sastDate */
	public Date getSastDate() {
		return this.sastDate;
	}

	/** @return the srok */
	public Date getSrok() {
		return this.srok;
	}

	/** @return the subjectNar */
	public String getSubjectNar() {
		return this.subjectNar;
	}

	/** @return the submitDate */
	public Date getSubmitDate() {
		return this.submitDate;
	}

	/** @return the submitMethod */
	public Integer getSubmitMethod() {
		return this.submitMethod;
	}

	/** @return the userLastMod */
	public Integer getUserLastMod() {
		return this.userLastMod;
	}

	/** @return the vidOpl */
	public Integer getVidOpl() {
		return this.vidOpl;
	}

	/** @return the zasPrava */
	public Integer getZasPrava() {
		return this.zasPrava;
	}

	/** */
	@Override
	public boolean isAuditable() {
		return false; // запис в контекста на документ
	}

	/** @param codeExpert the codeExpert to set */
	public void setCodeExpert(Integer codeExpert) {
		this.codeExpert = codeExpert;
	}

	/** @param codeNar the codeNar to set */
	public void setCodeNar(Integer codeNar) {
		this.codeNar = codeNar;
	}

	/** @param codeZveno the codeZveno to set */
	public void setCodeZveno(Integer codeZveno) {
		this.codeZveno = codeZveno;
	}

	/** @param completePorodeni the completePorodeni to set */
	public void setCompletePorodeni(Boolean completePorodeni) {
		this.completePorodeni = completePorodeni;
	}

	/** @param corruption the corruption to set */
	public void setCorruption(Integer corruption) {
		this.corruption = corruption;
	}

	/** @param dateLastMod the dateLastMod to set */
	@Override
	public void setDateLastMod(Date dateLastMod) {
		this.dateLastMod = dateLastMod;
	}

	/** @param dateNar the dateNar to set */
	public void setDateNar(Date dateNar) {
		this.dateNar = dateNar;
	}

	/** @param dbCodeExpert the dbCodeExpert to set */
	public void setDbCodeExpert(Integer dbCodeExpert) {
		this.dbCodeExpert = dbCodeExpert;
	}

	/** @param dbCodeZveno the dbCodeZveno to set */
	public void setDbCodeZveno(Integer dbCodeZveno) {
		this.dbCodeZveno = dbCodeZveno;
	}

	/** @param dbDopExpertCodes the dbDopExpertCodes to set */
	public void setDbDopExpertCodes(List<Integer> dbDopExpertCodes) {
		this.dbDopExpertCodes = dbDopExpertCodes;
	}

	/** @param dbSast the dbSast to set */
	public void setDbSast(Integer dbSast) {
		this.dbSast = dbSast;
	}

	/** @param deloStatusChanged the deloStatusChanged to set */
	public void setDeloStatusChanged(Boolean deloStatusChanged) {
		this.deloStatusChanged = deloStatusChanged;
	}

	/** @param dopExpertCodes the dopExpertCodes to set */
	public void setDopExpertCodes(List<Integer> dopExpertCodes) {
		this.dopExpertCodes = dopExpertCodes;
	}

	/** @param dopust the dopust to set */
	public void setDopust(Integer dopust) {
		this.dopust = dopust;
	}

	/** @param finMethod the finMethod to set */
	public void setFinMethod(Integer finMethod) {
		this.finMethod = finMethod;
	}

	/** @param id the id to set */
	public void setId(Integer id) {
		this.id = id;
	}

	/** @param instCheck the instCheck to set */
	public void setInstCheck(Integer instCheck) {
		this.instCheck = instCheck;
	}

	/** @param instNames the instNames to set */
	public void setInstNames(String instNames) {
		this.instNames = instNames;
	}

	/** @param jalbaResultList the jalbaResultList to set */
	public void setJalbaResultList(List<DocJalbaResult> jalbaResultList) {
		this.jalbaResultList = jalbaResultList;
	}

	/** @param jalbaText the jalbaText to set */
	public void setJalbaText(String jalbaText) {
		this.jalbaText = jalbaText;
	}

	/** @param jbpAddr the jbpAddr to set */
	public void setJbpAddr(String jbpAddr) {
		this.jbpAddr = jbpAddr;
	}

	/** @param jbpAge the jbpAge to set */
	public void setJbpAge(Integer jbpAge) {
		this.jbpAge = jbpAge;
	}

	/** @param jbpEgn the jbpEgn to set */
	public void setJbpEgn(String jbpEgn) {
		this.jbpEgn = jbpEgn;
	}

	/** @param jbpEik the jbpEik to set */
	public void setJbpEik(String jbpEik) {
		this.jbpEik = jbpEik;
	}

	/** @param jbpEkatte the jbpEkatte to set */
	public void setJbpEkatte(Integer jbpEkatte) {
		this.jbpEkatte = jbpEkatte;
	}

	/** @param jbpEmail the jbpEmail to set */
	public void setJbpEmail(String jbpEmail) {
		this.jbpEmail = jbpEmail;
	}

	/** @param jbpGrj the jbpGrj to set */
	public void setJbpGrj(Integer jbpGrj) {
		this.jbpGrj = jbpGrj;
	}

	/** @param jbpHidden the jbpHidden to set */
	public void setJbpHidden(Integer jbpHidden) {
		this.jbpHidden = jbpHidden;
	}

	/** @param jbpLnc the jbpLnc to set */
	public void setJbpLnc(String jbpLnc) {
		this.jbpLnc = jbpLnc;
	}

	/** @param jbpName the jbpName to set */
	public void setJbpName(String jbpName) {
		this.jbpName = jbpName;
	}

	/** @param jbpPhone the jbpPhone to set */
	public void setJbpPhone(String jbpPhone) {
		this.jbpPhone = jbpPhone;
	}

	/** @param jbpPol the jbpPol to set */
	public void setJbpPol(Integer jbpPol) {
		this.jbpPol = jbpPol;
	}

	/** @param jbpPost the jbpPost to set */
	public void setJbpPost(String jbpPost) {
		this.jbpPost = jbpPost;
	}

	/** @param jbpType the jbpType to set */
	public void setJbpType(Integer jbpType) {
		this.jbpType = jbpType;
	}

	/** @param osnNedopust the osnNedopust to set */
	public void setOsnNedopust(Integer osnNedopust) {
		this.osnNedopust = osnNedopust;
	}

	/** @param publicVisible the publicVisible to set */
	public void setPublicVisible(Integer publicVisible) {
		this.publicVisible = publicVisible;
	}

	/** @param registComment the registComment to set */
	public void setRegistComment(String registComment) {
		this.registComment = registComment;
	}

	/** @param requestText the requestText to set */
	public void setRequestText(String requestText) {
		this.requestText = requestText;
	}

	/** @param sast the sast to set */
	public void setSast(Integer sast) {
		this.sast = sast;
	}

	/** @param sastDate the sastDate to set */
	public void setSastDate(Date sastDate) {
		this.sastDate = sastDate;
	}

	/** @param srok the srok to set */
	public void setSrok(Date srok) {
		this.srok = srok;
	}

	/** @param subjectNar the subjectNar to set */
	public void setSubjectNar(String subjectNar) {
		this.subjectNar = subjectNar;
	}

	/** @param submitDate the submitDate to set */
	public void setSubmitDate(Date submitDate) {
		this.submitDate = submitDate;
	}

	/** @param submitMethod the submitMethod to set */
	public void setSubmitMethod(Integer submitMethod) {
		this.submitMethod = submitMethod;
	}

	/** @param userLastMod the userLastMod to set */
	@Override
	public void setUserLastMod(Integer userLastMod) {
		this.userLastMod = userLastMod;
	}

	/** @param vidOpl the vidOpl to set */
	public void setVidOpl(Integer vidOpl) {
		this.vidOpl = vidOpl;
	}

	/** @param zasPrava the zasPrava to set */
	public void setZasPrava(Integer zasPrava) {
		this.zasPrava = zasPrava;
	}
}
