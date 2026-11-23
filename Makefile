SHELL := /bin/bash
.DEFAULT_GOAL := quick
.NOTPARALLEL:
export ANDROID_HOME ?= /opt/homebrew/share/android-commandlinetools
export JAVA_HOME ?= $(shell /usr/libexec/java_home -v 17)
IOS_REPO ?= /Users/rickgout/Sites/chronoflow-ios
WEB_REPO ?= /Users/rickgout/Sites/chronoflow-new
export IOS_REPO WEB_REPO
GRADLE := ./gradlew --console=plain
RUN_TIER := Scripts/run-tier.sh
JVM_MODULES := core:model core:plan core:sync core:reminders core:wizard core:network core:data-mock core:testing core:testing-junit konsist tools:tokengen
TIMING_MODULES := core:plan
RESULTS_DEBUG := --results '*/build/test-results/testDebugUnitTest/*.xml' --results '*/*/build/test-results/testDebugUnitTest/*.xml'
TIMINGS_DEBUG := --timings '*/build/cf-timings/testDebugUnitTest/*.json' --timings '*/*/build/cf-timings/testDebugUnitTest/*.json'
RESULTS_JVM := --results 'konsist/build/test-results/test/*.xml' --results 'core/*/build/test-results/test/*.xml' --results 'tools/*/build/test-results/test/*.xml'
TIMINGS_JVM := --timings 'konsist/build/cf-timings/test/*.json' --timings 'core/*/build/cf-timings/test/*.json' --timings 'tools/*/build/cf-timings/test/*.json'
SMOKE := net.chronoflow.core.testing.tiers.Smoke
SMOKE_TASK := :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.annotation=$(SMOKE) -Pandroid.testInstrumentationRunnerArguments.notClass=net.chronoflow.app.reminders.RemindersPermissionDeviceTest -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true
GENERATORS :=
GENERATOR_CHECKS :=
GENERATORS += tokens
GENERATORS += icons
GENERATORS += strings
GENERATORS += plan-copy
GENERATORS += contracts
GENERATORS += mock-seed
GENERATORS += vectors
GENERATOR_CHECKS += tokens-check
GENERATOR_CHECKS += fonts-check
GENERATOR_CHECKS += assets-check
GENERATOR_CHECKS += strings-check
GENERATOR_CHECKS += plan-copy-check
GENERATOR_CHECKS += contracts-check
GENERATOR_CHECKS += mock-seed-check
GENERATOR_CHECKS += vectors-check

.PHONY: quick check test timing format lint targeted generate generators-check size emu emu-down emu-clean avd-create matrix du
.PHONY: plan-copy-check strings-check tokens tokens-check fonts fonts-check icons assets assets-check strings plan-copy contracts contracts-check snapshots captures perf perf-timeline baseline-profile mock-seed mock-seed-check vectors vectors-check

quick:
	$(RUN_TIER) quick $(RESULTS_JVM) $(RESULTS_DEBUG) $(TIMINGS_JVM) $(TIMINGS_DEBUG) -- $(GRADLE) spotlessCheck $(JVM_MODULES:%=:%:test) testDebugUnitTest -Pcf.tier=quick
	$(RUN_TIER) build-logic --results 'build-logic/convention/build/test-results/test/*.xml' -- $(GRADLE) :build-logic:convention:test
	Scripts/test_scripts.sh
	$(MAKE) timing

check: quick
	$(RUN_TIER) check --results 'app/build/test-results/testReleaseUnitTest/*.xml' --timings 'app/build/cf-timings/testReleaseUnitTest/*.json' -- $(GRADLE) :app:lintDebug :app:testReleaseUnitTest -Pcf.tier=quick :app:verifyReleaseManifest :app:analyzeReleaseR8Config
	$(RUN_TIER) layout $(RESULTS_DEBUG) $(TIMINGS_DEBUG) -- $(GRADLE) testDebugUnitTest -Pcf.tier=layout
	$(MAKE) generators-check
	Scripts/emulator.sh ensure
	$(RUN_TIER) smoke --device --emulator --results 'app/build/outputs/androidTest-results/connected/**/*.xml' -- $(GRADLE) $(SMOKE_TASK)
	$(RUN_TIER) reminders-permission --device --emulator --results 'build/reminders-permission/result.xml' -- python3 -I Scripts/reminders_permission.py

