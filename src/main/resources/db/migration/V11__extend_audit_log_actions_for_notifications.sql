ALTER TABLE rental.audit_logs
    DROP CONSTRAINT IF EXISTS audit_logs_action_check;

ALTER TABLE rental.audit_logs
    ADD CONSTRAINT audit_logs_action_check
        CHECK (
            action IN (
                       'USER_CREATED',
                       'USER_UPDATED',
                       'USER_DELETED',

                       'TENANT_CREATED',
                       'TENANT_UPDATED',
                       'TENANT_DELETED',

                       'LOGIN_SUCCESS',
                       'LOGIN_FAILED',

                       'VEHICLE_CREATED',
                       'VEHICLE_UPDATED',
                       'VEHICLE_DELETED',

                       'RESERVATION_CREATED',
                       'RESERVATION_UPDATED',
                       'RESERVATION_CANCELLED',

                       'RENTAL_STARTED',
                       'RENTAL_COMPLETED',
                       'RENTAL_CANCELLED',

                       'PAYMENT_CREATED',
                       'PAYMENT_COMPLETED',
                       'PAYMENT_FAILED',

                       'REFUND_CREATED',
                       'REFUND_COMPLETED',

                       'INSURANCE_PACKAGE_CREATED',
                       'INSURANCE_PACKAGE_UPDATED',
                       'INSURANCE_PACKAGE_DELETED',

                       'NOTIFICATION_CREATED',
                       'NOTIFICATION_SENT',
                       'NOTIFICATION_FAILED'
                )
            );