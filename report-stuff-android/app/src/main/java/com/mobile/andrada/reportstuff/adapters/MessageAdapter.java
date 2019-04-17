package com.mobile.andrada.reportstuff.adapters;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.mobile.andrada.reportstuff.R;
import com.mobile.andrada.reportstuff.activities.ChatActivity;
import com.mobile.andrada.reportstuff.db.ChatMessage;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * RecyclerView adapter for a list of Messages.
 */
public class MessageAdapter extends FirestoreAdapter<MessageAdapter.ViewHolder> {
//    ProgressBar mProgressBar;

    public interface OnMessagePlayClickedListener {

        void onMessagePlayClicked(DocumentSnapshot message);

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
//        mProgressBar = parent.findViewById(R.id.progressBar);
        return new ViewHolder(inflater.inflate(R.layout.item_message, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getSnapshot(position), mListener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ChatMessage chatMessage;

        @BindView(R.id.messageTextView)
        TextView messageTextView;

        @BindView(R.id.messageImageView)
        ImageView messageImageView;

        @BindView(R.id.playMessageButton)
        Button playMessageButton;

        @BindView(R.id.messengerTextView)
        TextView messengerTextView;

        @BindView(R.id.messengerImageView)
        CircleImageView messengerImageView;


        ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        void bind(final DocumentSnapshot snapshot, final OnMessagePlayClickedListener listener) {
//            mProgressBar.setVisibility(ProgressBar.INVISIBLE);
            chatMessage = snapshot.toObject(ChatMessage.class);
            playMessageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (listener != null) {
                        listener.onMessagePlayClicked(snapshot);
                    }
                }
            });

            handleMessenger();
            switch (chatMessage.getMediaType()) {
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
            messengerTextView.setText(chatMessage.getName());
            if (chatMessage.getPhotoUrl() == null) {
                messengerImageView.setImageDrawable(resources.getDrawable(R.drawable.ic_account_circle_black));
            } else {
                Glide.with(messengerImageView.getContext())
                        .load(chatMessage.getPhotoUrl())
                        .into(messengerImageView);
            }
        }

        private void handleText() {
            if (chatMessage.getText() != null && !chatMessage.getText().isEmpty()) {
                messageTextView.setText(chatMessage.getText());
                messageTextView.setVisibility(TextView.VISIBLE);
                messageImageView.setVisibility(ImageView.GONE);
                playMessageButton.setVisibility(ImageView.GONE);
            }
        }

        private void handleImage() {
            if (chatMessage.getMediaUrl() != null) {
                String imageUrl = chatMessage.getMediaUrl();
                if (imageUrl.startsWith("gs://")) {
                    StorageReference storageReference = FirebaseStorage.getInstance()
                            .getReferenceFromUrl(imageUrl);
                    storageReference.getDownloadUrl().addOnCompleteListener(
                            new OnCompleteListener<Uri>() {
                                @Override
                                public void onComplete(@NonNull Task<Uri> task) {
                                    if (task.isSuccessful()) {
                                        String downloadUrl = task.getResult().toString();
                                        Glide.with(messageImageView.getContext())
                                                .load(downloadUrl)
                                                .into(messageImageView);
                                    } else {
                                        Log.w(ChatActivity.TAG, "Getting download url was not successful.",
                                                task.getException());
                                    }
                                }
                            });
                } else {
                    Glide.with(messageImageView.getContext())
                            .load(chatMessage.getMediaUrl())
                            .into(messageImageView);
                }
                messageImageView.setVisibility(ImageView.VISIBLE);
                messageTextView.setVisibility(TextView.GONE);
                playMessageButton.setVisibility(ImageView.GONE);
            }
        }

        private void handleVideo() {
            playMessageButton.setVisibility(Button.VISIBLE);
            messageTextView.setVisibility(TextView.GONE);
            messageImageView.setVisibility(ImageView.GONE);
        }

        private void handleAudio() {
            //TODO: handleAudio
        }
    }
}
