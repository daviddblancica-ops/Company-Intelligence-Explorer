# Company Intelligence Explorer

Company Intelligence Explorer je Java backend pro praci s firemnimi daty. Projekt ukazuje male, ale smysluplne MVP aplikace, ktera umi prijmout data o firmach, sjednotit nazvy, ulozit vazby na osoby, uchovat historii zmen a nabidnout rychle vyhledavani pres REST API.

## Co projekt resi

Firemni data byvaji casto roztristena, nejednotna a spatne dohledatelna. Tato aplikace je prevadi do jednotneho modelu, aby bylo mozne najit firmu podle nazvu nebo identifikace a zobrazit jeji detail vcetne propojenych osob a historie zmen.

## Hlavni casti

- import firemnich dat z JSON nebo CSV
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

## Jak projekt popsat u obhajoby

Jde o navrzene MVP backendoveho systemu pro praci s firemnimi daty. Smyslem je ukazat cisty navrh Java aplikace pro datove orientovany use-case: import dat, sjednoceni nazvu, vazby mezi firmami a osobami, historii zmen a API rozhrani pro rychle hledani.
