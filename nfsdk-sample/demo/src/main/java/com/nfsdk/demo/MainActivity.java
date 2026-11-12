package com.nfsdk.demo;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import com.nfsdk.NFsdk;
import com.nfsdk.PageType;
import com.nfsdk.PaidType;
import com.nfsdk.PaymentListener;
import com.nfsdk.ProductObject;
import com.nfsdk.TriggerEnumType;
import com.nfsdk.UserListener;

/** Demo NFsdk: mỗi nút gọi 1 API public. Kết quả callback hiện ở khung log. */
public class MainActivity extends Activity {
    private TextView log;
    private ScrollView pageScroll;
    // chưa đăng nhập ⇒ chỉ nút "Sign in"; đăng nhập xong ⇒ thông tin tài khoản + các nút API
    private Button loginBtn;
    private TextView account;
    private GridLayout grid;
    // giá trị nhập lần trước trong hộp thoại payment
    private String lastProductId = "com.joyplay.pack1";
    private String lastAmount = "25000";

    private final UserListener userListener = new UserListener() {
        @Override
        public void onSignInInfo(String userId, String username, String name, String accessToken) {
            print("onSignInInfo userId=" + userId + " username=" + username + " name=" + name
                    + " token=" + (accessToken == null ? "null" : accessToken.length() + " chars"));
            showSignedIn(userId, username, name);
        }

        @Override
        public void onSignOut() {
            print("onSignOut");
            showSignedOut();
        }

        @Override
        public void onFail(int code, String message) {
            print("onFail " + code + " " + message);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (BuildConfig.DEBUG) {
            // phát hiện I/O / mạng trên main do SDK gây ra
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectNetwork().penaltyLog().build());
        }
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        int p = (int) (12 * getResources().getDisplayMetrics().density);
        col.setPadding(p, p, p, p);
        TextView title = new TextView(this);
        title.setText("NFsdk Demo " + com.nfsdk.BuildConfig.SDK_VERSION);
        title.setTextSize(18);
        col.addView(title);

        // Chưa đăng nhập: chỉ 1 nút "Sign in"
        loginBtn = new Button(this);
        loginBtn.setAllCaps(false);
        loginBtn.setText("Sign in");
        loginBtn.setTextSize(16);
        loginBtn.setOnClickListener(v -> NFsdk.getInstance().showSignIn(this, userListener));
        LinearLayout.LayoutParams loginLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        loginLp.topMargin = p;
        col.addView(loginBtn, loginLp);

        // Đã đăng nhập: thông tin tài khoản + các nút API
        account = new TextView(this);
        account.setTextSize(14);
        account.setTextColor(Color.parseColor("#1C1C1E"));
        account.setPadding(p, p * 2 / 3, p, p * 2 / 3);
        GradientDrawable accBox = new GradientDrawable();
        accBox.setColor(Color.parseColor("#EEF6EE"));
        accBox.setCornerRadius(p * 0.7f);
        account.setBackground(accBox);
        LinearLayout.LayoutParams accLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        accLp.topMargin = p;
        accLp.bottomMargin = p / 2;
        col.addView(account, accLp);

        // Các nút API xếp lưới 2 cột cho gọn
        grid = new GridLayout(this);
        grid.setColumnCount(2);
        col.addView(grid, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        // "enter game" = kiểm bảo trì trước khi vào game (dialog().maintenance): không bảo trì ⇒ onOpen
        btn(grid, "enter game", v -> NFsdk.dialog().maintenance(this, () -> {
            print("enter game ⇒ onOpen (no maintenance)");
            // chữ của demo viết cứng — demo độc lập, không dùng ngôn ngữ của SDK
            new AlertDialog.Builder(this)
                    .setTitle("Enter game")
                    .setMessage("Entered the game successfully.")
                    .setPositiveButton("OK", null)
                    .show();
        }));
        btn(grid, "payment", v -> askPayment());
        //btn(grid, "phoneActive", v -> NFsdk.getInstance().phoneActive(this));
        btn(grid, "logout", v -> NFsdk.getInstance().logout(this));
        //btn(grid, "WEB_ADS", v -> NFsdk.dialog().show(this, TriggerEnumType.WEB_ADS, () -> print("ads ⇒ onOpen")));
        /*btn(grid, "setMaintenance(true)", v -> {
            NFsdk.getInstance().setMaintenance(true);
            print("setMaintenance(true)");
        });*/
        btn(grid, "test maintenance 10s", v -> { // true ⇒ maintenance ⇒ 10s ⇒ false (đóng trang)
            NFsdk.getInstance().setMaintenance(true);
            NFsdk.dialog().maintenance(this, () -> print("maintenance ⇒ onOpen"));
            v.postDelayed(() -> {
                NFsdk.getInstance().setMaintenance(false);
                print("setMaintenance(false) after 10s");
            }, 10000);
        });
        btn(grid, "tracking (all events)", v -> trackAll());
        btn(grid, "toPage FANPAGE", v -> NFsdk.toPage(this, PageType.FB_FANPAGE, () -> print("toPage ⇒ onCompleted")));
        btn(grid, "toPage GROUP", v -> NFsdk.toPage(this, PageType.FB_GROUP, () -> print("toPage ⇒ onCompleted")));
        btn(grid, "toPage APP_PAGE", v -> NFsdk.toPage(this, PageType.APP_PAGE, () -> print("toPage ⇒ onCompleted")));
        btn(grid, "toPage REVIEW", v -> NFsdk.toPage(this, PageType.REVIEW, () -> print("toPage ⇒ onCompleted")));

        // Khung log bên dưới các nút, mới nhất ở trên; cả màn (nút + log) cuộn chung ⇒ màn ngang vẫn xem đủ
        TextView logTitle = new TextView(this);
        logTitle.setText("Log");
        logTitle.setTextSize(13);
        logTitle.setTextColor(Color.GRAY);
        logTitle.setPadding(0, p, 0, p / 3);
        col.addView(logTitle);
        log = new TextView(this);
        log.setTextSize(12);
        log.setTextColor(Color.DKGRAY);
        log.setTextIsSelectable(true);
        log.setPadding(p, p, p, p);
        log.setMinHeight(p * 14);
        GradientDrawable box = new GradientDrawable();
        box.setColor(Color.WHITE);
        box.setCornerRadius(p * 0.7f);
        box.setStroke(Math.max(1, p / 12), Color.parseColor("#D6D6DC"));
        log.setBackground(box);
        col.addView(log, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        pageScroll = new ScrollView(this);
        pageScroll.setFillViewport(true);
        pageScroll.setVerticalScrollBarEnabled(true);
        pageScroll.addView(col);
        setContentView(pageScroll);
        showSignedOut();

        NFsdk.getInstance().setNotifyListener((type, data) -> {
            print("notify " + type + " " + data);
            if (NFsdk.NOTIFY_LOGOUT.equals(type)) showSignedOut();
        });
        // init lúc mở app rồi showSignIn
        NFsdk.getInstance().initSdk(this, s -> {
            print("initSdk ⇒ " + s);
            boolean noAuto = getIntent().getBooleanExtra("noAuto", false); // QA: adb ... --ez noAuto true
            if ("success".equals(s) && !noAuto) NFsdk.getInstance().showSignIn(this, userListener);
        });
    }

    private void showSignedIn(String userId, String username, String name) {
        runOnUiThread(() -> {
            account.setText("username: " + username);
            loginBtn.setVisibility(View.GONE);
            account.setVisibility(View.VISIBLE);
            grid.setVisibility(View.VISIBLE);
        });
    }

    private void showSignedOut() {
        runOnUiThread(() -> {
            loginBtn.setVisibility(View.VISIBLE);
            account.setVisibility(View.GONE);
            grid.setVisibility(View.GONE);
        });
    }

    /** Hộp thoại nhập productId + amount rồi gọi payment. */
    private void askPayment() {
        int p = (int) (16 * getResources().getDisplayMetrics().density);
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(p, p / 2, p, 0);
        final EditText productId = edit(form, "productId (Play Console)", lastProductId, false);
        final EditText amount = edit(form, "amount", lastAmount, true);
        new AlertDialog.Builder(this)
                .setTitle("Payment")
                .setView(form)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Pay", (d, w) -> {
                    lastProductId = productId.getText().toString().trim();
                    lastAmount = amount.getText().toString().trim();
                    pay(lastProductId, lastAmount);
                })
                .show();
    }

