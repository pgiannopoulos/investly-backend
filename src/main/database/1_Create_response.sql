DROP TABLE IF EXISTS response;
DROP SEQUENCE IF EXISTS response_seq;

CREATE SEQUENCE response_seq START 1;

CREATE TABLE response (
    id BIGINT PRIMARY KEY DEFAULT nextval('response_seq'),
    message TEXT NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    message_id BIGINT NOT NULL,
    CONSTRAINT fk_message
        FOREIGN KEY (message_id)
        REFERENCES messages(id)
        ON DELETE CASCADE
);
