CREATE TABLE rental.notifications (
                                      id BIGSERIAL PRIMARY KEY,

                                      tenant_id BIGINT NOT NULL,

                                      type VARCHAR(100) NOT NULL,
                                      channel VARCHAR(50) NOT NULL,
                                      status VARCHAR(50) NOT NULL,

                                      recipient VARCHAR(255) NOT NULL,
                                      subject VARCHAR(255) NOT NULL,

                                      message TEXT NOT NULL,
                                      error_message TEXT,

                                      sent_at TIMESTAMP,
                                      created_at TIMESTAMP NOT NULL,

                                      CONSTRAINT fk_notifications_tenant
                                          FOREIGN KEY (tenant_id)
                                              REFERENCES rental.tenants(id),

                                      CONSTRAINT notifications_type_check
                                          CHECK (
                                              type IN (
                                                       'PAYMENT_COMPLETED',
                                                       'RESERVATION_CONFIRMED',
                                                       'RESERVATION_EXPIRED',
                                                       'RENTAL_STARTED',
                                                       'RENTAL_COMPLETED',
                                                       'REFUND_PROCESSED'
                                                  )
                                              ),

                                      CONSTRAINT notifications_channel_check
                                          CHECK (
                                              channel IN (
                                                          'EMAIL',
                                                          'SMS',
                                                          'PUSH'
                                                  )
                                              ),

                                      CONSTRAINT notifications_status_check
                                          CHECK (
                                              status IN (
                                                         'PENDING',
                                                         'SENT',
                                                         'FAILED'
                                                  )
                                              )
);

CREATE INDEX idx_notifications_tenant_id
    ON rental.notifications(tenant_id);

CREATE INDEX idx_notifications_status
    ON rental.notifications(status);

CREATE INDEX idx_notifications_type
    ON rental.notifications(type);

CREATE INDEX idx_notifications_created_at
    ON rental.notifications(created_at);