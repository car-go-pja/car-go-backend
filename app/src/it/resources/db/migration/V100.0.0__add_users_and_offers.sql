INSERT INTO cargo.users (id, email, password, is_verified, balance, first_name, last_name, driving_licence, phone, date_of_birth) VALUES
    ('fd45598d-134a-4c1f-823d-149b0406b08f', 'cargo@email.com', '$2a$10$oJ.6/333b1IF79mTWg9c4eIlUgaWNGKZ3Ukf4h4YgHiIrUM4GSaUy', true, 0.0, 'Stephen', 'Johnson', 'xyz', '123456789', '2000-01-01'),
    ('32358234-f79f-46a5-93a8-9c3494d505a0', 'cargo@other.com', '$2a$10$oJ.6/333b1IF79mTWg9c4eIlUgaWNGKZ3Ukf4h4YgHiIrUM4GSaUy', true, 4000.0, 'Donald', 'Trump', 'xyz', '123456789', '2000-01-01');

INSERT INTO cargo.car_offers (id, owner_id, make, model, year, price_per_day, horsepower, fuel_type, features, city, seats_amount, geolocation, img_urls, created_at) VALUES
    ('f53a8a80-94b0-4aab-9ef0-36a53befe69e', 'fd45598d-134a-4c1f-823d-149b0406b08f', 'bmw', 'x7', '2020', 419.0, '500', 'diesel', '{four_by_four,ac,panorama_roof}', 'Warszawa', '5', NULL, '{}', NOW());

INSERT INTO cargo.reservations (id, renter_id, offer_id, status, start_date, end_date, total_price, created_at) VALUES
    ('10f0213b-8617-4d8c-9546-e0a19c3de762', '32358234-f79f-46a5-93a8-9c3494d505a0', 'f53a8a80-94b0-4aab-9ef0-36a53befe69e', 'accepted', '2023-01-14T00:00:00Z', '2023-01-18T00:00:00Z', 1776.0, NOW());