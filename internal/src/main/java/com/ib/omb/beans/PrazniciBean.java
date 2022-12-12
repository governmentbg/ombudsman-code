package com.ib.omb.beans;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.inject.Named;

import org.omnifaces.cdi.ViewScoped;
import org.primefaces.PrimeFaces;
import org.primefaces.event.SelectEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.db.dao.PrazniciDAO;
import com.ib.omb.db.dto.Praznici;
import com.ib.omb.system.SystemData;
import com.ib.system.db.JPA;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.DbErrorException;


/** 
 * @author yonchoy*/

@Named
@ViewScoped
public class PrazniciBean  extends IndexUIbean implements Serializable {
	
	private static final long serialVersionUID = 1277161116379674871L;
	private static final Logger LOGGER = LoggerFactory.getLogger(PrazniciBean.class);
	
	public static final String UIBEANMESSAGES = "ui_beanMessages";
	public static final String ERRDATABASEMSG = "general.errDataBaseMsg";
	public static final String SUCCESSAVEMSG = "general.succesSaveMsg";
	public static final String SUCCESDELETEMSG = "general.succesDeleteMsg";

	private List<Date> multi= new ArrayList<>();// за множествения избор на дати от компонентата
	private List<Date> officialHolidays=new ArrayList<>();// тук ще се зареждат официалните празниц като дати
	private String holidaysDates; // тук ще се държат празниците, който ще се предават на страницата
	private List<Date> datesInDB = new ArrayList<>(); // датите вече записани в базата данни
	
	// Ако искаме календара да има дати оцветени с различни цветове, добавя още една колона за вид дата в БД и съобразно нея променяме цвета. 
	//В побитовата матрица за празниците на страницата вместо 1 слагаме вида на датата, а в template функцията оказваме цвета.
	private String colorDate="#81C784";
	private int yearCalendar;
	
	
	//за модалния панел
	private List<Date> datesModal = new ArrayList<>();
	private int yearModal=0;
	
	
	@PostConstruct
	public void initData() {
		PrazniciDAO dao = new PrazniciDAO(getUserData());
		int year = Calendar.getInstance().get(Calendar.YEAR);
		this.yearCalendar=year;
		this.yearModal=(yearModal!=0?this.yearModal:year);
		try {
			List<Date> days = dao.getHolidaysInYear(year);
			this.datesInDB.clear();
			this.datesInDB.addAll(days);
			this.datesModal.clear();
			this.datesModal.addAll(days);
			
			//holidays.clear();

			// for(Date d: days) { this.holidays.add(d.getTime()); }
			// System.out.println("Holidays "+Arrays.toString(holidays.toArray()));

			this.holidaysDates=loadDateData(days);
		} catch (DbErrorException e) {
			LOGGER.error(e.getMessage(), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
					getMessageResourceString(UIBEANMESSAGES, ERRDATABASEMSG), e.getMessage());
		}
	} 
	
	public String loadModalHolidays() {
		PrazniciDAO dao = new PrazniciDAO(getUserData());
		try {
			//System.out.println("this.yearModal "+yearModal);
			//this.yearModal=2022;
			//System.out.println("this.yearModal "+yearModal);
			List<Date> days = dao.getHolidaysInYear(yearModal);
			this.datesModal.clear();
			this.datesModal.addAll(days);
		} catch (DbErrorException e) {
			LOGGER.error(e.getMessage(), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
					getMessageResourceString(UIBEANMESSAGES, ERRDATABASEMSG), e.getMessage());
		}
		return "";
	}
	
	
	public void loadDatesDB() {
		PrazniciDAO dao = new PrazniciDAO(getUserData());
		try {
			List<Date> days = dao.getHolidaysInYear(this.yearCalendar);
			this.datesInDB.clear();
			this.datesInDB.addAll(days);
			this.holidaysDates=loadDateData(days);
		} catch (DbErrorException e) {
			LOGGER.error(e.getMessage(), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
					getMessageResourceString(UIBEANMESSAGES, ERRDATABASEMSG), e.getMessage());
		}
	}
	
	/**
	 * Метод за зареждане на определена дата в календара
	 */
	public Date setParticularDate(int day, int month, int year) {
		Calendar cal=Calendar.getInstance();
		cal.set(Calendar.YEAR, year);
		cal.set(Calendar.MONTH, month);
		cal.set(Calendar.DAY_OF_MONTH, day);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		return cal.getTime();
	}
	
	public void refreshData() {
		loadDatesDB();
		//PrimeFaces.current().executeScript("data="+this.holidaysDates+";");
		System.out.println("holidaysDates:   "+ this.holidaysDates);
		getSystemData(SystemData.class).setPraznici(null); // за рефреш на кеша
		PrimeFaces.current().executeScript("loadData('"+this.holidaysDates+"');");
	}
	
