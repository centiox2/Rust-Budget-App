//! Every logged spending, grouped by day.

use egui::{Align2, Pos2, Rect, Vec2};

use crate::app::{inset, App};
use crate::icons::Icon;
use crate::model::money;
use crate::screens::Screen;
use crate::theme::*;
use crate::time;
use crate::widgets::*;

pub fn show(app: &mut App, ui: &mut egui::Ui) {
    let currency = app.data.currency.clone();
    let width = ui.available_width();
    let top = ui.cursor().min.y;

    header_gradient(
        ui,
        Rect::from_min_max(Pos2::new(0.0, top - 8.0), Pos2::new(width, top + 300.0)),
    );
    ui.add_space(inset() + 2.0);

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
        "All spending",
        semibold(17.0),
        TEXT,
    );

    ui.add_space(20.0);
    let summary = band(ui, 46.0);
    ui.painter().text(
        Pos2::new(summary.left() + PAD, summary.center().y),
        Align2::LEFT_CENTER,
        money(app.data.spent_this_month(), &currency),
        heavy(30.0),
        TEXT,
    );
    ui.painter().text(
        Pos2::new(summary.right() - PAD, summary.center().y + 4.0),
        Align2::RIGHT_CENTER,
        format!("this {}", time::month_name(time::now()).to_lowercase()),
        medium(12.5),
        DIM,
    );

    ui.add_space(14.0);

    let entries: Vec<_> = app.data.all_sorted().into_iter().cloned().collect();
    if entries.is_empty() {
        let empty = band(ui, 120.0);
        ui.painter().text(
            empty.center(),
            Align2::CENTER_CENTER,
            "Nothing logged yet — tap + to add a spending",
            medium(14.0),
            DIM,
        );
        return;
    }

    let mut last_day = String::new();
    for entry in &entries {
        let key = time::day_key(entry.at);
        if key != last_day {
            last_day = key;
            ui.add_space(10.0);
            let header = band(ui, 26.0);
            ui.painter().text(
                Pos2::new(header.left() + PAD, header.center().y),
                Align2::LEFT_CENTER,
                time::day_label(entry.at),
                semibold(13.0),
                DIM,
            );
        }
        entry_row(ui, &app.data, entry, 62.0);
    }
}
