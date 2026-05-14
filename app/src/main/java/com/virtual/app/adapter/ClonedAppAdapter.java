package com.virtual.app.adapter;

import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.virtual.app.R;
import com.virtual.core.entity.VirtualApp;

import java.util.ArrayList;
import java.util.List;

public class ClonedAppAdapter extends RecyclerView.Adapter<ClonedAppAdapter.ClonedAppViewHolder> implements Filterable {

    public interface OnItemClickListener {
        void onItemClick(int position, VirtualApp app);
    }

    public interface OnActionClickListener {
        void onOpenClick(int position, VirtualApp app);
        void onDeleteClick(int position, VirtualApp app);
    }

    private List<VirtualApp> apps;
    private List<VirtualApp> appsFiltered;
    private OnItemClickListener itemListener;
    private OnActionClickListener actionListener;

    public ClonedAppAdapter(List<VirtualApp> apps) {
        this.apps = apps;
        this.appsFiltered = new ArrayList<>(apps);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemListener = listener;
    }

    public void setOnOpenClickListener(OnActionClickListener listener) {
        this.actionListener = listener;
    }

    public void setOnDeleteClickListener(OnActionClickListener listener) {
        this.actionListener = listener;
    }

    @NonNull
    @Override
    public ClonedAppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_cloned_app, parent, false);
        return new ClonedAppViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ClonedAppViewHolder holder, int position) {
        VirtualApp app = appsFiltered.get(position);
        holder.bind(app);
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
                List<VirtualApp> filtered = new ArrayList<>();
                String filterPattern = constraint.toString().toLowerCase().trim();

                for (VirtualApp app : apps) {
                    if (app.packageName.toLowerCase().contains(filterPattern) ||
                        app.appName.toLowerCase().contains(filterPattern)) {
                        filtered.add(app);
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
                    appsFiltered.addAll((List<VirtualApp>) results.values);
                }
                notifyDataSetChanged();
            }
        };
    }

    class ClonedAppViewHolder extends RecyclerView.ViewHolder {

        private final ImageView appIcon;
        private final TextView appName;
        private final TextView userId;
        private final TextView cloneCount;
        private final Button btnOpen;
        private final Button btnDelete;

        ClonedAppViewHolder(@NonNull View itemView) {
            super(itemView);
            appIcon = itemView.findViewById(R.id.appIcon);
            appName = itemView.findViewById(R.id.appName);
            userId = itemView.findViewById(R.id.userId);
            cloneCount = itemView.findViewById(R.id.cloneCount);
            btnOpen = itemView.findViewById(R.id.btnOpen);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }

        void bind(VirtualApp app) {
            appName.setText(app.appName);
            userId.setText("User ID: " + app.userId);
            cloneCount.setText(app.clonedPackages.size() + " clones");

            try {
                PackageManager pm = itemView.getContext().getPackageManager();
                android.content.pm.ApplicationInfo sourceInfo = pm.getApplicationInfo(app.packageName, 0);
                appIcon.setImageDrawable(sourceInfo.loadIcon(pm));
            } catch (Exception e) {
                appIcon.setImageResource(android.R.drawable.sym_def_app_icon);
            }

            btnOpen.setOnClickListener(v -> {
                if (actionListener != null) {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        actionListener.onOpenClick(pos, appsFiltered.get(pos));
                    }
                }
            });

            btnDelete.setOnClickListener(v -> {
                if (actionListener != null) {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        actionListener.onDeleteClick(pos, appsFiltered.get(pos));
                    }
                }
            });

            itemView.setOnClickListener(v -> {
                if (itemListener != null) {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        itemListener.onItemClick(pos, appsFiltered.get(pos));
                    }
                }
            });
        }
    }
}
