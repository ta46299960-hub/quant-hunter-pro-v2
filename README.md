# QUANT HUNTER PRO V2

Independent Android market-research application (first release candidate).

## Included in this build

- Eight-section native Android interface with charcoal/black, gold and fluorescent-green styling.
- Arabic/English language toggle and dark/light theme, with preferences saved locally.
- Market scanner depth choices: 32, 64, 128, 256 and AUTO.
- Public Binance Spot crypto candles with source, fetch time, candle time and quality checks; only fully closed candles enter analysis, and depth counts completed candles. The watchlist stores up to eight symbols locally.
- Multi-asset crypto watchlist with quality/freshness checks, plus conservative BTC/USDT opportunity assessment using 15m, 1h and 4h candles. A directional assessment is withheld when data quality, freshness or timeframe agreement is insufficient.
- Paper-trade calculator with configurable code-level fee/slippage assumptions; no broker connection or real orders.
- Risk position sizing with stop-distance and exposure-cap checks.
- Historical BTC/USDT 1h EMA-cross research backtest, including fees, spread, slippage, maximum drawdown and a chronological 70%/30% in-sample/out-of-sample split.
- Scheduled background crypto price alerts through Android WorkManager, with network constraints, notification permission handling and a 2% hourly-candle movement threshold.
- Unit tests for data quality, multi-timeframe assessment, paper trading, risk sizing and backtesting.

## Market coverage

| Market | Data connection |
|---|---|
| Crypto | Binance public Spot candles (BTC/USDT flow implemented) |
| Forex | Not connected |
| US stocks | Not connected |
| Global indices | Not connected |
| Commodities (including gold and oil) | Not connected |

The UI can describe all five market groups, but only the stated Crypto Spot adapter supplies live data in this release. Do not interpret an unconnected market as live.

## Research and safety boundaries

- Scores and confidence describe heuristic evidence alignment, not probability of profit.
- The backtest uses a simple EMA crossover and assumptions for fees, spread and slippage. Results are historical and are not a forecast.
- The backtest is not a broker-grade fill model; it does not model funding, borrow costs, market impact, exchange outages or all intrabar paths.
- Paper trading only. No real order execution.
- No profit, accuracy or win-rate claims are made.
- Do not place API secrets in source code or the repository.

## Build and verification

GitHub Actions workflow: [Android CI](https://github.com/ta46299960-hub/quant-hunter-pro-v2/actions/workflows/android-ci.yml)

The workflow assembles a debug APK and runs local unit tests. Use the artifact from a successful run matching the latest commit; an artifact from an older commit is not the current build. Background alerts are best-effort periodic Android work, not guaranteed real-time delivery.

## Installation

Download the `quant-hunter-pro-v2-debug` artifact ZIP from the successful GitHub Actions run, extract it, then install `app-debug.apk` on an Android device. Android may ask you to allow installation from the app used to open the APK.

## Disclaimer

For research and educational use only. Market data availability, freshness and quality can vary. This application is not financial advice.
