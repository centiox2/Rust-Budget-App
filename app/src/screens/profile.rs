//! Your details, the month at a glance, and the reset hatch.

use egui::{Align2, Pos2, Rect, Vec2};

use crate::app::{inset, App};
use crate::icons::Icon;
use crate::model::money;
use crate::theme::*;
use crate::widgets::*;

pub fn show(app: &mut App, ui: &mut egui::Ui) {
    let currency = app.data.currency.clone();
    let width = ui.available_width();
    let top = ui.cursor().min.y;

    header_gradient(
        ui,
        Rect::from_min_max(Pos2::new(0.0, top - 8.0), Pos2::new(width, top + 340.0)),
    );
    ui.add_space(inset() + 4.0);

    let row = band(ui, 46.0);
    ui.painter().text(
        Pos2::new(row.left() + PAD, row.center().y),
        Align2::LEFT_CENTER,
        "Profile",
        heavy(30.0),
        TEXT,
    );

    ui.add_space(22.0);

    text_row(ui, "YOUR NAME", &mut app.data.name, "Amara");
    ui.add_space(18.0);
    text_row(ui, "CURRENCY", &mut app.data.currency, "£");

    ui.add_space(6.0);
    let save = band(ui, 54.0);
    let save_rect = Rect::from_min_max(
        Pos2::new(save.left() + PAD, save.top()),
        Pos2::new(save.right() - PAD, save.bottom()),
    );
    if primary_button(ui, save_rect, "Save details", Some(Icon::Check), LIME, INK).clicked() {
        app.save();
        app.notify("Saved");
    }

    ui.add_space(30.0);

    // ---- This month ------------------------------------------------------
    let heading = band(ui, 26.0);
    ui.painter().text(
        Pos2::new(heading.left() + PAD, heading.center().y),
        Align2::LEFT_CENTER,
        "This month",
        heavy(20.0),
        TEXT,
    );
    ui.add_space(12.0);

    let spent = app.data.spent_this_month();
    let budget = app.data.budget_total();
    let total_entries = app.data.entries.len();

    let outer = band(ui, 176.0);
    let card = Rect::from_min_max(
        Pos2::new(outer.left() + PAD, outer.top()),
        Pos2::new(outer.right() - PAD, outer.bottom()),
    );
    ui.painter().rect_filled(card, 26.0, CARD);

    let rows: [(&str, String); 4] = [
        ("Monthly budget", money(budget, &currency)),
        ("Spent so far", money(spent, &currency)),
        ("Safe to spend", money(app.data.safe_to_spend(), &currency)),
        ("Spendings logged", format!("{total_entries}")),
    ];
    for (index, (label, value)) in rows.iter().enumerate() {
        let y = card.top() + 26.0 + index as f32 * 42.0;
        ui.painter().text(
            Pos2::new(card.left() + 20.0, y),
            Align2::LEFT_CENTER,
            *label,
            medium(14.0),
            DIM,
        );
        ui.painter().text(
            Pos2::new(card.right() - 20.0, y),
            Align2::RIGHT_CENTER,
            value,
            semibold(15.0),
            if index == 2 { LIME } else { TEXT },
        );
    }

    ui.add_space(22.0);

    let reset = band(ui, 54.0);
    let reset_rect = Rect::from_min_max(
        Pos2::new(reset.left() + PAD, reset.top()),
        Pos2::new(reset.right() - PAD, reset.bottom()),
    );
    if outline_button(ui, reset_rect, "Reset demo data", None).clicked() {
        app.data = crate::seed::demo();
        app.save();
        app.notify("Demo data restored");
    }

    ui.add_space(26.0);
    let footer = band(ui, 40.0);
    tracked_centered(
        ui,
        footer,
        "AMARA · RUST + EGUI · 0.1.0",
        semibold(10.0),
        FAINT,
        1.4,
    );
}

fn text_row(ui: &mut egui::Ui, title: &str, value: &mut String, hint: &str) {
    let label = band(ui, 14.0);
    tracked(
        ui,
        Pos2::new(label.left() + PAD, label.center().y),
        title,
        semibold(10.5),
        DIM,
        1.2,
    );
    ui.add_space(8.0);
    let outer = band(ui, 58.0);
    let rect = Rect::from_min_max(
        Pos2::new(outer.left() + PAD, outer.top()),
        Pos2::new(outer.right() - PAD, outer.bottom()),
    );
    ui.painter().rect_filled(rect, 18.0, FIELD);
    let input = Rect::from_min_size(
        Pos2::new(rect.left() + 18.0, rect.top() + 14.0),
        Vec2::new(rect.width() - 36.0, 30.0),
    );
    text_input(ui, input, value, hint, medium(16.0), Align2::LEFT_CENTER);
}
