# Media Optimizer – PRD (Product Requirements Document)

| | |
|---|---|
| **Verze** | 0.2 |
| **Datum** | 2026-10-24 |
| **Autor** | Ondra (zadání), Claude (sepsání) |
| **Stav** | Schváleno (připomínky zapracovány) |
| **Platforma** | Android 10+ (API 29+) |

---

## 1. Shrnutí

Media Optimizer je Android aplikace, která rychle zmenší a zkomprimuje fotky a videa z telefonu, aby se daly snadno poslat e-mailem, přes WhatsApp a podobně. Uživatel označí více souborů v galerii, pošle je přes Android „Sdílet“ do aplikace, zvolí míru zmenšení a výsledné kopie hned sdílí dál. Originály zůstávají nedotčené.

## 2. Problém

- Fotky z moderních telefonů mají 3–10 MB, videa desítky až stovky MB.
- E-mail má obvykle limit cca 20–25 MB na zprávu, ostatní aplikace zmenšují nekontrolovaně, nebo vůbec.
- Ruční zmenšování je zdlouhavé a vyžaduje více aplikací.

## 3. Cíle

1. Zmenšit dávku fotek a videí na pár klepnutí: **Sdílet → zvolit předvolbu → Sdílet dál**.
2. Spolehlivě dostat výsledek pod zadaný limit (např. 25 MB pro e-mail).
3. Ukázat odhad velikosti předem a skutečný výsledek potom.
4. Vše zpracovat offline v telefonu, bez nahrávání na server.

### Mimo rozsah (zatím)

- Zpracování extra dlouhých videí na pozadí (řešit později).
- Úpravy obsahu (filtry, ořez obrazu, text, nálepky).
- Cloud, účty, synchronizace.
- Reklamy (nebudou nikdy).
- iOS verze.

## 4. Uživatelé

- **Primárně:** autor aplikace (osobní použití).
- **Výhledově:** běžní uživatelé Google Play, kteří potřebují rychle poslat fotky a krátká videa.

## 5. Hlavní scénáře (user stories)

| # | Jako uživatel chci… | Abych… |
|---|---|---|
| US1 | v galerii označit více fotek a videí a sdílet je do aplikace | nemusel nic vybírat znovu |
| US2 | otevřít aplikaci a vybrat média přímo v ní | měl alternativu ke sdílení |
| US3 | zvolit jednoduchou předvolbu (Malá / Střední / Velká) | nemusel řešit technické detaily |
| US4 | zadat limit „vejdi se do X MB“ | mohl výsledek poslat e-mailem |
| US5 | vidět odhad velikosti před konverzí a skutečnou velikost po ní | věděl, kolik ušetřím |
| US6 | výsledné soubory hned sdílet přes Android „Sdílet“ | je rychle poslal kamkoliv |
| US7 | volitelně zabalit výsledky do jednoho ZIP souboru | poslal vše jako jednu přílohu |
| US8 | u videa oříznout začátek/konec a odebrat zvuk | poslal jen to podstatné a menší |
| US9 | volitelně odstranit polohu GPS a EXIF údaje | chránil své soukromí |
| US10 | přepnout jazyk (EN/CZ) a motiv (světlý/tmavý/systém) | měl aplikaci podle sebe |

## 6. Uživatelský tok

```
Galerie → označit soubory → Sdílet → Media Optimizer
        (nebo: otevřít aplikaci → Vybrat média)
   ↓
Obrazovka „Nastavení konverze“
   - seznam vybraných souborů (náhledy, původní velikost)
   - předvolba: Malá / Střední / Velká  (+ „Vejdi se do X MB“)
   - odkaz „Podrobné nastavení“
   - odhad výsledné velikosti (celkem i po souborech)
   ↓
[Převést] → průběh (progress bar, možnost zrušit)
   ↓
Obrazovka „Výsledek“
   - před / po (velikost, % úspory) pro každý soubor
   - [Sdílet]  [Sdílet jako ZIP]
   ↓
Android „Sdílet“ → e-mail / WhatsApp / …
   ↓
Dočasné soubory se po sdílení (nebo při dalším spuštění) smažou
```

