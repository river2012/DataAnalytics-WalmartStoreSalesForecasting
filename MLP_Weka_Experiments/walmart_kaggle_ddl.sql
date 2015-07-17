// DDL script for importing data into MySQL

CREATE TABLE stores(
  store int PRIMARY KEY,
  type varchar(1),
  size int
)
COPY stores
FROM '/Users/Neha/Downloads/stores.csv'
(FORMAT 'csv', HEADER TRUE)

CREATE TABLE features(
  store int,
  date date,
  temperature real,
  fuelPrice real,
  markdown1 real null,
  markdown2 real null,
  markdown3 real null,
  markdown4 real null,
  markdown5 real null,
  cpi real null,
  unemployment real null,
  holiday boolean,
  PRIMARY KEY (store, date)
)
COPY features
FROM '/Users/Neha/Downloads/features.csv'
(FORMAT 'csv', HEADER TRUE)

CREATE TABLE features2(
  store int,
  date date,
  temperature real,
  fuelPrice real,
  markdown1 real null,
  markdown2 real null,
  markdown3 real null,
  markdown4 real null,
  markdown5 real null,
  cpi real null,
  unemployment real null,
  holiday boolean,
  PRIMARY KEY (store, date)
)
COPY features2
FROM '/Users/Neha/Downloads/features.csv'
(FORMAT 'csv', HEADER TRUE)

CREATE TABLE train(
  store int,
  dept int,
  date date,
  weeklySales real,
  holiday boolean,
  PRIMARY KEY (dept, store, date)
)
COPY train
FROM '/Users/Neha/Downloads/train.csv'
(FORMAT 'csv', HEADER TRUE)

CREATE TABLE test(
  store int,
  dept int,
  date date,
  holiday boolean,
  PRIMARY KEY (dept, store, date)
)
COPY test
FROM '/Users/Neha/Downloads/test.csv'
(FORMAT 'csv', HEADER TRUE)

CREATE TABLE weeks(
  date date PRIMARY KEY,
  year int,
  week int,
  holiday int,
  holiday1 int,
  holiday2 int,
  holiday3 int,
  holiday4 int
)
COPY weeks
FROM '/Users/Neha/Downloads/weeks.csv'
(FORMAT 'csv', HEADER TRUE)
