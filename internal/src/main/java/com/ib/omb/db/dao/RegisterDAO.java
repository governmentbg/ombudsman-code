package com.ib.omb.db.dao;

import static com.ib.omb.system.OmbConstants.CODE_CLASSIF_REGISTRATURI;
import static com.ib.omb.system.OmbConstants.CODE_CLASSIF_REGISTRI;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_DOC_TYPE_WRK;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_JOURNAL_REGISTER;
import static com.ib.system.SysConstants.CODE_DEIN_IZTRIVANE;
import static com.ib.system.SysConstants.CODE_ZNACHENIE_DA;
import static com.ib.system.SysConstants.CODE_ZNACHENIE_NE;
import static com.ib.system.utils.SearchUtils.asInteger;

import java.util.Date;
import java.util.List;
import java.util.Objects;

import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.omb.db.dto.Register;
import com.ib.omb.system.OmbConstants;
import com.ib.system.ActiveUser;
import com.ib.system.BaseSystemData;
import com.ib.system.SysConstants;
import com.ib.system.db.AbstractDAO;
import com.ib.system.db.dto.SystemJournal;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.ObjectInUseException;
import com.ib.system.utils.SearchUtils;

/**
 * DAO for {@link Register}
 *
 * @author belev
 */
public class RegisterDAO extends AbstractDAO<Register> {

	private static final Logger LOGGER = LoggerFactory.getLogger(RegisterDAO.class);

	/** @param user */
	public RegisterDAO(ActiveUser user) {
		super(Register.class, user);
	}

