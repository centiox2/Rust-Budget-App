//! The demo workspace the app starts with: eight envelopes and a few weeks of
//! spending, so every screen has something real to show on first launch.

use chrono::{Duration, Local, TimeZone};

use crate::model::{Category, Data, Entry};

struct Draft {
    /// Days before today.
    days_ago: i64,
    hour: u32,
    minute: u32,
    category: &'static str,
    cents: i64,
    note: &'static str,
}

/// (name, budget in cents, colour, icon key)
const CATEGORIES: [(&str, i64, [u8; 3], &str); 8] = [
    ("Groceries", 28_000, [0x7C, 0x6C, 0xFF], "cart"),
    ("Bills", 30_000, [0xFF, 0x73, 0x63], "bolt"),
    ("Eating out", 12_000, [0x4F, 0xDD, 0xA5], "cup"),
    ("Transport", 11_000, [0x54, 0xAF, 0xF7], "pin"),
    ("Rent", 88_000, [0xB4, 0x9B, 0xFF], "wallet"),
    ("Fun", 10_000, [0xCB, 0xF9, 0x5B], "gift"),
    ("Shopping", 24_000, [0xFF, 0x9F, 0x6B], "tag"),
    ("Subscriptions", 15_000, [0x8B, 0x7B, 0xFF], "chart"),
];

const ENTRIES: &[Draft] = &[
    // --- this month ---
    Draft { days_ago: 0, hour: 8, minute: 24, category: "Groceries", cents: 3_420, note: "Whole Foods" },
    Draft { days_ago: 1, hour: 9, minute: 12, category: "Eating out", cents: 540, note: "Blue Bottle" },
    Draft { days_ago: 1, hour: 18, minute: 5, category: "Transport", cents: 280, note: "TfL travel" },
    Draft { days_ago: 2, hour: 17, minute: 40, category: "Groceries", cents: 4_200, note: "Tesco" },
    Draft { days_ago: 3, hour: 19, minute: 30, category: "Eating out", cents: 2_600, note: "Dishoom" },
    Draft { days_ago: 4, hour: 7, minute: 55, category: "Transport", cents: 1_500, note: "TfL travel" },
    Draft { days_ago: 5, hour: 20, minute: 15, category: "Fun", cents: 4_200, note: "Odeon" },
    Draft { days_ago: 6, hour: 12, minute: 10, category: "Groceries", cents: 5_600, note: "Sainsbury's" },
    Draft { days_ago: 7, hour: 13, minute: 5, category: "Eating out", cents: 1_800, note: "Pret" },
    Draft { days_ago: 8, hour: 8, minute: 0, category: "Transport", cents: 2_670, note: "TfL travel" },
    Draft { days_ago: 9, hour: 9, minute: 30, category: "Bills", cents: 8_500, note: "Vodafone" },
    Draft { days_ago: 9, hour: 21, minute: 0, category: "Eating out", cents: 1_200, note: "Uber Eats" },
    Draft { days_ago: 10, hour: 15, minute: 20, category: "Shopping", cents: 9_800, note: "Amazon" },
    Draft { days_ago: 11, hour: 11, minute: 45, category: "Groceries", cents: 4_330, note: "Sainsbury's" },
    Draft { days_ago: 12, hour: 6, minute: 0, category: "Bills", cents: 12_000, note: "Octopus Energy" },
    Draft { days_ago: 13, hour: 19, minute: 0, category: "Fun", cents: 4_700, note: "Tate Modern" },
    Draft { days_ago: 14, hour: 7, minute: 0, category: "Bills", cents: 11_500, note: "Sky Broadband" },
    Draft { days_ago: 15, hour: 22, minute: 10, category: "Eating out", cents: 3_460, note: "Dishoom" },
    Draft { days_ago: 16, hour: 16, minute: 30, category: "Shopping", cents: 2_200, note: "IKEA" },
    Draft { days_ago: 18, hour: 6, minute: 0, category: "Subscriptions", cents: 8_000, note: "Netflix + Spotify" },
    Draft { days_ago: 19, hour: 10, minute: 15, category: "Groceries", cents: 3_100, note: "Waitrose" },
    // --- last month ---
    Draft { days_ago: 24, hour: 10, minute: 0, category: "Groceries", cents: 8_200, note: "Waitrose" },
    Draft { days_ago: 26, hour: 18, minute: 0, category: "Eating out", cents: 2_400, note: "Pret" },
    Draft { days_ago: 28, hour: 6, minute: 0, category: "Bills", cents: 12_000, note: "Octopus Energy" },
    Draft { days_ago: 30, hour: 20, minute: 0, category: "Fun", cents: 3_600, note: "Odeon" },
    Draft { days_ago: 31, hour: 12, minute: 0, category: "Shopping", cents: 6_400, note: "Amazon" },
    Draft { days_ago: 33, hour: 6, minute: 0, category: "Subscriptions", cents: 8_000, note: "Netflix + Spotify" },
    Draft { days_ago: 34, hour: 9, minute: 0, category: "Transport", cents: 1_200, note: "TfL travel" },
    Draft { days_ago: 36, hour: 19, minute: 0, category: "Eating out", cents: 3_100, note: "Dishoom" },
    Draft { days_ago: 38, hour: 6, minute: 0, category: "Bills", cents: 8_500, note: "Vodafone" },
    Draft { days_ago: 40, hour: 11, minute: 0, category: "Groceries", cents: 6_800, note: "Tesco" },
];

fn timestamp(days_ago: i64, hour: u32, minute: u32) -> i64 {
    let date = Local::now().date_naive() - Duration::days(days_ago);
    let naive = match date.and_hms_opt(hour, minute, 0) {
        Some(at) => at,
        None => return crate::time::now(),
    };
    Local
        .from_local_datetime(&naive)
        .single()
        .map(|at| at.timestamp())
        .unwrap_or_else(crate::time::now)
}

/// A brand-new workspace.
pub fn demo() -> Data {
    let mut data = Data {
        name: "Amara".to_owned(),
        currency: "£".to_owned(),
        categories: CATEGORIES
            .iter()
            .enumerate()
            .map(|(i, (name, budget, color, icon))| Category {
                id: i as u64 + 1,
                name: (*name).to_owned(),
                budget_cents: *budget,
                color: *color,
                icon: (*icon).to_owned(),
            })
            .collect(),
        entries: Vec::new(),
        next_id: CATEGORIES.len() as u64 + 1,
    };

    for (i, draft) in ENTRIES.iter().enumerate() {
        let category_id = data
            .categories
            .iter()
            .find(|c| c.name == draft.category)
            .map(|c| c.id)
            .unwrap_or(1);
        data.entries.push(Entry {
            id: i as u64 + 1,
            category_id,
            amount_cents: draft.cents,
            note: draft.note.to_owned(),
            at: timestamp(draft.days_ago, draft.hour, draft.minute),
        });
    }
    data.next_id += ENTRIES.len() as u64;
    data
}
