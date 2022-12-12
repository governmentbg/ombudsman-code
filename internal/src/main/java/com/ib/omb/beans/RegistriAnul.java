package com.ib.omb.beans;

import java.io.IOException;
import java.io.Serializable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;

import javax.faces.view.ViewScoped;
import javax.inject.Named;

import static com.ib.system.utils.SearchUtils.asInteger;

import org.primefaces.component.export.PDFOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.customexporter.CustomExpPreProcess;
import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.db.dao.DocVidSettingDAO;
import com.ib.omb.db.dao.RegisterDAO;
import com.ib.omb.db.dto.DocVidSetting;
import com.ib.omb.db.dto.Register;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.UserData;
import com.ib.system.db.JPA;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.DbErrorException;

@Named
@ViewScoped
public class RegistriAnul extends IndexUIbean  implements Serializable {	
	
	/**
	 * Анулиране на регистри за документи
	 * 
	 */
	
	private static final long serialVersionUID = -2192535582473115944L;
	private static final Logger LOGGER = LoggerFactory.getLogger(RegistriAnul.class);
		
	private Register register = new Register();
	private DocVidSetting registerHD = new DocVidSetting();
	private transient RegisterDAO registerDAO;
	private transient DocVidSettingDAO registerDAOHD;
	
	private Integer selRegistratura;
	private String selRegistraturaT;
	private Integer tipAlg;
	private int currentLang=getCurrentLang();
	//private List<Object[]> registriListA = new ArrayList<Object[]>();
	private transient List<Object[]> registriListA;
	private transient List<Object[]> selectedRegisters;
	
	private transient List<Object[]> registriListAHD;
	private transient List<Object[]> selectedRegistersHD;
	
	private transient List<Object[]> registriListAll;
	private transient List<Object[]> registriListAllHD;
	public static final String ERRRECREG= "Грешка при запис на регистър! ";
	
	public static final  String ERRNULLREG = "regNull.errExport";
	
	
	
	
	private boolean useCommonRegisters;
	
	
	


