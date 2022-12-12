package com.ib.omb.experimental;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;

import com.ib.omb.db.dto.Delo;
import com.ib.omb.db.dto.Doc;
import com.ib.omb.db.dto.DocAccess;
import com.ib.omb.db.dto.DocDvij;
import com.ib.omb.db.dto.NotificationPatternVariables;
import com.ib.omb.db.dto.NotificationPatterns;
import com.ib.omb.db.dto.ReferentDelegation;
import com.ib.omb.db.dto.Task;
import com.ib.omb.db.dto.UserNotifications;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.system.exceptions.DbErrorException;

public class Notification {
	
	
	private Integer notificationCode; 
	private Integer rolia;
	private Integer userId;
	private TreeSet<Integer> adresati = new TreeSet<Integer>();
	private Integer idRegistratura;

	private boolean skipCurrent;
	
	
	private UserNotifications uMessage;
	
	
	/** Основен документ */
	private Doc doc;
	/** Допълнителен документ */
	private Doc otherDoc;	
	/** Задача */
	private Task task;
	/** Преписка */
	private  Delo delo;
	
	private DocDvij docDvij;
	
	/** Системен идентификатор на процедура */
	private Integer procId;
	
	
	private DocAccess docAccess;
	
	private String comment;
	
	
	/** Заместване */
	private ReferentDelegation refD;
	
	
	private SystemData sd;
	
	public SystemData getSd() {
		return sd;
	}

	public Notification(SystemData sd) {
		this.sd = sd;
	}
	
	public Notification(Integer userId, Integer idRegistratura, Integer notificationCode, Integer rolia, SystemData sd) {
		this.userId = userId;
		this.notificationCode = notificationCode;
		this.rolia = rolia;
		this.sd = sd;
		this.idRegistratura = idRegistratura;
	}

	public Doc getDoc() {
		return doc;
	}
	public void setDoc(Doc doc) {
		this.doc = doc;
	}
	public Doc getOtherDoc() {
		return otherDoc;
	}
	public void setOtherDoc(Doc otherDoc) {
		this.otherDoc = otherDoc;
	}
	public Task getTask() {
		return task;
	}
	public void setTask(Task task) {
		this.task = task;
	}
	public Delo getDelo() {
		return delo;
	}
	public void setDelo(Delo delo) {
		this.delo = delo;
	}

	
	private String  getAttr(String attr, Integer codeClassif) {
		
		String value = null;
		
		try {
			if (attr == null) {
				return null;
			}
			Object tekClass = this;
			
			String[] chain = attr.split("\\.");
			for (String tek : chain) {				
				if (tekClass != null) {				
					Field f = tekClass.getClass().getDeclaredField(tek);
					if (f != null) {
						f.setAccessible(true);
						tekClass = f.get(tekClass);
						
						if (tekClass instanceof List<?>){
							List<?> l = (List<?>)tekClass;
							if (l.size() > 0) {
								tekClass = l.get(0);
							}
						}
						
						
					}else {
						return null;
					}
				}else {
					return null;
				}
			}
			
			if (tekClass instanceof String) {
				return (String)tekClass;
			}
			
			if (tekClass instanceof Date) {
				return new SimpleDateFormat("dd.MM.yyyy").format(tekClass);
			}
			
			if (tekClass instanceof Integer && codeClassif != null) {
				return sd.decodeItem(codeClassif, (Integer)tekClass, OmbConstants.CODE_DEFAULT_LANG, new Date());
			}
			
			if (tekClass instanceof Integer) {
				return String.valueOf(tekClass);
			}
			
			
			
			
			
			
		} catch (Exception e) {
			return "Error in " + attr;
		}
		
		 
		
		
		return value;
	}
	
	
	public void generateNotif() throws DbErrorException {
		
		UserNotifications mess= new UserNotifications();
		mess.setMessageType(notificationCode);
		mess.setDateMessage(new Date());
		mess.setSeverity("info");
		if (task != null) {
			mess.setTaskId(task.getId());
		}
		if (doc != null) {
			mess.setCodeObject(doc.getCodeMainObject());
			mess.setIdObject(doc.getId());
		}
		if (delo != null) {
			mess.setCodeObject(delo.getCodeMainObject());
			mess.setIdObject(delo.getId());
		}
		
		if (mess.getIdObject() == null && task != null) {
			mess.setCodeObject(task.getCodeMainObject());
			mess.setIdObject(task.getId());
		}
		
		
		if (mess.getIdObject() == null && procId != null) {
			mess.setCodeObject(OmbConstants.CODE_ZNACHENIE_JOURNAL_PROC_EXE);
			mess.setIdObject(procId);
		}
		
		
		
		NotificationPatterns pattern = sd.getPattern(notificationCode, rolia);
		if (pattern != null) {
			String subject = pattern.getSubject();
			String body = pattern.getBody();
			
			int index = 1;
			while (index != -1) {
				index = body.indexOf("<!--(");
				if (index != -1) {
					int endIndex = body.indexOf("-->");
					String block = body.substring(index, endIndex+3);
					String newBlock ="";
					try {
						int varBeg = block.indexOf("(");
						int varEnd = block.indexOf(")");
						String var = block.substring( varBeg+ 1, varEnd );						
						for (NotificationPatternVariables pvar : pattern.getVariables()) {
							if (pvar.getVarName().equals(var)) {
								String replString = getAttr(pvar.getVarRefl(), pvar.getCodeClassif());								
								if (replString != null &&  !replString.trim().isEmpty()) {
									newBlock = block.substring(varEnd+1, block.length()-3);
								}
							}
						}
						
					}catch (Exception e) {
						e.printStackTrace();
						newBlock = "*** Error in block definition ***";
					}
															
					body = body.replace(block, newBlock);
				}
			}
			
			
			for (NotificationPatternVariables var : pattern.getVariables()) {
				String nameVar = var.getVarName();				
				String replString = getAttr(var.getVarRefl(), var.getCodeClassif());
				if (replString != null) {
					if (subject != null) {
						subject = subject.replaceAll(nameVar, replString);
					}
					
					if (body != null) {
						body = body.replaceAll(nameVar, replString);
					}
				}
			}
			mess.setTitle(subject);
			mess.setDetails(body);
		}
			
		
		
		
		
		
		setuMessage(mess) ;
	}
	
	
	
