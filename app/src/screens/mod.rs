//! The five screens and the router between them.

pub mod budgets;
pub mod home;
pub mod insights;
pub mod profile;
pub mod transactions;

use crate::app::App;

#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub enum Screen {
    Home,
    Budgets,
    Insights,
    Profile,
    Transactions,
}

pub fn show(app: &mut App, ui: &mut egui::Ui) {
    match app.screen {
        Screen::Home => home::show(app, ui),
        Screen::Budgets => budgets::show(app, ui),
        Screen::Insights => insights::show(app, ui),
        Screen::Profile => profile::show(app, ui),
        Screen::Transactions => transactions::show(app, ui),
    }
}
