# Company Intelligence Explorer

Company Intelligence Explorer je Java backend pro praci s firemnimi daty. Projekt ukazuje male, ale smysluplne MVP aplikace, ktera umi prijmout data o firmach, sjednotit nazvy, ulozit vazby na osoby, uchovat historii zmen a nabidnout rychle vyhledavani pres REST API.

## Co projekt resi

Firemni data byvaji casto roztristena, nejednotna a spatne dohledatelna. Tato aplikace je prevadi do jednotneho modelu, aby bylo mozne najit firmu podle nazvu nebo identifikace a zobrazit jeji detail vcetne propojenych osob a historie zmen.

## Hlavni casti

- import firemnich dat z JSON nebo CSV
- import firemnich dat z ARES podle ICO
- sjednoceni nazvu pro spolehlive vyhledavani
- ulozeni firem, osob a jejich roli
- historie vytvoreni a aktualizaci firmy
- REST API pro zalozeni, detail a hledani firmy
- H2 databaze pro jednoduche lokalni spusteni

## Spusteni

Projekt je pripraveny pro Windows a nevyzaduje globalne nainstalovany Maven. Maven se pri prvnim spusteni stahne do slozky `.mvn-local`.

```bat
mvnw.cmd spring-boot:run
```

Aplikace bezi na:

```text
http://localhost:8080
```

H2 konzole:

```text
http://localhost:8080/h2-console
```

JDBC URL:

```text
jdbc:h2:mem:companyintel
```

## Testy

```bat
mvnw.cmd test
```

## Produkcni profil a hosting

Pro nasazeni s externi MariaDB/MySQL databazi je pripraveny profil `prod`.
Postup pro Webglobe, SFTP/SCP a databazove promenne je v:

```text
docs/webglobe-deploy.md
```

## API priklady

Vytvoreni nebo aktualizace firmy:

```http
POST /api/companies
Content-Type: application/json
```

```json
{
  "name": "Nova Data Systems s.r.o.",
  "registrationNumber": "12345678",
  "country": "CZ",
  "legalForm": "s.r.o.",
  "people": [
    {
      "fullName": "Jan Novak",
      "role": "jednatel"
    }
  ]
}
```

Vyhledani firmy:

```http
GET /api/companies/search?q=nova
```

Detail firmy:

```http
GET /api/companies/1
```

Import JSON pole:

```http
POST /api/import/json
Content-Type: application/json
```

Import CSV:

```http
POST /api/import/csv
Content-Type: text/csv
```

CSV format:

```csv
name,registrationNumber,country,legalForm,people
Nova Data Systems s.r.o.,12345678,CZ,s.r.o.,Jan Novak|jednatel;Eva Svobodova|spolecnik
```

Import z ARES podle ICO:

```http
POST /api/import/ares/00006947
```

Filtrovani audit logu podle typu, zavaznosti, firmy, importu a obdobi:

```http
GET /api/audit?type=IMPORT_PARTIAL&severity=WARNING&query=Atlas&importRunId=12&from=2026-01-01&to=2026-12-31
```

CSV export pouziva stejne filtry:

```http
GET /api/audit/export.csv?severity=WARNING&from=2026-01-01
```

Hromadna archivace nebo obnoveni udalosti:

```http
POST /api/audit/archive
Content-Type: application/json
```

```json
{
  "ids": [15, 16],
  "archived": true
}
```

## Jak projekt popsat u obhajoby

Jde o navrzene MVP backendoveho systemu pro praci s firemnimi daty. Smyslem je ukazat cisty navrh Java aplikace pro datove orientovany use-case: import dat, sjednoceni nazvu, vazby mezi firmami a osobami, historii zmen a API rozhrani pro rychle hledani.

## Jednoducha analogie

Kdybys mel velkou skrin plnou sanonu o firmach, tenhle projekt dela ctyri veci:

1. vezme rozhazene papiry a spravne je zaradi
2. nadepise je jednotne, aby se daly najit
3. doplni karticku s historii zmen
4. prida rychly rejstrik, kde jde vse hledat podle jmena nebo identifikace

## Jak to vysvetlit bez technickeho zargonu

Misto tohohle:

- Elasticsearch
- entity normalization
- history events
- REST API
- monolithic backend

Rikej radeji:

- chytry vyhledavac
- sjednoceni dat
- historie zmen
- rozhrani pro komunikaci se systemem
- jedna cista aplikace misto rozdeleneho sloziteho reseni
