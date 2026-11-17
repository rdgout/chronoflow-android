# Source from a perf runner. Keep old output before Gradle replaces it, then archive the new evidence as well.
begin_perf_archive() {
  local suite="$1" config="$2" commit="$3"
  local archive_root="${PERF_ARCHIVE_ROOT:-build/perf/$suite}"
  mkdir -p "$archive_root" || return 1
  PERF_ARCHIVE="$(mktemp -d "$archive_root/$(date -u +%Y%m%dT%H%M%SZ)-$commit-$config.XXXXXX")" || return 1
  if [ -d "$PERF_OUTPUT_DIR" ]; then
    mv "$PERF_OUTPUT_DIR" "$PERF_ARCHIVE/previous-output" || return 1
  fi
  echo "perf-$suite: evidence archive $PERF_ARCHIVE"
}

finish_perf_archive() {
  if [ -d "$PERF_OUTPUT_DIR" ]; then
    mv "$PERF_OUTPUT_DIR" "$PERF_ARCHIVE/connected_android_test_additional_output" || return 1
  fi
  local xml="benchmark/build/outputs/androidTest-results/connected/benchmarkRelease"
  if [ -d "$xml" ]; then cp -R "$xml" "$PERF_ARCHIVE/androidTest-results" || return 1; fi
  RESULTS_DIR="$PERF_ARCHIVE/connected_android_test_additional_output/benchmarkRelease"
}
