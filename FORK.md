# smtv-home

A fork of [Gathin23/cleanyourtv](https://github.com/Gathin23/cleanyourtv) (MIT), used as the Home launcher
for the SmartTV Manager app. Forked at upstream commit `eb0131a32fd8567086f39002f1782f7c1b078ca6`.

Differences from upstream:
- The default `applicationId` is `com.smtv_manager.home`, so it installs next to `com.gathin.tvhome`.
- Planned: a dashboard (weather, lunar calendar, prices) that needs the `INTERNET` permission.
  Upstream deliberately has none, so this fork is not meant to be sent back as a pull request.

The upstream licence stays in `LICENSE`. The original README is `README.md`.
