/*
 * Faeryware HOUSE overlay for pinned iappyxOS Launcher.
 * iappyx upstream remains MIT-licensed; this bridge is RavenOS-Home integration code.
 */
package com.iappyx.launcher.faeryware;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.webkit.JavascriptInterface;

import org.json.JSONObject;

/** Read-only JS bridge for the single bundled `faeryware_resident` widget. */
public final class FaerywareResidentBridge {
    private static final Uri URI = Uri.parse("content://com.faeryware.launcher.resident/state");
    private final Context context;

    public FaerywareResidentBridge(Context context) {
        this.context = context.getApplicationContext();
    }

    @JavascriptInterface
    public String state() {
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(URI, null, null, null, null);
            if (cursor == null || !cursor.moveToFirst()) return unavailable("resident provider returned no row");
            JSONObject out = new JSONObject();
            out.put("available", getInt(cursor, "available", 0) == 1);
            out.put("residentIndex", getInt(cursor, "resident_index", -1));
            out.put("name", getString(cursor, "resident_name"));
            out.put("stamp", getString(cursor, "resident_stamp"));
            out.put("lane", getString(cursor, "lane"));
            out.put("visualState", getString(cursor, "visual_state"));
            out.put("color", getInt(cursor, "color", 0));
            out.put("summary", getString(cursor, "summary"));
            out.put("officeState", getString(cursor, "office_state"));
            out.put("speech", getString(cursor, "speech"));
            out.put("sourceSha", getString(cursor, "source_sha"));
            out.put("updatedAt", getLong(cursor, "updated_at", 0L));
            return out.toString();
        } catch (SecurityException e) {
            return unavailable("resident provider rejected launcher");
        } catch (Throwable e) {
            return unavailable("Faeryware colony unavailable");
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private static String unavailable(String reason) {
        try { return new JSONObject().put("available", false).put("reason", reason).toString(); }
        catch (Throwable ignored) { return "{\"available\":false}"; }
    }

    private static int getInt(Cursor c, String name, int fallback) {
        int i = c.getColumnIndex(name); return i < 0 || c.isNull(i) ? fallback : c.getInt(i);
    }
    private static long getLong(Cursor c, String name, long fallback) {
        int i = c.getColumnIndex(name); return i < 0 || c.isNull(i) ? fallback : c.getLong(i);
    }
    private static String getString(Cursor c, String name) {
        int i = c.getColumnIndex(name); return i < 0 || c.isNull(i) ? "" : c.getString(i);
    }
}
