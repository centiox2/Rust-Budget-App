//! A small vector icon set drawn straight onto the painter, so the app needs no
//! icon fonts or image assets.

use std::f32::consts::{PI, TAU};

use egui::{Color32, Painter, Pos2, Rect, Shape, Stroke, StrokeKind, Vec2};

#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub enum Icon {
    Plus,
    Minus,
    Home,
    Wallet,
    Tag,
    Chart,
    User,
    Search,
    Bell,
    Cart,
    Cup,
    Bolt,
    Pin,
    Gift,
    Check,
    Close,
    Back,
    More,
    TrendUp,
    Sparkle,
}

impl Icon {
    /// The icons offered in the new-category sheet.
    pub const CHOICES: [Icon; 5] = [Icon::Tag, Icon::Cup, Icon::Bolt, Icon::Pin, Icon::Gift];

    pub fn key(self) -> &'static str {
        match self {
            Icon::Plus => "plus",
            Icon::Minus => "minus",
            Icon::Home => "home",
            Icon::Wallet => "wallet",
            Icon::Tag => "tag",
            Icon::Chart => "chart",
            Icon::User => "user",
            Icon::Search => "search",
            Icon::Bell => "bell",
            Icon::Cart => "cart",
            Icon::Cup => "cup",
            Icon::Bolt => "bolt",
            Icon::Pin => "pin",
            Icon::Gift => "gift",
            Icon::Check => "check",
            Icon::Close => "close",
            Icon::Back => "back",
            Icon::More => "more",
            Icon::TrendUp => "trend-up",
            Icon::Sparkle => "sparkle",
        }
    }

    pub fn from_key(key: &str) -> Self {
        match key {
            "cart" => Icon::Cart,
            "cup" => Icon::Cup,
            "bolt" => Icon::Bolt,
            "pin" => Icon::Pin,
            "gift" => Icon::Gift,
            "wallet" => Icon::Wallet,
            "chart" => Icon::Chart,
            "home" => Icon::Home,
            "user" => Icon::User,
            "search" => Icon::Search,
            "bell" => Icon::Bell,
            _ => Icon::Tag,
        }
    }
}

fn arc(center: Pos2, radius: f32, from: f32, to: f32, steps: usize) -> Vec<Pos2> {
    (0..=steps)
        .map(|i| {
            let t = i as f32 / steps as f32;
            let angle = from + (to - from) * t;
            center + Vec2::new(angle.cos(), angle.sin()) * radius
        })
        .collect()
}

