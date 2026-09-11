package com.example.qqprivatenotify;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.widget.*;

public final class SettingsActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state); setTitle("QQ 通知设置");
        int pad = dp(12);
        int topSpacing = dp(28);
        LinearLayout content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(pad,topSpacing,pad,pad);
        TextView tip = new TextView(this); tip.setText("分别控制私聊和群聊消息通知。开关打开时允许，关闭时屏蔽。\n"); tip.setTextSize(14); tip.setTextColor(Color.DKGRAY); content.addView(tip);
        android.content.SharedPreferences prefs = getSharedPreferences("settings", 0);
        Switch privateSwitch = new Switch(this); privateSwitch.setText("打开私聊消息通知"); privateSwitch.setTextSize(16); privateSwitch.setMinHeight(dp(48));
        Switch groupSwitch = new Switch(this); groupSwitch.setText("打开群聊消息通知"); groupSwitch.setTextSize(16); groupSwitch.setMinHeight(dp(48));
        privateSwitch.setChecked(prefs.getBoolean(WhitelistConfig.PRIVATE_ENABLED, true)); groupSwitch.setChecked(prefs.getBoolean(WhitelistConfig.GROUP_ENABLED, false));
        LinearLayout.LayoutParams privateParams = new LinearLayout.LayoutParams(-1,-2); privateParams.topMargin = dp(12);
        content.addView(privateSwitch, privateParams); content.addView(groupSwitch, new LinearLayout.LayoutParams(-1,-2));
        content.addView(new Space(this), new LinearLayout.LayoutParams(1,0,1));
        Button save = new Button(this); save.setText("保存"); content.addView(save);
        privateSwitch.setOnCheckedChangeListener((button, checked) -> prefs.edit().putBoolean(WhitelistConfig.PRIVATE_ENABLED, checked).apply());
        groupSwitch.setOnCheckedChangeListener((button, checked) -> prefs.edit().putBoolean(WhitelistConfig.GROUP_ENABLED, checked).apply());
        save.setOnClickListener(v->{prefs.edit().putBoolean(WhitelistConfig.PRIVATE_ENABLED,privateSwitch.isChecked()).putBoolean(WhitelistConfig.GROUP_ENABLED,groupSwitch.isChecked()).apply();save.setText("已保存");});
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            int left, top, right, bottom;
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets safe = insets.getInsets(
                        android.view.WindowInsets.Type.systemBars()
                        | android.view.WindowInsets.Type.displayCutout());
                left = safe.left; top = safe.top; right = safe.right; bottom = safe.bottom;
            } else {
                left = insets.getSystemWindowInsetLeft(); top = insets.getSystemWindowInsetTop();
                right = insets.getSystemWindowInsetRight(); bottom = insets.getSystemWindowInsetBottom();
            }
            view.setPadding(left, top, right, bottom);
            return insets;
        });
        scroll.addView(content); setContentView(scroll); scroll.requestApplyInsets();
    }
    private int dp(int value){return (int)(value*getResources().getDisplayMetrics().density+.5f);}
}
