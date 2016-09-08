package net.waterfoul.gear.gears2persistantnotifications;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Pair;
import android.widget.RemoteViews;

import com.samsung.android.sdk.richnotification.SrnRichNotificationManager;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class NotificationService extends NotificationListenerService {
    SrnRichNotificationManager richNotificationManager;
    Context context;
    SharedPreferences settings = null;
    private final ReentrantLock lock = new ReentrantLock();

    @Override
    public void onListenerConnected () {
        Log.d("Notify", "Connected");
        context = getApplicationContext();

        richNotificationManager = new SrnRichNotificationManager(context);
        richNotificationManager.registerRichNotificationListener(GearNotifications.getCurrent());
        richNotificationManager.start();

        settings = context.getSharedPreferences("enabledApps", 0);

        if(GearNotifications.modelList == null) {
            GearNotifications.modelList = new ArrayList<>();
        }

        StatusBarNotification[] active = getActiveNotifications();
        for (StatusBarNotification cur: active) {
            onNotificationPosted(cur);
        }
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if ( !sbn.isOngoing() ) {
            return;
        }
        lock.lock();
        try {

            Notification ntfy = sbn.getNotification();

            String pack = sbn.getPackageName();
            String ticker = "";
            if (ntfy.tickerText != null) {
                ticker = ntfy.tickerText.toString();
            }
            Bundle extras = ntfy.extras;
            CharSequence title = extras.getCharSequence(Notification.EXTRA_TITLE);
            CharSequence text = extras.getCharSequence(Notification.EXTRA_TEXT);
            CharSequence bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
            CharSequence[] lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
            Icon icon = ntfy.getSmallIcon();
            if (icon == null) {
                icon = ntfy.getLargeIcon();
            }

            Bitmap iconBmp = iconToBitmap(icon, context);

            if (pack.equals("com.google.android.apps.maps")) {
                Pair<Bitmap, List<String>> ret = null;
                if (ntfy.bigContentView != null) {
                    ret = getText(ntfy.bigContentView);
                } else if (ntfy.contentView != null) {
                    ret = getText(ntfy.contentView);
                }
                if (ret != null) {
                    if (ret.first != null) {
                        iconBmp = ret.first;
                    }

                    String distance = ret.second.get(1);
                    String time = ret.second.get(2);
                    String[] info = ret.second.get(3).split(" [·-] ");

                    if (info[2].startsWith("toward")) {
                        title = "Head " + info[2];
                    } else {
                        title = info[2];
                    }

                    text = "";
                    if (!distance.equals("0 ft") && !distance.equals("")) {
                        text = text + "In " + distance + " - ";
                    }

                    text = title + "\n" + text + info[1] + " (" + time + ") " + info[0] + " left";
                }
            }

            if (bigText != null && !bigText.equals("")) {
                bigText = bigText.toString().replace(text, "");
            }

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            iconBmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] byteArray = stream.toByteArray();

            Intent msgrcv = new Intent("Msg");
            msgrcv.putExtra("id", sbn.getId());
            msgrcv.putExtra("package", pack);
            msgrcv.putExtra("ticker", ticker);
            msgrcv.putExtra("title", title);
            msgrcv.putExtra("text", text);
            msgrcv.putExtra("bigText", bigText);
            try {
                msgrcv.putExtra("icon", byteArray);
            } catch (Exception e) {
                byte[] ba = null;
                msgrcv.putExtra("icon", ba);
            }
            LocalBroadcastManager.getInstance(context).sendBroadcast(msgrcv);
            Model model = GearNotifications.fetchAndUpdateModel(
                    sbn.getId(),
                    pack,
                    ticker,
                    title,
                    text,
                    bigText,
                    iconBmp,
                    false,
                    richNotificationManager,
                    this
            );
            if (settings.getBoolean(model.pack + '-' + model.id, true)) {
                GearNotifications.send(richNotificationManager, model, true, context);
            }
        } finally {
            lock.unlock();
        }
    }

    public static Pair<Bitmap, List<String>> getText(RemoteViews views)
    {
        // Use reflection to examine the m_actions member of the given RemoteViews object.
        // It's not pretty, but it works.
        Pair<Bitmap, List<String>> retVal = new Pair<Bitmap, List<String>>(
                null,
                new ArrayList<String>()
        );
        try
        {
            Field field = views.getClass().getDeclaredField("mActions");
            field.setAccessible(true);

            @SuppressWarnings("unchecked")
            ArrayList<Parcelable> actions = (ArrayList<Parcelable>) field.get(views);

            // Find the setText() and setTime() reflection actions
            for (Parcelable p : actions)
            {
                Parcel parcel = Parcel.obtain();
                p.writeToParcel(parcel, 0);
                parcel.setDataPosition(0);

                // The tag tells which type of action it is (2 is ReflectionAction, 12 is BitmapReflectionAction)
                int tag = parcel.readInt();
                if (tag != 2 && tag != 12) continue;

                // View ID
                int viewId = parcel.readInt();

                String methodName = parcel.readString();
                if (methodName == null || methodName.equals("setBackgroundColor")) {
                    continue;
                } else if (methodName.equals("setText")) {
                    // Save Strings

                    // Parameter type (10 = Character Sequence)
                    parcel.readInt();

                    // Store the actual string
                    String t = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel).toString().trim();
                    retVal.second.add(t);
                } else if (methodName.equals("setImageBitmap")) {
                    // Save Images
                    Field f = p.getClass().getDeclaredField("bitmap"); //NoSuchFieldException
                    f.setAccessible(true);
                    retVal = new Pair<Bitmap, List<String>>(
                            (Bitmap) f.get(p),
                            retVal.second
                    );

                } else if (methodName.equals("setTime")) {
                    // Save times

                    // Parameter type (5 = Long)
                    parcel.readInt();

                    String t = new SimpleDateFormat("h:mm a").format(new Date(parcel.readLong()));
                    retVal.second.add(t);
                } else {
                    Log.d("Notify", "Unhandled widget " + methodName);
                }

                parcel.recycle();
            }
        }

        // It's not usually good style to do this, but then again, neither is the use of reflection...
        catch (Exception e)
        {
            Log.e("NotificationClassifier", e.toString());
        }

        return retVal;
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Intent msgrcv = new Intent("Msg");

        msgrcv.putExtra("id", sbn.getId());
        msgrcv.putExtra("remove", true);

        LocalBroadcastManager.getInstance(context).sendBroadcast(msgrcv);
    }

    public Bitmap iconToBitmap(Icon ic, Context ctx) {
        return drawableToBitmap(ic.loadDrawable(ctx));
    }

    public static Bitmap drawableToBitmap (Drawable drawable) {
        Bitmap bitmap = null;

        if(drawable == null) {
            return null;
        }

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if(bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if(drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }
}
