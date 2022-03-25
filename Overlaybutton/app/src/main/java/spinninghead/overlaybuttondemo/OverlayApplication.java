package spinninghead.overlaybuttondemo;


import android.app.Application;

import spinninghead.overlaybutton.OverlayButtonManager;

/**
 * Created by donmatthews on 11/6/17.
 */

public class OverlayApplication extends Application {

    OverlayButtonManager overlayManager = null;

    protected OverlayButtonManager getOverlayManager() {

        if (overlayManager==null) {
            overlayManager = new OverlayButtonManager(null, R.drawable.overlay_icon, 0);
        }

        return overlayManager;
    }



}
