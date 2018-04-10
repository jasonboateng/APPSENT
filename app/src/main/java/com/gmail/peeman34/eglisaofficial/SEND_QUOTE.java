package com.gmail.peeman34.eglisaofficial;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by pee on 8/1/2016.
 */

public class SEND_QUOTE extends AppCompatActivity {


    private  EditText SendquoteTitle;
    private  EditText SendQuoteDesc;
    private  EditText SendQuoteDate;


    String mPostKey;

    private Button SendQuotePost;
    private Uri mImageUri = null;
    private static final int GALLERY_REQUEST =1;
    private  ImageButton SendQuoteImage;
    private StorageReference mStorage;
    private DatabaseReference mDatabase;
    private DatabaseReference mCHURCHCHOSEN;
    private ProgressDialog mProgress;
    private FirebaseAuth Auth;
    private FirebaseUser mCurrentUser;
    private  DatabaseReference mDatabaseUser;


    public SEND_QUOTE() {
        super();

    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.send_quote);


        Auth = FirebaseAuth.getInstance();
        mCurrentUser = Auth.getCurrentUser();


        mStorage = FirebaseStorage.getInstance().getReference();
        mDatabase = FirebaseDatabase.getInstance().getReference().child("QUOTES");
        mDatabaseUser = FirebaseDatabase.getInstance().getReference().child("Users").child(mCurrentUser.getUid());
        mCHURCHCHOSEN = FirebaseDatabase.getInstance().getReference().child("CHURCHCHOSEN");

        mDatabase.keepSynced(true);
        mDatabaseUser.keepSynced(true);
        mCHURCHCHOSEN.keepSynced(true);

        SendQuoteImage = (ImageButton)findViewById(R.id.sendquoteimage);
        SendquoteTitle = (EditText)findViewById(R.id.sendquotetitle);
        SendQuoteDesc = (EditText)findViewById(R.id.sendquotedescription);
        SendQuoteDate  =(EditText)findViewById(R.id.sendquotedate);

        SendQuotePost = (Button)findViewById(R.id.sendquotepost);



        mProgress  = new ProgressDialog(this);


        SendQuoteImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
                galleryIntent.setType("image/*");
                startActivityForResult(galleryIntent, GALLERY_REQUEST);

            }



        });
        SendQuotePost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startPosting();
            }
        });

    }










    private  void startPosting(){
        final String titleval =    SendquoteTitle.getText().toString();
        final  String descval =SendQuoteDesc.getText().toString();
        final String dateval = SendQuoteDate.getText().toString();

        if (!TextUtils.isEmpty(titleval) && !TextUtils.isEmpty(descval) && !TextUtils.isEmpty(dateval) && mImageUri !=null){
            mProgress.setMessage("Posting QUOTES");

            mProgress.show();
            StorageReference filepath = mStorage.child("QuotePhotos").child(mImageUri.getLastPathSegment());
            filepath.putFile(mImageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    final Uri downloadUrl = taskSnapshot.getDownloadUrl();
                    final DatabaseReference newPost = mDatabase.push();

                    final  String churchkey = getIntent().getExtras().getString("churchkey");

                    mDatabaseUser.addValueEventListener(new ValueEventListener() {
                        @Override

                        public void onDataChange(final DataSnapshot dataSnapshot) {

                            newPost.child("title").setValue(titleval);
                            newPost.child("churchid").setValue(churchkey);

                            newPost.child("desc").setValue(descval);
                            newPost.child("date").setValue(dateval);
                            newPost.child("image").setValue(downloadUrl.toString());

                            newPost.child("uid").setValue(mCurrentUser.getUid());
                            newPost.child("username").setValue(dataSnapshot.child("name").getValue()).addOnCompleteListener(new OnCompleteListener<Void>() {

                                @Override
                                public void onComplete(@NonNull Task<Void> task) {

                                    if (task.isSuccessful()) {


                                        mCHURCHCHOSEN.addValueEventListener(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(DataSnapshot dataSnapshot) {
                                                newPost.child("churchname").setValue(dataSnapshot.child("title").getValue()).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        if (task.isSuccessful()){
                                                            startActivity(new Intent(SEND_QUOTE.this, QUOTES.class));
                                                        }
                                                    }
                                                });


                                            }

                                            @Override
                                            public void onCancelled(DatabaseError databaseError) {

                                            }
                                        });

                                    }
                                }
                            });



                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });


                    mProgress.dismiss();


                }
            });
        }


    }

    // OnActivity result is lacking behind, I have to get the URi from it
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GALLERY_REQUEST && resultCode == RESULT_OK) {

            Uri imageUri = data.getData();

            CropImage.activity(imageUri).setGuidelines(CropImageView.Guidelines.ON).setAspectRatio(1, 1).start(this);


        }

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {

                mImageUri = result.getUri();

                SendQuoteImage.setImageURI(mImageUri);

            } else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {

                Exception error = result.getError();
            }

        }
    }
}









































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































