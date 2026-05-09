# Company Intelligence Explorer - znalostni zakladna T=0

Tento dokument popisuje stav projektu v case T=0, tedy aktualni implementovanou funkcionalitu pred dalsim rozsirovanim. Slouzi jako technicka mapa projektu, podklad pro prezentaci a kontrolni seznam pro dalsi iterace.

## 1. Strucna definice projektu

Company Intelligence Explorer je Java/Spring Boot backendova aplikace pro praci s firemnimi daty. Aplikace umi prijimat data z vice zdroju, sjednocovat firemni nazvy, ukladat firmy, osoby a jejich role, uchovavat historii zmen a poskytovat rychle vyhledavani pres REST API i jednoduche webove rozhrani.

Projekt neni jen seznam firem. Je to male MVP systemu, ktery ukazuje kompletni datovy tok:

1. vstup dat,
2. kontrola a sjednoceni,
3. ulozeni do databaze,
4. propojeni firem s lidmi,
5. zapis historie,
6. vyhledani a zobrazeni vysledku.

## 2. Aktualne implementovane moduly

### Dashboard

Dashboard je uvodni pohled aplikace. Ukazuje stav systemu, zakladni metriky a rychle odkazy do hlavnich casti aplikace.

Implementovano:

- stav API a databaze,
- pocet firem,
- pocet osob,
- pocet vazeb mezi firmami a osobami,
- pocet firem na watchlistu,
- pocet auditnich udalosti,
- pocet importnich behu,
- mini nahled audit logu,
- rychle karty do modulu Importy, Rejstrik firem, Registr lidi, Audit log a TODO list.

Backend:

- `GET /api/health`
- `GET /api/dashboard`

### Importni centrum

Importni centrum prijima firemni data z ruznych zdroju a uklada je do jednotneho modelu.

Implementovano:

- JSON import firem vcetne osob a roli,
- CSV import firem vcetne osob a roli,
- ARES import podle ICO,
- demo importy z predpripravenych ICO,
- historie importnich behu,
- chybove radky importu,
- souhrn importu v UI: pocet behu, ulozene radky, chybne radky a posledni stav,
- rucni obnoveni historie importu.

Backend:

- `POST /api/import/json`
- `POST /api/import/csv`
- `POST /api/import/ares/{ico}`
- `GET /api/import/runs`

CSV format:

```csv
name,registrationNumber,country,legalForm,people
Nova Data Systems s.r.o.,12345678,CZ,s.r.o.,Jan Novak|jednatel;Eva Svobodova|spolecnik
```

JSON format:

```json
[
  {
    "name": "Atlas Data Lab s.r.o.",
    "registrationNumber": "70010001",
    "country": "CZ",
    "legalForm": "s.r.o.",
    "address": "Na Prikope 12, Praha",
    "people": [
      { "fullName": "Jan Novak", "role": "jednatel" }
    ]
  }
]
```

### Rejstrik firem

Rejstrik firem je hlavni vyhledavaci cast aplikace. Umoznuje najit firmu a otevrit detail zaznamu.

Implementovano:

- vyhledavani podle normalizovaneho nazvu firmy,
- vyhledavani podle ICO,
- vyhledavani podle osoby,
- vyhledavani podle role osoby,
- vysledkovy seznam firem,
- detail firmy,
- zobrazeni napojenych osob,
- zobrazeni historie zmen,
- filtrovani watchlistu,
- rychle oznaceni firmy jako sledovane.

Backend:

- `GET /api/companies/search?q=...`
- `GET /api/companies/{id}`
- `POST /api/companies`
- `PATCH /api/companies/{id}/watchlist`

### Registr lidi

Registr lidi je samostatny modul pro prehled osob napojenych na firmy.

Implementovano:

- vyhledavani osob podle jmena,
- detail osoby,
- prehled firem, kde je osoba napojena,
- prehled roli osoby ve firmach,
- prechod z osoby zpet na firmu.

Backend:

- `GET /api/people?q=...`
- `GET /api/people/{id}`

### Prirazovani osob k firmam

Prirazovani osob je dulezita cast projektu, protoze z jednoduche databaze firem dela vztahovy system.

Implementovano:

- rucni prirazeni osoby k firme,
- ulozeni role osoby ve firme,
- uprava role existujici osoby,
- odstraneni vazby osoby z firmy,
- zapis techto zmen do historie.

Backend:

