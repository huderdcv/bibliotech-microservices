CREATE TABLE books
(
    id               BIGSERIAL PRIMARY KEY,
    isbn             VARCHAR(255) UNIQUE NOT NULL,
    title            VARCHAR(255)        NOT NULL,
    author           VARCHAR(255)        NOT NULL,
    total_copies     INT       DEFAULT 0,
    available_copies INT       DEFAULT 0,
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);