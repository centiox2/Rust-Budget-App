//! Small painted primitives shared by every screen.

use std::sync::Arc;

use egui::{
    text::LayoutJob, Align2, Color32, CursorIcon, FontId, Galley, Margin, Pos2, Rect, Response,
    Sense, Shape, Stroke, TextFormat, Ui, Vec2,
};

use crate::icons::{self, Icon};
use crate::model::{self, Data, Entry};
use crate::theme::*;
use crate::time;

/// Space to leave above the first row so content clears the system status bar.
pub fn top_inset() -> f32 {
    if cfg!(target_os = "android") {
        34.0
    } else {
        22.0
    }
}

/// Reserve a full-width horizontal strip of the current layout.
pub fn band(ui: &mut Ui, height: f32) -> Rect {
    let width = ui.available_width();
    ui.allocate_exact_size(Vec2::new(width, height), Sense::hover())
        .0
}

/// Make `rect` tappable. The id is stable across frames for a given position.
pub fn hit(ui: &Ui, rect: Rect, name: &str) -> Response {
    ui.interact(
        rect,
        ui.scope_id()
            .with((name, rect.min.x as i32, rect.min.y as i32)),
        Sense::click(),
    )
    .on_hover_cursor(CursorIcon::PointingHand)
}

// ---- Text -------------------------------------------------------------------

pub fn tracked_galley(
    ui: &Ui,
    text: &str,
    font: FontId,
    color: Color32,
    tracking: f32,
) -> Arc<Galley> {
    let mut job = LayoutJob::default();
    job.append(
        text,
        0.0,
        TextFormat {
            font_id: font,
            color,
            extra_letter_spacing: tracking,
            ..Default::default()
        },
    );
    ui.painter().layout_job(job)
}

/// Letterspaced label, left aligned at `pos` (vertically centred).
pub fn tracked(ui: &Ui, pos: Pos2, text: &str, font: FontId, color: Color32, tracking: f32) {
    let galley = tracked_galley(ui, text, font, color, tracking);
    let top = pos.y - galley.size().y / 2.0;
    ui.painter()
        .galley(Pos2::new(pos.x, top), galley, color);
}

/// Letterspaced label centred inside `rect`.
pub fn tracked_centered(
    ui: &Ui,
    rect: Rect,
    text: &str,
    font: FontId,
    color: Color32,
    tracking: f32,
) {
    let galley = tracked_galley(ui, text, font, color, tracking);
    let min = rect.center() - galley.size() / 2.0;
    ui.painter().galley(min, galley, color);
}

pub fn wrapped(ui: &Ui, rect: Rect, text: &str, font: FontId, color: Color32) {
    let galley = ui
        .painter()
        .layout(text.to_owned(), font, color, rect.width());
    ui.painter().galley(rect.left_top(), galley, color);
}

// ---- Surfaces ---------------------------------------------------------------

fn gradient_quad(mesh: &mut egui::Mesh, rect: Rect, top: Color32, bottom: Color32) {
    let base = mesh.vertices.len() as u32;
    mesh.colored_vertex(rect.left_top(), top);
    mesh.colored_vertex(rect.right_top(), top);
    mesh.colored_vertex(rect.left_bottom(), bottom);
    mesh.colored_vertex(rect.right_bottom(), bottom);
    mesh.add_triangle(base, base + 1, base + 2);
    mesh.add_triangle(base + 1, base + 3, base + 2);
}

/// The purple wash at the top of a screen, fading into the background.
pub fn header_gradient(ui: &Ui, rect: Rect) {
    let mut mesh = egui::Mesh::default();
    let mid = rect.top() + rect.height() * 0.45;
    gradient_quad(
        &mut mesh,
        Rect::from_min_max(rect.min, Pos2::new(rect.right(), mid)),
        GRAD_TOP,
        GRAD_MID,
    );
    gradient_quad(
        &mut mesh,
        Rect::from_min_max(Pos2::new(rect.left(), mid), rect.max),
        GRAD_MID,
        Color32::TRANSPARENT,
    );
    ui.painter().add(Shape::mesh(mesh));
}

