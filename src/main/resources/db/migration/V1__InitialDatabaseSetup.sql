CREATE TABLE PARFUM (
                     ID BIGSERIAL PRIMARY KEY,
                     NAME VARCHAR NOT NULL,
                     CATEGORY VARCHAR NOT NULL,
                     DESCRIPTION  VARCHAR NOT NULL,
                     PRICE INT NOT NULL,
                     STATUS VARCHAR NOT NULL
);

CREATE TABLE CARTS (
                    ID BIGSERIAL PRIMARY KEY,
                    USER_ID BIGSERIAL NOT NULL,
                    ITEM_ID BIGSERIAL NOT NULL
);

CREATE TABLE USERS (
                    ID BIGSERIAL PRIMARY KEY,
                    FIRST_NAME VARCHAR NOT NULL,
                    LAST_NAME VARCHAR NOT NULL,
                    EMAIL VARCHAR NOT NULL,
                    PASSWORD VARCHAR NOT NULL,
                    PHONE VARCHAR NOT NULL
);