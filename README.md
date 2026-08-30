# Company Intelligence Explorer

Company Intelligence Explorer je Java backend pro práci s firemními daty. Projekt ukazuje malé, ale smysluplné MVP aplikace, která umí přijmout data o firmách, sjednotit názvy, uložit vazby na osoby, uchovat historii změn a nabídnout rychlé vyhledávání přes REST API.

## Co projekt řeší

Firemní data bývají často roztříštěná, nejednotná a špatně dohledatelná. Tato aplikace je převádí do jednotného modelu, aby bylo možné najít firmu podle názvu nebo identifikace a zobrazit její detail včetně propojených osob a historie změn.

## Hlavní části

- import firemních dat z JSON nebo CSV
- import firemních dat z ARES podle IČO
- sjednocení názvů pro spolehlivé vyhledávání
- uložení firem, osob a jejich rolí
- historie vytvoření a aktualizací firmy
- přihlášení a role administrátor, editor a uživatel pouze pro čtení
- REST API pro založení, detail a hledání firmy
- H2 databáze pro jednoduché lokální spuštění

## Spuštění

Projekt je připravený pro Windows a nevyžaduje globálně nainstalovaný Maven. Maven se při prvním spuštění stáhne do složky `.mvn-local`.

```powershell
.\mvnw.cmd spring-boot:run
```

Aplikace běží na:

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

### Lokální přihlášení

Vývojový profil obsahuje pouze lokální účty určené pro práci na vlastním počítači:

| Role | Uživatel | Heslo | Oprávnění |
|---|---|---|---|
| `ADMIN` | `admin` | `admin-local-2026` | čtení, úpravy, mazání a archivace auditu |
| `EDITOR` | `editor` | `editor-local-2026` | čtení, importy a běžné úpravy |
| `VIEWER` | `viewer` | `viewer-local-2026` | pouze čtení |

Tyto údaje se nesmí použít na veřejném serveru. Produkční profil vyžaduje vlastní účty z proměnných prostředí.

## Testy

```powershell
.\mvnw.cmd test
```

## Produkční profil a hosting

Pro nasazení s externí MariaDB/MySQL databází je připravený profil `prod`.
Před spuštěním musí být kromě databáze nastavené také bezpečnostní proměnné:

```powershell
$env:APP_ADMIN_USERNAME="spravce"
$env:APP_ADMIN_PASSWORD="DLOUHE_NAHODNE_HESLO"
$env:APP_EDITOR_USERNAME="editor"
$env:APP_EDITOR_PASSWORD="JINE_DLOUHE_NAHODNE_HESLO"
$env:APP_VIEWER_USERNAME="ctenar"
$env:APP_VIEWER_PASSWORD="TRETI_DLOUHE_NAHODNE_HESLO"
$env:SESSION_COOKIE_SECURE="true"
$env:REQUIRE_HTTPS="true"
$env:RATE_LIMIT_ENABLED="true"
$env:RATE_LIMIT_LOGIN_MAX_REQUESTS="10"
$env:RATE_LIMIT_LOGIN_WINDOW="5m"
$env:RATE_LIMIT_ARES_MAX_REQUESTS="30"
$env:RATE_LIMIT_ARES_WINDOW="1m"
$env:RATE_LIMIT_IMPORT_MAX_REQUESTS="20"
$env:RATE_LIMIT_IMPORT_WINDOW="1m"
$env:IMPORT_MAX_PAYLOAD_BYTES="1048576"
$env:IMPORT_MAX_ROWS="1000"
$env:IMPORT_MAX_PEOPLE_PER_COMPANY="100"
```

Při lokálním ověřování produkčního profilu přes obyčejné HTTP nastavte obě poslední
hodnoty na `false`. Na veřejném serveru musí zůstat `true` a reverzní proxy musí
předávat hlavičky `X-Forwarded-For` a `X-Forwarded-Proto`.

