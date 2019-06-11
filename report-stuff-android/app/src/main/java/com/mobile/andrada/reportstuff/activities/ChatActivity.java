package com.mobile.andrada.reportstuff.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.mobile.andrada.reportstuff.R;
import com.mobile.andrada.reportstuff.adapters.MessageAdapter;
import com.mobile.andrada.reportstuff.firestore.Message;

import java.util.Calendar;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.mobile.andrada.reportstuff.activities.ReportsListActivity.REPORTS_STATUS;
import static com.mobile.andrada.reportstuff.utils.LocationHelper.MY_PERMISSIONS_REQUEST_ACCESS_LOCATION;
import static com.mobile.andrada.reportstuff.utils.LocationHelper.checkForLocationPermission;
import static com.mobile.andrada.reportstuff.utils.LocationHelper.convertLocation;

public class ChatActivity extends AppCompatActivity implements
        GoogleApiClient.OnConnectionFailedListener,
        MessageAdapter.OnMessagePlayClickedListener {

    public final static String ANONYMOUS = "anonymous";
    public static final String CHAT_MSG_LENGTH = "chat_msg_length";
    public static final String EXTRA_MEDIA_URI = "extra_media_uri";
    public static final String IS_OFFICIAL = "is_official";
    public static final String MEDIA_URL_FIELD = "mediaUrl";
    public static final String MESSAGES_CHILD = "messages";
    public final static String REPORT_ID = "report_id";
    public static final String REPORTS_CHILD = "reports";
    public static final String STATUS_FIELD = "status";
    public static final String STATUS_CLOSED = "closed";
    public static final String STATUS_OPEN = "open";
    public final static String TAG = "ChatActivity";

    public static final int DEFAULT_MSG_LENGTH_LIMIT = 200;
    private static final int PLAY_MEDIA = 2;
    private static final int REQUEST_IMAGE = 1;

    private String mDisplayName;
    private String mPhotoUrl;
    private String mReportId;
    private String mReportStatus;
    private boolean mIsOfficial;

    private FusedLocationProviderClient fusedLocationClient;
    private MediaPlayer mMediaPlayer;
    private SharedPreferences mSharedPreferences;

    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private FirebaseFirestore mFirestore;
    private MessageAdapter mMessageAdapter;
    private Query mQuery;

    @BindView(R.id.selectMediaImageView)
    ImageView mSelectMediaImageView;

    @BindView(R.id.messageEditText)
    EditText mMessageEditText;

    @BindView(R.id.messageRecyclerView)
    RecyclerView mMessageRecyclerView;

    @BindView(R.id.sendButton)
    Button mSendButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        ButterKnife.bind(this);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("Chat");
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mDisplayName = ANONYMOUS;

        // Initialize Firebase Auth
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();

        if (mFirebaseUser == null) {
            startActivity(new Intent(this, SignInActivity.class));
            finish();
            return;
        }

        mDisplayName = mFirebaseUser.getDisplayName();
        if (mFirebaseUser.getPhotoUrl() != null) {
            mPhotoUrl = mFirebaseUser.getPhotoUrl().toString();
        }

        // Get chat from firestore
        FirebaseFirestore.setLoggingEnabled(true);
        mFirestore = FirebaseFirestore.getInstance();

        mReportStatus = getIntent().getStringExtra(REPORTS_STATUS);

        // reportID is never empty!
        mReportId = getIntent().getStringExtra(REPORT_ID);
        mQuery = mFirestore.collection(REPORTS_CHILD)
                .document(mReportId).collection(MESSAGES_CHILD).orderBy("time");

        mMessageAdapter = new MessageAdapter(mQuery, this) {

            @Override
            protected void onDataChanged() {
                // Show/hide content if the query returns empty.
                if (getItemCount() == 0) {
                    mMessageRecyclerView.setVisibility(View.GONE);
                } else {
                    mMessageRecyclerView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            protected void onError(FirebaseFirestoreException e) {
                Snackbar.make(findViewById(android.R.id.content),
                        "Error: check logs for info.", Snackbar.LENGTH_LONG).show();
            }
        };

        LinearLayoutManager mLinearLayoutManager = new LinearLayoutManager(this);
        mLinearLayoutManager.setStackFromEnd(true);
        mMessageRecyclerView.setLayoutManager(mLinearLayoutManager);
        mMessageRecyclerView.setAdapter(mMessageAdapter);

        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(mSharedPreferences
                .getInt(CHAT_MSG_LENGTH, DEFAULT_MSG_LENGTH_LIMIT))});
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0 && !mReportStatus.equals(STATUS_CLOSED)) {
                    mSendButton.setEnabled(true);
                } else {
                    mSendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        mSelectMediaImageView.setOnClickListener(view -> {
            if (mReportStatus.equals(STATUS_CLOSED)) {
                return;
            }
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            startActivityForResult(intent, REQUEST_IMAGE);
        });

        mSendButton.setOnClickListener(view -> addMessageToFirestore("text", null,
                task -> mMessageEditText.setText("")));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mIsOfficial = getIntent().getBooleanExtra(IS_OFFICIAL, false);
        if (!mIsOfficial || mReportStatus.equals(STATUS_CLOSED))
            return false;
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.report_menu, menu);

        return true;
    }

    @Override
    public void onPause() {
        mMessageAdapter.stopListening();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mMessageAdapter.startListening();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
        } else if (id == R.id.more_menu) {
            handleCloseReport();
        }
        return super.onOptionsItemSelected(item);
    }

    private void handleCloseReport() {
        if (!mIsOfficial || mReportStatus.equals(STATUS_CLOSED))
            return;
        DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    mFirestore.collection(REPORTS_CHILD)
                            .document(mReportId)
                            .update(STATUS_FIELD, STATUS_CLOSED);
                    finish();
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    break;
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure you want to close this report?")
                .setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);

        if (requestCode == REQUEST_IMAGE) {
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    final Uri uri = data.getData();
                    Log.d(TAG, "Uri: " + uri.toString());

                    String mediaType = "document";
                    if (uri.toString().contains("image")) {
                        mediaType = "image";
                    } else if (uri.toString().contains("video")) {
                        mediaType = "video";
                    } else if (uri.toString().contains("audio")) {
                        mediaType = "audio";
                    }
                    addMediaMessageToFirestore(uri, mediaType);
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        // If request is cancelled, the result arrays are empty.
        if (requestCode == MY_PERMISSIONS_REQUEST_ACCESS_LOCATION) {
            if (grantResults.length == 0
                    || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this,
                        "Permissions for location needed in order to automatically send it to rescuers.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    public void addMessageToFirestore(String mediaType, String mediaUrl, OnCompleteListener<DocumentReference> onCompleteListener) {
        if (!checkForLocationPermission(this)) {
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    // Got last known location. In some rare situations this can be null.
                    Message message = new Message(
                            mFirebaseUser.getEmail(),
                            convertLocation(location),
                            mediaType,
                            mediaUrl,
                            mDisplayName,
                            mPhotoUrl,
                            mMessageEditText.getText().toString(),
                            Calendar.getInstance().getTime()
                    );
                    mFirestore.collection(REPORTS_CHILD)
                            .document(mReportId)
                            .collection(MESSAGES_CHILD)
                            .add(message)
                            .addOnCompleteListener(onCompleteListener);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error on getLatestLocation" + e));
    }

    protected void addMediaMessageToFirestore(final Uri uri, final String mediaType) {
        // First put media in storage
        StorageReference storageReference =
                FirebaseStorage.getInstance()
                        .getReference(mFirebaseUser.getUid())
                        .child(uri.getLastPathSegment());

        storageReference.putFile(uri).addOnCompleteListener(ChatActivity.this,
                task -> {
                    String bucket = storageReference.getBucket();
                    String path = storageReference.getPath();
                    String mediaUrl = "gs://" + bucket + path;

                    // Then add message with mediaUrl in firestore
                    addMessageToFirestore(mediaType, mediaUrl, task2 -> {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "Unable to write message to database.", task.getException());
                        }
                    });
                }
        );
    }

    @Override
    public void onOpenVideoClicked(DocumentSnapshot message) {
        Intent intent = new Intent(this, VideoPlayer.class);
        Message chatMessage = message.toObject(Message.class);
        String uriString = chatMessage.getMediaUrl();
        if (uriString == null)
            return;

        intent.putExtra(EXTRA_MEDIA_URI, uriString);
        startActivityForResult(intent, PLAY_MEDIA);
    }

    @Override
    public void onPlayAudioClicked(DocumentSnapshot message) {
        if (mMediaPlayer != null) {
            mMediaPlayer.start();
            return;
        }
        mMediaPlayer = new MediaPlayer();
        Message chatMessage = message.toObject(Message.class);

        try {
            mMediaPlayer.setDataSource(chatMessage != null ? chatMessage.getMediaUrl() : null);
            mMediaPlayer.prepare();
            mMediaPlayer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPauseAudioClicked(DocumentSnapshot message) {
        if (mMediaPlayer != null) {
            mMediaPlayer.pause();
        }
    }

    @Override
    public void onStopAudioClicked(DocumentSnapshot message) {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer = null;
        }
    }
}
