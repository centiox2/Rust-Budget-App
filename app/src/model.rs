//! The whole data model: envelopes, spendings and the arithmetic over them.

use serde::{Deserialize, Serialize};

use crate::time;

/// A spending envelope, e.g. "Groceries, £280 a month, violet, cart icon".
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Category {
    pub id: u64,
    pub name: String,
    pub budget_cents: i64,
    pub color: [u8; 3],
    pub icon: String,
}

impl Category {
    /// 0.0 when no budget is set.
    pub fn percent_used(&self, spent_cents: i64) -> f64 {
        if self.budget_cents <= 0 {
            0.0
        } else {
            spent_cents as f64 / self.budget_cents as f64
        }
    }
}

/// A single spending.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Entry {
    pub id: u64,
    pub category_id: u64,
    pub amount_cents: i64,
    pub note: String,
    /// Unix seconds.
    pub at: i64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Data {
    pub name: String,
    pub currency: String,
    pub categories: Vec<Category>,
    pub entries: Vec<Entry>,
    pub next_id: u64,
}

impl Data {
    pub fn budget_total(&self) -> i64 {
        self.categories.iter().map(|c| c.budget_cents).sum()
    }

    pub fn category(&self, id: u64) -> Option<&Category> {
        self.categories.iter().find(|c| c.id == id)
    }

    pub fn category_name(&self, id: u64) -> &str {
        self.category(id).map(|c| c.name.as_str()).unwrap_or("Other")
    }

    pub fn spent_between(&self, start: i64, end: i64) -> i64 {
        self.entries
            .iter()
            .filter(|e| e.at >= start && e.at < end)
            .map(|e| e.amount_cents)
            .sum()
    }

    pub fn spent_for(&self, category_id: u64, start: i64, end: i64) -> i64 {
        self.entries
            .iter()
            .filter(|e| e.category_id == category_id && e.at >= start && e.at < end)
            .map(|e| e.amount_cents)
            .sum()
    }

    pub fn spent_this_month(&self) -> i64 {
        let now = time::now();
        self.spent_between(time::start_of_month(now), time::start_of_next_month(now))
    }

    pub fn spent_today(&self) -> i64 {
        let now = time::now();
        self.spent_between(time::start_of_day(now), time::start_of_day(now) + 86_400)
    }

    pub fn spent_this_week(&self) -> i64 {
        let now = time::now();
        self.spent_between(time::start_of_week(now), time::now() + 1)
    }

    /// Money that is still free to spend this month.
    pub fn safe_to_spend(&self) -> i64 {
        self.budget_total() - self.spent_this_month()
    }

    /// Change in spending for the last seven days, against the seven before.
    pub fn week_delta(&self) -> i64 {
        let now = time::now();
        let week_ago = now - 7 * 86_400;
        let recent = self.spent_between(week_ago, now + 1);
        let before = self.spent_between(week_ago - 7 * 86_400, week_ago);
        before - recent
    }

    /// Last month's spending over the same number of elapsed days.
    pub fn last_month_to_date(&self) -> i64 {
        let now = time::now();
        let start = time::start_of_month(now);
        let elapsed = now - start;
        let last_start = time::start_of_month(start - 86_400);
        self.spent_between(last_start, last_start + elapsed)
    }

    pub fn month_delta(&self) -> i64 {
        self.last_month_to_date() - self.spent_this_month()
    }

    /// Spend per weekday (Monday..Sunday) for the week containing `ts`.
    pub fn week_bars(&self, ts: i64) -> [i64; 7] {
        let start = time::start_of_week(ts);
        let mut bars = [0i64; 7];
        for (i, slot) in bars.iter_mut().enumerate() {
            let day = start + i as i64 * 86_400;
            *slot = self.spent_between(day, day + 86_400);
        }
        bars
    }

    /// Categories that have spending in the window, biggest first.
    pub fn breakdown(&self, start: i64, end: i64) -> Vec<(u64, i64)> {
        let mut rows: Vec<(u64, i64)> = self
            .categories
            .iter()
            .map(|c| (c.id, self.spent_for(c.id, start, end)))
            .filter(|(_, cents)| *cents > 0)
            .collect();
        rows.sort_by_key(|(_, cents)| -*cents);
        rows
    }

    /// Merchants in a category, biggest first, for the "where it went" subtitles.
    pub fn merchants(&self, category_id: u64, start: i64, end: i64, max: usize) -> Vec<String> {
        let mut rows: Vec<(String, i64)> = Vec::new();
        for entry in self
            .entries
            .iter()
            .filter(|e| e.category_id == category_id && e.at >= start && e.at < end)
        {
            if entry.note.trim().is_empty() {
                continue;
            }
            match rows.iter_mut().find(|(name, _)| *name == entry.note) {
                Some((_, cents)) => *cents += entry.amount_cents,
                None => rows.push((entry.note.clone(), entry.amount_cents)),
            }
        }
        rows.sort_by_key(|(_, cents)| -*cents);
        rows.into_iter().take(max).map(|(n, _)| n).collect()
    }

