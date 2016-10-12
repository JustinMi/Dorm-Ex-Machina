package edu.berkeley.destroyers.concrete.los.therememberer;

import android.content.Context;
import android.content.pm.PackageManager;

/**
 * Created by wilsonyan on 10/7/16.
 */
public class Tools {
    public static final String CALL_PERMISSION = "android.permission.CALL_PHONE";

    public static boolean checkIfPermissionGranted(String permission, Context context) {
        int res = context.checkCallingOrSelfPermission(permission);
        return (res == PackageManager.PERMISSION_GRANTED);
    }

}
