# Media Optimizer – Technický návrh

| | |
|---|---|
| **Verze** | 0.1 (návrh) |
| **Datum** | 2026-10-24 |
| **Navazuje na** | [PRD.md](PRD.md) v0.2 |
| **Stav** | K připomínkám |

---

## 1. Základní parametry

| Parametr | Hodnota |
|---|---|
| Jazyk | Kotlin |
| UI | Jetpack Compose + Material 3 |
| minSdk | 29 (Android 10) |
| target/compileSdk | nejnovější stabilní |
| Video | AndroidX Media3 Transformer |
| Fotky | `ImageDecoder` + `Bitmap.compress` + `ExifInterface` |
| Nastavení | Jetpack DataStore |
| Navigace | Navigation Compose |
| Architektura | MVVM (ViewModel + StateFlow), jeden modul `app` |
| DI | ručně (jednoduchý `AppContainer`), bez Hiltu – pro začátek jednodušší |
| Vývojové prostředí | Android Studio (Windows), Git |

Verze knihoven: vždy aktuální stabilní v době založení projektu (Android Studio je nabídne samo).

## 2. Architektura

Tři vrstvy, každá má jasnou odpovědnost:

```
┌──────────────────────────────┐
│  UI (Compose obrazovky)      │  co uživatel vidí, reaguje na stav
├──────────────────────────────┤
│  ViewModely                  │  drží stav obrazovek, volá logiku
├──────────────────────────────┤
│  Logika (domain + processing)│  konverze, odhady, ZIP, sdílení
└──────────────────────────────┘
```

- UI jen zobrazuje stav (`StateFlow`) a posílá akce do ViewModelu.
- Veškerá práce se soubory běží mimo hlavní vlákno (Kotlin coroutines, `Dispatchers.IO` / `Default`).
- Logika nezávisí na UI, takže jde testovat jednotkovými testy.

## 3. Struktura projektu

```
app/src/main/java/cz/mediaoptimizer/
├── MainActivity.kt            // vstup aplikace, příjem sdílení (intent)
├── MediaOptimizerApp.kt       // Application, AppContainer
├── ui/
│   ├── theme/                 // barvy, typografie, světlý/tmavý motiv
│   ├── navigation/            // NavHost, cesty mezi obrazovkami
│   ├── home/                  // úvodní obrazovka (Vybrat média)
│   ├── setup/                 // seznam souborů + předvolby + odhad
│   ├── progress/              // průběh konverze
│   ├── result/                // před/po + Sdílet / ZIP
│   ├── videotrim/             // ořez videa
│   └── settings/              // jazyk, motiv, výchozí hodnoty
├── domain/
│   ├── model/                 // MediaItem, Preset, ConversionSettings, ConversionResult
│   ├── estimate/              // SizeEstimator
│   └── budget/                // TargetSizePlanner („vejdi se do X MB“)
├── processing/
│   ├── ImageProcessor.kt
│   ├── VideoProcessor.kt
│   ├── MetadataHandler.kt
│   ├── ZipPacker.kt
│   └── ConversionQueue.kt     // zpracuje dávku soubor po souboru
├── data/
│   ├── MediaInfoReader.kt     // načte velikost, rozměry, délku z Uri
│   ├── CacheManager.kt        // dočasné soubory + úklid
│   ├── ShareHelper.kt         // FileProvider + Android „Sdílet“
│   └── SettingsRepository.kt  // DataStore
└── billing/                   // až ve v1.0
```

## 4. Datový model

```kotlin
enum class MediaType { IMAGE, VIDEO }

data class MediaItem(
    val uri: Uri,
    val type: MediaType,
    val name: String,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val durationMs: Long?,        // jen video
    val isHdr: Boolean = false,   // jen video
)

enum class Preset { SMALL, MEDIUM, LARGE }

data class ConversionSettings(
    val preset: Preset = Preset.MEDIUM,
    val targetTotalBytes: Long? = null,   // režim „vejdi se do X MB“
    val stripMetadata: Boolean = true,
    val imageFormat: ImageFormat = ImageFormat.JPEG,
    val imageMaxSide: Int? = null,        // podrobné nastavení
    val imageQuality: Int? = null,
    val videoHeight: Int? = null,
    val videoBitrate: Int? = null,
    val removeAudio: Boolean = false,
    val trims: Map<Uri, TrimRange> = emptyMap(),
)

data class ConversionResult(
    val source: MediaItem,
    val outputFile: File?,
    val outputBytes: Long,
    val error: String?,
)
```

## 5. Obrazovky a navigace

```
Home ──(vybrat média / přijaté sdílení)──► Setup ──► Progress ──► Result
                                             │                    │
                                             ├─► VideoTrim        └─► (Android Sdílet)
                                             └─► Podrobné nastavení (bottom sheet)
Settings (z menu na Home)
```

