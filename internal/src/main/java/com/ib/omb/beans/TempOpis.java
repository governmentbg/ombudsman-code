package com.ib.omb.beans;

import java.io.Serializable;
import java.util.Date;

public class TempOpis implements Serializable{

	private static final long serialVersionUID = 1L;
	public Long   id;
	public String docTom;
	public String regNomer;
	public Date   dataReg;
	public String otnosno;
	public Long   brLista;
	public Date   dataVl;
	public String tekStr;
	
	
	public String getTekStr() {
		return tekStr;
	}

	public void setTekStr(String tekStr) {
		this.tekStr = tekStr;
	}

	public TempOpis(Long id, String docTom, String regNomer, Date dataReg,String otnosno,
			Long brLista,Date dataVl, String tekStr) {
		this.id		 = id;
		this.docTom  = docTom;
		this.regNomer= regNomer;
		this.dataReg = dataReg;
		this.otnosno = otnosno;
		this.brLista = brLista;
		this.dataVl  = dataVl;
		this.tekStr  = tekStr;
	}

	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
   	public String getDocTom() {
			return docTom;
	}

	public void setDocTom(String docTom) {
		this.docTom = docTom;
	}

	public String getRegNomer() {
		return regNomer;
	}
	public void setRegNomer(String regNomer) {
		this.regNomer = regNomer;
	}
	public Date getDataReg() {
		return dataReg;
	}
	public void setDataReg(Date dataReg) {
		this.dataReg = dataReg;
	}
	public String getOtnosno() {
		return otnosno;
	}
	public void setOtnosno(String otnosno) {
		this.otnosno = otnosno;
	}
	public Long getBrLista() {
		return brLista;
	}
	public void setBrLista(Long brLista) {
		this.brLista = brLista;
	}
	public Date getDataVl() {
		return dataVl;
	}
	public void setDataVl(Date dataVl) {
		this.dataVl = dataVl;
	}

	public int compareTo(TempOpis o) {
		// TODO Auto-generated method stub
		return 0;
	}

	public int compare(TempOpis arg0, TempOpis arg1) {
		// TODO Auto-generated method stub
		return 0;
	}
	

}
