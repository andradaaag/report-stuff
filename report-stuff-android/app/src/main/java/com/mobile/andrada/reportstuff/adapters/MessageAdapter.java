package com.mobile.andrada.reportstuff.adapters;

import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.mobile.andrada.reportstuff.R;
import com.mobile.andrada.reportstuff.activities.ChatActivity;
import com.mobile.andrada.reportstuff.firestore.Message;
import com.mobile.andrada.reportstuff.utils.Utils;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;

public class MessageAdapter extends FirestoreAdapter<MessageAdapter.ViewHolder> {

    public interface OnMessagePlayClickedListener {

        void onOpenVideoClicked(DocumentSnapshot message);
        void onPlayAudioClicked(DocumentSnapshot message);
        void onPauseAudioClicked(DocumentSnapshot message);
        void onStopAudioClicked(DocumentSnapshot message);

    }

    private OnMessagePlayClickedListener mListener;

    protected MessageAdapter(Query query, OnMessagePlayClickedListener listener) {
        super(query);
        mListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        return new ViewHolder(inflater.inflate(R.layout.item_message, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getSnapshot(position), mListener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        Message message;

        @BindView(R.id.messageTextView)
        TextView messageTextView;

        @BindView(R.id.messageImageView)
        ImageView messageImageView;

        @BindView(R.id.openVideoButton)
        Button openVideoButton;

        @BindView(R.id.playAudioButton)
        ImageView playAudioButton;

        @BindView(R.id.pauseAudioButton)
        ImageView pauseAudioButton;

        @BindView(R.id.stopAudioButton)
        ImageView stopAudioButton;

        @BindView(R.id.messengerTextView)
        TextView messengerTextView;

        @BindView(R.id.messengerImageView)
        CircleImageView messengerImageView;


        ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        void bind(final DocumentSnapshot snapshot, final OnMessagePlayClickedListener listener) {
            message = snapshot.toObject(Message.class);
            openVideoButton.setOnClickListener(view -> {
                if (listener != null) {
                    listener.onOpenVideoClicked(snapshot);
                }
            });
            playAudioButton.setOnClickListener(view -> {
                if (listener != null) {
                    listener.onPlayAudioClicked(snapshot);
                }
                pauseAudioButton.setVisibility(Button.VISIBLE);
                stopAudioButton.setVisibility(Button.VISIBLE);
                playAudioButton.setVisibility(Button.GONE);
            });
            pauseAudioButton.setOnClickListener(view -> {
                if (listener != null) {
                    listener.onPauseAudioClicked(snapshot);
                }
                playAudioButton.setVisibility(Button.VISIBLE);
                stopAudioButton.setVisibility(Button.VISIBLE);
                pauseAudioButton.setVisibility(Button.GONE);
            });
            stopAudioButton.setOnClickListener(view -> {
                if (listener != null) {
                    listener.onStopAudioClicked(snapshot);
                }
                playAudioButton.setVisibility(Button.VISIBLE);
                pauseAudioButton.setVisibility(Button.GONE);
                stopAudioButton.setVisibility(Button.GONE);
            });

            handleMessenger();
            switch (message.getMediaType()) {
                case "text":
                    handleText();
                    break;
                case "image":
                    handleImage();
                    break;
                case "video":
                    handleVideo();
                    break;
                case "audio":
                    handleAudio();
                    break;
                default:
                    handleText();
                    break;
            }
        }

        private void handleMessenger() {
            Resources resources = itemView.getResources();
            String text = message.getName() + " • " + Utils.prettyTime(message.getTime());
            messengerTextView.setText(text);
            if (message.getPhotoUrl() == null) {
                messengerImageView.setImageDrawable(resources.getDrawable(R.drawable.ic_account_circle_black));
            } else {
                Glide.with(messengerImageView.getContext())
                        .load(message.getPhotoUrl())
                        .into(messengerImageView);
            }
        }

        private void handleText() {
            if (message.getText() != null && !message.getText().isEmpty()) {
                messageTextView.setText(message.getText());
                messageTextView.setVisibility(TextView.VISIBLE);
            }
        }

        private void handleImage() {
            if (message.getMediaUrl() != null) {
                String imageUrl = message.getMediaUrl();
                if (imageUrl.startsWith("gs://")) {
                    StorageReference storageReference = FirebaseStorage.getInstance()
                            .getReferenceFromUrl(imageUrl);
                    storageReference.getDownloadUrl().addOnCompleteListener(
                            task -> {
                                if (task.isSuccessful()) {
                                    String downloadUrl = task.getResult().toString();
                                    Glide.with(messageImageView.getContext())
                                            .load(downloadUrl)
                                            .into(messageImageView);
                                } else {
                                    Log.w(ChatActivity.TAG, "Getting download url was not successful.",
                                            task.getException());
                                }
                            });
                } else {
                    Glide.with(messageImageView.getContext())
                            .load(message.getMediaUrl())
                            .into(messageImageView);
                }
                messageImageView.setVisibility(ImageView.VISIBLE);
                messageTextView.setVisibility(TextView.GONE);
            }
        }

        private void handleVideo() {
            openVideoButton.setVisibility(Button.VISIBLE);
            messageTextView.setVisibility(TextView.GONE);
        }

        private void handleAudio() {
            playAudioButton.setVisibility(Button.VISIBLE);
            messageTextView.setVisibility(TextView.GONE);
        }
    }
}
