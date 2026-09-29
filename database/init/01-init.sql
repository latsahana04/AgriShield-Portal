-- =========================================================================
-- AGRISHIELD PORTAL POSTGRESQL 15 INITIALIZATION SCHEMA
-- Creates: users, vet_shifts, animals, medications, treatments, audit_logs
-- =========================================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. USERS & RBAC TABLE
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(36) PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(30) NOT NULL CHECK (role IN ('FARMHAND', 'VETERINARIAN', 'MANAGER', 'AUDITOR')),
    license_id VARCHAR(50),
    org_name VARCHAR(100) NOT NULL,
    x509_cert_fingerprint VARCHAR(128),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. VETERINARY DUTY SHIFTS TABLE
CREATE TABLE IF NOT EXISTS vet_shifts (
    id VARCHAR(36) PRIMARY KEY,
    shift_id VARCHAR(50) UNIQUE NOT NULL,
    vet_id VARCHAR(50) NOT NULL,
    vet_name VARCHAR(100) NOT NULL,
    shift_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    barn_assigned VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 3. MEDICATIONS & MRL MASTER TABLE
CREATE TABLE IF NOT EXISTS medications (
    id VARCHAR(36) PRIMARY KEY,
    drug_id VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,
    withdrawal_hours INT NOT NULL,
    mrl_limit VARCHAR(100) NOT NULL,
    standard_dose_mg_kg NUMERIC(8, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 4. LIVESTOCK ANIMALS TABLE
CREATE TABLE IF NOT EXISTS animals (
    id VARCHAR(36) PRIMARY KEY,
    tag_id VARCHAR(50) UNIQUE NOT NULL,
    species VARCHAR(50) NOT NULL,
    breed VARCHAR(50) NOT NULL,
    barn VARCHAR(50) NOT NULL,
    pen VARCHAR(50) NOT NULL,
    weight_kg NUMERIC(8, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SAFE' CHECK (status IN ('SAFE', 'LOCKED')),
    last_drug_id VARCHAR(50),
    administered_at TIMESTAMP WITH TIME ZONE,
    safe_release_time TIMESTAMP WITH TIME ZONE,
    withdrawal_hours INT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Index for sub-10ms RFID scan lookups
CREATE INDEX IF NOT EXISTS idx_animals_tag_id ON animals(tag_id);
CREATE INDEX IF NOT EXISTS idx_animals_status ON animals(status);

-- 5. TREATMENTS & PRESCRIPTIONS TABLE
CREATE TABLE IF NOT EXISTS treatments (
    id VARCHAR(36) PRIMARY KEY,
    tx_id VARCHAR(64) UNIQUE,
    composite_key VARCHAR(64) UNIQUE NOT NULL,
    animal_tag_id VARCHAR(50) NOT NULL REFERENCES animals(tag_id),
    medication_id VARCHAR(50) NOT NULL REFERENCES medications(drug_id),
    dosage VARCHAR(50) NOT NULL,
    dosage_mg NUMERIC(10, 2) NOT NULL,
    route VARCHAR(50) NOT NULL,
    administered_at TIMESTAMP WITH TIME ZONE NOT NULL,
    safe_release_time TIMESTAMP WITH TIME ZONE NOT NULL,
    vet_license_id VARCHAR(50) NOT NULL,
    vet_signature TEXT NOT NULL,
    fabric_block_number BIGINT,
    fabric_tx_hash VARCHAR(128),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Index for deduplication lookups
CREATE INDEX IF NOT EXISTS idx_treatments_composite_key ON treatments(composite_key);
CREATE INDEX IF NOT EXISTS idx_treatments_animal_tag ON treatments(animal_tag_id);

-- 6. AUDIT LOGS TABLE
CREATE TABLE IF NOT EXISTS audit_logs (
    id VARCHAR(36) PRIMARY KEY,
    action VARCHAR(50) NOT NULL,
    details TEXT NOT NULL,
    actor_username VARCHAR(50) NOT NULL,
    actor_role VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    ip_address VARCHAR(50),
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 7. SHIPMENT PERMITS TABLE
CREATE TABLE IF NOT EXISTS shipment_permits (
    id VARCHAR(36) PRIMARY KEY,
    permit_id VARCHAR(50) UNIQUE NOT NULL,
    batch_size INT NOT NULL,
    animal_tags JSONB NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('APPROVED', 'DENIED')),
    denial_reason TEXT,
    issued_by VARCHAR(50) NOT NULL,
    issued_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    fabric_tx_hash VARCHAR(128)
);

-- =========================================================================
-- SEED INITIAL DATA
-- =========================================================================

-- Seed Users (Passwords hashed using BCrypt for 'password123')
-- $2a$10$wN1iN6aZ.b9mJpZ8Fw0D8e9p7gR7V8k5N9u4X2e8k7o0w6Q2l1v7e -> password123
INSERT INTO users (id, username, password_hash, full_name, role, license_id, org_name, x509_cert_fingerprint)
VALUES
('usr-1', 'farmhand_jake', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhiM.yV6gL8k.dIqXvP5tI6P0tQ5X3m2', 'Jake Miller', 'FARMHAND', 'FARM-EMP-91', 'Org1MSP (Farm Operations)', NULL),
('usr-2', 'vet_sarah', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhiM.yV6gL8k.dIqXvP5tI6P0tQ5X3m2', 'Dr. Sarah Lin, DVM', 'VETERINARIAN', 'VET-USA-4402', 'Org2MSP (Vet Association)', 'SHA256:7B:3A:90:E4:11:5F:82:1C:2B:64:89:12:F1:C9:4A:88'),
('usr-3', 'manager_john', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhiM.yV6gL8k.dIqXvP5tI6P0tQ5X3m2', 'John Reynolds', 'MANAGER', 'MGR-HQ-01', 'Org1MSP (Farm Management)', 'SHA256:C4:22:98:AA:FE:09:41:00:88:51:D3:99:BC:11:EA:02'),
('usr-4', 'auditor_elena', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhiM.yV6gL8k.dIqXvP5tI6P0tQ5X3m2', 'Elena Rostova', 'AUDITOR', 'GOV-AUDIT-55', 'GovernmentAuditMSP', 'SHA256:EE:99:11:44:22:00:BB:CC:DD:AA:33:44:55:66:77:88')
ON CONFLICT (id) DO NOTHING;

-- Seed Medications Master
INSERT INTO medications (id, drug_id, name, category, withdrawal_hours, mrl_limit, standard_dose_mg_kg)
VALUES
('med-1', 'DRUG-01', 'Penicillin G Procaine', 'Beta-lactams', 72, '4 ug/kg (milk) / 50 ug/kg (muscle)', 10.0),
('med-2', 'DRUG-02', 'Oxytetracycline 200', 'Tetracyclines', 120, '100 ug/kg (muscle)', 20.0),
('med-3', 'DRUG-03', 'Ceftiofur Sodium', 'Cephalosporins', 48, '1000 ug/kg (muscle)', 2.2),
('med-4', 'DRUG-04', 'Tilmicosin', 'Macrolides', 672, '50 ug/kg (muscle)', 10.0),
('med-5', 'DRUG-05', 'Enrofloxacin', 'Fluoroquinolones', 192, '100 ug/kg (muscle)', 7.5)
ON CONFLICT (id) DO NOTHING;

-- Seed Scheduled Vet Shifts
INSERT INTO vet_shifts (id, shift_id, vet_id, vet_name, shift_date, start_time, end_time, barn_assigned)
VALUES
('sh-1', 'SH-101', 'VET-USA-4402', 'Dr. Sarah Lin', CURRENT_DATE, '08:00:00', '18:00:00', 'Barn 1, 2, 3'),
('sh-2', 'sh-102', 'VET-USA-1187', 'Dr. Marcus Vance', CURRENT_DATE, '18:00:00', '23:59:00', 'Barn 4'),
('sh-3', 'sh-103', 'VET-USA-4402', 'Dr. Sarah Lin', CURRENT_DATE + INTERVAL '1 day', '08:00:00', '17:00:00', 'All Sectors')
ON CONFLICT (id) DO NOTHING;

-- Seed Initial Animals
INSERT INTO animals (id, tag_id, species, breed, barn, pen, weight_kg, status, last_drug_id, administered_at, safe_release_time, withdrawal_hours)
VALUES
('an-1', 'COW-804', 'Bovine', 'Holstein Dairy', 'Barn 1', 'Pen A-12', 620.0, 'LOCKED', 'DRUG-01', NOW() - INTERVAL '70 hours', NOW() + INTERVAL '2 hours', 72),
('an-2', 'COW-805', 'Bovine', 'Holstein Dairy', 'Barn 1', 'Pen A-14', 590.0, 'SAFE', 'DRUG-03', NOW() - INTERVAL '60 hours', NOW() - INTERVAL '12 hours', 48),
('an-3', 'COW-310', 'Bovine', 'Angus Beef', 'Barn 3', 'Pen C-01', 680.0, 'LOCKED', 'DRUG-03', NOW() - INTERVAL '10 hours', NOW() + INTERVAL '38 hours', 48),
('an-4', 'COW-311', 'Bovine', 'Angus Beef', 'Barn 3', 'Pen C-02', 710.0, 'SAFE', NULL, NULL, NULL, 0),
('an-5', 'COW-312', 'Bovine', 'Angus Beef', 'Barn 3', 'Pen C-02', 695.0, 'SAFE', NULL, NULL, NULL, 0),
('an-6', 'PIG-102', 'Swine', 'Yorkshire', 'Barn 2', 'Pen B-03', 110.0, 'SAFE', 'DRUG-02', NOW() - INTERVAL '130 hours', NOW() - INTERVAL '10 hours', 120),
('an-7', 'PIG-045', 'Swine', 'Landrace', 'Barn 2', 'Pen B-05', 115.0, 'LOCKED', 'DRUG-01', NOW() - INTERVAL '2 hours', NOW() + INTERVAL '70 hours', 72),
('an-8', 'HEN-221', 'Poultry', 'Leghorn', 'Barn 4', 'Flock F-7', 2.1, 'SAFE', 'DRUG-02', NOW() - INTERVAL '200 hours', NOW() - INTERVAL '80 hours', 120),
('an-9', 'HEN-222', 'Poultry', 'Leghorn', 'Barn 4', 'Flock F-7', 2.3, 'SAFE', NULL, NULL, NULL, 0)
ON CONFLICT (id) DO NOTHING;
