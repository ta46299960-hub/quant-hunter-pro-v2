# QUANT HUNTER PRO V2

A clean, independent Android market-analysis application built from scratch.

## Approved scope

Markets: Crypto, Forex, Stocks, Indices, and Commodities.  
Analysis mode: Hybrid — a shared market overview combined with market-specific rules.

## Product principles

- Eight modules: Command Center, Market Scanner, Quant Hunter, Watch Room, Paper Trading, Analysis Lab, Risk Manager, Settings & Alerts.
- Arabic and English UI; dark and light themes.
- Black/charcoal surfaces with gold and fluorescent-green accents.
- Data freshness and source quality are visible; unavailable data is never presented as live.
- Signals are explainable and are not guarantees of profit.
- Paper trading only. No real order execution.
- New codebase; the legacy repository is not modified.

## Engineering status

This repository is being initialized. The first milestone is a buildable Android shell, typed market-domain models, deterministic analysis primitives, and automated tests. Live market adapters will be added behind provider interfaces and clearly report their limitations.

## Build

Open the project in Android Studio and run the `app` configuration, or use the included GitHub Actions workflow to build the debug APK.

## Disclaimer

For research and educational use only. Market data may be delayed, incomplete, or unavailable. This app does not provide financial advice.
