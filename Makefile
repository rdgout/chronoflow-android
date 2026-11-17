SHELL := /bin/bash
.DEFAULT_GOAL := quick
.NOTPARALLEL:
export ANDROID_HOME ?= /opt/homebrew/share/android-commandlinetools
export JAVA_HOME ?= $(shell /usr/libexec/java_home -v 17)
export ANDROID_SERIAL ?= emulator-5554
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
SMOKE_TASK := :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.annotation=$(SMOKE)
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

matrix:
	@set -e; for avd in fold small api29; do \
	  case $$avd in fold) port=5556;; small) port=5558;; api29) port=5560;; esac; \
	  if ! $(ANDROID_HOME)/cmdline-tools/latest/bin/avdmanager list avd -c | grep -qx $$avd; then \
	    echo "make matrix: avd '$$avd' is missing; run: make avd-create AVD=$$avd" >&2; exit 1; \
	  fi; \
	done; \
	for avd in fold small api29; do \
	  case $$avd in fold) port=5556;; small) port=5558;; api29) port=5560;; esac; \
	  Scripts/emulator.sh ensure $$avd; \
	  ANDROID_SERIAL=emulator-$$port $(RUN_TIER) smoke-$$avd --device --results 'app/build/outputs/androidTest-results/connected/**/*.xml' -- $(GRADLE) $(SMOKE_TASK); \
	  Scripts/emulator.sh down $$avd; \
	done

du:
	@du -sh ~/.gradle/caches ~/.android/avd build 2>/dev/null || true
	@git worktree list --porcelain | sed -n 's/^worktree //p' | while read -r wt; do du -sh "$$wt"; done

icons:
	python3 -I tools/assets/build_assets.py icons

assets:
	python3 -I tools/assets/build_assets.py icons moon

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

snapshots:
	$(RUN_TIER) snapshots $(RESULTS_DEBUG) $(TIMINGS_DEBUG) -- $(GRADLE) $(if $(SNAPSHOT_RECORD),recordRoborazziDebug,verifyRoborazziDebug) -Pcf.tier=snapshot

captures:
	@if [ -n "$(DEVICE)" ]; then echo "make captures DEVICE=1 arrives in Phase 2 (feat/phase-2-design-system)" >&2; exit 1; fi
	rm -rf build/captures
	$(RUN_TIER) captures $(RESULTS_DEBUG) $(TIMINGS_DEBUG) -- $(GRADLE) testDebugUnitTest --rerun -Pcf.tier=capture -Pcf.captureOnly=$(ONLY)
	@n=$$(find build/captures -name '*.png' 2>/dev/null | wc -l | tr -d ' '); echo "captures: $$n PNGs in build/captures"; \
	  if [ "$$n" = 0 ] && [ -z "$(ONLY)" ]; then echo "captures: no PNGs written" >&2; exit 1; fi

perf-timeline:
	$(RUN_TIER) perf-timeline --device --results 'benchmark/build/outputs/androidTest-results/connected/benchmarkRelease/**/*.xml' -- Scripts/perf-timeline.sh

baseline-profile:
	$(RUN_TIER) baseline-profile --device --results 'benchmark/build/outputs/androidTest-results/connected/**/*.xml' -- Scripts/baseline-profile.sh
