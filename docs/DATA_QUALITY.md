# Market data quality gates

The V2 engine must not treat a provider response as trustworthy merely because an HTTP request succeeded.

## Provider contract
- Implement `MarketDataProvider` for each external source.
- Keep provider-specific parsing and symbol mapping outside the scoring engine.
- Run network work away from the Android main thread.
- Never commit credentials, private keys, or user API secrets.

## Candle validation
`DataQualityValidator` sorts candles, removes duplicate timestamps, rejects malformed OHLCV values and future timestamps, and checks freshness against the requested interval. A report is usable only when it contains at least two valid candles, is fresh, and has a quality score of at least 60.

## Status honesty
- A provider's `isLive` flag is metadata, not proof of exchange execution-time freshness.
- Show source and fetch timestamp in the UI.
- Missing, stale, malformed, or delayed data must block live-style opportunity claims.
- No bundled sample data may be labelled LIVE.

## Analysis boundaries
Multi-timeframe agreement is an evidence-alignment measure. It is not a win probability, expected return, or guarantee. Backtests must include fees, spread, slippage, and out-of-sample validation before performance claims are made.
