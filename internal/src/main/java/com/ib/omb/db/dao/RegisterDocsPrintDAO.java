package com.ib.omb.db.dao;

import java.util.Date;
import java.util.List;
import javax.persistence.Query;

import com.ib.omb.db.dto.Doc;
import com.ib.omb.system.OmbConstants;
import com.ib.system.ActiveUser;
import com.ib.system.db.AbstractDAO;
import com.ib.system.db.DialectConstructor;
import com.ib.system.db.JPA;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.utils.DateUtils;

/**
 *
 * @author ivanc
 */


public class RegisterDocsPrintDAO extends AbstractDAO<Doc>{
	public static final  String PARIDDOC = "idDoc";
	
	/** @param user */
	public RegisterDocsPrintDAO(ActiveUser user) {
		super(Doc.class,user);
	}
		

			
		/**
		 * Този метод връща данни за Справка за регистри за документи
		 *
		 * @param registratura
		 * @param register
		 * @param fromDat
		 * @param toDat
		 * @return
		 * @throws DbErrorException
		 */
		@SuppressWarnings("unchecked")
		public List<Object[]> selectRegisterDocs(Integer registratura, Integer register, Date fromDat, Date toDat, String sortCol, String ord) throws DbErrorException {
			try {
				StringBuilder select = new StringBuilder();
				StringBuilder from = new StringBuilder();
				StringBuilder where = new StringBuilder();
//				String title = "LABELS";	
				
				
				
				String dialect = JPA.getUtil().getDbVendorName();
	// || , TO_CHAR, NVL, -> dialect			
				select.append( 
						"SELECT DISTINCT " + 
						"d.DOC_ID A0, "+DocDAO.formRnDocSelect("d.", dialect)+" A1, d.RN_PREFIX A2, d.RN_PORED A3, d.DOC_DATE A4, d.DOC_TYPE A5, d.DOC_VID A6, " + 
						DialectConstructor.limitBigString(dialect, "d.OTNOSNO", 300)+" A7, "+
						"d.URGENT A8, d.CODE_REF_CORRESP A9, d.DATE_REG A10, d.DATE_LAST_MOD A11, " + 
						"CASE WHEN d.DOC_TYPE = "+OmbConstants.CODE_ZNACHENIE_DOC_TYPE_IN+" THEN null ELSE "+
						DialectConstructor.convertToDelimitedString(dialect, "dr.CODE_REF", "DOC_REFERENTS dr where dr.DOC_ID = d.DOC_ID and dr.ROLE_REF ="+OmbConstants.CODE_ZNACHENIE_DOC_REF_ROLE_AUTHOR+" ", "+dr.PORED")+" END A12, " +
						"d.TEH_NOMER A13, d.TEH_DATE A14, d.USER_REG A15, d.USER_LAST_MOD A16, " + 
						"z.USER_ID A17, z.LOCK_DATE A18, d.COUNT_FILES A19, d.COUNT_COPIES A20, d.COUNT_ORIGINALS A21, d.COUNT_SHEETS A22, d.WORK_OFF_ID A23, " + 
						"d.DOC_NAME A24, "+DialectConstructor.limitBigString(dialect, "d.DOC_INFO", 300)+" A25, d.VALID A26, d.VALID_DATE A27, slt.TEKST A28, slv.TEKST A29, ar.REF_NAME A30, " + 
						"ara.ADDR_TEXT A31, "+DialectConstructor.concatSQLStr(dialect, "ea.TVM", DialectConstructor.concatSQLStr(dialect, "' '", "ea.IME")) + " A32, obl.IME A33, obs.IME A34, "+DocDAO.formRnDocSelect("dof.", dialect)+" A35, dof.DOC_DATE A36, "+
						"sls.TEKST A37, d.VALID_DATE A38, d.RECEIVE_METHOD A39, d.RECEIVE_DATE A40,  d.RECEIVED_BY A41, d.DELIVERY_METHOD A42, d.WAIT_ANSWER_DATE A43, d.IRREGULAR A44, " + 
						"sli.TEKST A45, d.STATUS A46, d.STATUS_DATE A47, slst.TEKST A48 "
						);
				
				from.append(
					"FROM "+
					"DOC d "+ 
					"LEFT OUTER JOIN LOCK_OBJECTS z on z.OBJECT_TIP = "+OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC+" and z.OBJECT_ID = d.DOC_ID and z.USER_ID <> "+ getUserId() +" "+ 
					"LEFT OUTER JOIN ADM_REFERENTS ar on ar.CODE = d.CODE_REF_CORRESP "+
					"LEFT OUTER JOIN ADM_REF_ADDRS ara on ara.CODE_REF = d.CODE_REF_CORRESP "+ 
					"LEFT OUTER JOIN EKATTE_ATT ea on ea.EKATTE = ara.EKATTE "+
					"LEFT OUTER JOIN EKATTE_OBLASTI obl on obl.OBLAST = ea.OBLAST " +
					"LEFT OUTER JOIN EKATTE_OBSTINI obs on obs.OBSTINA = ea.OBSTINA "+ 
					
					"LEFT OUTER JOIN SYSTEM_CLASSIF sct on sct.CODE_CLASSIF = "+OmbConstants.CODE_CLASSIF_DOC_TYPE+" and d.DOC_TYPE=sct.CODE and sct.DATE_DO is null "+ 
					"LEFT OUTER JOIN SYSTEM_CLASSIF scv on scv.CODE_CLASSIF = "+OmbConstants.CODE_CLASSIF_DOC_VID+" and d.DOC_VID=scv.CODE and scv.DATE_DO is null "+
					"LEFT OUTER JOIN SYSTEM_CLASSIF scs on scs.CODE_CLASSIF = "+OmbConstants.CODE_CLASSIF_DOC_VALID+" and d.VALID=scs.CODE and scs.DATE_DO is null "+
					"LEFT OUTER JOIN SYSTEM_CLASSIF sci on sci.CODE_CLASSIF = "+OmbConstants.CODE_CLASSIF_DOC_IRREGULAR+" and d.IRREGULAR=sci.CODE and sci.DATE_DO is null "+
					"LEFT OUTER JOIN SYSTEM_CLASSIF scst on scst.CODE_CLASSIF = "+OmbConstants.CODE_CLASSIF_DOC_STATUS+" and d.STATUS=scst.CODE and scst.DATE_DO is null "+ 
					
					"LEFT OUTER JOIN SYSCLASSIF_MULTILANG slt on slt.TEKST_KEY=sct.TEKST_KEY and slt.LANG="+getUserLang()+ " "+
					"LEFT OUTER JOIN SYSCLASSIF_MULTILANG slv on slv.TEKST_KEY=scv.TEKST_KEY and slv.LANG="+getUserLang()+ " "+
					"LEFT OUTER JOIN SYSCLASSIF_MULTILANG sls on sls.TEKST_KEY=scs.TEKST_KEY and sls.LANG="+getUserLang()+ " "+
					"LEFT OUTER JOIN SYSCLASSIF_MULTILANG sli on sli.TEKST_KEY=sci.TEKST_KEY and sli.LANG="+getUserLang()+ " "+
					"LEFT OUTER JOIN SYSCLASSIF_MULTILANG slst on slst.TEKST_KEY=scst.TEKST_KEY and slst.LANG="+getUserLang()+ " "+ 
					
					"LEFT OUTER JOIN DOC dof on dof.DOC_ID=d.WORK_OFF_ID "
					
					/*"LEFT OUTER JOIN DELO_ARCHIVE dd on dd.DOC_ID=d.DOC_ID "	*/
				);
				
				where.append("WHERE 1=1 ");
				if (null!=registratura) // в търсената регистратура
					where.append("and d.REGISTRATURA_ID = :registratura ");
				if (null!=registratura) // в търсен регистър
					where.append("and d.REGISTER_ID = :register ");
				
				if (null!=fromDat && null!=toDat) // датите
					where.append("and d.DOC_DATE >="+ DialectConstructor.convertDateToSQLString(dialect, DateUtils.startDate(fromDat)) + "and d.DOC_DATE <= "+ DialectConstructor.convertDateToSQLString(dialect, DateUtils.endDate(toDat))+" ");
				
				if (null!=sortCol && !sortCol.isEmpty())
					where.append(" ORDER BY "+sortCol);
				if (null!=ord && !ord.isEmpty())
					where.append(" "+ord);
				
				String sqlSel=select.toString() + from.toString() + where.toString();
				
	

				Query query = createNativeQuery(sqlSel);

				if (registratura != null)
					query.setParameter("registratura", registratura);
				if (register != null)
					query.setParameter("register", register);
				
				

				return query.getResultList();

			} catch (Exception e) {
				throw new DbErrorException(e.getMessage());
			}
		}
		
		
		/**
		 * Този метод връща вложени екземпляри на документи в дело/преписки
		 *
		 * @param idDoc
		 * @param sortCol
		 * @return
		 * @throws DbErrorException
		 */
		@SuppressWarnings("unchecked")
		public List<Object[]> selVlojEkz(Integer idDoc, String sortCol) throws DbErrorException {
			try {
				StringBuilder select = new StringBuilder();
				StringBuilder from = new StringBuilder();
				StringBuilder where = new StringBuilder();
				
				String dialect = JPA.getUtil().getDbVendorName();
				
				select.append( 
						"select dd.ID a0, dd.INPUT_DATE a1, dd.EKZ_NOMER a2, dd.TOM_NOMER a3, dd.DELO_ID a4, "+ 
						"d.INIT_DOC_ID a5, d.RN_DELO a6, d.DELO_DATE a7, d.STATUS a8, d.STATUS_DATE a9, d.DELO_TYPE a10, "+ 
						 DialectConstructor.limitBigString(dialect, "d.DELO_NAME", 300) + " a11, dd.SECTION_TYPE a12, d.WITH_SECTION a13, d.WITH_TOM a14 "
						);
				
				from.append(
					"from DELO_DOC dd inner join DELO d on d.DELO_ID = dd.DELO_ID "	
				);
				
				if (null!=idDoc)
					where.append("where dd.INPUT_DOC_ID = :idDoc ");
				
				if (null!=sortCol && !sortCol.isEmpty())
					where.append(" ORDER BY "+sortCol);
				
				String sqlSel=select.toString() + from.toString() + where.toString();
				
	

				Query query = createNativeQuery(sqlSel);

				if (idDoc != null)
					query.setParameter(PARIDDOC, idDoc);
				

				return query.getResultList();

			} catch (Exception e) {
				throw new DbErrorException("Грешка при търсене на вложени екземпляри на документи!", e);
			}
		}
		
		
		
		
		/**
		 * Този метод връща данни унищожени документи
		 *
		 * @param idDoc
		 * @return
		 * @throws DbErrorException
		 */
		@SuppressWarnings("unchecked")
		public List<Object[]> selDestrDocs(Integer idDoc) throws DbErrorException {
			try {
				StringBuilder select = new StringBuilder();
				StringBuilder from = new StringBuilder();
				StringBuilder where = new StringBuilder();
				
				String dialect = JPA.getUtil().getDbVendorName();
				select.append( 
						"select dd.EKZ_NOMERA a0, "+DocDAO.formRnDocSelect("d.", dialect)+" a1, d.DOC_DATE a2, "+DocDAO.formRnDocSelect("dof.", dialect)+" a3, dof.DOC_DATE a4, d.DOC_TYPE a5, "+
						"dof.DOC_TYPE a6, d.VALID a7, d.VALID_DATE a8, dof.VALID a9, dof.VALID_DATE a10, slv1.TEKST a11, sls.TEKST a12 " 
						);
				
				from.append(
					"from DOC_DESTRUCT dd "+
					"join DOC d on dd.PROTOCOL_ID = d.DOC_ID "+
					"LEFT OUTER JOIN DOC dof on dof.DOC_ID=d.WORK_OFF_ID "+
					
					"LEFT OUTER JOIN SYSTEM_CLASSIF scv1 on scv1.CODE_CLASSIF = "+OmbConstants.CODE_CLASSIF_DOC_VID+" and d.DOC_VID=scv1.CODE and scv1.DATE_DO is null "+
					"LEFT OUTER JOIN SYSCLASSIF_MULTILANG slv1 on slv1.TEKST_KEY=scv1.TEKST_KEY and slv1.LANG="+getUserLang()+ " " +
					
					"LEFT OUTER JOIN SYSTEM_CLASSIF scs1 on scs1.CODE_CLASSIF = "+OmbConstants.CODE_CLASSIF_DOC_VID+" and dof.DOC_VID=scs1.CODE and scs1.DATE_DO is null "+
					"LEFT OUTER JOIN SYSCLASSIF_MULTILANG sls on sls.TEKST_KEY=scs1.TEKST_KEY and sls.LANG="+getUserLang()+ " "
					
				);
				
				if (null!=idDoc)
					where.append("where dd.DOC_ID = :idDoc order by d.DOC_DATE");
				
				
				
				String sqlSel=select.toString() + from.toString() + where.toString();
				
	

				Query query = createNativeQuery(sqlSel);

				if (idDoc != null)
					query.setParameter(PARIDDOC, idDoc);
				

				return query.getResultList();

			} catch (Exception e) {
				throw new DbErrorException("Грешка при търсене на унищожени документи!", e);
			}
		}
		
		
		
