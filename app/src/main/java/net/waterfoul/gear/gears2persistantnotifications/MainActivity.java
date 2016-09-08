package net.waterfoul.gear.gears2persistantnotifications;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import com.android.internal.util.Predicate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MainActivity extends Activity {

    ListView list;
    CustomListAdapter adapter;
    ArrayList<Model> modelList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(GearNotifications.modelList == null) {
            GearNotifications.modelList = new ArrayList<>();
        }

        //noinspection unchecked
        modelList = (ArrayList<Model>) GearNotifications.modelList.clone();

        setContentView(R.layout.activity_main);
        adapter = new CustomListAdapter(getApplicationContext(), modelList);
        list=(ListView)findViewById(R.id.list);
        list.setAdapter(adapter);
        LocalBroadcastManager.getInstance(this).registerReceiver(onNotice, new IntentFilter("Msg"));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);//Menu Resource, Menu
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent intent = new Intent(
                        "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private BroadcastReceiver onNotice= new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            int id = intent.getIntExtra("id", -1);
            String pack = intent.getStringExtra("package");
            String ticker = intent.getStringExtra("ticker");
            CharSequence title = intent.getCharSequenceExtra("title");
            CharSequence text = intent.getCharSequenceExtra("text");
            CharSequence bigText = intent.getCharSequenceExtra("bigText");
            boolean remove = intent.getBooleanExtra("remove", false);
            byte[] byteArray = intent.getByteArrayExtra("icon");

            Context remotePackageContext = null;
            try {

                //noinspection unchecked
                modelList = (ArrayList<Model>) GearNotifications.modelList.clone();
                adapter = new CustomListAdapter(getApplicationContext(), modelList);
                list=(ListView)findViewById(R.id.list);
                list.setAdapter(adapter);
                adapter.notifyDataSetChanged();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };
}
