package org.levimc.launcher.oblivion;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;

import org.json.JSONArray;
import org.json.JSONObject;
import org.levimc.launcher.R;

/**
 * Party: friends-only groups with text chat, backed by the Oblivion server. A party only lists accepted
 * friends and nobody else can see it. Voice chat is not implemented yet (needs a realtime media server).
 */
public class PartyActivity extends OblivionBaseActivity {
    private enum Mode {LIST, FRIENDS, CHAT}

    private Mode mode = Mode.LIST;
    private String partyId;
    private String partyName = "";
    private long lastMsgId;
    private LinearLayout chatBox;
    private ScrollView chatScroll;
    private final Handler poll = new Handler(Looper.getMainLooper());
    private final Runnable pollTask = new Runnable() {
        @Override
        public void run() {
            if (mode == Mode.CHAT && partyId != null) fetchMessages();
            poll.postDelayed(this, 3000);
        }
    };

    @Override
    protected int titleRes() {
        return R.string.oblivion_party;
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (mode != Mode.LIST) {
                    mode = Mode.LIST;
                    rebuild();
                } else {
                    finish();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        poll.postDelayed(pollTask, 3000);
    }

    @Override
    protected void onPause() {
        poll.removeCallbacks(pollTask);
        super.onPause();
    }

    private String token() {
        return OblivionSession.token(this);
    }

    @Override
    protected void buildContent(LinearLayout col) {
        if (!OblivionConfig.isServerConfigured(this) || token() == null) {
            LinearLayout card = OUi.card(this);
            card.addView(OUi.title(this, getString(R.string.oblivion_party)));
            card.addView(OUi.body(this, getString(R.string.oblivion_party_need_account)));
            if (OblivionConfig.isServerConfigured(this)) {
                card.addView(OUi.button(this, getString(R.string.oblivion_link_discord), true,
                        v -> OblivionSession.beginLogin(this)));
            }
            col.addView(card);
            return;
        }
        if (mode == Mode.LIST) buildList(col);
        else if (mode == Mode.FRIENDS) buildFriends(col);
        else buildChat(col);
    }

    private EditText input(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setHintTextColor(OblivionTheme.textDim);
        e.setTextColor(OblivionTheme.text);
        e.setTypeface(OUi.font(this));
        e.setInputType(InputType.TYPE_CLASS_TEXT);
        return e;
    }

    private void toast(String m) {
        Toast.makeText(this, m, Toast.LENGTH_SHORT).show();
    }

    private void buildList(LinearLayout col) {
        LinearLayout create = OUi.card(this);
        create.addView(OUi.title(this, getString(R.string.oblivion_party_create)));
        EditText name = input(getString(R.string.oblivion_party_name_hint));
        create.addView(name);
        create.addView(OUi.button(this, getString(R.string.oblivion_party_create), true, v -> {
            try {
                OblivionApi.postJson(this, "/v1/party", token(), new JSONObject().put("name", name.getText().toString()),
                        new OblivionApi.JsonCb() {
                            @Override
                            public void ok(JSONObject json) {
                                rebuild();
                            }

                            @Override
                            public void fail(String message) {
                                toast(message);
                            }
                        });
            } catch (Exception ignored) {
            }
        }));
        col.addView(create);
        col.addView(OUi.tile(this, getString(R.string.oblivion_friends), getString(R.string.oblivion_party_friends_sub), v -> {
            mode = Mode.FRIENDS;
            rebuild();
        }));
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        col.addView(list);
        OblivionApi.get(this, "/v1/party", token(), new OblivionApi.JsonCb() {
            @Override
            public void ok(JSONObject json) {
                JSONArray items = json.optJSONArray("items");
                if (items == null || items.length() == 0) {
                    list.addView(OUi.body(PartyActivity.this, getString(R.string.oblivion_party_none)));
                    return;
                }
                for (int i = 0; i < items.length(); i++) {
                    JSONObject p = items.optJSONObject(i);
                    if (p == null) continue;
                    JSONArray members = p.optJSONArray("members");
                    StringBuilder sb = new StringBuilder();
                    if (members != null) {
                        for (int k = 0; k < members.length(); k++) {
                            if (k > 0) sb.append(", ");
                            sb.append(members.optString(k));
                        }
                    }
                    final String id = p.optString("id");
                    final String nm = p.optString("name");
                    list.addView(OUi.tile(PartyActivity.this, nm, sb.toString(), v -> {
                        partyId = id;
                        partyName = nm;
                        lastMsgId = 0;
                        mode = Mode.CHAT;
                        rebuild();
                    }));
                }
            }

            @Override
            public void fail(String message) {
                toast(message);
            }
        });
    }

    private void buildFriends(LinearLayout col) {
        LinearLayout add = OUi.card(this);
        add.addView(OUi.title(this, getString(R.string.oblivion_party_add_friend)));
        EditText name = input(getString(R.string.oblivion_party_friend_hint));
        add.addView(name);
        add.addView(OUi.button(this, getString(R.string.oblivion_party_send_request), true, v -> {
            try {
                OblivionApi.postJson(this, "/v1/friends/request", token(),
                        new JSONObject().put("username", name.getText().toString()), simple(R.string.oblivion_party_request_sent));
            } catch (Exception ignored) {
            }
        }));
        col.addView(add);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        col.addView(list);
        OblivionApi.get(this, "/v1/friends", token(), new OblivionApi.JsonCb() {
            @Override
            public void ok(JSONObject json) {
                section(list, R.string.oblivion_party_incoming, json.optJSONArray("incoming"), true);
                section(list, R.string.oblivion_friends, json.optJSONArray("friends"), false);
                section(list, R.string.oblivion_party_outgoing, json.optJSONArray("outgoing"), false);
            }

            @Override
            public void fail(String message) {
                toast(message);
            }
        });
    }

    private void section(LinearLayout list, int title, JSONArray arr, boolean accept) {
        if (arr == null || arr.length() == 0) return;
        list.addView(OUi.title(this, getString(title)));
        for (int i = 0; i < arr.length(); i++) {
            JSONObject f = arr.optJSONObject(i);
            if (f == null) continue;
            LinearLayout card = OUi.card(this);
            card.addView(OUi.title(this, f.optString("username")));
            final String id = f.optString("discord_id");
            if (accept) {
                card.addView(OUi.button(this, getString(R.string.oblivion_accept), true, v -> post("/v1/friends/accept", id, true)));
            }
            card.addView(OUi.button(this, getString(R.string.oblivion_party_remove), false, v -> post("/v1/friends/remove", id, true)));
            list.addView(card);
        }
    }

    private void post(String path, String discordId, boolean refresh) {
        try {
            OblivionApi.postJson(this, path, token(), new JSONObject().put("discord_id", discordId), new OblivionApi.JsonCb() {
                @Override
                public void ok(JSONObject json) {
                    if (refresh) rebuild();
                }

                @Override
                public void fail(String message) {
                    toast(message);
                }
            });
        } catch (Exception ignored) {
        }
    }

    private OblivionApi.JsonCb simple(int okMsg) {
        return new OblivionApi.JsonCb() {
            @Override
            public void ok(JSONObject json) {
                toast(getString(okMsg));
            }

            @Override
            public void fail(String message) {
                toast(message);
            }
        };
    }

    private void buildChat(LinearLayout col) {
        LinearLayout head = OUi.card(this);
        head.addView(OUi.title(this, partyName));
        TextView voice = OUi.button(this, getString(R.string.oblivion_party_voice), false, null);
        voice.setEnabled(false);
        voice.setAlpha(0.5f);
        head.addView(voice);
        head.addView(OUi.body(this, getString(R.string.oblivion_party_voice_soon)));
        head.addView(OUi.button(this, getString(R.string.oblivion_party_invite), false, v -> pickInvite()));
        head.addView(OUi.button(this, getString(R.string.oblivion_party_leave), false, v -> {
            OblivionApi.postJson(this, "/v1/party/" + partyId + "/leave", token(), new JSONObject(), new OblivionApi.JsonCb() {
                @Override
                public void ok(JSONObject json) {
                    mode = Mode.LIST;
                    rebuild();
                }

                @Override
                public void fail(String message) {
                    toast(message);
                }
            });
        }));
        col.addView(head);

        chatScroll = new ScrollView(this);
        chatBox = new LinearLayout(this);
        chatBox.setOrientation(LinearLayout.VERTICAL);
        chatScroll.addView(chatBox);
        col.addView(chatScroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, OUi.dp(this, 220)));
        EditText msg = input(getString(R.string.oblivion_party_message_hint));
        col.addView(msg);
        col.addView(OUi.button(this, getString(R.string.oblivion_party_send), true, v -> {
            String text = msg.getText().toString().trim();
            if (text.isEmpty()) return;
            try {
                OblivionApi.postJson(this, "/v1/party/" + partyId + "/messages", token(), new JSONObject().put("text", text),
                        new OblivionApi.JsonCb() {
                            @Override
                            public void ok(JSONObject json) {
                                msg.setText("");
                                fetchMessages();
                            }

                            @Override
                            public void fail(String message) {
                                toast(message);
                            }
                        });
            } catch (Exception ignored) {
            }
        }));
        fetchMessages();
    }

