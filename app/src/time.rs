//! Local-time helpers. Everything is stored as unix seconds.

use chrono::{DateTime, Datelike, Duration, Local, NaiveDate, TimeZone, Timelike};

pub fn now() -> i64 {
    Local::now().timestamp()
}

pub fn dt(ts: i64) -> DateTime<Local> {
    Local.timestamp_opt(ts, 0).single().unwrap_or_else(Local::now)
}

fn midnight(date: NaiveDate) -> i64 {
    Local
        .from_local_datetime(&date.and_hms_opt(0, 0, 0).unwrap_or_default())
        .single()
        .map(|d| d.timestamp())
        .unwrap_or_else(now)
}

pub fn today() -> NaiveDate {
    Local::now().date_naive()
}

pub fn start_of_day(ts: i64) -> i64 {
    midnight(dt(ts).date_naive())
}

pub fn start_of_week(ts: i64) -> i64 {
    let date = dt(ts).date_naive();
    let back = date.weekday().num_days_from_monday() as i64;
    midnight(date - Duration::days(back))
}

pub fn start_of_month(ts: i64) -> i64 {
    let d = dt(ts);
    midnight(NaiveDate::from_ymd_opt(d.year(), d.month(), 1).unwrap_or_else(today))
}

pub fn start_of_next_month(ts: i64) -> i64 {
    let d = dt(ts);
    let (y, m) = if d.month() == 12 {
        (d.year() + 1, 1)
    } else {
        (d.year(), d.month() + 1)
    };
    midnight(NaiveDate::from_ymd_opt(y, m, 1).unwrap_or_else(today))
}

/// Days remaining in the current month, counting from tomorrow.
pub fn days_left_in_month(ts: i64) -> i64 {
    let end = dt(start_of_next_month(ts)).date_naive();
    (end - dt(ts).date_naive()).num_days().max(0)
}

pub fn month_name(ts: i64) -> String {
    dt(ts).format("%B").to_string()
}

pub fn clock(ts: i64) -> String {
    let d = dt(ts);
    let hour = d.hour() % 12;
    let hour = if hour == 0 { 12 } else { hour };
    let suffix = if d.hour() < 12 { "AM" } else { "PM" };
    format!("{}:{:02} {}", hour, d.minute(), suffix)
}

/// "8:24 AM" today, "Yesterday" yesterday, otherwise "12 Nov".
pub fn day_label(ts: i64) -> String {
    let date = dt(ts).date_naive();
    let today = today();
    if date == today {
        clock(ts)
    } else if date == today - Duration::days(1) {
        "Yesterday".to_owned()
    } else {
        dt(ts).format("%-d %b").to_string()
    }
}

pub fn long_date(ts: i64) -> String {
    dt(ts).format("%-d %B %Y").to_string()
}

/// Year-month-day, used to group a list of spendings by day.
pub fn day_key(ts: i64) -> String {
    dt(ts).format("%Y-%m-%d").to_string()
}

/// Monday = 0 … Sunday = 6.
pub fn weekday_index(ts: i64) -> usize {
    dt(ts).weekday().num_days_from_monday() as usize
}

pub fn weekday_letter(index: usize) -> &'static str {
    ["M", "T", "W", "T", "F", "S", "S"]
        .get(index)
        .copied()
        .unwrap_or("")
}
