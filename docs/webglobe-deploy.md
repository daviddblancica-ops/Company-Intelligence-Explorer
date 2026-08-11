# Nasazeni na Webglobe

Tento projekt je Spring Boot Java aplikace. Webglobe udaje, ktere mas k dispozici, staci pro SFTP/SCP a databazi pres Adminer/phpMyAdmin. Pred produkcnim nasazenim je nutne overit, ze hosting umi dlouho bezici Java proces.

## Co je pripravene v projektu

- lokalni profil pouziva H2 databazi
- produkcni profil `prod` pouziva MariaDB/MySQL pres environment variables
- produkcni schema spravuji verzovane Flyway migrace
- build vytvari spustitelny `.jar`
- hesla a pristupy nejsou ulozene v repozitari

## Build

```powershell
.\mvnw.cmd clean package
```

Vysledny soubor:

```text
target/company-intelligence-explorer-0.1.0.jar
```

## Promenne pro produkci

Na hostingu nebo v shell session nastav:

```bash
export SPRING_PROFILES_ACTIVE=prod
export DB_HOST="server-databaze"
export DB_PORT="3306"
export DB_NAME="nazev_databaze"
export DB_USER="uzivatel_databaze"
export DB_PASSWORD="heslo_databaze"
export PORT="8080"
```

Hodnoty `DB_HOST`, `DB_NAME`, `DB_USER` a `DB_PASSWORD` vezmi z Webglobe administrace databaze. Heslo nepatri do gitu ani do dokumentace.

Pri prvnim startu profilu `prod` Flyway vytvori tabulku `flyway_schema_history` a provede dosud chybejici migrace. Funguje to pro prazdnou databazi i pro databazi, ve ktere uz Hibernate drive vytvoril tabulky. Hibernate v produkci schema pouze overuje a sam ho nemeni.

Pred prvnim spustenim nad existujici databazi vytvor zalohu. Po startu musi log obsahovat uspesne dokoncene migrace a validaci schematu bez DDL chyb.

## Test spusteni pres SSH

Po nahrani `.jar` na hosting zkus v SSH:

```bash
java -version
java -jar company-intelligence-explorer-0.1.0.jar
```

Pokud Java neni dostupna nebo proces hosting po odhlaseni ukonci, nejde tento typ Spring Boot aplikace provozovat primo na sdilenem hostingu. V tom pripade lze databazi na Webglobe pouzit, ale Java backend musi bezet na VPS nebo jine sluzbe pro dlouho bezici aplikace.

## SFTP/SCP

Server:

```text
185.102.21.128
```

Port:

```text
222
```

Priklad nahrani pres SCP:

```bash
scp -P 222 target/company-intelligence-explorer-0.1.0.jar USER@185.102.21.128:/cesta/k/aplikaci/
```

`USER` je prihlasovaci jmeno hlavniho FTP uctu.

## Cron

Cron je vhodny pro pravidelne ulohy, ne pro provoz web serveru. Dava smysl az pro doplnkove ulohy, napriklad denni import nebo kontrolu dostupnosti.

Pro samotny Spring Boot backend je potreba dlouho bezici proces, typicky systemd, supervisor, Docker, VPS, nebo hosting s podporou Java aplikaci.
