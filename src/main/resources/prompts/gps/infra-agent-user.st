## GPS Data Stale Alert — Infrastructure Analysis

Alert: {alertName}
Severity: {severity}
Message: {message}
Started at: {startsAt}

Investigate from an infrastructure and container perspective:
1. Has the application container crashed or been OOM-killed?
2. Is this a widespread failure affecting all wards, or isolated to specific wards?
3. Are there resource constraints (disk full, memory pressure) preventing GPS writes?

Use fetchSsmLogs for container inspection and queryPrometheus for request rate metrics.
Do not suggest fixes — only diagnose the root cause.
