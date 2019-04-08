package com.mobile.andrada.reportstuff.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mobile.andrada.reportstuff.R;
import com.mobile.andrada.reportstuff.db.Report;

import java.util.ArrayList;
import java.util.List;

public class ReportsRecyclerViewAdapter extends RecyclerView.Adapter<ReportsRecyclerViewAdapter.ViewHolder> {
    private List<Report> reports;
    private OnItemClickListener onItemClickListener;

    public ReportsRecyclerViewAdapter() {
        this.reports = new ArrayList<>();
    }

    public void setData(List<Report> reports) {
        this.reports = reports;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.from(viewGroup.getContext()).
                inflate(R.layout.item_report, viewGroup, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        Report currentReport = reports.get(i);
        viewHolder.locationTextView.setText(currentReport.getLocation());
        viewHolder.citizenNameTextView.setText(currentReport.getCitizenName());
        viewHolder.dateTextView.setText(currentReport.getDate());
    }

    @Override
    public int getItemCount() {
        return reports.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        final TextView locationTextView;
        final TextView citizenNameTextView;
        final TextView dateTextView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            locationTextView = itemView.findViewById(R.id.locationTextView);
            citizenNameTextView = itemView.findViewById(R.id.citizenNameTextView);
            dateTextView = itemView.findViewById(R.id.dateTextView);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (onItemClickListener != null && position != RecyclerView.NO_POSITION)
                        onItemClickListener.onItemClick(reports.get(position));
                }
            });
        }
    }

    public interface OnItemClickListener {
        void onItemClick(Report report);
    }

    public void setOnClickListener(OnItemClickListener onClickListener) {
        this.onItemClickListener = onClickListener;
    }
}