CREATE TABLE orders(
    order_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    buyer_id UUID NOT NULL,
    order_sum DECIMAL(19,2) NOT NULL,
    order_status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    first_name VARCHAR (255) NOT NULL,
    last_name VARCHAR (255) NOT NULL,
    co VARCHAR (255),
    street_name VARCHAR (255) NOT NULL,
    street_name_2 VARCHAR (255),
    postal_code VARCHAR (10) NOT NULL,
    city VARCHAR (255) NOT NULL,
    country VARCHAR (255) NOT NULL

);

CREATE TABLE order_items(
    order_item_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL,
    product_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL,
    price_per_item DECIMAL(19,2) NOT NULL,

    CONSTRAINT fk_order FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE
)