test:
	$(RUN_TIER) test $(RESULTS_JVM) $(RESULTS_DEBUG) $(TIMINGS_JVM) $(TIMINGS_DEBUG) -- $(GRADLE) $(JVM_MODULES:%=:%:test) testDebugUnitTest :app:testReleaseUnitTest -Pcf.tier=test
	$(MAKE) timing

timing:
	@if [ -z "$(strip $(TIMING_MODULES))" ]; then echo "timing: no timing tests yet (1b adds them)"; else \
	  $(RUN_TIER) timing --results '*/build/test-results/timingTest/*.xml' --results '*/*/build/test-results/timingTest/*.xml' --timings '*/build/cf-timings/timingTest/*.json' --timings '*/*/build/cf-timings/timingTest/*.json' -- $(GRADLE) $(TIMING_MODULES:%=:%:timingTest) --max-workers=1; fi

format:
	$(GRADLE) spotlessApply

lint:
	$(GRADLE) spotlessCheck :app:lintDebug :konsist:test

targeted:
	python3 -I Scripts/targeted.py $(ONLY)

generate:
	@if [ -z "$(strip $(GENERATORS))" ]; then echo "generate: no generators yet (0b, 0c, 0d add them)"; else $(MAKE) $(GENERATORS); fi

tokens:
	sh tools/tokengen/copy-ios-inputs.sh && $(GRADLE) -q :tools:tokengen:generateTokens

tokens-check:
	python3 -I Scripts/check-sha256-manifest.py tools/tokengen/ios-inputs.sha256 && $(GRADLE) -q :tools:tokengen:verifyTokens :tools:tokengen:test

fonts:
	sh tools/fonts/copy-fonts.sh

fonts-check:
	python3 -I Scripts/check-sha256-manifest.py core/designsystem/fonts.sha256 && $(GRADLE) -q :core:designsystem:testDebugUnitTest --tests '*FontHashTest'

generators-check:
	@if [ -z "$(strip $(GENERATOR_CHECKS))" ]; then echo "generators-check: nothing to check yet"; else $(MAKE) $(GENERATOR_CHECKS); fi

size:
	$(GRADLE) :app:measureBundleSize

emu:
	Scripts/emulator.sh ensure

emu-down:
	Scripts/emulator.sh down

emu-clean:
	Scripts/emulator.sh wipe

avd-create:
	@if [ -z "$(AVD)" ]; then echo "usage: make avd-create AVD=fold|small|api29" >&2; exit 1; fi
	Scripts/emulator.sh create $(AVD)

MATRIX_AVDS = $(if $(strip $(ONLY)),$(strip $(ONLY)),fold small api29)
matrix:
	$(if $(filter-out fold small api29,$(MATRIX_AVDS)),$(error matrix: ONLY must name fold, small or api29))
	python3 -I Scripts/device_lock.py -- bash -ec ' \
	  for avd in $(MATRIX_AVDS); do \
	    if ! $(ANDROID_HOME)/cmdline-tools/latest/bin/avdmanager list avd -c | grep -qx "$$avd"; then \
	      echo "make matrix: avd '\''$$avd'\'' is missing; run: make avd-create AVD=$$avd" >&2; exit 1; \
	    fi; \
	  done; \
	  for avd in $(MATRIX_AVDS); do \
	    case $$avd in fold) port=5556;; small) port=5558;; api29) port=5560;; esac; \
	    Scripts/emulator.sh ensure "$$avd"; \
	    status=0; \
	    ANDROID_SERIAL=emulator-$$port $(RUN_TIER) smoke-$$avd --device --emulator --results "app/build/outputs/androidTest-results/connected/**/*.xml" -- $(GRADLE) $(SMOKE_TASK) || status=$$?; \
	    python3 -I Scripts/matrix-result.py --avd "$$avd" --input "build/test-results/smoke-$$avd/timings.json" --output "build/audit/matrix/$$avd.json" || status=$$?; \
	    if [ "$$status" != 0 ]; then exit "$$status"; fi; \
	    Scripts/emulator.sh down "$$avd"; \
	  done'

