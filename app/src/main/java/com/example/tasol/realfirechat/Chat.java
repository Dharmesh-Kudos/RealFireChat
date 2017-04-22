package com.example.tasol.realfirechat;

import android.content.ContentValues;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
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
import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Chat extends AppCompatActivity {
    LinearLayout layout;
    TextView sendButton;
    EditText messageArea;
    ScrollView scrollView;
    Firebase reference1, reference2, referenceTyping;
    private boolean typingStarted;
    TextView txtTyping;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);




        layout = (LinearLayout) findViewById(R.id.layout1);
        sendButton = (TextView) findViewById(R.id.sendButton);
        messageArea = (EditText) findViewById(R.id.messageArea);
        scrollView = (ScrollView) findViewById(R.id.scrollView);
        txtTyping = (TextView) findViewById(R.id.txtTyping);
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
//                    Map<String, String> map = new HashMap<String, String>();
//                    map.put("isTyping", "true");
//                    referenceTyping.push().setValue(map);
                    //send typing started status
                } else if (s.toString().trim().length() == 0 && typingStarted) {
                    //Log.i(TAG, “typing stopped event…”);
                    typingStarted = false;
                    fetch("false");
//                    Map<String, String> map = new HashMap<String, String>();
//                    map.put("isTyping", "false");
//                    referenceTyping.push().setValue(map);
                    //send typing stopped status
                }

            }
        });


        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String messageText = messageArea.getText().toString();

                SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
                String currentDateandTime = sdf.format(new Date());

                if (!messageText.equals("")) {
                    Map<String, String> map = new HashMap<String, String>();
                    map.put("message", messageText);
                    map.put("user", UserDetails.username);
                    map.put("timestamp", currentDateandTime);
                    reference1.push().setValue(map);
                    reference2.push().setValue(map);
                    messageArea.setText("");
                }
            }
        });


        referenceTyping.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
//                Log.d("S = ", s.toString());
//
//                Map map = dataSnapshot.getValue(Map.class);
//                Log.d("MAP = ", map.toString());
                //String isTyping = map.get("isTyping").toString();
                if (dataSnapshot.getValue().equals("true")) {
                    Log.d("isTyping = ", "TRUE");
                    txtTyping.setVisibility(View.VISIBLE);
                } else {
                    Log.d("isTyping = ", "FALSE");
                    txtTyping.setVisibility(View.GONE);
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                if (dataSnapshot.getValue().equals("true")) {
                    Log.d("isTyping = ", "TRUE");
                    txtTyping.setVisibility(View.VISIBLE);
                } else {
                    Log.d("isTyping = ", "FALSE");
                    txtTyping.setVisibility(View.GONE);
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
                String timestamp = map.get("timestamp").toString();

                if (userName.equals(UserDetails.username)) {
                    addMessageBox("You:-\n" + message + "\n " + timestamp, 1);
                } else {
                    addMessageBox(UserDetails.chatWith + ":-\n" + message + "\n " + timestamp, 2);
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
                Firebase reference = new Firebase("https://realfirechat.firebaseio.com/Typing");

                if (s.equals("null")) {
                    reference.child(UserDetails.username).child("isTyping").setValue(typingValue);
                } else {
                    try {
                        JSONObject obj = new JSONObject(s);

                        if (!obj.has(UserDetails.username)) {
                            reference.child(UserDetails.username).child("isTyping").setValue(typingValue);
                        } else {
                            reference.child(UserDetails.username).child("isTyping").setValue(typingValue);
                        }

                    } catch (JSONException e) {

                        e.printStackTrace();
                    }
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

    public void addMessageBox(String message, int type) {
        TextView textView = new TextView(Chat.this);
        textView.setText(message);
        textView.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 10);
        textView.setLayoutParams(lp);

        if (type == 1) {
            textView.setGravity(Gravity.RIGHT);
            textView.setBackgroundResource(R.drawable.rounded_corner1);
        } else {
            textView.setGravity(Gravity.LEFT);
            textView.setBackgroundResource(R.drawable.rounded_corner2);
        }

        layout.addView(textView);
        scrollView.fullScroll(View.FOCUS_DOWN);
    }
}