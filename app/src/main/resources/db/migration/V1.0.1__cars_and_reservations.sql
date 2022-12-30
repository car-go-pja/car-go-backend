ALTER TABLE cargo.users
  ADD first_name VARCHAR(255) DEFAULT NULL,
  ADD last_name VARCHAR(255) DEFAULT NULL,
  ADD driving_licence VARCHAR(255) DEFAULT NULL,
  ADD phone VARCHAR(9) DEFAULT NULL,
  ADD balance NUMERIC(10,2) DEFAULT 0.0,
  ADD date_of_birth DATE DEFAULT NULL;

CREATE TABLE cargo.car_offers (
  id UUID DEFAULT gen_random_uuid() NOT NULL PRIMARY KEY,
  owner_id UUID REFERENCES cargo.users(id) ON DELETE CASCADE,
  make VARCHAR(128) NOT NULL,
  model VARCHAR(128) NOT NULL,
  year CHAR(4) NOT NULL,
  price_per_day NUMERIC(10,2) NOT NULL,
  horsepower CHAR(4) NOT NULL,
  fuel_type VARCHAR(16) NOT NULL,
  features varchar(255)[] NOT NULL,
  city VARCHAR(128) NOT NULL,
  seats_amount CHAR(2) NOT NULL,
  geolocation POINT DEFAULT NULL,
  created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE cargo.reservations (
  id UUID DEFAULT gen_random_uuid() NOT NULL PRIMARY KEY,
  renter_id UUID REFERENCES cargo.users(id) ON DELETE CASCADE,
  offer_id UUID REFERENCES cargo.car_offers(id) ON DELETE CASCADE,
  status VARCHAR(64) NOT NULL,
  start_date TIMESTAMPTZ NOT NULL,
  end_date TIMESTAMPTZ NOT NULL,
  total_price NUMERIC(10,2) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL
);
