package com.hawassa.unifix.shared.utils;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.hawassa.unifix.R;
import com.hawassa.unifix.models.DashboardItem;
import java.util.List;

public class DashboardAdapter extends BaseAdapter {
    private Context context;
    private List<DashboardItem> items;
    private LayoutInflater inflater;

    public DashboardAdapter(Context context, List<DashboardItem> items) {
        this.context = context;
        this.items = items;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_dashboard, parent, false);
            holder = new ViewHolder();
            holder.icon = convertView.findViewById(R.id.ivIcon);
            holder.title = convertView.findViewById(R.id.tvTitle);
            holder.subtitle = convertView.findViewById(R.id.tvSubtitle);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        DashboardItem item = items.get(position);
        holder.icon.setImageResource(item.getIconResId());
        holder.title.setText(item.getTitle());
        holder.subtitle.setText(item.getSubtitle());

        return convertView;
    }

    static class ViewHolder {
        ImageView icon;
        TextView title;
        TextView subtitle;
    }
}