	/**
	 * функция, която зарежда официалните празници в страната:
	 *  1 януари  – Нова година.
		3 март – Ден на Освобождението на България от османско робство.
		1 май – Ден на труда и на международната работническа солидарност.
		6 май – Гергьовден, Ден на храбростта и Българската армия.
		24 май – Денят на българската просвета и култура и на славянската писменост.
		6 септември – Съединението на България.
		22 септември – Ден на независимостта на България.
		24, 25, 26 декември – Коледа
		Великденските празници датата е плаваща
	 */
	public void loadOfficialholidays() {
		PrazniciDAO dao = new PrazniciDAO(getUserData());
		Calendar cal = Calendar.getInstance();
		this.officialHolidays.clear();
		int year = cal.get(Calendar.YEAR);
		int[] months = { 0, 2, 4, 4, 4, 8, 8, 11, 11, 11 };
		int[] days = { 1, 3, 1, 6, 24, 6, 22, 24, 25, 26 };
		for (int i = 0; i < days.length; i++) {
			Date d = setParticularDate(days[i], months[i], year);
			System.out.println(d.toString());
			this.officialHolidays.add(d);
		}
		this.colorDate="#F0240A";
		// махаме от официалните празници дати, ако има колизия с вече записаните в базата данни
		this.officialHolidays = avoidDoubleRecords(this.datesInDB, this.officialHolidays);
		if (!this.officialHolidays.isEmpty()) {
			try {
				JPA.getUtil().runInTransaction(() -> {
					for (Date d : this.officialHolidays) {
						dao.save(new Praznici(d));
					}
				});
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO,
						getMessageResourceString(UIBEANMESSAGES, SUCCESSAVEMSG));
				
				loadModalHolidays();
				refreshData();
			} catch (DbErrorException e) {
				LOGGER.error(e.getMessage(), e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			} catch (BaseException e) {
				LOGGER.error("Грешка при запис на празник! ", e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
						getMessageResourceString(UIBEANMESSAGES, ERRDATABASEMSG), e.getMessage());
			}
		}

	}
	
	/**
	 * Метод, с който избягваме записа на една и съща дата повече от веднъж като празник
	 */
	public List<Date> avoidDoubleRecords(List<Date> datesInDB, List<Date> list) {
		for(int i=0;i<list.size();++i) {
			for(int j=0;j<datesInDB.size();++j) {
				if(compareDates(datesInDB.get(j), list.get(i))) {
					list.remove(i);
				}
			}
		}
		return list;
	}
	
	/** По-добър метод за сравнение на две дати с точност до ден, подобен на DateUtils.sameStartDate
	 */
	public boolean compareDates(Date d1, Date d2) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		try {
			d1 = sdf.parse(sdf.format(d1));
			d2 = sdf.parse(sdf.format(d2));
		} catch (ParseException e) {
			LOGGER.error(e.getMessage(), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
		}
		
		return d1.equals(d2);
	}
	
	
	/**
	 * Метод преобразуващ списък от дати в стринг, форматиран подходящо за обработка на страницата
	 */
	public String loadDateData(List<Date> days){
		StringBuilder sb=new StringBuilder("");
		Calendar cal = Calendar.getInstance();
		for(Date d: days) {
			cal.setTime(d);
			if(!sb.toString().isEmpty()) {
				sb.append(",");
			}
			sb.append(cal.get(Calendar.MONTH)).append(",").append(cal.get(Calendar.DAY_OF_MONTH));
		}
		String res=sb.toString();
		return res;
	}
	
	/**
	 * Метод на запис на нови дати като празници
	 */
	public void actionSave() {
		PrazniciDAO dao = new PrazniciDAO(getUserData());
		// махаме от избраните дати, ако има колизия с вече записаните в базата данни
		this.multi = avoidDoubleRecords(this.datesInDB, this.multi);
		this.colorDate="#81c784";
		//System.out.println("Multi size " + multi.size());
		if (!this.multi.isEmpty()) {
			
			try {
				JPA.getUtil().runInTransaction(() -> {
					for (Date d : this.multi) {
						dao.save(new Praznici(d));
					}
				});
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO,
						getMessageResourceString(UIBEANMESSAGES, SUCCESSAVEMSG));
				loadModalHolidays();
				refreshData();
			} catch (DbErrorException e) {
				LOGGER.error(e.getMessage(), e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			} catch (BaseException e) {
				LOGGER.error("Грешка при запис на празник! ", e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
						getMessageResourceString(UIBEANMESSAGES, ERRDATABASEMSG), e.getMessage());
			}
		}

	}
	
	/**
	 * Метод за изтриване на празници
	 */
	public void actionDelete() {
		PrazniciDAO dao = new PrazniciDAO(getUserData());
		try {
			
			JPA.getUtil().runInTransaction(() -> {
				for(Date d:this.multi) {
					if(this.datesInDB.contains(d)) {
						dao.delete(d);
					}
				}
			});
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO,
					getMessageResourceString(UIBEANMESSAGES, SUCCESDELETEMSG));
			loadModalHolidays();
			refreshData();
		} catch (DbErrorException e) {
			LOGGER.error(e.getMessage(), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
		} catch (BaseException e) {
			LOGGER.error("Грешка при изтриване на празник! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
					getMessageResourceString(UIBEANMESSAGES, ERRDATABASEMSG), e.getMessage());
		}
	}


	public PrazniciBean() {
		super();
	}

	public List<Date> getMulti() {
		return multi;
	}

	public void setMulti(List<Date> multi) {
		this.multi = multi;
	}

	public String getHolidaysDates() {
		return holidaysDates;
	}

	public void setHolidaysDates(String holidaysDates) {
		this.holidaysDates = holidaysDates;
	}

	public List<Date> getOfficialHolidays() {
		return officialHolidays;
	}

	public void setOfficialHolidays(List<Date> officialHolidays) {
		this.officialHolidays = officialHolidays;
	}

	public List<Date> getDatesInDB() {
		return datesInDB;
	}

	public void setDatesInDB(List<Date> datesInDB) {
		this.datesInDB = datesInDB;
	}


	public List<Date> getDatesModal() {
		return datesModal;
	}


	public void setDatesModal(List<Date> datesModal) {
		this.datesModal = datesModal;
	}


	public String getColorDate() {
		return colorDate;
	}


	public void setColorDate(String colorDate) {
		this.colorDate = colorDate;
	}


	public int getYearCalendar() {
		return yearCalendar;
	}


	public void setYearCalendar(int yearCalendar) {
		this.yearCalendar = yearCalendar;
	}


	public int getYearModal() {
		return yearModal;
	}


	public void setYearModal(int yearModal) {
		this.yearModal = yearModal;
	}

}
