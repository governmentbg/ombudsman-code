-- коментар за заявката
update SYSCLASSIF_MULTILANG set TEKST = 'Ново вложените документи и преписки се движат със съдържащата ги преписка' where TEKST_KEY in (select TEKST_KEY from SYSTEM_CLASSIF where CODE_CLASSIF = 151 and CODE = 9)
go

INSERT INTO SYSCLASSIF_MULTILANG(TEKST_KEY, LANG, TEKST, TEKSTOPI)
VALUES(1556, 1, 'Право да администрира Групи служители и Групи кореспонденти на всички регистратури', NULL);
GO

INSERT INTO SYSTEM_CLASSIF(ID, CODE, CODE_CLASSIF, TEKST_KEY, CODE_PREV, CODE_PARENT, CODE_EXT, LEVEL_NUMBER, DATE_OT, DATE_DO, DOP_INFO, USER_REG, DATE_REG, USER_LAST_MOD, DATE_LAST_MOD)
VALUES(1539, 20, 139, 1556, 19, 0, NULL, 1, TO_DATE('1901-01-01 00:00:00','YYYY-MM-DD HH24:MI:SS'), NULL, NULL, -1, TO_TIMESTAMP('1901-01-01 00:00:00:0','YYYY-MM-DD HH24:MI:SS:FF'), NULL, NULL)
GO

-- от тука се маха класификацията на регистрите (144)
update SYSTEM_OPTIONS set OPTION_VALUE = '104,105,129,157,160' where OPTION_LABEL = 'system.classificationsNotFilteredIfNotGranted'
go

-- маха се дефинитивно право 'Право да въвежда и редактира кореспонденти'
delete from ADM_USER_ROLES where CODE_CLASSIF = 139 and CODE_ROLE = 19;

delete from ADM_GROUP_ROLES where CODE_CLASSIF = 139 and CODE_ROLE = 19;

delete from SYSCLASSIF_MULTILANG where TEKST_KEY in (select TEKST_KEY from SYSTEM_CLASSIF where CODE_CLASSIF = 139 and CODE = 19)
go
delete from SYSTEM_CLASSIF where CODE_CLASSIF = 139 and CODE = 19
go
update SYSTEM_CLASSIF set CODE_PREV = 18 where CODE_CLASSIF = 139 and CODE_PREV = 19
go


CREATE VIEW V_PRAVA ( USER_ID, CODE_CLASSIF, CODE_ROLE, GROUP_ID )
AS
select ur.USER_ID, ur.CODE_CLASSIF, ur.CODE_ROLE, 0 GROUP_ID from ADM_USER_ROLES ur
    union
    select ug.USER_ID, gr.CODE_CLASSIF, gr.CODE_ROLE, ug.GROUP_ID from ADM_USER_GROUP ug inner join ADM_GROUP_ROLES gr on gr.GROUP_ID = ug.GROUP_ID
GO


-- за подписване на дефиниция и изпълнение на задача
alter table task
add sign_def varchar(8192)
go
alter table task
add sign_exe varchar(8192)
go
INSERT INTO SYSTEM_OPTIONS(ID, OPTION_LABEL, OPTION_VALUE, USER_EDITABLE, DOP_INFO, USER_REG, DATE_REG) 
	VALUES(80, 'delo.taskDefExeSign', '1', 't', 'Включено подписване на дефиниция и изпълнение на задача (1-да/ 0-не)', -1, TO_DATE('1901-01-01 00:00:00','YYYY-MM-DD HH24:MI:SS'))
GO

-- нова справка
INSERT INTO SYSCLASSIF_MULTILANG(TEKST_KEY, LANG, TEKST, TEKSTOPI)
VALUES(1275, 1, 'Предоставени права', NULL)
GO
INSERT INTO SYSTEM_CLASSIF(ID, CODE, CODE_CLASSIF, TEKST_KEY, CODE_PREV, CODE_PARENT, CODE_EXT, LEVEL_NUMBER, DATE_OT, DATE_DO, DOP_INFO, USER_REG, DATE_REG, USER_LAST_MOD, DATE_LAST_MOD)
VALUES(1429, 107, 7, 1275, 106, 64, NULL, 1, TO_DATE('1901-01-01 00:00:00','YYYY-MM-DD HH24:MI:SS'), NULL, NULL, -1, TO_TIMESTAMP('1901-01-01 00:00:00:0','YYYY-MM-DD HH24:MI:SS:FF'), NULL, NULL)
GO
update SYSTEM_CLASSIF set CODE_PREV = 107 where CODE_CLASSIF = 7 and CODE = 49
go

-- !!! нови настройки за подписите !!! при клиентите е важно каква е стойността
INSERT INTO SYSTEM_OPTIONS(ID, OPTION_LABEL, OPTION_VALUE, USER_EDITABLE, DOP_INFO, USER_REG, DATE_REG, USER_LAST_MOD, DATE_LAST_MOD)
VALUES(313, 'commonTrustStorePass', 'krasig', 'f', NULL, -1, TO_TIMESTAMP('1901-01-01 00:00:00:0','YYYY-MM-DD HH24:MI:SS:FF'), NULL, NULL)
GO
INSERT INTO SYSTEM_OPTIONS(ID, OPTION_LABEL, OPTION_VALUE, USER_EDITABLE, DOP_INFO, USER_REG, DATE_REG, USER_LAST_MOD, DATE_LAST_MOD)
VALUES(314, 'commonTrustStoreURL', '/opt/wildfly-16.0.0.Final/standalone/configuration/trustStore.jks', 'f', NULL, -1, TO_TIMESTAMP('1901-01-01 00:00:00:0','YYYY-MM-DD HH24:MI:SS:FF'), NULL, NULL)
GO

-- IB_BEGIN_UACTION
-- и тука може да има коментар
Има две нови настройки 'commonTrustStorePass' и 'commonTrustStoreURL'. Стойността при инсталация трябва да се нагласи.
-- IB_END_UACTION

-- ===== PosgreSQL ====
-- create new column
alter table user_notifications
	add column details2 varchar(2000) null
go
-- copy data from old to new columen
update user_notifications set details2 = SUBSTR(details,1,2000)
go
-- drop old column
alter table user_notifications drop column details
go
-- rename new column
alter table user_notifications rename column details2 to details
go

-- махане на безмисени настройки
delete from SYSTEM_OPTIONS where OPTION_LABEL in ('system.addUserCert', 'system.changeOwnPassword')
go


INSERT INTO SYSTEM_OPTIONS(ID, OPTION_LABEL, OPTION_VALUE, USER_EDITABLE, DOP_INFO, USER_REG, DATE_REG)
VALUES(81, 'delo.newUserSendMail', '0', 't', 'Да се изпраща мейл при регистрация и смяна на парола на потребител (1-да/ 0-не)', -1, TO_TIMESTAMP('1901-01-01 00:00:00:0','YYYY-MM-DD HH24:MI:SS:FF'))
GO

-- заради лдап-а трябва и празно да се пуска
ALTER TABLE adm_users ALTER password DROP NOT NULL
GO