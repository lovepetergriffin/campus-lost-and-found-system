CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(30) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    contact VARCHAR(100),
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    sort_order INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL,
    category_id BIGINT NOT NULL,
    location VARCHAR(200) NOT NULL,
    event_time TIMESTAMP NOT NULL,
    description VARCHAR(500) NOT NULL,
    image_url VARCHAR(500),
    contact VARCHAR(100),
    status VARCHAR(20) NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_item_category FOREIGN KEY (category_id) REFERENCES category(id),
    CONSTRAINT fk_item_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
);

CREATE TABLE IF NOT EXISTS claim (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_id BIGINT NOT NULL,
    applicant_id BIGINT NOT NULL,
    description VARCHAR(500) NOT NULL,
    proof VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP,
    CONSTRAINT uk_claim_item_applicant UNIQUE (item_id, applicant_id),
    CONSTRAINT fk_claim_item FOREIGN KEY (item_id) REFERENCES item(id),
    CONSTRAINT fk_claim_user FOREIGN KEY (applicant_id) REFERENCES sys_user(id)
);

CREATE TABLE IF NOT EXISTS review_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_id BIGINT NOT NULL,
    reviewer_id BIGINT NOT NULL,
    result VARCHAR(20) NOT NULL,
    comment VARCHAR(500),
    review_time TIMESTAMP NOT NULL,
    CONSTRAINT fk_review_item FOREIGN KEY (item_id) REFERENCES item(id),
    CONSTRAINT fk_review_user FOREIGN KEY (reviewer_id) REFERENCES sys_user(id)
);

CREATE TABLE IF NOT EXISTS notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    receiver_id BIGINT NOT NULL,
    type VARCHAR(40) NOT NULL,
    content VARCHAR(500) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_notification_user FOREIGN KEY (receiver_id) REFERENCES sys_user(id)
);
