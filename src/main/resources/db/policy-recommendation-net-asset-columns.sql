ALTER TABLE policy_recommendation_profile
    ADD COLUMN household_net_asset_amount DECIMAL(19,2) NULL,
    ADD COLUMN prospective_household_head BOOLEAN NULL;