    pub fn recent(&self, count: usize) -> Vec<&Entry> {
        let mut rows: Vec<&Entry> = self.entries.iter().collect();
        rows.sort_by_key(|e| -e.at);
        rows.into_iter().take(count).collect()
    }

    pub fn all_sorted(&self) -> Vec<&Entry> {
        let mut rows: Vec<&Entry> = self.entries.iter().collect();
        rows.sort_by_key(|e| -e.at);
        rows
    }

    pub fn add_entry(&mut self, category_id: u64, amount_cents: i64, note: String, at: i64) -> u64 {
        let id = self.next_id;
        self.next_id += 1;
        self.entries.push(Entry {
            id,
            category_id,
            amount_cents,
            note,
            at,
        });
        id
    }

    pub fn add_category(
        &mut self,
        name: String,
        budget_cents: i64,
        color: [u8; 3],
        icon: String,
    ) -> u64 {
        let id = self.next_id;
        self.next_id += 1;
        self.categories.push(Category {
            id,
            name,
            budget_cents,
            color,
            icon,
        });
        id
    }

    pub fn remove_category(&mut self, id: u64) {
        self.categories.retain(|c| c.id != id);
        self.entries.retain(|e| e.category_id != id);
    }

    /// The category the add-expense sheet starts on: the one used most often.
    pub fn most_used_category(&self) -> u64 {
        let mut best = (0u64, 0usize);
        for category in &self.categories {
            let uses = self
                .entries
                .iter()
                .filter(|e| e.category_id == category.id)
                .count();
            if uses > best.1 {
                best = (category.id, uses);
            }
        }
        if best.0 == 0 {
            self.categories.first().map(|c| c.id).unwrap_or(0)
        } else {
            best.0
        }
    }
}

// ---- Money formatting -------------------------------------------------------

fn grouped(whole: i64) -> String {
    let digits = whole.to_string();
    let mut out = String::new();
    for (i, ch) in digits.chars().enumerate() {
        if i > 0 && (digits.len() - i) % 3 == 0 {
            out.push(',');
        }
        out.push(ch);
    }
    out
}

/// `£960` for whole pounds, `£1,240.50` otherwise.
pub fn money(cents: i64, currency: &str) -> String {
    let negative = cents < 0;
    let cents = cents.abs();
    let mut text = grouped(cents / 100);
    if cents % 100 != 0 {
        text.push_str(&format!(".{:02}", cents % 100));
    }
    format!("{}{}{}", if negative { "-" } else { "" }, currency, text)
}

/// Always two decimals, for the amount well and category budgets.
pub fn money_exact(cents: i64, currency: &str) -> String {
    let negative = cents < 0;
    let cents = cents.abs();
    format!(
        "{}{}{}.{:02}",
        if negative { "-" } else { "" },
        currency,
        grouped(cents / 100),
        cents % 100
    )
}

pub fn percent(frac: f64) -> String {
    format!("{}%", (frac * 100.0).round() as i64)
}

/// Parse a typed amount ("34.20", "34,20", "£34.2") into cents.
pub fn parse_money(text: &str) -> i64 {
    let cleaned: String = text
        .chars()
        .filter(|c| c.is_ascii_digit() || *c == '.')
        .collect();
    if cleaned.is_empty() {
        return 0;
    }
    let mut parts = cleaned.split('.');
    let whole: i64 = parts.next().unwrap_or("0").parse().unwrap_or(0);
    let frac_text = parts.next().unwrap_or("");
    let frac: i64 = match frac_text.len() {
        0 => 0,
        1 => frac_text.parse::<i64>().unwrap_or(0) * 10,
        _ => frac_text[..2].parse::<i64>().unwrap_or(0),
    };
    whole * 100 + frac
}

/// Show existing cents back in an editable field.
pub fn amount_text(cents: i64) -> String {
    if cents % 100 == 0 {
        format!("{}", cents / 100)
    } else {
        format!("{}.{:02}", cents / 100, cents % 100)
    }
}

/// Keep a partially typed amount sane: digits and at most one dot, two decimals.
pub fn sanitize_amount(text: &mut String) {
    let mut seen_dot = false;
    let mut decimals = 0;
    text.retain(|c| {
        if c.is_ascii_digit() {
            if seen_dot {
                decimals += 1;
                decimals <= 2
            } else {
                true
            }
        } else if c == '.' && !seen_dot {
            seen_dot = true;
            true
        } else {
            false
        }
    });
}
