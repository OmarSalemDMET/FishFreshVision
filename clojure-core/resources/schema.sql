CREATE TABLE IF NOT EXISTS jobs (
    id UUID PRIMARY KEY,
    image_path TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'pending', -- pending | done | error
    result JSONB, -- Store the full {cnn, agent, analysis} map
    error_message TEXT,
    created_at TIMESTAMPTZ DEFAULT now(),
    completed_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS model_metadata (
    id SERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    version TEXT NOT NULL,
    input_shape TEXT,
    output_classes TEXT,
    resource_path TEXT,
    created_at TIMESTAMPTZ DEFAULT now()
);
