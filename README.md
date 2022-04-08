# Overlay Button
Overlay Button is an Android library that allows an app to show an action button type overlay over other apps or the home screen. The button would normally be used to navigate back to an app that is using a service for background processing, but it is capable of performing any android intent. 

- Show overlay button over other apps or the homescreen
- Integrated settings UI for the overlay
- Overlay size and position are user adjustable
- Integrated permissions request and management
- Demo app included


![Overlay Demo](https://github.com/donm1000/android_overlay_button/blob/main/readme_images/overlay_use.gif) ![Overlay Settings](https://github.com/donm1000/android_overlay_button/blob/main/readme_images/overlay_settings.gif)

## How To Use The Library
###  1) Add the library to your application build.gradle
```
implementation 'spinninghead:overlaybutton:0.1'
```

###  2) Add an instance variable in your service for OverlayButtonManager. The intent that will be performed when the button is tapped is specified in the OverlayButtonManager constructor. Implementing a singleton here is a nice option.
```
OverlayButtonManager overlayManager = null;
```
```
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
```    

###  3) Call methods in OverlayButtonManager as appropriate to show or hide the overlay. I've wrapped the code in convenience methods below.
```
    protected void showOverlay() {
        getButtonManager().showNewButtonOverlay(getApplicationContext(), 200l);
    }

    protected void removeOverlay() {
        getButtonManager().removeOverlays(getApplicationContext());
    }
```

### These are the broad strokes for implmenting the library, but it leaves out other necessary steps such as Activity to Service communication and details that might be specific to your app. The sequence below shows the steps taken to show or hide the overlay in the sample app.
### Sequence Diagram for Showing Overlay

<img width="1388" alt="Screen Shot 2022-04-06 at 4 02 42 PM" src="https://user-images.githubusercontent.com/71778976/162061210-8b16136c-0939-4382-86cc-07430fc7ef90.png">
