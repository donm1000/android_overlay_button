package spinninghead.overlaybuttondemo;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import spinninghead.overlaybutton.OverlayButtonManager;

/**
 * This is a sample/demo service for the OverlayButton library
 */
public class OverlayService extends Service {

    static String ACTION = "action";
    static String COMMAND_SHOW_OVERLAY = "show_overlay";
    static String COMMAND_HIDE_OVERLAY = "hide_overlay";
    static String SERVICE_COMMAND = "service_command";

    OverlayButtonManager overlayButtonManager = null;

    public OverlayService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Returns a singleton instance of OverlayButtonManager that will open the MainActivity when the overlay button is tapped
     * @return
     */
    protected OverlayButtonManager getButtonManager() {

        if (overlayButtonManager==null) {

            //Intent to be performed when overlay button is tapped
            Intent resultIntent = new Intent(getApplicationContext(), MainActivity.class);
            PendingIntent appIntent = PendingIntent.getActivity(getApplicationContext(), 0, resultIntent, 0);

            overlayButtonManager=new OverlayButtonManager(appIntent, R.drawable.overlay_demo_button, 0);
        }

        return overlayButtonManager;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        LocalBroadcastManager.getInstance(this).registerReceiver(
                mMessageReceiver, new IntentFilter(ACTION));



        return START_STICKY;
    }

    @Override
    public void onDestroy() {

        removeOverlay();

        overlayButtonManager = null;

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);

        // Tell the user we stopped.
        Toast.makeText(getApplicationContext(), "Overlay Service Stopped", Toast.LENGTH_SHORT).show();
    }

    protected void showOverlay() {

        getButtonManager().showNewButtonOverlay(getApplicationContext(), 200l);
    }

    protected void removeOverlay() {
        getButtonManager().removeOverlays(getApplicationContext());
    }


    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String command = intent.getStringExtra(SERVICE_COMMAND);

            if (COMMAND_HIDE_OVERLAY.equals(command)) {
                removeOverlay();
            } else if (COMMAND_SHOW_OVERLAY.equals(command)) {
                showOverlay();
            }

        }
    };

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (overlayButtonManager!=null) {
            getButtonManager().updateOrientation(getApplicationContext());
        }
    }
}
