package com.ib.omb.components;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.faces.component.FacesComponent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.CompObjAuditSys;
import com.ib.indexui.db.dto.AdmGroup;
import com.ib.indexui.db.dto.AdmUser;
import com.ib.indexui.db.dto.StatTable;
import com.ib.indexui.db.dto.UniversalReport;
import com.ib.indexui.report.uni.SprObject;
import com.ib.indexui.system.Constants;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.db.dto.Delo;
import com.ib.omb.db.dto.DeloAccessJournal;
import com.ib.omb.db.dto.DeloArchive;
import com.ib.omb.db.dto.DeloDelo;
import com.ib.omb.db.dto.DeloDoc;
import com.ib.omb.db.dto.DeloDvij;
import com.ib.omb.db.dto.DeloStorage;
import com.ib.omb.db.dto.Doc;
import com.ib.omb.db.dto.DocAccessJournal;
import com.ib.omb.db.dto.DocDestruct;
import com.ib.omb.db.dto.DocDoc;
import com.ib.omb.db.dto.DocDvij;
import com.ib.omb.db.dto.DocMember;
import com.ib.omb.db.dto.DocPril;
import com.ib.omb.db.dto.DocShema;
import com.ib.omb.db.dto.DocVidSetting;
import com.ib.omb.db.dto.DocWSOptions;
import com.ib.omb.db.dto.Event;
import com.ib.omb.db.dto.Praznici;
import com.ib.omb.db.dto.ProcDef;
import com.ib.omb.db.dto.ProcDefEtap;
import com.ib.omb.db.dto.ProcDefTask;
import com.ib.omb.db.dto.ProcExe;
import com.ib.omb.db.dto.ProcExeEtap;
import com.ib.omb.db.dto.ProcExeTask;
import com.ib.omb.db.dto.Referent;
import com.ib.omb.db.dto.ReferentDelegation;
import com.ib.omb.db.dto.Register;
import com.ib.omb.db.dto.Registratura;
import com.ib.omb.db.dto.RegistraturaGroup;
import com.ib.omb.db.dto.RegistraturaMailBox;
import com.ib.omb.db.dto.RegistraturaReferent;
import com.ib.omb.db.dto.RegistraturaSetting;
import com.ib.omb.db.dto.Task;
import com.ib.omb.db.dto.TaskSchedule;
import com.ib.omb.db.dto.UserNotifications;
import com.ib.omb.experimental.ObjectComparator;
import com.ib.omb.search.DeloSearch;
import com.ib.omb.search.DocSearch;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.system.ObjectsDifference;
import com.ib.system.SysConstants;
import com.ib.system.db.dto.Files;
import com.ib.system.db.dto.SyslogicListEntity;
import com.ib.system.db.dto.SyslogicListOpisEntity;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.db.dto.SystemClassifOpis;
import com.ib.system.db.dto.SystemJournal;
import com.ib.system.db.dto.SystemOption;
import com.ib.system.utils.JAXBHelper;

