INSERT INTO cargo.users (id, email, password, is_verified) VALUES
    ('fd45598d-134a-4c1f-823d-149b0406b08f', 'cargo@email.com', '$2a$10$oJ.6/333b1IF79mTWg9c4eIlUgaWNGKZ3Ukf4h4YgHiIrUM4GSaUy', true);

INSERT INTO cargo.car_offers (id, owner_id, make, model, year, price_per_day, horsepower, fuel_type, features, city, seats_amount, geolocation, img_urls, created_at) VALUES
    ('f53a8a80-94b0-4aab-9ef0-36a53befe69e', 'fd45598d-134a-4c1f-823d-149b0406b08f', 'bmw', 'x7', '2020', 419.0, '500', 'diesel', '{four_by_four,ac,panorama_roof}', 'Warszawa', '5', NULL, '{}', NOW());