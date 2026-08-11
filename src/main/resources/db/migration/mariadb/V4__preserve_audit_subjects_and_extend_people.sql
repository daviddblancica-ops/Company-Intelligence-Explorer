ALTER TABLE change_event
    ADD COLUMN IF NOT EXISTS company_name VARCHAR(255) NULL AFTER company_id;

ALTER TABLE change_event
    ADD COLUMN IF NOT EXISTS registration_number VARCHAR(255) NULL AFTER company_name;

UPDATE change_event event
JOIN company company ON company.id = event.company_id
SET event.company_name = company.name,
    event.registration_number = company.registration_number
WHERE event.company_name IS NULL OR event.registration_number IS NULL;

CREATE INDEX IF NOT EXISTS idx_change_subject
    ON change_event (registration_number, company_name);

ALTER TABLE person
    ADD COLUMN IF NOT EXISTS date_of_birth DATE NULL AFTER normalized_name;

ALTER TABLE person
    ADD COLUMN IF NOT EXISTS residence_address VARCHAR(600) NULL AFTER date_of_birth;

ALTER TABLE person
    ADD COLUMN IF NOT EXISTS note VARCHAR(1200) NULL AFTER residence_address;

DELETE FROM change_event
WHERE registration_number IN ('70010001', '70010002', '70010003');

DELETE FROM company_person_role
WHERE company_id IN (
    SELECT id FROM company WHERE registration_number IN ('70010001', '70010002', '70010003')
);

DELETE FROM company
WHERE registration_number IN ('70010001', '70010002', '70010003');

DELETE person
FROM person
LEFT JOIN company_person_role relationship ON relationship.person_id = person.id
WHERE relationship.id IS NULL
  AND person.normalized_name IN ('michaela cerna', 'karel novak', 'lucie hruba', 'pavel urban');

UPDATE task_item
SET title = '2. Overit import realne firmy z ARES podle ICO'
WHERE title = '2. Pridat startup demo data pro firmy, osoby, vazby a audit';
