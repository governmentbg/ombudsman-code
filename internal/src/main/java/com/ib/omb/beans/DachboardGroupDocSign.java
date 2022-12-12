package com.ib.omb.beans;

import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import javax.mail.internet.MimeUtility;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.primefaces.PrimeFaces;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ib.indexui.fileupload.FileForJS;
import com.ib.indexui.fileupload.Token;
import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.db.dao.DocDAO;
import com.ib.omb.db.dao.TaskDAO;
import com.ib.omb.db.dto.Task;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.system.db.JPA;
import com.ib.system.db.dao.FilesDAO;
import com.ib.system.db.dto.Files;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.utils.Base64;
import com.ib.system.utils.DateUtils;
import com.ib.system.utils.SearchUtils;

/**
 * Групово подписване на документи, избрани от работния плот
 * 
 */
@Named ("dachboardGroupDocSign")
@ViewScoped
public class DachboardGroupDocSign  extends IndexUIbean   {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3464193177609828057L;

	private static final Logger LOGGER = LoggerFactory.getLogger(DachboardGroupDocSign.class);

	private List<Object[]> docsForSign;	
	private Map<Long, List<Files>> selFiles; // key=ИД на документ, value=списък с файлове 
	
	private HashMap<Long, Boolean> selectFiles;
	
	private Task tmpTask;
	private List<SystemClassif> opinionLst;
	private Integer endOpinion;
	private Date realEnd;
	private String statusComments;
	private List<Integer> idTasks;
	private List<Integer> idDocsOnFile;
	private boolean saveTask;
	
