/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.timezonepicker;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;

import java.util.Locale;
import java.util.TimeZone;

public class TimeZonePickerUtils {
    private static final String TAG = "TimeZonePickerUtils";

    private Locale mDefaultLocale;
    private String[] mOverrideIds;
    private String[] mOverrideLabels;

    /**
     * This needs to be an instantiated class so that it doesn't need to continuously re-load the
     * list of timezone IDs that need to be overridden.
     * @param context
     */
    public TimeZonePickerUtils(Context context) {
        // Instead of saving a reference to the context (because we might need to look up the
        // labels every time getGmtDisplayName is called), we'll cache the lists of override IDs
        // and labels now.
        cacheOverrides(context);
    }

    /**
     * Given a timezone id (e.g. America/Los_Angeles), returns the corresponding timezone
     * display name (e.g. (GMT-7.00) Pacific Time).
     *
     * @param context Context in case the override labels need to be re-cached.
     * @param id The timezone id
     * @param millis The time (daylight savings or not)
     * @return The display name of the timezone.
     */
    public String getGmtDisplayName(Context context, String id, long millis) {
        TimeZone timezone = TimeZone.getTimeZone(id);
        if (timezone == null) {
            return null;
        }

        final Locale defaultLocale = Locale.getDefault();
        if (!defaultLocale.equals(mDefaultLocale)) {
            // If the IDs and labels haven't been set yet, or if the locale has been changed
            // recently, we'll need to re-cache them.
            mDefaultLocale = defaultLocale;
            cacheOverrides(context);
        }
        return buildGmtDisplayName(timezone, millis);
    }

    private String buildGmtDisplayName(TimeZone tz, long timeMillis) {
        Time time = new Time(tz.getID());
        time.set(timeMillis);

        StringBuilder sb = new StringBuilder();
        final int gmtOffset = tz.getOffset(timeMillis);
        appendGmtOffset(sb, gmtOffset);

        String displayName = getDisplayName(tz, time.isDst != 0);
        sb.append(" ");
        sb.append(displayName);

        if (tz.useDaylightTime()) {
            sb.append(" ");
            sb.append(getDstSymbol()); // Sun symbol
        }
        return sb.toString();
    }

    public static void appendGmtOffset(StringBuilder sb, final int gmtOffset) {
        sb.append("(GMT");

        if (gmtOffset < 0) {
            sb.append('-');
        } else {
            sb.append('+');
        }

        final int p = Math.abs(gmtOffset);
        sb.append(p / DateUtils.HOUR_IN_MILLIS); // Hour

        final int min = (p / (int) DateUtils.MINUTE_IN_MILLIS) % 60;
        if (min != 0) { // Show minutes if non-zero
            sb.append(':');
            if (min < 10) {
                sb.append('0');
            }
            sb.append(min);
        }
        sb.append(')');
    }

    public static char getDstSymbol() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            return '\u2600'; // The Sun emoji icon.
        } else {
            return '*';
        }
    }

    /**
     * Gets the display name for the specified Timezone ID. If the ID matches the list of IDs that
     * need to be have their default display names overriden, use the pre-set display name from
     * R.arrays.
     * @param id The timezone ID.
     * @param daylightTime True for daylight time, false for standard time
     * @return The display name of the timezone. This will just use the default display name,
     * except that certain timezones have poor defaults, and should use the pre-set override labels
     * from R.arrays.
     */
    private String getDisplayName(TimeZone tz, boolean daylightTime) {
        if (mOverrideIds == null || mOverrideLabels == null) {
            // Just in case they somehow didn't get loaded correctly.
            return tz.getDisplayName(daylightTime, TimeZone.LONG, Locale.getDefault());
        }

        for (int i = 0; i < mOverrideIds.length; i++) {
            if (tz.getID().equals(mOverrideIds[i])) {
                if (mOverrideLabels.length > i) {
                    return mOverrideLabels[i];
                }
                Log.e(TAG, "timezone_rename_ids len=" + mOverrideIds.length +
                        " timezone_rename_labels len=" + mOverrideLabels.length);
                break;
            }
        }

        // If the ID doesn't need to have the display name overridden, or if the labels were
        // malformed, just use the default.
        return tz.getDisplayName(daylightTime, TimeZone.LONG, Locale.getDefault());
    }

    private void cacheOverrides(Context context) {
        Resources res = context.getResources();
        mOverrideIds = res.getStringArray(R.array.timezone_rename_ids);
        mOverrideLabels = res.getStringArray(R.array.timezone_rename_labels);
    }
}
