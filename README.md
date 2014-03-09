==================
LetsRuinMusicServer
===================

Server for the project https://github.com/pogestudio/LetsRuinMusic


==================
Preparation
==================

Unzip the dependencies in the folder depends/

Requires a MySQL server running. 
Manual preparation work is still needed. There has to be two tables, states and clients:
The rest should be configurable from config/config.json

```
create table states
(
ID INT NOT NULL,
X INT NOT NULL,
Y INT NOT NULL,
VAL INT NOT NULL
);
ALTER TABLE states ADD PRIMARY KEY (ID, X, Y);

create table clients
(
ID INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
X INT,
Y INT,
W INT,
H INT,
NAME VARCHAR(64)
);
INSERT INTO clients VALUES (1,0,0,0,0,"Global");
```

==================
Running the server
==================

There are at least two ways of starting the server:

1. Run start.bat or use the equivalent unix command.
2. Run the Deploy class from Eclipse.
