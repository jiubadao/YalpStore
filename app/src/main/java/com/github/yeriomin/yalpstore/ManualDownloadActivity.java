package com.github.yeriomin.yalpstore;

import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.github.yeriomin.yalpstore.fragment.details.DownloadOrInstall;
import com.github.yeriomin.yalpstore.model.App;

import java.util.Timer;
import java.util.TimerTask;

public class ManualDownloadActivity extends DetailsActivity {

    @Override
    protected void onNewIntent(Intent intent) {
        if (null == DetailsActivity.app) {
            Log.e(getClass().getName(), "No app stored");
            finish();
            return;
        }
        draw(DetailsActivity.app);
    }

    private void draw(App app) {
        setTitle(app.getDisplayName());
        setContentView(R.layout.manual_download_activity_layout);
        app.setOfferType(1);
        ((TextView) findViewById(R.id.compatibility)).setText(
            app.getVersionCode() > 0
                ? R.string.manual_download_compatible
                : R.string.manual_download_incompatible
        );
        if (app.getVersionCode() > 0) {
            ((EditText) findViewById(R.id.version_code)).setHint(String.valueOf(app.getVersionCode()));
        }
        downloadOrInstallFragment = new DownloadOrInstall(this, app);
        ManualDownloadTextWatcher textWatcher = new ManualDownloadTextWatcher(
            app,
            (Button) findViewById(R.id.download),
            (Button) findViewById(R.id.install),
            downloadOrInstallFragment
        );
        String versionCode = Integer.toString(app.getVersionCode());
        textWatcher.onTextChanged(versionCode, 0, 0, versionCode.length());
        ((EditText) findViewById(R.id.version_code)).addTextChangedListener(textWatcher);
        downloadOrInstallFragment.registerReceivers();
        downloadOrInstallFragment.draw();
    }

    @Override
    public void redrawDetails(App app) {
        if (null == downloadOrInstallFragment) {
            return;
        }
        downloadOrInstallFragment.draw();
    }

    static private class ManualDownloadTextWatcher implements TextWatcher {

        static private final int TIMEOUT = 1000;

        private final App app;
        private final Button downloadButton;
        private final Button installButton;
        private DownloadOrInstall downloadOrInstallFragment;
        private Timer timer;

        public ManualDownloadTextWatcher(App app, Button downloadButton, Button installButton, DownloadOrInstall downloadOrInstallFragment) {
            this.app = app;
            this.downloadButton = downloadButton;
            this.installButton = installButton;
            this.downloadOrInstallFragment = downloadOrInstallFragment;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            try {
                app.setVersionCode(Integer.parseInt(s.toString()));
                installButton.setVisibility(View.GONE);
                downloadButton.setText(R.string.details_download_checking);
                downloadButton.setEnabled(false);
                downloadButton.setVisibility(View.VISIBLE);
                restartTimer();
            } catch (NumberFormatException e) {
                Log.w(getClass().getName(), s.toString() + " is not a number");
            }
        }

        @Override
        public void afterTextChanged(Editable s) {

        }

        private void restartTimer() {
            if (null != timer) {
                timer.cancel();
            }
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    getTask(timer).execute();
                }
            }, TIMEOUT);
        }

        private PurchaseCheckTask getTask(Timer timer) {
            PurchaseCheckTask task = new PurchaseCheckTask();
            task.setContext(downloadButton.getContext());
            task.setTimer(timer);
            task.setApp(app);
            task.setDownloadOrInstallFragment(downloadOrInstallFragment);
            task.setDownloadButton(downloadButton);
            return task;
        }
    }
}
