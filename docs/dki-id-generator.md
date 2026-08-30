# DKI ID Generator

DKI ID Generator 1.0.0 je samostatná desktopová aplikace pro Windows x64. Slouží
k vytváření identifikátorů a jejich ukládání do šifrovaného lokálního archivu.
Company Intelligence Explorer na tento nástroj odkazuje jako na doprovodnou pomůcku,
ale nespouští jej na serveru a nepřenáší z něj data do databáze.

## Stažení

- [instalátor EXE](https://github.com/daviddblancica-ops/Company-Intelligence-Explorer/releases/download/dki-id-generator-v1.0.0/DKI-ID-Generator-Setup.exe)
- [instalační balíček MSI](https://github.com/daviddblancica-ops/Company-Intelligence-Explorer/releases/download/dki-id-generator-v1.0.0/DKI-ID-Generator-x64.msi)
- [přehled vydání 1.0.0](https://github.com/daviddblancica-ops/Company-Intelligence-Explorer/releases/tag/dki-id-generator-v1.0.0)

Pro běžnou instalaci je určen soubor EXE. MSI je vhodné pro správu nebo hromadné
nasazení na více počítačů.

## Bezpečnostní hranice

- aplikace pracuje lokálně na počítači uživatele
- heslo, identifikátory a obsah trezoru se automaticky neposílají do CIE
- instalační soubory nejsou součástí Git historie
- verze 1.0.0 zatím nemá digitální podpis vydavatele
- instalaci provádějte pouze z oficiálního vydání projektu a po kontrole SHA-256

Absence digitálního podpisu znamená, že Windows může zobrazit varování SmartScreen.
Kontrolní součet potvrzuje shodu souboru s publikovaným balíčkem, ale nenahrazuje
podpis důvěryhodným certifikátem.

## Kontrolní součty

| Soubor | Velikost | SHA-256 |
|---|---:|---|
| `DKI-ID-Generator-Setup.exe` | 47 805 747 B | `9617146330E65857BA4641160E43F6318C53DC555B1176B5CC156FB17F7FA742` |
| `DKI-ID-Generator-x64.msi` | 46 841 856 B | `45B9C71439E9A89B35D2DB84291739B1A82F1F045E04D6ACB7C37032C2ADD3DB` |

Ověření v PowerShellu:

```powershell
Get-FileHash -Algorithm SHA256 .\DKI-ID-Generator-Setup.exe
Get-FileHash -Algorithm SHA256 .\DKI-ID-Generator-x64.msi
```

Výsledný sloupec `Hash` musí přesně odpovídat hodnotě v tabulce. Při neshodě soubor
nespouštějte a stáhněte jej znovu z přehledu vydání.

## Vztah k CIE

Aktuální verze generátoru neposkytuje REST API, příkazovou řádku ani vlastní URL
protokol. Integrace je proto záměrně oddělená: CIE poskytuje instalační a ověřovací
informace, zatímco vlastní citlivý archiv zůstává pouze v desktopové aplikaci.
