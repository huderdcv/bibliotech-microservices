CREATE TABLE loans
(
    id         BIGSERIAL PRIMARY KEY,
    book_isbn  VARCHAR(255)                                       NOT NULL,
    member_id  VARCHAR(255)                                       NOT NULL,
    loan_date  DATE                                               NOT NULL,
    due_date   DATE                                               NOT NULL,
    status     VARCHAR(50)                                        NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);