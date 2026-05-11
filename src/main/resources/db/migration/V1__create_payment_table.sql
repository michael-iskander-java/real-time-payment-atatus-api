CREATE TABLE public.payment (
    id                BIGSERIAL PRIMARY KEY,
    reference_id      VARCHAR(64)    NOT NULL,
    amount            NUMERIC(19, 4) NOT NULL,
    currency          CHAR(3)        NOT NULL,
    debtor_name       VARCHAR(255)   NOT NULL,
    debtor_iban       VARCHAR(34)    NOT NULL,
    creditor_iban     VARCHAR(34)    NOT NULL,
    value_date        DATE           NOT NULL,
    status            VARCHAR(20)    NOT NULL,
    event_timestamp   TIMESTAMPTZ    NOT NULL,
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ    NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_payment_reference_id UNIQUE (reference_id),
    CONSTRAINT chk_payment_status CHECK (status IN ('PENDING', 'PROCESSING', 'SETTLED', 'REJECTED'))
);

CREATE INDEX idx_payment_reference_id ON public.payment (reference_id);