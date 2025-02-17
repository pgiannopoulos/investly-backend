ALTER TABLE messages DROP CONSTRAINT IF EXISTS fk_messages_mask;


ALTER TABLE messages DROP COLUMN IF EXISTS mask_id;