- **Příjem sdílení:** `MainActivity` má v manifestu `intent-filter` pro `ACTION_SEND` a `ACTION_SEND_MULTIPLE` s typy `image/*` a `video/*`. Přijaté `Uri` předá do sdíleného ViewModelu a otevře rovnou **Setup**.
- **Výběr v aplikaci:** systémový Photo Picker (`PickMultipleVisualMedia`) – nepotřebuje oprávnění k úložišti.

## 6. Zpracování fotek

1. Načtení přes `ImageDecoder` s cílovou velikostí (`setTargetSize`) – dekóduje rovnou menší obrázek (šetří paměť), zvládá HEIC i správnou orientaci.
2. Komprese `Bitmap.compress(JPEG, kvalita)` do `cache/out/`.
3. Metadata:
   - `stripMetadata = true` → výstup je bez EXIF (výchozí chování `Bitmap.compress`).
   - `stripMetadata = false` → zkopírují se vybrané EXIF tagy (datum, fotoaparát, GPS…) přes `ExifInterface`, orientace se nastaví na „normální“ (obrázek už je otočený).
4. Obrázek se nikdy nezvětšuje.

| Předvolba | Max. strana | Kvalita |
|---|---|---|
| SMALL | 1280 px | 75 |
| MEDIUM | 1920 px | 80 |
| LARGE | 2560 px | 85 |

## 7. Zpracování videí

Media3 Transformer:

- **Rozlišení:** efekt `Presentation.createForShortSide(480 / 720 / 1080)` (funguje pro na výšku i na šířku), nikdy nezvětšovat.
- **Bitrate:** `VideoEncoderSettings` s cílovým datovým tokem.
- **Výstup:** MP4, H.264 + AAC.
- **HDR → SDR:** `Composition.HDR_MODE_TONE_MAP_HDR_TO_SDR_USING_OPEN_GL`.
- **Ořez:** `MediaItem.ClippingConfiguration` (začátek/konec).
- **Bez zvuku:** `EditedMediaItem.setRemoveAudio(true)`.
- **Průběh:** `Transformer.getProgress()` dotazovaný ~každých 200 ms → progress bar.
- **Zrušení:** `Transformer.cancel()`.
- **Metadata:** ověřit, co Transformer přenáší (poloha); pokud je třeba, odstranit při zápisu.

| Předvolba | Kratší strana | Video bitrate | Audio | ≈ 30 s |
|---|---|---|---|---|
| SMALL | 480 | 1,0 Mb/s | 128 kb/s | ~4 MB |
| MEDIUM | 720 | 2,5 Mb/s | 128 kb/s | ~10 MB |
| LARGE | 1080 | 5,0 Mb/s | 128 kb/s | ~19 MB |

## 8. Odhad velikosti (`SizeEstimator`)

- **Video:** `(video_bitrate + audio_bitrate) × délka / 8 × 1,02` (režie kontejneru). Přesné, protože bitrate určujeme my.
- **Fotky:** odhad z počtu pixelů výstupu × koeficient podle kvality (např. 0,10–0,25 B/px). Pro zpřesnění se jedna fotka z dávky zkusmo zkomprimuje v paměti a koeficient se podle ní upraví.
- Po konverzi se zobrazí skutečné hodnoty.

## 9. Režim „Vejdi se do X MB“ (`TargetSizePlanner`)

1. Od limitu se odečte 3 % rezerva.
2. Fotky dostanou rozpočet podle odhadu při MEDIUM (případně se sníží).
3. Zbytek se rozdělí mezi videa podle délky.
4. Pro každé video: `bitrate = rozpočet × 8 / délka − audio`. Rozlišení se zvolí podle bitrate (pod ~1,5 Mb/s → 480p).
5. Pokud by bitrate klesl pod minimum (~300 kb/s), aplikace předem upozorní („zkraťte video / zvyšte limit“).
6. Fotky: postupně snižovat kvalitu (90 → 50), pak zmenšovat rozměr (× 0,8), dokud se nevejdou.
7. Po konverzi kontrola skutečné velikosti; při překročení se nejhorší soubor převede znovu s nižším cílem.

## 10. Výstup, sdílení, úklid

- Výstupy v `cacheDir/out/<id-dávky>/`.
- **Sdílení:** `FileProvider` + `Intent.ACTION_SEND_MULTIPLE` s `ClipData` a `FLAG_GRANT_READ_URI_PERMISSION`.
- **ZIP:** `ZipOutputStream` v režimu bez komprese (STORED) – média jsou už zkomprimovaná, takže je to rychlé.
- **Úklid** (`CacheManager`): po návratu ze sdílení, při spuštění aplikace a vše starší než 24 h.