Postup pro Webglobe, SFTP/SCP a databázové proměnné je v:

```text
docs/webglobe-deploy.md
```

Lokální spuštění s proměnnými z ignorovaného `.env.local`:

```powershell
.\scripts\run-with-env.ps1
```

Skutečná hesla patří pouze do `.env.local` nebo do správy proměnných hostingu.
Soubor `.env.example` obsahuje jen seznam požadovaných klíčů a je bezpečný pro Git.

## Příklady API

Vytvoření nebo aktualizace firmy:

```http
POST /api/companies
Content-Type: application/json
```

```json
{
  "name": "Nová Data Systems s.r.o.",
  "registrationNumber": "12345678",
  "country": "CZ",
  "legalForm": "s.r.o.",
  "registryFileNumber": "C 12345/MSPH",
  "registryRegistrationDate": "2024-01-15",
  "incorporationDate": "2024-01-15",
  "shareCapital": 200000,
  "shareCapitalCurrency": "CZK",
  "people": [
    {
      "fullName": "Jan Novák",
      "role": "jednatel"
    }
  ]
}
```

Vyhledání firmy:

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

Formát CSV:

```csv
name,registrationNumber,country,legalForm,people
Nová Data Systems s.r.o.,12345678,CZ,s.r.o.,Jan Novák|jednatel;Eva Svobodová|společník
```

CSV podporuje standardní uvozovky pro hodnoty obsahující čárku. Výchozí bezpečnostní
limity jsou 1 MiB na požadavek, 1 000 firem v jednom importu, 100 osob u jedné firmy
a 20 hromadných importních nebo kontrolních požadavků za minutu. V produkci je lze
upravit uvedenými proměnnými prostředí.

Import z ARES podle IČO:

```http
POST /api/import/ares/00006947
```

ARES import doplňuje také spisovou značku, datum zápisu, datum vzniku a základní kapitál,
pokud jsou tyto údaje dostupné ve veřejném rejstříku.

Filtrování audit logu podle typu, závažnosti, firmy, importu a období:

```http
GET /api/audit?type=IMPORT_PARTIAL&severity=WARNING&query=Atlas&importRunId=12&from=2026-01-01&to=2026-12-31
```

Export CSV používá stejné filtry:

```http
GET /api/audit/export.csv?severity=WARNING&from=2026-01-01
```

Textové buňky exportu, které by tabulkový procesor mohl vyhodnotit jako vzorec,
se zapisují jako prostý text. Audit tak lze bezpečně otevřít v Excelu nebo LibreOffice.

Hromadná archivace nebo obnovení událostí:

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

Běžné zapisovací endpointy kontrolují povinná pole, délky, datumové hodnoty a číselné
rozsahy ještě před uložením. Neplatný nebo poškozený JSON vrací stav `400` s bezpečnou
zprávou pro uživatele.

## Jak projekt popsat u obhajoby

Jde o navržené MVP backendového systému pro práci s firemními daty. Smyslem je ukázat čistý návrh Java aplikace pro datově orientovaný use-case: import dat, sjednocení názvů, vazby mezi firmami a osobami, historii změn a API rozhraní pro rychlé hledání.

## Jednoduchá analogie

Kdybys měl velkou skříň plnou šanonů o firmách, tenhle projekt dělá čtyři věci:

1. vezme rozházené papíry a správně je zařadí
2. nadepíše je jednotně, aby se daly najít
3. doplní kartičku s historií změn
4. přidá rychlý rejstřík, kde jde vše hledat podle jména nebo identifikace

## Jak to vysvětlit bez technického žargonu

Místo tohohle:

- Elasticsearch
- entity normalization
- history events
- REST API
- monolithic backend

Říkej raději:

- chytrý vyhledávač
- sjednocení dat
- historie změn
- rozhraní pro komunikaci se systémem
- jedna čistá aplikace místo rozděleného složitého řešení
