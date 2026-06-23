CREATE UNIQUE INDEX IF NOT EXISTS uk_payment_transactions_success_deposit_per_reservation
    ON rental.payment_transactions(reservation_id)
    WHERE reservation_id IS NOT NULL
      AND payment_type = 'DEPOSIT_PAYMENT'
      AND payment_status = 'SUCCESS';
