package net.waterfoul.gear.gears2persistantnotifications;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.Html;
import android.text.Spannable;
import android.text.format.DateUtils;
import android.util.Log;

import com.samsung.android.sdk.SsdkUnsupportedException;
import com.samsung.android.sdk.richnotification.Srn;
import com.samsung.android.sdk.richnotification.SrnImageAsset;
import com.samsung.android.sdk.richnotification.SrnRichNotification;
import com.samsung.android.sdk.richnotification.SrnRichNotificationManager;
import com.samsung.android.sdk.richnotification.templates.SrnStandardSecondaryTemplate;
import com.samsung.android.sdk.richnotification.templates.SrnStandardTemplate;

import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

public class GearNotifications implements SrnRichNotificationManager.EventListener {
    static GearNotifications cur = null;
    public static ArrayList<Model> modelList = null;

    private static boolean checkStrings(CharSequence before, CharSequence after) {
        if(before == null) {
            return after == null;
        } else if (after == null) {
            return true;
        } else {
            return before.toString().compareTo(after.toString()) == 0;
        }
    }

    private static boolean checkStrings(String before, String after) {
        if(before == null) {
            return after == null;
        } else {
            return before.compareTo(after) == 0;
        }
    }

    public static Model fetchAndUpdateModel(
            int id,
            String pack,
            String ticker,
            CharSequence title,
            CharSequence text,
            CharSequence bigText,
            Bitmap bmp,
            boolean remove,
            SrnRichNotificationManager richNotificationManager,
            Context context
    ) {
        int length = modelList.size();
        Model model = null;
        if (length > 0) {
            model = modelList.get(0);
            int i = 1;
            while (model.id != id && !model.pack.equals(pack) && i < length) {
                model = modelList.get(i);
                i++;
            }
            if(model.id != id && !model.pack.equals(pack)) {
                model = null;
            } else if(remove) {
                model.pack = pack;
                model.ticker = "";
                model.title = "";
                model.text = "";
                model.bigText = "";
                model.image = null;
                model.removed = true;
                GearNotifications.remove(richNotificationManager, model, context);
            } else {
                model.pack = pack;
                model.ticker = ticker;
                model.title = title;
                model.text = text;
                model.bigText = bigText;
                model.image = bmp;
                model.removed = false;
            }
        }

        if(!remove && model == null) {
            model = new Model(id, pack, ticker, title, text, bigText, bmp);
            modelList.add(model);
        }

        return model;
    }


    public static GearNotifications getCurrent() {
        if (cur == null) {
            cur = new GearNotifications();
        }
        return cur;
    }

    public static void remove(
        SrnRichNotificationManager richNotificationManager,
        Model mdl,
        Context context
    ) {
        if(!mdl.removed || mdl.gearId == null) {
            return;
        }

        richNotificationManager.dismiss(mdl.gearId);
        mdl.gearId = null;

        richNotificationManager.stop();
    }

    public static void send(
            SrnRichNotificationManager richNotificationManager,
            Model mdl,
            boolean vibrate,
            Context context
    ) {
        if(mdl.removed) {
            return;
        }

        Srn srn = new Srn();
        try {
            // Initializes an instance of Srn.
            srn.initialize(context);
        } catch (SsdkUnsupportedException e) {
            switch (e.getType()) {
                case SsdkUnsupportedException.DEVICE_NOT_SUPPORTED:
                    return;
            }
        }

        SrnRichNotification myRichNotification;
        try {
            if (
                mdl.gearId != null &&
                (
                    mdl.lastUpdate.after( // Rate limit notifications from naughty apps (gmaps)
                        new Date(System.currentTimeMillis() - (1000))
                    ) ||
                    (
                        checkStrings(mdl.text, mdl.lastText) &&
                        checkStrings(mdl.bigText, mdl.lastBigText) &&
                        checkStrings(mdl.title, mdl.lastTitle)
                    )
                )
            ) {
                return;
            }

            if(mdl.gearId != null && checkStrings(mdl.title, mdl.lastTitle)) {
                vibrate = false;
            }
            myRichNotification = new SrnRichNotification(context);
            myRichNotification.setTitle(mdl.title.toString());
            SrnStandardTemplate myPrimaryTemplate = new SrnStandardTemplate(SrnStandardTemplate.HeaderSizeType.SMALL);
            if (Spannable.class.isInstance(mdl.text)) {
                myPrimaryTemplate.setBody(Html.toHtml(Spannable.class.cast(mdl.text)));
            } else {
                myPrimaryTemplate.setBody(mdl.text.toString());
            }
            if (mdl.image != null) {
                SrnImageAsset myAppIcon = new SrnImageAsset(
                    context,
                    mdl.pack + ".noti.icon." + mdl.id,
                    mdl.image
                );
                myRichNotification.setIcon(myAppIcon);
            }
            myRichNotification.setPrimaryTemplate(myPrimaryTemplate);
            if(mdl.bigText != null && !mdl.bigText.equals("")) {
                SrnStandardSecondaryTemplate mySecondaryTemplate = new SrnStandardSecondaryTemplate();
                mySecondaryTemplate.setTitle("-------------------");
                mySecondaryTemplate.setSubHeader("");
                if (Spannable.class.isInstance(mdl.bigText)) {
                    mySecondaryTemplate.setBody(Html.toHtml(Spannable.class.cast(mdl.bigText)));
                } else if (mdl.bigText != null) {
                    mySecondaryTemplate.setBody(mdl.bigText.toString());
                }
                myRichNotification.setSecondaryTemplate(mySecondaryTemplate);
            }
            myRichNotification.setAssociatedAndroidNotification(mdl.id);
            if (vibrate) {
                myRichNotification.setAlertType(SrnRichNotification.AlertType.SOUND_AND_VIBRATION, SrnRichNotification.PopupType.NORMAL);
            } else {
                myRichNotification.setAlertType(SrnRichNotification.AlertType.SILENCE, SrnRichNotification.PopupType.NONE);
            }
            if(mdl.gearId != null) {
                richNotificationManager.dismiss(mdl.gearId);
            }
            mdl.gearId = richNotificationManager.notify(myRichNotification);
            mdl.lastUpdate = new Date();
            mdl.lastText = mdl.text;
            mdl.lastBigText = mdl.bigText;
            mdl.lastTitle = mdl.title;
        } catch(Error e) {
            Log.e("GearNotifications", "Failed to notify " + mdl.id + '/' + mdl.pack + ' ' + e.toString());
        }
    }
    @Override
    public void onRemoved(UUID uuid) {
        int length = modelList.size();
        Model model = null;
        if (length > 0) {
            model = modelList.get(0);
            int i = 1;
            while (model.gearId != uuid && i < length) {
                model = modelList.get(i);
                i++;
            }

            if (model.gearId == uuid) {
                model.gearId = null;
            }
        }
    }
    @Override
    public void onRead(UUID uuid) {
    }
    @Override
    public void onError(UUID uuid, SrnRichNotificationManager.ErrorType error) {
    }
}
