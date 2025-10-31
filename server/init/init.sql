-- initialize submissions table and seed with example rows (matches RawRow in Grid.tsx)

CREATE TABLE IF NOT EXISTS authentication (
    id SERIAL PRIMARY KEY,
    username TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS submissions (
    id SERIAL PRIMARY KEY,
    author TEXT NOT NULL,
    title TEXT NOT NULL,
    genre TEXT NOT NULL,
    submit_date DATE,
    status TEXT NOT NULL,
    emailed BOOLEAN NOT NULL DEFAULT false
);

-- add admin user if not exists
INSERT INTO authentication (username, password_hash) VALUES
('admin', '$2a$10$dDLcI.YnwA.n43OB3783ReeQ84KJv2w0iSfc3iXslm2eT/rBy81OK') -- password is 'adminpass'
ON CONFLICT DO NOTHING;

-- seed example rows
INSERT INTO submissions (author, title, genre, submit_date, status, emailed) VALUES
('Test Author 1', 'Test Work 1', 'FICTION', '2025-10-22', 'UNASSIGNED', true),
('Test Author 2', 'Test Work 2', 'POETRY', '2025-10-12', 'FINALIST', false),
('Test Author 3', 'Test Work 3', 'POETRY', '2025-10-14', 'REJECTED', false)
ON CONFLICT DO NOTHING;