	@SuppressWarnings("unchecked")
	@PostConstruct
	public void initData() {
		
		this.docsForSign = new ArrayList<>();
		this.selFiles = new LinkedHashMap<>();
		this.selectFiles = new HashMap<>();
		this.opinionLst = new ArrayList<>();
		this.tmpTask = new Task();
		this.idTasks = new ArrayList<>();
		this.idDocsOnFile = new ArrayList<>();
		this.saveTask = false;
		
		HttpSession session = (HttpSession) JSFUtils.getExternalContext().getSession(false);
		List<Long> idDocs = (List<Long>) session.getAttribute("idDocs");
		Integer selMenu = (Integer) session.getAttribute("selMenu");
		this.idTasks = (List<Integer>) session.getAttribute("idTasks");
					
		if(idDocs != null && !idDocs.isEmpty()) {
			
			try {
				
				List<Files> filesListDoc = new ArrayList<>();
				
				for (Long docId : idDocs) {
					
					Object[] docData = new DocDAO(getUserData()).findDocData(docId.intValue());
					
					Object[] doc = new Object[4];
					
					doc[0] = SearchUtils.asInteger(docData[0]);
					doc[1] = DocDAO.formRnDoc(docData[1], docData[5]);
					doc[2] = SearchUtils.asDate(docData[2]);
					doc[3] = SearchUtils.asString(docData[4]);					

					filesListDoc = new FilesDAO(getUserData()).selectByFileObjectDop(SearchUtils.asInteger(doc[0]), OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC);
					
					if (!filesListDoc.isEmpty()) {
						for (Files file : filesListDoc) {
							if (file.getFilePurpose() != null && file.getFilePurpose().equals(OmbConstants.CODE_ZNACHENIE_FILE_PURPOSE_MAIN_DOC)) {
								selectFiles.put(Long.valueOf(file.getId()),Boolean.TRUE);
								if (!checkExistIdDoc(SearchUtils.asInteger(doc[0]))) {
									this.idDocsOnFile.add(SearchUtils.asInteger(doc[0]));
								}								
							}							
						}						
					}
										
					docsForSign.add(doc);
					selFiles.put(docId, filesListDoc);
					
				}
				
				if (selMenu.intValue() == OmbConstants.CODE_ZNACHENIE_DASHBOARD_FOR_SAGL) {
					this.opinionLst = getSystemData().getClassifByListVod(OmbConstants.CODE_LIST_TASK_TYPE_TASK_OPINION, Integer.valueOf(OmbConstants.CODE_ZNACHENIE_TASK_TYPE_SAGL), getCurrentLang(), new Date());
					this.endOpinion = OmbConstants.CODE_ZNACHENIE_TASK_OPINION_SAGL;					
				}
				
				if (selMenu.intValue() == OmbConstants.CODE_ZNACHENIE_DASHBOARD_FOR_PODPIS) {
					this.opinionLst = getSystemData().getClassifByListVod(OmbConstants.CODE_LIST_TASK_TYPE_TASK_OPINION, Integer.valueOf(OmbConstants.CODE_ZNACHENIE_TASK_TYPE_PODPIS), getCurrentLang(), new Date());
					this.endOpinion = OmbConstants.CODE_ZNACHENIE_TASK_OPINION_PODPIS;
				}
				
				this.realEnd = new Date();
			
			} catch (BaseException e) {
				LOGGER.error("Грешка при зареждане данните на документа! ", e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			
			} catch (Exception e) {
				LOGGER.error("Грешка при зареждане данните на документа!!", e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
						IndexUIbean.getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
			} finally {
				JPA.getUtil().closeConnection(); 
			}
										
		}
	}
	
	/**
	 * Маркиране на всички
	 * 
	 */
	public void actionMarkAll( ) {
		
		if(selectFiles!=null) {
			for (HashMap.Entry<Long, Boolean> entry : selectFiles.entrySet()) {
				entry.setValue(Boolean.TRUE);
		    }
		}
		
	}
	
	/**
	 * Размаркиране на всички
	 * 
	 */
	public void actionUnmarkAll() {
		
		if(selectFiles!=null) {
			for (HashMap.Entry<Long, Boolean> entry : selectFiles.entrySet()) {
				entry.setValue(Boolean.FALSE);
		    }
		}
				
	}
	
	/**
	 * Пренасочване към разглеждане на документи
	 * 
	 */
	public String actionGotoViewDoc(Integer idDoc) {

		return "docView.xhtml?faces-redirect=true&idObj=" + idDoc;
	}
	
	//Това се налага защото jsf HashMap не работи с integer
	public Long castLong(Integer i) {
		
		if(i == null) return -1L;
		
		return Long.valueOf(i); 		
	}
	
	/**
	 * Download selected file
	 *
	 * @param files
	 */
	public void download(Files files) {
		try {
			if (files.getId() != null){
			
				FilesDAO dao = new FilesDAO(getUserData());					
			
				try {
					
					files = dao.findById(files.getId());	
				
				} finally {
					JPA.getUtil().closeConnection();
				}
				
				if(files.getContent() == null){					
					files.setContent(new byte[0]);
				}
			}
			

			FacesContext facesContext = FacesContext.getCurrentInstance();
			ExternalContext externalContext = facesContext.getExternalContext();

			HttpServletRequest request = (HttpServletRequest) externalContext.getRequest();
			String agent = request.getHeader("user-agent");

			String codedfilename = "";

			if (null != agent && (-1 != agent.indexOf("MSIE") || -1 != agent.indexOf("Mozilla") && -1 != agent.indexOf("rv:11") || -1 != agent.indexOf("Edge"))) {
				codedfilename = URLEncoder.encode(files.getFilename(), "UTF8");
			} else if (null != agent && -1 != agent.indexOf("Mozilla")) {
				codedfilename = MimeUtility.encodeText(files.getFilename(), "UTF8", "B");
			} else {
				codedfilename = URLEncoder.encode(files.getFilename(), "UTF8");
			}

			externalContext.setResponseHeader("Content-Type", "application/x-download");
			externalContext.setResponseHeader("Content-Length", files.getContent().length + "");
			externalContext.setResponseHeader("Content-Disposition", "attachment;filename=\"" + codedfilename + "\"");
			externalContext.getResponseOutputStream().write(files.getContent());

			facesContext.responseComplete();

		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}
	}	
	

	public List<Object[]> getDocsForSign() {
		return docsForSign;
	}

	public void setDocsForSign(List<Object[]> docsForSign) {
		this.docsForSign = docsForSign;
	}

	public Map<Long, List<Files>> getSelFiles() {
		return selFiles;
	}

	public void setSelFiles(Map<Long, List<Files>> selFiles) {
		this.selFiles = selFiles;
	}
	
	public HashMap<Long, Boolean> getSelectFiles() {
		return selectFiles;
	}

	public void setSelectFiles(HashMap<Long, Boolean> selectFiles) {
		this.selectFiles = selectFiles;
	}	
	
	public Task getTmpTask() {
		return tmpTask;
	}

	public void setTmpTask(Task tmpTask) {
		this.tmpTask = tmpTask;
	}

	public List<SystemClassif> getOpinionLst() {
		return opinionLst;
	}

	public void setOpinionLst(List<SystemClassif> opinionLst) {
		this.opinionLst = opinionLst;
	}
	
	public Integer getEndOpinion() {
		return endOpinion;
	}

	public void setEndOpinion(Integer endOpinion) {
		this.endOpinion = endOpinion;
	}

	public Date getRealEnd() {
		return realEnd;
	}

	public void setRealEnd(Date realEnd) {
		this.realEnd = realEnd;
	}

	public String getStatusComments() {
		return statusComments;
	}

	public void setStatusComments(String statusComments) {
		this.statusComments = statusComments;
	}

	public List<Integer> getIdTasks() {
		return idTasks;
	}

	public void setIdTasks(List<Integer> idTasks) {
		this.idTasks = idTasks;
	}
	
	public List<Integer> getIdDocsOnFile() {
		return idDocsOnFile;
	}

	public void setIdDocsOnFile(List<Integer> idDocsOnFile) {
		this.idDocsOnFile = idDocsOnFile;
	}

	public boolean isSaveTask() {
		return saveTask;
	}

	public void setSaveTask(boolean saveTask) {
		this.saveTask = saveTask;
	}

	public void addDocIdOnSelFile(Long docId) {
		if (!checkExistIdDoc(SearchUtils.asInteger(docId))) {
			this.idDocsOnFile.add(SearchUtils.asInteger(docId));
		}		
	}
	
	public void actionCheckSelFile() {
		
		boolean isCheck = false;
		for (HashMap.Entry<Long, Boolean> entry : selectFiles.entrySet()) {
			if (entry.getValue().booleanValue()) {
				isCheck = true;
				break;
			}
		}
		
		if (!isCheck) {
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dashboardGroupSign.choiceDocsForSign"));
			return;
		
		} else {
			PrimeFaces.current().executeScript("PF('hiddenSign').jq.click();");			
		}
	}
	
	public void actionChangeOpinion() {
		
		try {
			
			if (this.endOpinion == null) {
				JSFUtils.addMessage("formGroupDocSign:opinion", FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "dashboardGroupSign.mnenieGrPodpis")));
				return;
	
			} else {
	
				if (getSystemData().matchClassifItems(OmbConstants.CODE_CLASSIF_TASK_OPINION_WITH_COMMENT, this.endOpinion, new Date())
						&& (this.statusComments == null || this.statusComments.isEmpty())) {
					JSFUtils.addMessage("formGroupDocSign:comentar", FacesMessage.SEVERITY_ERROR, IndexUIbean.getMessageResourceString(beanMessages, "task.msgStComment"));
					return;
				}
			}
		
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при  проверка за мнения, изискващи коментар! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, IndexUIbean.getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
		}
		
	}

