--liquibase formatted sql

--changeset codex:20260530-0003-create-offices
--comment: Add state/federal office catalog for branch, jurisdiction, level, and seat-based comparisons.
CREATE TABLE IF NOT EXISTS offices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    branch VARCHAR(50) NOT NULL CHECK (branch IN ('EXECUTIVE', 'LEGISLATIVE', 'JUDICIAL')),
    office_level VARCHAR(50) NOT NULL CHECK (office_level IN ('STATE', 'FEDERAL')),
    jurisdiction VARCHAR(100) NOT NULL,
    state VARCHAR(50),
    district VARCHAR(100),
    seat_identifier VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_offices_seat_identifier UNIQUE (seat_identifier)
);
--rollback DROP TABLE IF EXISTS offices CASCADE;

--changeset codex:20260530-0004-create-politician-offices
--comment: Track office history separately from the legacy politician.office summary field.
CREATE TABLE IF NOT EXISTS politician_offices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    politician_id UUID NOT NULL REFERENCES politicians(id) ON DELETE CASCADE,
    office_id UUID NOT NULL REFERENCES offices(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE,
    is_current BOOLEAN NOT NULL DEFAULT false,
    source_url TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_politician_offices_dates CHECK (end_date IS NULL OR end_date >= start_date),
    CONSTRAINT uq_politician_offices_term UNIQUE (politician_id, office_id, start_date)
);
--rollback DROP TABLE IF EXISTS politician_offices CASCADE;

--changeset codex:20260530-0005-create-elections
--comment: Add election records so the app can compare candidates running for the same seat.
CREATE TABLE IF NOT EXISTS elections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    office_id UUID NOT NULL REFERENCES offices(id) ON DELETE CASCADE,
    election_date DATE NOT NULL,
    election_type VARCHAR(50) NOT NULL CHECK (election_type IN ('PRIMARY', 'GENERAL', 'SPECIAL', 'RUNOFF')),
    cycle_year INTEGER NOT NULL,
    jurisdiction VARCHAR(100) NOT NULL,
    source_url TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_elections_office_date_type UNIQUE (office_id, election_date, election_type)
);
--rollback DROP TABLE IF EXISTS elections CASCADE;

--changeset codex:20260530-0006-create-election-candidates
--comment: Join politicians to elections with ballot status and outcome fields.
CREATE TABLE IF NOT EXISTS election_candidates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    election_id UUID NOT NULL REFERENCES elections(id) ON DELETE CASCADE,
    politician_id UUID NOT NULL REFERENCES politicians(id) ON DELETE CASCADE,
    party VARCHAR(100),
    ballot_status VARCHAR(50) NOT NULL DEFAULT 'FILED' CHECK (ballot_status IN ('FILED', 'CERTIFIED', 'WITHDRAWN', 'DISQUALIFIED')),
    result_status VARCHAR(50) CHECK (result_status IN ('WON', 'LOST', 'RUNOFF', 'PENDING')),
    vote_total INTEGER,
    vote_percentage NUMERIC(5, 2),
    source_url TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_election_candidates UNIQUE (election_id, politician_id)
);
--rollback DROP TABLE IF EXISTS election_candidates CASCADE;

--changeset codex:20260530-0007-create-office-election-indexes
CREATE INDEX IF NOT EXISTS idx_offices_branch_level ON offices(branch, office_level);
CREATE INDEX IF NOT EXISTS idx_offices_state_district ON offices(state, district);
CREATE INDEX IF NOT EXISTS idx_politician_offices_politician_id ON politician_offices(politician_id);
CREATE INDEX IF NOT EXISTS idx_politician_offices_office_id ON politician_offices(office_id);
CREATE INDEX IF NOT EXISTS idx_politician_offices_current ON politician_offices(is_current);
CREATE INDEX IF NOT EXISTS idx_elections_office_id ON elections(office_id);
CREATE INDEX IF NOT EXISTS idx_elections_cycle_year ON elections(cycle_year);
CREATE INDEX IF NOT EXISTS idx_election_candidates_election_id ON election_candidates(election_id);
CREATE INDEX IF NOT EXISTS idx_election_candidates_politician_id ON election_candidates(politician_id);
--rollback DROP INDEX IF EXISTS idx_election_candidates_politician_id;
--rollback DROP INDEX IF EXISTS idx_election_candidates_election_id;
--rollback DROP INDEX IF EXISTS idx_elections_cycle_year;
--rollback DROP INDEX IF EXISTS idx_elections_office_id;
--rollback DROP INDEX IF EXISTS idx_politician_offices_current;
--rollback DROP INDEX IF EXISTS idx_politician_offices_office_id;
--rollback DROP INDEX IF EXISTS idx_politician_offices_politician_id;
--rollback DROP INDEX IF EXISTS idx_offices_state_district;
--rollback DROP INDEX IF EXISTS idx_offices_branch_level;
