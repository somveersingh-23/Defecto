package com.crriccn.defecto;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.PrivateKey;
import java.util.ArrayList;

import okhttp3.*;

public class ChatActivity extends AppCompatActivity {

    EditText messageEditText;
    Button sendButton;
    RecyclerView chatRecyclerView;
    ArrayList<MessageModel> messageList;
    ChatAdapter adapter;

    // 🔐 Replace this with your actual Gemini API Key securely in production
    private String GEMINI_API_KEY;
    private String GEMINI_API_URL;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        GEMINI_API_KEY = MetaDataUtil.getMetaDataValue(this,"GEMINI_API_KEY");
        GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + GEMINI_API_KEY;
        messageEditText = findViewById(R.id.messageEditText);
        sendButton = findViewById(R.id.sendButton);
        chatRecyclerView = findViewById(R.id.chatRecyclerView);

        messageList = new ArrayList<>();
        adapter = new ChatAdapter(messageList);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(adapter);

        sendButton.setOnClickListener(v -> {
            String userMessage = messageEditText.getText().toString().trim();
            if (!userMessage.isEmpty()) {
                messageList.add(new MessageModel(userMessage, MessageModel.USER));
                adapter.notifyItemInserted(messageList.size() - 1);
                chatRecyclerView.scrollToPosition(messageList.size() - 1);
                messageEditText.setText("");
                getBotResponse(userMessage);
            }
        });
    }

    private void getBotResponse(String message) {
        OkHttpClient client = new OkHttpClient();

        // Build JSON body as required by Gemini API
        JSONObject jsonBody = new JSONObject();
        try {
            JSONArray partsArray = new JSONArray();
            JSONObject part = new JSONObject();
            part.put("text", message);
            partsArray.put(part);

            JSONObject content = new JSONObject();
            content.put("role", "user");
            content.put("parts", partsArray);

            JSONArray contents = new JSONArray();
            contents.put(content);

            jsonBody.put("contents", contents);
        } catch (JSONException e) {
            runOnUiThread(() -> addBotMessage("Failed to build request JSON: " + e.getMessage()));
            return;
        }

        RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(GEMINI_API_URL)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> addBotMessage("Failed to connect: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> addBotMessage("API Error: " + response.code() + "\n" + responseBody));
                    return;
                }

                try {
                    JSONObject json = new JSONObject(responseBody);
                    JSONArray candidates = json.getJSONArray("candidates");

                    JSONObject firstCandidate = candidates.getJSONObject(0);
                    JSONObject content = firstCandidate.getJSONObject("content");
                    JSONArray parts = content.getJSONArray("parts");

                    String botReply = parts.getJSONObject(0).getString("text");
                    runOnUiThread(() -> addBotMessage(botReply.trim()));

                } catch (Exception e) {
                    runOnUiThread(() -> addBotMessage("Response parse error: " + e.getMessage() + "\nRaw response:\n" + responseBody));
                }
            }
        });
    }

    private void addBotMessage(String message) {
        messageList.add(new MessageModel(message, MessageModel.BOT));
        adapter.notifyItemInserted(messageList.size() - 1);
        chatRecyclerView.scrollToPosition(messageList.size() - 1);
    }
}