	public void actionSignBnt() {
		
	//	System.out.println("actionSignBnt: ");
		
		if(selectFiles!=null) {
			List<FileForJS> idFiles = new ArrayList<>();
			
			FilesDAO dao = new FilesDAO(getUserData());
			try {
				for (HashMap.Entry<Long, Boolean> entry : selectFiles.entrySet()) {
					if(entry.getValue().booleanValue()) {
						Files files = dao.findByIdMetaData(Integer.valueOf(entry.getKey().intValue()));
						
						FileForJS fileJs = new FileForJS();
						fileJs.setId(files.getId());
						fileJs.setFilename(files.getFilename());
						fileJs.setContentType(files.getContentType());
						
//						String data = Base64.encodeBytes(files.getContent());
//						data = data.replace("\n", " ");
//						//System.out.println( "dfdd-> "+data.indexOf("\n"));
//					    fileJs.setContent(data);

						
						fileJs.setUserId(getCurrentUserId());
						
						String strId = files.getId().toString();
						
						String salt = strId;
			        	int len = salt.length();
			        	if(len<8) {
			        		for (int i=1; i<=(8-len); i++) {
			        			salt += ""+i;
			        		}
			        	}
						
						Token tokenObj = new Token();
			        	tokenObj.setIdObj(strId);
			        	tokenObj.setCreated(getUserData().getUserId().toString());
			        	tokenObj.setFiLength(Long.valueOf(new Date().getTime()));
			        	
			        	
			        	String tokenval = new ObjectMapper().writeValueAsString(tokenObj);
			        	
						String token = encrypt(tokenval ,salt.getBytes() , getUserData().getUserId()+"barbaqni46barbaqni46");
						fileJs.setToken(token);
						
						idFiles.add(fileJs);
					}
			    }
				
			//	System.out.println("idFiles size: "+idFiles.size());
				
				if(!idFiles.isEmpty()) {
					actionSaveTask();
					//actionSignComplete();
					
					PrimeFaces.current().executeScript("loadFileDataMultySign('"+new ObjectMapper().writeValueAsString(idFiles)+"')");
					//PrimeFaces.current().executeScript("loadFileDataMultySign('"+new Gson().toJson(idFiles)+"')");
				
				} else {
					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dashboardGroupSign.choiceDocsForSign"));	
					return;			
				}
			
			} catch (Exception e) {
				LOGGER.error(e.getMessage(), e);
			
			} finally {
				JPA.getUtil().closeConnection();
			}
		}
	}
	
