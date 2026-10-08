CREATE TABLE expenses
(
    id           UUID                           NOT NULL,
    user_id      UUID                           NOT NULL,
    category     VARCHAR(255),
    description  VARCHAR(200),
    occurred_on  date,
    status       VARCHAR(255)                   NOT NULL,
    origin       VARCHAR(255)                   NOT NULL,
    capture_id   UUID                           NOT NULL,
    created_at   TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    updated_at   TIMESTAMP(6) WITHOUT TIME ZONE,
    confirmed_at TIMESTAMP(6) WITHOUT TIME ZONE,
    amount       DECIMAL,
    currency     VARCHAR(3),
    CONSTRAINT pk_expenses PRIMARY KEY (id)
);

CREATE TABLE expense_confirmations
(
    id         UUID                           NOT NULL,
    expense_id UUID,
    question   VARCHAR(200)                   NOT NULL,
    answer     VARCHAR(200),
    field      VARCHAR(255),
    status     VARCHAR(255),
    created_at TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITHOUT TIME ZONE,
    CONSTRAINT pk_expense_confirmations PRIMARY KEY (id)
);

ALTER TABLE expense_confirmations
    ADD CONSTRAINT FK_EXPENSE_CONFIRMATIONS_ON_EXPENSE FOREIGN KEY (expense_id) REFERENCES expenses (id);

INSERT INTO expenses (id, user_id, status, origin, capture_id, created_at)
VALUES ('1feba59f-c140-404f-aba5-9fc140804f28', 'e9d65866-55c1-4eeb-9658-6655c15eeba4', 'PENDING_CONFIRMATION', 'MANUAL',
        gen_random_uuid(), now());