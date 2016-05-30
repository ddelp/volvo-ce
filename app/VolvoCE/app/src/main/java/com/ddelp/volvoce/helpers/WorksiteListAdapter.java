package com.ddelp.volvoce.helpers;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.ddelp.volvoce.R;
import com.ddelp.volvoce.objects.WorksiteSummary;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

/**
 * Created by Denny on 5/13/16.
 */
public class WorksiteListAdapter extends ArrayAdapter<WorksiteSummary> {
    //TODO: test
    private Context context;
    private int layoutResourceId;
    private ArrayList<WorksiteSummary> data;

    public WorksiteListAdapter(Context context, int layoutResourceID, ArrayList<WorksiteSummary> data) {
        super(context, layoutResourceID, data);
        this.layoutResourceId = layoutResourceID;
        this.context = context;
        this.data = data;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        ViewHolder holder = null;

        if(row == null) {
            LayoutInflater inflater = ((Activity)context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId,parent,false);
            holder = new ViewHolder();
            holder.name = (TextView)row.findViewById(R.id.worksite_name);
            holder.icon = (ImageView)row.findViewById(R.id.worksite_icon);
            row.setTag(holder);
        }
        else {
            holder = (ViewHolder) row.getTag();
        }

        // Pull data from ride and put in holder
        WorksiteSummary thisWorksite = data.get(position);
        holder.name.setText(thisWorksite.name);
        Picasso.with(context)
                .load(thisWorksite.imageID)
                .resize(150,150)
                .transform(new CircleTransform())
                .into(holder.icon);
        return row;
    }


    // View lookup cache
    private static class ViewHolder {
        TextView name;
        ImageView icon;
    }

}
