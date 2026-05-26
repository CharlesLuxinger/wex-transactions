CREATE TABLE purchases (
    id BIGSERIAL PRIMARY KEY,
    description VARCHAR(50) NOT NULL,
    transaction_amount DECIMAL(18,2) NOT NULL,
    transaction_currency VARCHAR(3) NOT NULL,
    transaction_date TIMESTAMP WITH TIME ZONE NOT NULL,
    target_currency VARCHAR(3) NOT NULL,
    exchange_rate DECIMAL(18,2) NOT NULL,
    converted_amount DECIMAL(18,2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_purchases_transaction_date ON purchases (transaction_date);
