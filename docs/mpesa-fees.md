# M-Pesa fees

Fees are stored on each transaction, separately from its principal. Cash withdrawal is best recorded as a transfer from M-Pesa to a Cash account: the principal stays in your accounts and only the fee counts as spending. Fees on expenses and incoming payments are also supported. Amounts use integer minor units and a fee is deducted from the source account exactly once.

KES entries in M-Pesa or General accounts can suggest an offline fee. Other accounts and currencies allow manually entered fees. Each fee has a type, amount band, tariff/source label, and confirmation state. An edit to the amount/type resets confirmation. Actual receipts can override every estimate, including a zero fee. Changing a reference table never rewrites historical records. Scheduled payments can include an expected fee; each recorded occurrence remains unconfirmed until checked.

The Fees screen separates confirmed charges from estimates, by month/all time and transaction type. It groups count, principal moved, confirmed/estimated fees, average fee, and effective percentage by transaction type and amount band, with individual charges and CSV export. Main CSV and portable backups retain fee metadata. Transaction fees also appear in ordinary spending, category budgets, and daily charts.

## Sources and limitations

Reference version: 24 November 2026. These are offline estimates, not live rates. The official consumer page returned HTTP 403 to the research client, so current consumer send/withdrawal bands were cross-checked against agreeing published reproductions. The app explicitly asks users to verify against their receipt or statement; it does not label an estimate as a confirmed charge.

- [Safaricom consumer charges](https://www.safaricom.co.ke/personal/m-pesa/mpesa-charges)
- [Safaricom Paybill standard tariff](https://www.safaricom.co.ke/images/Downloads/paybill-standard-tariff.pdf) and [Paybill tariff guide](https://www.safaricom.co.ke/images/Downloads/M-Pesa-Paybill-Tariff-guide.pdf): customer pays, shared, business pays are distinct choices.
- [Safaricom Buy Goods guide](https://www.safaricom.co.ke/images/Downloads/Buy-goods-guides.pdf): ordinary Buy Goods customer payments are free; fuel-station charges require actual entry.
- Consumer band cross-checks: [Contifield](https://contifield.com/ke/mpesa-charges), [Bakifast](https://bakifast.com/en/tools/mpesa-charges-calculator/), [Shmoney](https://www.shmoney.co.ke/blog/mpesa-charges-kenya/).

Pochi, fuel, bank, unknown Paybill, fractional-KES amounts, and unsupported limits require an actual fee. Expired Pochi promotional rates are not reused. Agent and ATM withdrawals have separate schedules. No SMS reading, bank integration, cloud service, or internet permission is used. The app does not make payments.

Tests cover tariff boundaries, unavailable quotes, manual overrides, confirmed versus estimated buckets, transfer conservation, split totals, CSV round trips, draft saving, and the native entry flow.
