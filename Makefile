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
SMOKE := net.chronoflow.core.testing.tiers.Smoke
SMOKE_TASK := :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.annotation=$(SMOKE)
GENERATORS :=
GENERATOR_CHECKS :=
GENERATORS += tokens
GENERATORS += icons
GENERATORS += strings
GENERATORS += plan-copy
GENERATORS += contracts
GENERATOR_CHECKS += tokens-check
GENERATOR_CHECKS += fonts-check
GENERATOR_CHECKS += assets-check
GENERATOR_CHECKS += strings-check
GENERATOR_CHECKS += plan-copy-check
GENERATOR_CHECKS += contracts-check

.PHONY: quick check test format lint targeted generate generators-check size emu emu-down emu-clean avd-create matrix du
.PHONY: plan-copy-check strings-check tokens tokens-check fonts fonts-check icons assets assets-check strings plan-copy contracts contracts-check snapshots captures perf perf-timeline baseline-profile

quick:
	$(RUN_TIER) quick --results '*/build/test-results/test/*.xml' --results '*/*/build/test-results/test/*.xml' --results 'build-logic/convention/build/test-results/test/*.xml' -- $(GRADLE) spotlessCheck $(JVM_MODULES:%=:%:test) :build-logic:convention:test
	Scripts/test_scripts.sh

check: quick
	$(RUN_TIER) check --results 'app/build/test-results/**/*.xml' -- $(GRADLE) :app:lintDebug :app:testDebugUnitTest :app:testReleaseUnitTest :app:verifyReleaseManifest :app:analyzeReleaseR8Config
	$(MAKE) generators-check
	Scripts/emulator.sh ensure
	$(RUN_TIER) smoke --device --results 'app/build/outputs/androidTest-results/connected/**/*.xml' -- $(GRADLE) $(SMOKE_TASK)

test:
	$(GRADLE) test

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

snapshots captures perf:
	@echo "make $@: lands in Phase 0e (feat/phase-0e-test-harness)" >&2; exit 1

perf-timeline baseline-profile:
	@echo "make $@: lands in Phase 1e (feat/phase-1e-timeline-perf)" >&2; exit 1
