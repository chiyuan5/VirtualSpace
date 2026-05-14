package com.virtual.app.adapter;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.virtual.app.R;

import java.util.ArrayList;
import java.util.List;

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.AppViewHolder> implements Filterable {

    public interface OnItemClickListener {
        void onItemClick(int position, PackageInfo packageInfo);
    }

    private List<PackageInfo> apps;
    private List<PackageInfo> appsFiltered;
    private final boolean selectionMode;
    private OnItemClickListener listener;
    private List<PackageInfo> selectedApps = new ArrayList<>();

    public AppAdapter(List<PackageInfo> apps, boolean selectionMode) {
        this.apps = apps;
        this.appsFiltered = new ArrayList<>(apps);
        this.selectionMode = selectionMode;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_app, parent, false);
        return new AppViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        PackageInfo pkg = appsFiltered.get(position);
        holder.bind(pkg);
    }

    @Override
    public int getItemCount() {
        return appsFiltered.size();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                List<PackageInfo> filtered = new ArrayList<>();
                String filterPattern = constraint.toString().toLowerCase().trim();

                for (PackageInfo pkg : apps) {
                    if (pkg.packageName.toLowerCase().contains(filterPattern)) {
                        filtered.add(pkg);
                    } else {
                        try {
                            String appName = pkg.applicationInfo.loadLabel(
                                holder.itemView.getContext().getPackageManager()).toString();
                            if (appName.toLowerCase().contains(filterPattern)) {
                                filtered.add(pkg);
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }

                FilterResults results = new FilterResults();
                results.values = filtered;
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                appsFiltered.clear();
                if (results.values != null) {
                    appsFiltered.addAll((List<PackageInfo>) results.values);
                }
                notifyDataSetChanged();
            }
        };
    }

    public List<PackageInfo> getSelectedApps() {
        return selectedApps;
    }

    public void clearSelection() {
        selectedApps.clear();
        notifyDataSetChanged();
    }

    class AppViewHolder extends RecyclerView.ViewHolder {

        private final ImageView appIcon;
        private final TextView appName;
        private final TextView appPackage;
        private final TextView appVersion;
        private final CheckBox checkBox;

        AppViewHolder(@NonNull View itemView) {
            super(itemView);
            appIcon = itemView.findViewById(R.id.appIcon);
            appName = itemView.findViewById(R.id.appName);
            appPackage = itemView.findViewById(R.id.appPackage);
            appVersion = itemView.findViewById(R.id.appVersion);
            checkBox = itemView.findViewById(R.id.checkBox);
        }

        void bind(PackageInfo pkg) {
            PackageManager pm = itemView.getContext().getPackageManager();

            try {
                appName.setText(pkg.applicationInfo.loadLabel(pm));
                appPackage.setText(pkg.packageName);
                appVersion.setText("v" + pkg.versionName);
                appIcon.setImageDrawable(pkg.applicationInfo.loadIcon(pm));
            } catch (Exception e) {
                appName.setText(pkg.packageName);
                appPackage.setText("");
                appVersion.setText("");
            }

            checkBox.setVisibility(selectionMode ? View.VISIBLE : View.GONE);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        listener.onItemClick(pos, appsFiltered.get(pos));
                    }
                }
            });
        }
    }
}
