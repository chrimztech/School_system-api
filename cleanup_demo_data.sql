-- ============================================================================
-- SRMS Database Cleanup Script - Remove Demo/Seed Data
-- ============================================================================
-- This script safely removes all demo data created by the DataSeeder classes.
-- 
-- IMPORTANT: 
-- 1. Backup your database FIRST before running this!
--    pg_dump -h localhost -U postgres -Fc -f srms_backup.dump school_system
-- 
-- 2. Adjust the WHERE clauses if you want to keep specific demo data.
-- 
-- 3. Run with caution - these are destructive operations.
-- ============================================================================

-- Remove demo users created in DataSeeder
DELETE FROM app_users 
WHERE email IN (
  'admin@srms.zm',
  'head@lta.zm', 'j.phiri@lta.zm', 'finance@lta.zm', 'p.kasonde@gmail.com',
  'head@kps.zm',
  'head@ngs.zm'
);

-- Remove demo schools (keep only if you want to preserve real schools)
DELETE FROM schools 
WHERE short_code IN ('LTA', 'KPS', 'NGS');

-- Optional: Remove all supplemental data for demo schools
-- Warning: Only run if you want a complete wipe; disable by commenting out

-- Find and delete all child records for demo schools
-- Using a CTE to capture school IDs before deletion
WITH demo_schools AS (
  SELECT id FROM schools 
  WHERE short_code IN ('LTA', 'KPS', 'NGS')
)
DELETE FROM exam_papers WHERE school_id IN (SELECT id FROM demo_schools);

WITH demo_schools AS (
  SELECT id FROM schools 
  WHERE short_code IN ('LTA', 'KPS', 'NGS')
)
DELETE FROM welfare_cases WHERE school_id IN (SELECT id FROM demo_schools);

WITH demo_schools AS (
  SELECT id FROM schools 
  WHERE short_code IN ('LTA', 'KPS', 'NGS')
)
DELETE FROM counseling_sessions WHERE school_id IN (SELECT id FROM demo_schools);

WITH demo_schools AS (
  SELECT id FROM schools 
  WHERE short_code IN ('LTA', 'KPS', 'NGS')
)
DELETE FROM procurement_requests WHERE school_id IN (SELECT id FROM demo_schools);

WITH demo_schools AS (
  SELECT id FROM schools 
  WHERE short_code IN ('LTA', 'KPS', 'NGS')
)
DELETE FROM work_orders WHERE school_id IN (SELECT id FROM demo_schools);

WITH demo_schools AS (
  SELECT id FROM schools 
  WHERE short_code IN ('LTA', 'KPS', 'NGS')
)
DELETE FROM bursary_awards WHERE school_id IN (SELECT id FROM demo_schools);

WITH demo_schools AS (
  SELECT id FROM schools 
  WHERE short_code IN ('LTA', 'KPS', 'NGS')
)
DELETE FROM calendar_events WHERE school_id IN (SELECT id FROM demo_schools);

WITH demo_schools AS (
  SELECT id FROM schools 
  WHERE short_code IN ('LTA', 'KPS', 'NGS')
)
DELETE FROM duty_assignments WHERE school_id IN (SELECT id FROM demo_schools);

WITH demo_schools AS (
  SELECT id FROM schools 
  WHERE short_code IN ('LTA', 'KPS', 'NGS')
)
DELETE FROM training_records WHERE school_id IN (SELECT id FROM demo_schools);

WITH demo_schools AS (
  SELECT id FROM schools 
  WHERE short_code IN ('LTA', 'KPS', 'NGS')
)
DELETE FROM vendors WHERE school_id IN (SELECT id FROM demo_schools);

WITH demo_schools AS (
  SELECT id FROM schools 
  WHERE short_code IN ('LTA', 'KPS', 'NGS')
)
DELETE FROM compliance_items WHERE school_id IN (SELECT id FROM demo_schools);

WITH demo_schools AS (
  SELECT id FROM schools 
  WHERE short_code IN ('LTA', 'KPS', 'NGS')
)
DELETE FROM risk_entries WHERE school_id IN (SELECT id FROM demo_schools);

WITH demo_schools AS (
  SELECT id FROM schools 
  WHERE short_code IN ('LTA', 'KPS', 'NGS')
)
DELETE FROM incidents WHERE school_id IN (SELECT id FROM demo_schools);

WITH demo_schools AS (
  SELECT id FROM schools 
  WHERE short_code IN ('LTA', 'KPS', 'NGS')
)
DELETE FROM timetable_slots WHERE school_id IN (SELECT id FROM demo_schools);

WITH demo_schools AS (
  SELECT id FROM schools 
  WHERE short_code IN ('LTA', 'KPS', 'NGS')
)
DELETE FROM journal_entries WHERE school_id IN (SELECT id FROM demo_schools);

WITH demo_schools AS (
  SELECT id FROM schools 
  WHERE short_code IN ('LTA', 'KPS', 'NGS')
)
DELETE FROM expenses WHERE school_id IN (SELECT id FROM demo_schools);

WITH demo_schools AS (
  SELECT id FROM schools 
  WHERE short_code IN ('LTA', 'KPS', 'NGS')
)
DELETE FROM students WHERE school_id IN (SELECT id FROM demo_schools);

WITH demo_schools AS (
  SELECT id FROM schools 
  WHERE short_code IN ('LTA', 'KPS', 'NGS')
)
DELETE FROM teachers WHERE school_id IN (SELECT id FROM demo_schools);

