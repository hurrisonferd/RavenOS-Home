package com.faeryware.launcher.resident;

/** Explicit surface/capability table. A resident never gains authority merely by occupying a surface. */
public final class ResidentCapabilityPolicy {
    public enum Surface { HOME_CELL, STOCK_WIDGET, WALLPAPER, OVERLAY, REACTION_CAPSULE, CLIPPINGS, SEARCH, GHOST_HOUSE }
    public enum Capability { RENDER, REACT_LOCAL, MOVE_SELF, MUTATE_LAYOUT, READ_NOTIFICATION_SOURCE, READ_FOREGROUND_APP, OWNER_NATIVE_SPEECH }

    private ResidentCapabilityPolicy() {}

    public static boolean allows(Surface surface, Capability capability) {
        switch (capability) {
            case RENDER:
            case REACT_LOCAL:
                return true;
            case MOVE_SELF:
                return surface == Surface.HOME_CELL || surface == Surface.OVERLAY;
            case MUTATE_LAYOUT:
                return surface == Surface.HOME_CELL;
            case READ_NOTIFICATION_SOURCE:
            case READ_FOREGROUND_APP:
                return surface != Surface.WALLPAPER;
            case OWNER_NATIVE_SPEECH:
                return false; // OfficeBridge must explicitly supply verified owner-native text.
            default:
                return false;
        }
    }
}
