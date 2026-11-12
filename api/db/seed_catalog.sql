-- Catalogue data. INSERT OR IGNORE keeps this file safe to run more than once.

INSERT OR IGNORE INTO services (id, name, price, duration_label) VALUES
    (1,  'Wash & Blow',          150, '45 min'),
    (2,  'Trim & Style',         120, '30 min'),
    (3,  'Box Braids',           450, '3-4 hours'),
    (4,  'Sew-in Weave',         350, '2-3 hours'),
    (5,  'Relaxer Treatment',    250, '1.5 hours'),
    (6,  'Full Style & Blowout', 200, '1 hour'),
    -- Extra sample services so the table holds 10 records (edit or remove to match the website).
    (7,  'Kids Cut',              80, '30 min'),
    (8,  'Fade & Beard Trim',    130, '45 min'),
    (9,  'Cornrows',             300, '2-3 hours'),
    (10, 'Silk Press',           280, '1.5 hours');

INSERT OR IGNORE INTO stylists (id, name, specialty, rating, reviews) VALUES
    (1, 'Asenathi Valashiya', 'Braiding Specialist', 4.9, 24),
    (2, 'Siphokazi Nofemele', 'Cuts & Relaxers',     4.8, 18),
    (3, 'Rethabile Minnaar',  'Weaves & Extensions', 4.9, 31),
    (4, 'Luna Summerton',     'Barber',              4.8, 22);
