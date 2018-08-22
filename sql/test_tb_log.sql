create table test.tb_log
(
  id int auto_increment
    primary key,
  content text null,
  time datetime not null
)
  engine=InnoDB charset=latin1
;


INSERT INTO test.tb_log (id, content, time) VALUES (1, 'test', '2018-07-30 18:10:58');
INSERT INTO test.tb_log (id, content, time) VALUES (2, 'test', '2018-07-31 11:12:24');
INSERT INTO test.tb_log (id, content, time) VALUES (3, 'test sysdate', '2018-08-21 18:40:02');
INSERT INTO test.tb_log (id, content, time) VALUES (4, 'test sysdate', '2018-08-21 18:43:35');
INSERT INTO test.tb_log (id, content, time) VALUES (5, 'test sysdate', '2018-08-21 18:43:55');