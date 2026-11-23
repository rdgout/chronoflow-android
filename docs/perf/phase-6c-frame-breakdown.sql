-- Pixel timelineScroll iter000, for each of the three archives in docs/perf-6c-review.md.
SELECT t.name AS thread_name, count(*) AS n,
       avg(s.dur) / 1e6 AS mean_ms,
       percentile(s.dur, 99) / 1e6 AS p99_ms
FROM slice s
JOIN thread_track tt ON tt.id = s.track_id
JOIN thread t USING (utid)
JOIN process p USING (upid)
WHERE p.name = 'net.chronoflow.app'
  AND ((t.tid = p.pid AND s.name LIKE 'Choreographer#doFrame%')
       OR (t.name = 'RenderThread' AND s.name LIKE 'DrawFrame%'))
GROUP BY t.name;

SELECT t.name AS thread_name, c.name, count(*) AS n,
       avg(c.dur) / 1e6 AS mean_ms,
       percentile(c.dur, 99) / 1e6 AS p99_ms
FROM slice s
JOIN thread_track tt ON tt.id = s.track_id
JOIN thread t USING (utid)
JOIN process p USING (upid)
JOIN slice c ON c.parent_id = s.id
WHERE p.name = 'net.chronoflow.app'
  AND ((t.tid = p.pid AND s.name LIKE 'Choreographer#doFrame%')
       OR (t.name = 'RenderThread' AND s.name LIKE 'DrawFrame%'))
GROUP BY t.name, c.name
ORDER BY sum(c.dur) DESC;

SELECT p.name AS process_name, s.name, count(*) AS n,
       avg(s.dur) / 1e6 AS mean_ms,
       percentile(s.dur, 99) / 1e6 AS p99_ms
FROM slice s
JOIN thread_track tt ON tt.id = s.track_id
JOIN thread t USING (utid)
JOIN process p USING (upid)
WHERE s.dur > 0
  AND ((p.name = 'com.google.android.apps.nexuslauncher'
        AND s.name IN ('input', '[Gesture Monitor] swipe-up',
                       'Drawing  0.00  0.00 1080.00 63.00'))
       OR (p.name = '/system/bin/surfaceflinger'
           AND s.name IN ('prepareFrame', 'postComposition')))
GROUP BY p.name, s.name;
