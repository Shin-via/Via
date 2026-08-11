ALTER TABLE policy_recommendation_profile
    ADD COLUMN basic_pension_recipient BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN disability_benefit_recipient BOOLEAN NOT NULL DEFAULT FALSE;
