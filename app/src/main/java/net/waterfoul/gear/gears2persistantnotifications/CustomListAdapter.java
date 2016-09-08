package net.waterfoul.gear.gears2persistantnotifications;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class CustomListAdapter extends BaseAdapter {
    SharedPreferences settings = null;
    Context context;
    ArrayList<Model> modelList;
    PackageManager packageManager = null;

    public CustomListAdapter(Context context, ArrayList<Model> modelList) {
        this.context = context;
        this.modelList = modelList;
    }

    @Override
    public int getCount() {
        return modelList.size();
    }

    @Override
    public Object getItem(int position) {
        return modelList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View view, ViewGroup parent) {
        if(packageManager == null) {
            packageManager = context.getPackageManager();
        }
        if(settings == null) {
            settings = context.getSharedPreferences("enabledApps", 0);
        }
        LayoutInflater inflater=(LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.list_item, null,true);

        TextView txtTitle = (TextView) rowView.findViewById(R.id.Itemname);
        ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
        CheckBox checkBox = (CheckBox) rowView.findViewById(R.id.itemChk);

        final Model m = modelList.get(position);
        try {
            txtTitle.append(m.pack == null ?
                "No Package" :
                packageManager.getApplicationLabel(packageManager.getApplicationInfo(m.pack, 0))
            );
        } catch (PackageManager.NameNotFoundException e) {
            txtTitle.append("Failed to get package");
        }
        txtTitle.append("\n");
        txtTitle.append(m.title == null ? "" : m.title);
        txtTitle.append("\n");
        txtTitle.append(m.text == null ? "" : m.text);
        if(m.bigText != null && !m.bigText.equals("")) {
            txtTitle.append("\n----------------\n");
            txtTitle.append(m.bigText == null ? "" : m.bigText);
        }
        if(m.image != null)
            imageView.setImageBitmap(m.image);

        checkBox.setChecked(settings.getBoolean(m.pack + '-' + m.id, true));
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = settings.edit();
                editor.putBoolean(m.pack + '-' + m.id, isChecked);
                editor.apply();
            }
        });

        return rowView;

    };
}