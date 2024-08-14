DROP TABLE IF EXISTS check_rule;

CREATE TABLE check_rule
(
    check_rule_id VARCHAR(255)                       NOT NULL,
    rule_content  VARCHAR(255)                       NOT NULL,
    rule_name     VARCHAR(255),
    description   VARCHAR(255),
    create_dtm    datetime default CURRENT_TIMESTAMP not null,
    update_dtm    datetime null on update CURRENT_TIMESTAMP,
    PRIMARY KEY (check_rule_id)
);

DROP TABLE IF EXISTS check_rule_history;

CREATE TABLE check_rule_history
(
    check_rule_history_nid BIGINT AUTO_INCREMENT NOT NULL,
    check_rule_id          VARCHAR(255)                       NOT NULL,
    history_type_code      VARCHAR(10)                        NOT NULL,
    check_rule_content     VARCHAR(255)                       NOT NULL,
    create_dtm             datetime default CURRENT_TIMESTAMP not null,
    update_dtm             datetime null on update CURRENT_TIMESTAMP,
    PRIMARY KEY (check_rule_history_nid)
);
