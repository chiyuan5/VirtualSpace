package com.virtual.app.adapter;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.virtual.app.R;
import com.virtual.core.entity.VirtualApp;

import java.util.ArrayList;
import java.util.List;

public class ClonedAppAdapter extends RecyclerView.Adapter<ClonedAppAdapter.ClonedAppViewHolder> {

    public interface OnActionClickListener {
        void onOpenClick(int position, VirtualApp app);
        void onDeleteClick(int position, VirtualApp app);
    }

    public interface OnItemClickListener {
        void onItemClick(int position, VirtualApp app);
    }

    private List<VirtualApp> apps;
    private OnActionClickListener openListener;
    private OnActionClickListener deleteListener;
    private OnItemClickListener itemListener;

    public ClonedAppAdapter(List<VirtualApp> apps) {
        this.apps = apps != null ? apps : new ArrayList<>();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemListener = listener;
    }

    public void setOnOpenClickListener(OnActionClickListener listener) {
        this.openListener = listener;
    }

    public void setOnDeleteClickListener(OnActionClickListener listener) {
        this.deleteListener = listener;
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
        VirtualApp app = apps.get(position);
        holder.bind(app);
    }

    @Override
    public int getItemCount() {
        return apps.size();
    }

    public android.widget.Filter getFilter() {
        return new android.widget.Filter() {
            @Override
            protected android.widget.FilterResults performFiltering(CharSequence constraint) {
                return null;
            }

            @Override
            protected void publishResults(CharSequence constraint, android.widget.FilterResults results) {
            }
        };
    }

    class ClonedAppViewHolder extends RecyclerView.ViewHolder {

        private final ImageView appIcon;
        private final TextView appName;
        private final TextView appPackage;
        private final TextView appUserId;
        private final View openButton;
        private final View deleteButton;

        ClonedAppViewHolder(@NonNull View itemView) {
            super(itemView);
            appIcon = itemView.findViewById(R.id.appIcon);
            appName = itemView.findViewById(R.id.appName);
            appPackage = itemView.findViewById(R.id.appPackage);
            appUserId = itemView.findViewById(R.id.appUserId);
            openButton = itemView.findViewById(R.id.btnOpen);
            deleteButton = itemView.findViewById(R.id.btnDelete);
        }

        void bind(VirtualApp app) {
            Context context = itemView.getContext();
            PackageManager pm = context.getPackageManager();

            try {
                appName.setText(app.appName);
                appPackage.setText(app.fakePackageName);
                appUserId.setText("User: " + app.userId);
                
                try {
                    appIcon.setImageDrawable(pm.getApplicationIcon(app.fakePackageName));
                } catch (Exception e) {
                    appIcon.setImageResource(R.mipmap.ic_launcher);
                }
            } catch (Exception e) {
                appName.setText(app.appName);
                appPackage.setText(app.packageName);
                appUserId.setText("User: " + app.userId);
            }

            itemView.setOnClickListener(v -> {
                if (itemListener != null) {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        itemListener.onItemClick(pos, apps.get(pos));
                    }
                }
            });

            openButton.setOnClickListener(v -> {
                if (openListener != null) {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        openListener.onOpenClick(pos, apps.get(pos));
                    }
                }
            });

            deleteButton.setOnClickListener(v -> {
                if (deleteListener != null) {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        deleteListener.onDeleteClick(pos, apps.get(pos));
                    }
                }
            });
        }
    }
}