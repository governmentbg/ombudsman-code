package com.ib.omb.beans;

import java.io.IOException;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.faces.view.ViewScoped;
import javax.inject.Named;
import javax.xml.bind.JAXBException;

import org.primefaces.event.SelectEvent;
import org.primefaces.event.timeline.TimelineSelectEvent;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;
import org.primefaces.model.timeline.TimelineEvent;
import org.primefaces.model.timeline.TimelineModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.components.CompObjAudit;
import com.ib.omb.db.dto.Delo;
import com.ib.omb.db.dto.DeloArchive;
import com.ib.omb.db.dto.DeloDelo;
import com.ib.omb.db.dto.DeloDoc;
import com.ib.omb.db.dto.DeloDvij;
import com.ib.omb.db.dto.DeloStorage;
import com.ib.omb.db.dto.Doc;
import com.ib.omb.db.dto.DocAccessJournal;
import com.ib.omb.db.dto.DocDoc;
import com.ib.omb.db.dto.DocDvij;
import com.ib.omb.db.dto.DocPril;
import com.ib.omb.db.dto.Task;
import com.ib.omb.experimental.ObjectComparator;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.system.ObjectsDifference;
import com.ib.system.db.dao.JournalDAO;
import com.ib.system.db.dto.FileObject;
import com.ib.system.db.dto.Files;
import com.ib.system.db.dto.SystemJournal;
import com.ib.system.utils.JAXBHelper;


@Named
@ViewScoped
public class TestAudit implements Serializable {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestAudit.class);
	JournalDAO jDAO = new JournalDAO();

	private Integer docID = 3144;
	private List<SystemJournal> docHostory = new ArrayList<SystemJournal>();

