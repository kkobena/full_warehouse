-- Dashboard Layouts Table
CREATE TABLE IF NOT EXISTS dashboard_layout (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    user_id BIGINT REFERENCES app_user(id) ON DELETE CASCADE,
    scope VARCHAR(50) NOT NULL DEFAULT 'PRIVATE',
    is_default BOOLEAN DEFAULT FALSE,
    layout_config JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_dashboard_layout_user_id ON dashboard_layout(user_id);
CREATE INDEX idx_dashboard_layout_scope ON dashboard_layout(scope);
CREATE INDEX idx_dashboard_layout_is_default ON dashboard_layout(is_default);
CREATE INDEX idx_dashboard_layout_updated_at ON dashboard_layout(updated_at DESC);

-- GIN index on JSONB for fast querying of layout configuration
CREATE INDEX idx_dashboard_layout_config ON dashboard_layout USING GIN (layout_config);

-- Ensure only one default per user
CREATE UNIQUE INDEX idx_dashboard_layout_user_default ON dashboard_layout(user_id) WHERE is_default = TRUE;

COMMENT ON TABLE dashboard_layout IS 'Customizable dashboard layouts for users';
COMMENT ON COLUMN dashboard_layout.scope IS 'PRIVATE, SHARED, or PUBLIC';
COMMENT ON COLUMN dashboard_layout.layout_config IS 'JSONB configuration for GridStack layout and widgets. Allows native PostgreSQL JSON queries.';
