package com.ib.omb.system;

import static com.ib.indexui.system.Constants.CODE_CLASSIF_ADMIN_STR;
import static com.ib.indexui.system.Constants.CODE_CLASSIF_REFERENTS;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.system.Constants;
import com.ib.omb.system.OmbClassifAdapter;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.system.SysConstants;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.DbErrorException;

/**  */
public class TestSystemData {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestSystemData.class);

	private static SystemData sd;

	/** @throws Exception */
	@BeforeClass
	public static void setUp() throws Exception {
		sd = new SystemData();
		sd.getSysClassification(OmbConstants.CODE_CLASSIF_REGISTRI, null, 1);
	}

	/** */
//	@Test
	public void testClassifAdmStruct() { // CODE_CLASSIF_ADMIN_STR само административната структура ЙЕРАРХИЧНА
		try {
			List<SystemClassif> list = sd.queryClassification(CODE_CLASSIF_ADMIN_STR, "", new Date(), 1);
			LOGGER.info("АДМ.СТРУКТУРА");
			for (SystemClassif item : list) {
				LOGGER.info("\t" + item.getTekst() + " --> " + item.getDopInfo());
			}

			LOGGER.info("РАЗКОДИРАНЕ");
			LOGGER.info("\t" + sd.decodeItem(CODE_CLASSIF_ADMIN_STR, 6, 1, new Date()));

			LOGGER.info("СПЕЦИФИКИ");
			SystemClassif item = sd.decodeItemLite(CODE_CLASSIF_ADMIN_STR, 6, 1, new Date(), true);

			LOGGER.info("\tREF_TYPE=" + item.getSpecifics()[OmbClassifAdapter.ADM_STRUCT_INDEX_REF_TYPE]);
			LOGGER.info("\tREGISTRATURA=" + item.getSpecifics()[OmbClassifAdapter.ADM_STRUCT_INDEX_REGISTRATURA]);
			LOGGER.info("\tCONTACT_EMAIL=" + item.getSpecifics()[OmbClassifAdapter.ADM_STRUCT_INDEX_CONTACT_EMAIL]);

		} catch (Exception e) {
			fail(e.getMessage());
			LOGGER.error(e.getMessage(), e);
		}
	}

	/** */
	@Test
	public void testClassifEmplReplaces() {
		try {
//			List<SystemClassif> list = sd.getSysClassification(Constants.CODE_CLASSIF_EMPL_REPLACES, new Date(), 1);
//			for (SystemClassif item : list) {
//				LOGGER.info("\t" + item.getTekst());
//			}

			SystemClassif zamestnik = sd.decodeItemLite(Constants.CODE_CLASSIF_EMPL_REPLACES, 6, 1, new Date(), false);

			if (zamestnik != null) { // има заместник към подадената дата

				LOGGER.info(zamestnik.getTekst());
				LOGGER.info("код на заместник={}", zamestnik.getCodeExt());
			}

		} catch (Exception e) {
			fail(e.getMessage());
			LOGGER.error(e.getMessage(), e);
		}
	}

	/** */
