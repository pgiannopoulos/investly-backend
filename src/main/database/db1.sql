DROP TABLE IF EXISTS responses;

CREATE TABLE responses (
    id SERIAL PRIMARY KEY,                              -- Auto-incrementing primary key.
    message TEXT NOT NULL,                              -- Column for the message.
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,        -- Timestamp column.
    message_id INT NOT NULL,                            -- Foreign key column.
    CONSTRAINT fk_message
        FOREIGN KEY (message_id)
        REFERENCES messages(id)                         -- Adjust table name if your messages table is named differently.
        ON DELETE CASCADE                               -- Optional: deletes user_messages rows if the referenced message is deleted.
);
