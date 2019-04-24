package com.mobile.andrada.reportstuff.activities;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
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
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.mobile.andrada.reportstuff.R;
import com.mobile.andrada.reportstuff.adapters.MessageAdapter;
import com.mobile.andrada.reportstuff.db.ChatMessage;
import com.mobile.andrada.reportstuff.firestore.Message;

import java.util.Calendar;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.mobile.andrada.reportstuff.activities.ReportsListActivity.REPORTS_STATUS;

public class ChatActivity extends AppCompatActivity implements
        GoogleApiClient.OnConnectionFailedListener,
        MessageAdapter.OnMessagePlayClickedListener {

    public final static String REPORT_ID = "report_id";
    public final static String TAG = "ChatActivity";
    public static final String MESSAGES_CHILD = "messages";
    public static final String REPORTS_CHILD = "reports";
    public static final String MEDIA_URL_FIELD = "mediaUrl";
    public final static String ANONYMOUS = "anonymous";
    public static final String CHAT_MSG_LENGTH = "chat_msg_length";
    public static final String EXTRA_MEDIA_URI = "extra_media_uri";
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 10;
    private static final int REQUEST_IMAGE = 1;
    private static final int PLAY_MEDIA = 2;
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_LOCATION = 3;

    private String mDisplayName;
    private String mPhotoUrl;
    private String mReportId;

    private SharedPreferences mSharedPreferences;
    private MediaPlayer mediaPlayer;
    private FusedLocationProviderClient fusedLocationClient;

    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private GoogleApiClient mGoogleApiClient;
    private FirebaseFirestore mFirestore;
    private MessageAdapter mAdapter;
    private Query mQuery;

//    @BindView(R.id.progressBar)
//    ProgressBar mProgressBar;

    @BindView(R.id.sendButton)
    Button mSendButton;

    @BindView(R.id.messageRecyclerView)
    RecyclerView mMessageRecyclerView;

    @BindView(R.id.messageEditText)
    EditText mMessageEditText;

    @BindView(R.id.addMessageImageView)
    ImageView mAddMessageImageView;

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

        mDisplayName = mFirebaseUser.getDisplayName();
        if (mFirebaseUser.getPhotoUrl() != null) {
            mPhotoUrl = mFirebaseUser.getPhotoUrl().toString();
        }

        // Only used for sign out
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API)
                .build();

        // Get chat from firestore
        FirebaseFirestore.setLoggingEnabled(true);
        mFirestore = FirebaseFirestore.getInstance();

        // reportID is never empty!
        mReportId = getIntent().getStringExtra(REPORT_ID);
        mQuery = mFirestore.collection(REPORTS_CHILD)
                .document(mReportId).collection(MESSAGES_CHILD).orderBy("time");

        mAdapter = new MessageAdapter(mQuery, this) {

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
        mMessageRecyclerView.setAdapter(mAdapter);

        //TODO: Maybe add some remote configs

        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(mSharedPreferences
                .getInt(CHAT_MSG_LENGTH, DEFAULT_MSG_LENGTH_LIMIT))});
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    mSendButton.setEnabled(true);
                } else {
                    mSendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        mAddMessageImageView.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            startActivityForResult(intent, REQUEST_IMAGE);
        });

        mSendButton.setOnClickListener(view -> {
            addMessageToFirestore("text", task -> mMessageEditText.setText(""));
        });
    }

    @Override
    public void onPause() {
        mAdapter.stopListening();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mAdapter.startListening();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            Intent intent = new Intent(this, ReportsListActivity.class);
            intent.putExtra(REPORTS_STATUS, getIntent().getStringExtra(REPORTS_STATUS));
            NavUtils.navigateUpTo(this, intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
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

    public boolean checkForLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_LOCATION);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length == 0
                        || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this,
                            "Permissions for location needed in order to automatically send it to rescuers.",
                            Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    public void addMessageToFirestore(String mediaType, OnCompleteListener<DocumentReference> onCompleteListener) {
        if (!checkForLocationPermission()) {
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    // Got last known location. In some rare situations this can be null.
                    Message message = new Message(
                            mFirebaseUser.getEmail(),
                            new GeoPoint(location.getLatitude(), location.getLongitude()),
                            mediaType,
                            null,
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
        addMessageToFirestore(mediaType, task -> {
            if (task.isSuccessful()) {
                DocumentReference docRef = task.getResult();
                String key = docRef.getId();
                StorageReference storageReference =
                        FirebaseStorage.getInstance()
                                .getReference(mFirebaseUser.getUid())
                                .child(key)
                                .child(uri.getLastPathSegment());

                putMediaInStorage(storageReference, uri, key, mediaType);
            } else {
                Log.w(TAG, "Unable to write message to database.", task.getException());
            }
        });
    }

    private void putMediaInStorage(final StorageReference storageReference, Uri uri, final String key, final String mediaType) {
        storageReference.putFile(uri).addOnCompleteListener(ChatActivity.this,
                task -> storageReference.getDownloadUrl()
                        .addOnSuccessListener(
                                uri1 ->
                                        mFirestore.collection(REPORTS_CHILD)
                                                .document(mReportId)
                                                .collection(MESSAGES_CHILD)
                                                .document(key)
                                                .update(MEDIA_URL_FIELD, uri1.toString())
                        ).addOnFailureListener(
                                e ->
                                        Log.w(TAG, "Image upload task was not successful.", e)
                        )
        );
    }

    @Override
    public void onOpenVideoClicked(DocumentSnapshot message) {
        Intent intent = new Intent(this, VideoPlayer.class);
        ChatMessage chatMessage = message.toObject(ChatMessage.class);
        String uriString = chatMessage.getMediaUrl();
        if (uriString == null)
            return;

        intent.putExtra(EXTRA_MEDIA_URI, uriString);
        startActivityForResult(intent, PLAY_MEDIA);
    }

    @Override
    public void onPlayAudioClicked(DocumentSnapshot message) {
        if (mediaPlayer != null) {
            mediaPlayer.start();
            return;
        }
        mediaPlayer = new MediaPlayer();
        ChatMessage chatMessage = message.toObject(ChatMessage.class);

        try {
            mediaPlayer.setDataSource(chatMessage != null ? chatMessage.getMediaUrl() : null);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPauseAudioClicked(DocumentSnapshot message) {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
        }
    }

    @Override
    public void onStopAudioClicked(DocumentSnapshot message) {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer = null;
        }
    }
}
