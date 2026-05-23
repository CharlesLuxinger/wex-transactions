CREATE TABLE purchases (
    id BIGSERIAL PRIMARY KEY,
    description VARCHAR(255) NOT NULL,
    transaction_amount DECIMAL(18,6) NOT NULL,
    transaction_currency VARCHAR(3) NOT NULL,
    transaction_date TIMESTAMP WITH TIME ZONE NOT NULL,
    target_currency VARCHAR(3) NOT NULL,
    exchange_rate DECIMAL(18,6) NOT NULL,
    converted_amount DECIMAL(18,6) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_purchases_transaction_date ON purchases (transaction_date);

CREATE TABLE exchange_rates (
    id BIGSERIAL PRIMARY KEY,
    rate_date DATE NOT NULL,
    source_currency VARCHAR(3) NOT NULL,
    target_currency VARCHAR(3) NOT NULL,
    exchange_rate DECIMAL(18,6) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_exchange_rates_date_source_target UNIQUE (rate_date, source_currency, target_currency)
);