//	@Test
	public void testClassifGroupEmployees() {
		// Това е за групи служители. По подбен начин може да се вземе всичко необходимо и за групи кореспонденти

		try {
			Date date = new Date();

			List<SystemClassif> allGroups = sd.getSysClassification(OmbConstants.CODE_CLASSIF_GROUP_EMPL, date, SysConstants.CODE_DEFAULT_LANG);

			LOGGER.info("---------------------------------------------------");
			LOGGER.info("Всички групи служители без значение регистратурата");
			for (SystemClassif group : allGroups) {
				LOGGER.info("\t" + group.getCode() + "-" + group.getTekst());
			}
			Integer registratura = 1; // в класификациите за групи служители има специфика по регистратура!
			Map<Integer, Object> specRegistratura = Collections.singletonMap(OmbClassifAdapter.REG_GROUP_INDEX_REGISTRATURA, registratura);
			List<SystemClassif> groupsByRegistratura = sd.queryClassification(OmbConstants.CODE_CLASSIF_GROUP_EMPL, null, date, SysConstants.CODE_DEFAULT_LANG, specRegistratura);

			LOGGER.info("---------------------------------------------------");
			LOGGER.info("Всички групи служители за регистратура 1");
			for (SystemClassif group : groupsByRegistratura) {
				LOGGER.info("\t" + group.getCode() + "-" + group.getTekst());
			}

			if (!groupsByRegistratura.isEmpty()) {
				Integer selectedGroup = groupsByRegistratura.get(0).getCode();

				// в групата има специфика на всички служители (кодовете с разделител ',')
				String emplCodes = (String) sd.getItemSpecific(OmbConstants.CODE_CLASSIF_GROUP_EMPL, selectedGroup, SysConstants.CODE_DEFAULT_LANG, date, OmbClassifAdapter.REG_GROUP_INDEX_MEMBERS);
				if (emplCodes != null) { // защото може и да е празна
					String[] codes = emplCodes.split(",");

					LOGGER.info("---------------------------------------------------");
					LOGGER.info("Всички служители в групата");
					for (String code : codes) {

						// взимам елемента заедно със спецификите
						SystemClassif empl = sd.decodeItemLite(Constants.CODE_CLASSIF_ADMIN_STR, Integer.valueOf(code), SysConstants.CODE_DEFAULT_LANG, date, true);

						LOGGER.info("CODE={}", empl.getCode());
						LOGGER.info("\tNAME={}", empl.getTekst());
						LOGGER.info("\tDOP_INFO={}", empl.getDopInfo());
						LOGGER.info("\tEMAIL={}", empl.getSpecifics()[OmbClassifAdapter.ADM_STRUCT_INDEX_CONTACT_EMAIL]);

						// по codeParent имам достъп до звеното, а като се разкодира и до името му
						LOGGER.info("\tZVENO_CODE={}", empl.getCodeParent());
						LOGGER.info("\tZVENO_NAME={}", sd.decodeItem(Constants.CODE_CLASSIF_ADMIN_STR, empl.getCodeParent(), SysConstants.CODE_DEFAULT_LANG, date));
					}
				}
			}

		} catch (Exception e) {
			fail(e.getMessage());
			LOGGER.error(e.getMessage(), e);
		}
	}

	/** */
//	@Test
	public void testClassifReferents() { // CODE_CLASSIF_REFERENTS може да се използва за търсене и разкодиране
		try {
			// търсене само в кореспонденти - специфика REFERENTS_INDEX_CORRESPONDENT=1
			List<SystemClassif> list = sd.queryClassification(CODE_CLASSIF_REFERENTS, "", new Date(), 1);
			LOGGER.info("КОРЕСПОНДЕНТИ");
			for (SystemClassif item : list) {
				LOGGER.info("\t" + item.getCode() + "-" + item.getTekst());
			}

			if (!list.isEmpty()) {
				LOGGER.info("РАЗКОДИРАНЕ");
				LOGGER.info("\t" + sd.decodeItem(CODE_CLASSIF_REFERENTS, list.get(0).getCode(), 1, new Date()));

				LOGGER.info("СПЕЦИФИКИ");
				SystemClassif item = sd.decodeItemLite(CODE_CLASSIF_REFERENTS, list.get(0).getCode(), 1, new Date(), true);

				System.out.println(item.getSpecifics()[0]);
				System.out.println(item.getSpecifics()[1]);
				System.out.println(item.getSpecifics()[2]);
			}

		} catch (Exception e) {
			fail(e.getMessage());
			LOGGER.error(e.getMessage(), e);
		}
	}

	/** */
//	@Test
	public void testClassifRegistri() {
		try {
			List<SystemClassif> list = new ArrayList<>();
			Map<Integer, Object> map = Collections.singletonMap(OmbClassifAdapter.REGISTRI_INDEX_DOC_TYPE, Optional.of(1));

			long t1 = System.currentTimeMillis();
			for (int i = 0; i < 100000; i++) {

				list = sd.queryClassification(OmbConstants.CODE_CLASSIF_REGISTRI, "индекс", null, 1, map);
			}
			long t2 = System.currentTimeMillis();

			for (SystemClassif item : list) {
				LOGGER.info("\t" + item.getTekst());
			}

			LOGGER.info("{}", t2 - t1);

		} catch (Exception e) {
			fail(e.getMessage());
			LOGGER.error(e.getMessage(), e);
		}
	}
	
	
//	@Test
	public void testNotifMap() {
		
		try {
			boolean result = sd.checkUserNotifSettings(-1, 11);
			System.out.println("Result is: " + result);
			
		} catch (DbErrorException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
	}
	
	
	
	
	
	
}