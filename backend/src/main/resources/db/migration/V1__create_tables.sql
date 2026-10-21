CREATE TABLE users (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    created_at    TIMESTAMP NOT NULL
);

CREATE TABLE orders (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    created_by       INTEGER NOT NULL REFERENCES users(id),
    customer_name    VARCHAR(150) NOT NULL,
    delivery_address VARCHAR(255) NOT NULL,
    status           VARCHAR(30) NOT NULL,
    created_at       TIMESTAMP NOT NULL,
    updated_at       TIMESTAMP NOT NULL
);

CREATE TABLE order_items (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    order_id     INTEGER NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_name VARCHAR(150) NOT NULL,
    quantity     INTEGER NOT NULL CHECK (quantity > 0)
);

CREATE INDEX idx_orders_status ON orders(status);