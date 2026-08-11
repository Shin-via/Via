ALTER TABLE policy_recommendation_profile
    ADD COLUMN north_korean_defector BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN child_headed_household BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN earned_income_tax_credit_recipient BOOLEAN NOT NULL DEFAULT FALSE;