	/** */
	@PostConstruct
	void initData() {
		LOGGER.debug("!!! PostConstruct RegistriAnul !!!");
		this.registriListA = new ArrayList<>();
		this.selectedRegisters=new ArrayList<>();
		
		this.registriListAHD = new ArrayList<>();
		this.selectedRegistersHD = new ArrayList<>();
		
		this.registriListAll = new ArrayList<>();
		this.registriListAllHD = new ArrayList<>();
		
		
		this.tipAlg=OmbConstants.CODE_ZNACHENIE_ALG_INDEX_STEP;
		this.useCommonRegisters = false;
		this.selRegistratura=getUserData(UserData.class).getRegistratura();
		

		try {
			
						
			if ((getSystemData().getSettingsValue("delo.useCommonRegisters")).equals("1")) //{
				this.setUseCommonRegisters(true);
			/*} else {
				this.setUseCommonRegisters(false);
			}*/
			
			
			if (null!=this.selRegistratura) {
				this.selRegistraturaT=getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_REGISTRATURI, this.selRegistratura, currentLang, new Date());
				actionLoadSelected();
			}
			
		
		} catch (BaseException e) {
			LOGGER.error("Грешка при четене на системните настройки! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}
	
	}
		
	
	public void actionLoadSelected() {
		
		try {
			
			if (null!=this.selRegistratura && null!=this.getTipAlg()) {									
				Boolean valid = null;
		
				if (this.tipAlg.equals(Integer.valueOf(OmbConstants.CODE_ZNACHENIE_ALG_INDEX_STEP))) {// Algoritam index with steps
					
					this.registerDAO = new RegisterDAO(getUserData());
					JPA.getUtil().runWithClose(() -> this.registriListA = this.registerDAO.findAnulByRegistraturaId(this.selRegistratura, this.useCommonRegisters, valid, this.tipAlg));
					if (null!=this.selectedRegisters)
						this.getSelectedRegisters().clear();
				}else {// Algoritam index for vid documents
					
					this.registerDAOHD = new DocVidSettingDAO(getUserData());
					JPA.getUtil().runWithClose(() -> this.registriListAHD = this.registerDAOHD.selectRegistriHDanul(this.selRegistratura, valid));
					if (null!=this.selectedRegistersHD)
						this.getSelectedRegistersHD().clear();
				}
			}

		
		} catch (BaseException e) {
			LOGGER.error("Грешка при зареждане данните на регистрите! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}
		
	}
	


	/**
	 * @return the useCommonRegisters
	 */
	public boolean isUseCommonRegisters() {
		return useCommonRegisters;
	}

	/**
	 * @param useCommonRegisters the useCommonRegisters to set
	 */
	public void setUseCommonRegisters(boolean useCommonRegisters) {
		this.useCommonRegisters = useCommonRegisters;
	}

	/**
	 * @return the registerDAO
	 */
	public RegisterDAO getRegisterDAO() {
		return registerDAO;
	}

	/**
	 * @param registerDAO the registerDAO to set
	 */
	public void setRegisterDAO(RegisterDAO registerDAO) {
		this.registerDAO = registerDAO;
	}

	/**
	 * @return the registriListA
	 */
	public List<Object[]> getRegistriListA() {
		return registriListA;
	}

	/**
	 * @param registriListA the registriListA to set
	 */
	public void setRegistriListA(List<Object[]> registriListA) {
		this.registriListA = registriListA;
	}

	/**
	 * @return the selectedRegisters
	 */
	public List<Object[]> getSelectedRegisters() {
		return selectedRegisters;
	}

	/**
	 * @param selectedRegisters the selectedRegisters to set
	 */
	public void setSelectedRegisters(List<Object[]> selectedRegisters) {
		this.selectedRegisters = selectedRegisters;
	}

	/**
	 * @return the selRegistratura
	 */
	public Integer getSelRegistratura() {
		return selRegistratura;
	}

	/**
	 * @param selRegistratura the selRegistratura to set
	 */
	public void setSelRegistratura(Integer selRegistratura) {
		this.selRegistratura = selRegistratura;
	}

		
	// Нулиране избрани регистри от избрана регистратура 
	public void actionAnulSelRegisters() {

		if (this.tipAlg.equals(Integer.valueOf(OmbConstants.CODE_ZNACHENIE_ALG_INDEX_STEP))){ //1. Registri za documenti s alg index+step
			if (null!=this.selectedRegisters && this.selectedRegisters.isEmpty()) {
				
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "register.noSelectedNull"));
				return;
			}
			nullRegAlgIndStep();
			
		}else{  //2. s index po vid na documenta
			if (null!=this.selectedRegistersHD && this.selectedRegistersHD.isEmpty()) {
				
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "register.noSelectedNull"));
				return;
			}
			
			nullRegAlgVidDoc();

		}
					
		JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "register.succesNullMsg"));
		
	}


	// Нулиране всички регистри от всички регистратури за избран алгоритъм
	public void actionAnulAllRegisters() {
				

		if (this.tipAlg.equals(Integer.valueOf(OmbConstants.CODE_ZNACHENIE_ALG_INDEX_STEP))){ //1. Registri za documenti s alg index+step
			
			anulAllAIS();
		}else{  //2. s index po vid na documenta
			
			anulAllIVD();
		}

		JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "register.succesNullMsg"));
	
	}
	
	
	

	/**
	 * @return the tipAlg
	 */
	public Integer getTipAlg() {
		return tipAlg;
	}



	/**
	 * @param tipAlg the tipAlg to set
	 */
	public void setTipAlg(Integer tipAlg) {
		this.tipAlg = tipAlg;
	}




	/**
	 * @return the registerDAOHD
	 */
	public DocVidSettingDAO getRegisterDAOHD() {
		return registerDAOHD;
	}



	/**
	 * @param registerDAOHD the registerDAOHD to set
	 */
	public void setRegisterDAOHD(DocVidSettingDAO registerDAOHD) {
		this.registerDAOHD = registerDAOHD;
	}



	/**
	 * @return the registriListAHD
	 */
	public List<Object[]> getRegistriListAHD() {
		return registriListAHD;
	}



	/**
	 * @param registriListAHD the registriListAHD to set
	 */
	public void setRegistriListAHD(List<Object[]> registriListAHD) {
		this.registriListAHD = registriListAHD;
	}



	/**
	 * @return the selectedRegistersHD
	 */
	public List<Object[]> getSelectedRegistersHD() {
		return selectedRegistersHD;
	}



	/**
	 * @param selectedRegistersHD the selectedRegistersHD to set
	 */
	public void setSelectedRegistersHD(List<Object[]> selectedRegistersHD) {
		this.selectedRegistersHD = selectedRegistersHD;
	}


	/**
	 * @return the registerHD
	 */
	public DocVidSetting getRegisterHD() {
		return registerHD;
	}


	/**
	 * @param registerHD the registerHD to set
	 */
	public void setRegisterHD(DocVidSetting registerHD) {
		this.registerHD = registerHD;
	}


	/**
	 * @return the registriListAll
	 */
	public List<Object[]> getRegistriListAll() {
		return registriListAll;
	}


	/**
	 * @param registriListAll the registriListAll to set
	 */
	public void setRegistriListAll(List<Object[]> registriListAll) {
		this.registriListAll = registriListAll;
	}


	/**
	 * @return the registriListAllHD
	 */
	public List<Object[]> getRegistriListAllHD() {
		return registriListAllHD;
	}


	/**
	 * @param registriListAllHD the registriListAllHD to set
	 */
	public void setRegistriListAllHD(List<Object[]> registriListAllHD) {
		this.registriListAllHD = registriListAllHD;
	}


	/*public void actionSelAllReg(ToggleSelectEvent event) {// Метод за мултиселект
		this.selectedRegisters.clear(); // Изтрива селектираниет
		if (event.isSelected())  //Избрани - всички
			this.selectedRegisters.addAll(this.getRegistriListA());
		
	}*/
	
	
	
