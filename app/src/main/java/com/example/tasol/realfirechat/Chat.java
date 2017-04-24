package com.example.tasol.realfirechat;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

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
import com.romainpiel.shimmer.Shimmer;
import com.romainpiel.shimmer.ShimmerTextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
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
        layout = (LinearLayout) findViewById(R.id.layout1);
        sendButton = (TextView) findViewById(R.id.sendButton);
        messageArea = (EditText) findViewById(R.id.messageArea);
        scrollView = (ScrollView) findViewById(R.id.scrollView);
        txtTyping = (ShimmerTextView) findViewById(R.id.txtTyping);

        Firebase.setAndroidContext(this);

        reference1 = new Firebase("https://realfirechat.firebaseio.com/messages/" + UserDetails.username + "_" + UserDetails.chatWith);
        reference2 = new Firebase("https://realfirechat.firebaseio.com/messages/" + UserDetails.chatWith + "_" + UserDetails.username);
        referenceTyping = new Firebase("https://realfirechat.firebaseio.com/Typing/" + UserDetails.chatWith);

        txtTyping.setText(UserDetails.chatWith + " is Typing...");

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

                if (!messageText.equals("")) {
                    Map<String, String> map = new HashMap<String, String>();
                    map.put("message", messageText);
                    map.put("user", UserDetails.username);
                    map.put("date", currentDate);
                    map.put("time", currentTime);
                    reference1.push().setValue(map);
                    reference2.push().setValue(map);
                    messageArea.setText("");
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
                String message = map.get("message").toString();
                String userName = map.get("user").toString();
                String datestamp = map.get("date").toString();
                String timestamp = map.get("time").toString();

                if (userName.equals(UserDetails.username)) {
                    addMessageBox(message, timestamp, 1);
                } else {
                    addMessageBox(message, timestamp, 2);
                }
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

    public void addMessageBox(String message, String time, int type) {
        LayoutInflater inflater = LayoutInflater.from(Chat.this);

        if (type == 1) {
            View fieldView = inflater.inflate(R.layout.sender_message_conversation, null);
            TextView text = (TextView) fieldView.findViewById(R.id.txtInputBox);
            TextView txtInputDateTime = (TextView) fieldView.findViewById(R.id.txtInputDateTime);
            text.setText(message);
            txtInputDateTime.setText(getFormattedTime(time));
//            textView.setBackgroundColor(getResources().getColor(R.color.accent));
//            textView.setGravity(Gravity.LEFT);
//            textView.setPadding(5,5,5,5);
//            textView.setTextSize(30);
//            textView.setTextColor(getResources().getColor(R.color.white));
            layout.addView(fieldView);

        } else {
            View fieldView = inflater.inflate(R.layout.reciever_message_conversation, null);
            TextView text = (TextView) fieldView.findViewById(R.id.txtInputBox);
            TextView txtInputDateTime = (TextView) fieldView.findViewById(R.id.txtInputDateTime);
            text.setText(message);
            txtInputDateTime.setText(getFormattedTime(time));
            //            textView.setBackgroundColor(getResources().getColor(R.color.darkPrimary));
//            textView.setGravity(Gravity.RIGHT);
//            textView.setTextColor(getResources().getColor(R.color.white));
//            textView.setPadding(5,5,5,5);
//            textView.setTextSize(30);
            layout.addView(fieldView);
        }

//        TextView textView = new TextView(Chat.this);
//        textView.setText(message);
//        textView.setTextColor(Color.WHITE);
//        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
//        lp.setMargins(0, 0, 0, 10);
//        textView.setLayoutParams(lp);
//
//        if (type == 1) {
//            textView.setGravity(Gravity.RIGHT);
//            textView.setBackgroundResource(R.drawable.rounded_corner1);
//        } else {
//            textView.setGravity(Gravity.LEFT);
//            textView.setBackgroundResource(R.drawable.rounded_corner2);
//        }
//
//        layout.addView(textView);
        scrollView.fullScroll(View.FOCUS_DOWN);
    }

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