pub fn card(ui: &Ui, rect: Rect, fill: Color32) {
    ui.painter().rect_filled(rect, CARD_RADIUS, fill);
}

pub fn progress(ui: &Ui, rect: Rect, frac: f64, color: Color32) {
    let painter = ui.painter();
    painter.rect_filled(rect, rect.height() / 2.0, TRACK);
    let frac = frac.clamp(0.0, 1.0) as f32;
    if frac > 0.0 {
        let width = (rect.width() * frac).max(rect.height());
        painter.rect_filled(
            Rect::from_min_size(rect.min, Vec2::new(width, rect.height())),
            rect.height() / 2.0,
            color,
        );
    }
}

pub fn icon_tile(ui: &Ui, rect: Rect, color: Color32, icon: Icon) {
    let painter = ui.painter();
    painter.rect_filled(rect, rect.width() * 0.3, color.gamma_multiply(0.16));
    icons::paint(
        painter,
        Rect::from_center_size(rect.center(), Vec2::splat(rect.width() * 0.5)),
        icon,
        color,
        1.7,
    );
}

pub fn badge(ui: &Ui, rect: Rect, label: &str, fg: Color32, bg: Color32) {
    ui.painter().rect_filled(rect, rect.height() / 2.0, bg);
    tracked_centered(ui, rect, label, semibold(11.0), fg, 0.6);
}

// ---- Controls ---------------------------------------------------------------

pub fn circle_button(
    ui: &mut Ui,
    rect: Rect,
    fill: Color32,
    icon: Icon,
    color: Color32,
    icon_fraction: f32,
    stroke_width: f32,
) -> Response {
    let response = hit(ui, rect, "circle_button");
    let painter = ui.painter();
    painter.circle_filled(rect.center(), rect.width() / 2.0, fill);
    icons::paint(
        painter,
        Rect::from_center_size(rect.center(), Vec2::splat(rect.width() * icon_fraction)),
        icon,
        color,
        stroke_width,
    );
    response
}

/// Filled pill button with optional leading icon.
pub fn primary_button(
    ui: &mut Ui,
    rect: Rect,
    label: &str,
    icon: Option<Icon>,
    fill: Color32,
    ink: Color32,
) -> Response {
    let response = hit(ui, rect, "primary_button");
    let painter = ui.painter();
    painter.rect_filled(rect, rect.height() / 2.0, fill);
    let galley = ui
        .painter()
        .layout_no_wrap(label.to_owned(), semibold(16.0), ink);
    let icon_space = if icon.is_some() { 26.0 } else { 0.0 };
    let start = rect.center().x - (galley.size().x + icon_space) / 2.0;
    if let Some(icon) = icon {
        icons::paint_centered(
            painter,
            Pos2::new(start + 9.0, rect.center().y),
            18.0,
            icon,
            ink,
            2.0,
        );
    }
    painter.galley(
        Pos2::new(start + icon_space, rect.center().y - galley.size().y / 2.0),
        galley,
        ink,
    );
    response
}

/// Outlined pill button, e.g. "New budget".
pub fn outline_button(ui: &mut Ui, rect: Rect, label: &str, icon: Option<Icon>) -> Response {
    let response = hit(ui, rect, "outline_button");
    let painter = ui.painter();
    painter.rect_stroke(
        rect,
        rect.height() / 2.0,
        Stroke::new(1.2, Color32::from_white_alpha(38)),
        egui::StrokeKind::Inside,
    );
    let galley = ui
        .painter()
        .layout_no_wrap(label.to_owned(), semibold(15.0), TEXT);
    let icon_space = if icon.is_some() { 24.0 } else { 0.0 };
    let start = rect.center().x - (galley.size().x + icon_space) / 2.0;
    if let Some(icon) = icon {
        icons::paint_centered(
            painter,
            Pos2::new(start + 8.0, rect.center().y),
            16.0,
            icon,
            TEXT,
            1.8,
        );
    }
    painter.galley(
        Pos2::new(start + icon_space, rect.center().y - galley.size().y / 2.0),
        galley,
        TEXT,
    );
    response
}

