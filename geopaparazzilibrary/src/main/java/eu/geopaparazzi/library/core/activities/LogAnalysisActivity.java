/*
 * Geopaparazzi - Digital field mapping on Android based devices
 * Copyright (C) 2016  HydroloGIS (www.hydrologis.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package eu.geopaparazzi.library.core.activities;

import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import eu.geopaparazzi.library.GPApplication;
import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.util.Compat;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.library.util.Utilities;

/**
 * A log list activity.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class LogAnalysisActivity extends ListActivity implements View.OnClickListener {
    private static final int COLOR_ERROR = Color.RED;
    private static final int COLOR_CHECK = Color.CYAN;
    private static final int COLOR_INFO = Color.GREEN;
    private static final int COLOR_GPS = Color.YELLOW;
    private static final int COLOR_MEMORY = Color.LTGRAY;
    private static final int COLOR_ANOMALY = Color.MAGENTA;

    private ToggleButton errorToggleButton;
    private ToggleButton gpsToggleButton;
    private ToggleButton infoToggleButton;
    private ToggleButton checkToggleButton;
    private ToggleButton anomalyToggleButton;
    private ToggleButton memoryToggleButton;

    private boolean showError;
    private boolean showSession;
    private boolean showEvento;
    private boolean showSendCleanup;
    private boolean showAnomalie;
    private boolean showMemory;
    private String query;
    private SQLiteDatabase database;
    private List<String> messagesList;
    private ProgressDialog importDialog;

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.log_list);

        query = GPLog.getLogQuery();

        try {
            database = GPApplication.getInstance().getDatabase();
            if (!database.isOpen()) {
                database = null;
            }
        } catch (Exception e) {
            GPLog.error(this, null, e);
        }

        messagesList = new ArrayList<String>();

        LinearLayout errorToggleLayout = (LinearLayout) findViewById(R.id.errorToggleLayout);
        errorToggleLayout.setBackgroundColor(COLOR_ERROR);
        LinearLayout gpsToggleLayout = (LinearLayout) findViewById(R.id.gpsToggleLayout);
        gpsToggleLayout.setBackgroundColor(COLOR_GPS);
        LinearLayout infoToggleLayout = (LinearLayout) findViewById(R.id.infoToggleLayout);
        infoToggleLayout.setBackgroundColor(COLOR_INFO);
        LinearLayout checkToggleLayout = (LinearLayout) findViewById(R.id.checkToggleLayout);
        checkToggleLayout.setBackgroundColor(COLOR_CHECK);
        LinearLayout anomalyToggleLayout = (LinearLayout) findViewById(R.id.anomalieToggleLayout);
        anomalyToggleLayout.setBackgroundColor(COLOR_ANOMALY);
        LinearLayout memoryToggleLayout = (LinearLayout) findViewById(R.id.memoryToggleLayout);
        memoryToggleLayout.setBackgroundColor(COLOR_MEMORY);

        errorToggleButton = (ToggleButton) findViewById(R.id.errorToggleButton);
        errorToggleButton.setChecked(true);
        errorToggleButton.setOnClickListener(this);
        gpsToggleButton = (ToggleButton) findViewById(R.id.gpsToggleButton);
        gpsToggleButton.setChecked(false);
        gpsToggleButton.setOnClickListener(this);
        infoToggleButton = (ToggleButton) findViewById(R.id.infoToggleButton);
        infoToggleButton.setChecked(false);
        infoToggleButton.setOnClickListener(this);
        checkToggleButton = (ToggleButton) findViewById(R.id.checkToggleButton);
        checkToggleButton.setChecked(false);
        checkToggleButton.setOnClickListener(this);
        anomalyToggleButton = (ToggleButton) findViewById(R.id.anomalieToggleButton);
        anomalyToggleButton.setChecked(false);
        anomalyToggleButton.setOnClickListener(this);
        memoryToggleButton = (ToggleButton) findViewById(R.id.memoryToggleButton);
        memoryToggleButton.setChecked(false);
        memoryToggleButton.setOnClickListener(this);

        setButtonColor(errorToggleButton);

        FloatingActionButton refreshButton = (FloatingActionButton) findViewById(R.id.refreshButton);
        refreshButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                refreshListWithSpin();
            }
        });
        refreshListWithSpin();
    }

    @Override
    protected void onPause() {
        if (importDialog != null && importDialog.isShowing()) {
            importDialog.dismiss();
        }
        super.onPause();
    }

    private void refreshListWithSpin() {
        importDialog = new ProgressDialog(this);
        importDialog.setCancelable(true);
        importDialog.setTitle("Reading data...");
        importDialog.setMessage("");
        importDialog.setCancelable(false);
        importDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        importDialog.setIndeterminate(true);
        importDialog.show();

        new AsyncTask<String, Void, String>() {

            protected String doInBackground(String... params) {
                try {
                    refreshList();
                    return "";
                } catch (Exception e) {
                    GPLog.error(this, null, e);
                    return "ERROR: " + e.getLocalizedMessage();
                }
            }

            protected void onPostExecute(String response) { // on UI thread!
                if (importDialog != null && importDialog.isShowing()) {
                    importDialog.dismiss();
                }
                if (response.startsWith("ERROR")) {
                    GPDialogs.warningDialog(getApplicationContext(), response, new Runnable() {
                        public void run() {
                            finish();
                        }
                    });
                } else {
                    try {
                        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.log_row,
                                messagesList) {
                            @Override
                            public View getView(final int position, View cView, ViewGroup parent) {
                                LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                                final TextView textView = (TextView) inflater.inflate(R.layout.log_row, null);
                                String message = messagesList.get(position);
                                int color = getColor(message);
                                textView.setBackgroundColor(color);
                                textView.setText(message);
                                return textView;
                            }
                        };
                        setListAdapter(arrayAdapter);
                    } catch (Exception e) {
                        GPLog.error(this, null, e);
                    }
                }
            }
        }.execute((String) null);

    }

    private void checkSelections() {
        showError = errorToggleButton.isChecked();
        showSession = gpsToggleButton.isChecked();
        showEvento = infoToggleButton.isChecked();
        showSendCleanup = checkToggleButton.isChecked();
        showAnomalie = anomalyToggleButton.isChecked();
        showMemory = memoryToggleButton.isChecked();
    }

    private void refreshList() {
        if (database != null && query != null) {
            messagesList.clear();
            checkSelections();
            Cursor cursor = database.rawQuery(query, null);
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                String logMessage = getLogMessage(cursor);
                if (messageOk(logMessage)) {
                    messagesList.add(logMessage);
                }
                cursor.moveToNext();
            }
            cursor.close();
        }
    }

    private boolean messageOk(String logMessage) {
        boolean allFalse = //
                !showError && //
                        !showSession && //
                        !showEvento && //
                        !showSendCleanup && //
                        !showAnomalie && //
                        !showMemory;
        String logMessageLC = logMessage.toLowerCase();
        if (isGps(logMessageLC)) {
            if (!allFalse && !showSession) {
                return false;
            }
        } else if (isInfo(logMessageLC)) {
            if (!allFalse && !showEvento) {
                return false;
            }
        } else if (isCheck(logMessageLC)) {
            if (!allFalse && !showSendCleanup) {
                return false;
            }
        } else if (isError(logMessageLC, logMessage)) {
            if (!allFalse && !showError) {
                return false;
            }
        } else if (isAnomaly(logMessageLC)) {
            if (!allFalse && !showAnomalie) {
                return false;
            }
        } else if (isMemory(logMessageLC)) {
            if (!allFalse && !showMemory) {
                return false;
            }
        } else {
            if (!allFalse) {
                return false;
            }
        }
        return true;
    }

    private static int getColor(String logMessage) {
        int color = Color.WHITE;
        String logMessageLC = logMessage.toLowerCase();
        if (isError(logMessageLC, logMessage)) {
            color = COLOR_ERROR;
        } else if (isGps(logMessageLC)) {
            color = COLOR_GPS;
        } else if (isInfo(logMessageLC)) {
            color = COLOR_INFO;
        } else if (isCheck(logMessageLC)) {
            color = COLOR_CHECK;
        } else if (isAnomaly(logMessageLC)) {
            color = COLOR_ANOMALY;
        } else if (isMemory(logMessageLC)) {
            color = COLOR_MEMORY;
        }
        return color;
    }

    private static boolean isGps(String logMessageLC) {
        return logMessageLC.contains("gps");
    }

    private static boolean isMemory(String logMessageLC) {
        return logMessageLC.contains("memory pss") || //
                logMessageLC.contains("mem@") //
                ;
    }

    private static boolean isInfo(String logMessageLC) {
        return logMessageLC.contains("daotrackoid: evento aggiunto") || //
                logMessageLC.contains("daotrackoid: eventi made clean") || //
                logMessageLC.contains("daotrackoid: closed evento");
    }

    private static boolean isCheck(String logMessageLC) {
        return logMessageLC.contains("customtiledownloader called with");
    }

    private static boolean isError(String logMessageLC, String logMessage) {
        return logMessageLC.contains("error") || //
                logMessage.contains("Exception") || //
                logMessageLC.contains("problem");
    }

    private static boolean isAnomaly(String logMessageLC) {
        return logMessageLC.contains("emergenza");
    }

    private static String getLogMessage(Cursor cursor) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cursor.getColumnCount(); i++) {
            String field = cursor.getColumnName(i);
            if (field.equals("_id")) {
                continue;
            }
            String value = cursor.getString(i);
            sb.append("\n").append(value);
        }
        String text = "";
        if (sb.length() > 1) {
            text = sb.substring(1);
        }
        return text;
    }

    @Override
    public void onClick(View v) {
        if (v instanceof ToggleButton) {
            ToggleButton toggleButton = (ToggleButton) v;

            setButtonColor(toggleButton);
        }

    }

    private void setButtonColor(ToggleButton toggleButton) {
        if (toggleButton.isChecked()) {
            toggleButton.setBackground(Compat.getDrawable(this, R.drawable.button_background_drawable_selected));
        } else {
            toggleButton.setBackground(Compat.getDrawable(this, R.drawable.button_background_drawable));
        }
    }
}
