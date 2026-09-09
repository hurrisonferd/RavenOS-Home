# Faeryware Device Haunt v4 — Resident Phone Goblins

Status: PUBLIC-SAFE IMPLEMENTATION PACKET

Goal: make Faeryware feel resident rather than like a themed app.

## Surfaces
- Default HOME launcher with one-swipe Haunt Feed and app catacombs.
- Live wallpaper using the active Fae chibi renderer.
- Context-aware widget.
- Quick Settings haunt level tile.
- User-started resident overlay with a main draggable chibi and edge-dwelling companions.
- Haunt Console for all special-access controls.

## Explicit awareness
Optional Accessibility Service:
- observes window/app package changes only
- `canRetrieveWindowContent=false`
- does not copy view text, typed text, passwords, or keystrokes

Optional Notification Listener:
- records source package/app + timestamp only
- does not copy notification message bodies

## Local memory
A small ring buffer remembers recent foreground app packages and notification-source packages locally.
There is no network transport in this layer.
The Haunt Console can clear it.

## Resident behavior
CALM:
- main resident only
- no companion swarm

HAUNTED:
- main resident + one non-interactive edge companion

FERAL:
- main resident + two non-interactive edge companions
- faster, wider bounded movement

Main resident:
- drag anywhere
- snaps to nearest screen edge
- tap opens Faeryware
- double tap cycles Fae override
- long press cycles MINI / BUBBLE / CARD
- follow-context action clears manual override

## Reboot
Start-after-reboot is OFF until the user enables it in the Haunt Console.
It only attempts to restore residents when Android overlay access is already granted.
Android remains free to defer the foreground service.

## Boundaries
- no root
- no device admin
- no hidden fullscreen intercept
- no arbitrary command execution
- no other-app touch/key scraping
- no silent HOME takeover
- every special access can be revoked from Android Settings