/// Paint `icon` inside `rect`, in `color`, with a stroke width of `width`.
pub fn paint(p: &Painter, rect: Rect, icon: Icon, color: Color32, width: f32) {
    let width = width.max(1.0);
    let stroke = Stroke::new(width, color);
    let at = |x: f32, y: f32| rect.lerp_inside(Vec2::new(x, y));
    let w = rect.width();

    let poly = |pts: &[(f32, f32)]| -> Vec<Pos2> {
        pts.iter().map(|(x, y)| at(*x, *y)).collect()
    };
    let line = |pts: Vec<Pos2>| {
        if pts.len() > 1 {
            p.add(Shape::line(pts, stroke));
        }
    };
    let ring = |pts: Vec<Pos2>| {
        if pts.len() > 2 {
            p.add(Shape::closed_line(pts, stroke));
        }
    };

    match icon {
        Icon::Plus => {
            p.line_segment([at(0.5, 0.2), at(0.5, 0.8)], stroke);
            p.line_segment([at(0.2, 0.5), at(0.8, 0.5)], stroke);
        }
        Icon::Minus => {
            p.line_segment([at(0.22, 0.5), at(0.78, 0.5)], stroke);
        }
        Icon::Close => {
            p.line_segment([at(0.27, 0.27), at(0.73, 0.73)], stroke);
            p.line_segment([at(0.73, 0.27), at(0.27, 0.73)], stroke);
        }
        Icon::Check => {
            line(poly(&[(0.2, 0.53), (0.42, 0.75), (0.8, 0.28)]));
        }
        Icon::Back => {
            line(poly(&[(0.82, 0.5), (0.22, 0.5)]));
            line(poly(&[(0.45, 0.23), (0.21, 0.5), (0.45, 0.77)]));
        }
        Icon::Search => {
            p.circle_stroke(at(0.44, 0.44), w * 0.22, stroke);
            p.line_segment([at(0.62, 0.62), at(0.82, 0.82)], stroke);
        }
        Icon::Bell => {
            let mut body = poly(&[(0.25, 0.66), (0.31, 0.56), (0.31, 0.45)]);
            body.extend(arc(at(0.5, 0.45), w * 0.19, PI, TAU, 12));
            body.extend(poly(&[(0.69, 0.45), (0.69, 0.56), (0.75, 0.66)]));
            line(body);
            p.line_segment([at(0.2, 0.68), at(0.8, 0.68)], stroke);
            p.line_segment([at(0.44, 0.78), at(0.56, 0.78)], stroke);
        }
        Icon::Cart => {
            line(poly(&[(0.12, 0.22), (0.26, 0.22), (0.42, 0.64), (0.76, 0.64)]));
            line(poly(&[(0.32, 0.38), (0.86, 0.38), (0.76, 0.64)]));
            p.circle_stroke(at(0.46, 0.8), w * 0.07, stroke);
            p.circle_stroke(at(0.72, 0.8), w * 0.07, stroke);
        }
        Icon::Cup => {
            let mut body = poly(&[(0.6, 0.3), (0.6, 0.52)]);
            body.extend(arc(at(0.42, 0.52), w * 0.18, PI, 0.0, 14));
            body.push(at(0.24, 0.3));
            line(body);
            line(arc(at(0.68, 0.44), w * 0.11, -PI / 2.0, PI / 2.0, 8));
            p.line_segment([at(0.36, 0.14), at(0.36, 0.23)], stroke);
            p.line_segment([at(0.5, 0.11), at(0.5, 0.21)], stroke);
        }
        Icon::Bolt => {
            ring(poly(&[
                (0.56, 0.1),
                (0.28, 0.54),
                (0.46, 0.54),
                (0.44, 0.9),
                (0.72, 0.46),
                (0.54, 0.46),
            ]));
        }
        Icon::Pin => {
            p.circle_stroke(at(0.5, 0.38), w * 0.18, stroke);
            line(poly(&[(0.35, 0.52), (0.5, 0.88), (0.65, 0.52)]));
            p.circle_filled(at(0.5, 0.38), width * 0.6, color);
        }
        Icon::Gift => {
            p.rect_stroke(
                Rect::from_two_pos(at(0.2, 0.44), at(0.8, 0.86)),
                w * 0.06,
                stroke,
                StrokeKind::Middle,
            );
            p.line_segment([at(0.5, 0.44), at(0.5, 0.86)], stroke);
            p.circle_stroke(at(0.43, 0.32), w * 0.07, stroke);
            p.circle_stroke(at(0.57, 0.32), w * 0.07, stroke);
        }
        Icon::Tag => {
            ring(poly(&[
                (0.13, 0.47),
                (0.47, 0.13),
                (0.87, 0.13),
                (0.87, 0.53),
                (0.53, 0.87),
            ]));
            p.circle_filled(at(0.7, 0.3), width * 0.85, color);
        }
        Icon::Wallet => {
            p.rect_stroke(
                Rect::from_two_pos(at(0.14, 0.28), at(0.86, 0.76)),
                w * 0.11,
                stroke,
                StrokeKind::Middle,
            );
            p.line_segment([at(0.14, 0.44), at(0.86, 0.44)], stroke);
            p.circle_filled(at(0.7, 0.6), width * 0.85, color);
        }
        Icon::Chart => {
            let bar = |x: f32, top: f32| {
                p.line_segment([at(x, top), at(x, 0.74)], Stroke::new(width * 1.8, color));
                p.circle_filled(at(x, top), width * 0.9, color);
                p.circle_filled(at(x, 0.74), width * 0.9, color);
            };
            bar(0.3, 0.5);
            bar(0.5, 0.28);
            bar(0.7, 0.42);
        }
        Icon::Home => {
            line(poly(&[(0.13, 0.5), (0.5, 0.15), (0.87, 0.5)]));
            line(poly(&[(0.23, 0.44), (0.23, 0.87), (0.77, 0.87), (0.77, 0.44)]));
        }
        Icon::User => {
            p.circle_stroke(at(0.5, 0.34), w * 0.17, stroke);
            line(arc(at(0.5, 0.99), w * 0.33, PI, TAU, 16));
        }
        Icon::More => {
            for x in [0.26, 0.5, 0.74] {
                p.circle_filled(at(x, 0.5), width * 0.9, color);
            }
        }
        Icon::TrendUp => {
            line(poly(&[(0.08, 0.74), (0.38, 0.44), (0.58, 0.62), (0.9, 0.28)]));
            line(poly(&[(0.7, 0.26), (0.9, 0.26), (0.9, 0.46)]));
        }
        Icon::Sparkle => {
            ring(poly(&[
                (0.5, 0.08),
                (0.58, 0.42),
                (0.92, 0.5),
                (0.58, 0.58),
                (0.5, 0.92),
                (0.42, 0.58),
                (0.08, 0.5),
                (0.42, 0.42),
            ]));
        }
    }
}

/// Convenience: paint an icon centred in a square of `size` at `center`.
pub fn paint_centered(p: &Painter, center: Pos2, size: f32, icon: Icon, color: Color32, width: f32) {
    paint(p, Rect::from_center_size(center, Vec2::splat(size)), icon, color, width);
}