## 11. Nastavení aplikace

- `SettingsRepository` (DataStore): motiv, výchozí předvolba, odstraňovat metadata.
- **Jazyk:** per-app jazyk přes `AppCompatDelegate.setApplicationLocales` (EN výchozí, CZ). Texty v `strings.xml` od začátku, ne napevno v kódu.
- **Motiv:** světlý / tmavý / systém.

## 12. Testování

- **Jednotkové testy:** `SizeEstimator`, `TargetSizePlanner`, výpočty rozměrů.
- **Ruční testy na telefonu:** testovací sada souborů (JPG, HEIC, PNG, video na výšku/šířku, HDR video, 4K video) a kontrolní seznam pro každou část.
- Kód se verzuje v Gitu, každá hotová část = jeden commit (případně tag).

---

## 13. Vývoj po částech

Každá část je malá, má jasný cíl a končí funkční aplikací, kterou vyzkoušíte na telefonu. Na další část se přejde až po vašem odsouhlasení.

**Postup v každé části:** já napíšu kód do složky projektu → vy ho v Android Studiu spustíte na telefonu → dáte mi zpětnou vazbu → opravíme → uložíme (commit).

### Fáze MVP (v0.1)

| Část | Cíl | Hotovo, když… |
|---|---|---|
| **0. Prostředí** | Instalace Android Studia, založení projektu, Git, spuštění na telefonu (USB ladění) | Na telefonu běží prázdná aplikace „Hello“ |
| **1. Kostra** | Motiv, navigace, prázdné obrazovky Home / Setup / Result | Dá se proklikat mezi obrazovkami |
| **2. Vstup médií** | Photo Picker + příjem ze „Sdílet“ v galerii, seznam s náhledy a velikostmi | Z galerie sdílím 5 fotek a 1 video a vidím je v seznamu |
| **3. Konverze fotek** | Předvolby S/M/L, JPG, průběh | Fotky se zmenší, vidím novou velikost |
| **4. Výsledek a sdílení** | Obrazovka před/po, tlačítko Sdílet, úklid cache | Zmenšené fotky pošlu e-mailem ✉️ |
| **5. Konverze videí** | Předvolby 480/720/1080p, HDR→SDR, průběh, zrušení | 30s video pošlu e-mailem |

### Fáze v0.2

| Část | Cíl | Hotovo, když… |
|---|---|---|
| **6. Odhad velikosti** | Odhad před konverzí (soubory i celkem) | Odhad se liší od výsledku max. o ~20 % |
| **7. Metadata** | Přepínač odstranění EXIF/GPS (výchozí zapnuto) | Výstup nemá GPS polohu |
| **8. ZIP** | Sdílení jako jeden ZIP | Pošlu jednu přílohu ZIP |
| **9. Vejdi se do X MB** | Limit pro dávku, upozornění při nesplnitelném limitu | Dávka se vejde do 25 MB |

### Fáze v0.3

| Část | Cíl | Hotovo, když… |
|---|---|---|
| **10. Ořez videa a zvuk** | Posuvník začátek/konec, odebrání zvuku | Pošlu jen 10 s bez zvuku |
| **11. Podrobné nastavení** | Vlastní rozlišení, kvalita, bitrate, formát (JPG/PNG/WebP) | Nastavím vlastní hodnoty |

### Fáze v0.4

| Část | Cíl | Hotovo, když… |
|---|---|---|
| **12. Nastavení aplikace** | Jazyk EN/CZ, motiv, výchozí hodnoty | Přepnu jazyk a motiv |
| **13. Vzhled a ladění** | Ikona, název, drobnosti v UI, chybové stavy | Aplikace působí hotově |

### Fáze v1.0 (Google Play)

| Část | Cíl | Hotovo, když… |
|---|---|---|
| **14. Pro verze** | Google Play Billing, jednorázový nákup, omezení Zdarma | Nákup odemkne Pro funkce |
| **15. Vydání** | Podepsání, zásady ochrany soukromí, popis a obrázky pro Google Play | Aplikace je v interním testování na Play |

## 14. Rizika

| Riziko | Řešení |
|---|---|
| Některé telefony neumí HDR→SDR převod | Zachytit chybu, převést bez tone-mappingu a upozornit |
| Odhad velikosti fotek je nepřesný | Zkušební komprese jedné fotky; po konverzi skutečné hodnoty |
| Velké fotky (200 MP) vyčerpají paměť | Dekódování rovnou do menší velikosti (`setTargetSize`) |
| Dlouhá videa trvají dlouho | Zatím jen v popředí s možností zrušit; na pozadí později |