du:
	@du -sh ~/.gradle/caches ~/.android/avd build 2>/dev/null || true
	@git worktree list --porcelain | sed -n 's/^worktree //p' | while read -r wt; do du -sh "$$wt"; done

icons:
	python3 -I tools/assets/build_assets.py icons

assets:
	python3 -I tools/assets/build_assets.py icons moon wordmark

assets-check:
	python3 -I tools/assets/build_assets.py check

strings:
	python3 -I tools/l10n/xcstrings2android.py --ios "$(IOS_REPO)"

strings-check:
	python3 -I tools/l10n/xcstrings2android.py --ios "$(IOS_REPO)" --check
	python3 -I -m unittest discover -s tools/l10n -t tools/l10n -p 'test_*.py'

plan-copy:
	python3 -I tools/l10n/plan_copy.py --ios "$(IOS_REPO)"

plan-copy-check:
	python3 -I tools/l10n/plan_copy.py --ios "$(IOS_REPO)" --check

contracts:
	python3 -I tools/contracts-gen/copy_fixtures.py --ios-repo "$(IOS_REPO)"
	python3 -I tools/contracts-gen/generate_kotlin.py --web-repo "$(WEB_REPO)"

contracts-check:
	python3 -I tools/contracts-gen/copy_fixtures.py --ios-repo "$(IOS_REPO)" --check
	python3 -I tools/contracts-gen/generate_kotlin.py --web-repo "$(WEB_REPO)" --check

vectors:
	python3 -I tools/icons/generate_icons.py

vectors-check:
	python3 -I tools/icons/generate_icons.py --check
	python3 -I -m unittest discover -s tools/icons -t tools/icons -p 'test_*.py'

mock-seed:
	python3 -I tools/fixtures/extract_mock_seed.py --ios-repo "$(IOS_REPO)"

mock-seed-check:
	python3 -I tools/fixtures/extract_mock_seed.py --ios-repo "$(IOS_REPO)" --check

perf:
	$(RUN_TIER) perf --device --results 'benchmark/build/outputs/androidTest-results/connected/**/*.xml' -- Scripts/perf-startup.sh

.PHONY: perf-today-title
perf-today-title:
	$(RUN_TIER) perf-today-title --device --results 'benchmark/build/outputs/androidTest-results/connected/benchmarkRelease/**/*.xml' -- bash -ec ' \
	  ADB="$${ADB:-$${ANDROID_HOME}/platform-tools/adb}"; PERF_SUITE=perf-today-title; \
	  source Scripts/perf-device.sh; resolve_perf_device; \
	  trap '\''Scripts/emulator.sh prep-functional "$$SERIAL"'\'' EXIT; \
	  Scripts/emulator.sh prep-perf "$$SERIAL"; \
	  PERF_OUTPUT_DIR=benchmark/build/outputs/connected_android_test_additional_output; \
	  source Scripts/perf-archive.sh; \
	  begin_perf_archive today-title default "$$(git rev-parse --short HEAD)"; \
	  status=0; \
	  $(GRADLE) :benchmark:connectedBenchmarkReleaseAndroidTest \
	    -Pandroid.testInstrumentationRunnerArguments.class=net.chronoflow.benchmark.TodayTitleBenchmark \
	    -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=Macrobenchmark \
	    -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.suppressErrors=EMULATOR \
	    -Pandroid.testInstrumentationRunnerArguments.timeout_msec=300000 || status=$$?; \
	  finish_perf_archive; \
	  python3 -I Scripts/check-benchmarks.py --suite today-title --device-class unknown --results-dir "$$RESULTS_DIR" || status=$$?; \
	  exit $$status'

