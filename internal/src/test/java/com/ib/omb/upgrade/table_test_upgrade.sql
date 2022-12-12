-- зачиствам е изцяло
DELETE from TABLE_TEST_UPGRADE;

-- зареждам някакви данни
INSERT INTO TABLE_TEST_UPGRADE(ID, NUM_COL, DATE_COL, TEXT_COL) 
	VALUES(1, 2, SYSDATE, 'текст1')
GO
INSERT INTO TABLE_TEST_UPGRADE(ID, NUM_COL, DATE_COL, TEXT_COL) 
	VALUES(2, 2, SYSDATE, 'текст2')
GO
INSERT INTO TABLE_TEST_UPGRADE(ID, NUM_COL, DATE_COL, TEXT_COL) 
	VALUES(3, 2, SYSDATE, 'текст3');
GO

-- добавям нова колона
alter table TABLE_TEST_UPGRADE
add NUM_COL2 NUMBER(10 , 0);

INSERT INTO TABLE_TEST_UPGRADE(ID, NUM_COL, DATE_COL, TEXT_COL, NUM_COL2) 
	VALUES(4, 2, SYSDATE, 'текст3', '123456');
GO

-- IB_COMMIT

-- и после тази нова колона се маха
alter table TABLE_TEST_UPGRADE
drop column NUM_COL2;

-- IB_BEGIN_UACTION
Потребителят трябва да провери дали NUM_COL2 съществува.
-- IB_END_UACTION