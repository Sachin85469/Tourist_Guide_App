package com.arriva.touristguideapp.sos.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.os.CountDownTimer;
import com.arriva.touristguideapp.sos.manager.SOSManager;
import com.arriva.touristguideapp.sos.utils.Logger;

public class EmergencyDialog {
    public static void showConfirmation(Context context) {
        if (context == null) return;
        
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("EMERGENCY SOS");
            builder.setMessage("Triggering SOS in 3 seconds...");
            builder.setNegativeButton("CANCEL", (dialog, which) -> {
                dialog.dismiss();
            });

            AlertDialog dialog = builder.create();
            dialog.show();

            new CountDownTimer(3000, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    try {
                        if (dialog.isShowing()) {
                            dialog.setMessage("Triggering SOS in " + (millisUntilFinished / 1000 + 1) + " seconds...");
                        }
                    } catch (Exception ignored) {}
                }

                @Override
                public void onFinish() {
                    try {
                        if (dialog.isShowing()) {
                            dialog.dismiss();
                            SOSManager.getInstance(context).triggerSOS("In-App Button");
                        }
                    } catch (Exception e) {
                        Logger.e("Error in EmergencyDialog countdown finish", e);
                    }
                }
            }.start();
        } catch (Exception e) {
            Logger.e("Error showing EmergencyDialog", e);
        }
    }
}
