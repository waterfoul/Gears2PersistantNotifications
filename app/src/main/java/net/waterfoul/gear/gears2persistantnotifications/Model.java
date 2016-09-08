package net.waterfoul.gear.gears2persistantnotifications;

import android.graphics.Bitmap;

import java.util.Date;
import java.util.UUID;

public class Model {
    public int id;
    public String pack;
    public String ticker;
    public CharSequence title;
    public CharSequence text;
    public CharSequence bigText;
    public Bitmap image;
    public UUID gearId = null;
    public boolean removed = false;
    public Date lastUpdate = new Date();
    public CharSequence lastTitle;
    public CharSequence lastText;
    public CharSequence lastBigText;

    public Model(int id, String pack, String ticker, CharSequence title, CharSequence text, CharSequence bigText, Bitmap image) {
        this.id = id;
        this.pack = pack;
        this.ticker = ticker;
        this.title = title;
        this.text = text;
        this.bigText = bigText;
        this.image = image;
    }
}
