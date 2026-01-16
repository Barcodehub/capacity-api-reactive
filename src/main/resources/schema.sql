CREATE TABLE IF NOT EXISTS capacity (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(90) NOT NULL
);

CREATE TABLE IF NOT EXISTS capacity_technology (
    id BIGSERIAL PRIMARY KEY,
    capacity_id BIGINT NOT NULL,
    technology_id BIGINT NOT NULL,
    CONSTRAINT fk_capacity FOREIGN KEY (capacity_id) REFERENCES capacity(id),
    CONSTRAINT uk_capacity_technology UNIQUE (capacity_id, technology_id)
);



