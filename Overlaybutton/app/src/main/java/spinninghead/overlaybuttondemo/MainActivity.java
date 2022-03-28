package spinninghead.overlaybuttondemo;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.Button;

import spinninghead.overlaybutton.OverlayButtonManager;
import spinninghead.overlaybutton.OverlaySettingsActivity;

public class MainActivity extends Activity {

    OverlayButtonManager overlayManager = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button showButton = (Button)findViewById(R.id.showbutton);

        Button hideButton = (Button)findViewById(R.id.hideButton);

        Button startService = (Button)findViewById(R.id.startService);

        Button stopService = (Button)findViewById(R.id.stopService);

        Button settings = (Button) findViewById(R.id.btnSettings);

        showButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getOverlayManager().showNewButtonOverlay(view.getContext().getApplicationContext(), 0l);

            }
        });

        hideButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getOverlayManager().removeOverlays(view.getContext().getApplicationContext());

            }
        });

        startService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent serviceIntent = new Intent(v.getContext(), OverlayService.class);
                startService(serviceIntent);
            }
        });

        stopService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent serviceIntent = new Intent(v.getContext(), OverlayService.class);
                stopService(serviceIntent);
            }
        });

        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(v.getContext(), OverlaySettingsActivity.class);
                intent.putExtra(OverlaySettingsActivity.EXTRA_ICON_RES_ID, R.drawable.overlay_demo_button);
                v.getContext().startActivity(intent);
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();

        Intent intent = new Intent(OverlayService.ACTION);
        intent.putExtra(OverlayService.SERVICE_COMMAND, OverlayService.COMMAND_HIDE_OVERLAY);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);



        //checkDrawOverlayPermission();
    }

    @Override
    protected void onPause() {
        //getOverlayManager().showNewButtonOverlay(getApplicationContext());



        Intent intent = new Intent(OverlayService.ACTION);
        intent.putExtra(OverlayService.SERVICE_COMMAND, OverlayService.COMMAND_SHOW_OVERLAY);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        super.onPause();

        //((OverlayApplication) getApplication()).getOverlayManager().showNewButtonOverlay(getApplicationContext());

    }

    protected OverlayButtonManager getOverlayManager() {

        if (overlayManager==null) {
            overlayManager = new OverlayButtonManager(null, R.drawable.overlay_demo_button, 0);
        }

        return overlayManager;
    }


    public boolean checkDrawOverlayPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
            return false;
        } else {
            return true;
        }
    }
}
