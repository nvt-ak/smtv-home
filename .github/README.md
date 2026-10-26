# smtv-home

**smtv-home** is a fork of [CleanYourTV](https://github.com/Gathin23/cleanyourtv) by Gathin (MIT licence), kept as the
Home launcher for my own app, [SmartTV Manager](https://github.com/nvt-ak/smart-tv-manager). It is built for one TV
and one language pair, so it is not meant to be sent back upstream as a pull request: upstream deliberately has no
network access, and this fork adds some.

The fork lives on the branch `smtv-main`. It was forked at upstream commit
[`eb0131a`](https://github.com/Gathin23/cleanyourtv/commit/eb0131a32fd8567086f39002f1782f7c1b078ca6). Everything
the fork changes is in the commits after it (`git log eb0131a..smtv-main`), and [FORK.md](../FORK.md) lists it in words.

## What this fork changes

| Area | Change |
|---|---|
| **Package** | `applicationId` is `com.smtv_manager.home`, so it installs next to upstream's `com.gathin.tvhome`. The Java package is still `com.gathin.tvhome`. |
| **Network** | Adds the `INTERNET` permission, used only by the live cards below. Upstream has none. Nothing else in the launcher uses the network. |
| **Looks** | `design_frame` lays Home out on a reference design's coordinates (2000 x 1125 units, scaled to the screen). `design_profile` picks the design: `crimson` (the default), `aurora` (glass tiles, page dots, cyan focus ring), `hanoi` (sidebar list, greeting and clock, three cards) or `hanoi2` ("Trang chủ" pill, timeline, one column of cards, round header buttons). |
| **Live cards** | Weather with humidity, wind and air quality; SJC gold; RON 95-III petrol; a lunar calendar card with the day's Hoàng Đạo hours; a month calendar. Numbers are fetched in the background at most every 20 to 30 minutes while Home is open, cached, and shown as a dash until a fetch works. |
| **Config keys** | `dashboard`, `design_frame`, `design_profile`, `tagline`, `corner_right`, `corner_bottom`, `subline`, `weather_city`, `weather_lat`, `weather_lon`, `lunar_date`, `single_row`, `app_subtitles`. They are documented in [themes/README.md](../themes/README.md). |
| **Tiles** | In the glass looks a tile shows the app's icon instead of its banner, adaptive icons are drawn by their foreground only, and the sidebar looks show a small line under each app's name. |
| **Languages** | Only English and Vietnamese resources are built (`resourceConfigurations`), which keeps the APK near 400 KB. |
| **Boot** | At boot, while storage is mounted but not listable yet, the launcher keeps showing the last theme and wallpaper from an internal copy instead of falling back to the built-in look. |
| **Build** | `.github/workflows/build-apk.yml` builds and tests the APK on every push to `smtv-main` and publishes it to the rolling prerelease `smtv-home-latest`. The optional secret `SMTV_KEYSTORE_B64` keeps the signing key stable so a new build installs over the old one. |

Upstream's own tools, tests and documentation are untouched unless a commit says otherwise. The themes shipped with
this fork's launcher folder are upstream's; the themes used with SmartTV Manager (`aurora-midnight`, `ha-noi-night`,
`ha-noi-night-2`) live in that app's repository.

## Credits and sources

| What | Where it comes from | Licence or terms |
|---|---|---|
| The launcher, tools, tests, docs and everything not listed below | [Gathin23/cleanyourtv](https://github.com/Gathin23/cleanyourtv) | MIT, Copyright (c) 2026 Gathin. The licence text stays in [LICENSE](../LICENSE). |
| Lunar calendar (`Lunar.java`) | Upstream, based on [Hồ Ngọc Đức's public algorithm](https://www.informatik.uni-leipzig.de/~duc/amlich/) (new moons and solar terms from Jean Meeus' formulas). The fork adds the day, month and year names, the Hoàng Đạo day and hours, and the "next new or full moon" walk on top of it. | Public algorithm |
| Weather, humidity, wind, visibility | [Open-Meteo](https://open-meteo.com/) forecast API | CC BY 4.0, free for non-commercial use. Weather data by Open-Meteo.com. |
| Air quality (US AQI) | [Open-Meteo air-quality API](https://open-meteo.com/en/docs/air-quality-api), which is built on [Copernicus Atmosphere Monitoring Service](https://atmosphere.copernicus.eu/) data | CC BY 4.0. Contains modified Copernicus Atmosphere Monitoring Service information. |
| Gold prices (SJC 9999 and rings) | [vang.today](https://www.vang.today/api/prices) | A public endpoint with no stated terms or uptime promise. Not affiliated with this project. |
| Petrol prices (RON 95-III, vùng 1) | The public portal API of [Petrolimex](https://portals.petrolimex.com.vn/) | Public data, read-only. The API gives no previous price, so the launcher remembers the last change itself to show the rise or fall. Not affiliated with Petrolimex. |
| Weather, gold, fuel, moon, calendar, drop, wind and eye icons (`ic_wx_*`, `ic_moon`, `ic_gold`, `ic_ring`, `ic_fuel`, `ic_chart`, `ic_drop`, `ic_wind`, `ic_eye`, `ic_calendar`, `ic_yinyang`) | [Fluent Emoji Flat](https://github.com/microsoft/fluentui-emoji), Microsoft, converted to Android vector drawables with [svg2vectordrawable](https://github.com/Ashung/svg2vectordrawable) | MIT, Copyright (c) Microsoft Corporation |
| Home glyph (`ic_home`) | [Material Icons](https://fonts.google.com/icons) "home", Google | Apache License 2.0 |
| Outfit font | [Outfit](https://github.com/Outfitio/Outfit-Fonts) by Rodrigo Fuenzalida and the Outfit Project Authors, bundled by upstream | SIL Open Font License 1.1 (see [`addons/*/FONT-LICENSE-OFL.txt`](../addons)) |
| Reference designs and theme wallpapers | Supplied by the owner of this fork for his own use. Wallpapers are not stored in this repository. | Not licensed for reuse |

The fork's own commits are released under the same MIT licence as the rest of the repository.

## Building

JDK 17 and the Android SDK (platform 34, build-tools 34.0.0), then in `launcher/`:

```
./gradlew --no-daemon testReleaseUnitTest lintRelease assembleRelease
```

The APK is `launcher/app/build/outputs/apk/release/app-release.apk`; upstream's size gate is 524,288 bytes and this
fork stays under it. The Python tests run with `python3 -m unittest discover -s tests`.

## Upstream

The original project's README, with its TV list, themes, add-ons and restore guide, is [README.md](../README.md) in this
repository (GitHub shows this page first because it lives in `.github/`). Its numbers and claims (TV models, themes,
languages, launcher size, press) describe upstream, not this fork. Upstream's translations:
[简体中文](../README.zh-CN.md), [Tiếng Việt](../README.vi.md), [日本語](../README.ja.md).