## 7. Funkční požadavky

### 7.1 Vstup

- **F1** Příjem více souborů přes Android sdílení (`ACTION_SEND` i `ACTION_SEND_MULTIPLE`), mix fotek a videí.
- **F2** Výběr médií v aplikaci přes systémový Photo Picker (bez nutnosti oprávnění k celému úložišti).
- **F3** Podporované vstupy:
  - Fotky: JPG, PNG, HEIC/HEIF, WebP
  - Videa: MP4, MOV, 3GP, WebM (dle podpory zařízení)
- **F4** Nepodporovaný soubor se zobrazí s upozorněním a ze zpracování se vynechá.

### 7.2 Fotky

- **F5** Předvolby (delší strana obrázku):

  | Předvolba | Max. delší strana | Kvalita JPG |
  |---|---|---|
  | Malá | 1280 px | ~75 % |
  | Střední | 1920 px | ~80 % |
  | Velká | 2560 px | ~85 % |

- **F6** Obrázek se nikdy nezvětšuje (menší originál zůstane ve svém rozlišení).
- **F7** Zachovat správnou orientaci (EXIF rotace).
- **F8** Výchozí výstupní formát fotek je vždy **JPG** (nejmenší a nejkompatibilnější). Podrobné nastavení: vlastní max. rozlišení, kvalita (%), výstupní formát (JPG / PNG / WebP).

### 7.3 Videa

- **F9** Předvolby:

  | Předvolba | Rozlišení | Orientační velikost 30 s |
  |---|---|---|
  | Malá | 480p | ~3–5 MB |
  | Střední | 720p | ~8–12 MB |
  | Velká | 1080p | ~20–30 MB |

- **F10** Výstup: MP4 (H.264 video + AAC zvuk) – nejširší kompatibilita.
- **F11** Oříznutí začátku a konce (posuvník s náhledem).
- **F12** Odebrání zvuku (přepínač).
- **F13** Podrobné nastavení: rozlišení, datový tok (bitrate), snímková frekvence.
- **F13a** HDR videa se vždy převádějí na SDR (kvůli kompatibilitě u příjemce).

### 7.4 Režim „Vejdi se do X MB“

- **F14** Uživatel zvolí cílový limit pro celou dávku (rychlé volby: 10 / 20 / 25 MB, nebo vlastní).
- **F15** Aplikace rozdělí limit mezi soubory a dopočítá parametry:
  - video: bitrate z délky a cílové velikosti
  - fotky: postupné snižování kvality / rozlišení, dokud se nevejdou
- **F16** Pokud limit nejde splnit ani při minimální rozumné kvalitě, aplikace to předem oznámí (např. „video je na 25 MB příliš dlouhé, zkraťte ho“).

### 7.5 Odhad a výsledek

- **F17** Před konverzí: odhad výsledné velikosti po souborech i celkem, aktualizuje se při změně nastavení.
- **F18** Po konverzi: skutečná velikost, původní velikost a % úspory.

### 7.6 Metadata

- **F19** Volba „Odstranit polohu a EXIF údaje“, ve výchozím stavu **zapnuto** (metadata se neposílají).

### 7.7 Výstup a sdílení

- **F20** Výsledky se ukládají jen dočasně (cache aplikace), originály se nikdy nemění.
- **F21** Tlačítko **Sdílet** otevře Android „Sdílet“ se všemi výslednými soubory.
- **F22** Tlačítko **Sdílet jako ZIP** zabalí výsledky do jednoho souboru (`MediaOptimizer_RRRR-MM-DD_HHMM.zip`).
- **F23** Dočasné soubory se smažou po sdílení / při dalším spuštění / nejpozději po 24 h.
- **F24** Názvy výstupů: `původní_název_opt.jpg` apod.

