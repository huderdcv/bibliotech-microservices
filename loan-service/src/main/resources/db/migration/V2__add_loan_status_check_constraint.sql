-- Add a CHECK constraint to enforce the exact Java enum values at the DB level
ALTER TABLE loans
    ADD CONSTRAINT chk_loan_status
        CHECK (status IN ('PENDING', 'ACTIVE', 'FAILED', 'RETURNED', 'OVERDUE'));