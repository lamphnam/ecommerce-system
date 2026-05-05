# Experiment report

## Normal-load results (REAL)

**Source:** `results/01-normal-load.txt`

This scenario executed successfully against the running stack and provides the only baseline in this report that is explicitly treated as real measured data. The async path (`POST /api/orders`) returned `202` with `0.00%` request failures and async p95 latency of `115.8ms`. The sync baseline path (`POST /api/orders/sync`) returned `200` with `0.00%` request failures and sync p95 latency of `740.81ms`. Across 601 completed iterations and 602 HTTP requests, all checks passed (`601/601`) and `http_req_failed` remained `0.00%`, showing that under steady 10 req/s load the asynchronous path is substantially faster than the synchronous path while both remain functionally correct.

## Spike test (REAL)

**Source available:** `results/02-spike-test.txt`

This scenario was executed. It demonstrates how both request paths behave during a sharp concurrency increase up to 200 VUs. All requests still succeeded (`http_req_failed = 0.00%`, `checks_failed = 0.00%`), but the async latency threshold configured in the script was crossed: async p95 reached `7.38s` and sync p95 reached `7.93s`. The result demonstrates that the system remained available under the spike, but latency degraded significantly under this test profile.

## Slow provider test (REAL RUN, PRECONDITION NOT APPLIED)

**Source available:** `results/03-slow-provider.txt`

This scenario was executed, but the expected slow-provider precondition was not actually in effect during the run. The script completed with `http_req_failed = 0.00%` and `checks_failed = 0.00%`, async p95 latency remained low at `34.81ms`, and sync p50 was only `575.96ms`, which failed the script threshold `p(50)>1000`. This demonstrates that the test ran successfully, but it does **not** prove slow-provider behavior because payment latency was not elevated enough in the running environment.

## Provider failure test (REAL RUN, PRECONDITION NOT APPLIED)

**Source available:** `results/04-provider-failure.txt`

This scenario was executed successfully. All requests succeeded (`http_req_failed = 0.00%`, `checks_failed = 0.00%`), async p95 latency was `20.79ms`, and sync requests returned terminal order states as expected by the script. However, because sync error rate remained `0.00%`, the intended payment-failure condition was not meaningfully triggered during this run. The result demonstrates that the script and request paths worked, but it should not be interpreted as evidence of real degraded payment behavior.

## DLQ behaviour test (REAL)

**Source available:** `results/06-dlq-behaviour.txt`

This scenario was executed successfully and produced direct evidence of dead-letter handling. Orders continued to be accepted (`order created: 202`), request failures remained `0.00%`, and the script confirmed messages in both `payment.process.dlq` and `order.payment-failed.dlq`. Order-create latency remained low (p95 `25.73ms`), which demonstrates the core async property: client-facing order submission stayed responsive while failure handling was pushed into background messaging and DLQ routing.

## Recovery test (REAL RUN, NO MANUAL OUTAGE INJECTED)

**Source available:** `results/07-recovery.txt`

This scenario was executed successfully from k6’s perspective, with `http_req_failed = 0.00%`, `checks_failed = 0.00%`, and async order acceptance preserved throughout the run. The queue monitor reported a `payment_queue_depth` average of `646.7` with p95 `744`. However, because the script is designed for a manual payment-service stop/start sequence and no explicit outage injection was recorded in the run output, this result should be interpreted as a successful script execution and queue observation run, not as a verified recovery drill.

## Projected behavior for intentionally configured scenarios

Where scenario-specific preconditions are intentionally enabled, the expected behavior remains:
- **Slow provider:** async requests stay fast while sync requests inherit provider latency.
- **Provider failure:** async submission remains responsive while downstream saga transitions fail and compensate.
- **Recovery:** queue depth rises during consumer outage and drains after restart.

Those expectations match the design of the implementation, but they should be treated as projected unless the environment is deliberately configured to trigger them during the test run.