		/**
		 * Този метод връща данни за архивирани документи
		 *
		 * @param idDoc
		 * @return
		 * @throws DbErrorException
		 */
		@SuppressWarnings("unchecked")
		public List<Object[]> selArhivDocs(Integer idDoc) throws DbErrorException {
			try {
				StringBuilder select = new StringBuilder();
				StringBuilder from = new StringBuilder();
				StringBuilder where = new StringBuilder();
				
				String dialect = JPA.getUtil().getDbVendorName();
				select.append("Select " + 
						"da.PROTOCOL_ID a0, " + 
						"dp.DOC_TYPE a1, dp.DOC_VID a2, "+DocDAO.formRnDocSelect("dp.", dialect)+" a3, dp.DOC_DATE a4, " + 
						"dd.TOM_NOMER a5, dd.EKZ_NOMER a6, slt1.TEKST a7, slv2.TEKST a8, " + 
						"dof.DOC_TYPE a9, dof.DOC_VID a10, "+DocDAO.formRnDocSelect("dof.", dialect)+" a11, dof.DOC_DATE a12, d.RN_DELO a13, d.DELO_DATE a14, da.DATE_REG a15, d.delo_type a16,  sld.TEKST a17 "					
						);
				
				from.append(
					"from  " + 
					"DELO_DOC dd " + 
					"JOIN DELO_ARCHIVE da on da.DELO_ID=dd.DELO_ID " + 
					"JOIN DELO d on d.DELO_ID=dd.DELO_ID " +
					/*"LEFT OUTER JOIN DELO_STORAGE ds on ds.DELO_ID=dd.DELO_ID and ds.TOM_NOMER = dd.TOM_NOMER " + 
					
					"LEFT OUTER JOIN DELO_DELO dde on dde.INPUT_DELO_ID = dd.DELO_ID " + 
					"LEFT OUTER JOIN DELO_STORAGE ds on ds.DELO_ID=dde.DELO_ID " + */
					
					"LEFT OUTER JOIN DOC dp on dp.DOC_ID=da.PROTOCOL_ID " + 
					"LEFT OUTER JOIN DOC dof on dof.DOC_ID=dp.WORK_OFF_ID " +
					
					"LEFT OUTER JOIN SYSTEM_CLASSIF sct1 on sct1.CODE_CLASSIF = "+OmbConstants.CODE_CLASSIF_DOC_VID+" and dp.DOC_VID=sct1.CODE and sct1.DATE_DO is null "+
					"LEFT OUTER JOIN SYSCLASSIF_MULTILANG slt1 on slt1.TEKST_KEY=sct1.TEKST_KEY and slt1.LANG="+getUserLang()+ " "+
					
					"LEFT OUTER JOIN SYSTEM_CLASSIF scv2 on scv2.CODE_CLASSIF = "+OmbConstants.CODE_CLASSIF_DOC_VID+" and dof.DOC_VID=scv2.CODE and scv2.DATE_DO is null "+
					"LEFT OUTER JOIN SYSCLASSIF_MULTILANG slv2 on slv2.TEKST_KEY=scv2.TEKST_KEY and slv2.LANG="+getUserLang()+ " " +
					
					"LEFT OUTER JOIN SYSTEM_CLASSIF scd on scd.CODE_CLASSIF = "+OmbConstants.CODE_CLASSIF_DELO_TYPE+" and d.delo_type=scd.CODE and scd.DATE_DO is null " + 
					"LEFT OUTER JOIN SYSCLASSIF_MULTILANG sld on sld.TEKST_KEY=scd.TEKST_KEY and sld.LANG="+getUserLang()+ " ");
				
				
				
				
				if (null!=idDoc)
					where.append("where dd.INPUT_DOC_ID = :idDoc order by dp.DOC_DATE");
				
				
				String sqlSel=select.toString() + from.toString() + where.toString();
				
	

				Query query = createNativeQuery(sqlSel);

				if (idDoc != null)
					query.setParameter(PARIDDOC, idDoc);
				

				return query.getResultList();

			} catch (Exception e) {
				throw new DbErrorException("Грешка при търсене на архивирани документи!", e);
			}
		}
		
		
		