snapshots:
	$(RUN_TIER) snapshots $(RESULTS_DEBUG) $(TIMINGS_DEBUG) -- $(GRADLE) $(if $(SNAPSHOT_RECORD),recordRoborazziDebug,verifyRoborazziDebug) -Pcf.tier=snapshot

captures:
ifeq ($(DEVICE),1)
	python3 -I Scripts/device_lock.py -- python3 -I Scripts/capture-device.py "$(ONLY)"
else
	python3 -I Scripts/prepare-captures.py build/captures --only "$(ONLY)" --append "$(if $(filter 1,$(APPEND)),1,0)"
	$(RUN_TIER) captures $(RESULTS_DEBUG) $(TIMINGS_DEBUG) -- $(GRADLE) testDebugUnitTest --rerun -Pcf.tier=capture -Pcf.captureOnly=$(ONLY)
	@n=$$(find build/captures -name '*.png' 2>/dev/null | wc -l | tr -d ' '); echo "captures: $$n PNGs in build/captures"; \
	  if [ "$$n" = 0 ] && [ -z "$(ONLY)" ]; then echo "captures: no PNGs written" >&2; exit 1; fi
endif

.PHONY: audit-coverage
audit-coverage:
	python3 -I Scripts/board-coverage.py --canvas _design/canvas/canvas.json --scope tools/audit/board-scope.json --captures build/captures --out build/audit

.PHONY: ios-parity
ios-parity:
	python3 -I Scripts/device_lock.py -- python3 -I Scripts/ios-parity.py $(if $(ONLY),--only "$(ONLY)")

.PHONY: a11y
a11y:
	python3 -I Scripts/device_lock.py -- bash -c 'Scripts/emulator.sh ensure && exec $(RUN_TIER) a11y --device --emulator --results "app/build/outputs/androidTest-results/connected/**/*.xml" -- $(GRADLE) :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.package=net.chronoflow.app.audit -Pandroid.testInstrumentationRunnerArguments.notAnnotation=net.chronoflow.core.testing.tiers.Capture $(if $(filter 1,$(FULL)),-Pandroid.testInstrumentationRunnerArguments.cf.auditFull=1)'

perf-timeline:
	$(RUN_TIER) perf-timeline --device --results 'benchmark/build/outputs/androidTest-results/connected/benchmarkRelease/**/*.xml' -- bash -c ' \
	  reset_window() { \
	    local adb_cmd="$${ADB:-$${ANDROID_HOME}/platform-tools/adb}"; \
	    if [ -n "$${ANDROID_SERIAL:-}" ]; then \
	      "$$adb_cmd" -s "$$ANDROID_SERIAL" shell wm size reset; \
	    else "$$adb_cmd" shell wm size reset; fi; \
	  }; \
	  trap reset_window EXIT; \
	  Scripts/perf-timeline.sh'

baseline-profile: export TIER_IDLE_TIMEOUT := 1200
baseline-profile:
	$(RUN_TIER) baseline-profile --device --results 'benchmark/build/outputs/androidTest-results/connected/**/*.xml' -- Scripts/baseline-profile.sh

.PHONY: version-bump
version-bump:
	python3 -I Scripts/bump-version.py $(PART)

.PHONY: release-check
release-check:
	$(GRADLE) :app:assembleRelease :app:testReleaseUnitTest :app:verifyReleaseManifest
	python3 -I Scripts/check-release-apk.py --apk "app/build/outputs/apk/release/$$(python3 -I -c 'import json; print(json.load(open("app/build/outputs/apk/release/output-metadata.json"))["elements"][0]["outputFile"])')" --mapping app/build/outputs/mapping/release/mapping.txt --allowed-perf-stubs app/src/releaseNoPerf/kotlin/net/chronoflow/app/perf
	$(MAKE) size

