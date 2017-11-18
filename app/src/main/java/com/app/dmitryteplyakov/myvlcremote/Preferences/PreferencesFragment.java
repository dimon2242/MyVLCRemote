package com.app.dmitryteplyakov.myvlcremote.Preferences;

import android.os.Bundle;
import android.support.v7.preference.PreferenceFragmentCompat;

import com.app.dmitryteplyakov.myvlcremote.R;

/**
 * Copyright 2017 Dmitry Teplyakov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class PreferencesFragment extends PreferenceFragmentCompat {
    private static final String ARG_PREF = "com.app.preferenceFragment.arg_pref";

    public static PreferencesFragment newInstance(String sett) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_PREF, sett);
        PreferencesFragment fragment = new PreferencesFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
            addPreferencesFromResource(R.xml.preference);
    }
}