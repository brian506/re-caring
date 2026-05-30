## GPS Data Stale Alert — SSE Server Analysis

Alert: {alertName}
Severity: {severity}
Message: {message}
Started at: {startsAt}

Investigate from a server-side SSE and GPS processing perspective:
1. Is the GPS endpoint receiving requests at all, or have writes stopped completely?
2. Are there errors in the GPS write or SSE broadcast path?
3. Is container memory or CPU causing SSE instability?

Use fetchSsmLogs for log inspection and queryPrometheus for metrics.
Do not suggest fixes — only diagnose the root cause.