		/**
		 * Този метод връща данни за съхранени документи
		 *
		 * @param idDoc
		 * @return
		 * @throws DbErrorException
		 */
		@SuppressWarnings("unchecked")
		public List<Object[]> selStoredDocs(Integer idDoc) throws DbErrorException {
			try {
				
//				String dialect = JPA.getUtil().getDbVendorName();
				StringBuilder select = new StringBuilder();
				StringBuilder from = new StringBuilder();
				StringBuilder where = new StringBuilder();
			
				select.append("select ds.DELO_ID a0, ds.TOM_NOMER a1, ds.ARCH_NOMER a2, ds.ROOM a3, ds.SHKAF a4, ds.STILLAGE a5, ds.BOX a6, " + 
						"dd.input_doc_id a7, dd.tom_nomer a8, dd.ekz_nomer a9, d.RN_DELO a10, d.DELO_DATE a11, d.delo_type a12, slt2.TEKST a13 ");
				from.append(" from "+
						"delo_storage ds " + 
						"join delo_doc dd on ds.delo_id=dd.delo_id and ds.tom_nomer=dd.tom_nomer " + 
						"join delo d on d.delo_id=dd.delo_id " + 
						"LEFT OUTER JOIN SYSTEM_CLASSIF sct2 on sct2.CODE_CLASSIF = "+OmbConstants.CODE_CLASSIF_DELO_TYPE+" and d.delo_type=sct2.CODE and sct2.DATE_DO is null " + 
						"LEFT OUTER JOIN SYSCLASSIF_MULTILANG slt2 on slt2.TEKST_KEY=sct2.TEKST_KEY and slt2.LANG="+getUserLang()+ " ");
						
						
				if (null!=idDoc)
					where.append(" where dd.input_doc_id = :idDoc ");
				
				where.append(" ORDER BY dd.delo_id ");
				
				String sqlSel=select.toString() + from.toString() + where.toString();
	
				Query query = createNativeQuery(sqlSel);
				
				if (null!=idDoc)
					query.setParameter(PARIDDOC, idDoc);

				return query.getResultList();

			} catch (Exception e) {
				throw new DbErrorException("Грешка при търсене на съхранявани документи!", e);
			}
		}
		
	
		
