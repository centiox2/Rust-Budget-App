//! The overview: safe-to-spend, quick actions, this month's envelope and the
//! three most recent purchases.

use egui::{Align2, Color32, Pos2, Rect, Vec2};

use crate::app::{inset, App};
use crate::icons::{self, Icon};
use crate::model::money;
use crate::screens::Screen;
use crate::theme::*;
use crate::time;
use crate::widgets::*;

pub fn show(app: &mut App, ui: &mut egui::Ui) {
    let currency = app.data.currency.clone();
    let name = app.data.name.clone();
    let now = time::now();
    let width = ui.available_width();
    let top = ui.cursor().min.y;

    header_gradient(
        ui,
        Rect::from_min_max(Pos2::new(0.0, top - 8.0), Pos2::new(width, top + 640.0)),
    );

    ui.add_space(inset() + 6.0);

    // ---- Greeting + circular actions -------------------------------------
    let row = band(ui, 46.0);
    ui.painter().text(
        Pos2::new(row.left() + PAD, row.center().y),
        Align2::LEFT_CENTER,
        format!("Hey {name}"),
        heavy(27.0),
        TEXT,
    );
    let bell = Rect::from_center_size(
        Pos2::new(row.right() - PAD - 21.0, row.center().y),
        Vec2::splat(42.0),
    );
    let search = bell.translate(Vec2::new(-52.0, 0.0));
    if circle_button(ui, search, GLASS, Icon::Search, TEXT, 0.44, 1.9).clicked() {
        app.screen = Screen::Transactions;
    }
    if circle_button(ui, bell, GLASS, Icon::Bell, TEXT, 0.44, 1.9).clicked() {
        app.notify("You're all caught up");
    }

    ui.add_space(22.0);

    // ---- Safe to spend ---------------------------------------------------
    let label = band(ui, 14.0);
    tracked(
        ui,
        Pos2::new(label.left() + PAD, label.center().y),
        "SAFE TO SPEND",
        semibold(11.0),
        DIM,
        1.2,
    );
    ui.add_space(8.0);
    let hero = band(ui, 56.0);
    ui.painter().text(
        Pos2::new(hero.left() + PAD, hero.center().y),
        Align2::LEFT_CENTER,
        money(app.data.safe_to_spend(), &currency),
        heavy(46.0),
        TEXT,
    );

    ui.add_space(14.0);

    // ---- Week-over-week chip --------------------------------------------
    let delta = app.data.week_delta();
    let chip_text = if delta >= 0 {
        format!("{} more than last week", money(delta, &currency))
    } else {
        format!("{} less than last week", money(-delta, &currency))
    };
    let galley = tracked_galley(ui, &chip_text, medium(13.0), TEXT, 0.0);
    let chip_size = Vec2::new(14.0 + 18.0 + 9.0 + galley.size().x + 16.0, 32.0);
    let cursor = ui.cursor().min;
    let chip = Rect::from_min_size(Pos2::new(cursor.x + PAD, cursor.y), chip_size);
    ui.painter()
        .rect_filled(chip, chip.height() / 2.0, Color32::from_black_alpha(105));
    icons::paint_centered(
        ui.painter(),
        Pos2::new(chip.left() + 23.0, chip.center().y),
        16.0,
        Icon::TrendUp,
        LIME,
        1.9,
    );
    ui.painter().galley(
        Pos2::new(chip.left() + 41.0, chip.center().y - galley.size().y / 2.0),
        galley,
        TEXT,
    );
    band(ui, chip_size.y);

    ui.add_space(28.0);

    // ---- Quick actions ---------------------------------------------------
    let actions = band(ui, 88.0);
    let cell = (actions.width() - PAD * 2.0) / 4.0;
    let entries: [(Icon, &str, bool); 4] = [
        (Icon::Plus, "Add", true),
        (Icon::Tag, "Budgets", false),
        (Icon::Wallet, "Accounts", false),
        (Icon::Chart, "Insights", false),
    ];
    for (index, (icon, label, filled)) in entries.iter().enumerate() {
        let centre = Pos2::new(
            actions.left() + PAD + cell * (index as f32 + 0.5),
            actions.top() + 28.0,
        );
        let circle = Rect::from_center_size(centre, Vec2::splat(56.0));
        if *filled {
            if hit(ui, circle, "quick_add").clicked() {
                app.open_expense();
            }
            ui.painter().circle_filled(centre, 28.0, LIME);
            icons::paint_centered(ui.painter(), centre, 24.0, Icon::Plus, INK, 2.4);
        } else if circle_button(ui, circle, GLASS, *icon, TEXT, 0.4, 1.9).clicked() {
            app.screen = match *icon {
                Icon::Tag => Screen::Budgets,
                Icon::Wallet => Screen::Profile,
                _ => Screen::Insights,
            };
        }
        ui.painter().text(
            Pos2::new(centre.x, actions.top() + 70.0),
            Align2::CENTER_CENTER,
            *label,
            semibold(12.5),
            TEXT,
        );
    }

    ui.add_space(26.0);

    // ---- This month's budget --------------------------------------------
    let spent = app.data.spent_this_month();
    let total_budget = app.data.budget_total();
    let days_left = time::days_left_in_month(now);

    let outer = band(ui, 172.0);
    let card_rect = Rect::from_min_max(
        Pos2::new(outer.left() + PAD, outer.top()),
        Pos2::new(outer.right() - PAD, outer.bottom()),
    );
    ui.painter()
        .rect_filled(card_rect, 28.0, CARD_ON_GRADIENT);

    ui.painter().text(
        Pos2::new(card_rect.left() + 20.0, card_rect.top() + 30.0),
        Align2::LEFT_CENTER,
        format!("{} budget", time::month_name(now)),
        display(17.0),
        TEXT,
    );

    let (status, status_ink, status_bg) = if spent > total_budget {
        ("OVER", CORAL, CORAL.gamma_multiply(0.16))
    } else if spent as f64 > total_budget as f64 * 0.9 {
        ("WATCH IT", LIME, LIME.gamma_multiply(0.14))
    } else {
        ("ON TRACK", LIME, LIME.gamma_multiply(0.14))
    };
    let status_w = if status == "WATCH IT" { 96.0 } else { 84.0 };
    let pill = Rect::from_min_size(
        Pos2::new(card_rect.right() - 20.0 - status_w, card_rect.top() + 17.0),
        Vec2::new(status_w, 28.0),
    );
    badge(ui, pill, status, status_ink, status_bg);

    let spent_text = money(spent, &currency);
    let spent_galley = ui
        .painter()
        .layout_no_wrap(spent_text, heavy(34.0), TEXT);
    let baseline = card_rect.top() + 78.0;
    ui.painter().galley(
        Pos2::new(
            card_rect.left() + 20.0,
            baseline - spent_galley.size().y / 2.0,
        ),
        spent_galley.clone(),
        TEXT,
    );
    ui.painter().text(
        Pos2::new(
            card_rect.left() + 20.0 + spent_galley.size().x + 9.0,
            baseline + 5.0,
        ),
        Align2::LEFT_CENTER,
        format!("of {}", money(total_budget, &currency)),
        medium(15.0),
        DIM,
    );

    let track = Rect::from_min_size(
        Pos2::new(card_rect.left() + 20.0, card_rect.top() + 108.0),
        Vec2::new(card_rect.width() - 40.0, 9.0),
    );
    let frac = if total_budget > 0 {
        spent as f64 / total_budget as f64
    } else {
        0.0
    };
    progress(ui, track, frac, LIME);

    ui.painter().text(
        Pos2::new(card_rect.left() + 20.0, card_rect.bottom() - 24.0),
        Align2::LEFT_CENTER,
        format!("{} of budget used", model_percent(frac)),
        medium(12.5),
        DIM,
    );
    ui.painter().text(
        Pos2::new(card_rect.right() - 20.0, card_rect.bottom() - 24.0),
        Align2::RIGHT_CENTER,
        format!("{days_left} days left"),
        medium(12.5),
        DIM,
    );

    ui.add_space(26.0);

    // ---- Recent ----------------------------------------------------------
    let row = band(ui, 32.0);
    ui.painter().text(
        Pos2::new(row.left() + PAD, row.center().y),
        Align2::LEFT_CENTER,
        "Recent",
        heavy(22.0),
        TEXT,
    );
    let see_all = Rect::from_min_size(
        Pos2::new(row.right() - PAD - 58.0, row.top() + 1.0),
        Vec2::new(58.0, 30.0),
    );
    if hit(ui, see_all, "see_all").clicked() {
        app.screen = Screen::Transactions;
    }
    ui.painter().text(
        see_all.right_center(),
        Align2::RIGHT_CENTER,
        "See all",
        medium(13.5),
        DIM,
    );

    ui.add_space(10.0);
    let recent: Vec<_> = app.data.recent(3).into_iter().cloned().collect();
    for entry in &recent {
        entry_row(ui, &app.data, entry, 64.0);
    }
}

fn model_percent(frac: f64) -> String {
    format!("{}%", (frac * 100.0).round() as i64)
}
