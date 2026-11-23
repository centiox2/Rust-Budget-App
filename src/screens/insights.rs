//! Spending over time: range switcher, weekly bars, and where the money went.

use egui::{Align2, Color32, Pos2, Rect, Vec2};

use crate::app::{inset, App};
use crate::icons::{self, Icon};
use crate::model::money;
use crate::theme::*;
use crate::time;
use crate::widgets::*;

pub fn show(app: &mut App, ui: &mut egui::Ui) {
    let currency = app.data.currency.clone();
    let now = time::now();
    let width = ui.available_width();
    let top = ui.cursor().min.y;

    header_gradient(
        ui,
        Rect::from_min_max(Pos2::new(0.0, top - 8.0), Pos2::new(width, top + 520.0)),
    );
    ui.add_space(inset() + 4.0);

    // ---- Title -----------------------------------------------------------
    let row = band(ui, 46.0);
    ui.painter().text(
        Pos2::new(row.left() + PAD, row.center().y),
        Align2::LEFT_CENTER,
        "Insights",
        heavy(30.0),
        TEXT,
    );
    let more = Rect::from_center_size(
        Pos2::new(row.right() - PAD - 21.0, row.center().y),
        Vec2::splat(42.0),
    );
    if circle_button(ui, more, GLASS, Icon::More, TEXT, 0.4, 2.0).clicked() {
        app.notify("Nothing to fine-tune yet");
    }

    ui.add_space(18.0);

    // ---- Range -----------------------------------------------------------
    let seg = band(ui, 46.0);
    let seg_rect = Rect::from_min_max(
        Pos2::new(seg.left() + PAD, seg.top()),
        Pos2::new(seg.right() - PAD, seg.bottom()),
    );
    if let Some(index) = segmented(ui, seg_rect, &["Daily", "Weekly", "Monthly"], app.range) {
        app.range = index;
    }

    ui.add_space(22.0);

    let (start, end) = match app.range {
        0 => (time::start_of_day(now), time::start_of_day(now) + 86_400),
        1 => (time::start_of_week(now), now + 1),
        _ => (time::start_of_month(now), now + 1),
    };
    let spent = app.data.spent_between(start, end);
    let period = match app.range {
        0 | 1 => "week",
        _ => "month",
    };
    let delta = match app.range {
        0 | 1 => app.data.week_delta(),
        _ => app.data.month_delta(),
    };

    let label = band(ui, 14.0);
    tracked(
        ui,
        Pos2::new(label.left() + PAD, label.center().y),
        "TOTAL SPENT",
        semibold(11.0),
        DIM,
        1.2,
    );
    ui.add_space(8.0);
    let hero = band(ui, 52.0);
    ui.painter().text(
        Pos2::new(hero.left() + PAD, hero.center().y),
        Align2::LEFT_CENTER,
        money(spent, &currency),
        heavy(40.0),
        TEXT,
    );

    ui.add_space(12.0);
    let delta_text = if delta >= 0 {
        format!("{} less than last {period}", money(delta, &currency))
    } else {
        format!("{} more than last {period}", money(-delta, &currency))
    };
    let galley = tracked_galley(ui, &delta_text, medium(12.5), TEXT, 0.0);
    let size = Vec2::new(14.0 + 18.0 + 9.0 + galley.size().x + 16.0, 30.0);
    let cursor = ui.cursor().min;
    let chip = Rect::from_min_size(Pos2::new(cursor.x + PAD, cursor.y), size);
    ui.painter()
        .rect_filled(chip, chip.height() / 2.0, Color32::from_black_alpha(105));
    icons::paint_centered(
        ui.painter(),
        Pos2::new(chip.left() + 23.0, chip.center().y),
        15.0,
        Icon::TrendUp,
        if delta >= 0 { LIME } else { CORAL },
        1.9,
    );
    ui.painter().galley(
        Pos2::new(chip.left() + 41.0, chip.center().y - galley.size().y / 2.0),
        galley,
        TEXT,
    );
    band(ui, size.y);

    ui.add_space(24.0);

    // ---- This week's bars -------------------------------------------------
    let bars = app.data.week_bars(now);
    let today = time::weekday_index(now);
    let outer = band(ui, 234.0);
    let rect = Rect::from_min_max(
        Pos2::new(outer.left() + PAD, outer.top()),
        Pos2::new(outer.right() - PAD, outer.bottom()),
    );
    ui.painter().rect_filled(rect, 28.0, CARD);
    ui.painter().text(
        Pos2::new(rect.left() + 20.0, rect.top() + 30.0),
        Align2::LEFT_CENTER,
        "This week",
        display(16.5),
        TEXT,
    );
    let badge_rect =
        Rect::from_center_size(Pos2::new(rect.right() - 36.0, rect.top() + 30.0), Vec2::splat(34.0));
    ui.painter().circle_filled(badge_rect.center(), 17.0, CARD_SOFT);
    ui.painter().text(
        badge_rect.center(),
        Align2::CENTER_CENTER,
        time::weekday_letter(today),
        semibold(12.0),
        LIME,
    );

    let area = Rect::from_min_max(
        Pos2::new(rect.left() + 20.0, rect.top() + 62.0),
        Pos2::new(rect.right() - 20.0, rect.bottom() - 36.0),
    );
    let max = bars.iter().copied().max().unwrap_or(0).max(1) as f32;
    let slot = area.width() / 7.0;
    for (index, value) in bars.iter().enumerate() {
        let share = *value as f32 / max;
        let height = (area.height() * share).max(9.0);
        let x = area.left() + slot * (index as f32 + 0.5);
        let bar = Rect::from_center_size(
            Pos2::new(x, area.bottom() - height / 2.0),
            Vec2::new(17.0, height),
        );
        let is_today = index == today;
        ui.painter().rect_filled(
            bar,
            8.5,
            if is_today {
                LIME
            } else {
                Color32::from_white_alpha(40)
            },
        );
        ui.painter().text(
            Pos2::new(x, area.bottom() + 18.0),
            Align2::CENTER_CENTER,
            time::weekday_letter(index),
            semibold(11.5),
            if is_today { LIME } else { DIM },
        );
    }

    ui.add_space(26.0);

    // ---- Where it went ----------------------------------------------------
    let breakdown = app.data.breakdown(start, end);
    let row = band(ui, 32.0);
    ui.painter().text(
        Pos2::new(row.left() + PAD, row.center().y),
        Align2::LEFT_CENTER,
        "Where it went",
        heavy(22.0),
        TEXT,
    );
    ui.painter().text(
        Pos2::new(row.right() - PAD, row.center().y),
        Align2::RIGHT_CENTER,
        format!(
            "{} {}",
            breakdown.len(),
            if breakdown.len() == 1 {
                "category"
            } else {
                "categories"
            }
        ),
        medium(12.5),
        DIM,
    );

    ui.add_space(14.0);
    let total_spent = spent.max(1) as f64;
    for (category_id, cents) in breakdown.iter().take(4) {
        let Some(category) = app.data.category(*category_id) else {
            continue;
        };
        let color = rgb(category.color);
        let share = *cents as f64 / total_spent;
        let merchants = app.data.merchants(*category_id, start, end, 3);

        let outer = band(ui, 56.0);
        let circle = Rect::from_center_size(
            Pos2::new(outer.left() + PAD + 21.0, outer.center().y),
            Vec2::splat(42.0),
        );
        ui.painter()
            .circle_filled(circle.center(), 21.0, color.gamma_multiply(0.18));
        ui.painter().text(
            circle.center(),
            Align2::CENTER_CENTER,
            format!("{}%", (share * 100.0).round() as i64),
            semibold(11.5),
            color,
        );

        let text_x = circle.right() + 15.0;
        let painter = ui.painter();
        painter.text(
            Pos2::new(text_x, outer.center().y - 9.0),
            Align2::LEFT_CENTER,
            &category.name,
            semibold(16.0),
            TEXT,
        );
        let subtitle = if merchants.is_empty() {
            "No merchants recorded".to_owned()
        } else {
            merchants.join(", ")
        };
        painter.text(
            Pos2::new(text_x, outer.center().y + 12.0),
            Align2::LEFT_CENTER,
            subtitle,
            medium(12.0),
            DIM,
        );
        painter.text(
            Pos2::new(outer.right() - PAD, outer.center().y),
            Align2::RIGHT_CENTER,
            money(*cents, &currency),
            semibold(14.5),
            TEXT,
        );
        ui.add_space(2.0);
    }

    ui.add_space(16.0);

    // ---- Encouragement ----------------------------------------------------
    let banner = band(ui, 78.0);
    let banner_rect = Rect::from_min_max(
        Pos2::new(banner.left() + PAD, banner.top()),
        Pos2::new(banner.right() - PAD, banner.bottom()),
    );
    ui.painter()
        .rect_filled(banner_rect, 24.0, Color32::from_rgb(0x1C, 0x22, 0x0E));
    ui.painter().rect_stroke(
        banner_rect,
        24.0,
        egui::Stroke::new(1.0, LIME.gamma_multiply(0.22)),
        egui::StrokeKind::Inside,
    );
    let icon_centre = Pos2::new(banner_rect.left() + 36.0, banner_rect.center().y);
    ui.painter()
        .circle_filled(icon_centre, 18.0, LIME.gamma_multiply(0.14));
    icons::paint_centered(ui.painter(), icon_centre, 18.0, Icon::Sparkle, LIME, 1.7);

    let message = if delta >= 0 {
        format!(
            "You spent {} less than last {period}. Keep it up.",
            money(delta, &currency)
        )
    } else {
        format!(
            "You spent {} more than last {period}. Trim one envelope.",
            money(-delta, &currency)
        )
    };
    wrapped(
        ui,
        Rect::from_min_max(
            Pos2::new(icon_centre.x + 30.0, banner_rect.top() + 20.0),
            Pos2::new(banner_rect.right() - 16.0, banner_rect.bottom() - 16.0),
        ),
        &message,
        medium(13.0),
        TEXT,
    );
}
