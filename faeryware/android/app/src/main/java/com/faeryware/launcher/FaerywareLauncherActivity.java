package com.faeryware.launcher;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/** App icon entry only. One UI remains HOME; Faeryware lives as an opt-in overlay colony. */
public final class FaerywareLauncherActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        startActivity(new Intent(this, FaerywareHauntConsoleActivity.class));
        finish();
    }
}
