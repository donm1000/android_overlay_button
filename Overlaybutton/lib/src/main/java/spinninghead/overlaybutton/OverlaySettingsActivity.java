package spinninghead.overlaybutton;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;

/**
 * This class is used to show a settings screen for the overlay button
 */
public class OverlaySettingsActivity extends Activity {

    static String Key_Overlay = "overlaybutton";
    static String Overlay_Size_Ratio = "overlaySizeRatio";
    static public String EXTRA_ICON_RES_ID = "iconID";

    OverlayButtonManager overlayManager = null;

    int iconId = 0;
    float ratio = 1;

    /**
     * Retrieves overlay button enabled from shared preferences
     * @param context
     * @return
     */
    static public boolean IsOverlayEnabled(Context context) {

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        boolean overlay = sharedPref.getBoolean(Key_Overlay, false);

        if (overlay&&CheckDrawOverlayPermission(context)) {
            overlay = true;
        } else {
            overlay = false;
        }

        return overlay;
    }

    /**
     * Convenience method to determine if app has draw overlay permission
     * @param context
     * @return
     */
    public static boolean CheckDrawOverlayPermission(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (!Settings.canDrawOverlays(context)) {
            return false;
        } else {
            return true;
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_overlay_settings);

        iconId = getIntent().getIntExtra(EXTRA_ICON_RES_ID, 0);

        Switch switchOverlay = (Switch) findViewById(R.id.switchOverlay);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean overlay = sharedPref.getBoolean(Key_Overlay, false);
        ratio = sharedPref.getFloat(Overlay_Size_Ratio, 1);

        if (overlay&&CheckDrawOverlayPermission(this)) {
            switchOverlay.setChecked(overlay);
            setUIForOverlayEnabled();
        } else {
            SharedPreferences.Editor edit = sharedPref.edit();
            edit.putBoolean(Key_Overlay, false);
            edit.commit();
            switchOverlay.setChecked(false);
            setUIForOverlayDisabled();
        }


        switchOverlay.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (!CheckDrawOverlayPermission(buttonView.getContext())) {

                        AlertDialog.Builder builder = new AlertDialog.Builder(buttonView.getContext());

                        builder.setPositiveButton("Ok, take me there", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                showSystemOverlaySettings();
                            }
                        });
                        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // User cancelled the dialog
                            }
                        });
                        builder.setMessage("You will need to enable this button to show over other apps in your phone settings.");

                        AlertDialog dialog = builder.create();

                        dialog.show();

                        buttonView.setChecked(false);

                        setUIForOverlayDisabled();
                    } else {
                        setUIForOverlayEnabled();

                        getOverlayManager().showNewButtonOverlay(buttonView.getContext(), 0);

                        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        SharedPreferences.Editor edit = sharedPref.edit();
                        edit.putBoolean(Key_Overlay, true);
                        edit.commit();

                    }
                } else {
                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    SharedPreferences.Editor edit = sharedPref.edit();
                    edit.putBoolean(Key_Overlay, false);
                    edit.commit();

                    setUIForOverlayDisabled();
                    overlayManager.removeOverlays(buttonView.getContext());
                }
            }
        });

        Button button = (Button) findViewById(R.id.btnChoosePosition);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                getOverlayManager().resetButtonPosition(v.getContext());

            }
        });

        SeekBar seekSize = (SeekBar) findViewById(R.id.seekSize);
        seekSize.setProgress((int) (ratio*50));

        seekSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                int progress = seekBar.getProgress();
                if (progress<15) {
                    progress = 15;
                }

                ratio = progress/50f;


                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                SharedPreferences.Editor edit = sharedPref.edit();
                edit.putFloat(Overlay_Size_Ratio, ratio);
                edit.commit();

                getOverlayManager().updateButtonSize(seekBar.getContext());

            }
        });
    }

    @Override
    protected void onPause() {

        if (overlayManager!=null) {
            overlayManager.removeOverlays(this);

            overlayManager = null;
        }

        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (IsOverlayEnabled(this)) {

            getOverlayManager().showNewButtonOverlay(this, 0);
        }
    }

    protected OverlayButtonManager getOverlayManager() {
        if (overlayManager==null) {
            overlayManager = new OverlayButtonManager(null, iconId, 0);
        }

        return overlayManager;

    }

    protected void showSystemOverlaySettings () {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    protected void setUIForOverlayEnabled() {
        findViewById(R.id.txtRepositionMessage).setEnabled(true);
        findViewById(R.id.txtExitMessage).setEnabled(true);
        findViewById(R.id.btnChoosePosition).setEnabled(true);
        findViewById(R.id.seekSize).setEnabled(true);
        findViewById(R.id.txtSizeLabel).setEnabled(true);
    }

    protected void setUIForOverlayDisabled() {
        findViewById(R.id.txtRepositionMessage).setEnabled(false);
        findViewById(R.id.txtExitMessage).setEnabled(false);
        findViewById(R.id.btnChoosePosition).setEnabled(false);
        findViewById(R.id.seekSize).setEnabled(false);
        findViewById(R.id.txtSizeLabel).setEnabled(false);
    }

}
