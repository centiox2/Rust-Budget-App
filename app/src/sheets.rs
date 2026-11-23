//! Bottom sheets: logging a spending and creating an envelope.

use egui::{
    Align2, Color32, CornerRadius, Frame, Id, Margin, Order, Pos2, Rect, Sense, Stroke, StrokeKind,
    Vec2,
};

use crate::app::App;
use crate::icons::{self, Icon};
use crate::model::{self, money};
use crate::theme::*;
use crate::time;
use crate::widgets::{band, chip, circle_button, hit, primary_button, text_input, tracked, tracked_centered};

#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub enum Sheet {
    None,
    Expense,
    Category,
}

pub fn show(app: &mut App, ctx: &egui::Context) {
    if app.sheet == Sheet::None {
        return;
    }
    let screen = ctx.content_rect();

    egui::Area::new(Id::unique("sheet_dim"))
        .order(Order::Foreground)
        .fixed_pos(screen.min)
        .show(ctx, |ui| {
            let (rect, response) = ui.allocate_exact_size(screen.size(), Sense::click());
            ui.painter()
                .rect_filled(rect, 0.0, Color32::from_black_alpha(165));
            if response.clicked() {
                app.sheet = Sheet::None;
            }
        });

    egui::Area::new(Id::unique("sheet_body"))
        .order(Order::Foreground)
        .anchor(Align2::CENTER_BOTTOM, Vec2::ZERO)
        .show(ctx, |ui| {
            ui.set_min_width(screen.width());
            Frame::NONE
                .fill(SHEET)
                .corner_radius(CornerRadius {
                    nw: 30,
                    ne: 30,
                    sw: 0,
                    se: 0,
                })
                .inner_margin(Margin {
                    left: 22,
                    right: 22,
                    top: 10,
                    bottom: 24,
                })
                .show(ui, |ui| {
                    ui.set_min_width(screen.width() - 44.0);
                    match app.sheet {
                        Sheet::Expense => expense(app, ui),
                        Sheet::Category => category(app, ui),
                        Sheet::None => {}
                    }
                });
        });
}

fn handle(ui: &mut egui::Ui) {
    let strip = band(ui, 18.0);
    ui.painter().rect_filled(
        Rect::from_center_size(strip.center(), Vec2::new(46.0, 5.0)),
        3.0,
        Color32::from_white_alpha(55),
    );
}

fn title_row(ui: &mut egui::Ui, app: &mut App, title: &str) {
    let row = band(ui, 46.0);
    ui.painter().text(
        Pos2::new(row.left(), row.center().y),
        Align2::LEFT_CENTER,
        title,
        heavy(24.0),
        TEXT,
    );
    let close = Rect::from_center_size(
        Pos2::new(row.right() - 19.0, row.center().y),
        Vec2::splat(38.0),
    );
    if circle_button(ui, close, Color32::from_white_alpha(16), Icon::Close, TEXT, 0.42, 2.0)
        .clicked()
    {
        app.sheet = Sheet::None;
    }
}

// ---- New expense ------------------------------------------------------------

