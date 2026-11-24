# Custom Compose interface

Verdant 2.1 uses Compose Foundation and its own visual components. The Material 3 and Material icon libraries are not dependencies. `BespokeUi.kt` implements text, buttons, bordered fields, selection controls, switches, sliders, menus, toast messages, dialogs, sheets, bottom navigation, and a calendar. `BrandIcons.kt` contains consistent custom line vectors.

Poppins, dark translucent panels, restrained lime accents, thin borders, and rounded geometry follow the user's reference. There are no Material theme tokens, floating Material text-field labels, Material sliders, Material navigation bars, or Material date pickers. Foreground labels retain contrast against the liquid backdrop.

Interactive controls use Compose semantics for roles, selection, editable text, progress, disabled states, and announcements. Dialogs use the platform Compose window on Android and an in-window popup on desktop, allowing the shared interface to be rendered in headless UI tests. Forms remain bounded and scrollable around the keyboard.

Home quick actions make both icon and label tappable. Home links intentionally select the current month, while ordinary tab navigation preserves the user's selected period and filters. Home keeps its balance header on the liquid backdrop and uses a uniform near-black surface beneath it. Its transaction rows sit directly on that surface with thin separators; there is no promotional banner or enclosing transaction card. The liquid backdrop remains on the other screens. Currency labels and category cards adapt to large amounts and larger text.

Financial persistence is independent of closing animations. A canceled animation after a successful save cannot turn the committed write into a false failure message. Actual task cancellation propagates, failed writes preserve drafts, and stable draft IDs keep retries idempotent.
