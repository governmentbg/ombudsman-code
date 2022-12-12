package com.ib.omb.beans;

import java.io.Serializable;
import java.lang.Thread.State;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
//import javax.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.beans.SystemClassifList;
import com.ib.indexui.system.IndexUIbean;
import com.ib.omb.search.IndexThread;

@Named(value = "fullTextIndexesReload")
@ViewScoped
public class FullTextIndexesReload extends IndexUIbean implements Serializable {
	/**
	 * Индексиране
	 * 
	 * @author l.varbanov
	 */
	private static final long serialVersionUID = -1797923728263798891L;

	/*
	 * Съхранява всеки обект на индексиране в HashMap от име на обекта и масив от
	 * "дата от"[0], "дата до"[1]на обекта При необходимост от добавяне на нови
	 * обекти за индексиране е неодходимо те да бъдат добавени като Object[] в
	 * initData() и като елементи на листа в getValues().
	 */

	static final Logger LOGGER = LoggerFactory.getLogger(SystemClassifList.class);
	private static Map<String, Object[]> indexProps = new HashMap<String, Object[]>();
	private static Map<String, IndexThread> threads = new HashMap<String, IndexThread>();
	private static Date dateFrom;
	private static Date dateTo;
	private String status;
	private static boolean isDateFromChanged;
	private static boolean isDateToChanged;

	@PostConstruct
	public void initData() {
		Object[] doc = new Object[2];
		Object[] delo = new Object[2];
		Object[] task = new Object[2];

		indexProps.putIfAbsent("Документ", doc);
		indexProps.putIfAbsent("Преписка/Дело", delo);
		indexProps.putIfAbsent("Задача", task);


		setDateFrom(null);
		setDateTo(null);
		setDateFromChanged(false);
		setDateToChanged(false);

		clearIndexDate("Документ");
		clearIndexDate("Преписка/Дело");
		clearIndexDate("Задача");
	}

	public List<String> getValues() {
		List<String> list = Arrays.asList("Документ", "Преписка/Дело", "Задача");
		return list;
	}

	public static Object[] findIndex(String name) {
		Object[] props = indexProps.get(name);
		return props;
	}

	@SuppressWarnings("static-access")
	public void setIndexDate(String name) {
		Object[] current = findIndex(name);
		
		if(isDateFromChanged) {
			current[0] = this.dateFrom;
		}else if(isDateToChanged) {
			current[1] = this.dateTo;
		}
		setDateFromChanged(false);
		setDateToChanged(false);
		
		indexProps.put(name, current);
		LOGGER.info("\r\nTHREAD " + name);
		LOGGER.info("DateFrom: " + current[0]);
		LOGGER.info("DateTo: " + current[1]);
	}

	// Проверява статуса на нишката за избрания ред
	public String getIndexStatus(String indexName) {
		IndexThread th = getThreadFor(indexName);
		String status = th.getState().toString();

		if (status.equals("TERMINATED") || status.equals("NEW")) {
			status = "Неактивно";
		} else {
			status = "Активно";
		}
		setStatus(status);

		return status;
	}

	/*
	 * Проверява дали има вече създадена нишка за избрания ред: 1. ако няма създава
	 * нова 2. ако има, но е със статус TERMINATED пак създава нова 3. в останалите
	 * случаи връща нишката
	 */
	public static IndexThread getThreadFor(String name) {
		IndexThread th;
		Object[] index = findIndex(name);
		Date dateFrom = (Date) index[0];
		Date dateTo = (Date) index[1];
		synchronized (threads) {
			if (threads.containsKey(name)) {
				th = threads.get(name);
				if (th.getState().toString().equals("TERMINATED")) {
					th = new IndexThread(dateFrom, dateTo, name);
				}
				if (isDateFromChanged) {
					th.setDateFrom(dateFrom);
				}
				if (isDateToChanged) {
					th.setDateTo(dateTo);
				}
				setDateFromChanged(false);
				setDateToChanged(false);
			} else {
				th = new IndexThread(dateFrom, dateTo, name);
				LOGGER.info("---------------THREAD CREATED-----------------");
			}
			threads.put(name, th);
		}
		return th;
	}

	// Проверява дали в момента протича индексиране за избрания ред
	public boolean isRunning(String name) {
		IndexThread th = getThreadFor(name);
		String threadStatus = th.getState().toString();
		if (threadStatus.equals("TERMINATED") || threadStatus.equals("NEW")) {
			return false;
		}
		return true;
	}

	// Стартира нишката за избраният индекс
	public void actionStartThread(String indexName) throws InterruptedException {
		IndexThread th = getThreadFor(indexName);
		threads.put(indexName, th);
		LOGGER.info("--------START THREAD-----" + indexName);

		if (th.getState().toString().equals("NEW")) {
			th.start();
		} else if (th.getState().toString().equals("TERMINATED")) {
			LOGGER.error("Метод getThreadFor() не е създал нова инстанция на нишка със статус TERMINATED!");
		} else {
			return;
		}
		setStatus(th.getState().toString());
	}

	// Чисти датата на индекса
	public void clearIndexDate(String name) {
		Object[] current = findIndex(name);
		current[0] = null;
		current[1] = null;
		indexProps.put(name, current);
	}

	// <<<<<<<<<GETTERS AND SETTERS>>>>>>>>>

	public Date getDateFrom() {
		return dateFrom;
	}

	@SuppressWarnings("static-access")
	public void setDateFrom(Date dateFrom) {
		this.dateFrom = dateFrom;
		setDateFromChanged(true);

	}

	public Date getDateTo() {
		return dateTo;
	}

	@SuppressWarnings("static-access")
	public void setDateTo(Date dateTo) {
		this.dateTo = dateTo;
		setDateToChanged(true);
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public boolean isDateFromChanged() {
		return isDateFromChanged;
	}

	public static void setDateFromChanged(boolean isDateFromChanged) {
		FullTextIndexesReload.isDateFromChanged = isDateFromChanged;
	}

	public boolean isDateToChanged() {
		return isDateToChanged;
	}

	public static void setDateToChanged(boolean isDateToChanged) {
		FullTextIndexesReload.isDateToChanged = isDateToChanged;
	}

}
