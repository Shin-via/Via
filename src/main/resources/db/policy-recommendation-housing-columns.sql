ALTER TABLE policy_recommendation_profile
    ADD COLUMN household_annual_income DECIMAL(19,2) NULL,
    ADD COLUMN homeless_household BOOLEAN NULL,
    ADD COLUMN household_head BOOLEAN NULL,
    ADD COLUMN jeonse_fraud_victim BOOLEAN NOT NULL DEFAULT FALSE;