	/**
	 * Изтриване на регистър в контекста на регистратура
	 *
	 * @param contextRegistratura
	 * @param entity
	 * @param sd
	 * @throws DbErrorException
	 * @throws ObjectInUseException
	 */
	public void delete(Integer contextRegistratura, Register entity, BaseSystemData sd) throws DbErrorException, ObjectInUseException {
		try {
			Integer cnt;

			if (Objects.equals(entity.getCommon(), CODE_ZNACHENIE_DA)) { // общ регистър

				if (Objects.equals(contextRegistratura, entity.getRegistraturaId())) { // трие е се от тази, която го поддържа

					StringBuilder sql = new StringBuilder();
					sql.append(" select r.REGISTRATURA_ID, r.VALID from REGISTRATURA_REGISTER rr ");
					sql.append(" inner join REGISTRATURI r on r.REGISTRATURA_ID = rr.REGISTRATURA_ID ");
					sql.append(" where rr.REGISTER_ID = :regId order by r.VALID ");
					
					@SuppressWarnings("unchecked")
					List<Object[]> otherRegistraturi = createNativeQuery(sql.toString()) //
							.setParameter("regId", entity.getId()).getResultList();

					if (!otherRegistraturi.isEmpty()) { // използва се и от други
						Object[] first = otherRegistraturi.get(0);
	
						// трябва да се изтрие връзката със нея
						createNativeQuery("delete from REGISTRATURA_REGISTER where REGISTRATURA_ID = ?1 and REGISTER_ID = ?2")
							.setParameter(1, first[0]).setParameter(2, entity.getId()).executeUpdate();

						SystemJournal journal = new SystemJournal();
						Date date = new Date();

						journal.setCodeAction(CODE_DEIN_IZTRIVANE);
						journal.setCodeObject(CODE_ZNACHENIE_JOURNAL_REGISTER);
						journal.setDateAction(date);
						journal.setIdentObject("Премахване на "+sd.decodeItem(CODE_CLASSIF_REGISTRI, entity.getId(), getUserLang(), date)+" от регистратура " 
													+ sd.decodeItem(CODE_CLASSIF_REGISTRATURI, contextRegistratura, getUserLang(), date));
						journal.setIdObject(entity.getId());
						journal.setIdUser(getUserId());

						journal.setJoinedCodeObject1(OmbConstants.CODE_ZNACHENIE_JOURNAL_REISTRATURA);
						journal.setJoinedIdObject1(contextRegistratura);

						saveAudit(journal);

						entity.setRegistraturaId(SearchUtils.asInteger(first[0]));
						save(entity); // и тя да му стане собственик

						return; // реално няма да има изтриване
					}

				} else { // но се трие от регистратура, която го използва, а не от тази, която го поддържа

					// реално се прави премахване от таблица REGISTRATURA_REGISTER
					Query query = createNativeQuery(" delete from REGISTRATURA_REGISTER where REGISTRATURA_ID = :registratura and REGISTER_ID = :register ");
					query.setParameter("registratura", contextRegistratura);
					query.setParameter("register", entity.getId());

					cnt = query.executeUpdate();
					LOGGER.debug("Delete {} REGISTRATURA_REGISTER for REGISTRATURA_ID={},REGISTER_ID={}", cnt, contextRegistratura, entity.getId());

					SystemJournal journal = new SystemJournal();
					Date date = new Date();

					journal.setCodeAction(CODE_DEIN_IZTRIVANE);
					journal.setCodeObject(CODE_ZNACHENIE_JOURNAL_REGISTER);
					journal.setDateAction(date);
					journal.setIdentObject("Премахване на "+sd.decodeItem(CODE_CLASSIF_REGISTRI, entity.getId(), getUserLang(), date)+" от регистратура " 
												+ sd.decodeItem(CODE_CLASSIF_REGISTRATURI, contextRegistratura, getUserLang(), date));
					journal.setIdObject(entity.getId());
					journal.setIdUser(getUserId());

					journal.setJoinedCodeObject1(OmbConstants.CODE_ZNACHENIE_JOURNAL_REISTRATURA);
					journal.setJoinedIdObject1(contextRegistratura);

					saveAudit(journal);

					return; // !!! няма повече какво да се прави
				}
			}

			cnt = asInteger( // DOC.REGISTER_ID
				createNativeQuery("select count (*) as cnt from DOC where REGISTER_ID = :regId") //
					.setParameter("regId", entity.getId()) //
					.getResultList().get(0));
			if (cnt != null && cnt.intValue() > 0) {
				String msg = "В регистъра има регистрирани документи и не може да бъде изтрит!";
				if (Objects.equals(entity.getValid(), SysConstants.CODE_ZNACHENIE_DA)) {
					msg+=" Моля, направете го неактивен.";
				}
				throw new ObjectInUseException(msg);
			}

			cnt = asInteger( // DOC_VID_SETTINGS.REGISTER_ID
				createNativeQuery("select count (*) as cnt from DOC_VID_SETTINGS where REGISTER_ID = :regId") //
					.setParameter("regId", entity.getId()) //
					.getResultList().get(0));
			if (cnt != null && cnt.intValue() > 0) {
				String msg = "За регистъра има дефинирани настройки по вид документ и не може да бъде изтрит!";
				if (Objects.equals(entity.getValid(), SysConstants.CODE_ZNACHENIE_DA)) {
					msg+=" Моля, направете го неактивен.";
				}
				throw new ObjectInUseException(msg);
			}

			// ADM_USER_ROLES.CODE_ROLE (CODE_CLASSIF=OmbConstants.CODE_CLASSIF_REGISTRI)
			Query query = createNativeQuery(" delete from ADM_USER_ROLES where CODE_CLASSIF = :classifRegistri and CODE_ROLE = :register ");
			query.setParameter("classifRegistri", CODE_CLASSIF_REGISTRI);
			query.setParameter("register", entity.getId());

			cnt = query.executeUpdate();
			LOGGER.debug("Delete {} ADM_USER_ROLES for CODE_CLASSIF={},CODE_ROLE={}", cnt, CODE_CLASSIF_REGISTRI, entity.getId());

		} catch (ObjectInUseException e) {
			throw e; // за да не се преопакова

		} catch (Exception e) {
			throw new DbErrorException("Грешка при изтриване на подчинени обекти за регистър!", e);
		}

		super.delete(entity); // ако нещо не е избило горе значи ще се трие
	}

