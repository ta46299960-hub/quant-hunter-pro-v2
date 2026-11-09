# Market data provider status

## Binance public Spot adapter

- Implementation: `BinanceSpotProvider`
- Coverage: public cryptocurrency Spot OHLCV candles only.
- Authentication: no API key is needed for this public endpoint.
- Endpoint: Binance Spot `/api/v3/klines`.
- Supported intervals: 1m, 5m, 15m, 1h, 4h, 1d, 1w.
- Limits: request limit is validated by `CandleRequest` (2–2000); the provider uses the supplied limit.
- Network: synchronous adapter; callers must execute it on a background thread.
- Failure behavior: HTTP, network, and parsing failures return an unsuccessful envelope and must not be displayed as live prices.
- Scope boundary: this adapter does not provide Forex, stocks, indices, commodities, derivatives, or trading execution.

## Remaining provider work

Forex, equities, indices, and commodities need separate provider adapters with documented symbol mapping, licensing, rate limits, timestamps, and freshness policies. Do not infer those prices from crypto data or label placeholder values as live.