- `POST /api/companies/{id}/people`
- `PATCH /api/companies/{id}/people/{personId}`
- `DELETE /api/companies/{id}/people/{personId}`

### Audit log a historie zmen

Audit log uchovava udalosti, ktere se v systemu odehraly.

Implementovano:

- historie vytvoreni firmy,
- historie aktualizace firmy,
- historie watchlistu,
- historie prirazeni, upravy a odebrani osoby,
- udalosti demo dat,
- udalosti importu a chyb,
- filtrovani podle typu udalosti,
- filtrovani podle dulezitosti v UI,
- archivace auditnich udalosti,
- zobrazeni archivu,
- tiskovy vypis audit logu.

Backend:

- `GET /api/audit`
- `GET /api/audit/types`
- `POST /api/audit/{id}/archive`

### TODO list projektu

TODO list je primo soucasti aplikace. Slouzi pro rizeni dalsiho vyvoje a prokazuje, ze projekt umi pracovat i s jednoduchym pracovnim workflow.

Implementovano:

- seznam ukolu,
- zalozeni ukolu,
- uprava ukolu,
- oznaceni ukolu jako hotoveho,
- archivace ukolu,
- zobrazeni archivu,
- segmenty ukolu,
- priority ukolu.

Backend:

- `GET /api/tasks`
- `POST /api/tasks`
- `PUT /api/tasks/{id}`
- `PATCH /api/tasks/{id}/done`
- `PATCH /api/tasks/{id}/archive`

## 3. Datovy model

### Company

Reprezentuje firmu.

Hlavni vlastnosti:

- nazev firmy,
- normalizovany nazev,
- ICO nebo registracni cislo,
- stat,
- pravni forma,
- adresa,
- zdroj dat,
- watchlist priznak,
- datum vytvoreni a aktualizace,
- vazby na osoby,
- historie zmen.

### Person

Reprezentuje osobu.

Hlavni vlastnosti:

- cele jmeno,
- normalizovane jmeno,
- vazby na firmy pres role.

### CompanyPersonRole

Spojovaci entita mezi firmou a osobou.

Hlavni vlastnosti:

- firma,
- osoba,
- role osoby ve firme.

Tato entita je dulezita, protoze stejna osoba muze vystupovat u vice firem a v kazde muze mit jinou roli.

### ChangeEvent

Reprezentuje auditni udalost nebo historii zmen.

Hlavni vlastnosti:

- typ udalosti,
- popis udalosti,
- dulezitost,
- vazba na firmu,
- priznak archivace,
- datum vytvoreni.

### ImportRun

Reprezentuje jeden importni beh.

Hlavni vlastnosti:

- typ zdroje: JSON, CSV, ARES,
- stav: SUCCESS, PARTIAL, FAILED,
- pocet radku,
- pocet ulozenych radku,
- pocet chybnych radku,
- cas startu a dokonceni,
- seznam chyb radku.

### ImportRowError

Reprezentuje chybu konkretniho radku importu.

Hlavni vlastnosti:

- cislo radku,
- puvodni hodnota,
- text chyby,
- vazba na importni beh.

### TaskItem

Reprezentuje ukol v projektovem TODO listu.

Hlavni vlastnosti:

- nazev,
- segment,
- priorita,
- stav hotovo,
- stav archivovano,
- datum vytvoreni a aktualizace.

## 4. Technologicky stack

Aktualni stack:

- Java 8 kompatibilni projekt,
- Spring Boot 2.7.18,
- Spring Web,
- Spring Data JPA,
- Hibernate,
- H2 pro lokalni vyvoj,
- MariaDB driver pro produkcni profil,
- Maven Wrapper,
- staticke HTML/CSS/JavaScript UI,
- JUnit/Spring Boot testy.

Konfigurace:

- lokalni profil pouziva H2 databazi,
- produkcni profil `prod` pouziva MariaDB pres promenne prostredi,
- H2 konzole je lokalne dostupna,
- produkcne je H2 konzole vypnuta.

## 5. Vyhledavani

Aktualni vyhledavani neni plnotextovy engine typu Elasticsearch. Je to rychle databazove vyhledavani pres JPA dotaz.

Vyhledava se podle:

- zacatku normalizovaneho nazvu firmy,
- zacatku ICO nebo registracniho cisla,
- casti normalizovaneho jmena osoby,
- casti role osoby.

