use serde::{Deserialize, Serialize};
use std::collections::{HashMap, HashSet};

const CATEGORIES: [&str; 8] = [
    "Groceries",
    "Dining",
    "Transport",
    "Shopping",
    "Utilities",
    "Health",
    "Entertainment",
    "Other",
];
const MAX_AMOUNT: u64 = 100_000_000_000;

#[derive(Debug, Serialize, Deserialize)]
pub struct Transaction {
    id: String,
    title: String,
    amount: u64,
    #[serde(rename = "type")]
    kind: String,
    category: String,
    date: String,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct BudgetData {
    version: u32,
    name: String,
    currency: String,
    onboarded: bool,
    transactions: Vec<Transaction>,
    budgets: HashMap<String, HashMap<String, u64>>,
}

fn valid_month(value: &str) -> bool {
    value.len() == 7
        && value.as_bytes()[4] == b'-'
        && value[..4].bytes().all(|c| c.is_ascii_digit())
        && value[5..]
            .parse::<u32>()
            .is_ok_and(|m| (1..=12).contains(&m))
}

fn valid_date(value: &str) -> bool {
    if !value.is_ascii()
        || value.len() != 10
        || value.as_bytes()[7] != b'-'
        || !valid_month(&value[..7])
    {
        return false;
    }
    let year = value[..4].parse::<u32>().unwrap_or(0);
    let month = value[5..7].parse::<usize>().unwrap_or(0);
    let days = [
        31,
        if year % 4 == 0 && (year % 100 != 0 || year % 400 == 0) {
            29
        } else {
            28
        },
        31,
        30,
        31,
        30,
        31,
        31,
        30,
        31,
        30,
        31,
    ];
    value[8..]
        .parse::<u32>()
        .is_ok_and(|d| d > 0 && d <= days[month - 1])
}

impl BudgetData {
    pub fn validate(&self) -> Result<(), String> {
        if self.version != 1
            || self.name.chars().count() > 50
            || !["USD", "KES", "EUR", "GBP", "INR"].contains(&self.currency.as_str())
            || self.transactions.len() > 10_000
        {
            return Err("Invalid budget data or unsupported version".into());
        }
        let mut ids = HashSet::new();
        for t in &self.transactions {
            if t.id.is_empty()
                || !ids.insert(&t.id)
                || t.title.trim().is_empty()
                || t.title.chars().count() > 100
                || t.amount == 0
                || t.amount > MAX_AMOUNT
                || !["income", "expense"].contains(&t.kind.as_str())
                || !CATEGORIES.contains(&t.category.as_str())
                || !valid_date(&t.date)
            {
                return Err("Invalid transaction".into());
            }
        }
        for (month, limits) in &self.budgets {
            if !month.is_ascii() || !valid_month(month) {
                return Err("Invalid budget month".into());
            }
            for (category, amount) in limits {
                if !CATEGORIES.contains(&category.as_str()) || *amount == 0 || *amount > MAX_AMOUNT
                {
                    return Err("Invalid category limit".into());
                }
            }
        }
        Ok(())
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    #[test]
    fn calendar_dates_are_checked() {
        assert!(valid_date("2024-02-29"));
        for date in [
            "2026-02-29",
            "2026-04-31",
            "2026-00-01",
            "2026-12-00",
            "invalid",
            "ééééé",
        ] {
            assert!(!valid_date(date));
        }
    }
    #[test]
    fn roundtrip_and_reject_invalid_money() {
        let value = serde_json::json!({"version":1,"name":"Alex","currency":"KES","onboarded":true,"transactions":[{"id":"one","title":"Market","amount":299,"type":"expense","category":"Groceries","date":"2026-11-23"}],"budgets":{"2026-11":{"Groceries":5000}}});
        let mut data: BudgetData = serde_json::from_value(value).unwrap();
        assert!(data.validate().is_ok());
        let serialized = serde_json::to_string(&data).unwrap();
        assert!(serde_json::from_str::<BudgetData>(&serialized)
            .unwrap()
            .validate()
            .is_ok());
        data.transactions[0].amount = 0;
        assert!(data.validate().is_err());
    }
}
