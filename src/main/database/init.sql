CREATE SEQUENCE public.mask_entity_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;

CREATE SEQUENCE public.message_entity_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;

CREATE TABLE public.masks (
	id int4 DEFAULT nextval('mask_entity_id_seq'::regclass) NOT NULL,
	"name" varchar(255) NOT NULL,
	"label" varchar(255) NOT NULL,
	CONSTRAINT mask_entity_pkey PRIMARY KEY (id)
);

CREATE TABLE public.messages (
	id int4 DEFAULT nextval('message_entity_id_seq'::regclass) NOT NULL,
	mask_id int4 NOT NULL,
	text_prompt text NOT NULL,
	"timestamp" timestamptz NOT NULL,
	CONSTRAINT message_entity_pkey PRIMARY KEY (id),
	CONSTRAINT message_entity_mask_id_fkey FOREIGN KEY (mask_id) REFERENCES public.masks(id) ON DELETE CASCADE
);

