package com.via.shinvia.lifecycle.reference.model;

import com.via.shinvia.lifecycle.common.model.HousingType;
import com.via.shinvia.lifecycle.common.model.VehicleClass;
import com.via.shinvia.lifecycle.common.model.VehicleCondition;

public final class LifecycleReferenceTypes {

    private LifecycleReferenceTypes() {
    }

    // 모든 이벤트 공통
    public static final String LIFESTYLE_COST_MULTIPLIER =
            "LIFESTYLE_COST_MULTIPLIER";

    // 결혼
    public static final String MARRIAGE_TOTAL_COST =
            "TOTAL_COST";

    // 출산
    public static final String POSTPARTUM_CARE_CENTER_COST =
            "POSTPARTUM_CARE_CENTER_COST";

    public static final String POSTPARTUM_HOME_COST =
            "POSTPARTUM_HOME_COST";

    public static final String MONTHLY_CHILDCARE_COST =
            "MONTHLY_CHILDCARE_COST";

    // 차량
    public static final String VEHICLE_BASE_PRICE =
            "VEHICLE_BASE_PRICE";

    public static final String VEHICLE_MONTHLY_MAINTENANCE_COST =
            "VEHICLE_MONTHLY_MAINTENANCE_COST";

    // 월세
    public static final String RENT_BASE_DEPOSIT =
            "RENT_BASE_DEPOSIT";

    public static final String MONTHLY_RENT_BASE_AMOUNT =
            "MONTHLY_RENT_BASE_AMOUNT";

    // 전세
    public static final String JEONSE_BASE_DEPOSIT =
            "JEONSE_BASE_DEPOSIT";

    // 주택구매
    public static final String HOME_BASE_PURCHASE_PRICE =
            "HOME_BASE_PURCHASE_PRICE";

    public static final String ACQUISITION_TAX_RATE =
            "ACQUISITION_TAX_RATE";

    // 주거 공통
    public static final String BASE_AREA_SQM =
            "BASE_AREA_SQM";

    // 상환
    public static final String PREPAYMENT_FEE_RATE =
            "PREPAYMENT_FEE_RATE";

    public static String vehicleConditionMultiplier(
            VehicleCondition condition
    ) {
        return "VEHICLE_CONDITION_MULTIPLIER_" + condition.name();
    }

    public static String vehicleClassMultiplier(
            VehicleClass vehicleClass
    ) {
        return "VEHICLE_CLASS_MULTIPLIER_" + vehicleClass.name();
    }

    public static String housingTypeMultiplier(
            HousingType housingType
    ) {
        return "HOUSING_TYPE_MULTIPLIER_" + housingType.name();
    }
}