    private void pay(String productId, String amount) {
        ProductObject o = new ProductObject();
        o.productId = productId;
        o.productName = "package 1";
        o.amount = amount;
        o.orderId = UUID.randomUUID().toString().substring(0, 10);
        o.serverId = "s1";
        o.roleId = "role-demo";
        o.extraInfo = "demo";
        o.logData = "";
        NFsdk.getInstance().payment(this, o, new PaymentListener() {
            @Override
            public void onSuccess(String message) {
                print("payment onSuccess " + message);
            }

            @Override
            public void onFail(String message) {
                print("payment onFail " + message);
            }
        });
    }

    private void trackAll() {
        try {
            NFsdk.tracking().createRole("role-demo");
            NFsdk.tracking().trackingStartTrial();
            NFsdk.tracking().tutorialCompleted();
            NFsdk.tracking().purchaseCheckOut();
            for (PaidType t : PaidType.values()) NFsdk.tracking().paid(t);
            NFsdk.tracking().levelUp(10);
            NFsdk.tracking().vipLevelUp(2);
            NFsdk.tracking().retention(1);
            NFsdk.tracking().joinGuild();
            NFsdk.tracking().stageComplete();
            NFsdk.tracking().stageComplete(3);
            NFsdk.tracking().level(5);
            NFsdk.tracking().vip(1);
            NFsdk.tracking().purchase("pack1");
            NFsdk.tracking().trackingEvent("demo_event");
            NFsdk.tracking().trackingEvent("demo_event_json", "{\"server\":\"s1\"}");
            NFsdk.tracking().trackingEvent("demo_event_obj", new JSONObject().put("character", "abc").put("level", 3));
            NFsdk.tracking().getFcmToken(token -> print("FCM token: " + token.length() + " chars"));
            NFsdk.tracking().crashlytics().setUserId("demo");
            print("tracking: all events sent");
        } catch (Exception e) {
            print("tracking error " + e);
        }
    }

    private EditText edit(LinearLayout col, String hint, String value, boolean number) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setText(value);
        e.setSingleLine(true);
        if (number) e.setInputType(InputType.TYPE_CLASS_NUMBER);
        col.addView(e);
        return e;
    }

    /** Nút trong lưới 2 cột, chia đều chiều rộng. */
    private void btn(GridLayout g, String text, View.OnClickListener l) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(text);
        b.setTextSize(13);
        b.setSingleLine(true);
        b.setOnClickListener(l);
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED), GridLayout.spec(GridLayout.UNDEFINED, 1f));
        lp.width = 0;
        lp.height = GridLayout.LayoutParams.WRAP_CONTENT;
        g.addView(b, lp);
    }

    private void print(final String s) {
        runOnUiThread(() -> {
            String time = new SimpleDateFormat("HH:mm:ss", Locale.ROOT).format(new Date());
            log.setText(time + "  " + s + "\n" + log.getText()); // mới nhất ở trên; không tự cuộn trang
        });
    }
}
