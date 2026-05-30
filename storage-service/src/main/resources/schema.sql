-- Create Politicians Table
CREATE TABLE politicians (
    id UUID PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    party VARCHAR(100),
    state VARCHAR(50),
    office VARCHAR(100),
    biography TEXT,
    profile_image_url TEXT,
    start_date DATE,
    end_date DATE
);

CREATE TABLE bills (
                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       bill_number VARCHAR(50) UNIQUE NOT NULL,
                       title TEXT NOT NULL,
                       description TEXT,
                       introduced_by UUID REFERENCES politicians(id),
                       status VARCHAR(50) CHECK (status IN ('Pending', 'Passed', 'Failed', 'Vetoed')),
                       introduced_date DATE NOT NULL,
                       last_action_date DATE NULL,
                       bill_url TEXT,
                       created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE voting_records (
                                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                politician_id UUID REFERENCES politicians(id) ON DELETE CASCADE,
                                bill_id UUID REFERENCES bills(id),
                                vote_type VARCHAR(20) CHECK (vote_type IN ('YEA', 'NAY', 'ABSTAIN')),
                                vote_date DATE NOT NULL,
                                created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE media_files (
                             id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                             politician_id UUID REFERENCES politicians(id) ON DELETE SET NULL,
                             file_type VARCHAR(10) CHECK (file_type IN ('video', 'audio')),
                             storage_url TEXT NOT NULL,
                             transcript TEXT,
                             tags TEXT[],
                             uploaded_at TIMESTAMP DEFAULT NOW()
);

-- Create Content Items Table
CREATE TABLE content_items (
    id UUID PRIMARY KEY,
    title TEXT NOT NULL,
    content_type VARCHAR(50) NOT NULL, -- "tweet", "article", "speech", "video"
    text_body TEXT,
    media_url TEXT,
    published_at TIMESTAMP NOT NULL,
    content_hash VARCHAR(256) UNIQUE,
    source_url TEXT NOT NULL,
    politician_id UUID NOT NULL,
    FOREIGN KEY (politician_id) REFERENCES politicians (id) ON DELETE CASCADE
);

-- Add Provenance Metadata
CREATE TABLE provenance (
    id UUID PRIMARY KEY,
    content_item_id UUID NOT NULL,
    source_type VARCHAR(100), -- e.g., Twitter API, Scraper
    timestamp TIMESTAMP,
    FOREIGN KEY (content_item_id) REFERENCES content_items (id) ON DELETE CASCADE
);
