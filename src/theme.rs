//! Colours, fonts and global styles for the app.

use std::sync::Arc;

use egui::{Color32, FontData, FontDefinitions, FontFamily, FontId, Stroke};

// ---- Surfaces ---------------------------------------------------------------

/// The near-black plum the whole app sits on.
pub const BG: Color32 = Color32::from_rgb(0x0A, 0x07, 0x12);
/// Bottom navigation bar.
pub const NAV: Color32 = Color32::from_rgb(0x0D, 0x0A, 0x17);
/// Raised cards on the background.
pub const CARD: Color32 = Color32::from_rgb(0x18, 0x13, 0x2B);
/// Cards that sit on top of the purple gradient.
pub const CARD_ON_GRADIENT: Color32 = Color32::from_rgba_premultiplied(0x24, 0x1B, 0x3D, 0xEA);
/// Lighter card used for small badges inside cards.
pub const CARD_SOFT: Color32 = Color32::from_rgb(0x24, 0x1E, 0x3D);
/// Bottom sheets.
pub const SHEET: Color32 = Color32::from_rgb(0x1A, 0x15, 0x2D);
/// Text inputs and amount wells.
pub const FIELD: Color32 = Color32::from_rgb(0x1D, 0x18, 0x31);
/// Translucent chip / circle fills on top of a gradient.
pub const GLASS: Color32 = Color32::from_rgba_premultiplied(0xFF, 0xFF, 0xFF, 0x1F);
pub const GLASS_STRONG: Color32 = Color32::from_rgba_premultiplied(0xFF, 0xFF, 0xFF, 0x33);
/// Progress bar groove.
pub const TRACK: Color32 = Color32::from_rgba_premultiplied(0xFF, 0xFF, 0xFF, 0x14);

// ---- Ink --------------------------------------------------------------------

pub const TEXT: Color32 = Color32::from_rgb(0xF6, 0xF3, 0xFB);
pub const DIM: Color32 = Color32::from_rgb(0x9C, 0x93, 0xB8);
pub const FAINT: Color32 = Color32::from_rgb(0x6E, 0x67, 0x88);
/// Ink used on top of lime.
pub const INK: Color32 = Color32::from_rgb(0x10, 0x14, 0x05);

// ---- Accents ----------------------------------------------------------------

pub const LIME: Color32 = Color32::from_rgb(0xCB, 0xF9, 0x5B);
pub const VIOLET: Color32 = Color32::from_rgb(0x7C, 0x6C, 0xFF);
pub const CORAL: Color32 = Color32::from_rgb(0xFF, 0x73, 0x63);
pub const MINT: Color32 = Color32::from_rgb(0x4F, 0xDD, 0xA5);
pub const BLUE: Color32 = Color32::from_rgb(0x54, 0xAF, 0xF7);

// ---- Gradient header --------------------------------------------------------

pub const GRAD_TOP: Color32 = Color32::from_rgb(0x8A, 0x5C, 0xFA);
pub const GRAD_MID: Color32 = Color32::from_rgb(0x46, 0x2A, 0x86);

// ---- Metrics ----------------------------------------------------------------

pub const PAD: f32 = 20.0;
pub const CARD_RADIUS: f32 = 26.0;

/// The five swatches offered when creating a category.
pub const SWATCHES: [Color32; 5] = [VIOLET, CORAL, MINT, BLUE, LIME];

// ---- Fonts ------------------------------------------------------------------

pub const FAM_REG: &str = "Inter";
pub const FAM_MED: &str = "InterMedium";
pub const FAM_SEMI: &str = "InterSemiBold";
pub const FAM_DISPLAY: &str = "InterDisplay";
pub const FAM_HEAVY: &str = "InterDisplayHeavy";

pub fn font(size: f32, family: &str) -> FontId {
    FontId::new(size, FontFamily::Name(Arc::from(family)))
}

pub fn regular(size: f32) -> FontId {
    font(size, FAM_REG)
}
pub fn medium(size: f32) -> FontId {
    font(size, FAM_MED)
}
pub fn semibold(size: f32) -> FontId {
    font(size, FAM_SEMI)
}
/// Tight, slightly condensed display cut — section headings and card titles.
pub fn display(size: f32) -> FontId {
    font(size, FAM_DISPLAY)
}
/// The heaviest cut — balances, totals, the hero number.
pub fn heavy(size: f32) -> FontId {
    font(size, FAM_HEAVY)
}

/// Register the bundled Inter cuts and give the app its dark styling.
pub fn install(ctx: &egui::Context) {
    let mut defs = FontDefinitions::default();

    let cuts: [(&str, &'static [u8]); 5] = [
        ("inter-reg", include_bytes!("../assets/fonts/Inter-Regular.ttf")),
        ("inter-med", include_bytes!("../assets/fonts/Inter-Medium.ttf")),
        ("inter-semi", include_bytes!("../assets/fonts/Inter-SemiBold.ttf")),
        (
            "inter-disp",
            include_bytes!("../assets/fonts/InterDisplay-SemiBold.ttf"),
        ),
        (
            "inter-heavy",
            include_bytes!("../assets/fonts/InterDisplay-ExtraBold.ttf"),
        ),
    ];
    for (key, bytes) in cuts {
        defs.font_data
            .insert(key.to_owned(), Arc::new(FontData::from_static(bytes)));
    }

    // Whatever egui ships as fallbacks (emoji, symbols) stays available.
    let fallbacks = defs
        .families
        .get(&FontFamily::Proportional)
        .cloned()
        .unwrap_or_default();

    for (family, cut) in [
        (FAM_REG, "inter-reg"),
        (FAM_MED, "inter-med"),
        (FAM_SEMI, "inter-semi"),
        (FAM_DISPLAY, "inter-disp"),
        (FAM_HEAVY, "inter-heavy"),
    ] {
        let mut list = vec![cut.to_owned()];
        list.extend(fallbacks.iter().cloned());
        defs.families
            .insert(FontFamily::Name(Arc::from(family)), list);
    }

    let mut proportional = vec!["inter-reg".to_owned()];
    proportional.extend(fallbacks);
    defs.families.insert(FontFamily::Proportional, proportional);

    ctx.set_fonts(defs);

    ctx.all_styles_mut(|style| {
        style.visuals = egui::Visuals::dark();
        style.visuals.panel_fill = BG;
        style.visuals.window_fill = BG;
        style.visuals.extreme_bg_color = FIELD;
        style.visuals.override_text_color = Some(TEXT);
        style.visuals.widgets.noninteractive.bg_stroke = Stroke::NONE;
        style.visuals.widgets.inactive.bg_stroke = Stroke::NONE;
        style.visuals.widgets.hovered.bg_stroke = Stroke::NONE;
        style.visuals.widgets.active.bg_stroke = Stroke::NONE;
        style.visuals.selection.bg_fill = VIOLET.gamma_multiply(0.5);
        style.spacing.item_spacing = egui::vec2(8.0, 8.0);
        style.spacing.button_padding = egui::vec2(0.0, 0.0);
        style.text_styles.insert(egui::TextStyle::Body, medium(14.0));
        style.text_styles.insert(egui::TextStyle::Button, semibold(14.0));
        style.text_styles.insert(egui::TextStyle::Small, medium(12.0));
    });
}

/// Colour for a category's stored RGB triple.
pub fn rgb(c: [u8; 3]) -> Color32 {
    Color32::from_rgb(c[0], c[1], c[2])
}
