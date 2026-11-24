# Custom Compose interface

Verdant 2.2 uses Compose Foundation and its own visual components. The Material 3 and Material icon libraries are not dependencies. `BespokeUi.kt` implements text, buttons, bordered fields, selection controls, switches, sliders, menus, toast messages, dialogs, sheets, bottom navigation, and a calendar. `BrandIcons.kt` contains consistent custom line vectors.

Poppins, dark translucent panels, restrained lime accents, thin borders, and rounded geometry follow the user's reference. There are no Material theme tokens, floating Material text-field labels, Material sliders, Material navigation bars, or Material date pickers. Foreground labels retain contrast against the liquid headers. Appearance changes save quietly; failures still show an error.

Interactive controls use Compose semantics for roles, selection, editable text, progress, disabled states, and announcements. Dialogs use the platform Compose window on Android and an in-window popup on desktop, allowing the shared interface to be rendered in headless UI tests. Forms remain bounded and scrollable around the keyboard.

Home quick actions make both icon and label tappable. Home links intentionally select the current month, while ordinary tab navigation preserves the user's selected period and filters. Home keeps its balance header on the liquid backdrop and uses a uniform near-black surface beneath it. Its transaction rows sit directly on that surface with thin separators; there is no promotional banner or enclosing transaction card. Every other page now uses a clipped liquid header over the same solid content surface. Activity rows and budget/category rows have thin separators rather than enclosing cards. Settings uses flat appearance, profile, and data sections. Page titles match the Activity and Insights navigation labels. Currency labels and category rows adapt to large amounts and larger text.

Financial persistence is independent of closing animations. A canceled animation after a successful save cannot turn the committed write into a false failure message. Actual task cancellation propagates, failed writes preserve drafts, and stable draft IDs keep retries idempotent.

Motion includes directional page fades/slides, pressed-button scaling, animated selection colors, chart reveals, progress interpolation, and toast entrance/exit. Compose animation APIs honor the system duration scale. Continuous motion is limited to liquid headers/previews and can be disabled in Settings.

Scroll regression tests exercise actual swipe gestures, first/last item reachability, large text, a 1,000-row virtualized history, and native profile editing around the keyboard. End-of-page bounds are checked against bottom navigation.