fn expense(app: &mut App, ui: &mut egui::Ui) {
    let currency = app.data.currency.clone();
    let width = ui.available_width();

    handle(ui);
    title_row(ui, app, "New expense");
    ui.add_space(16.0);

    // Amount well.
    let well = band(ui, 122.0);
    ui.painter().rect_filled(well, 22.0, FIELD);
    tracked_centered(
        ui,
        Rect::from_min_size(
            Pos2::new(well.left(), well.top() + 16.0),
            Vec2::new(well.width(), 12.0),
        ),
        "AMOUNT",
        semibold(10.5),
        DIM,
        1.3,
    );

    let shown = if app.expense.amount.is_empty() {
        "0.00".to_owned()
    } else {
        app.expense.amount.clone()
    };
    let amount_width = ui
        .painter()
        .layout_no_wrap(shown, heavy(38.0), TEXT)
        .size()
        .x;
    let currency_width = ui
        .painter()
        .layout_no_wrap(currency.clone(), heavy(38.0), TEXT)
        .size()
        .x;
    let baseline = well.top() + 66.0;
    ui.painter().text(
        Pos2::new(well.center().x - amount_width / 2.0 - currency_width - 1.0, baseline),
        Align2::LEFT_CENTER,
        &currency,
        heavy(38.0),
        TEXT,
    );
    let input = Rect::from_min_size(
        Pos2::new(well.left() + 14.0, well.top() + 44.0),
        Vec2::new(well.width() - 28.0, 46.0),
    );
    text_input(
        ui,
        input,
        &mut app.expense.amount,
        "0.00",
        heavy(38.0),
        Align2::CENTER_CENTER,
    );
    model::sanitize_amount(&mut app.expense.amount);

    let category_name = app.data.category_name(app.expense.category_id).to_owned();
    ui.painter().text(
        Pos2::new(well.center().x, well.bottom() - 20.0),
        Align2::CENTER_CENTER,
        format!("{category_name} · today"),
        medium(12.5),
        DIM,
    );

    // Category chips, wrapped.
    ui.add_space(20.0);
    let label = band(ui, 14.0);
    tracked(
        ui,
        Pos2::new(label.left(), label.center().y),
        "CATEGORY",
        semibold(10.5),
        DIM,
        1.3,
    );
    ui.add_space(12.0);

    let chip_height = 42.0;
    let mut line = 0.0;
    let mut x = 0.0;
    let origin = ui.cursor().min;
    let categories = app.data.categories.clone();
    for category in &categories {
        let text_width = ui
            .painter()
            .layout_no_wrap(category.name.clone(), semibold(13.0), TEXT)
            .size()
            .x;
        let width_of_chip = text_width + 30.0;
        if x > 0.0 && x + width_of_chip > width {
            x = 0.0;
            line += chip_height + 8.0;
        }
        let rect = Rect::from_min_size(
            Pos2::new(origin.x + x, origin.y + line),
            Vec2::new(width_of_chip, chip_height),
        );
        let selected = app.expense.category_id == category.id;
        if chip(ui, rect, &category.name, selected, LIME).clicked() {
            app.expense.category_id = category.id;
        }
        x += width_of_chip + 8.0;
    }
    band(ui, line + chip_height);

    // Note.
    ui.add_space(18.0);
    let note = band(ui, 56.0);
    ui.painter().rect_filled(note, 16.0, FIELD);
    let note_input = Rect::from_min_size(
        Pos2::new(note.left() + 18.0, note.top() + 13.0),
        Vec2::new(note.width() - 36.0, 30.0),
    );
    text_input(
        ui,
        note_input,
        &mut app.expense.note,
        "Add a note — Whole Foods",
        medium(15.0),
        Align2::LEFT_CENTER,
    );

    // Save.
    ui.add_space(20.0);
    let save = band(ui, 58.0);
    let cents = model::parse_money(&app.expense.amount);
    let ready = cents > 0 && app.expense.category_id != 0;
    if primary_button(
        ui,
        save,
        "Save expense",
        Some(Icon::Check),
        if ready { LIME } else { LIME.gamma_multiply(0.32) },
        INK,
    )
    .clicked()
        && ready
    {
        let note = app.expense.note.trim().to_owned();
        let category_id = app.expense.category_id;
        app.data.add_entry(category_id, cents, note, time::now());
        app.save();
        app.sheet = Sheet::None;
        app.notify(format!("{} logged", money(cents, &currency)));
    }
}

// ---- New category -----------------------------------------------------------