### 7.8 Nastavení aplikace

- **F25** Jazyk: angličtina (výchozí), čeština.
- **F26** Motiv: světlý / tmavý / podle systému.
- **F27** Výchozí předvolba a výchozí stav volby „odstranit metadata“.

## 8. Nefunkční požadavky

- **N1 Soukromí:** vše offline, žádná data neopouštějí telefon, žádná analytika třetích stran.
- **N2 Výkon:** dávka 10 fotek do ~5 s; 30s video 1080p→720p do ~30 s na novějším telefonu (orientačně).
- **N3 Stabilita:** chyba jednoho souboru nezastaví celou dávku.
- **N4 Zpracování:** v popředí v aplikaci, s možností zrušit. Zpracování na pozadí je pro budoucí verzi.
- **N5 Kompatibilita:** Android 10+ (API 29+).
- **N6 Přístupnost:** podpora velkých písem a čteček obrazovky (základní úroveň).

## 9. Technologie (návrh)

| Oblast | Volba | Proč |
|---|---|---|
| Jazyk | Kotlin | standard pro Android |
| UI | Jetpack Compose + Material 3 | moderní, podpora tmavého režimu |
| Video | AndroidX Media3 Transformer | oficiální knihovna Google, HW akcelerace, malá velikost aplikace |
| Fotky | Android Bitmap API + ExifInterface | vestavěné, bez závislostí |
| ZIP | `java.util.zip` | vestavěné |
| Sdílení souborů | FileProvider | bezpečné sdílení z cache |
| Nákup Pro | Google Play Billing | jednorázový nákup |
| Nastavení | DataStore | ukládání preferencí |

## 10. Monetizace (výhled)

- **Bez reklam.**
- Zdarma základní funkce + **jednorázový nákup „Pro“** pro plné odemčení.
- Rozdělení (schváleno):

  | Funkce | Zdarma | Pro |
  |---|---|---|
  | Předvolby fotek a videí | ✓ | ✓ |
  | Sdílení výsledků | ✓ | ✓ |
  | Odstranění metadat | ✓ | ✓ |
  | Počet souborů v dávce | do 10 | neomezeně |
  | Režim „Vejdi se do X MB“ | – | ✓ |
  | Sdílení jako ZIP | – | ✓ |
  | Podrobné nastavení, ořez videa | – | ✓ |

- V první (osobní) verzi bude vše odemčené, Billing se přidá před vydáním.

## 11. Fáze vývoje

| Fáze | Obsah |
|---|---|
| **MVP (v0.1)** | Příjem ze sdílení + Photo Picker, předvolby fotek a videí, konverze, výsledek před/po, sdílení, mazání cache |
| **v0.2** | Režim „Vejdi se do X MB“, odhad velikosti předem, ZIP, metadata |
| **v0.3** | Ořez videa, odebrání zvuku, podrobné nastavení, převod formátů |
| **v0.4** | Jazyky EN/CZ, motivy, ladění UI, ikona a název |
| **v1.0 (Play)** | Billing (Pro), zásady ochrany soukromí, příprava pro Google Play |
| **Později** | Zpracování na pozadí, dlouhá videa, další formáty |

## 12. Metriky úspěchu

- Fotka zmenšená předvolbou „Střední“ má typicky **< 1 MB**.
- 30s video v předvolbě „Malá“ projde e-mailem (**< 25 MB** s rezervou).
- Režim „Vejdi se do X MB“ dodrží limit v **≥ 95 %** případů.
- Celý tok od sdílení po odeslání zvládnu **do 4 klepnutí**.

## 13. Rozhodnutí a otevřené otázky

Rozhodnuto (2026-10-24):

- Odstranění metadat je ve výchozím stavu zapnuté.
- Rozdělení Zdarma / Pro dle kap. 10.
- Výchozí výstup fotek je vždy JPG.
- HDR videa se převádějí na SDR.

Otevřené:

1. Konečný název aplikace („Media Optimizer“ je pracovní).