	/** */
	@Override
	public Register findById(Object id) throws DbErrorException {
		Register register = super.findById(id);

		if (register != null) {
			register.setDbCommon(register.getCommon());
			register.setDbAlg(register.getAlg());
			register.setDbDocType(register.getDocType());
			register.setDbPrefix(SearchUtils.trimToNULL(register.getPrefix()));
		}

		return register;
	}

	/**
	 * Намира регистрите за подадената регистраура, като изтегля само данните от вида: <br>
	 * [0]-REGISTER_ID<br>
	 * [1]-REGISTRATURA_ID<br>
	 * [2]-REGISTER<br>
	 * [3]-REGISTER_TYPE<br>
	 * [4]-VALID<br>
	 * [5]-COMMON<br>
	 * [6]-DOC_TYPE<br>
	 * [7]-PREFIX<br>
	 * [8]-ALG<br>
	 * [9]-SORT_NOMER<br>
	 * [10]-BEG_NOMER<br>
	 * [11]-STEP<br>
	 * [12]-ACT_NOMER<br>
	 *
	 * @param registratura
	 * @param withCommon   при <code>true</code> значи може да има и общи регистри и те също се връщат
	 * @return
	 * @throws DbErrorException
	 */
	public List<Object[]> findByRegistraturaId(Integer registratura, boolean withCommon) throws DbErrorException {
		return selectRegisters(registratura, null, withCommon, false, null, null);
	}
	
	
	/**
	 * Намира регистрите за подадената регистраура и алгоритъм, като изтегля само данните от вида: <br>
	 * [0]-REGISTER_ID<br>
	 * [1]-REGISTRATURA_ID<br>
	 * [2]-REGISTER<br>
	 * [3]-REGISTER_TYPE<br>
	 * [4]-VALID<br>
	 * [5]-COMMON<br>
	 * [6]-DOC_TYPE<br>
	 * [7]-PREFIX<br>
	 * [8]-ALG<br>
	 * [9]-SORT_NOMER<br>
	 * [10]-BEG_NOMER<br>
	 * [11]-STEP<br>
	 * [12]-ACT_NOMER<br>
	 *
	 * @param registratura
	 * @param withCommon   при <code>true</code> значи може да има и общи регистри и те също се връщат
	 * @param tipAlg
	 * @return
	 * @throws DbErrorException
	 */
	public List<Object[]> findAnulByRegistraturaId(Integer registratura, boolean withCommon, Boolean valid, Integer alg) throws DbErrorException {
		return selectRegisters(registratura, null, withCommon, false, valid, alg);
	}

	/**
	 * Намира общите регистри. Ако е подадена excludeRegistratura, това означава че наличните регистри на подадената регистратура
	 * няма да се върнат. Резултата е от вида: {@link #findByRegistraturaId(Integer, boolean)}
	 *
	 * @param excludeRegistratura
	 * @return
	 * @throws DbErrorException
	 */
	public List<Object[]> findRegistersCommon(Integer excludeRegistratura) throws DbErrorException {
		return selectRegisters(null, excludeRegistratura, true, true, true, null);
	}

	/**
	 * Намира регистрите по подадена регистратура за копиране в текущата. Ако е подадена excludeRegistratura, това означава че
	 * наличните регистри (само за общте има смисъл) на подадената регистратура няма да се върнат. Резултата е от вида:
	 * {@link #findByRegistraturaId(Integer, boolean)}
	 *
	 * @param fromRegistratura
	 * @param excludeRegistratura
	 * @param withCommon          при <code>true</code> значи може да има и общи регистри и те също се връщат
	 * @return
	 * @throws DbErrorException
	 */
	public List<Object[]> findRegistersCopy(Integer fromRegistratura, Integer excludeRegistratura, boolean withCommon) throws DbErrorException {
		return selectRegisters(fromRegistratura, excludeRegistratura, withCommon, false, true, null);
	}

