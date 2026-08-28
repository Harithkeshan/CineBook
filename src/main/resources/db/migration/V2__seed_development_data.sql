-- CineBook Development Seed Data Migration (V2)

-- 1. Locations
INSERT INTO locations (name, address, city) VALUES
('CineBook Colombo', 'Colombo City Centre, 137 Sir James Pieris Mawatha', 'Colombo'),
('CineBook Kandy', 'Kandy City Centre, 5 Dalada Veediya', 'Kandy');

-- 2. Halls
INSERT INTO halls (location_id, name) VALUES
((SELECT id FROM locations WHERE name = 'CineBook Colombo'), 'Screen 1 (Dolby Atmos)'),
((SELECT id FROM locations WHERE name = 'CineBook Colombo'), 'Screen 2 (IMAX 3D)'),
((SELECT id FROM locations WHERE name = 'CineBook Kandy'), 'Screen 1 (VIP Cinema)');

-- 3. Sections
INSERT INTO sections (hall_id, name, type) VALUES
((SELECT id FROM halls WHERE name = 'Screen 1 (Dolby Atmos)' AND location_id = (SELECT id FROM locations WHERE name = 'CineBook Colombo')), 'Ground Section', 'GROUND'),
((SELECT id FROM halls WHERE name = 'Screen 1 (Dolby Atmos)' AND location_id = (SELECT id FROM locations WHERE name = 'CineBook Colombo')), 'Balcony Section', 'BALCONY'),
((SELECT id FROM halls WHERE name = 'Screen 2 (IMAX 3D)' AND location_id = (SELECT id FROM locations WHERE name = 'CineBook Colombo')), 'Ground Section', 'GROUND'),
((SELECT id FROM halls WHERE name = 'Screen 1 (VIP Cinema)' AND location_id = (SELECT id FROM locations WHERE name = 'CineBook Kandy')), 'Ground Section', 'GROUND'),
((SELECT id FROM halls WHERE name = 'Screen 1 (VIP Cinema)' AND location_id = (SELECT id FROM locations WHERE name = 'CineBook Kandy')), 'Balcony Section', 'BALCONY');

-- 4. Seats
-- Colombo Hall 1 - Ground Section (32 seats: Row A1-A8, Row B1-B8, Row C1-C8, Row D1-D8)
INSERT INTO seats (section_id, row_label, seat_number, seat_type, position_x, position_y, is_active)
SELECT 
    sec.id,
    'A',
    n::text,
    CASE WHEN n IN (1, 2) THEN 'ACCESSIBLE' ELSE 'STANDARD' END,
    n,
    1,
    TRUE
FROM sections sec
JOIN halls h ON sec.hall_id = h.id
JOIN locations l ON h.location_id = l.id
CROSS JOIN generate_series(1, 8) AS n
WHERE l.name = 'CineBook Colombo' AND h.name = 'Screen 1 (Dolby Atmos)' AND sec.type = 'GROUND';

INSERT INTO seats (section_id, row_label, seat_number, seat_type, position_x, position_y, is_active)
SELECT 
    sec.id,
    'B',
    n::text,
    'STANDARD',
    n,
    2,
    TRUE
FROM sections sec
JOIN halls h ON sec.hall_id = h.id
JOIN locations l ON h.location_id = l.id
CROSS JOIN generate_series(1, 8) AS n
WHERE l.name = 'CineBook Colombo' AND h.name = 'Screen 1 (Dolby Atmos)' AND sec.type = 'GROUND';

INSERT INTO seats (section_id, row_label, seat_number, seat_type, position_x, position_y, is_active)
SELECT 
    sec.id,
    'C',
    n::text,
    'STANDARD',
    n,
    3,
    TRUE
FROM sections sec
JOIN halls h ON sec.hall_id = h.id
JOIN locations l ON h.location_id = l.id
CROSS JOIN generate_series(1, 8) AS n
WHERE l.name = 'CineBook Colombo' AND h.name = 'Screen 1 (Dolby Atmos)' AND sec.type = 'GROUND';

INSERT INTO seats (section_id, row_label, seat_number, seat_type, position_x, position_y, is_active)
SELECT 
    sec.id,
    'D',
    n::text,
    'PREMIUM',
    n,
    4,
    TRUE
FROM sections sec
JOIN halls h ON sec.hall_id = h.id
JOIN locations l ON h.location_id = l.id
CROSS JOIN generate_series(1, 8) AS n
WHERE l.name = 'CineBook Colombo' AND h.name = 'Screen 1 (Dolby Atmos)' AND sec.type = 'GROUND';

