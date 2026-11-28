INSERT INTO products (code, name, category, unit, default_rate, stock, status, image_path) VALUES
('jar',   '20 Ltr Jar',   'Jar',       'jar',    40.00, 1240, 'ACTIVE', 'assets/products/icon-jar-20l.png'),
('ch',    'Chilled Jar',  'Chilled',   'jar',    50.00,  860, 'ACTIVE', 'assets/products/icon-jar-chilled.png'),
('pouch', 'Pouch',        'Pouch',     'pouch',  35.00,  420, 'ACTIVE', 'assets/products/icon-pouch.png'),
('l2',    '2 Ltr',        'Bottle',    'bottle', 50.00,   95, 'ACTIVE', 'assets/products/icon-bottle-2l.png'),
('l1',    '1 Ltr',        'Bottle',    'bottle', 90.00,  180, 'ACTIVE', 'assets/products/icon-bottle-1l.png'),
('ml500', '500 ml',       'Bottle',    'bottle',120.00,  210, 'ACTIVE', 'assets/products/icon-bottle-500ml.png'),
('ml250', '250 ml',       'Bottle',    'bottle',120.00,  260, 'ACTIVE', 'assets/products/icon-bottle-250ml.png'),
('disp',  'Dispenser',    'Accessory', 'pcs',     0.00,   45, 'ACTIVE', 'assets/products/icon-dispenser.png');

INSERT INTO customers (name, phone, city_area, type, card_no, status) VALUES
('G R Electrical', '9876500001', 'Sitapura', 'DEALER', 'PA-1001', 'ACTIVE'),
('Sharma Traders', '9876500002', 'Malviya Nagar', 'RETAIL', 'PA-1002', 'ACTIVE'),
('Hotel Pearl', '9876500003', 'Tonk Road', 'INSTITUTION', 'PA-1003', 'ACTIVE'),
('Anand Kirana', '9876500004', 'Jagatpura', 'RETAIL', 'PA-1004', 'ACTIVE'),
('City Water Point', '9876500005', 'Mansarovar', 'DEALER', 'PA-1005', 'ACTIVE');
