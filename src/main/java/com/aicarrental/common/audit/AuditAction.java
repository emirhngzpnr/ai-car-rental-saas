package com.aicarrental.common.audit;

public enum AuditAction {
    USER_CREATED,
    USER_UPDATED,
    USER_DELETED,

    TENANT_CREATED,
    TENANT_UPDATED,
    TENANT_DELETED,

    LOGIN_SUCCESS,
    LOGIN_FAILED,

    VEHICLE_CREATED,
    VEHICLE_UPDATED,
    VEHICLE_DELETED

}
