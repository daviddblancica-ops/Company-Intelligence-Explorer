# Nasazení na Webglobe

Tento projekt je Spring Boot Java aplikace. Webglobe údaje, které máš k dispozici, stačí pro SFTP/SCP a databázi přes Adminer nebo phpMyAdmin. Před produkčním nasazením je nutné ověřit, že hosting umí dlouho běžící Java proces.

## Co je připravené v projektu

- lokální profil používá H2 databázi
- produkční profil `prod` používá MariaDB/MySQL přes proměnné prostředí
- produkční schéma spravují verzované Flyway migrace
- aplikace vyžaduje přihlášení a rozlišuje role `ADMIN`, `EDITOR` a `VIEWER`
- build vytváří spustitelný `.jar`
- hesla a přístupy nejsou uložené v repozitáři

## Build

```powershell
.\mvnw.cmd clean package
```

Výsledný soubor:

```text
target/company-intelligence-explorer-0.1.0.jar
```

## Proměnné pro produkci

Na hostingu nebo v shell session nastav databázi, port a tři aplikační účty:

```bash
export SPRING_PROFILES_ACTIVE=prod
export DB_HOST="server-databaze"
export DB_PORT="3306"
export DB_NAME="nazev_databaze"
export DB_USER="uzivatel_databaze"
export DB_PASSWORD="heslo_databaze"
export PORT="8080"

export APP_ADMIN_USERNAME="spravce"
export APP_ADMIN_PASSWORD="dlouhe-nahodne-heslo-pro-spravce"
export APP_EDITOR_USERNAME="editor"
export APP_EDITOR_PASSWORD="jine-dlouhe-nahodne-heslo"
export APP_VIEWER_USERNAME="ctenar"
export APP_VIEWER_PASSWORD="treti-dlouhe-nahodne-heslo"
export SESSION_COOKIE_SECURE="true"
```

Hodnoty `DB_HOST`, `DB_NAME`, `DB_USER` a `DB_PASSWORD` vezmi z Webglobe administrace databáze. Hesla nepatří do Gitu ani do dokumentace. Všechna tři uživatelská jména musí být odlišná a každé heslo musí mít alespoň deset znaků.

`SESSION_COOKIE_SECURE=true` použij na serveru dostupném přes HTTPS. Pro dočasné lokální spuštění přes obyčejné HTTP nastav `false`, jinak prohlížeč relační cookie správně neodešle.

Při prvním startu profilu `prod` Flyway vytvoří tabulku `flyway_schema_history` a provede dosud chybějící migrace. Funguje to pro prázdnou databázi i pro databázi, ve které už Hibernate dříve vytvořil tabulky. Hibernate v produkci schéma pouze ověřuje a sám ho nemění.

Před prvním spuštěním nad existující databází vytvoř zálohu. Po startu musí log obsahovat úspěšně dokončené migrace a validaci schématu bez DDL chyb.

## Test spuštění přes SSH

Po nahrání `.jar` na hosting zkus v SSH:

```bash
java -version
java -jar company-intelligence-explorer-0.1.0.jar
```

Pokud Java není dostupná nebo proces hosting po odhlášení ukončí, nejde tento typ Spring Boot aplikace provozovat přímo na sdíleném hostingu. V tom případě lze databázi na Webglobe použít, ale Java backend musí běžet na VPS nebo jiné službě pro dlouho běžící aplikace.

## SFTP/SCP

Použij adresu a port uvedené v aktuální administraci hostingu. Příklad nahrání přes SCP:

```bash
scp -P PORT target/company-intelligence-explorer-0.1.0.jar USER@SERVER:/cesta/k/aplikaci/
```

`USER` je přihlašovací jméno SSH/SFTP účtu. Konkrétní server, uživatele ani heslo neukládej do repozitáře.

## Cron

Cron je vhodný pro pravidelné úlohy, ne pro provoz web serveru. Dává smysl až pro doplňkové úlohy, například denní import nebo kontrolu dostupnosti.

Pro samotný Spring Boot backend je potřeba dlouho běžící proces, typicky systemd, supervisor, Docker, VPS nebo hosting s podporou Java aplikací.