.PHONY: release-smoke
release-smoke:
	$(RUN_TIER) release-smoke --device --results 'benchmark/build/outputs/androidTest-results/connected/**/*.xml' -- Scripts/release-smoke.sh

.PHONY: perf-rn
perf-rn:
	$(RUN_TIER) perf-rn --device --results 'benchmark/build/outputs/androidTest-results/connected/**/*.xml' -- Scripts/perf-compare-rn.sh

.PHONY: font-scale-check
font-scale-check:
	python3 -I Scripts/device_lock.py -- sh -ec 'Scripts/emulator.sh ensure; $(GRADLE) :app:installDebug; python3 -I Scripts/font-scale-check.py'

.PHONY: audit-suite
audit-suite:
	python3 -I Scripts/device_lock.py -- bash -c 'Scripts/emulator.sh ensure || exit $$?; status=0; $(RUN_TIER) audit-suite --device --emulator --results "app/build/outputs/androidTest-results/connected/**/*.xml" -- $(GRADLE) :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.notClass=net.chronoflow.app.reminders.RemindersPermissionDeviceTest -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true -Pandroid.testInstrumentationRunnerArguments.cf.audit.language=nl -Pandroid.testInstrumentationRunnerArguments.cf.audit.theme=dark -Pandroid.testInstrumentationRunnerArguments.cf.audit.reduceMotion=1 || status=$$?; $(RUN_TIER) audit-permission --device --emulator --results "build/reminders-permission/result.xml" -- python3 -I Scripts/reminders_permission.py --audit || status=$$?; exit $$status'

.PHONY: process-death
process-death:
	python3 -I Scripts/device_lock.py -- sh -ec 'Scripts/emulator.sh ensure; $(GRADLE) :app:installDebug; python3 -I Scripts/process-death-sweep.py $(if $(ONLY),--only "$(ONLY)")'

AUDIT_ANDROID_MODULES := $(subst /,:,$(patsubst %/src/androidTest,%,$(wildcard */src/androidTest */*/src/androidTest)))
.PHONY: strictmode-run
strictmode-run:
	python3 -I Scripts/device_lock.py -- bash -c ' \
	  set -u; \
	  Scripts/emulator.sh ensure || exit $$?; \
	  mkdir -p build/audit; \
	  adb_cmd="$${ADB:-$${ANDROID_HOME}/platform-tools/adb}"; serial="$${ANDROID_SERIAL:-emulator-5554}"; \
	  "$$adb_cmd" -s "$$serial" logcat -G 16M || exit $$?; \
	  "$$adb_cmd" -s "$$serial" logcat -c || exit $$?; \
	  status=0; \
	  $(RUN_TIER) strictmode --device --emulator $(foreach module,$(AUDIT_ANDROID_MODULES),--results "$(subst :,/,$(module))/build/outputs/androidTest-results/connected/debug/**/*.xml") -- $(GRADLE) $(AUDIT_ANDROID_MODULES:%=:%:connectedDebugAndroidTest) -Pandroid.testInstrumentationRunnerArguments.notClass=net.chronoflow.app.reminders.RemindersPermissionDeviceTest --max-workers=1 --continue -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true || status=$$?; \
	  $(RUN_TIER) strictmode-permission --device --emulator --results "build/reminders-permission/result.xml" -- python3 -I Scripts/reminders_permission.py || status=$$?; \
	  python3 -I Scripts/ios-parity.py || status=$$?; \
	  "$$adb_cmd" -s "$$serial" logcat -d -v threadtime > build/audit/strictmode-logcat.txt || exit $$?; \
	  python3 -I Scripts/strictmode-scan.py build/audit/strictmode-logcat.txt || status=$$?; \
	  exit $$status'