	public void generatеFakeNotif(Integer codeNotif) throws DbErrorException {
		
		UserNotifications mess= new UserNotifications();
		mess.setMessageType(codeNotif);
		mess.setDateMessage(new Date());
		mess.setSeverity("info");		
		mess.setTitle("");
		mess.setDetails("Reload: " + codeNotif);
		setuMessage(mess);
	}

	public ReferentDelegation getRefD() {
		return refD;
	}

	public void setRefD(ReferentDelegation refD) {
		this.refD = refD;
	}

	public Integer getNotificationCode() {
		return notificationCode;
	}

	public void setNotificationCode(Integer notificationCode) {
		this.notificationCode = notificationCode;
	}

	public Integer getRolia() {
		return rolia;
	}

	public void setRolia(Integer rolia) {
		this.rolia = rolia;
	}

	
	public Integer getUserId() {
		return userId;
	}

	public void setUserId(Integer userId) {
		this.userId = userId;
	}

	

	public void setAdresati(List<Integer> adresati) {
		this.adresati.addAll(adresati);
	}
	
	
	public void setAdresat(Integer adresat) {
		this.adresati.add(adresat);
	}

	public boolean isSkipCurrent() {
		return skipCurrent;
	}

	public void setSkipCurrent(boolean skipCurrent) {
		this.skipCurrent = skipCurrent;
	}

	public void send() {		
		sd.sendNotification(this);
		
	}

	public UserNotifications getuMessage() {
		return uMessage;
	}

	public void setuMessage(UserNotifications uMessage) {
		this.uMessage = uMessage;
	}
	
	public TreeSet<Integer> getAdresati() {
		return adresati;
	}

	public DocAccess getDocAccess() {
		return docAccess;
	}

	public void setDocAccess(DocAccess docAccess) {
		this.docAccess = docAccess;
	}

	public DocDvij getDocDvij() {
		return docDvij;
	}

	public void setDocDvij(DocDvij docDvij) {
		this.docDvij = docDvij;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public Integer getIdRegistratura() {
		return idRegistratura;
	}

	public void setIdRegistratura(Integer idRegistratura) {
		this.idRegistratura = idRegistratura;
	}

	public Integer getProcId() {
		return procId;
	}

	public void setProcId(Integer procId) {
		this.procId = procId;
	}
}