		/**
		 * Този метод връща данни за движенията на документи
		 *
		 * @param idDoc
		 * @return
		 * @throws DbErrorException
		 */
		@SuppressWarnings("unchecked")
		public List<Object[]> selDvigDocs(Integer idDoc) throws DbErrorException {
			try {
				String dialect = JPA.getUtil().getDbVendorName();
				StringBuilder select = new StringBuilder();
				StringBuilder from = new StringBuilder();
				StringBuilder where = new StringBuilder();
				
				select.append("Select " + 
						"dd.DOC_ID a0, " + 
						"dd.EKZ_NOMER a1, dd.DVIJ_DATE a2, dd.DVIJ_METHOD a3, dd.DVIJ_TEXT a4, dd.DVIJ_EMAIL a5, "+
						"dd.DVIJ_INFO a6, dd.RETURN_TO_DATE a7, dd.STATUS a8, dd.STATUS_DATE a9, "+
						DialectConstructor.limitBigString(dialect, "dd.STATUS_TEXT", 300)+ " a10, "+
						"dd.RETURN_DATE a11, dd.RETURN_METHOD a12, dd.RETURN_INFO a13, dd.CODE_REF a14, "+
						/*"lmt.TEKST a15, lst.TEKST a16, lmtr.TEKST a17  "*/
						"lst.TEKST a15 " 
						);
				
				from.append(
					"from  " + 
					"DOC_DVIJ dd " + 
					
					/*"LEFT OUTER JOIN SYSTEM_CLASSIF scm on scm.CODE_CLASSIF = "+OmbConstants.CODE_CLASSIF_DVIJ_METHOD+" and dd.DVIJ_METHOD=scm.CODE and scm.DATE_DO is null "+
					"LEFT OUTER JOIN SYSCLASSIF_MULTILANG lmt on lmt.TEKST_KEY=scm.TEKST_KEY and lmt.LANG="+getUserLang()+ " "+
					
					"LEFT OUTER JOIN SYSTEM_CLASSIF scmr on scmr.CODE_CLASSIF = "+OmbConstants.CODE_CLASSIF_DVIJ_METHOD+" and dd.RETURN_METHOD=scmr.CODE and scmr.DATE_DO is null "+ 
					"LEFT OUTER JOIN SYSCLASSIF_MULTILANG lmtr on lmtr.TEKST_KEY=scmr.TEKST_KEY and lmtr.LANG="+getUserLang()+ " "+*/
					
					"LEFT OUTER JOIN SYSTEM_CLASSIF scs2 on scs2.CODE_CLASSIF = "+OmbConstants.CODE_CLASSIF_DOC_PREDAVANE_STATUS+" and dd.STATUS=scs2.CODE and scs2.DATE_DO is null "+
					"LEFT OUTER JOIN SYSCLASSIF_MULTILANG lst on lst.TEKST_KEY=scs2.TEKST_KEY and lst.LANG="+getUserLang()+ " "
				);
				
				
				
				if (null!=idDoc)
					where.append("where dd.DOC_ID = :idDoc order by dd.DVIJ_DATE ");
				
				
				String sqlSel=select.toString() + from.toString() + where.toString();
				
	

				Query query = createNativeQuery(sqlSel);

				if (idDoc != null)
					query.setParameter(PARIDDOC, idDoc);
				

				return query.getResultList();

			} catch (Exception e) {
				throw new DbErrorException("Грешка при четене на движения документи!", e);
			}
		}
		
		
		/**
		 * Този метод връща данни за приложенията на документи
		 *
		 * @param idDoc
		 * @return
		 * @throws DbErrorException
		 */
		@SuppressWarnings("unchecked")
		public List<Object[]> selPrilDocs(Integer idDoc) throws DbErrorException {
			
			try {
				StringBuilder select = new StringBuilder();
				StringBuilder from = new StringBuilder();
				StringBuilder where = new StringBuilder();
			
				select.append("Select p.ID a0, p.DOC_ID a1, p.NOMER a2, p.MEDIA_TYPE a3, p.PRIL_NAME a4, p.PRIL_INFO a5, p.COUNT_SHEETS a6, lst1.TEKST a7  ");
				from.append(" from DOC_PRIL p "+
						"LEFT OUTER JOIN SYSTEM_CLASSIF scs3 on scs3.CODE_CLASSIF = "+OmbConstants.CODE_CLASSIF_MEDIA_TYPE+" and p.MEDIA_TYPE=scs3.CODE and scs3.DATE_DO is null "+
						"LEFT OUTER JOIN SYSCLASSIF_MULTILANG lst1 on lst1.TEKST_KEY=scs3.TEKST_KEY and lst1.LANG="+getUserLang()+ " ");
				if (null!=idDoc)
					where.append(" where p.DOC_ID = :idDoc ");
				
				where.append(" ORDER BY p.NOMER ");
				
				String sqlSel=select.toString() + from.toString() + where.toString();
	
				Query query = createNativeQuery(sqlSel);
				
				if (null!=idDoc)
					query.setParameter(PARIDDOC, idDoc);

				return query.getResultList();

			} catch (Exception e) {
				throw new DbErrorException("Грешка при четене на приложения на документи!", e);
			}
			
		
		
		}
		
		
		