/** */
@FacesComponent(value = "compObjAudit", createTag = true)
public class CompObjAudit extends CompObjAuditSys {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(CompObjAudit.class);

	
	/**
	 * Зарежда текущите разликите - сегашно и предишно състояние
	 * @param selectedEvent
	 */
	@Override
	public List<ObjectsDifference> loadCurrentDiff(SystemJournal currentEventTmp,SystemJournal previousEventTmp) {
		List<ObjectsDifference> compareResult=new ArrayList<ObjectsDifference>(); 
		
		
		LOGGER.info("LoadCurrentDiff between {} and {}",currentEventTmp!=null?currentEventTmp.getId():null,previousEventTmp!=null?previousEventTmp.getId():null);
		
		try {
						
//			Object xmlToObject2 = JAXBHelper.xmlToObject2(getSelectedEvent().getObjectXml());
//			System.out.println("==========================="+xmlToObject2.getClass());
			
			Object currentObj=null,prevObj=null;
			Integer codeObject=currentEventTmp!=null?currentEventTmp.getCodeObject():previousEventTmp.getCodeObject();
			Integer codeAction=currentEventTmp!=null?currentEventTmp.getCodeAction():previousEventTmp.getCodeAction();
			
			if (codeAction != null && codeAction.equals(OmbConstants.CODE_DEIN_UNISEARCH)) {
				if (previousEventTmp.getObjectXml() != null) {
					SprObject spr = JAXBHelper.xmlToObject(SprObject.class, previousEventTmp.getObjectXml());
					
					return convertSprObjectToDifferences(spr, getSystemData(), previousEventTmp.getDateAction()) ;
				}
			}
			
			
			switch (codeObject) {
				case OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC:
					if (codeAction != null && codeAction.equals(OmbConstants.CODE_DEIN_SEARCH)) {
						currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(DocSearch.class, currentEventTmp.getObjectXml()):new DocSearch();
						prevObj=previousEventTmp!=null?JAXBHelper.xmlToObject(DocSearch.class, previousEventTmp.getObjectXml()):new DocSearch();
					}else {
						currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(Doc.class, currentEventTmp.getObjectXml()):new Doc();
						prevObj=previousEventTmp!=null?JAXBHelper.xmlToObject(Doc.class, previousEventTmp.getObjectXml()):new Doc();
					}
					break;
				case OmbConstants.CODE_ZNACHENIE_JOURNAL_DELO:
					if (codeAction != null && codeAction.equals(OmbConstants.CODE_DEIN_SEARCH)) {
						currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(DeloSearch.class, currentEventTmp.getObjectXml()):new DeloSearch();
						prevObj=previousEventTmp!=null?JAXBHelper.xmlToObject(DeloSearch.class, previousEventTmp.getObjectXml()):new DeloSearch();
					}else {
						currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(Delo.class, currentEventTmp.getObjectXml()):new Delo();
						prevObj=previousEventTmp!=null?JAXBHelper.xmlToObject(Delo.class, previousEventTmp.getObjectXml()):new Delo();
					}
					break;
				case OmbConstants.CODE_ZNACHENIE_JOURNAL_TASK:
					currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(Task.class, currentEventTmp.getObjectXml()):new Task();
					prevObj=previousEventTmp!=null?JAXBHelper.xmlToObject(Task.class, previousEventTmp.getObjectXml()):new Task();
					break;
				case OmbConstants.CODE_ZNACHENIE_JOURNAL_FILE:
					currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(Files.class, currentEventTmp.getObjectXml()):new Files();
					prevObj=previousEventTmp!=null?JAXBHelper.xmlToObject(Files.class, previousEventTmp.getObjectXml()):new Files();
					break;
				case OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC_DVIJ:
					currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(DocDvij.class, currentEventTmp.getObjectXml()):new DocDvij();
					prevObj=previousEventTmp!=null?JAXBHelper.xmlToObject(DocDvij.class, previousEventTmp.getObjectXml()):new DocDvij();
					break;
				case OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC_DOC:
					currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(DocDoc.class, currentEventTmp.getObjectXml()):new DocDoc();
					prevObj=previousEventTmp!=null?JAXBHelper.xmlToObject(DocDoc.class, previousEventTmp.getObjectXml()):new DocDoc();
					break;
				case OmbConstants.CODE_ZNACHENIE_JOURNAL_DELO_DOC:
					currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(DeloDoc.class, currentEventTmp.getObjectXml()):new DeloDoc();
					prevObj=previousEventTmp!=null?JAXBHelper.xmlToObject(DeloDoc.class, previousEventTmp.getObjectXml()):new DeloDoc();
					break;
				case OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC_PRIL:
					currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(DocPril.class, currentEventTmp.getObjectXml()):new DocPril();
					prevObj=previousEventTmp!=null?JAXBHelper.xmlToObject(DocPril.class, previousEventTmp.getObjectXml()):new DocPril();
					break;
				case OmbConstants.CODE_ZNACHENIE_JOURNAL_IZR_DOST:
					currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(DocAccessJournal.class, currentEventTmp.getObjectXml()):new DocAccessJournal();
					prevObj=previousEventTmp!=null?JAXBHelper.xmlToObject(DocAccessJournal.class, previousEventTmp.getObjectXml()):new DocAccessJournal();
					break;
				case OmbConstants.CODE_ZNACHENIE_JOURNAL_DELO_ARCHIVE:
					currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(DeloArchive.class, currentEventTmp.getObjectXml()):new DeloArchive();
					prevObj=previousEventTmp!=null?JAXBHelper.xmlToObject(DeloArchive.class, previousEventTmp.getObjectXml()):new DeloArchive();
					break;	
				case OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC_DESTRUCT:
					currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(DocDestruct.class, currentEventTmp.getObjectXml()):new DocDestruct();
					prevObj=previousEventTmp!=null?JAXBHelper.xmlToObject(DocDestruct.class, previousEventTmp.getObjectXml()):new DocDestruct();
					break;	
				case OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC_MEMBER:
					currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(DocMember.class, currentEventTmp.getObjectXml()):new DocMember();
					prevObj=previousEventTmp!=null?JAXBHelper.xmlToObject(DocMember.class, previousEventTmp.getObjectXml()):new DocMember();
					break;	
				case OmbConstants.CODE_ZNACHENIE_JOURNAL_TASK_SCHEDULE:
					currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(TaskSchedule.class, currentEventTmp.getObjectXml()):new TaskSchedule();
					prevObj=previousEventTmp!=null?JAXBHelper.xmlToObject(TaskSchedule.class, previousEventTmp.getObjectXml()):new TaskSchedule();
					break;	
					
				case OmbConstants.CODE_ZNACHENIE_JOURNAL_DELO_DVIJ:
					currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(DeloDvij.class, currentEventTmp.getObjectXml()):new DeloDvij();
					prevObj=previousEventTmp!=null?JAXBHelper.xmlToObject(DeloDvij.class, previousEventTmp.getObjectXml()):new DeloDvij();
					break;	
					
				case OmbConstants.CODE_ZNACHENIE_JOURNAL_DELO_STORAGE:
					currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(DeloStorage.class, currentEventTmp.getObjectXml()):new DeloStorage();
					prevObj=previousEventTmp!=null?JAXBHelper.xmlToObject(DeloStorage.class, previousEventTmp.getObjectXml()):new DeloStorage();
					break;	
					
				case OmbConstants.CODE_ZNACHENIE_JOURNAL_DELO_DELO:
					currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(DeloDelo.class, currentEventTmp.getObjectXml()):new DeloDelo();
					prevObj=previousEventTmp!=null?JAXBHelper.xmlToObject(DeloDelo.class, previousEventTmp.getObjectXml()):new DeloDelo();
					break;
	
				case OmbConstants.CODE_ZNACHENIE_JOURNAL_REISTRATURA:
					currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(Registratura.class, currentEventTmp.getObjectXml()):new Registratura();
					prevObj=previousEventTmp!=null?JAXBHelper.xmlToObject(Registratura.class, previousEventTmp.getObjectXml()):new Registratura();
					break;
				case OmbConstants.CODE_ZNACHENIE_JOURNAL_REGISTER:
					currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(Register.class, currentEventTmp.getObjectXml()):new Register();
					prevObj=previousEventTmp!=null?JAXBHelper.xmlToObject(Register.class, previousEventTmp.getObjectXml()):new Register();
					break;
				case OmbConstants.CODE_ZNACHENIE_JOURNAL_REISTRATURA_SETT:
					currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(RegistraturaSetting.class, currentEventTmp.getObjectXml()):new RegistraturaSetting();
					prevObj=previousEventTmp!=null?JAXBHelper.xmlToObject(RegistraturaSetting.class, previousEventTmp.getObjectXml()):new RegistraturaSetting();
					break;
				case OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC_VID_SETT:
					currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(DocVidSetting.class, currentEventTmp.getObjectXml()):new DocVidSetting();
					prevObj=previousEventTmp!=null?JAXBHelper.xmlToObject(DocVidSetting.class, previousEventTmp.getObjectXml()):new DocVidSetting();
					break;
				case OmbConstants.CODE_ZNACHENIE_JOURNAL_MAILBOX:
					currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(RegistraturaMailBox.class, currentEventTmp.getObjectXml()):new RegistraturaMailBox();
					prevObj=previousEventTmp!=null?JAXBHelper.xmlToObject(RegistraturaMailBox.class, previousEventTmp.getObjectXml()):new RegistraturaMailBox();
					break;
				case OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC_WS_OPT:
					currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(DocWSOptions.class, currentEventTmp.getObjectXml()):new DocWSOptions();
					prevObj=previousEventTmp!=null?JAXBHelper.xmlToObject(DocWSOptions.class, previousEventTmp.getObjectXml()):new DocWSOptions();
					break;
	
				case OmbConstants.CODE_ZNACHENIE_JOURNAL_REISTRATURA_GROUP:
					currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(RegistraturaGroup.class, currentEventTmp.getObjectXml()):new RegistraturaGroup();
					prevObj=previousEventTmp!=null?JAXBHelper.xmlToObject(RegistraturaGroup.class, previousEventTmp.getObjectXml()):new RegistraturaGroup();
					break;
				case OmbConstants.CODE_ZNACHENIE_JOURNAL_REG_GROUP_CORRESP:
					currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(RegistraturaReferent.class, currentEventTmp.getObjectXml()):new RegistraturaReferent();
					prevObj=previousEventTmp!=null?JAXBHelper.xmlToObject(RegistraturaReferent.class, previousEventTmp.getObjectXml()):new RegistraturaReferent();
					break;
					
				case OmbConstants.CODE_ZNACHENIE_JOURNAL_USER:
					currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(AdmUser.class, currentEventTmp.getObjectXml()):new AdmUser();
					prevObj=previousEventTmp!=null?JAXBHelper.xmlToObject(AdmUser.class, previousEventTmp.getObjectXml()):new AdmUser();
					break;
					
				case OmbConstants.CODE_ZNACHENIE_JOURNAL_PROC_DEF:
					currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(ProcDef.class, currentEventTmp.getObjectXml()):new ProcDef();
					prevObj=previousEventTmp!=null?JAXBHelper.xmlToObject(ProcDef.class, previousEventTmp.getObjectXml()):new ProcDef();
					break;
				
				case OmbConstants.CODE_ZNACHENIE_JOURNAL_PROC_DEF_ETAP:
					currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(ProcDefEtap.class, currentEventTmp.getObjectXml()):new ProcDefEtap();
					prevObj=previousEventTmp!=null?JAXBHelper.xmlToObject(ProcDefEtap.class, previousEventTmp.getObjectXml()):new ProcDefEtap();
					break;
				case OmbConstants.CODE_ZNACHENIE_JOURNAL_PROC_DEF_TASK:
					currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(ProcDefTask.class, currentEventTmp.getObjectXml()):new ProcDefTask();
					prevObj=previousEventTmp!=null?JAXBHelper.xmlToObject(ProcDefTask.class, previousEventTmp.getObjectXml()):new ProcDefTask();
					break;
	
				case OmbConstants.CODE_ZNACHENIE_JOURNAL_REFERENT:
					currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(Referent.class, currentEventTmp.getObjectXml()):new Referent();
					prevObj=previousEventTmp!=null?JAXBHelper.xmlToObject(Referent.class, previousEventTmp.getObjectXml()):new Referent();
					break;
				case OmbConstants.CODE_ZNACHENIE_JOURNAL_DELEGATION:
					currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(ReferentDelegation.class, currentEventTmp.getObjectXml()):new ReferentDelegation();
					prevObj=previousEventTmp!=null?JAXBHelper.xmlToObject(ReferentDelegation.class, previousEventTmp.getObjectXml()):new ReferentDelegation();
					break;
	
				case OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC_SHEMA:
					currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(DocShema.class, currentEventTmp.getObjectXml()):new DocShema();
					prevObj=previousEventTmp!=null?JAXBHelper.xmlToObject(DocShema.class, previousEventTmp.getObjectXml()):new DocShema();
					break;
	
				case SysConstants.CODE_ZNACHENIE_JOURNAL_OPTION:
					currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(SystemOption.class, currentEventTmp.getObjectXml()):new SystemOption();
					prevObj=previousEventTmp!=null?JAXBHelper.xmlToObject(SystemOption.class, previousEventTmp.getObjectXml()):new SystemOption();
					break;
			
				case OmbConstants.CODE_ZNACHENIE_JOURNAL_EVENT:
					currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(Event.class, currentEventTmp.getObjectXml()):new Event();
					prevObj=previousEventTmp!=null?JAXBHelper.xmlToObject(Event.class, previousEventTmp.getObjectXml()):new Event();
					break;
	
				case Constants.CODE_ZNACHENIE_JOURNAL_STAT_TABLE:
					currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(StatTable.class, currentEventTmp.getObjectXml()):new StatTable();
					prevObj=previousEventTmp!=null?JAXBHelper.xmlToObject(StatTable.class, previousEventTmp.getObjectXml()):new StatTable();
					break;
				case Constants.CODE_ZNACHENIE_JOURNAL_UNI_REPORT:
					currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(UniversalReport.class, currentEventTmp.getObjectXml()):new UniversalReport();
					prevObj=previousEventTmp!=null?JAXBHelper.xmlToObject(UniversalReport.class, previousEventTmp.getObjectXml()):new UniversalReport();
					break;
	
				case SysConstants.CODE_ZNACHENIE_JOURNAL_OPIS:
					currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(SystemClassifOpis.class, currentEventTmp.getObjectXml()):new SystemClassifOpis();
					prevObj=previousEventTmp!=null?JAXBHelper.xmlToObject(SystemClassifOpis.class, previousEventTmp.getObjectXml()):new SystemClassifOpis();
					break;
				case SysConstants.CODE_ZNACHENIE_JOURNAL_CLASSIF:
					currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(SystemClassif.class, currentEventTmp.getObjectXml()):new SystemClassif();
					prevObj=previousEventTmp!=null?JAXBHelper.xmlToObject(SystemClassif.class, previousEventTmp.getObjectXml()):new SystemClassif();
					break;
				case SysConstants.CODE_ZNACHENIE_JOURNAL_LIST:
					currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(SyslogicListOpisEntity.class, currentEventTmp.getObjectXml()):new SyslogicListOpisEntity();
					prevObj=previousEventTmp!=null?JAXBHelper.xmlToObject(SyslogicListOpisEntity.class, previousEventTmp.getObjectXml()):new SyslogicListOpisEntity();
					break;
				case SysConstants.CODE_ZNACHENIE_JOURNAL_LISTROW:
					currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(SyslogicListEntity.class, currentEventTmp.getObjectXml()):new SyslogicListEntity();
					prevObj=previousEventTmp!=null?JAXBHelper.xmlToObject(SyslogicListEntity.class, previousEventTmp.getObjectXml()):new SyslogicListEntity();
					break;
				case OmbConstants.CODE_ZNACHENIE_JOURNAL_IZR_DOST_DELO:
					currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(DeloAccessJournal.class, currentEventTmp.getObjectXml()):new DeloAccessJournal();
					prevObj=previousEventTmp!=null?JAXBHelper.xmlToObject(DeloAccessJournal.class, previousEventTmp.getObjectXml()):new DeloAccessJournal();
					break;
	
				case OmbConstants.CODE_ZNACHENIE_JOURNAL_PROC_EXE:
					currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(ProcExe.class, currentEventTmp.getObjectXml()):new ProcExe();
					prevObj=previousEventTmp!=null?JAXBHelper.xmlToObject(ProcExe.class, previousEventTmp.getObjectXml()):new ProcExe();
					break;
				case OmbConstants.CODE_ZNACHENIE_JOURNAL_PROC_EXE_ETAP:
					currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(ProcExeEtap.class, currentEventTmp.getObjectXml()):new ProcExeEtap();
					prevObj=previousEventTmp!=null?JAXBHelper.xmlToObject(ProcExeEtap.class, previousEventTmp.getObjectXml()):new ProcExeEtap();
					break;
				case OmbConstants.CODE_ZNACHENIE_JOURNAL_PROC_EXE_TASK:
					currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(ProcExeTask.class, currentEventTmp.getObjectXml()):new ProcExeTask();
					prevObj=previousEventTmp!=null?JAXBHelper.xmlToObject(ProcExeTask.class, previousEventTmp.getObjectXml()):new ProcExeTask();
					break;
	
				case OmbConstants.CODE_ZNACHENIE_JOURNAL_PRAZNIK:
					currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(Praznici.class, currentEventTmp.getObjectXml()):new Praznici();
					prevObj=previousEventTmp!=null?JAXBHelper.xmlToObject(Praznici.class, previousEventTmp.getObjectXml()):new Praznici();
					break;
					
				case OmbConstants.CODE_ZNACHENIE_JOURNAL_GROUPUSER:
					currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(AdmGroup.class, currentEventTmp.getObjectXml()):new AdmGroup();
					prevObj=previousEventTmp!=null?JAXBHelper.xmlToObject(AdmGroup.class, previousEventTmp.getObjectXml()):new AdmGroup();
					break;
	
				case OmbConstants.CODE_ZNACHENIE_JOURNAL_NOTIF: // журналира се само изтриването на нотификация
					currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(UserNotifications.class, currentEventTmp.getObjectXml()):new UserNotifications();
					prevObj=previousEventTmp!=null?JAXBHelper.xmlToObject(UserNotifications.class, previousEventTmp.getObjectXml()):new UserNotifications();
					break;
	
	//			case OmbConstants.CODE_ZNACHENIE_JOURNAL_FILE_OBJECT:
	//				currentObj=JAXBHelper.xmlToObject(FileObject.class, getSelectedEvent().getObjectXml());
	//				prevObj=previousEventTmp!=null?JAXBHelper.xmlToObject(FileObject.class, previousEventTmp.getObjectXml()):new FileObject();
	//				break;
					
				default:
					LOGGER.error("Object code="+currentEventTmp.getCodeObject()+" not implemented");
					break;
			}
//			Doc currentDoc = JAXBHelper.xmlToObject(Doc.class, getSelectedEvent().getObjectXml());
//			Doc prevDoc = previousEventTmp!=null?JAXBHelper.xmlToObject(Doc.class, previousEventTmp.getObjectXml()):new Doc();
//			
			 compareResult = new ObjectComparator(
					previousEventTmp!=null?previousEventTmp.getDateAction():new Date(),
					currentEventTmp!=null?currentEventTmp.getDateAction():new Date(),
							(SystemData) JSFUtils.getManagedBean("systemData"), 
							null).compare( prevObj,currentObj);
			 
			 
			
			 
			
		} catch (Exception e1) {
			LOGGER.error("",e1);
			e1.printStackTrace();
		} 
		return compareResult;

	}
	


	
	
}