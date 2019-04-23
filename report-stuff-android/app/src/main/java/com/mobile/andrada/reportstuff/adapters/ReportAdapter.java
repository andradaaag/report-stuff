package com.mobile.andrada.reportstuff.adapters;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.mobile.andrada.reportstuff.R;
import com.mobile.andrada.reportstuff.db.ChatMessage;
import com.mobile.andrada.reportstuff.firestore.Report;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * RecyclerView adapter for a list of Messages.
 */
public class ReportAdapter extends FirestoreAdapter<ReportAdapter.ViewHolder> {
    private OnItemClickListener mListener;

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
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        return new ViewHolder(inflater.inflate(R.layout.item_report, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getSnapshot(position));
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        Report report;

        @BindView(R.id.locationTextView)
        TextView locationTextView;

        @BindView(R.id.citizenNameTextView)
        TextView citizenNameTextView;

        @BindView(R.id.dateTextView)
        TextView dateTextView;

        ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.onItemClick(report);
                }
            });
        }

        void bind(final DocumentSnapshot snapshot) {
            report = snapshot.toObject(Report.class);
            report.setRid(snapshot.getId());

            locationTextView.setText(report.getLastLocation());
            citizenNameTextView.setText(report.getCitizenName());
            dateTextView.setText(report.getLastTime().toString());
        }
    }
}
