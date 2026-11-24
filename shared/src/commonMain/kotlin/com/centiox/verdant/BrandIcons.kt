package com.centiox.verdant

import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.*
import androidx.compose.ui.unit.dp

/** Small, consistent line drawings for Verdant; no Material icon or widget dependency. */
private fun strokeIcon(name: String, vararg paths: String): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        .apply {
            paths.forEach { path ->
                addPath(
                    PathParser().parsePathString(path).toNodes(),
                    fill = null,
                    stroke = SolidColor(Color.White),
                    strokeLineWidth = 1.6f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }
        .build()

object Icons {
    object Outlined {
        val Home = strokeIcon("Home", "M3 11L12 3L21 11M5 10V21H10V15H14V21H19V10")
        val Add = strokeIcon("Add", "M12 5V19M5 12H19")
        val Close = strokeIcon("Close", "M6 6L18 18M6 18L18 6")
        val Check = strokeIcon("Check", "M5 12L10 17L20 6")
        val ArrowBack = strokeIcon("Back", "M19 12H5M11 6L5 12L11 18")
        val ArrowForward = strokeIcon("Forward", "M5 12H19M13 6L19 12L13 18")
        val NorthEast = strokeIcon("Outflow", "M5 19L19 5M7 5H19V17")
        val SouthWest = strokeIcon("Inflow", "M19 5L5 19M5 7V19H17")
        val ChevronLeft = strokeIcon("Previous", "M15 5L8 12L15 19")
        val ChevronRight = strokeIcon("Next", "M9 5L16 12L9 19")
        val ExpandMore = strokeIcon("Expand", "M6 9L12 15L18 9")
        val Search =
            strokeIcon("Search", "M10.5 3a7.5 7.5 0 1 0 0 15a7.5 7.5 0 1 0 0 -15M16 16L21 21")
        val Info =
            strokeIcon("Information", "M12 3a9 9 0 1 0 0 18a9 9 0 1 0 0 -18M12 11V17M12 7V7.01")
        val AccountBalanceWallet =
            strokeIcon(
                "Wallet",
                "M4 6H20V20H4a2 2 0 0 1 -2 -2V5a2 2 0 0 1 2 -2H17V6M16 11H22V16H16ZM18 13.5H18.01",
            )
        val Payments =
            strokeIcon(
                "Income",
                "M3 6H21V19H3ZM7 3H18M7 10H7.01M17 15H17.01M12 9a3 3 0 1 0 0 6a3 3 0 1 0 0 -6",
            )
        val ShoppingBag = strokeIcon("Bag", "M5 8H19L21 21H3ZM8 9V6a4 4 0 0 1 8 0V9")
        val ShoppingBasket =
            strokeIcon("Basket", "M3 9H21L18 21H6ZM7 9L11 3M17 9L13 3M9 13V17M15 13V17")
        val ReceiptLong =
            strokeIcon("Plan", "M5 3H19V21L16 19L13 21L10 19L7 21L5 19ZM9 7H15M9 11H15M9 15H12")
        val GridView = strokeIcon("More", "M4 4H9V9H4ZM15 4H20V9H15ZM4 15H9V20H4ZM15 15H20V20H15Z")
        val BarChart = strokeIcon("Insights", "M4 20V12M10 20V5M16 20V9M22 20V2")
        val CalendarMonth =
            strokeIcon(
                "Calendar",
                "M5 5H19a2 2 0 0 1 2 2V20H3V7a2 2 0 0 1 2 -2ZM7 2V8M17 2V8M3 10H21M7 14H8M12 14H13M17 14H18M7 17H8M12 17H13",
            )
        val NotificationsNone =
            strokeIcon("Alerts", "M6 10a6 6 0 0 1 12 0V16L20 19H4L6 16ZM10 22H14M12 2V4")
        val Tune =
            strokeIcon(
                "Preferences",
                "M3 6H8M12 6H21M3 12H14M18 12H21M3 18H6M10 18H21M8 3V9M14 9V15M6 15V21",
            )
        val Settings =
            strokeIcon(
                "Settings",
                "M9 3H15L16 6L19 7L21 12L19 15L18 18L15 19L12 21L7 19L6 16L3 15L2 10L4 7L7 6ZM12 8a4 4 0 1 0 0 8a4 4 0 1 0 0 -8",
            )
        val Palette =
            strokeIcon(
                "Palette",
                "M12 3a9 9 0 1 0 0 18H14a2 2 0 0 0 0 -4H13a2 2 0 0 1 0 -4H18a3 3 0 0 0 3 -3C21 6 17 3 12 3ZM7 10H7.01M10 6H10.01M15 6H15.01M6 14H6.01",
            )
        val ContentCopy = strokeIcon("Copy", "M9 8H21V21H9ZM4 16H3V3H15V4")
        val FileDownload = strokeIcon("Export", "M12 3V16M7 11L12 16L17 11M4 17V21H20V17")
        val FileUpload = strokeIcon("Restore", "M12 17V4M7 9L12 4L17 9M4 17V21H20V17")
        val DeleteOutline =
            strokeIcon("Delete", "M3 6H21M9 6V3H15V6M6 6L7 21H17L18 6M10 10V17M14 10V17")
        val Spa = strokeIcon("Leaf", "M5 20C3 10 7 4 20 3C20 16 14 21 5 20ZM5 20L16 9")
        val Restaurant =
            strokeIcon(
                "Dining",
                "M5 3V10M9 3V10M3 3V8a4 4 0 0 0 8 0V3M7 12V21M18 3C15 7 15 12 18 13H21V3M20 13V21",
            )
        val DirectionsBus =
            strokeIcon(
                "Transport",
                "M6 3H18a2 2 0 0 1 2 2V19H4V5a2 2 0 0 1 2 -2ZM4 11H20M8 6H16M7 15H7.01M17 15H17.01M7 19V22M17 19V22",
            )
        val Bolt = strokeIcon("Utilities", "M13 2L4 14H11L10 22L20 9H13Z")
        val FavoriteBorder = strokeIcon("Health", "M12 21L4 13C-2 6 5 -1 12 6C19 -1 26 6 20 13Z")
        val Movie =
            strokeIcon(
                "Entertainment",
                "M3 9H21V21H3ZM3 9L2 4L20 1L21 6ZM6 4L9 8M13 3L16 7M10 13L16 16L10 19Z",
            )
        val MoreHoriz = strokeIcon("Other", "M5 12H5.01M12 12H12.01M19 12H19.01")
    }
}
