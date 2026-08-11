UPDATE task_item
SET title = '1. Stabilizovat jádro: health endpoint, chybové odpovědi, stav databáze'
WHERE title = '1. Stabilizovat jadro: health endpoint, chybove odpovedi, stav databaze';

UPDATE task_item
SET title = '2. Ověřit import reálné firmy z ARES podle IČO'
WHERE title = '2. Overit import realne firmy z ARES podle ICO';

UPDATE task_item
SET title = '3. Dodělat registr lidí a detail osoby s vazbami na firmy'
WHERE title = '3. Dodelat registr lidi a detail osoby s vazbami na firmy';

UPDATE task_item
SET title = '4. Rozšířit rychlé vyhledávání podle firmy, IČO, osoby a role'
WHERE title = '4. Rozsirit rychle vyhledavani podle firmy, ICO, osoby a role';

UPDATE task_item
SET title = '5. Posílit audit: filtry, typy událostí, archiv a tiskový výpis'
WHERE title = '5. Posilit audit: filtry, typy udalosti, archiv a tiskovy vypis';

UPDATE task_item
SET title = '6. Přidat historii importních běhů včetně chybných řádků'
WHERE title = '6. Pridat historii importnich behu vcetne chybovych radku';

UPDATE task_item
SET title = '7. Zpřehlednit dashboard: metriky firem, osob, vazeb a watchlistu'
WHERE title = '7. Zprehlednit dashboard: metriky firem, osob, vazeb a watchlistu';

UPDATE task_item SET segment = 'Lidé' WHERE segment = 'Lide';
UPDATE task_item SET segment = 'Vyhledávání' WHERE segment = 'Vyhledavani';

UPDATE change_event SET description = 'Profil firmy byl aktualizován'
WHERE description = 'Company profile updated';

UPDATE change_event SET description = 'Firma byla importována'
WHERE description = 'Company imported';

UPDATE change_event SET description = 'Profil firmy byl ručně upraven'
WHERE description = 'Company profile updated manually';

UPDATE change_event SET description = 'Firma byla přidána na watchlist'
WHERE description = 'Company added to watchlist';

UPDATE change_event SET description = 'Firma byla odebrána z watchlistu'
WHERE description = 'Company removed from watchlist';

UPDATE change_event
SET description = REPLACE(description, 'Company deleted from registry: ', 'Firma byla smazána z registru: ')
WHERE description LIKE 'Company deleted from registry: %';

UPDATE change_event
SET description = REPLACE(description, ' assigned as ', ' přiřazen jako ')
WHERE type = 'PERSON_ASSIGNED' AND description LIKE '% assigned as %';

UPDATE change_event
SET description = REPLACE(description, 'Person role updated to ', 'Role osoby byla změněna na ')
WHERE type = 'PERSON_ROLE_UPDATED' AND description LIKE 'Person role updated to %';

UPDATE change_event SET description = 'Osoba byla odebrána od firmy'
WHERE type = 'PERSON_REMOVED' AND description = 'Person removed from company';

UPDATE change_event
SET description = REPLACE(description, 'Person profile updated: ', 'Profil osoby byl upraven: ')
WHERE type = 'PERSON_UPDATED' AND description LIKE 'Person profile updated: %';

UPDATE change_event
SET description = REPLACE(description, ' updated to ', ' změněn na ')
WHERE type = 'PERSON_UPDATED' AND description LIKE '% updated to %';

UPDATE change_event
SET description = REPLACE(description, 'Person deleted from registry: ', 'Osoba byla smazána z registru: ')
WHERE type = 'PERSON_DELETED' AND description LIKE 'Person deleted from registry: %';

UPDATE change_event
SET description = REPLACE(description, 'removed relationships: ', 'odstraněné vazby: ')
WHERE type = 'PERSON_DELETED' AND description LIKE 'Osoba byla smazána z registru:%';

UPDATE change_event
SET description = REPLACE(description, ' deleted from person registry', ' smazán z registru osob')
WHERE type = 'PERSON_DELETED' AND description LIKE '% deleted from person registry';

UPDATE change_event
SET description = REPLACE(description, 'Import run #', 'Importní běh #')
WHERE description LIKE 'Import run #%';

UPDATE change_event
SET description = REPLACE(description, ' finished with status ', ' dokončen se stavem ')
WHERE description LIKE 'Importní běh #% finished with status %';

UPDATE change_event
SET description = REPLACE(description, ': imported ', ': importováno ')
WHERE description LIKE 'Importní běh #%: imported %';

UPDATE change_event
SET description = REPLACE(description, ', failed ', ', chybně ')
WHERE description LIKE 'Importní běh #%';

UPDATE change_event
SET description = REPLACE(description, ', total ', ', celkem ')
WHERE description LIKE 'Importní běh #%';
