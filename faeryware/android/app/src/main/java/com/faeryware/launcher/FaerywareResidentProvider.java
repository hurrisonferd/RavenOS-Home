package com.faeryware.launcher;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Process;

import com.faeryware.launcher.resident.ResidentEventBus;
import com.faeryware.launcher.resident.ResidentOfficeBridge;
import com.faeryware.launcher.resident.ResidentRuntime;
import com.faeryware.launcher.resident.ResidentSignalStore;

/**
 * Read-only, deliberately tiny inter-app surface for HOUSE mode.
 * Exposes only resident presentation state to the reviewed HOUSE launcher package(s).
 * No private memory, notification bodies, page text, keys, write methods, or model credentials.
 */
public final class FaerywareResidentProvider extends ContentProvider {
    public static final String AUTHORITY = "com.faeryware.launcher.resident";
    public static final Uri STATE_URI = Uri.parse("content://" + AUTHORITY + "/state");

    private static final String[] COLUMNS = {
        "available",
        "resident_index",
        "resident_name",
        "resident_stamp",
        "lane",
        "visual_state",
        "color",
        "summary",
        "office_state",
        "speech",
        "source_sha",
        "updated_at"
    };

    @Override public boolean onCreate() { return true; }

    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Context context = providerContext();
        enforceReader(context);
        if (uri == null || !AUTHORITY.equals(uri.getAuthority()) || !"state".equals(uri.getLastPathSegment())) {
            throw new IllegalArgumentException("unsupported resident URI");
        }

        boolean contextMode = context.getSharedPreferences(FaerywareMemoryStore.PUBLIC_PREFS, Context.MODE_PRIVATE)
            .getBoolean(FaerywareMemoryStore.KEY_CONTEXT_MODE, true);
        int resident = ResidentRuntime.activeResident(context, contextMode);
        String owner = ResidentRuntime.name(resident);
        int visualState = ResidentRuntime.visualState(context, resident);
        ResidentOfficeBridge.SoakState officeState = ResidentOfficeBridge.state(context, owner);
        String verifiedSpeech = ResidentOfficeBridge.verifiedSpeech(context, owner);

        MatrixCursor cursor = new MatrixCursor(COLUMNS, 1);
        cursor.addRow(new Object[] {
            1,
            resident,
            owner,
            ResidentRuntime.stamp(resident),
            ResidentRuntime.lane(resident),
            ResidentRuntime.visualStateName(visualState),
            ResidentRuntime.color(resident),
            ResidentSignalStore.summary(context),
            officeState.name(),
            verifiedSpeech == null ? "" : verifiedSpeech,
            ResidentOfficeBridge.sourceSha(context, owner),
            Math.max(ResidentEventBus.lastAt(context), Math.max(ResidentSignalStore.foregroundAt(context), ResidentSignalStore.notificationAt(context)))
        });
        cursor.setNotificationUri(context.getContentResolver(), STATE_URI);
        return cursor;
    }

    @Override public String getType(Uri uri) { return "vnd.android.cursor.item/vnd.faeryware.resident.state"; }
    @Override public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException("read only"); }
    @Override public int delete(Uri uri, String selection, String[] selectionArgs) { throw new UnsupportedOperationException("read only"); }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) { throw new UnsupportedOperationException("read only"); }

    private Context providerContext() {
        Context context = getContext();
        if (context == null) throw new IllegalStateException("provider context unavailable");
        return context;
    }

    private void enforceReader(Context context) {
        int uid = Binder.getCallingUid();
        if (uid == Process.myUid()) return;
        String[] packages = context.getPackageManager().getPackagesForUid(uid);
        if (packages != null) {
            for (String pkg : packages) {
                if ("com.ravenos.launcher".equals(pkg) ||
                    "com.iappyx.launcher".equals(pkg) ||
                    "com.faeryware.house".equals(pkg)) return;
            }
        }
        throw new SecurityException("Faeryware resident state is not exported to this caller");
    }
}