	/**
	 * Прави валидации чрез селекти дали промяната е възможна и ако не може се дава {@link ObjectInUseException}
	 *
	 * @param entity
	 * @param dein дейност, от която се прави промяната, защото се изисква при нулиране в журнала да е ясно че е направено нулиране
	 * @return
	 * @throws DbErrorException
	 * @throws ObjectInUseException
	 */
	public Register save(Register entity, String dein) throws DbErrorException, ObjectInUseException {
		entity.setPrefix(SearchUtils.trimToNULL(entity.getPrefix())); // за да е винаги еднакво

		if (entity.getId() != null) { // корекция

			if (!Objects.equals(entity.getCommon(), entity.getDbCommon()) // сменено е полето ОБЩ
				&& Objects.equals(entity.getCommon(), CODE_ZNACHENIE_NE)) { // и новото му състояние е изключено

				Integer cnt; // ако някоя друга регистратура го използва трябва да се даде грешка
				try {
					cnt = asInteger(createNativeQuery("select count (*) as cnt from REGISTRATURA_REGISTER where REGISTER_ID = :regId") //
						.setParameter("regId", entity.getId()) //
						.getResultList().get(0));
				} catch (Exception e) {
					throw new DbErrorException("Грешка при търсене на регистратури използващи общ регистър!", e);
				}
				if (cnt != null && cnt.intValue() > 0) {
					throw new ObjectInUseException("Регистърът е общ и вече се използва от " + cnt + " регистратури!");
				}
			}

			boolean prefixChanged = !Objects.equals(entity.getPrefix(), entity.getDbPrefix());
			boolean algChanged = !Objects.equals(entity.getAlg(), entity.getDbAlg());
			boolean docTypeChanged = !Objects.equals(entity.getDocType(), entity.getDbDocType()) // сменен
				&& (Objects.equals(entity.getDbDocType(), CODE_ZNACHENIE_DOC_TYPE_WRK) // като е бил
					|| Objects.equals(entity.getDocType(), CODE_ZNACHENIE_DOC_TYPE_WRK)); // или станал работен

			if (algChanged || docTypeChanged || prefixChanged) { // смяна!
				Number cnt; // ако има вече документи трябва да се даде грешка
				try {
					cnt = (Number) createNativeQuery("select count (*) as cnt from DOC where REGISTER_ID = :regId") //
						.setParameter("regId", entity.getId()) //
						.getResultList().get(0);
				} catch (Exception e) {
					throw new DbErrorException("Грешка при търсене на документи за регистър", e);
				}
				if (cnt.intValue() > 0) {
					String error;
					if (algChanged) {
						error = "Алгоритъмът не може да бъде променен, защото в регистъра има регистрирани документи!";
					} else if (prefixChanged) {
						error = "Префиксът не може да бъде променен, защото в регистъра има регистрирани документи!";
					} else {
						error = "Тип документ не може да бъде променен, защото в регистъра има регистрирани документи!";
					}
					throw new ObjectInUseException(error);
				}
			}
		}

		if (dein != null) {
			if (entity.getId() != null) {
				entity = merge(entity); // това ще гарантира, че като се извика корекция и се журналира, dein ще е налично
			}
			entity.setDein(dein); // тука е сетвам в persistent обекта
		}

		Register saved = super.save(entity);

		saved.setDbCommon(entity.getCommon()); // за да се знае след merge-а
		saved.setDbAlg(entity.getAlg());
		saved.setDbDocType(entity.getDocType());
		saved.setDbPrefix(entity.getPrefix());

		return saved;
	}

