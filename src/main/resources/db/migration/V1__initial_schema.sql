CREATE TABLE purchases (
    id BIGSERIAL PRIMARY KEY,
    description VARCHAR(50) NOT NULL,
    transaction_amount DECIMAL(18,2) NOT NULL,
    transaction_currency VARCHAR(50) NOT NULL,
    transaction_date TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    idempotency_key UUID NOT NULL UNIQUE
);

CREATE INDEX idx_purchases_transaction_date ON purchases (transaction_date);
CREATE UNIQUE INDEX idx_purchases_idempotency_key ON purchases (idempotency_key);
