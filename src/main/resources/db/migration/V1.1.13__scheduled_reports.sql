-- Scheduled Reports - Automatic Report Generation and Email Delivery
-- Description: Table for configuring automatic report generation on schedule

CREATE TABLE scheduled_report (
    id BIGSERIAL PRIMARY KEY,
    report_name VARCHAR(255) NOT NULL,
    report_type VARCHAR(50) NOT NULL,
    frequency VARCHAR(20) NOT NULL,
    execution_time TIME,
    day_of_week INTEGER,
    day_of_month INTEGER,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    email_recipients TEXT,
    include_pdf BOOLEAN NOT NULL DEFAULT TRUE,
    include_excel BOOLEAN NOT NULL DEFAULT FALSE,
    last_execution TIMESTAMP,
    next_execution TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by_id BIGINT REFERENCES app_user(id),
    filter_params TEXT
);

-- Indexes
CREATE INDEX idx_scheduled_report_active ON scheduled_report(active);
CREATE INDEX idx_scheduled_report_next_execution ON scheduled_report(next_execution) WHERE active = TRUE;
CREATE INDEX idx_scheduled_report_type ON scheduled_report(report_type);
CREATE INDEX idx_scheduled_report_frequency ON scheduled_report(frequency);



-- Insert default scheduled reports (examples)
INSERT INTO scheduled_report (
    report_name,
    report_type,
    frequency,
    execution_time,
    day_of_week,
    email_recipients,
    active,
    next_execution
) VALUES
(
    'Rapport Quotidien - Alertes Stock',
    'STOCK_ALERTS',
    'DAILY',
    '08:00:00',
    NULL,
    'manager@pharmacy.com',
    FALSE, -- Disabled by default
    CURRENT_TIMESTAMP + INTERVAL '1 day'
),
(
    'Rapport Hebdomadaire - CA',
    'DASHBOARD_CA',
    'WEEKLY',
    '09:00:00',
    1, -- Monday
    'manager@pharmacy.com,accountant@pharmacy.com',
    FALSE,
    CURRENT_TIMESTAMP + INTERVAL '1 week'
),
(
    'Rapport Mensuel - Créances Tiers-Payants',
    'TIERS_PAYANT_CREANCES',
    'MONTHLY',
    '10:00:00',
    NULL,
    'manager@pharmacy.com,finance@pharmacy.com',
    FALSE,
    CURRENT_TIMESTAMP + INTERVAL '1 month'
);

-- Comments
COMMENT ON TABLE scheduled_report IS 'Configuration for automatic scheduled report generation and email delivery';
COMMENT ON COLUMN scheduled_report.report_type IS 'Type of report: DASHBOARD_CA, STOCK_ALERTS, TIERS_PAYANT_CREANCES, etc.';
COMMENT ON COLUMN scheduled_report.frequency IS 'Execution frequency: DAILY, WEEKLY, MONTHLY';
COMMENT ON COLUMN scheduled_report.execution_time IS 'Time of day to execute the report';
COMMENT ON COLUMN scheduled_report.day_of_week IS 'Day of week for WEEKLY frequency (1=Monday, 7=Sunday)';
COMMENT ON COLUMN scheduled_report.day_of_month IS 'Day of month for MONTHLY frequency (1-31)';
COMMENT ON COLUMN scheduled_report.filter_params IS 'JSON string containing report-specific filter parameters';
