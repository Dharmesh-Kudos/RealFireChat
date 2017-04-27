package com.example.tasol.realfirechat;

import android.*;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.fasterxml.jackson.core.io.CharTypes;
import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.Query;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.romainpiel.shimmer.Shimmer;
import com.romainpiel.shimmer.ShimmerTextView;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Chat extends AppCompatActivity {
    LinearLayout layout;
    TextView sendButton;
    EditText messageArea;
    ScrollView scrollView;
    Firebase reference1, reference2, referenceTyping;
    private boolean typingStarted;
    ShimmerTextView txtTyping;
    private boolean typeWithBool = false;
    Toolbar toolbar;
    Shimmer shimmer;
    private StorageReference storageRef;
    LinearLayout imgLayout;
    ImageView imgPreview;
    ImageView imgButton;
    private int PERMISSIONS_REQUEST_CODE_WRITE_EXTERNAL_STORAGE = 101;
    private int SELECT_PICTURE = 1;
    private Uri imgPath;
    private String selectedImagePath;
    private FirebaseApp app;
    private FirebaseStorage storage;
    private Uri downloadUrl;
    private boolean tickNow = false;
    private ArrayList<Map> mapArrayList = new ArrayList<>();
    RecyclerViewCartAdapter recyclerViewCartAdapter;
    RecyclerView chatList;
    LinearLayoutManager linearLayoutManager;
    private ArrayList<String> chatDates = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                supportFinishAfterTransition();
            }
        });

        getSupportActionBar().setTitle(UserDetails.chatWith);

        shimmer = new Shimmer();
        imgLayout = (LinearLayout) findViewById(R.id.imgLayout);
        imgPreview = (ImageView) findViewById(R.id.imgPreview);
        imgButton = (ImageView) findViewById(R.id.imgButton);
        //layout = (LinearLayout) findViewById(R.id.layout1);
        sendButton = (TextView) findViewById(R.id.sendButton);
        messageArea = (EditText) findViewById(R.id.messageArea);
        //scrollView = (ScrollView) findViewById(R.id.scrollView);
        txtTyping = (ShimmerTextView) findViewById(R.id.txtTyping);

        chatList = (RecyclerView) findViewById(R.id.chatList);
        linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        chatList.setLayoutManager(linearLayoutManager);
        chatList.setHasFixedSize(true);
        linearLayoutManager.setStackFromEnd(true);
        Firebase.setAndroidContext(this);
        app = FirebaseApp.getInstance();
        storage = FirebaseStorage.getInstance(app);
        reference1 = new Firebase("https://realfirechat.firebaseio.com/messages/" + UserDetails.username + "_" + UserDetails.chatWith);
        reference2 = new Firebase("https://realfirechat.firebaseio.com/messages/" + UserDetails.chatWith + "_" + UserDetails.username);
        referenceTyping = new Firebase("https://realfirechat.firebaseio.com/Typing/" + UserDetails.chatWith);

        txtTyping.setText(UserDetails.chatWith + " is Typing...");

        imgButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (CheckPermissionForWriteStorage()) {

                    OpenImageChooser();
                }
            }
        });

        messageArea.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!TextUtils.isEmpty(s.toString()) && s.toString().trim().length() == 1) {
                    //Log.i(TAG, “typing started event…”);
                    typingStarted = true;
                    fetch("true");
                } else if (s.toString().trim().length() == 0 && typingStarted) {
                    //Log.i(TAG, “typing stopped event…”);
                    typingStarted = false;
                    fetch("false");
                }

            }
        });


        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String messageText = messageArea.getText().toString();

                SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy");
                SimpleDateFormat stime = new SimpleDateFormat("HH:mm:ss");
                String currentDate = sdf.format(new Date());
                String currentTime = stime.format(new Date());

                if (!messageText.equals("") || tickNow) {
                    Map<String, String> map = new HashMap<String, String>();
                    if (messageText.length() > 0) {
                        map.put("message", messageText);
                    }
                    map.put("user", UserDetails.username);
                    map.put("date", currentDate);
                    map.put("time", currentTime);
                    if (downloadUrl != null) {
                        map.put("image", String.valueOf(downloadUrl));
                    }

                    reference1.push().setValue(map);

                    reference2.push().setValue(map);

                    messageArea.setText("");
                    downloadUrl = null;
                    imgLayout.setVisibility(View.GONE);
                }
            }
        });


        referenceTyping.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                if (dataSnapshot.getValue().equals("true") && typeWithBool) {
                    Log.d("isTyping = ", "TRUE");
                    fetch("true");
                    getSupportActionBar().setSubtitle("Online");
                    txtTyping.setVisibility(View.VISIBLE);
                    shimmer.start(txtTyping);
                } else {
                    fetch("false");
                    Log.d("isTyping = ", "FALSE");
                    txtTyping.setVisibility(View.GONE);
                    shimmer.cancel();
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                if (dataSnapshot.getValue().equals("true") && typeWithBool) {
                    Log.d("isTyping = ", "TRUE");
                    toolbar.setSubtitle("Online");
                    txtTyping.setVisibility(View.VISIBLE);
                    shimmer.start(txtTyping);
                } else {
                    Log.d("isTyping = ", "FALSE");
                    txtTyping.setVisibility(View.GONE);
                    shimmer.cancel();
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });


        reference1.addChildEventListener(new ChildEventListener() {

            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Map map = dataSnapshot.getValue(Map.class);
                mapArrayList.add(map);
                generateChats(mapArrayList);
                Log.d("@@@Date= ", chatDates.toString());
                recyclerViewCartAdapter = new RecyclerViewCartAdapter();
                chatList.setAdapter(recyclerViewCartAdapter);
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
            public void onCancelled(FirebaseError firebaseError) {

            }
        });
    }

    private void generateChats(ArrayList<Map> mapArrayList) {

        for (int i = 0; i < mapArrayList.size(); i++) {
            if (i == 0) {
                chatDates.add(mapArrayList.get(i).get("date").toString());
            } else {
                if (Integer.parseInt(mapArrayList.get(i).get("date").toString()
                        .substring(0, mapArrayList.get(i).get("date").toString().indexOf("-")))
                        != Integer.parseInt(chatDates.get(i - 1).substring(0, chatDates.get(i - 1).indexOf("-")))) {
                    chatDates.add(mapArrayList.get(i).get("date").toString());
                }
            }

        }
    }

    private class RecyclerViewCartAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {


        private int TYPE_WITH = 2;
        private int TYPE_YOU = 1;

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            RecyclerView.ViewHolder viewHolder;
            if (viewType == TYPE_YOU) {
                View youView = LayoutInflater.from(parent.getContext()).inflate(R.layout.sender_message_conversation,
                        parent, false);
                viewHolder = new YouViewHolder(youView);
            } else {
                View withView = LayoutInflater.from(parent.getContext()).inflate(R.layout.reciever_message_conversation,
                        parent, false);
                viewHolder = new WithViewHolder(withView);
            }

            return viewHolder;
        }

        @Override
        public int getItemViewType(int position) {
            if (mapArrayList.get(position).get("user").equals(UserDetails.username)) {
                return TYPE_YOU;
            }
            return TYPE_WITH;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
            if (holder instanceof YouViewHolder) {
                final YouViewHolder viewHolder = (YouViewHolder) holder;
                Map row = mapArrayList.get(position);
                if (row.containsKey("message") && row.containsKey("image")) {
                    viewHolder.txtImageDateTime.setVisibility(View.GONE);
                    viewHolder.txtInputDateTime.setVisibility(View.VISIBLE);
                    viewHolder.txtInputDateTime.setText(getFormattedTime(row.get("time").toString()));
                    viewHolder.text.setVisibility(View.VISIBLE);
                    viewHolder.text.setText(row.get("message").toString());
                    viewHolder.imgCard.setVisibility(View.VISIBLE);
                    viewHolder.imagePreview.setVisibility(View.VISIBLE);
                    Picasso.with(Chat.this).load(row.get("image").toString()).placeholder(R.drawable.ic_launcher).into(viewHolder.imagePreview);
                }

                if (!row.containsKey("image") && row.containsKey("message")) {
                    viewHolder.txtImageDateTime.setVisibility(View.GONE);
                    viewHolder.txtInputDateTime.setVisibility(View.VISIBLE);
                    viewHolder.txtInputDateTime.setText(getFormattedTime(row.get("time").toString()));
                    viewHolder.imgCard.setVisibility(View.GONE);
                    viewHolder.imagePreview.setVisibility(View.GONE);
                    viewHolder.text.setVisibility(View.VISIBLE);
                    viewHolder.text.setText(row.get("message").toString());
                }
                if (row.containsKey("image") && !row.containsKey("message")) {
                    viewHolder.text.setVisibility(View.GONE);
                    viewHolder.txtInputDateTime.setVisibility(View.GONE);
                    viewHolder.txtImageDateTime.setVisibility(View.VISIBLE);
                    viewHolder.txtImageDateTime.setText(getFormattedTime(row.get("time").toString()));
                    viewHolder.imgCard.setVisibility(View.VISIBLE);
                    viewHolder.imagePreview.setVisibility(View.VISIBLE);
                    Picasso.with(Chat.this).load(row.get("image").toString()).placeholder(R.drawable.ic_launcher).into(viewHolder.imagePreview);
                }


            } else if (holder instanceof WithViewHolder) {
                final WithViewHolder viewHolder = (WithViewHolder) holder;
                Map row = mapArrayList.get(position);
                if (row.containsKey("message") && row.containsKey("image")) {
                    viewHolder.txtImageDateTime.setVisibility(View.GONE);
                    viewHolder.txtInputDateTime.setVisibility(View.VISIBLE);
                    viewHolder.txtInputDateTime.setText(getFormattedTime(row.get("time").toString()));
                    viewHolder.text.setVisibility(View.VISIBLE);
                    viewHolder.text.setText(row.get("message").toString());
                    viewHolder.imgCard.setVisibility(View.VISIBLE);
                    viewHolder.imagePreview.setVisibility(View.VISIBLE);
                    Picasso.with(Chat.this).load(row.get("image").toString()).placeholder(R.drawable.ic_launcher).into(viewHolder.imagePreview);
                }

                if (!row.containsKey("image") && row.containsKey("message")) {
                    viewHolder.txtImageDateTime.setVisibility(View.GONE);
                    viewHolder.txtInputDateTime.setVisibility(View.VISIBLE);
                    viewHolder.txtInputDateTime.setText(getFormattedTime(row.get("time").toString()));
                    viewHolder.imgCard.setVisibility(View.GONE);
                    viewHolder.imagePreview.setVisibility(View.GONE);
                    viewHolder.text.setVisibility(View.VISIBLE);
                    viewHolder.text.setText(row.get("message").toString());
                }
                if (row.containsKey("image") && !row.containsKey("message")) {
                    viewHolder.text.setVisibility(View.GONE);
                    viewHolder.txtInputDateTime.setVisibility(View.GONE);
                    viewHolder.txtImageDateTime.setVisibility(View.VISIBLE);
                    viewHolder.txtImageDateTime.setText(getFormattedTime(row.get("time").toString()));
                    viewHolder.imgCard.setVisibility(View.VISIBLE);
                    viewHolder.imagePreview.setVisibility(View.VISIBLE);
                    Picasso.with(Chat.this).load(row.get("image").toString()).placeholder(R.drawable.ic_launcher).into(viewHolder.imagePreview);
                }

            }
        }


        @Override
        public int getItemCount() {
            return mapArrayList.size();
        }

        private class YouViewHolder extends RecyclerView.ViewHolder {


            private final CardView imgCard;
            private final TextView text;
            private final ImageView imagePreview;
            private final TextView txtInputDateTime;
            private final TextView txtImageDateTime;

            public YouViewHolder(View parentView) {
                super(parentView);
                text = (TextView) parentView.findViewById(R.id.txtInputBox);
                imagePreview = (ImageView) parentView.findViewById(R.id.imgPreview);
                txtInputDateTime = (TextView) parentView.findViewById(R.id.txtInputDateTime);
                txtImageDateTime = (TextView) parentView.findViewById(R.id.txtImageDateTime);
                imgCard = (CardView) parentView.findViewById(R.id.imgCard);
            }
        }

        private class WithViewHolder extends RecyclerView.ViewHolder {
            private final CardView imgCard;
            private final TextView text;
            private final ImageView imagePreview;
            private final TextView txtInputDateTime;
            private final TextView txtImageDateTime;


            public WithViewHolder(View parentView) {
                super(parentView);
                text = (TextView) parentView.findViewById(R.id.txtInputBox);
                imagePreview = (ImageView) parentView.findViewById(R.id.imgPreview);
                txtInputDateTime = (TextView) parentView.findViewById(R.id.txtInputDateTime);
                txtImageDateTime = (TextView) parentView.findViewById(R.id.txtImageDateTime);
                imgCard = (CardView) parentView.findViewById(R.id.imgCard);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {

        if (requestCode == PERMISSIONS_REQUEST_CODE_WRITE_EXTERNAL_STORAGE
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            OpenImageChooser();
        } else {
            Toast.makeText(Chat.this, "No Permission", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == SELECT_PICTURE) {
                final boolean isCamera;
                if (data == null) {
                    isCamera = true;
                } else {
                    final String action = data.getAction();
                    if (action == null) {
                        isCamera = false;
                    } else {
                        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                            isCamera = true;
                        } else {
                            isCamera = action.equals(MediaStore.ACTION_IMAGE_CAPTURE);
                        }
                    }
                }

                storageRef = storage.getReference("chat_photos_" + getString(R.string.app_name));
                if (isCamera) {
                    selectedImagePath = String.valueOf(imgPath);
                    scaleImage(selectedImagePath);
                    imgLayout.setVisibility(View.VISIBLE);
                    imgPreview.setImageURI(Uri.parse(selectedImagePath));
                    final StorageReference photoRef = storageRef.child(imgPath.getLastPathSegment());
                    // Upload file to Firebase Storage
                    photoRef.putFile(imgPath)
                            .addOnSuccessListener(Chat.this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                    // When the image has successfully uploaded, we get its download URL
                                    downloadUrl = taskSnapshot.getDownloadUrl();
                                    tickNow = true;
                                    Log.d("DURL_CAM = ", String.valueOf(downloadUrl));
                                    // Send message with Image URL

                                }
                            });
                } else {
                    selectedImagePath = getAbsolutePath(data.getData());
                    selectedImagePath = getRightAngleImage(selectedImagePath);
                    scaleImage(selectedImagePath);
                    imgLayout.setVisibility(View.VISIBLE);
                    imgPreview.setImageURI(Uri.parse(selectedImagePath));
                    final StorageReference photoRef = storageRef.child(data.getData().getLastPathSegment());
                    // Upload file to Firebase Storage
                    photoRef.putFile(data.getData())
                            .addOnSuccessListener(Chat.this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                    // When the image has successfully uploaded, we get its download URL
                                    downloadUrl = taskSnapshot.getDownloadUrl();
                                    tickNow = true;
                                    Log.d("DURL_GALLERY = ", String.valueOf(downloadUrl));
                                    // Send message with Image URL

                                }
                            });
                }


            }
        }
    }

    /**
     * This method is used to get right angle of image, this method will automatically make image oriented as per its aspect ratio.
     *
     * @param photoPath represents selected image path.
     * @return String
     */
    public String getRightAngleImage(String photoPath) {
        try {
            ExifInterface ei = new ExifInterface(photoPath);
            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return rotateImage(90, photoPath);
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return rotateImage(180, photoPath);
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return rotateImage(270, photoPath);
                default:
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return photoPath;
    }

    /**
     * This method is used to get image uri from file path.
     *
     * @param path represents image path of SD card.
     * @return Uri
     */
    public Uri getImageUri(String path) {
        return Uri.fromFile(new File(path));
    }

    /**
     * This method is used to decode image file path to bitmap.
     *
     * @param path represents selected image path.
     * @return Bitmap
     */
    public Bitmap decodeFileFromPath(String path) {
        Uri uri = getImageUri(path);
        InputStream in = null;
        try {
            in = getContentResolver().openInputStream(uri);

            // Decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;

            BitmapFactory.decodeStream(in, null, o);
            in.close();

            int scale = 1;
            int inSampleSize = 1024;
            if (o.outHeight > inSampleSize || o.outWidth > inSampleSize) {
                scale = (int) Math.pow(2, (int) Math.round(Math.log(inSampleSize / (double) Math.max(o.outHeight, o.outWidth)) / Math.log(0.5)));
            }

            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            in = getContentResolver().openInputStream(uri);
            Bitmap b = BitmapFactory.decodeStream(in, null, o2);
            in.close();

            return b;

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * This method is used to change orietation and rotate image as per needed as per its aspect ratio.
     *
     * @param degree represents degree to rotate the image.
     * @param path   represents selected image path.
     * @return String
     */
    public String rotateImage(int degree, String path) {
        try {
            Bitmap b = decodeFileFromPath(path);

            Matrix matrix = new Matrix();
            if (b.getWidth() > b.getHeight()) {
                matrix.setRotate(degree);
                b = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(),
                        matrix, true);
            }

            FileOutputStream fOut = new FileOutputStream(path);
            b.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
            fOut.flush();
            fOut.close();

            b.recycle();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return path;
    }

    /**
     * This method used to get absolute path from uri.
     *
     * @param uri represented uri
     * @return represented {@link String}
     */
    public String getAbsolutePath(Uri uri) {
        if (Build.VERSION.SDK_INT < 11)
            return RealPathUtil.getRealPathFromURI_BelowAPI11(this, uri);

            // SDK >= 11 && SDK < 19
        else if (Build.VERSION.SDK_INT < 19)
            return RealPathUtil.getRealPathFromURI_API11to18(this, uri);

            // SDK > 19 (Android 4.4)
        else
            return RealPathUtil.getRealPathFromURI_API19(this, uri);
    }

    public String scaleImage(String path) {
        String strMyImagePath = null;
        Bitmap scaledBitmap = null;

        try {
            // Part 1: Decode image
            Bitmap unscaledBitmap = ScalingUtilities.decodeFile(path, 800, 800, ScalingUtilities.ScalingLogic.FIT);

            if (!(unscaledBitmap.getWidth() <= 800 && unscaledBitmap.getHeight() <= 800)) {
                // Part 2: Scale image
                scaledBitmap = ScalingUtilities.createScaledBitmap(unscaledBitmap, 800, 800, ScalingUtilities.ScalingLogic.FIT);
            } else {
                unscaledBitmap.recycle();
                return path;
            }

            // Store to tmp file

            String extr = Environment.getExternalStorageDirectory().toString();
            File mFolder = new File(extr + "/myTmpDir");
            if (!mFolder.exists()) {
                mFolder.mkdir();
            }

            String s = "tmp.png";

            File f = new File(path);

            strMyImagePath = f.getAbsolutePath();
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(f);
                scaledBitmap.compress(Bitmap.CompressFormat.PNG, 70, fos);
                fos.flush();
                fos.close();
            } catch (FileNotFoundException e) {

                e.printStackTrace();
            } catch (Exception e) {

                e.printStackTrace();
            }

            scaledBitmap.recycle();
        } catch (Throwable e) {
        }

        if (strMyImagePath == null) {
            return path;
        }
        return strMyImagePath;

    }

    private void OpenImageChooser() {
        // Camera.
        final List<Intent> cameraIntents = new ArrayList<Intent>();
        final Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        final PackageManager packageManager = getPackageManager();
        final List<ResolveInfo> listCam = packageManager.queryIntentActivities(captureIntent, 0);
        for (ResolveInfo res : listCam) {
            final String packageName = res.activityInfo.packageName;
            final Intent intent = new Intent(captureIntent);
            intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
            intent.setPackage(packageName);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, setImageUri());
            cameraIntents.add(intent);
        }

        // Filesystem.
        final Intent galleryIntent = new Intent();
        galleryIntent.setType("image/*");
        galleryIntent.setAction(Intent.ACTION_GET_CONTENT);

        // Chooser of filesystem options.
        final Intent chooserIntent = Intent.createChooser(galleryIntent, "Select Source");

        // Add the camera options.
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, cameraIntents.toArray(new Parcelable[cameraIntents.size()]));

        startActivityForResult(chooserIntent, SELECT_PICTURE);
    }

    public Uri setImageUri() {
        // Store image in dcim
        File file = new File(Environment.getExternalStorageDirectory() + "/DCIM/", "image" + new Date().getTime() + ".png");
//        Uri imgUri = Uri.fromFile(file);
        imgPath = Uri.fromFile(file);
//        imgPath = file.getAbsolutePath();
        return imgPath;
    }

    public boolean CheckPermissionForWriteStorage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSIONS_REQUEST_CODE_WRITE_EXTERNAL_STORAGE);

            return false;
        }

        return true;
    }

    public void fetch(final String typingValue) {
        String url = "https://realfirechat.firebaseio.com/Typing.json";

        StringRequest request = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String s) {
                Log.d("FETCHED - ", s);
                try {
                    JSONObject allJsonObj = new JSONObject(s);
                    JSONObject chatWithJsonObj = allJsonObj.getJSONObject(UserDetails.chatWith);
                    if (chatWithJsonObj.getString("typeWith").equals(UserDetails.username)) {
                        typeWithBool = true;
                    } else {
                        typeWithBool = false;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }


                Firebase reference = new Firebase("https://realfirechat.firebaseio.com/Typing");

                if (s.equals("null")) {
                    reference.child(UserDetails.username).child("isTyping").setValue(typingValue);
                    reference.child(UserDetails.username).child("typeWith").setValue(UserDetails.chatWith);
                } else {
                    try {
                        JSONObject obj = new JSONObject(s);

                        if (!obj.has(UserDetails.username)) {
                            reference.child(UserDetails.username).child("isTyping").setValue(typingValue);
                            reference.child(UserDetails.username).child("typeWith").setValue(UserDetails.chatWith);
                        } else {
                            reference.child(UserDetails.username).child("isTyping").setValue(typingValue);
                            reference.child(UserDetails.username).child("typeWith").setValue(UserDetails.chatWith);
                        }

                    } catch (JSONException e) {

                        e.printStackTrace();
                    }
                }
                try {
                    JSONObject allJsonObj = new JSONObject(s);
                    JSONObject chatWithJsonObj = allJsonObj.getJSONObject(UserDetails.chatWith);
                    if (chatWithJsonObj.getString("typeWith").equals(UserDetails.username)) {
                        typeWithBool = true;
                    } else {
                        typeWithBool = false;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }

        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                System.out.println("" + volleyError);
            }
        });

        RequestQueue rQueue = Volley.newRequestQueue(Chat.this);
        rQueue.add(request);
    }

