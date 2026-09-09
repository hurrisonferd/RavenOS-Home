# Faeryware Android — Overlay Colony v5

Authority: RAVEN
Target: Samsung Galaxy S23 Ultra / regular Android launcher substrate
Mode: OVERLAY-FIRST

## Product correction

Faeryware v5 does not try to visually replace Samsung One UI. One UI remains the normal HOME surface. Faeryware becomes the inhabitant layer: small explicit TYPE_APPLICATION_OVERLAY windows containing Digi Fae sticker residents.

## Visual law

Normal use must preserve the user's existing wallpaper, app icons, folders, dock, search, spacing and Samsung gestures. Administrative controls belong in Ghost House, not on the home screen.

The Fae are the UI chrome. Raven-supplied Digi Fae sticker states are source-bound assets, not regenerated identity substitutes.

## Colony modes

HOME_ONLY — show residents over the normal launcher when app-awareness can identify Home.
FOLLOW_ME — allow residents to remain over ordinary apps too.

CALM — main resident only.
HAUNTED — main + 1 passive companion.
FERAL — main + 2 passive companions.
APOCALYPSE — all six Fae inhabit the screen in bounded small windows.

## Interaction

Main resident:
- tap → next expression
- double tap → next Fae
- drag → move
- release → snap to nearest perch
- long press → open Ghost House

Butterfly edge handle → Ghost House.

Passive companions do not consume touch input.

## Privacy / safety boundary

- no root
- no device admin
- no silent HOME takeover
- no invisible fullscreen overlay
- overlay windows are small and visible
- only the main resident and edge handle are touchable
- Accessibility awareness records foreground package/app label only
- notification awareness records source app + time only
- no notification bodies, keystrokes, passwords, page text, or other-app touch interception
- all special access is explicit Android user consent and revocable in Settings

## Proof boundary

Source binding does not prove S23 rendering. APK build and device behavior require observed build/install proof.