	/**
	 * Този метод се използва за разните търсения, за да няма на много места един и същ код.
	 *
	 * @param registratura
	 * @param excludeRegistratura
	 * @param withCommon
	 * @param onlyCommon
	 * @param valid
	 * @return
	 * @throws DbErrorException
	 */
	@SuppressWarnings("unchecked")
	private List<Object[]> selectRegisters(Integer registratura, Integer excludeRegistratura, boolean withCommon, boolean onlyCommon, Boolean valid, Integer alg) throws DbErrorException {
		try {
			StringBuilder sql = new StringBuilder();
			sql.append(" select r.REGISTER_ID, r.REGISTRATURA_ID, r.REGISTER, r.REGISTER_TYPE, r.VALID, r.COMMON, r.DOC_TYPE, r.PREFIX, r.ALG, r.SORT_NOMER, r.BEG_NOMER, r.STEP, r.ACT_NOMER ");
			sql.append(" from REGISTRI r where 1=1 ");

			if (registratura != null) { // в търсената регистратура
				sql.append(" and r.REGISTRATURA_ID = :registratura ");
			}
			if (onlyCommon) {
				sql.append(" and r.COMMON = :common ");
			}
			if (valid != null) {
				sql.append(" and r.VALID = :valid ");
			}
			if (alg != null) {
				sql.append(" and r.ALG = :alg ");
			}
			if (excludeRegistratura != null) {
				sql.append(" and r.REGISTER not in (select v.REGISTER from REGISTRI v where v.REGISTRATURA_ID = :excludeRegistratura) "); // съвпадение
																																			// по
																																			// име
				if (withCommon) { // и съвпадение по ИД за общите
					sql.append(" and r.REGISTER_ID not in (select v.REGISTER_ID from REGISTRATURA_REGISTER v where v.REGISTRATURA_ID = :excludeRegistratura) ");
				}
			}

			if (withCommon) {
				sql.append(" union ");

				sql.append(" select r.REGISTER_ID, r.REGISTRATURA_ID, r.REGISTER, r.REGISTER_TYPE, r.VALID, r.COMMON, r.DOC_TYPE, r.PREFIX, r.ALG, r.SORT_NOMER, r.BEG_NOMER, r.STEP, r.ACT_NOMER ");
				sql.append(" from REGISTRATURA_REGISTER rr ");
				sql.append(" inner join REGISTRI r on r.REGISTER_ID = rr.REGISTER_ID where 1=1 ");
				if (registratura != null) {
					sql.append(" and rr.REGISTRATURA_ID = :registratura and r.REGISTRATURA_ID != :registratura ");
				}
				if (onlyCommon) {
					sql.append(" and r.COMMON = :common ");
				}
				if (valid != null) {
					sql.append(" and r.VALID = :valid ");
				}
				if (alg != null) {
					sql.append(" and r.ALG = :alg ");
				}
				if (excludeRegistratura != null) {
					sql.append(" and r.REGISTER_ID not in (select v.REGISTER_ID from REGISTRATURA_REGISTER v where v.REGISTRATURA_ID = :excludeRegistratura) ");
				}
			}
			
			
			sql.append(" order by 6, 10, 3 "); // COMMON, SORT_NOMER, REGISTER

			Query query = createNativeQuery(sql.toString());

			if (registratura != null) {
				query.setParameter("registratura", registratura);
			}
			if (excludeRegistratura != null) {
				query.setParameter("excludeRegistratura", excludeRegistratura);
			}
			if (onlyCommon) {
				query.setParameter("common", CODE_ZNACHENIE_DA);
			}
			if (valid != null) {
				if (valid) {
					query.setParameter("valid", SysConstants.CODE_ZNACHENIE_DA);
				} else {
					query.setParameter("valid", SysConstants.CODE_ZNACHENIE_NE);
				}
			}
			if (alg != null) {
				query.setParameter("alg", alg);
			}

			return query.getResultList();

		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на регистри за регистратура!", e);
		}
	}
}