Aktualni prinos:

- uzivatel nemusi znat presny nazev firmy,
- lze najit firmu pres osobu,
- lze najit firmu pres roli,
- vysledky jsou razene podle posledni aktualizace.

Limit T=0:

- neni implementovany externi vyhledavaci index,
- neni implementovana relevance vysledku,
- neni implementovana fuzzy podobnost,
- pro vetsi objem dat bude vhodne pridat databazove indexy a pozdeji samostatnou vyhledavaci vrstvu.

## 6. API mapa T=0

### Firmy

```http
POST /api/companies
GET /api/companies/{id}
GET /api/companies/search?q=...
PATCH /api/companies/{id}/watchlist
POST /api/companies/{id}/people
PATCH /api/companies/{id}/people/{personId}
DELETE /api/companies/{id}/people/{personId}
```

### Lide

```http
GET /api/people?q=...
GET /api/people/{id}
```

### Importy

```http
POST /api/import/json
POST /api/import/csv
POST /api/import/ares/{ico}
GET /api/import/runs
```

### Audit

```http
GET /api/audit
GET /api/audit/types
POST /api/audit/{id}/archive
```

### Dashboard a stav systemu

```http
GET /api/health
GET /api/dashboard
```

### Ukoly

```http
GET /api/tasks
POST /api/tasks
PUT /api/tasks/{id}
PATCH /api/tasks/{id}/done
PATCH /api/tasks/{id}/archive
```

## 7. Co projekt uz dobre demonstruje

Projekt uz dnes demonstruje:

- navrh realne backendove aplikace,
- praci s databazi pres JPA,
- zakladni domenu firem, osob a vazeb,
- import dat z vice zdroju,
- normalizaci dat,
- auditni historii,
- REST API,
- jednoduche webove UI nad API,
- testovatelnost,
- pripravu na produkcni MariaDB profil,
- postupny vyvoj pres ciste commity.

## 8. Aktualni limity T=0

Nejde jeste o hotovy produkcni system. Aktualni limity:

- UI je funkcni, ale stale jednoduche,
- staticke UI je zatim v jednom hlavnim HTML souboru,
- v repu existuji i pripravene JS moduly, ale aktivni UI bezi primarne z inline skriptu v `index.html`,
- CSV parser je jednoduchy a nepokryva vsechny specialni pripady CSV,
- ARES import zavisi na dostupnosti externi sluzby,
- vyhledavani je databazove, ne plnotextove,
- chybi role a prihlasovani uzivatelu,
- chybi migracni nastroj typu Flyway nebo Liquibase,
- produkcni nasazeni Java backendu vyzaduje server s Javou, sdileny webhosting bez Javy nestaci.

## 9. Dalsi doporuceny postup

Dalsi rozvoj by mel jit po techto krocich:

1. Importni centrum
   - predimportni validace,
   - nahled radku pred ulozenim,
   - lepsi CSV parser,
   - detail importniho behu.

2. Osoby a vazby
   - deduplikace osob,
   - lepsi detail osoby,
   - historie vazeb osoby,
   - typy roli.

3. Vyhledavani
   - indexy v databazi,
   - pokrocile filtry,
   - razeni vysledku,
   - pozdeji plnotextovy index.

4. Audit
   - detail udalosti,
   - export auditu,
   - filtr podle firmy/osoby,
   - trvale archivacni pravidlo.

5. Produkcni priprava
   - Flyway/Liquibase migrace,
   - konfigurace pro Linux server,
   - systemd service,
   - zalohy databaze,
   - zakladni monitoring.

## 10. Bezpecna formulace pro prezentaci

Company Intelligence Explorer je navrzene MVP Java backendu pro praci s firemnimi daty. Aktualne umi importovat data z JSON, CSV a ARES, sjednotit nazvy firem, propojit firmy s osobami, uchovat historii zmen, vest auditni log a nabidnout rychle vyhledavani podle firmy, ICO, osoby nebo role. Projekt ukazuje prakticky backendovy problem: nejde jen o ulozeni dat, ale o jejich zpracovani, propojeni, dohledatelnost a dalsi rozsireni.

## 11. Jednovetna definice

Smyslem projektu je ukazat, jak se da z nejednotnych firemnich dat postavit srozumitelny a rozsiritelny Java backend, ktery umi data importovat, sjednotit, propojit s lidmi, auditovat a rychle vyhledavat.
