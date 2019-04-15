package com.mobile.andrada.reportstuff.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.Snackbar;
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
import android.widget.ProgressBar;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.mobile.andrada.reportstuff.R;
import com.mobile.andrada.reportstuff.adapters.MessageAdapter;
import com.mobile.andrada.reportstuff.db.ChatMessage;

import java.util.Calendar;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ChatActivity extends AppCompatActivity implements
        GoogleApiClient.OnConnectionFailedListener,
        MessageAdapter.OnMessageSelectedListener {

    public final static String EXTRA_ID = "com.mobile.andrada.reportstuff.activities.EXTRA_ID";
    public final static String TAG = "ChatActivity";
    public static final String MESSAGES_CHILD = "messages";
    public final static String ANONYMOUS = "anonymous";
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 10;
    public static final String CHAT_MSG_LENGTH = "chat_msg_length";
    private static final int REQUEST_IMAGE = 1;
    private static final String LOADING_IMAGE_URL = "https://www.google.com/images/spin-32.gif";

    private String mUsername;
    private String mPhotoUrl;
    private SharedPreferences mSharedPreferences;
    LinearLayoutManager mLinearLayoutManager;

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

        FirebaseFirestore.setLoggingEnabled(true);
        mFirestore = FirebaseFirestore.getInstance();
        mQuery = mFirestore.collection("messages");
//                .limit(10);

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mUsername = ANONYMOUS;

        // Initialize Firebase Auth
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();

        if (mFirebaseUser == null) {
            // Not signed in, launch the Sign In activity
            startActivity(new Intent(this, SignInActivity.class));
            finish();
            return;
        } else {
            mUsername = mFirebaseUser.getDisplayName();
            if (mFirebaseUser.getPhotoUrl() != null) {
                mPhotoUrl = mFirebaseUser.getPhotoUrl().toString();
            }
        }

        // Only used for sign out
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API)
                .build();

        if (mQuery == null) {
            Log.w(TAG, "No query, not initializing RecyclerView");
        }

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
                // Show a snackbar on errors
                Snackbar.make(findViewById(android.R.id.content),
                        "Error: check logs for info.", Snackbar.LENGTH_LONG).show();
            }
        };

        mLinearLayoutManager = new LinearLayoutManager(this);
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

        mAddMessageImageView.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                startActivityForResult(intent, REQUEST_IMAGE);
            }
        });

//        mSendButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                ChatMessage chatMessage = new ChatMessage(
//                        null,
//                        mUsername,
//                        Calendar.getInstance().getTime().toString(),
//                        mMessageEditText.getText().toString(),
//                        mPhotoUrl,
//                        null
//                );
//                mFirebaseDatabaseReference.child(MESSAGES_CHILD).push().setValue(chatMessage);
//                mMessageEditText.setText("");
//            }
//        });
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
            NavUtils.navigateUpTo(this, new Intent(this, MainActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
    }

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);
//
//        if (requestCode == REQUEST_IMAGE) {
//            if (resultCode == RESULT_OK) {
//                if (data != null) {
//                    final Uri uri = data.getData();
//                    Log.d(TAG, "Uri: " + uri.toString());
//
//                    ChatMessage tempMessage = new ChatMessage(
//                            null,
//                            mUsername,
//                            Calendar.getInstance().getTime().toString(),
//                            mMessageEditText.getText().toString(),
//                            mPhotoUrl,
//                            LOADING_IMAGE_URL
//                    );
//                    mFirebaseDatabaseReference.child(MESSAGES_CHILD).push()
//                            .setValue(tempMessage, new DatabaseReference.CompletionListener() {
//                                @Override
//                                public void onComplete(DatabaseError databaseError,
//                                                       DatabaseReference databaseReference) {
//                                    if (databaseError == null) {
//                                        String key = databaseReference.getKey();
//                                        StorageReference storageReference =
//                                                FirebaseStorage.getInstance()
//                                                        .getReference(mFirebaseUser.getUid())
//                                                        .child(key)
//                                                        .child(uri.getLastPathSegment());
//
//                                        putImageInStorage(storageReference, uri, key);
//                                    } else {
//                                        Log.w(TAG, "Unable to write message to database.",
//                                                databaseError.toException());
//                                    }
//                                }
//                            });
//                }
//            }
//        }
//    }
//
//    private void putImageInStorage(final StorageReference storageReference, Uri uri, final String key) {
//        storageReference.putFile(uri).addOnCompleteListener(ChatActivity.this,
//                new OnCompleteListener<UploadTask.TaskSnapshot>() {
//                    @Override
//                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
//                        storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
//                            @Override
//                            public void onSuccess(Uri uri) {
//                                mFirebaseDatabaseReference.child(MESSAGES_CHILD).child(key).child("imageUrl").setValue(uri.toString());
//                            }
//
//
//                        }).addOnFailureListener(new OnFailureListener() {
//                            @Override
//                            public void onFailure(@NonNull Exception e) {
//                                Log.w(TAG, "Image upload task was not successful.", e);
//                            }
//                        });
//                    }
//                });
//    }

    @Override
    public void onMessageSelected(DocumentSnapshot message) {
        //TODO: implement if needed
    }
}