-- Colombo Hall 1 - Balcony Section (18 seats: Rows A1-A6, B1-B6, C1-C6)
INSERT INTO seats (section_id, row_label, seat_number, seat_type, position_x, position_y, is_active)
SELECT 
    sec.id,
    r.row_label,
    n::text,
    'PREMIUM',
    n,
    r.y_pos,
    TRUE
FROM sections sec
JOIN halls h ON sec.hall_id = h.id
JOIN locations l ON h.location_id = l.id
CROSS JOIN generate_series(1, 6) AS n
CROSS JOIN (
    SELECT 'A' AS row_label, 5 AS y_pos UNION ALL
    SELECT 'B', 6 UNION ALL
    SELECT 'C', 7
) r
WHERE l.name = 'CineBook Colombo' AND h.name = 'Screen 1 (Dolby Atmos)' AND sec.type = 'BALCONY';

-- Colombo Hall 2 - Ground Section (24 seats)
INSERT INTO seats (section_id, row_label, seat_number, seat_type, position_x, position_y, is_active)
SELECT 
    sec.id,
    'A',
    n::text,
    CASE WHEN n IN (1, 2) THEN 'ACCESSIBLE' ELSE 'STANDARD' END,
    n,
    1,
    TRUE
FROM sections sec
JOIN halls h ON sec.hall_id = h.id
JOIN locations l ON h.location_id = l.id
CROSS JOIN generate_series(1, 6) AS n
WHERE l.name = 'CineBook Colombo' AND h.name = 'Screen 2 (IMAX 3D)' AND sec.type = 'GROUND';

INSERT INTO seats (section_id, row_label, seat_number, seat_type, position_x, position_y, is_active)
SELECT 
    sec.id,
    r.row_label,
    n::text,
    r.stype,
    n,
    r.y_pos,
    TRUE
FROM sections sec
JOIN halls h ON sec.hall_id = h.id
JOIN locations l ON h.location_id = l.id
CROSS JOIN generate_series(1, 6) AS n
CROSS JOIN (
    SELECT 'B' AS row_label, 2 AS y_pos, 'STANDARD' AS stype UNION ALL
    SELECT 'C', 3, 'STANDARD' UNION ALL
    SELECT 'D', 4, 'PREMIUM'
) r
WHERE l.name = 'CineBook Colombo' AND h.name = 'Screen 2 (IMAX 3D)' AND sec.type = 'GROUND';

-- Kandy Hall 1 - Ground Section (18 seats)
INSERT INTO seats (section_id, row_label, seat_number, seat_type, position_x, position_y, is_active)
SELECT 
    sec.id,
    'A',
    n::text,
    CASE WHEN n IN (1, 2) THEN 'ACCESSIBLE' ELSE 'STANDARD' END,
    n,
    1,
    TRUE
FROM sections sec
JOIN halls h ON sec.hall_id = h.id
JOIN locations l ON h.location_id = l.id
CROSS JOIN generate_series(1, 6) AS n
WHERE l.name = 'CineBook Kandy' AND h.name = 'Screen 1 (VIP Cinema)' AND sec.type = 'GROUND';

INSERT INTO seats (section_id, row_label, seat_number, seat_type, position_x, position_y, is_active)
SELECT 
    sec.id,
    r.row_label,
    n::text,
    r.stype,
    n,
    r.y_pos,
    TRUE
FROM sections sec
JOIN halls h ON sec.hall_id = h.id
JOIN locations l ON h.location_id = l.id
CROSS JOIN generate_series(1, 6) AS n
CROSS JOIN (
    SELECT 'B' AS row_label, 2 AS y_pos, 'STANDARD' AS stype UNION ALL
    SELECT 'C', 3, 'PREMIUM'
) r
WHERE l.name = 'CineBook Kandy' AND h.name = 'Screen 1 (VIP Cinema)' AND sec.type = 'GROUND';

-- Kandy Hall 1 - Balcony Section (8 seats)
INSERT INTO seats (section_id, row_label, seat_number, seat_type, position_x, position_y, is_active)
SELECT 
    sec.id,
    r.row_label,
    n::text,
    'PREMIUM',
    n,
    r.y_pos,
    TRUE