	public void actionSignComplete() {
		//TODO ..... 
		// ще се извика след записа на последният подписан файл
		if (saveTask) {
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, "Документите са подписани успешно!");
		}	
	}
	
	public boolean checkExistIdDoc(Integer idDoc) {
		
		boolean exist = false;
		
		for (Integer id : idDocsOnFile) {
			
			if (id.equals(idDoc)) {
				exist = true;
				break;				
			}
		}
		
		return exist;
	}
	
	/**
	 * Запис на задача
	 * 
	 */
	public void actionSaveTask() {

		try {
			
			boolean check = true;
			
			if (this.realEnd == null) {
				JSFUtils.addMessage("formGroupDocSign:exeDat", FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "docu.dateDoc")));
				check = false;				
			}
			
			if (this.endOpinion == null) {
				JSFUtils.addMessage("formGroupDocSign:opinion", FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "dashboardGroupSign.mnenieGrPodpis")));
				check = false;	
	
			} else {
	
				if (getSystemData().matchClassifItems(OmbConstants.CODE_CLASSIF_TASK_OPINION_WITH_COMMENT, this.endOpinion, new Date())
						&& (this.statusComments == null || this.statusComments.isEmpty())) {
					JSFUtils.addMessage("formGroupDocSign:comentar", FacesMessage.SEVERITY_ERROR, IndexUIbean.getMessageResourceString(beanMessages, "task.msgStComment"));
					check = false;	
				}
			}
			
			if (check) {

				for (Integer idTask : this.idTasks) {
					
					saveTask = false;
	
					JPA.getUtil().runWithClose(() -> tmpTask = new TaskDAO(getUserData()).findById(idTask));
						
					if (checkExistIdDoc(tmpTask.getDocId())) {
						
						tmpTask.setEndOpinion(this.endOpinion);
						tmpTask.setRealEnd(this.realEnd);  
						tmpTask.setStatusComments(this.statusComments); 
						
						boolean flag = true;

						if (tmpTask != null) {

							Date enddate = DateUtils.startDate(tmpTask.getRealEnd());
							Date adate = DateUtils.startDate(tmpTask.getAssignDate());

							if (enddate == null || enddate.before(adate)) {// дали не е преди дата на възлагaнe!
								JSFUtils.addMessage("formGroupDocSign:exeDat", FacesMessage.SEVERITY_ERROR, IndexUIbean.getMessageResourceString(beanMessages, "task.msgDateRealEnd"));
								flag = false;
							}

							if (getSystemData().matchClassifItems(OmbConstants.CODE_CLASSIF_TASK_OPINION_WITH_COMMENT, tmpTask.getEndOpinion(), new Date())
									&& (tmpTask.getStatusComments() == null || tmpTask.getStatusComments().isEmpty())) {
								JSFUtils.addMessage("formGroupDocSign:comentar", FacesMessage.SEVERITY_ERROR, IndexUIbean.getMessageResourceString(beanMessages, "task.msgStComment"));
								flag = false;
							}							

						} else {
							LOGGER.error("actionSaveTask -> Грешка  задачата е нулл!");
						}

						if (flag) {

							tmpTask.setStatus(OmbConstants.CODE_ZNACHENIE_TASK_STATUS_IZP);

							JPA.getUtil().runInTransaction(() -> new TaskDAO(getUserData()).save(tmpTask, null, (SystemData) getSystemData()));
							
							saveTask = true;

							//JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, IndexUIbean.getMessageResourceString(UI_beanMessages, SUCCESSAVEMSG));
						}						
					}					
										
				}
			}

		} catch (BaseException e) {
			LOGGER.error("actionSaveTask - > Грешка при запис на задача! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, IndexUIbean.getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
		}
		
	}
	
	
	private static final String SecretKeySpec = "IndexSigner v.4.32.2.0 / 15.06.2021";
	
	private String encrypt(String tokenString ,byte[]  salt , String  ivValue) throws Exception {
	    try {
	     
	    	
	    	// With the java libraries
			
			MessageDigest digest = MessageDigest.getInstance("SHA-1");
	        digest.reset();
	        digest.update(ivValue.getBytes("utf8"));
	        
	        byte iv[] =    Arrays.copyOfRange(digest.digest(), 0, 16);
	        
	        //---------------------------------------------------------------------------
	    	
	        IvParameterSpec ivspec = new IvParameterSpec(iv);
	 
	        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
	        KeySpec spec = new PBEKeySpec(SecretKeySpec.toCharArray(), salt, 2048, 256);
	        SecretKey tmp = factory.generateSecret(spec);
	        SecretKeySpec secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");
	 
	        //  SecretKey key = KeyGenerator.getInstance("AES").generateKey();
	      
	        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
	        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivspec);
	        
	        String data = Base64.encodeBytes(cipher.doFinal(tokenString.getBytes()));
	        data = data.replace("\n", " ");
	        return data;
	      
	    } catch (Exception e) {
	    	throw e;
	    }
	    
	}
	
}