WITH demo_schools AS (
  SELECT id FROM schools 
  WHERE short_code IN ('LTA', 'KPS', 'NGS')
)
DELETE FROM school_classes WHERE school_id IN (SELECT id FROM demo_schools);

WITH demo_schools AS (
  SELECT id FROM schools 
  WHERE short_code IN ('LTA', 'KPS', 'NGS')
)
DELETE FROM subjects WHERE school_id IN (SELECT id FROM demo_schools);

WITH demo_schools AS (
  SELECT id FROM schools 
  WHERE short_code IN ('LTA', 'KPS', 'NGS')
)
DELETE FROM staff_records WHERE school_id IN (SELECT id FROM demo_schools);

WITH demo_schools AS (
  SELECT id FROM schools 
  WHERE short_code IN ('LTA', 'KPS', 'NGS')
)
DELETE FROM leave_requests WHERE school_id IN (SELECT id FROM demo_schools);

WITH demo_schools AS (
  SELECT id FROM schools 
  WHERE short_code IN ('LTA', 'KPS', 'NGS')
)
DELETE FROM inventory_items WHERE school_id IN (SELECT id FROM demo_schools);

WITH demo_schools AS (
  SELECT id FROM schools 
  WHERE short_code IN ('LTA', 'KPS', 'NGS')
)
DELETE FROM menu_items WHERE school_id IN (SELECT id FROM demo_schools);

WITH demo_schools AS (
  SELECT id FROM schools 
  WHERE short_code IN ('LTA', 'KPS', 'NGS')
)
DELETE FROM canteen_orders WHERE school_id IN (SELECT id FROM demo_schools);

WITH demo_schools AS (
  SELECT id FROM schools 
  WHERE short_code IN ('LTA', 'KPS', 'NGS')
)
DELETE FROM hostel_rooms WHERE school_id IN (SELECT id FROM demo_schools);

WITH demo_schools AS (
  SELECT id FROM schools 
  WHERE short_code IN ('LTA', 'KPS', 'NGS')
)
DELETE FROM hostel_allocations WHERE school_id IN (SELECT id FROM demo_schools);

WITH demo_schools AS (
  SELECT id FROM schools 
  WHERE short_code IN ('LTA', 'KPS', 'NGS')
)
DELETE FROM health_visits WHERE school_id IN (SELECT id FROM demo_schools);

WITH demo_schools AS (
  SELECT id FROM schools 
  WHERE short_code IN ('LTA', 'KPS', 'NGS')
)
DELETE FROM lost_found_items WHERE school_id IN (SELECT id FROM demo_schools);

WITH demo_schools AS (
  SELECT id FROM schools 
  WHERE short_code IN ('LTA', 'KPS', 'NGS')
)
DELETE FROM activities WHERE school_id IN (SELECT id FROM demo_schools);

WITH demo_schools AS (
  SELECT id FROM schools 
  WHERE short_code IN ('LTA', 'KPS', 'NGS')
)
DELETE FROM alumni_records WHERE school_id IN (SELECT id FROM demo_schools);

WITH demo_schools AS (
  SELECT id FROM schools 
  WHERE short_code IN ('LTA', 'KPS', 'NGS')
)
DELETE FROM vehicles WHERE school_id IN (SELECT id FROM demo_schools);

WITH demo_schools AS (
  SELECT id FROM schools 
  WHERE short_code IN ('LTA', 'KPS', 'NGS')
)
DELETE FROM transport_routes WHERE school_id IN (SELECT id FROM demo_schools);

WITH demo_schools AS (
  SELECT id FROM schools 
  WHERE short_code IN ('LTA', 'KPS', 'NGS')
)
DELETE FROM library_books WHERE school_id IN (SELECT id FROM demo_schools);

WITH demo_schools AS (
  SELECT id FROM schools 
  WHERE short_code IN ('LTA', 'KPS', 'NGS')
)
DELETE FROM admission_applications WHERE school_id IN (SELECT id FROM demo_schools);

WITH demo_schools AS (
  SELECT id FROM schools 
  WHERE short_code IN ('LTA', 'KPS', 'NGS')
)
DELETE FROM fee_structures WHERE school_id IN (SELECT id FROM demo_schools);

WITH demo_schools AS (
  SELECT id FROM schools 
  WHERE short_code IN ('LTA', 'KPS', 'NGS')
)
DELETE FROM payroll_runs WHERE school_id IN (SELECT id FROM demo_schools);

WITH demo_schools AS (
  SELECT id FROM schools 
  WHERE short_code IN ('LTA', 'KPS', 'NGS')
)
DELETE FROM announcements WHERE school_id IN (SELECT id FROM demo_schools);

WITH demo_schools AS (
  SELECT id FROM schools 
  WHERE short_code IN ('LTA', 'KPS', 'NGS')
)
DELETE FROM discipline_cases WHERE school_id IN (SELECT id FROM demo_schools);

WITH demo_schools AS (
  SELECT id FROM schools 
  WHERE short_code IN ('LTA', 'KPS', 'NGS')
)
DELETE FROM visitor_logs WHERE school_id IN (SELECT id FROM demo_schools);

-- ============================================================================
-- Cleanup Summary
-- ============================================================================
-- After running this script:
-- ✓ Demo users removed (admin@srms.zm, demo schools' staff)
-- ✓ Demo schools deleted (LTA, KPS, NGS)
-- ✓ All associated records purged
--
-- Next steps:
-- 1. Create your first real school via the API or UI
-- 2. Create real user accounts for admins and staff
-- 3. Test the login flow with real credentials
-- ============================================================================