		/**
		 * Този метод връща данни за свързани документи
		 *
		 * @param idDoc
		 * @return
		 * @throws DbErrorException
		 */
		@SuppressWarnings("unchecked")
		public List<Object[]> selLinkDocs(Integer idDoc) throws DbErrorException {
			
			try {
				String dialect = JPA.getUtil().getDbVendorName();
				StringBuilder select = new StringBuilder();
				StringBuilder from = new StringBuilder();
				StringBuilder where = new StringBuilder();
			
				select.append("Select dd.DOC_ID1 a0, dd.DOC_ID2 a1, dd.REL_TYPE a2, "+DocDAO.formRnDocSelect("d.", dialect)+" a3, d.DOC_DATE a4, "+
						DialectConstructor.limitBigString(dialect, "d.OTNOSNO", 300)+ " a5, lst2.TEKST a6 ");
				from.append(" from "+
						"DOC_DOC dd "+
						"LEFT OUTER JOIN DOC d on d.DOC_ID=dd.DOC_ID2 "+
						"LEFT OUTER JOIN SYSTEM_CLASSIF scs4 on scs4.CODE_CLASSIF = "+OmbConstants.CODE_CLASSIF_DOC_REL_TYPE+" and dd.REL_TYPE=scs4.CODE and scs4.DATE_DO is null "+
						"LEFT OUTER JOIN SYSCLASSIF_MULTILANG lst2 on lst2.TEKST_KEY=scs4.TEKST_KEY and lst2.LANG="+getUserLang()+ " ");
				if (null!=idDoc)
					where.append(" where dd.DOC_ID1 = :idDoc ");
				
				where.append(" ORDER BY dd.DOC_ID2 ");
				
				String sqlSel=select.toString() + from.toString() + where.toString();
	
				Query query = createNativeQuery(sqlSel);
				
				if (null!=idDoc)
					query.setParameter(PARIDDOC, idDoc);

				return query.getResultList();

			} catch (Exception e) {
				throw new DbErrorException("Грешка при четене на свързаните документи!", e);
			}

		}
		
		
	}

