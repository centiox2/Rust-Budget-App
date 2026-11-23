import test from "node:test";
import assert from "node:assert/strict";
import {
  cents,
  summary,
  validate,
  emptyData,
  demoData,
} from "../src/model.mjs";
test("decimal amounts become exact integer cents", () => {
  assert.equal(cents("19.99"), 1999);
  assert.equal(cents("0.29"), 29);
  for (const value of [
    "0",
    "-5",
    "1.999",
    "NaN",
    "Infinity",
    "1e3",
    "1000000001",
    "",
  ])
    assert.throws(() => cents(value));
});
test("monthly totals and lifetime balance remain distinct", () => {
  const rows = [
    { type: "income", amount: 10000, date: "2026-10-01", category: "Other" },
    { type: "income", amount: 50000, date: "2026-11-01", category: "Other" },
    {
      type: "expense",
      amount: 1200,
      date: "2026-11-03",
      category: "Groceries",
    },
    { type: "expense", amount: 600, date: "2026-10-03", category: "Dining" },
  ];
  const s = summary(rows, "2026-11");
  assert.equal(s.balance, 58200);
  assert.equal(s.income, 50000);
  assert.equal(s.spent, 1200);
  assert.equal(s.byCategory.Groceries, 1200);
  assert.equal(s.byCategory.Dining, 0);
});
test("backup validates schema, dates, duplicate IDs and integer amounts", () => {
  assert.equal(validate(emptyData()).version, 1);
  const d = demoData("Alex", "USD");
  validate(d);
  for (const mutate of [
    (x) => (x.transactions[0].amount = -10),
    (x) => (x.transactions[0].amount = 1.5),
    (x) => (x.transactions[0].date = "2026-02-30"),
    (x) => x.transactions.push(x.transactions[0]),
    (x) => (x.budgets = { "2026-13": { Groceries: 100 } }),
    (x) => (x.currency = "XXX"),
    (x) => (x.transactions[0].title = "   "),
  ]) {
    const next = structuredClone(d);
    mutate(next);
    assert.throws(() => validate(next));
  }
});
