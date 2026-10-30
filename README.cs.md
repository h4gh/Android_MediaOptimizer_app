# Media Optimizer

[English](README.md) · **Čeština**

Android aplikace, která zmenší fotky a videa, abyste je mohli poslat e-mailem, přes WhatsApp a další aplikace. Označte soubory v galerii, sdílejte je do aplikace, zvolte velikost a lehké kopie pošlete dál. Originály zůstávají nedotčené a vše běží offline v telefonu.

<p align="center">
  <img src="docs/screenshots/home.png" width="200" alt="Úvodní obrazovka">
  <img src="docs/screenshots/setup.png" width="200" alt="Výběr velikosti">
  <img src="docs/screenshots/result.png" width="200" alt="Výsledek">
  <img src="docs/screenshots/settings.png" width="200" alt="Nastavení">
</p>

## Funkce

- **Sdílet sem, sdílet dál** – pošlete do aplikace více fotek a videí z galerie, nebo je vyberte systémovým výběrem médií.
- **Jednoduché předvolby** – Malá / Střední / Velká pro fotky i videa.
- **Vejdi se do X MB** – nastavte limit (5, 10, Gmail 18, 25 MB nebo vlastní) a aplikace zvolí kvalitu tak, aby se celá dávka vešla.
- **Odhad velikosti** – předem uvidíte očekávanou velikost a úsporu, potom skutečný výsledek.
- **Další možnosti** – u fotek delší strana, kvalita a formát (JPG / WebP / PNG); u videí rozlišení, datový tok a snímková frekvence.
- **Oříznutí videa** – zkraťte začátek a konec s živým náhledem, volitelně odeberte zvuk.
- **Soukromí** – poloha a údaje o fotoaparátu se ve výchozím stavu odstraní.
- **Sdílet jako ZIP** – všechny výsledky v jedné příloze.
- **Podle vás** – světlý, tmavý, AMOLED nebo systémový motiv; barvy Teal, Tangerine, Cobalt nebo podle tapety; angličtina nebo čeština; světlá nebo noční ikona.

<p align="center">
  <img src="docs/screenshots/options.png" width="200" alt="Další možnosti">
  <img src="docs/screenshots/trim.png" width="200" alt="Oříznutí videa">
  <img src="docs/screenshots/dark.png" width="200" alt="Tmavý motiv">
</p>

## Instalace

Každý push sestaví přes GitHub Actions ladicí APK a zveřejní ho v
[Releases](../../releases/latest). Otevřete stránku v telefonu, stáhněte APK
a nainstalujte ho. Vyžaduje Android 10 nebo novější.

## Technologie

| | |
|---|---|
| Jazyk | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Video | Media3 Transformer |
| Min. Android | 10 (API 29) |
| Písma | Sora a DM Sans (SIL Open Font License), přibalená v `app/src/main/res/font` |

Lokální build: `./gradlew assembleDebug`, unit testy: `./gradlew testDebugUnitTest`.

## Dokumentace

- [Požadavky na produkt](docs/PRD.md)
- [Technický návrh](docs/TECH_DESIGN.md)
