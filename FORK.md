# smtv-home

A fork of [Gathin23/cleanyourtv](https://github.com/Gathin23/cleanyourtv) (MIT), used as the Home launcher
for the [SmartTV Manager](https://github.com/nvt-ak/smart-tv-manager) app. Forked at upstream commit
`eb0131a32fd8567086f39002f1782f7c1b078ca6`, on the branch `smtv-main`.

`.github/README.md` is the fork's front page (GitHub shows it before the root README) and lists every source and
licence; this file keeps the details. The upstream licence stays in `LICENSE`, and upstream's own README stays
unchanged in `README.md`, because the tests and docs link to it.

## Differences from upstream

- The default `applicationId` is `com.smtv_manager.home`, so it installs next to `com.gathin.tvhome`.
- The manifest adds `INTERNET`, which upstream deliberately does not have, for the live cards. This fork is not
  meant to be sent back as a pull request.
- `design_frame` and `design_profile` (`crimson`, `aurora`, `hanoi`, `hanoi2`, `hanoi3`) lay Home out on a reference design's
  coordinates; `DesignFrame.java` holds each design's measurements.
- A dashboard of live cards: weather with air quality (`AuroraCards`, `HanoiRowCards`, `HanoiCards`, `NightCards`), prices, lunar
  and month calendars, fed by `LiveData.java`. Responses are cached in the `live` SharedPreferences.
- `HanoiDecor.java` draws the `hanoi2` header, timeline, quote and footer on one canvas, `NightDecor.java` the `hanoi3` panel, clock, greeting and corner texts; `MinuteTicker.java` redraws it
  from the system's own minute tick instead of polling.
- Sidebar looks: per-app subtitles (`app_subtitles`), round icon plates, a red focus pill.
- English and Vietnamese resources only (`resourceConfigurations`).
- At boot, the cached theme and wallpaper copy in internal storage is used while external storage is mounted but not
  listable yet (`Config.readable`).
- CI: `.github/workflows/build-apk.yml` builds `smtv-home.apk` to the prerelease `smtv-home-latest`.

## Data sources

| Data | Source | Terms |
|---|---|---|
| Weather | [Open-Meteo](https://open-meteo.com/) | CC BY 4.0, non-commercial |
| Air quality (US AQI) | [Open-Meteo air-quality API](https://open-meteo.com/en/docs/air-quality-api), from Copernicus Atmosphere Monitoring Service data | CC BY 4.0 |
| Gold | [vang.today](https://www.vang.today/api/prices) | No stated terms; may change or disappear |
| Petrol | [Petrolimex portal](https://portals.petrolimex.com.vn/) public API | Public data; no previous price, so the rise or fall is remembered locally |
| Lunar calendar | Upstream `Lunar.java`, [Hồ Ngọc Đức's algorithm](https://www.informatik.uni-leipzig.de/~duc/amlich/) | Computed offline |

## Third-party artwork

- Weather, gold, ring, fuel, chart, moon, drop, wind, eye, calendar and yin-yang icons
  (`res/drawable/ic_wx_*.xml`, `ic_moon.xml`, `ic_gold.xml`, `ic_ring.xml`, `ic_fuel.xml`, `ic_chart.xml`,
  `ic_drop.xml`, `ic_wind.xml`, `ic_eye.xml`, `ic_calendar.xml`, `ic_yinyang.xml`) are from
  [Fluent Emoji Flat](https://github.com/microsoft/fluentui-emoji) (MIT, Copyright (c) Microsoft Corporation),
  converted to vector drawables with [svg2vectordrawable](https://github.com/Ashung/svg2vectordrawable).
- `ic_home.xml` is the Material Icons "home" glyph (Apache License 2.0, Google).
- The Outfit font is from upstream (SIL Open Font License 1.1).