//    public void addMessageBox(String message, String time, String imgFile, int type) {
//        LayoutInflater inflater = LayoutInflater.from(Chat.this);
//
//        if (type == 1) {//YOU
//            View fieldView = inflater.inflate(R.layout.sender_message_conversation, null);
//            TextView text = (TextView) fieldView.findViewById(R.id.txtInputBox);
//            ImageView imagePreview = (ImageView) fieldView.findViewById(R.id.imgPreview);
//            TextView txtInputDateTime = (TextView) fieldView.findViewById(R.id.txtInputDateTime);
//            CardView imgCard = (CardView) fieldView.findViewById(R.id.imgCard);
//            if (message.length() != 0) {
//                text.setVisibility(View.VISIBLE);
//                text.setText(message);
//            }
//            txtInputDateTime.setText(getFormattedTime(time));
//            if (imgFile.length() != 0) {
//                imgCard.setVisibility(View.VISIBLE);
//                imagePreview.setVisibility(View.VISIBLE);
//                Picasso.with(Chat.this).load(imgFile).placeholder(R.drawable.ic_launcher).into(imagePreview);
//
//            }
//
//            layout.addView(fieldView);
//
//        } else {//SAAMNE WALA
//            View fieldView = inflater.inflate(R.layout.reciever_message_conversation, null);
//            TextView text = (TextView) fieldView.findViewById(R.id.txtInputBox);
//            ImageView imagePreview = (ImageView) fieldView.findViewById(R.id.imgPreview);
//            TextView txtInputDateTime = (TextView) fieldView.findViewById(R.id.txtInputDateTime);
//            CardView imgCard = (CardView) fieldView.findViewById(R.id.imgCard);
//            if (message.length() != 0) {
//                text.setVisibility(View.VISIBLE);
//                text.setText(message);
//            }
//            txtInputDateTime.setText(getFormattedTime(time));
//            if (imgFile.length() != 0) {
//                imgCard.setVisibility(View.VISIBLE);
//                imagePreview.setVisibility(View.VISIBLE);
//                Picasso.with(Chat.this).load(imgFile).placeholder(R.drawable.ic_launcher).into(imagePreview);
//
//            }
//
//            layout.addView(fieldView);
//        }
//
//
//        scrollView.fullScroll(View.FOCUS_DOWN);
//    }

    public static String getFormattedTime(String time) {
        Date parsedDate = null;
        SimpleDateFormat input = new SimpleDateFormat("HH:mm:ss");
        SimpleDateFormat output = new SimpleDateFormat("HH:mm");
        try {
            parsedDate = input.parse(time);
            return output.format(parsedDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }
}