package com.ib.omb.rest.mobileJalbi.v1;

import java.util.ArrayList;
import java.util.List;

public class JalbaJson {
	private Integer persType;       // тип JBP_TYPE
	private String  name;           // имена/наименование JBP_NAME
	private String  egn;            // ЕГН JBP_EGN
	private String  lnch;           // ЛНЧ JBP_LNC
	private String  eik;            // ЕИК JBP_EIK
	private Integer age;            // възраст JBP_AGE
	private Integer sex;            // пол JBP_POL
	private Integer nation;         // гражданство JBP_GRJ
	private Integer city;           // населено място JBP_EKATTE
	private String  pCode;          // пощенски код JBP_POST
	private String  address;        // адрес JBP_ADDR
	private String  phone;          // телефон JBP_PHONE
	private String  email;          // е-мейл JBP_EMAIL
	private Boolean protect;        // запазена самоличност JBP_HIDDEN
	private Long    date;           // дата на извършване на нарушение DATE_NAR
	private String  defendant;      // орган/лице, срещу което се подава жалба SUBJECT_NAR
	private Integer rights;         // засегнати права ZAS_PRAVA
	private Integer vid;            // вид оплакване VID_OPL
	private String  descr;          // описание от жалбоподателя JALBA_TEXT
	private String  request;        // конкретно искане REQUEST_TEXT
	private Boolean considered;     // разглеждан ли е от други институции INST_CHECK
	private String  consideredBy;   // институции, разглеждали проблема INST_NAMES
	private Integer answer;         // предоставяне на отговора DELIVERY_METHOD

	private List<JalbaFile> files;	//прикачени файлове и снимки
	
	public JalbaJson() {
		files = new ArrayList<>();
	}

	public Integer getPersType() {
		return persType;
	}

	public void setPersType(Integer persType) {
		this.persType = persType;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEgn() {
		return egn;
	}

	public void setEgn(String egn) {
		this.egn = egn;
	}

	public String getLnch() {
		return lnch;
	}

	public void setLnch(String lnch) {
		this.lnch = lnch;
	}

	public String getEik() {
		return eik;
	}

	public void setEik(String eik) {
		this.eik = eik;
	}

	public Integer getAge() {
		return age;
	}

	public void setAge(Integer age) {
		this.age = age;
	}

	public Integer getSex() {
		return sex;
	}

	public void setSex(Integer sex) {
		this.sex = sex;
	}

	public Integer getNation() {
		return nation;
	}

	public void setNation(Integer nation) {
		this.nation = nation;
	}

	public Integer getCity() {
		return city;
	}

	public void setCity(Integer city) {
		this.city = city;
	}

	public String getPCode() {
		return pCode;
	}

	public void setPCode(String pCode) {
		this.pCode = pCode;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Boolean getProtect() {
		return protect;
	}

	public void setProtect(Boolean protect) {
		this.protect = protect;
	}

	public Long getDate() {
		return date;
	}

	public void setDate(Long date) {
		this.date = date;
	}

	public String getDefendant() {
		return defendant;
	}

	public void setDefendant(String defendant) {
		this.defendant = defendant;
	}

	public Integer getRights() {
		return rights;
	}

	public void setRights(Integer rights) {
		this.rights = rights;
	}

	public Integer getVid() {
		return vid;
	}

	public void setVid(Integer vid) {
		this.vid = vid;
	}

	public String getDescr() {
		return descr;
	}

	public void setDescr(String descr) {
		this.descr = descr;
	}

	public String getRequest() {
		return request;
	}

	public void setRequest(String request) {
		this.request = request;
	}

	public Boolean getConsidered() {
		return considered;
	}

	public void setConsidered(Boolean considered) {
		this.considered = considered;
	}

	public String getConsideredBy() {
		return consideredBy;
	}

	public void setConsideredBy(String consideredBy) {
		this.consideredBy = consideredBy;
	}

	public Integer getAnswer() {
		return answer;
	}

	public void setAnswer(Integer answer) {
		this.answer = answer;
	}

	public List<JalbaFile> getFiles() {
		return files;
	}

	public void setFiles(List<JalbaFile> files) {
		this.files = files;
	}
	
}