FROM sections sec
JOIN halls h ON sec.hall_id = h.id
JOIN locations l ON h.location_id = l.id
CROSS JOIN generate_series(1, 4) AS n
CROSS JOIN (
    SELECT 'A' AS row_label, 4 AS y_pos UNION ALL
    SELECT 'B', 5
) r
WHERE l.name = 'CineBook Kandy' AND h.name = 'Screen 1 (VIP Cinema)' AND sec.type = 'BALCONY';

-- 5. Movies
INSERT INTO movies (title, description, duration_minutes, language, genre, poster_url, trailer_url, release_date, age_rating) VALUES
('Galactic Horizons', 'An epic space odyssey exploring uncharted wormholes and deep space colonies.', 150, 'English', 'Sci-Fi', 'https://example.com/posters/galactic-horizons.jpg', 'https://example.com/trailers/galactic-horizons.mp4', '2026-09-01', 'PG-13'),
('The Shadow Protocol', 'A high-stakes espionage thriller set in the neon-lit alleys of futuristic Tokyo.', 125, 'English', 'Action', 'https://example.com/posters/shadow-protocol.jpg', 'https://example.com/trailers/shadow-protocol.mp4', '2026-09-15', 'R'),
('Whispers of the Forest', 'A magical animated tale of an ancient woodland spirit and a courageous herbalist.', 105, 'Japanese', 'Animation', 'https://example.com/posters/whispers-forest.jpg', 'https://example.com/trailers/whispers-forest.mp4', '2026-09-10', 'PG');

-- 6. Showtimes
INSERT INTO showtimes (movie_id, hall_id, start_time, end_time, status) VALUES
(
    (SELECT id FROM movies WHERE title = 'Galactic Horizons'),
    (SELECT h.id FROM halls h JOIN locations l ON h.location_id = l.id WHERE l.name = 'CineBook Colombo' AND h.name = 'Screen 1 (Dolby Atmos)'),
    '2026-09-20 10:00:00+00',
    '2026-09-20 12:30:00+00',
    'SCHEDULED'
),
(
    (SELECT id FROM movies WHERE title = 'The Shadow Protocol'),
    (SELECT h.id FROM halls h JOIN locations l ON h.location_id = l.id WHERE l.name = 'CineBook Colombo' AND h.name = 'Screen 1 (Dolby Atmos)'),
    '2026-09-20 14:00:00+00',
    '2026-09-20 16:05:00+00',
    'SCHEDULED'
),
(
    (SELECT id FROM movies WHERE title = 'Whispers of the Forest'),
    (SELECT h.id FROM halls h JOIN locations l ON h.location_id = l.id WHERE l.name = 'CineBook Colombo' AND h.name = 'Screen 2 (IMAX 3D)'),
    '2026-09-20 11:00:00+00',
    '2026-09-20 12:45:00+00',
    'SCHEDULED'
),
(
    (SELECT id FROM movies WHERE title = 'Galactic Horizons'),
    (SELECT h.id FROM halls h JOIN locations l ON h.location_id = l.id WHERE l.name = 'CineBook Kandy' AND h.name = 'Screen 1 (VIP Cinema)'),
    '2026-09-20 18:00:00+00',
    '2026-09-20 20:30:00+00',
    'SCHEDULED'
);

-- 7. Showtime Seats
INSERT INTO showtime_seats (showtime_id, seat_id, status, hold_expires_at)
SELECT 
    st.id AS showtime_id,
    s.id AS seat_id,
    'AVAILABLE' AS status,
    NULL AS hold_expires_at
FROM showtimes st
JOIN sections sec ON sec.hall_id = st.hall_id
JOIN seats s ON s.section_id = sec.id
WHERE s.is_active = TRUE;

-- 8. Pricing Rules
INSERT INTO pricing_rules (showtime_id, section_id, ticket_type, price)
SELECT 
    st.id AS showtime_id,
    sec.id AS section_id,
    tt.ticket_type,
    CASE 
        WHEN sec.type = 'BALCONY' AND tt.ticket_type = 'ADULT' THEN 1800.00
        WHEN sec.type = 'BALCONY' AND tt.ticket_type = 'CHILD' THEN 1200.00
        WHEN sec.type = 'GROUND' AND tt.ticket_type = 'ADULT' THEN 1200.00
        WHEN sec.type = 'GROUND' AND tt.ticket_type = 'CHILD' THEN 800.00
    END AS price
FROM showtimes st
JOIN sections sec ON sec.hall_id = st.hall_id
CROSS JOIN (
    SELECT 'ADULT' AS ticket_type UNION ALL SELECT 'CHILD' AS ticket_type
) tt;