pub fn chip(ui: &Ui, rect: Rect, label: &str, on: bool, accent: Color32) -> Response {
    let response = hit(ui, rect, "chip");
    let painter = ui.painter();
    painter.rect_filled(rect, rect.height() / 2.0, if on { accent } else { GLASS });
    painter.text(
        rect.center(),
        Align2::CENTER_CENTER,
        label,
        semibold(13.0),
        if on { INK } else { TEXT },
    );
    response
}

/// Three-way segmented control. Returns the newly selected index, if any.
pub fn segmented(ui: &Ui, rect: Rect, labels: &[&str], selected: usize) -> Option<usize> {
    let painter = ui.painter();
    painter.rect_filled(rect, rect.height() / 2.0, Color32::from_white_alpha(18));
    let seg_width = rect.width() / labels.len().max(1) as f32;
    for (index, label) in labels.iter().enumerate() {
        let seg = Rect::from_min_size(
            Pos2::new(rect.left() + index as f32 * seg_width, rect.top()),
            Vec2::new(seg_width, rect.height()),
        );
        if index == selected {
            painter.rect_filled(
                seg.shrink2(Vec2::new(4.0, 5.0)),
                (rect.height() - 10.0) / 2.0,
                Color32::WHITE,
            );
        }
        painter.text(
            seg.center(),
            Align2::CENTER_CENTER,
            *label,
            semibold(13.5),
            if index == selected { INK } else { TEXT },
        );
    }
    let mut clicked = None;
    for index in 0..labels.len() {
        let seg = Rect::from_min_size(
            Pos2::new(rect.left() + index as f32 * seg_width, rect.top()),
            Vec2::new(seg_width, rect.height()),
        );
        if hit(ui, seg, "segment").clicked() && index != selected {
            clicked = Some(index);
        }
    }
    clicked
}

/// A text field painted into an exact rect.
pub fn text_input(
    ui: &mut Ui,
    rect: Rect,
    text: &mut String,
    hint: &str,
    font: FontId,
    align: Align2,
) -> Response {
    let edit = egui::TextEdit::singleline(text)
        .frame(egui::Frame::NONE)
        .font(font)
        .text_color(TEXT)
        .hint_text(egui::RichText::new(hint).color(FAINT))
        .align(align)
        .margin(Margin::same(0))
        .desired_width(rect.width());
    ui.put(rect, edit)
}

// ---- Rows -------------------------------------------------------------------

/// One spending row: tinted icon tile, note + category, amount on the right.
pub fn entry_row(ui: &mut Ui, data: &Data, entry: &Entry, height: f32) {
    let rect = band(ui, height);
    let tile = Rect::from_center_size(
        Pos2::new(rect.left() + PAD + 22.0, rect.center().y),
        Vec2::splat(44.0),
    );
    let category = data.category(entry.category_id);
    let color = category.map(|c| rgb(c.color)).unwrap_or(DIM);
    let icon = Icon::from_key(category.map(|c| c.icon.as_str()).unwrap_or("tag"));
    icon_tile(ui, tile, color, icon);

    let painter = ui.painter();
    let left = tile.right() + 14.0;
    let title = if entry.note.trim().is_empty() {
        data.category_name(entry.category_id).to_owned()
    } else {
        entry.note.clone()
    };
    painter.text(
        Pos2::new(left, rect.center().y - 10.0),
        Align2::LEFT_CENTER,
        title,
        semibold(15.5),
        TEXT,
    );
    painter.text(
        Pos2::new(left, rect.center().y + 11.0),
        Align2::LEFT_CENTER,
        format!(
            "{} · {}",
            data.category_name(entry.category_id),
            time::day_label(entry.at)
        ),
        medium(12.5),
        DIM,
    );
    painter.text(
        Pos2::new(rect.right() - PAD, rect.center().y),
        Align2::RIGHT_CENTER,
        format!("−{}", model::money(entry.amount_cents, &data.currency)),
        semibold(15.5),
        TEXT,
    );
}