//	private TimelineModel<String, ?> model = new TimelineModel<>();
	/**
	 * 
	 */
	private TreeNode rootEvent = new DefaultTreeNode();
	private SystemJournal selectedEvent = null;
	private static final long serialVersionUID = -8799051409655878717L;
	private String selectedIndex = "";
	private SystemJournal previousEvent;
	private List<ObjectsDifference> compare;
	private Integer selectedObjectCode;
	public void actionSearch() {
		LOGGER.info("actionSave was executed");
		  
        
		try {
			docHostory = jDAO.getEvents(docID,selectedObjectCode);
			LOGGER.info("Found " + docHostory.size() + " rows");
			docID = -9;
			// Generate TimeTable for experiment KG
			for (Iterator iterator = docHostory.iterator(); iterator.hasNext();) {
				SystemJournal systemJournal = (SystemJournal) iterator.next();
				Instant instant = systemJournal.getDateAction().toInstant();
//				model.add(TimelineEvent.<String>builder().data(systemJournal.getCodeAction()+" "+systemJournal.getCodeObject()).startDate(LocalDateTime.ofInstant(instant,ZoneId.systemDefault())).build());

				//new DefaultTreeNode(systemJournal, getRootEvent());
		         
		        
				
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	//Избор от мастер таблицата
	public void onRowSelect(SelectEvent<SystemJournal> event) {
		System.out.println("======selected id="+event.getObject().getId());
		setSelectedIndex(event.getObject().getId().toString());
		setSelectedEvent(event.getObject());
		SystemJournal currentEventTmp=getSelectedEvent();
		SystemJournal previousEventTmp = null;
		if (currentEventTmp.getCodeAction()==OmbConstants.CODE_DEIN_IZTRIVANE) {
			//LOGGER.info("There is action remove. Then will search diffs between {} and null",);
			previousEventTmp=currentEventTmp;
			currentEventTmp=null;
		//Ако е запис на нов обект, няма смисъл да търсим редходен
		}else if (currentEventTmp.getCodeAction()!=OmbConstants.CODE_DEIN_ZAPIS) {
			previousEventTmp=findPreviousEvent(currentEventTmp.getId(), currentEventTmp.getCodeObject(),currentEventTmp.getIdObject());
		}
		//Find difs
		List<ObjectsDifference> diffs=new CompObjAudit().loadCurrentDiff(currentEventTmp,previousEventTmp);
		//Load on skreen tree table
		
		setRootEvent(new  DefaultTreeNode());
		if (diffs.size()>0) {
			setRootEvent(new DefaultTreeNode(new ObjectsDifference(), null));
		      
			loadTree(diffs,getRootEvent());
		}
	}
	
	
	//
//	private void loadCurrentDiff(SystemJournal selectedEvent) {
//		SystemJournal currentEventTmp=selectedEvent;
//		SystemJournal previousEventTmp = null;
//		if (currentEventTmp.getCodeAction()==OmbConstants.CODE_DEIN_IZTRIVANE) {
//			//LOGGER.info("There is action remove. Then will search diffs between {} and null",);
//			previousEventTmp=currentEventTmp;
//			currentEventTmp=null;
//		//Ако е запис на нов обект, няма смисъл да търсим редходен
//		}else if (currentEventTmp.getCodeAction()!=OmbConstants.CODE_DEIN_ZAPIS) {
//			previousEventTmp=findPreviousEvent(currentEventTmp.getId(), currentEventTmp.getCodeObject(),currentEventTmp.getIdObject());
//		}
//		setPreviousEvent(previousEventTmp);
//		
//		LOGGER.info("LoadCurrentDiff between {} and {}",currentEventTmp!=null?currentEventTmp.getId():null,previousEventTmp!=null?previousEventTmp.getId():null);
//		
//		try {
////			Object xmlToObject2 = JAXBHelper.xmlToObject2(getSelectedEvent().getObjectXml());
////			System.out.println("==========================="+xmlToObject2.getClass());
//			
//			Object currentObj=null,prevObj=null;
//			
//			switch (getSelectedEvent().getCodeObject()) {
//			case OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC:
//				currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(Doc.class, currentEventTmp.getObjectXml()):new Doc();
//				prevObj=getPreviousEvent()!=null?JAXBHelper.xmlToObject(Doc.class, getPreviousEvent().getObjectXml()):new Doc();
//				break;
//			case OmbConstants.CODE_ZNACHENIE_JOURNAL_DELO:
//				currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(Delo.class, currentEventTmp.getObjectXml()):new Delo();
//				prevObj=getPreviousEvent()!=null?JAXBHelper.xmlToObject(Delo.class, getPreviousEvent().getObjectXml()):new Delo();
//				break;
//			case OmbConstants.CODE_ZNACHENIE_JOURNAL_TASK:
//				currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(Task.class, currentEventTmp.getObjectXml()):new Task();
//				prevObj=getPreviousEvent()!=null?JAXBHelper.xmlToObject(Task.class, getPreviousEvent().getObjectXml()):new Task();
//				break;
//			case OmbConstants.CODE_ZNACHENIE_JOURNAL_FILE:
//				currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(Files.class, currentEventTmp.getObjectXml()):new Files();
//				prevObj=getPreviousEvent()!=null?JAXBHelper.xmlToObject(Files.class, getPreviousEvent().getObjectXml()):new Files();
//				break;
//			case OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC_DVIJ:
//				currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(DocDvij.class, currentEventTmp.getObjectXml()):new DocDvij();
//				prevObj=getPreviousEvent()!=null?JAXBHelper.xmlToObject(DocDvij.class, getPreviousEvent().getObjectXml()):new DocDvij();
//				break;
//			case OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC_DOC:
//				currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(DocDoc.class, currentEventTmp.getObjectXml()):new DocDoc();
//				prevObj=getPreviousEvent()!=null?JAXBHelper.xmlToObject(DocDoc.class, getPreviousEvent().getObjectXml()):new DocDoc();
//				break;
//			case OmbConstants.CODE_ZNACHENIE_JOURNAL_DELO_DOC:
//				currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(DeloDoc.class, currentEventTmp.getObjectXml()):new DeloDoc();
//				prevObj=getPreviousEvent()!=null?JAXBHelper.xmlToObject(DeloDoc.class, getPreviousEvent().getObjectXml()):new DeloDoc();
//				break;
//			case OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC_PRIL:
//				currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(DocPril.class, currentEventTmp.getObjectXml()):new DocPril();
//				prevObj=getPreviousEvent()!=null?JAXBHelper.xmlToObject(DocPril.class, getPreviousEvent().getObjectXml()):new DocPril();
//				break;
//			case OmbConstants.CODE_ZNACHENIE_JOURNAL_IZR_DOST:
//				currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(DocAccessJournal.class, currentEventTmp.getObjectXml()):new DocAccessJournal();
//				prevObj=getPreviousEvent()!=null?JAXBHelper.xmlToObject(DocAccessJournal.class, getPreviousEvent().getObjectXml()):new DocAccessJournal();
//				break;
//			case OmbConstants.CODE_ZNACHENIE_JOURNAL_DELO_ARCHIVE:
//				currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(DeloArchive.class, currentEventTmp.getObjectXml()):new DeloArchive();
//				prevObj=getPreviousEvent()!=null?JAXBHelper.xmlToObject(DeloArchive.class, getPreviousEvent().getObjectXml()):new DeloArchive();
//				break;	
//				
//			case OmbConstants.CODE_ZNACHENIE_JOURNAL_DELO_DVIJ:
//				currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(DeloDvij.class, currentEventTmp.getObjectXml()):new DeloDvij();
//				prevObj=getPreviousEvent()!=null?JAXBHelper.xmlToObject(DeloDvij.class, getPreviousEvent().getObjectXml()):new DeloDvij();
//				break;	
//				
//			case OmbConstants.CODE_ZNACHENIE_JOURNAL_DELO_STORAGE:
//				currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(DeloStorage.class, currentEventTmp.getObjectXml()):new DeloStorage();
//				prevObj=getPreviousEvent()!=null?JAXBHelper.xmlToObject(DeloStorage.class, getPreviousEvent().getObjectXml()):new DeloStorage();
//				break;	
//				
//			case OmbConstants.CODE_ZNACHENIE_JOURNAL_DELO_DELO:
//				currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(DeloDelo.class, currentEventTmp.getObjectXml()):new DeloDelo();
//				prevObj=getPreviousEvent()!=null?JAXBHelper.xmlToObject(DeloDelo.class, getPreviousEvent().getObjectXml()):new DeloDelo();
//				break;
//				
////			case OmbConstants.CODE_ZNACHENIE_JOURNAL_FILE_OBJECT:
////				currentObj=JAXBHelper.xmlToObject(FileObject.class, getSelectedEvent().getObjectXml());
////				prevObj=getPreviousEvent()!=null?JAXBHelper.xmlToObject(FileObject.class, getPreviousEvent().getObjectXml()):new FileObject();
////				break;
//				
//			default:
//				LOGGER.error("Object code="+currentEventTmp.getCodeObject()+" not implemented");
//				break;
//			}
////			Doc currentDoc = JAXBHelper.xmlToObject(Doc.class, getSelectedEvent().getObjectXml());
////			Doc prevDoc = getPreviousEvent()!=null?JAXBHelper.xmlToObject(Doc.class, getPreviousEvent().getObjectXml()):new Doc();
////			
//			setCompare(new ObjectComparator(
//					getPreviousEvent()!=null?getPreviousEvent().getDateAction():new Date(),
//					currentEventTmp!=null?currentEventTmp.getDateAction():new Date(),
//							(SystemData) JSFUtils.getManagedBean("systemData"), 
//							null).compare( prevObj,currentObj));
//			printDiff(getCompare(), null);
//			rootEvent =  new DefaultTreeNode();
//			if (getCompare().size()>0) {
//				setRootEvent(new DefaultTreeNode(new ObjectsDifference(), null));
//			      
//				loadTree(getCompare(),getRootEvent());
//			}
//			
//			
//		} catch (Exception e1) {
//			LOGGER.error("",e1);
//			e1.printStackTrace();
//		} 
//	}
	private void loadTree(List<ObjectsDifference> compared, TreeNode treeNode) {
		for (Iterator iterator = compared.iterator(); iterator.hasNext();) {
			ObjectsDifference objectsDifference = (ObjectsDifference) iterator.next();
			DefaultTreeNode currentReeeNode = new DefaultTreeNode(objectsDifference, treeNode);
			if (objectsDifference.getCoplexDif()!=null && !objectsDifference.getCoplexDif().isEmpty() && objectsDifference.getCoplexDif().size()>0) {
				currentReeeNode.setExpanded(true);
				loadTree(objectsDifference.getCoplexDif(), currentReeeNode);
			}
		}
	}
	
	//Търси предишно събитие от за същия обект
	private SystemJournal findPreviousEvent(Integer id,Integer codeObject, Integer idObject) {
		SystemJournal pEvent=null;
		int pId = -1;
		try {
			for (int i = 0; i < docHostory.size(); i++) {

				SystemJournal systemJournal = docHostory.get(i);
				if (systemJournal.getId() < id && systemJournal.getCodeObject().equals(codeObject) && systemJournal.getIdObject().equals(idObject)) {
					pEvent = (SystemJournal) systemJournal.clone();
				} else if (systemJournal.getId() >= id) {
					break;
				}
			}
		} catch (CloneNotSupportedException e) {
			LOGGER.error("",e);
			e.printStackTrace();
		}
		return pEvent;
	}

	public Integer getDocID() {
		return docID;
	}

	public void setDocID(Integer docID) {
		this.docID = docID;
	}

	public List<SystemJournal> getDocHostory() {
		return docHostory;
	}

	public void setDocHostory(List<SystemJournal> docHostory) {
		this.docHostory = docHostory;
	}

	
	public static void main(String[] args) {

		String s = "1. Законът за изменение и допълнение на Закона за енергетиката (ДВ, бр. 54 от 2012 г.) предвижда мерки по прилагането на Регламент (ЕС, Евратом) № 617/2010 на Съвета от 24 юни 2010 г. относно съобщаването на Комисията на инвестиционни проекти за енергийна инфраструктура в Европейския съюз, както и за отмяна на Регламент (ЕО) № 736/96 (ОВ, L 180/7 от 15 юли 2010 г.).";
		System.out.println(s.indexOf("Регламент (ЕС, ЕВРАТОМ) № 617'/2010"));

	}

	public SystemJournal getSelectedEvent() {
		return selectedEvent;
	}

	public void setSelectedEvent(SystemJournal selectedEvent) {
		this.selectedEvent = selectedEvent;
	}

	public String getSelectedIndex() {
		return selectedIndex;
	}

	public void setSelectedIndex(String selectedIndex) {
		this.selectedIndex = selectedIndex;
	}

	public SystemJournal getPreviousEvent() {
		return previousEvent;
	}

	public void setPreviousEvent(SystemJournal pId) {
		this.previousEvent = pId;
	}
//Принти в конзолата. Да не се използва
public void printDiff (List<ObjectsDifference> result, String space) {
		
		if (space == null) {
			space = "\t";
		}
		
		for (ObjectsDifference diff : result) {
			if (diff == null) {
				//не би трябвало да стане но ...
				continue;
			}
			
			String type = "";
			if (diff.getType() == 3) {
				type = "ИЗТРИВАНЕ";
			}else {
				if (diff.getType() == 2) {
					type = "ПРОМЯНА";
				}else {
					if (diff.getType() == 1) {
						type = "ДОБАВЯНЕ";
					}
				}
			}
			
			
			if (diff.getError() != null) {
				System.out.println(diff.getFieldName() + "\tERROR: " + diff.getError());
			}else {
				
				if (diff.getCoplexDif().size() > 0) {
					//Списък е и има промени
					System.out.println(space + diff.getFieldName() + "\t" + type);
					printDiff(diff.getCoplexDif(), "\t"+space);
				}else {
					
					if (diff.getType() == 3) {
						//Изтриване
						System.out.println(space + diff.getFieldName() + "\t" + type);
					}else {
						if (diff.getType() == 1) {
							//ДОБАВЯНЕ
							System.out.println(space + diff.getFieldName() + "\t" + type);
						}else {
							System.out.println(space + diff.getFieldName() + ":\t" + diff.getOldVal() + " --> " + diff.getNewVal());
						}
					}
					
					
				}
			}
			
		}
	}

public List<ObjectsDifference> getCompare() {
	return compare;
}

public void setCompare(List<ObjectsDifference> compare) {
	this.compare = compare;
}

public TreeNode getRootEvent() {
	return rootEvent;
}

public void setRootEvent(TreeNode rootEvnet) {
	this.rootEvent = rootEvnet;
}

public Integer getSelectedObjectCode() {
	return selectedObjectCode;
}

public void setSelectedObjectCode(Integer selectedObjectCode) {
	this.selectedObjectCode = selectedObjectCode;
}
}
