import React, { useEffect, useRef, useState } from "react";
import { createRoot } from "react-dom/client";
import { invoke, isTauri } from "@tauri-apps/api/core";
import {
  ArrowUpRight,
  ArrowDownLeft,
  Plus,
  X,
  Home,
  ChartNoAxesCombined,
  Wallet,
  Settings,
  ChevronLeft,
  ChevronRight,
  ArrowRight,
  Check,
  ShoppingBasket,
  Utensils,
  Bus,
  ShoppingBag,
  Zap,
  Heart,
  Clapperboard,
  Ellipsis,
  Download,
  Upload,
  Trash2,
  Search,
  Leaf,
  Bell,
  SlidersHorizontal,
  CircleDollarSign,
  ReceiptText,
  Grid2X2,
} from "lucide-react";
import {
  categories,
  cents,
  summary,
  monthNow,
  emptyData,
  demoData,
  validate,
} from "./model.mjs";
import "./style.css";
type Transaction = {
  id: string;
  title: string;
  amount: number;
  type: "income" | "expense";
  category: string;
  date: string;
};
type Data = {
  version: number;
  name: string;
  currency: string;
  onboarded: boolean;
  transactions: Transaction[];
  budgets: Record<string, Record<string, number>>;
};
const icons: Record<string, React.ElementType> = {
  Groceries: ShoppingBasket,
  Dining: Utensils,
  Transport: Bus,
  Shopping: ShoppingBag,
  Utilities: Zap,
  Health: Heart,
  Entertainment: Clapperboard,
  Other: Ellipsis,
};
const localDate = () => {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
};
function App() {
  const [welcome, setWelcome] = useState(true);
  const [initialType, setInitialType] = useState<"income" | "expense">(
    "expense",
  );
  const [data, setData] = useState<Data>(emptyData() as Data),
    [ready, setReady] = useState(false),
    [error, setError] = useState(""),
    [tab, setTab] = useState("Home"),
    [month, setMonth] = useState(monthNow()),
    [modal, setModal] = useState<"transaction" | "budget" | null>(null),
    [editing, setEditing] = useState<Transaction | null>(null),
    [query, setQuery] = useState(""),
    [filter, setFilter] = useState("all"),
    [toast, setToast] = useState("");
  const [name, setName] = useState(""),
    [currency, setCurrency] = useState("USD"),
    [sample, setSample] = useState(false),
    [confirm, setConfirm] = useState(""),
    [backup, setBackup] = useState("");
  const fileRef = useRef<HTMLInputElement>(null),
    queue = useRef(Promise.resolve());
  useEffect(() => {
    (async () => {
      try {
        const raw = isTauri()
          ? await invoke("load_data")
          : JSON.parse(localStorage.getItem("verdant-budget") || "null");
        if (raw) {
          const d = validate(raw) as Data;
          setData(d);
          setName(d.name);
          setCurrency(d.currency);
        }
        setReady(true);
      } catch (e) {
        setError(`Could not read saved data. ${String(e)}`);
      }
    })();
  }, []);
  useEffect(() => {
    if (toast) {
      const t = setTimeout(() => setToast(""), 3200);
      return () => clearTimeout(t);
    }
  }, [toast]);
  useEffect(() => {
    if (!modal && !confirm) return;
    const previousFocus = document.activeElement as HTMLElement | null;
    const overflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    const focusable = () => {
      const dialogs = document.querySelectorAll<HTMLElement>('[role="dialog"]');
      const active = dialogs[dialogs.length - 1];
      return active
        ? Array.from(
            active.querySelectorAll<HTMLElement>(
              'button:not(:disabled),input,select,textarea,[tabindex="0"]',
            ),
          ).filter((el) => el.offsetParent !== null)
        : [];
    };
    const frame = requestAnimationFrame(() => focusable()[0]?.focus());
    const handleKey = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        event.preventDefault();
        if (confirm) setConfirm("");
        else setModal(null);
      }
      if (event.key === "Tab") {
        const items = focusable(),
          first = items[0],
          last = items.at(-1);
        if (event.shiftKey && document.activeElement === first) {
          event.preventDefault();
          last?.focus();
        } else if (!event.shiftKey && document.activeElement === last) {
          event.preventDefault();
          first?.focus();
        }
      }
    };
    document.addEventListener("keydown", handleKey);
    return () => {
      cancelAnimationFrame(frame);
      document.body.style.overflow = overflow;
      document.removeEventListener("keydown", handleKey);
      previousFocus?.focus();
    };
  }, [modal, confirm]);
  async function save(next: Data) {
    validate(next);
    const op = queue.current.then(async () => {
      if (isTauri()) await invoke("save_data", { data: next });
      else localStorage.setItem("verdant-budget", JSON.stringify(next));
    });
    queue.current = op.catch(() => {});
    try {
      await op;
      setData(next);
      return true;
    } catch (e) {
      setError("Your changes could not be saved: " + String(e));
      return false;
    }
  }
  const money = (v: number) =>
    new Intl.NumberFormat("en-US", {
      style: "currency",
      currency: data.currency,
      maximumFractionDigits: 2,
    }).format(v / 100);
  const s = summary(data.transactions, month),
    limits = data.budgets[month] || {},
    totalBudget = Object.values(limits).reduce((a, b) => a + b, 0),
    budgetSpent = Object.keys(limits).reduce(
      (a, c) => a + (s.byCategory[c] || 0),
      0,
    );
  const monthLabel = new Date(month + "-01T12:00:00").toLocaleDateString(
    "en-US",
    { month: "long", year: "numeric" },
  );
  const rows = data.transactions
    .filter(
      (t) =>
        t.date.startsWith(month) &&
        (filter === "all" || t.type === filter) &&
        `${t.title} ${t.category}`.toLowerCase().includes(query.toLowerCase()),
    )
    .sort((a, b) => b.date.localeCompare(a.date));
  const changeMonth = (step: number) => {
    const d = new Date(month + "-01T12:00:00");
    d.setMonth(d.getMonth() + step);
    setMonth(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}`);
  };
  const openTransaction = (t: Transaction | null = null) => {
    setInitialType(t?.type || "expense");
    setEditing(t);
    setModal("transaction");
  };
  async function start(e: React.FormEvent) {
    e.preventDefault();
    const n = name.trim() || "Friend";
    setName(n);
    await save(
      (sample
        ? demoData(n, currency)
        : { ...emptyData(), onboarded: true, name: n, currency }) as Data,
    );
  }
  async function exportBackup() {
    const content = JSON.stringify(data, null, 2);
    if (isTauri()) {
      setBackup(content);
      setConfirm("backup");
      return;
    }
    const url = URL.createObjectURL(
      new Blob([content], { type: "application/json" }),
    );
    const a = document.createElement("a");
    a.href = url;
    a.download = `verdant-${localDate()}.json`;
    a.click();
    URL.revokeObjectURL(url);
    setToast("Backup downloaded");
  }
  async function importText(text: string) {
    try {
      const next = validate(JSON.parse(text)) as Data;
      if (await save(next)) {
        setName(next.name);
        setCurrency(next.currency);
        setConfirm("");
        setReady(true);
        setError("");
        setToast("Backup restored");
      }
    } catch (e) {
      setError(String(e));
    }
  }
  const Icon = ({ category }: { category: string }) => {
    const I = icons[category] || Ellipsis;
    return <I size={20} />;
  };
  function transactionList(list: Transaction[], limit?: number) {
    return list.length ? (
      <div className="transactions">
        {list.slice(0, limit).map((t) => (
          <button
            className="transaction"
            aria-label={`${t.title}, ${t.type}, ${money(t.amount)}`}
            key={t.id}
            onClick={() => openTransaction(t)}
          >
            <span
              className={
                "category-icon transfer-icon " +
                (t.type === "income" ? "received" : "sent")
              }
            >
              {t.type === "income" ? (
                <ArrowDownLeft size={21} />
              ) : (
                <ArrowUpRight size={21} />
              )}
            </span>
            <span className="transaction-label">
              <strong>{t.title}</strong>
              <small>
                {t.category} <i />{" "}
                {new Date(t.date + "T12:00:00").toLocaleDateString("en-US", {
                  month: "short",
                  day: "numeric",
                })}
              </small>
            </span>
            <strong>
              {t.type === "income" ? "+" : "−"}
              {money(t.amount)}
            </strong>
          </button>
        ))}
      </div>
    ) : (
      <div className="empty">
        <Wallet />
        <h3>A fresh start</h3>
        <p>Add a transaction to see your money in motion.</p>
        <button className="text-button" onClick={() => openTransaction()}>
          Add a transaction <ArrowRight size={16} />
        </button>
      </div>
    );
  }
  function budgetCards() {
    return (
      <div className="budget-grid">
        {Object.entries(limits).map(([c, limit]) => {
          const spent = s.byCategory[c] || 0;
          return (
            <button
              className="budget-card"
              key={c}
              onClick={() => setModal("budget")}
            >
              <div className="spread">
                <span className={"category-icon " + c.toLowerCase()}>
                  <Icon category={c} />
                </span>
                <span className={"tag " + (spent > limit ? "danger" : "")}>
                  {spent > limit
                    ? "Over budget"
                    : `${Math.round((spent / limit) * 100)}% used`}
                </span>
              </div>
              <h3>{c}</h3>
              <div className="budget-amount">
                {money(spent)} <small>/ {money(limit)}</small>
              </div>
              <div className="progress">
                <span
                  style={{
                    width: Math.min((spent / limit) * 100, 100) + "%",
                    background: spent > limit ? "#f2a079" : undefined,
                  }}
                />
              </div>
              <p>
                {money(Math.abs(limit - spent))}{" "}
                {spent > limit ? "over limit" : "left to spend"}
              </p>
            </button>
          );
        })}
      </div>
    );
  }
  if (!ready)
    return (
      <main className="loading">
        <Leaf size={40} />
        <p>{error || "Growing a better money habit…"}</p>
        {error && (
          <form
            onSubmit={(e) => {
              e.preventDefault();
              void importText(backup);
            }}
          >
            <textarea
              aria-label="Recovery backup"
              value={backup}
              onChange={(e) => setBackup(e.target.value)}
              placeholder="Paste backup JSON to recover"
            />
            <button className="primary">Restore backup</button>
          </form>
        )}
      </main>
    );
  if (!data.onboarded && welcome)
    return (
      <main className="welcome-screen">
        <div
          className="welcome-art"
          role="img"
          aria-label="Two gold coins behind dark glass panels"
        />
        <div className="welcome-copy">
          <h1>
            Maintain Your
            <br />
            Personal Finance,
            <br />
            Made Simple
          </h1>
          <p>
            Manage your finances with ease. Start by customizing your app
            experience for smarter financial choices.
          </p>
        </div>
        <button className="start-button" onClick={() => setWelcome(false)}>
          Start Now <ArrowRight size={20} />
        </button>
      </main>
    );
  if (!data.onboarded)
    return (
      <main className="onboarding">
        <div className="wordmark">
          <Leaf /> verdant<span>budget</span>
        </div>
        <div className="setup-heading">
          <h1>Make it personal.</h1>
          <p className="intro">
            A few details, then you’re ready to take control.
          </p>
        </div>
        <form onSubmit={start}>
          <label>
            Your name
            <input
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="What should we call you?"
              maxLength={50}
            />
          </label>
          <label>
            Currency
            <select
              value={currency}
              onChange={(e) => setCurrency(e.target.value)}
            >
              {["USD", "KES", "EUR", "GBP", "INR"].map((c) => (
                <option key={c}>{c}</option>
              ))}
            </select>
          </label>
          <label className="checkbox">
            <input
              type="checkbox"
              checked={sample}
              onChange={(e) => setSample(e.target.checked)}
            />{" "}
            Explore with sample data
          </label>
          <button className="primary">
            Let’s get started <ArrowRight size={20} />
          </button>
        </form>
        <small className="privacy">
          <Leaf size={13} /> Your finances stay on this device.
        </small>
        {error && <p role="alert">{error}</p>}
      </main>
    );
  return (
    <div className={"app tab-" + tab.toLowerCase()}>
      <header className="topbar">
        {tab === "Home" ? (
          <>
            <button
              className="avatar"
              aria-label="Open settings"
              onClick={() => setTab("Settings")}
            >
              {data.name.slice(0, 1).toUpperCase()}
            </button>
            <div className="welcome-name">
              <span>Welcome Back</span>
              <h1>{data.name}</h1>
            </div>
            <button
              className="round-button"
              aria-label="View budget alerts"
              onClick={() => setTab("Budgets")}
            >
              <Bell size={22} />
              {Object.keys(limits).some((c) => s.byCategory[c] > limits[c]) && (
                <i className="alert-dot" />
              )}
            </button>
          </>
        ) : (
          <>
            <button
              className="round-button"
              aria-label="Back to home"
              onClick={() => setTab("Home")}
            >
              <ChevronLeft size={22} />
            </button>
            <h1>{tab}</h1>
            <button
              className="round-button"
              aria-label="Open settings"
              onClick={() => setTab("Settings")}
            >
              <SlidersHorizontal size={22} />
            </button>
          </>
        )}
      </header>
      <main className="content">
        {tab !== "Settings" && (
          <div className="month-picker">
            <button onClick={() => changeMonth(-1)} aria-label="Previous month">
              <ChevronLeft size={16} />
            </button>
            <span>{monthLabel}</span>
            <button onClick={() => changeMonth(1)} aria-label="Next month">
              <ChevronRight size={16} />
            </button>
            <span className="month-note">Monthly overview</span>
          </div>
        )}
        {tab === "Home" && (
          <>
            <section className="balance-card">
              <span className="balance-label">Total Balance</span>
              <h2>{money(s.balance)}</h2>
              <span className="savings-pill">
                <ArrowUpRight size={17} />
                {s.income
                  ? `${Math.round(((s.income - s.spent) / s.income) * 100)}% saved`
                  : "Your fresh start"}
              </span>
              <div className="quick-actions">
                <button
                  onClick={() => {
                    setEditing(null);
                    setInitialType("income");
                    setModal("transaction");
                  }}
                >
                  <span>
                    <CircleDollarSign size={26} />
                  </span>
                  Income
                </button>
                <button
                  aria-label="Add transaction"
                  onClick={() => openTransaction()}
                >
                  <span>
                    <ShoppingBag size={26} />
                  </span>
                  Expense
                </button>
                <button
                  aria-label="Set a budget"
                  onClick={() => setModal("budget")}
                >
                  <span>
                    <ReceiptText size={26} />
                  </span>
                  Budget
                </button>
                <button onClick={() => setTab("Settings")}>
                  <span>
                    <Grid2X2 size={26} />
                  </span>
                  More
                </button>
              </div>
            </section>
            <section className="insight-card reference-promo">
              <div>
                <h3>Save & Grow</h3>
                <p>
                  {totalBudget
                    ? `${money(Math.max(totalBudget - budgetSpent, 0))} of your budget left`
                    : "A little planning. More possibilities."}
                </p>
                <button
                  className="outline-button"
                  aria-label="View budgets"
                  onClick={() => setTab("Budgets")}
                >
                  View Budgets
                </button>
              </div>
              <img src="/ribbon.svg" alt="" className="ribbon-art" />
            </section>
            <section className="panel">
              <div className="section-title">
                <h2>Transactions</h2>
                <button
                  onClick={() => {
                    setQuery("");
                    setFilter("all");
                    setTab("Transactions");
                  }}
                >
                  See all <ArrowRight size={14} />
                </button>
              </div>
              {transactionList(
                data.transactions
                  .filter((t) => t.date.startsWith(month))
                  .sort((a, b) => b.date.localeCompare(a.date)),
                5,
              )}
            </section>
          </>
        )}
        {tab === "Transactions" && (
          <>
            <div className="toolbar">
              <label className="search">
                <Search size={18} />
                <input
                  aria-label="Search transactions"
                  value={query}
                  onChange={(e) => setQuery(e.target.value)}
                  placeholder="Search your transactions"
                />
              </label>
              <button
                className="icon-button green"
                aria-label="Add transaction"
                onClick={() => openTransaction()}
              >
                <Plus />
              </button>
            </div>
            <div className="chips">
              {["all", "expense", "income"].map((f) => (
                <button
                  className={filter === f ? "selected" : ""}
                  key={f}
                  onClick={() => setFilter(f)}
                >
                  {f === "all"
                    ? "All transactions"
                    : f === "expense"
                      ? "Expenses"
                      : "Income"}
                </button>
              ))}
            </div>
            <section className="panel">
              {rows.length ? (
                transactionList(rows)
              ) : (
                <div className="empty">
                  <Search />
                  <h3>No transactions found</h3>
                  <p>Try another search, month, or add a transaction.</p>
                </div>
              )}
            </section>
          </>
        )}
        {tab === "Budgets" && (
          <>
            <section className="panel budget-overview">
              <span className="eyebrow">MONTHLY SPENDING PLAN</span>
              <div className="spread">
                <h2>
                  {money(budgetSpent)} <small>of {money(totalBudget)}</small>
                </h2>
                <Wallet className="positive" />
              </div>
              <div className="progress">
                <span
                  style={{
                    width:
                      Math.min(
                        totalBudget ? (budgetSpent / totalBudget) * 100 : 0,
                        100,
                      ) + "%",
                  }}
                />
              </div>
              <p>
                {totalBudget
                  ? `${money(Math.max(totalBudget - budgetSpent, 0))} remaining across your planned categories`
                  : "Create your first category budget below."}
              </p>
            </section>
            <div className="section-title">
              <h2>Your categories</h2>
              <button onClick={() => setModal("budget")}>
                <Plus size={16} /> Set limits
              </button>
            </div>
            {budgetCards()}
            {!totalBudget && (
              <div className="empty">
                <Wallet size={32} />
                <h3>Plan a little. Breathe easier.</h3>
                <p>Set a limit for groceries, transport, or anything else.</p>
                <button className="primary" onClick={() => setModal("budget")}>
                  Create a budget <Plus size={18} />
                </button>
              </div>
            )}
          </>
        )}
        {tab === "Analytics" && (
          <>
            <section className="panel analytics-panel">
              <div className="section-title">
                <h2>Spending Overview</h2>
                <span className="category-icon lime">
                  <ArrowUpRight size={22} />
                </span>
              </div>
              <div className="analytics-update">
                <div>
                  <p>Last Update</p>
                  <span>
                    {data.transactions
                      .filter((t) => t.date.startsWith(month))
                      .map((t) => t.date)
                      .sort()
                      .at(-1) || "No entries yet"}
                  </span>
                </div>
                <strong>
                  {s.income
                    ? Math.round((s.spent / s.income) * 100) + "%"
                    : "—"}
                  <small>of income spent</small>
                </strong>
              </div>
              <p className="muted spending-label">Total spent this month</p>
              <div className="chart-total">{money(s.spent)}</div>
              <SpendingChart
                transactions={data.transactions.filter(
                  (t) => t.type === "expense" && t.date.startsWith(month),
                )}
                money={money}
                month={month}
              />
            </section>
            <div className="analytics-categories">
              {["Groceries", "Utilities"].map((c, i) => {
                const spent = s.byCategory[c] || 0;
                const percentage = limits[c]
                  ? Math.round((spent / limits[c]) * 100)
                  : s.spent
                    ? Math.round((spent / s.spent) * 100)
                    : 0;
                return (
                  <button
                    className={"mini-category mini-" + i}
                    key={c}
                    onClick={() => {
                      setQuery(c);
                      setFilter("expense");
                      setTab("Transactions");
                    }}
                  >
                    <div className="spread">
                      <h3>{c}</h3>
                      <span>Monthly</span>
                    </div>
                    <MiniChart
                      transactions={data.transactions.filter(
                        (t) =>
                          t.type === "expense" &&
                          t.category === c &&
                          t.date.startsWith(month),
                      )}
                      month={month}
                    />
                    <div className="spread">
                      <strong>{percentage}%</strong>
                      <small>{money(spent)}</small>
                    </div>
                    <p>
                      {limits[c] ? "of category budget" : "of monthly spending"}
                    </p>
                  </button>
                );
              })}
            </div>
            <section className="panel">
              <div className="section-title">
                <h2>Spending Transactions</h2>
                <button
                  onClick={() => {
                    setQuery("");
                    setFilter("expense");
                    setTab("Transactions");
                  }}
                >
                  See all
                </button>
              </div>
              {transactionList(
                data.transactions
                  .filter(
                    (t) => t.type === "expense" && t.date.startsWith(month),
                  )
                  .sort((a, b) => b.date.localeCompare(a.date)),
                3,
              )}
            </section>
            <section className="panel">
              <div className="section-title">
                <h2>Where it went</h2>
                <span className="muted">{monthLabel.split(" ")[0]}</span>
              </div>
              {s.spent ? (
                categories
                  .filter((c) => s.byCategory[c] > 0)
                  .sort((a, b) => s.byCategory[b] - s.byCategory[a])
                  .map((c) => (
                    <div className="category-breakdown" key={c}>
                      <span className={"category-icon " + c.toLowerCase()}>
                        <Icon category={c} />
                      </span>
                      <div>
                        <div className="spread">
                          <strong>{c}</strong>
                          <strong>{money(s.byCategory[c])}</strong>
                        </div>
                        <div className="progress">
                          <span
                            style={{
                              width: (s.byCategory[c] / s.spent) * 100 + "%",
                            }}
                          />
                        </div>
                      </div>
                      <small>
                        {Math.round((s.byCategory[c] / s.spent) * 100)}%
                      </small>
                    </div>
                  ))
              ) : (
                <div className="empty">
                  <ChartNoAxesCombined />
                  <h3>Your story starts here.</h3>
                  <p>Add expenses to reveal your spending patterns.</p>
                </div>
              )}
            </section>
          </>
        )}
        {tab === "Settings" && (
          <>
            <section className="panel settings">
              <div className="section-title">
                <h2>Your profile</h2>
                <Leaf className="positive" />
              </div>
              <form
                onSubmit={async (e) => {
                  e.preventDefault();
                  if (
                    await save({
                      ...data,
                      name: name.trim() || "Friend",
                      currency,
                    })
                  )
                    setToast("Preferences saved");
                }}
              >
                <label>
                  Name
                  <input
                    maxLength={50}
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    required
                  />
                </label>
                <label>
                  Display currency
                  <select
                    value={currency}
                    onChange={(e) => setCurrency(e.target.value)}
                  >
                    {["USD", "KES", "EUR", "GBP", "INR"].map((c) => (
                      <option key={c}>{c}</option>
                    ))}
                  </select>
                </label>
                <p className="hint">
                  Changing currency changes the symbol only; amounts are not
                  converted.
                </p>
                <button className="primary">
                  Save preferences <Check size={18} />
                </button>
              </form>
            </section>
            <section className="panel settings">
              <h2>Your data, your control.</h2>
              <p>
                Stored privately on this device. Keep a backup before
                uninstalling.
              </p>
              <button
                className="setting-row"
                onClick={() => void exportBackup()}
              >
                <Download size={19} /> Export backup <ChevronRight size={16} />
              </button>
              <button
                className="setting-row"
                onClick={() =>
                  isTauri()
                    ? (setBackup(""), setConfirm("import"))
                    : fileRef.current?.click()
                }
              >
                <Upload size={19} /> Restore backup <ChevronRight size={16} />
              </button>
              <input
                ref={fileRef}
                hidden
                type="file"
                accept="application/json,.json"
                onChange={async (e) => {
                  const f = e.target.files?.[0];
                  if (f) {
                    try {
                      const raw = await f.text();
                      validate(JSON.parse(raw));
                      setBackup(raw);
                      setConfirm("restore");
                    } catch (err) {
                      setError(String(err));
                    }
                  }
                  e.target.value = "";
                }}
              />
              <button
                className="setting-row danger"
                onClick={() => setConfirm("reset")}
              >
                <Trash2 size={19} /> Reset all data <ChevronRight size={16} />
              </button>
            </section>
            <p className="app-note">
              <Leaf size={16} /> Verdant Budget · 1.0.0
              <br />
              Room for what matters.
            </p>
          </>
        )}
      </main>
      <nav className="bottom-nav" aria-label="Main navigation">
        {[
          { name: "Home", icon: Home },
          { name: "Transactions", icon: ArrowUpRight },
          { name: "Budgets", icon: Wallet },
          { name: "Analytics", icon: ChartNoAxesCombined },
          { name: "Settings", icon: Settings },
        ].map(({ name, icon: I }) => (
          <button
            key={name}
            onClick={() => setTab(name)}
            className={tab === name ? "active" : ""}
          >
            <I size={21} />
            <span>{name}</span>
            {tab === name && <i />}
          </button>
        ))}
      </nav>
      {modal && (
        <div className="modal-backdrop" onClick={() => setModal(null)}>
          <section
            className="sheet"
            role="dialog"
            aria-modal="true"
            aria-label={
              modal === "transaction" ? "Transaction" : "Monthly budgets"
            }
            onClick={(e) => e.stopPropagation()}
          >
            <div className="sheet-handle" />
            <div className="section-title">
              <h2>
                {modal === "transaction"
                  ? editing
                    ? "Edit transaction"
                    : "A little money moment."
                  : "Set your intentions."}
              </h2>
              <button
                className="icon-button"
                aria-label="Close"
                onClick={() => setModal(null)}
              >
                <X size={20} />
              </button>
            </div>
            {modal === "transaction" ? (
              <TransactionForm
                key={editing?.id || "new"}
                transaction={editing}
                initialType={initialType}
                month={month}
                onSave={async (t) => {
                  const next = {
                    ...data,
                    transactions: editing
                      ? data.transactions.map((row) =>
                          row.id === t.id ? t : row,
                        )
                      : [...data.transactions, t],
                  };
                  if (await save(next)) {
                    setModal(null);
                    setToast(
                      editing ? "Transaction updated" : "Transaction added",
                    );
                  }
                }}
                onDelete={() => setConfirm("delete")}
              />
            ) : (
              <BudgetForm
                limits={limits}
                month={monthLabel}
                onSave={async (b) => {
                  if (
                    await save({
                      ...data,
                      budgets: { ...data.budgets, [month]: b },
                    })
                  ) {
                    setModal(null);
                    setToast("Budgets saved");
                  }
                }}
              />
            )}
          </section>
        </div>
      )}
      {confirm && (
        <div className="modal-backdrop">
          <section
            className="sheet"
            role="dialog"
            aria-modal="true"
            aria-label="Data action"
          >
            <div className="section-title">
              <h2>
                {confirm === "backup"
                  ? "Your backup"
                  : confirm === "import"
                    ? "Restore your backup"
                    : confirm === "delete"
                      ? "Delete this transaction?"
                      : confirm === "reset"
                        ? "Start fresh?"
                        : "Replace your data?"}
              </h2>
              <button aria-label="Close" onClick={() => setConfirm("")}>
                <X />
              </button>
            </div>
            {confirm === "backup" ? (
              <>
                <p>Copy all of this text and save it as a backup.</p>
                <textarea
                  readOnly
                  value={backup}
                  rows={8}
                  onFocus={(e) => e.target.select()}
                />
              </>
            ) : confirm === "import" ? (
              <form
                onSubmit={(e) => {
                  e.preventDefault();
                  void importText(backup);
                }}
              >
                <p>
                  Paste a Verdant backup. Restoring replaces all current data.
                </p>
                <textarea
                  required
                  rows={7}
                  placeholder="Paste backup JSON"
                  value={backup}
                  onChange={(e) => setBackup(e.target.value)}
                />
                <button className="primary">
                  Restore backup <Upload size={18} />
                </button>
              </form>
            ) : (
              <>
                <p>
                  {confirm === "delete"
                    ? "This entry will be permanently removed."
                    : confirm === "reset"
                      ? "All transactions, budgets, and preferences will be removed. Export a backup first if you want to keep them."
                      : "The backup will replace your current transactions and budgets."}
                </p>
                <div className="confirm-actions">
                  <button onClick={() => setConfirm("")}>Cancel</button>
                  <button
                    className="primary"
                    onClick={async () => {
                      if (confirm === "restore") await importText(backup);
                      else if (confirm === "delete") {
                        if (
                          await save({
                            ...data,
                            transactions: data.transactions.filter(
                              (t) => t.id !== editing?.id,
                            ),
                          })
                        ) {
                          setConfirm("");
                          setModal(null);
                          setToast("Transaction deleted");
                        }
                      } else if (await save(emptyData() as Data)) {
                        setConfirm("");
                        setName("");
                        setSample(false);
                        setWelcome(true);
                        setCurrency("USD");
                        setTab("Home");
                      }
                    }}
                  >
                    {confirm === "delete"
                      ? "Delete"
                      : confirm === "reset"
                        ? "Reset all data"
                        : "Restore backup"}
                  </button>
                </div>
              </>
            )}
          </section>
        </div>
      )}
      {error && (
        <div className="error-banner" role="alert">
          <span>{error}</span>
          <button aria-label="Dismiss error" onClick={() => setError("")}>
            <X size={18} />
          </button>
        </div>
      )}
      {toast && (
        <div className="toast" role="status">
          <Check size={17} />
          {toast}
        </div>
      )}
    </div>
  );
}
function TransactionForm({
  transaction,
  initialType,
  month,
  onSave,
  onDelete,
}: {
  transaction: Transaction | null;
  initialType: "income" | "expense";
  month: string;
  onSave: (t: Transaction) => Promise<void>;
  onDelete: () => void;
}) {
  const [type, setType] = useState(transaction?.type || initialType),
    [err, setErr] = useState(""),
    [busy, setBusy] = useState(false);
  return (
    <form
      onSubmit={async (e) => {
        e.preventDefault();
        setErr("");
        const f = new FormData(e.currentTarget);
        try {
          setBusy(true);
          const t = {
            id: transaction?.id || crypto.randomUUID(),
            title: String(f.get("title")).trim(),
            amount: cents(f.get("amount")),
            type,
            category: String(f.get("category")),
            date: String(f.get("date")),
          };
          await onSave(t);
        } catch (e) {
          setErr(String(e));
        } finally {
          setBusy(false);
        }
      }}
    >
      <div className="type-toggle">
        <button
          type="button"
          className={type === "expense" ? "selected" : ""}
          onClick={() => setType("expense")}
        >
          <ArrowUpRight size={17} />
          Expense
        </button>
        <button
          type="button"
          className={type === "income" ? "selected" : ""}
          onClick={() => setType("income")}
        >
          <ArrowDownLeft size={17} />
          Income
        </button>
      </div>
      <label>
        Amount
        <input
          autoFocus
          name="amount"
          className="amount-input"
          inputMode="decimal"
          placeholder="0.00"
          defaultValue={transaction ? String(transaction.amount / 100) : ""}
          required
        />
      </label>
      <label>
        What was it for?
        <input
          name="title"
          maxLength={100}
          placeholder={
            type === "expense" ? "e.g. Weekly groceries" : "e.g. Monthly salary"
          }
          defaultValue={transaction?.title}
          required
        />
      </label>
      <div className="form-grid">
        <label>
          Category
          <select
            aria-label="Category"
            name="category"
            defaultValue={transaction?.category || "Other"}
          >
            {categories.map((c) => (
              <option key={c}>{c}</option>
            ))}
          </select>
        </label>
        <label>
          Date
          <input
            type="date"
            name="date"
            defaultValue={
              transaction?.date ||
              (month === monthNow() ? localDate() : month + "-01")
            }
            required
          />
        </label>
      </div>
      {err && (
        <p role="alert" className="danger">
          {err}
        </p>
      )}
      <button className="primary" disabled={busy}>
        {busy ? "Saving…" : transaction ? "Save changes" : "Add transaction"}{" "}
        <Check size={18} />
      </button>
      {transaction && (
        <button className="delete-button" type="button" onClick={onDelete}>
          <Trash2 size={16} />
          Delete transaction
        </button>
      )}
    </form>
  );
}
function BudgetForm({
  limits,
  month,
  onSave,
}: {
  limits: Record<string, number>;
  month: string;
  onSave: (v: Record<string, number>) => Promise<void>;
}) {
  const [err, setErr] = useState(""),
    [busy, setBusy] = useState(false);
  return (
    <form
      onSubmit={async (e) => {
        e.preventDefault();
        setErr("");
        try {
          const f = new FormData(e.currentTarget),
            out: Record<string, number> = {};
          for (const c of categories) {
            const v = String(f.get(c)).trim();
            if (v && Number(v) !== 0) out[c] = cents(v);
          }
          setBusy(true);
          await onSave(out);
        } catch (e) {
          setErr(String(e));
        } finally {
          setBusy(false);
        }
      }}
    >
      <p className="muted">
        Monthly limits for {month}. Leave blank to remove a limit.
      </p>
      <div className="budget-inputs">
        {categories.map((c) => (
          <label key={c}>
            <span>{c}</span>
            <input
              aria-label={c + " budget"}
              name={c}
              inputMode="decimal"
              placeholder="No limit"
              defaultValue={limits[c] ? String(limits[c] / 100) : ""}
            />
          </label>
        ))}
      </div>
      {err && <p role="alert">{err}</p>}
      <button className="primary" disabled={busy}>
        {busy ? "Saving…" : "Save budgets"}
        <Check size={18} />
      </button>
    </form>
  );
}
function SpendingChart({
  transactions,
  money,
  month,
}: {
  transactions: Transaction[];
  money: (n: number) => string;
  month: string;
}) {
  const days = new Date(
      Number(month.slice(0, 4)),
      Number(month.slice(5)),
      0,
    ).getDate(),
    values = Array.from({ length: days }, (_, i) =>
      transactions
        .filter((t) => Number(t.date.slice(-2)) === i + 1)
        .reduce((s, t) => s + t.amount, 0),
    ),
    max = Math.max(...values, 100),
    points = values
      .map((v, i) => `${(i / (days - 1)) * 300},${130 - (v / max) * 100}`)
      .join(" ");
  return (
    <div className="chart">
      <svg
        viewBox="0 0 300 160"
        role="img"
        aria-label={`Daily spending for ${month}. Highest daily expense: ${money(Math.max(...values))}.`}
      >
        <defs>
          <linearGradient id="chartFill" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#a6dc79" stopOpacity=".24" />
            <stop offset="100%" stopColor="#a6dc79" stopOpacity="0" />
          </linearGradient>
        </defs>
        {[30, 80, 130].map((y) => (
          <line
            key={y}
            x1="0"
            x2="300"
            y1={y}
            y2={y}
            stroke="#ffffff14"
            strokeDasharray="2 4"
          />
        ))}
        <polygon points={`0,140 ${points} 300,140`} fill="url(#chartFill)" />
        <path
          d={smoothPath(values, 300, 100, 130, max)}
          fill="none"
          stroke="#279a37"
          strokeWidth="1.5"
        />
        {[0, 7, 14, 21, days - 1].map((i) => (
          <text
            key={i}
            x={Math.min((i / (days - 1)) * 300, 286)}
            y="157"
            fill="#8b9487"
            fontSize="10"
          >
            {i + 1}
          </text>
        ))}
      </svg>
      <span className="chart-caption">Daily expenses · {month}</span>
    </div>
  );
}
function smoothPath(
  values: number[],
  width: number,
  height: number,
  bottom: number,
  max: number,
) {
  const p = values.map((v, i) => [
    (i / (values.length - 1)) * width,
    bottom - (v / max) * height,
  ]);
  return p.reduce((path, point, i) => {
    if (!i) return `M ${point[0]},${point[1]}`;
    const previous = p[i - 1],
      before = p[Math.max(0, i - 2)],
      after = p[Math.min(p.length - 1, i + 1)];
    return (
      path +
      ` C ${previous[0] + (point[0] - before[0]) / 6},${previous[1] + (point[1] - before[1]) / 6} ${point[0] - (after[0] - previous[0]) / 6},${point[1] - (after[1] - previous[1]) / 6} ${point[0]},${point[1]}`
    );
  }, "");
}
function MiniChart({
  transactions,
  month,
}: {
  transactions: Transaction[];
  month: string;
}) {
  const days = new Date(
    Number(month.slice(0, 4)),
    Number(month.slice(5)),
    0,
  ).getDate();
  const values = Array.from({ length: 6 }, (_, i) =>
    transactions
      .filter(
        (t) =>
          Math.min(
            5,
            Math.floor(((Number(t.date.slice(-2)) - 1) / days) * 6),
          ) === i,
      )
      .reduce((s, t) => s + t.amount, 0),
  );
  const path = smoothPath(values, 180, 65, 85, Math.max(...values, 1));
  return (
    <svg viewBox="0 0 180 100" aria-hidden="true">
      <path d={path + " L180,100 L0,100 Z"} fill="currentColor" opacity=".09" />
      <path d={path} fill="none" stroke="currentColor" strokeWidth="1.5" />
    </svg>
  );
}
createRoot(document.getElementById("root")!).render(<App />);
import "./reference.css";
