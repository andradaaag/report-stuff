package com.mobile.andrada.reportstuff.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.mobile.andrada.reportstuff.R;
import com.mobile.andrada.reportstuff.firestore.Report;
import com.mobile.andrada.reportstuff.utils.LocationHelper;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ReportAdapter extends FirestoreAdapter<ReportAdapter.ViewHolder> {
    private OnItemClickListener mListener;
    private Context context;

    public interface OnItemClickListener {
        void onItemClick(Report report);
    }

    protected ReportAdapter(Query query, OnItemClickListener onClickListener) {
        super(query);
        mListener = onClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        return new ViewHolder(inflater.inflate(R.layout.item_report, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getSnapshot(position), context);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        Report report;

        @BindView(R.id.citizenNameTextView)
        TextView citizenNameTextView;

        @BindView(R.id.dateTextView)
        TextView dateTextView;

        @BindView(R.id.locationTextView)
        TextView locationTextView;

        ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(v -> mListener.onItemClick(report));
        }

        void bind(final DocumentSnapshot snapshot, Context context) {
            report = snapshot.toObject(Report.class);
            report.setRid(snapshot.getId());

            citizenNameTextView.setText(report.getCitizenName());
            dateTextView.setText(report.getLatestTime().toString());
            locationTextView.setText(LocationHelper.convertGeoPointToAdress(context, report.getLatestLocation()));
        }
    }
}
