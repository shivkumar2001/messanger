package com.example.myfriendlychatapp;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
public class MainActivity extends AppCompatActivity {
  private static final  String TAG="MainActivity";
  private static final  String ANONYMOUS="anonymous";

    private ListView mMessageListView;
    private myfriendlymessageadapter mMessageAdapter;
    private ProgressBar mProgressBar;
    private ImageButton mPhotoPickerButton;
    private EditText mMessageEditText;
    private Button mSendButton;

    private String mUsername;

    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mMessagesDatabaseReference;
    private ChildEventListener mChildEventListener;
    public static final int DEFAULT_MSG_LENGTH_LIMIT=1000;
    public static final int RC_SIGN_IN = 1;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private static final int RC_PHOTO_PICKER =  2;
    private FirebaseStorage mFirebaseStorage;
    private StorageReference mChatphotosStorageReference;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mUsername = ANONYMOUS;
        //initalize firebase components
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseStorage=FirebaseStorage.getInstance();
        mMessagesDatabaseReference = mFirebaseDatabase.getReference().child("messages");
        mChatphotosStorageReference=mFirebaseStorage.getReference().child("chat_photos");
        //initialize reference to views
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mMessageListView = (ListView) findViewById(R.id.messageListView);
        mPhotoPickerButton = (ImageButton) findViewById(R.id.photoPickerButton);
        mMessageEditText = (EditText) findViewById(R.id.messageEditText);
        mSendButton = (Button) findViewById(R.id.sendButton);
        //Initialize message listview and its adapter
        List<myfriendlymessage> friendlyMessages = new ArrayList<>();
        // Initialize message ListView and its adapter
        mMessageAdapter = new myfriendlymessageadapter(this, R.layout.item_message, friendlyMessages);
        mMessageListView.setAdapter(mMessageAdapter);
        // Send button sends a message and clears the EditText
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: Send messages on click
                myfriendlymessage friendlyMessage = new myfriendlymessage(mMessageEditText.getText().toString(), mUsername, null);
                mMessagesDatabaseReference.push().setValue(friendlyMessage);
                // Clear input box
                mMessageEditText.setText("");
            }
        });

        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    //Toast.makeText(MainActivity.this, "You're now signed in. Welcome to FriendlyChat.", Toast.LENGTH_SHORT).show();
                    onSignedInInitialize(user.getDisplayName());
                } else {

                    //user is signed out
                    onSignedOutCleanup();
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setAvailableProviders(Arrays.asList(
                                            new AuthUI.IdpConfig.EmailBuilder().build(),

                                            new AuthUI.IdpConfig.GoogleBuilder().build()

                                    ))
                                    .build(),
                            RC_SIGN_IN);
                }
            }
        };
        // ImagePickerButton shows an image picker to upload a image for a message
        mPhotoPickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(Intent.createChooser(intent, "Complete action using"), RC_PHOTO_PICKER);
            }
        });

    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                // Sign-in succeeded, set up the UI
                Toast.makeText(this, "Signed in!", Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED) {
                // Sign in was canceled by the user, finish the activity
                Toast.makeText(this, "Sign in canceled", Toast.LENGTH_SHORT).show();
                finish();
            }
          else if (requestCode==RC_PHOTO_PICKER&& resultCode==RESULT_OK)
            {
                Uri selectedImageUri = data.getData();

                final StorageReference photoRef =
                        mChatphotosStorageReference.child(selectedImageUri.getLastPathSegment());
                UploadTask uploadTask = photoRef.putFile(selectedImageUri);

                Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task< Uri>>(){
                    @Override
                    public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                        if(!task.isSuccessful()){
                            throw task.getException();
                        }

                        return photoRef.getDownloadUrl();
                    }
                }).addOnCompleteListener(new OnCompleteListener<Uri>(){
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        if(task.isSuccessful()){
                            Uri downloadUri = task.getResult();
                            myfriendlymessage friendlyMessage = new myfriendlymessage(null, mUsername, downloadUri.toString());
                            mMessagesDatabaseReference.push().setValue(friendlyMessage);
                        } else {
                            Toast.makeText(MainActivity.this, "upload failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

            }


            }
        }



    @Override
            protected void onResume() {
                super.onResume();
                mFirebaseAuth.addAuthStateListener(mAuthStateListener);
            }

            @Override
            protected void onPause() {
               super.onPause();
                if (mAuthStateListener != null) {
                    mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
                }
                detachdatabasereadlistener();
                mMessageAdapter.clear();
            }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sign_out_menu:
                //sign out
                AuthUI.getInstance().signOut(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

   private void onSignedInInitialize(String username){
     mUsername=username;
       attachdatabasereadlistener();
   }
   private void onSignedOutCleanup(){
     mUsername=ANONYMOUS;
     mMessageAdapter.clear();
     detachdatabasereadlistener();
   }

   private void attachdatabasereadlistener() {
       if (mChildEventListener == null) {
           mChildEventListener = new ChildEventListener() {
               @Override
               public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                   myfriendlymessage friendlyMessage = dataSnapshot.getValue(myfriendlymessage.class);
                   mMessageAdapter.add(friendlyMessage);
               }

               @Override
               public void onChildChanged(DataSnapshot dataSnapshot, String s) {
               }

               @Override
               public void onChildRemoved(DataSnapshot dataSnapshot) {
               }

               @Override
               public void onChildMoved(DataSnapshot dataSnapshot, String s) {
               }

               @Override
               public void onCancelled(DatabaseError databaseError) {
               }
           };
           mMessagesDatabaseReference.addChildEventListener(mChildEventListener);
       }
   }

   private void detachdatabasereadlistener() {
       if (mChildEventListener != null) {
           mMessagesDatabaseReference.removeEventListener(mChildEventListener);
           mChildEventListener=null;
       }
   }

}

