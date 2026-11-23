//! App state, the bottom navigation bar, and the frame loop.

use std::path::PathBuf;
use std::time::Duration;

use egui::{Align2, Color32, Frame, Id, Margin, Order, Pos2, Rect, Vec2};

use crate::icons::{self, Icon};
use crate::model::Data;
use crate::screens::{self, Screen};
use crate::sheets::{self, Sheet};
use crate::store::Store;
use crate::theme;
use crate::theme::*;
use crate::widgets::hit;

pub struct ExpenseDraft {
    pub amount: String,
    pub category_id: u64,
    pub note: String,
}

pub struct CategoryDraft {
    pub name: String,
    pub budget: String,
    pub color: usize,
    pub icon: Icon,
}

pub struct App {
    pub data: Data,
    pub store: Store,
    pub screen: Screen,
    pub sheet: Sheet,
    pub expense: ExpenseDraft,
    pub category: CategoryDraft,
    /// 0 = daily, 1 = weekly, 2 = monthly.
    pub range: usize,
    toast: Option<(String, f64)>,
}

impl App {
    pub fn new(cc: &eframe::CreationContext<'_>, data_dir: PathBuf) -> Self {
        theme::install(&cc.egui_ctx);
        let store = Store::new(&data_dir);
        let data = store.load();
        log::info!("data file: {}", store.path().display());

        let category_id = data.most_used_category();
        Self {
            data,
            store,
            screen: Screen::Home,
            sheet: Sheet::None,
            expense: ExpenseDraft {
                amount: String::new(),
                category_id,
                note: String::new(),
            },
            category: CategoryDraft {
                name: String::new(),
                budget: String::new(),
                color: 0,
                icon: Icon::Tag,
            },
            range: 2,
            toast: None,
        }
    }

    pub fn save(&self) {
        self.store.save(&self.data);
    }

    pub fn notify(&mut self, message: impl Into<String>) {
        self.toast = Some((message.into(), 2.6));
    }

    pub fn open_expense(&mut self) {
        self.expense = ExpenseDraft {
            amount: String::new(),
            category_id: self.data.most_used_category(),
            note: String::new(),
        };
        self.sheet = Sheet::Expense;
    }

    pub fn open_category(&mut self) {
        self.category = CategoryDraft {
            name: String::new(),
            budget: String::new(),
            color: 0,
            icon: Icon::Tag,
        };
        self.sheet = Sheet::Category;
    }

    fn nav(&mut self, ui: &mut egui::Ui) {
        let rect = ui.max_rect();
        let painter = ui.painter();
        painter.rect_filled(rect, 0.0, NAV);
        painter.rect_filled(
            Rect::from_min_size(rect.min, Vec2::new(rect.width(), 1.0)),
            0.0,
            Color32::from_white_alpha(12),
        );

        let centre_y = rect.top() + 38.0;
        let items: [(Screen, Icon, f32); 4] = [
            (Screen::Home, Icon::Home, 0.12),
            (Screen::Budgets, Icon::Wallet, 0.31),
            (Screen::Insights, Icon::Chart, 0.69),
            (Screen::Profile, Icon::User, 0.88),
        ];
        for (screen, icon, x) in items {
            let centre = Pos2::new(rect.left() + rect.width() * x, centre_y);
            let active = self.screen == screen
                || (screen == Screen::Budgets && self.screen == Screen::Transactions);
            if hit(ui, Rect::from_center_size(centre, Vec2::splat(54.0)), "nav").clicked() {
                self.screen = screen;
            }
            let painter = ui.painter();
            if active {
                painter.circle_filled(centre, 23.0, LIME.gamma_multiply(0.14));
            }
            icons::paint_centered(
                painter,
                centre,
                24.0,
                icon,
                if active { LIME } else { DIM },
                1.9,
            );
        }

        // The lime add button sits in the middle of the bar.
        let fab = Rect::from_center_size(Pos2::new(rect.center().x, centre_y), Vec2::splat(60.0));
        let response = hit(ui, fab, "fab");
        let painter = ui.painter();
        painter.circle_filled(fab.center(), 30.0, LIME);
        icons::paint_centered(painter, fab.center(), 24.0, Icon::Plus, INK, 2.6);
        if response.clicked() {
            self.open_expense();
        }
    }

    fn show_toast(&self, ctx: &egui::Context) {
        let Some((message, _)) = &self.toast else {
            return;
        };
        egui::Area::new(Id::unique("toast"))
            .order(Order::Foreground)
            .interactable(false)
            .anchor(Align2::CENTER_BOTTOM, Vec2::new(0.0, -108.0))
            .show(ctx, |ui| {
                Frame::NONE
                    .fill(Color32::from_rgb(0x26, 0x20, 0x3E))
                    .corner_radius(18.0)
                    .inner_margin(Margin::symmetric(18, 12))
                    .show(ui, |ui| {
                        ui.label(
                            egui::RichText::new(message)
                                .font(semibold(14.0))
                                .color(TEXT),
                        );
                    });
            });
        ctx.request_repaint_after(Duration::from_millis(120));
    }
}

/// Space to leave above the first row so content clears the status bar.
pub fn inset() -> f32 {
    crate::widgets::top_inset()
}

impl eframe::App for App {
    fn ui(&mut self, ui: &mut egui::Ui, _frame: &mut eframe::Frame) {
        let ctx = ui.ctx().clone();

        if let Some((_, remaining)) = &mut self.toast {
            *remaining -= ctx.input(|input| input.stable_dt) as f64;
            if *remaining <= 0.0 {
                self.toast = None;
            }
        }

        let nav_height = 92.0 + if cfg!(target_os = "android") { 10.0 } else { 0.0 };
        egui::Panel::bottom("nav")
            .exact_size(nav_height)
            .resizable(false)
            .frame(Frame::NONE.fill(NAV))
            .show(ui, |ui| self.nav(ui));

        egui::CentralPanel::default()
            .frame(Frame::NONE.fill(BG))
            .show(ui, |ui| {
                egui::ScrollArea::vertical()
                    .auto_shrink([false, false])
                    .show(ui, |ui| {
                        screens::show(self, ui);
                        ui.add_space(24.0);
                    });
            });

        sheets::show(self, &ctx);
        self.show_toast(&ctx);

        ctx.request_repaint_after(Duration::from_millis(500));
    }
}
