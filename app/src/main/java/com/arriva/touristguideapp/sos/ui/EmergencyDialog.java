package com.arriva.touristguideapp.sos.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.os.CountDownTimer;
import com.arriva.touristguideapp.sos.manager.SOSManager;
import com.arriva.touristguideapp.sos.utils.Logger;
import com.arriva.touristguideapp.R;

public class EmergencyDialog {
    public static void showConfirmation(Context context) {
        if (context == null) return;
        
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(context.getString(R.string.emergency_sos_title));
            builder.setMessage(context.getString(R.string.sos_triggering_countdown, 3));
            builder.setNegativeButton(context.getString(R.string.cancel), (dialog, which) -> {
                dialog.dismiss();
            });

            AlertDialog dialog = builder.create();
            dialog.show();

            new CountDownTimer(3000, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    try {
                        if (dialog.isShowing()) {
                            dialog.setMessage(context.getString(R.string.sos_triggering_countdown, (millisUntilFinished / 1000 + 1)));
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
