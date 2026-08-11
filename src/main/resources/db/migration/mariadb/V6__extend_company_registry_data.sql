ALTER TABLE company
    ADD COLUMN registry_file_number VARCHAR(120),
    ADD COLUMN registry_registration_date DATE,
    ADD COLUMN incorporation_date DATE,
    ADD COLUMN share_capital DECIMAL(19, 2),
    ADD COLUMN share_capital_currency VARCHAR(12);