fn category(app: &mut App, ui: &mut egui::Ui) {
    let currency = app.data.currency.clone();

    handle(ui);
    title_row(ui, app, "New category");
    ui.add_space(16.0);

    // Name.
    let label = band(ui, 14.0);
    tracked(
        ui,
        Pos2::new(label.left(), label.center().y),
        "NAME",
        semibold(10.5),
        DIM,
        1.3,
    );
    ui.add_space(10.0);
    let name = band(ui, 58.0);
    ui.painter().rect_filled(name, 18.0, FIELD);
    let name_input = Rect::from_min_size(
        Pos2::new(name.left() + 18.0, name.top() + 14.0),
        Vec2::new(name.width() - 36.0, 30.0),
    );
    text_input(
        ui,
        name_input,
        &mut app.category.name,
        "e.g. Subscriptions",
        medium(15.5),
        Align2::LEFT_CENTER,
    );

    // Monthly budget.
    ui.add_space(18.0);
    let label = band(ui, 14.0);
    tracked(
        ui,
        Pos2::new(label.left(), label.center().y),
        "MONTHLY BUDGET",
        semibold(10.5),
        DIM,
        1.3,
    );
    ui.add_space(10.0);
    let budget = band(ui, 62.0);
    ui.painter().rect_filled(budget, 18.0, FIELD);
    let prefix = ui
        .painter()
        .layout_no_wrap(currency.clone(), heavy(22.0), TEXT);
    let prefix_pos = Pos2::new(budget.left() + 18.0, budget.center().y - prefix.size().y / 2.0);
    ui.painter().galley(prefix_pos, prefix.clone(), TEXT);
    let budget_input = Rect::from_min_size(
        Pos2::new(prefix_pos.x + prefix.size().x + 7.0, budget.top() + 16.0),
        Vec2::new(budget.width() - 44.0 - prefix.size().x, 32.0),
    );
    text_input(
        ui,
        budget_input,
        &mut app.category.budget,
        "150",
        heavy(22.0),
        Align2::LEFT_CENTER,
    );
    model::sanitize_amount(&mut app.category.budget);

    // Colour and icon.
    ui.add_space(20.0);
    let label = band(ui, 14.0);
    tracked(
        ui,
        Pos2::new(label.left(), label.center().y),
        "COLOUR & ICON",
        semibold(10.5),
        DIM,
        1.3,
    );
    ui.add_space(12.0);

    let swatches = band(ui, 44.0);
    for (index, color) in SWATCHES.iter().enumerate() {
        let centre = Pos2::new(swatches.left() + 17.0 + index as f32 * 46.0, swatches.center().y);
        let rect = Rect::from_center_size(centre, Vec2::splat(34.0));
        if hit(ui, rect, "swatch").clicked() {
            app.category.color = index;
        }
        ui.painter().circle_filled(centre, 17.0, *color);
        if app.category.color == index {
            ui.painter()
                .circle_stroke(centre, 21.5, Stroke::new(1.8, Color32::WHITE));
        }
    }

    ui.add_space(12.0);
    let icons_row = band(ui, 54.0);
    for (index, icon) in Icon::CHOICES.iter().enumerate() {
        let centre = Pos2::new(icons_row.left() + 21.0 + index as f32 * 54.0, icons_row.center().y);
        let rect = Rect::from_center_size(centre, Vec2::splat(44.0));
        if hit(ui, rect, "icon_choice").clicked() {
            app.category.icon = *icon;
        }
        let selected = app.category.icon == *icon;
        ui.painter().rect_filled(
            rect,
            14.0,
            if selected { Color32::from_white_alpha(22) } else { FIELD },
        );
        icons::paint_centered(ui.painter(), centre, 22.0, *icon, TEXT, 1.8);
        if selected {
            ui.painter()
                .rect_stroke(rect, 14.0, Stroke::new(1.6, Color32::WHITE), StrokeKind::Inside);
        }
    }

    // Create.
    ui.add_space(22.0);
    let create = band(ui, 58.0);
    let budget_cents = model::parse_money(&app.category.budget);
    let name_text = app.category.name.trim().to_owned();
    let ready = !name_text.is_empty() && budget_cents > 0;
    if primary_button(
        ui,
        create,
        "Create category",
        Some(Icon::Plus),
        if ready { LIME } else { LIME.gamma_multiply(0.32) },
        INK,
    )
    .clicked()
        && ready
    {
        let swatch = SWATCHES[app.category.color.min(SWATCHES.len() - 1)];
        let color = [swatch.r(), swatch.g(), swatch.b()];
        let icon = app.category.icon.key().to_owned();
        app.data.add_category(name_text.clone(), budget_cents, color, icon);
        app.save();
        app.sheet = Sheet::None;
        app.notify(format!("{name_text} added to your envelopes"));
    }
}