    private void fetchMessages() {
        if (chatBox == null || partyId == null) return;
        OblivionApi.get(this, "/v1/party/" + partyId + "/messages?after=" + lastMsgId, token(), new OblivionApi.JsonCb() {
            @Override
            public void ok(JSONObject json) {
                JSONArray arr = json.optJSONArray("messages");
                if (arr == null || arr.length() == 0 || chatBox == null) return;
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject m = arr.optJSONObject(i);
                    if (m == null) continue;
                    lastMsgId = Math.max(lastMsgId, m.optLong("id"));
                    TextView t = OUi.body(PartyActivity.this, m.optString("user") + ": " + m.optString("text"));
                    t.setTextColor(m.optBoolean("mine") ? OblivionTheme.launchLight : OblivionTheme.text);
                    t.setPadding(0, OUi.dp(PartyActivity.this, 3), 0, OUi.dp(PartyActivity.this, 3));
                    chatBox.addView(t);
                }
                chatScroll.post(() -> chatScroll.fullScroll(View.FOCUS_DOWN));
            }

            @Override
            public void fail(String message) {
            }
        });
    }

    private void pickInvite() {
        OblivionApi.get(this, "/v1/friends", token(), new OblivionApi.JsonCb() {
            @Override
            public void ok(JSONObject json) {
                JSONArray arr = json.optJSONArray("friends");
                if (arr == null || arr.length() == 0) {
                    toast(getString(R.string.oblivion_party_no_friends));
                    return;
                }
                String[] names = new String[arr.length()];
                String[] ids = new String[arr.length()];
                for (int i = 0; i < arr.length(); i++) {
                    names[i] = arr.optJSONObject(i).optString("username");
                    ids[i] = arr.optJSONObject(i).optString("discord_id");
                }
                new androidx.appcompat.app.AlertDialog.Builder(PartyActivity.this)
                        .setTitle(R.string.oblivion_party_invite)
                        .setItems(names, (d, which) -> {
                            try {
                                OblivionApi.postJson(PartyActivity.this, "/v1/party/" + partyId + "/invite", token(),
                                        new JSONObject().put("discord_id", ids[which]), simple(R.string.oblivion_party_invited));
                            } catch (Exception ignored) {
                            }
                        }).show();
            }

            @Override
            public void fail(String message) {
                toast(message);
            }
        });
    }
}
