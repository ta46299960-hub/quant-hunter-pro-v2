# Architecture — QUANT HUNTER PRO V2

## Layers

1. Android presentation: eight feature areas, Arabic-first navigation, theme and locale settings.
2. Domain: market types, quote provenance/freshness, signal assessment, risk and paper-trading models.
3. Analysis: deterministic market-specific hybrid scoring, multi-timeframe agreement, explainable reasons.
4. Data providers: provider interfaces and adapters isolated from the scoring engine. Every quote carries source, timestamp and status.
5. Quality: unit tests, deterministic replay fixtures, backtest validation and CI APK builds.

## Android task lifecycle

UI-triggered market requests run through a single latest-request-wins worker. Navigating between sections cancels obsolete work, and Activity destruction shuts the worker down. This bounds background threads during repeated refreshes; provider connect/read timeouts remain the upper bound for blocking network I/O.

## Market-specific policy

The hybrid model shares a market-wide regime view but uses separate profiles for crypto, FX, equities, indices and commodities. A single universal threshold is not assumed to fit all instruments.

## Data integrity rules

- Never label fixture/demo data as live.
- Reject invalid, non-finite, non-positive or stale prices.
- Keep source and observation timestamp with every quote.
- Expose delayed/unavailable states to the UI.
- Do not silently mix instruments from different providers or sessions.

## Signal integrity

The initial scoring class is an explainable scaffold only. Its confidence value is a heuristic agreement indicator, not a calibrated probability, expected return or win rate. It must not be presented as proven performance. Before release, calibrate thresholds with out-of-sample walk-forward tests, transaction fees, spread, slippage and survivorship-bias controls.

## Safety

Paper trading only. No live order routing, custody, API secret collection or investment guarantees.
