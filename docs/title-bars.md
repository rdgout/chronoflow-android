# Top app bar comparison

User, 2026-11-22: "The medium topbar is much better in my eyes, please keep that."
Every `CfMediumTopAppBar` caller uses native M3 Medium chrome (112 dp expanded,
64 dp collapsed, plus safe top inset), with `exitUntilCollapsedScrollBehavior`.
The Phase 14 branch keeps the system font bold as the comparison baseline.

`TodayTitleBenchmark.todayTitleCollapse` cold-starts with the same
`signedInStartupIntent` as `StartupBenchmark.startupColdSignedIn`. The existing
perf-only Today fixture supplies its content. Each of three measured iterations
includes startup, then three up/down fling pairs. Structural tags identify the
bar and list; bounded height checks require actual collapse and expansion.
Metrics are `StartupTimingMetric` and `FrameTimingMetric`, with
`CompilationMode.Partial(BaselineProfileMode.Require)`.

On the booted pixel, run these on the system-font benchmark commit, then on the
Fraunces experiment commit, with the same device, theme, language and font scale:

```sh
ANDROID_SERIAL=emulator-5554 make perf
ANDROID_SERIAL=emulator-5554 make perf-today-title
```

Both targets hold the device lock, set the perf animation scales and restore
functional prep afterwards. Today evidence (JSON, traces and XML) is archived
under `build/perf/today-title/`, named with the commit. Startup evidence and
budget results use `docs/perf-startup.md` and `build/perf/startup/` as usual.
Compare TTID/TTFD and frame CPU/overrun percentiles against the system-font run;
apply the existing plan.md performance budgets to startup separately. The Today
title check requires `frameDurationCpuMs`, `frameOverrunMs`,
`timeToInitialDisplayMs` and `frameCount` for `todayTitleCollapse` from the fresh
archive. Missing metrics fail evidence; every Today title value is REPORT-only
for every device class, with no new performance threshold.
No device performance measurement has been run for this side task.

The wrapper rename changes profile symbols. Profile regeneration remains an
orchestrator action (`make baseline-profile`); keep the same committed profile
for the initial A/B comparison, then regenerate and remeasure the retained choice.
