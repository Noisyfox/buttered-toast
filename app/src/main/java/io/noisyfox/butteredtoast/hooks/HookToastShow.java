/*
 * This file is part of ButteredToast.
 *
 * Copyright 2013-2014 Gabriel Castro (c)
 *
 *     ButteredToast is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     ButteredToast is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with ButteredToast.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.noisyfox.butteredtoast.hooks;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.text.SpannableStringBuilder;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import io.noisyfox.butteredtoast.Util;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class HookToastShow extends XC_MethodHook implements AutoHookable {

    public HookToastShow() {
    }

    @Override
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
        Toast t = (Toast) param.thisObject;
        try {

            View view = t.getView();
            Context context = view.getContext();
            PackageManager pm = context.getPackageManager();
            ApplicationInfo info = context.getApplicationInfo();
            String appName = pm.getApplicationLabel(info).toString();
            List<TextView> list = new ArrayList<>();
            if (view instanceof TextView) {
                list.add((TextView) view);
                XposedBridge.log("[ButteredToast]TextView is toast view");
                ((TextView) view).setText(new SpannableStringBuilder(appName)
                        .append('\n')
                        .append(((TextView) view).getText())
                );
                return;
            } else if (view instanceof ViewGroup) {
                Util.finaAllTextView(list, (ViewGroup) view);
            }
            if (list.size() != 1) {
                throw new RuntimeException("number of TextViews in toast is not 1");
            }
            TextView text = list.get(0);
            ViewGroup parent = (ViewGroup) text.getParent();

            TextView appText = new TextView(text.getContext());
            appText.setTextColor(text.getTextColors());
            appText.setTextSize(TypedValue.COMPLEX_UNIT_PX, text.getTextSize());
            appText.setTypeface(text.getTypeface());

            boolean vertical = true;
            if (parent instanceof LinearLayout) {
                vertical = ((LinearLayout) parent).getOrientation() == LinearLayout.VERTICAL;
            }
            if (vertical) {
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                appText.setLayoutParams(params);
                appText.setGravity(Gravity.CENTER_HORIZONTAL);
                appText.setPadding(appText.getPaddingLeft(), appText.getPaddingTop(), appText.getPaddingRight(), appText.getPaddingBottom() * 2);
            } else {
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
                appText.setLayoutParams(params);
                appText.setGravity(Gravity.CENTER_VERTICAL);
                appText.setPadding(appText.getPaddingLeft(), appText.getPaddingTop(), appText.getPaddingRight() * 2, appText.getPaddingBottom());
            }

            // add NBSP's to force it to lock centered even if it's longer than the text
            appName = '\u00A0' + appName + '\u00A0';
            appText.setText(appName);
            parent.addView(appText, 0);
        } catch (RuntimeException e) {
            XposedBridge.log(e);
        }
    }

    @Override
    public Unhook hook() {
        XposedBridge.log("[ButteredToast]Toast.show() hooked!");
        return XposedHelpers.findAndHookMethod(Toast.class, "show", this);
    }
}