/******************************** EXPORTS **********************************/
	
	/**
	 * за експорт в excel - добавя заглавие и дата на изготвяне на справката и др. - за регистрите
	 */
	public void postProcessXLS(Object document) {
		
		Object[] dopInfoT=getReportTitles();
		if (null==dopInfoT)
			return;
		String title = "";
		if (null!=dopInfoT[0])
			title += dopInfoT[0].toString();	
		
		Object[] dopInfo=new Object[2];
		if (null!=dopInfoT[1])
			dopInfo[0]=dopInfoT[1];
		if (null!=dopInfoT[2])
			dopInfo[1]=dopInfoT[2];
			
		new CustomExpPreProcess().postProcessXLS(document, title, dopInfo, null, null);	
     
	}

	/**
	 * за експорт в pdf - добавя заглавие и дата на изготвяне на справката - за регистрите
	 */
	public void preProcessPDF(Object document) {
		
		Object[] dopInfoT=getReportTitles();

		if (null==dopInfoT)
			return;
		String title = "";
		if (null!=dopInfoT[0])
			title += dopInfoT[0].toString();	
		
		Object[] dopInfo=new Object[2];
		
		if (null!=dopInfoT[1])
			dopInfo[0]=dopInfoT[1];
		if (null!=dopInfoT[2])
			dopInfo[1]=dopInfoT[2];
		
		try{
			
			new CustomExpPreProcess().preProcessPDF(document, title, dopInfo, null, null);		
						
		} catch (IOException e) {
			LOGGER.error(e.getMessage(),e);	
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, ERRNULLREG));
		}
	}
	

	/**
	 * за експорт в pdf 
	 * @return
	 */
	public PDFOptions pdfOptions() {
		PDFOptions pdfOpt = new CustomExpPreProcess().pdfOptions(null, null, null);
        return pdfOpt;
	}
	
	/*public void preProcessPDF(Object document) throws Exception {
	    try {
	    	Document pdf = (Document) document;
	        pdf.open();
	        pdf.setPageSize(PageSize.A4.rotate());
	    } catch (Exception e) {
	    	LOGGER.error("Грешка при експорт в pdf", e);
			throw new Exception(e.getMessage());
	    }
	}*/
	
	
	
	
	
	
	public Object[] getReportTitles() {
		
		Object[] dopInfoT=new Object[3];
		try {
			
			String title = getMessageResourceString(LABELS, "register.reportTitle");
			dopInfoT[0]=title;
			
			if (null!=selRegistratura)
				dopInfoT[1]=getMessageResourceString(LABELS, "regData.registratura")+": "+ getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_REGISTRATURI, selRegistratura, currentLang, new Date());
			
			if (null!=tipAlg) {
				
				if (tipAlg.equals(Integer.valueOf(OmbConstants.CODE_ZNACHENIE_ALG_INDEX_STEP))){
					dopInfoT[2]=getMessageResourceString(LABELS, "registerAnul.tipNulirane")+" "+getMessageResourceString(LABELS, "registerAnul.algIndexStep");
				}else {
					dopInfoT[2]=getMessageResourceString(LABELS, "registerAnul.tipNulirane")+" "+getMessageResourceString(LABELS, "registerAnul.algIndexVidDoc");
				}
	
			}
			
		
		} catch (DbErrorException e) {	
			LOGGER.error(e.getMessage(),e);	
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, ERRNULLREG));
			return dopInfoT;
		} 	
		
		return dopInfoT;
	}
	
	
	public void nullRegAlgIndStep() {
		try {
		
		this.registerDAO = new RegisterDAO(getUserData());
		
		JPA.getUtil().runInTransaction(() -> {
				
		for (Object[] itemS: this.selectedRegisters) {
		
			register = new Register();
			register = this.registerDAO.findById(asInteger(itemS[0]));
										
			if (null!=register.getBegNomer() && null!=register.getStep()){
			
				Integer mod = register.getBegNomer()%register.getStep();
				Integer newactnom = mod;
				if (mod == 0)
					newactnom +=register.getStep();
				
				register.setBegNomer(newactnom);
				register.setActNomer(newactnom);	
				
				register=this.registerDAO.save(register, "нулиране");
			}
		}
			
		});
		
		JPA.getUtil().runWithClose(() -> this.registriListA = this.registerDAO.findAnulByRegistraturaId(this.selRegistratura, this.useCommonRegisters, null, this.tipAlg));
		
		} catch (BaseException e) {			
			LOGGER.error(ERRRECREG, e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
			
		}
	}
	
	
	public void nullRegAlgVidDoc() {
		
		try {
		
		this.registerDAOHD = new DocVidSettingDAO(getUserData());	
		JPA.getUtil().runInTransaction(() -> {
				
		for (Object[] itemS: this.selectedRegistersHD) {	

			registerHD = new DocVidSetting();
			registerHD = this.registerDAOHD.findById(asInteger(itemS[0]));
										
			if ( null!=registerHD && null!=registerHD.getBegNomer() && null!=registerHD.getStep()){
			
				Integer mod = registerHD.getBegNomer()%registerHD.getStep();
				Integer newactnom = mod;
				if (mod == 0)
					newactnom +=registerHD.getStep();
											
				registerHD.setBegNomer(newactnom);
				registerHD.setActNomer(newactnom);	
				
				registerHD=this.registerDAOHD.save(registerHD);

			}
	
		}
			
		});
		
		
		JPA.getUtil().runWithClose(() -> this.registriListAHD = this.registerDAOHD.selectRegistriHDanul(this.selRegistratura, null));

		this.selectedRegistersHD.clear();
		
		} catch (BaseException e) {			
			LOGGER.error(ERRRECREG, e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
			
		}
		
	}
	
	
	/*public void preProcessPDF(Object document) throws IOException, BadElementException, DocumentException {
		Document pdf = (Document) document;
        pdf.open();
        pdf.setPageSize(PageSize.A4);

        ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
        String logo = externalContext.getRealPath("") + File.separator + "resources" + File.separator + "demo" + File.separator + "images" + File.separator + "pdf.png";
        library="images" name="pdf.png" library="images" name="pdf.png" 
		pdf.add(Image.getInstance(logo));
        
        ServletContext servletContext = (ServletContext)
	    FacesContext.getCurrentInstance().getExternalContext().getContext();
	    String logo = servletContext.getRealPath("") + File.separator + "resources"+File.separator + "images" + File.separator + "html.png";
	    pdf.add(Image.getInstance(logo));
        
        
	}*/

	
	public void anulAllAIS() {// Нулиране на всички с алг. индекс и стъпка
		
		try {
		
			this.registerDAO = new RegisterDAO(getUserData());
			this.registriListAll = new ArrayList<>();
			
			JPA.getUtil().runWithClose(() -> this.registriListAll = this.registerDAO.findAnulByRegistraturaId(null, this.useCommonRegisters, null, this.tipAlg));
	
			
			JPA.getUtil().runInTransaction(() -> {
				
	                      
				for (Object[] item: this.registriListAll) {
					register = new Register();
					register = this.registerDAO.findById(asInteger(item[0]));
					
												
					if (null!=register.getBegNomer() && null!=register.getStep() && register.getActNomer()>1){
					
						Integer mod = register.getBegNomer()%register.getStep();
						
						Integer newactnom = mod;
						if (mod == 0)
							newactnom +=register.getStep();
						/*}else{
							newactnom = mod;
						}*/
						
						register.setBegNomer(newactnom);
						register.setActNomer(newactnom);	
						
						register=this.registerDAO.save(register, "нулиране");
	
					}
						
					
				}
				
			});
			this.registriListAll = new ArrayList<>();
			this.registriListA = new ArrayList<>();
			this.selRegistratura=null;
			
			JPA.getUtil().runWithClose(() -> this.registriListA = this.registerDAO.findAnulByRegistraturaId(this.selRegistratura, this.useCommonRegisters, null, this.tipAlg));
		
		} catch (BaseException e) {			
			LOGGER.error(ERRRECREG, e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
			
		}
		
	}
	
	public void anulAllIVD() {// Нулиране на всички с алг. по вид на документа
		
		try {
		
			this.registerDAOHD = new DocVidSettingDAO(getUserData());
			this.registriListAllHD = new ArrayList<>();
			JPA.getUtil().runWithClose(() -> this.registriListAllHD = this.registerDAOHD.selectRegistriHDanul(null, null));
			
			JPA.getUtil().runInTransaction(() -> {
			for (Object[] item: this.registriListAllHD) {
	
				registerHD = new DocVidSetting();
				registerHD = this.registerDAOHD.findById(asInteger(item[0]));
				
											
				if (null!=registerHD.getBegNomer() && null!=registerHD.getStep()){
				
					Integer mod = registerHD.getBegNomer()%registerHD.getStep();
					Integer newactnom = mod;
					
					if (mod == 0)
						newactnom += registerHD.getStep();
					/*}else{
						newactnom = mod;
					}*/
					
					registerHD.setBegNomer(newactnom);
					registerHD.setActNomer(newactnom);	
					
					registerHD=this.registerDAOHD.save(registerHD);
	
				}
			
			}
	
			});
			
			this.registriListAllHD = new ArrayList<>();
			this.registriListAHD= new ArrayList<>();
			this.selRegistratura=null;
			JPA.getUtil().runWithClose(() -> this.registriListAHD = this.registerDAOHD.selectRegistriHDanul(this.selRegistratura, null));

		
		} catch (BaseException e) {			
			LOGGER.error(ERRRECREG, e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
			
		}
		
	}


	/**
	 * @return the selRegistraturaT
	 */
	public String getSelRegistraturaT() {
		return selRegistraturaT;
	}


	/**
	 * @param selRegistraturaT the selRegistraturaT to set
	 */
	public void setSelRegistraturaT(String selRegistraturaT) {
		this.selRegistraturaT = selRegistraturaT;
	}
			
}
