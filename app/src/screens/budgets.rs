//! Envelopes: how much of each category budget has been used this month.

use egui::{Align2, Color32, Pos2, Rect, Vec2};

use crate::app::{inset, App};
use crate::icons::{self, Icon};
use crate::model::{money, Category};
use crate::screens::Screen;
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
        Rect::from_min_max(Pos2::new(0.0, top - 8.0), Pos2::new(width, top + 560.0)),
    );

    ui.add_space(inset() + 2.0);

    // ---- Title bar -------------------------------------------------------
    let row = band(ui, 46.0);
    let back = Rect::from_center_size(
        Pos2::new(row.left() + PAD + 21.0, row.center().y),
        Vec2::splat(42.0),
    );
    if circle_button(ui, back, GLASS, Icon::Back, TEXT, 0.4, 2.0).clicked() {
        app.screen = Screen::Home;
    }
    ui.painter().text(
        row.center(),
        Align2::CENTER_CENTER,
        "Budgets",
        semibold(17.0),
        TEXT,
    );
    let more = Rect::from_center_size(
        Pos2::new(row.right() - PAD - 21.0, row.center().y),
        Vec2::splat(42.0),
    );
    if circle_button(ui, more, GLASS, Icon::More, TEXT, 0.4, 2.0).clicked() {
        app.notify("Envelope settings are on the Profile tab");
    }

    ui.add_space(26.0);

    // ---- Month total -----------------------------------------------------
    let label = band(ui, 14.0);
    tracked(
        ui,
        Pos2::new(label.left() + PAD, label.center().y),
        &time::month_name(now).to_uppercase(),
        semibold(11.0),
        DIM,
        1.6,
    );

    ui.add_space(8.0);
    let spent = app.data.spent_this_month();
    let total_budget = app.data.budget_total();
    let hero = band(ui, 52.0);
    let spent_galley = ui
        .painter()
        .layout_no_wrap(money(spent, &currency), heavy(36.0), TEXT);
    ui.painter().galley(
        Pos2::new(
            hero.left() + PAD,
            hero.center().y - spent_galley.size().y / 2.0,
        ),
        spent_galley.clone(),
        TEXT,
    );
    ui.painter().text(
        Pos2::new(
            hero.left() + PAD + spent_galley.size().x + 10.0,
            hero.center().y + 6.0,
        ),
        Align2::LEFT_CENTER,
        format!("of {}", money(total_budget, &currency)),
        heavy(24.0),
        Color32::from_white_alpha(115),
    );

    ui.add_space(6.0);
    let frac = if total_budget > 0 {
        spent as f64 / total_budget as f64
    } else {
        0.0
    };
    let sub = band(ui, 20.0);
    ui.painter().text(
        Pos2::new(sub.left() + PAD, sub.center().y),
        Align2::LEFT_CENTER,
        format!(
            "{}% spent · {} days left",
            (frac * 100.0).round() as i64,
            time::days_left_in_month(now)
        ),
        medium(13.0),
        DIM,
    );

    ui.add_space(28.0);

    // ---- Categories ------------------------------------------------------
    let row = band(ui, 46.0);
    ui.painter().text(
        Pos2::new(row.left() + PAD, row.center().y),
        Align2::LEFT_CENTER,
        "Categories",
        heavy(22.0),
        TEXT,
    );
    let plus = Rect::from_center_size(
        Pos2::new(row.right() - PAD - 22.0, row.center().y),
        Vec2::splat(44.0),
    );
    if hit(ui, plus, "new_category").clicked() {
        app.open_category();
    }
    ui.painter().circle_filled(plus.center(), 22.0, LIME);
    icons::paint_centered(ui.painter(), plus.center(), 20.0, Icon::Plus, INK, 2.4);

    ui.add_space(16.0);

    let month_start = time::start_of_month(now);
    let month_end = time::start_of_next_month(now);
    let categories: Vec<Category> = app.data.categories.clone();
    for category in &categories {
        let spent = app.data.spent_for(category.id, month_start, month_end);
        category_card(ui, category, spent, &currency);
        ui.add_space(12.0);
    }

    ui.add_space(6.0);
    let outer = band(ui, 58.0);
    let rect = Rect::from_min_max(
        Pos2::new(outer.left() + PAD, outer.top()),
        Pos2::new(outer.right() - PAD, outer.bottom()),
    );
    if outline_button(ui, rect, "New budget", Some(Icon::Plus)).clicked() {
        app.open_category();
    }
}

fn category_card(ui: &mut egui::Ui, category: &Category, spent: i64, currency: &str) {
    let outer = band(ui, 102.0);
    let rect = Rect::from_min_max(
        Pos2::new(outer.left() + PAD, outer.top()),
        Pos2::new(outer.right() - PAD, outer.bottom()),
    );
    ui.painter().rect_filled(rect, 26.0, CARD);

    let color = rgb(category.color);
    let tile = Rect::from_min_size(Pos2::new(rect.left() + 16.0, rect.top() + 16.0), Vec2::splat(46.0));
    icon_tile(ui, tile, color, Icon::from_key(&category.icon));

    let text_x = tile.right() + 15.0;
    let frac = category.percent_used(spent);
    let tone = if frac > 1.0 { CORAL } else { color };

    let painter = ui.painter();
    painter.text(
        Pos2::new(text_x, rect.top() + 33.0),
        Align2::LEFT_CENTER,
        &category.name,
        semibold(16.5),
        TEXT,
    );
    painter.text(
        Pos2::new(text_x, rect.top() + 56.0),
        Align2::LEFT_CENTER,
        format!(
            "{} of {}",
            money(spent, currency),
            money(category.budget_cents, currency)
        ),
        medium(13.0),
        DIM,
    );
    painter.text(
        Pos2::new(rect.right() - 18.0, rect.top() + 33.0),
        Align2::RIGHT_CENTER,
        format!("{}%", (frac * 100.0).round() as i64),
        semibold(14.5),
        tone,
    );

    let bar = Rect::from_min_size(
        Pos2::new(text_x, rect.bottom() - 24.0),
        Vec2::new(rect.right() - 18.0 - text_x, 8.0),
    );
    progress(ui, bar, frac, tone);
}
