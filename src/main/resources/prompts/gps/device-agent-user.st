## GPS Data Stale Alert — Device Analysis

Alert: {alertName}
Severity: {severity}
Message: {message}
Started at: {startsAt}

Investigate from a device and authentication perspective:
1. Which ward devices have stopped sending GPS data in the last 10 minutes?
2. Are their device tokens valid and not expired?
3. Is this a device offline issue (battery dead, no network) or an authentication failure?

Use fetchSsmLogs to run read-only DB queries and log inspection.
Do not suggest fixes — only diagnose the root cause.
