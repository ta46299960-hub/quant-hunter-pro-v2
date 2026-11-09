# Risk engine rules

- Position sizing starts from account equity, a user-selected risk percentage, entry, stop and a unit-value multiplier.
- Per-trade risk input is constrained to 0.1%–5%; this is a validation ceiling, not a recommendation.
- A separate maximum notional exposure cap can reduce quantity and therefore reduce actual stop-loss budget usage.
- Bullish stops must be below entry; bearish stops must be above entry. Neutral direction is rejected.
- The calculator estimates loss at the stop only. It does not model gaps, fees, spread, slippage, funding, liquidation, taxes or contract-specific margin.
- All outputs are estimates for paper trading and education. They are not broker orders or guarantees.
