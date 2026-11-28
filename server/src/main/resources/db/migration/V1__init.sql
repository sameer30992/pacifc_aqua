CREATE TABLE users (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  name          VARCHAR(120) NOT NULL,
  phone         VARCHAR(20)  NOT NULL,
  email         VARCHAR(160) NULL,
  password_hash VARCHAR(100) NOT NULL,
  role          VARCHAR(20)  NOT NULL,
  status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
  avatar_path   VARCHAR(255) NULL,
  created_at    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_users_phone (phone),
  UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE customers (
  id               BIGINT PRIMARY KEY AUTO_INCREMENT,
  name             VARCHAR(160) NOT NULL,
  phone            VARCHAR(20)  NULL,
  city_area        VARCHAR(120) NULL,
  type             VARCHAR(20)  NOT NULL DEFAULT 'RETAIL',
  card_no          VARCHAR(40)  NULL,
  notes            VARCHAR(500) NULL,
  route_helper_id  BIGINT       NULL,
  avatar_path      VARCHAR(255) NULL,
  status           VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
  created_at       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  CONSTRAINT fk_customers_route_helper FOREIGN KEY (route_helper_id) REFERENCES users(id),
  KEY idx_customers_name (name),
  KEY idx_customers_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE products (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  code          VARCHAR(40)  NOT NULL,
  name          VARCHAR(120) NOT NULL,
  category      VARCHAR(60)  NOT NULL,
  unit          VARCHAR(40)  NOT NULL,
  default_rate  DECIMAL(12,2) NOT NULL DEFAULT 0,
  stock         INT          NOT NULL DEFAULT 0,
  status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
  image_path    VARCHAR(255) NULL,
  created_at    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_products_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE visits (
  id                 BIGINT PRIMARY KEY AUTO_INCREMENT,
  visit_no           VARCHAR(20)  NOT NULL,
  customer_id        BIGINT       NOT NULL,
  helper_id          BIGINT       NULL,
  visit_at           DATETIME(3)  NOT NULL,
  payment_mode       VARCHAR(20)  NOT NULL DEFAULT 'CASH',
  payment_status     VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
  amount_total       DECIMAL(12,2) NOT NULL DEFAULT 0,
  amount_paid        DECIMAL(12,2) NOT NULL DEFAULT 0,
  balance            DECIMAL(12,2) NOT NULL DEFAULT 0,
  remarks            VARCHAR(500) NULL,
  status             VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
  void_reason        VARCHAR(500) NULL,
  client_request_id  VARCHAR(64)  NULL,
  created_by         BIGINT       NULL,
  updated_by         BIGINT       NULL,
  created_at         DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at         DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_visits_visit_no (visit_no),
  UNIQUE KEY uk_visits_client_request (client_request_id),
  KEY idx_visits_visit_at (visit_at),
  KEY idx_visits_customer_at (customer_id, visit_at),
  KEY idx_visits_helper_at (helper_id, visit_at),
  CONSTRAINT fk_visits_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
  CONSTRAINT fk_visits_helper FOREIGN KEY (helper_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE visit_lines (
  id                 BIGINT PRIMARY KEY AUTO_INCREMENT,
  visit_id           BIGINT        NOT NULL,
  product_id         BIGINT        NOT NULL,
  product_name_snap  VARCHAR(120)  NOT NULL,
  rate_snap          DECIMAL(12,2) NOT NULL,
  qty_diya           INT           NOT NULL DEFAULT 0,
  qty_mila           INT           NOT NULL DEFAULT 0,
  line_amount        DECIMAL(12,2) NOT NULL DEFAULT 0,
  CONSTRAINT fk_visit_lines_visit FOREIGN KEY (visit_id) REFERENCES visits(id) ON DELETE CASCADE,
  CONSTRAINT fk_visit_lines_product FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE payments (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  visit_id     BIGINT        NOT NULL,
  amount       DECIMAL(12,2) NOT NULL,
  mode         VARCHAR(20)   NOT NULL,
  paid_at      DATETIME(3)   NOT NULL,
  recorded_by  BIGINT        NULL,
  created_at   DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  CONSTRAINT fk_payments_visit FOREIGN KEY (visit_id) REFERENCES visits(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE orders (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  customer_id     BIGINT       NOT NULL,
  helper_id       BIGINT       NOT NULL,
  priority        VARCHAR(20)  NOT NULL DEFAULT 'NORMAL',
  expected_hours  TINYINT      NULL,
  notes           VARCHAR(500) NULL,
  status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
  created_by      BIGINT       NULL,
  created_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  KEY idx_orders_helper_status (helper_id, status),
  CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
  CONSTRAINT fk_orders_helper FOREIGN KEY (helper_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE order_lines (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id    BIGINT NOT NULL,
  product_id  BIGINT NOT NULL,
  qty         INT    NOT NULL DEFAULT 1,
  CONSTRAINT fk_order_lines_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
  CONSTRAINT fk_order_lines_product FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE plant_trips (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  helper_id   BIGINT       NOT NULL,
  started_at  DATETIME(3)  NOT NULL,
  closed_at   DATETIME(3)  NULL,
  status      VARCHAR(20)  NOT NULL DEFAULT 'LOADED',
  notes       VARCHAR(500) NULL,
  created_at  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  CONSTRAINT fk_plant_trips_helper FOREIGN KEY (helper_id) REFERENCES users(id),
  KEY idx_plant_trips_started (started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE plant_trip_lines (
  id                 BIGINT PRIMARY KEY AUTO_INCREMENT,
  trip_id            BIGINT NOT NULL,
  product_id         BIGINT NOT NULL,
  qty_lifted         INT NOT NULL DEFAULT 0,
  qty_delivered      INT NOT NULL DEFAULT 0,
  qty_returned_full  INT NOT NULL DEFAULT 0,
  qty_empties        INT NOT NULL DEFAULT 0,
  variance_note      VARCHAR(500) NULL,
  CONSTRAINT fk_plant_trip_lines_trip FOREIGN KEY (trip_id) REFERENCES plant_trips(id) ON DELETE CASCADE,
  CONSTRAINT fk_plant_trip_lines_product FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE bill_sequences (
  ym       CHAR(7) PRIMARY KEY,
  last_seq INT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE visit_sequences (
  id       TINYINT PRIMARY KEY,
  last_seq BIGINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO visit_sequences (id, last_seq) VALUES (1, 0);
