package com.app.dmitryteplyakov.myvlcremote;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.app.dmitryteplyakov.myvlcremote.Connector.NewConnector;

import java.lang.ref.WeakReference;

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

public class RemoteFragment extends Fragment {
    private static Button mNextVideoButton;
    private static Button mPrevVideoButton;
    private static Button mPPVideoButton;
    private static SeekBar mSeekBarTimeline;
    private NewConnector mConnectorTimeline;
    private static TextView sMediaTimelineLength;
    private static TextView sMediaTimeDuration;
    private static final String TITLE_MEDIA_MSG = "remotefragment.title_media_msg";
    private ProgressHandler mProgressHandler;
    private static boolean globalStop;
    private static int sDelay;
    private static TextView mVolumeTextView;
    private static Button mVolumeIncButton;
    private static Button mVolumeDecButton;
    private static int sVolume;
    private static int sLocalVolume;
    private TimelineMonitor mTimelineThread;
    private NewConnector mConnectorVolume;
    private ConnectionStatusHandler mConnectionStatusHandler;
    static private TextView mConnectionStateTextView;
    private ConnectionStatusThread mConnectionStatusThread;
    private static Button sConnectionButton;
    private static boolean sConnState;
    private NewConnector mNewCommandConnector;
    private NewConnector mPingPongConnector;
    private NewConnector mCommonConnector;
    private int mRefreshFreq;
    private SharedPreferences mSp;
    private ConnectionButtonStateHandler mConnectionButtonStateHandler;
    private boolean mBatterySaverMode;
    private boolean mBackgroundActivityStop;
    private static TextView sMediaTitleTextView;

    public static Fragment newInstance() {
        return new RemoteFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mBatterySaverMode = mSp.getBoolean("battery_saver_mode", false);
        mBackgroundActivityStop = mSp.getBoolean("battery_economy_background", true);
        globalStop = false;
        mProgressHandler = new ProgressHandler(getActivity());
        if(!mBatterySaverMode)
            mConnectionStatusHandler = new ConnectionStatusHandler(getActivity());
        mConnectionButtonStateHandler = new ConnectionButtonStateHandler(getActivity());
        sDelay = 200;
    }

    private boolean connect() {
        boolean pingPongStateConnection;
        boolean timelineStateConnection;
        boolean commonStateConnection;
        boolean volumeStateConnection;
        boolean newCommandStateConnection;
        boolean resultStateConnetion;
        resultStateConnetion = true;

        globalStop = false;
        String host = mSp.getString("host", "");
        String port = mSp.getString("port", "");
        String password = mSp.getString("password", "");
        mRefreshFreq = Integer.parseInt(mSp.getString("refresh_freq", "1"));
        if(mRefreshFreq < 1)
            mRefreshFreq = 1;
        /**
         * Battery Saver Mode disable all network activity excepted general command connector.
         */
        if(!mBatterySaverMode) {
            mConnectorTimeline = new NewConnector(host, port, password);
            mPingPongConnector = new NewConnector(host, port, password);
            mCommonConnector = new NewConnector(host, port, password);
            timelineStateConnection = mConnectorTimeline.connect();
            pingPongStateConnection = mPingPongConnector.connect();
            commonStateConnection = mCommonConnector.connect();
            resultStateConnetion = timelineStateConnection && pingPongStateConnection && commonStateConnection;
        }
        mConnectorVolume = new NewConnector(host, port, password);
        mNewCommandConnector = new NewConnector(host, port, password);
        volumeStateConnection = mConnectorVolume.connect();
        newCommandStateConnection = mNewCommandConnector.connect();

        resultStateConnetion &= volumeStateConnection && newCommandStateConnection;

        return resultStateConnetion;

    }

    private void estabConnection() {
        if(connect()) {
            if (!mBatterySaverMode) {
                mTimelineThread = new TimelineMonitor();
                mConnectionStatusThread = new ConnectionStatusThread();
                mTimelineThread.start();
                mConnectionStatusThread.start();
                mConnectionStatusHandler.sendEmptyMessage(2);
            }
            sConnState = true;
        } else {
            mConnectionStatusHandler.obtainMessage(4).sendToTarget();
            globalStop = true;
        }
    }

    private void closeConnection() {
        stopSrv();
        if(!mBatterySaverMode)
            mConnectionStatusHandler.sendEmptyMessage(3);
    }

    private static void resetDurations() {
        sMediaTimeDuration.setText("00:00");
        sMediaTimelineLength.setText("00:00");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.remote_fragment, container, false);

        mNextVideoButton = (Button) view.findViewById(R.id.remote_next_video);
        mPrevVideoButton = (Button) view.findViewById(R.id.remote_prev_video);
        mPPVideoButton = (Button) view.findViewById(R.id.remote_play_and_pause_video);
        mSeekBarTimeline = (SeekBar) view.findViewById(R.id.remote_seekbar_timeline);
        sMediaTimelineLength = (TextView) view.findViewById(R.id.remote_timeline_length);
        sMediaTimeDuration = (TextView) view.findViewById(R.id.remote_timeline_length_duration);
        mVolumeTextView = (TextView) view.findViewById(R.id.remote_volume);
        mVolumeIncButton = (Button) view.findViewById(R.id.remote_volinc);
        mVolumeDecButton = (Button) view.findViewById(R.id.remote_voldec);
        mConnectionStateTextView = (TextView) view.findViewById(R.id.remote_conn_state);
        mConnectionStateTextView.setText(getString(R.string.disconnected));
        mConnectionStateTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        sConnectionButton = (Button) view.findViewById(R.id.remote_connect);
        sMediaTitleTextView = (TextView) view.findViewById(R.id.remote_media_title);

        if(mBatterySaverMode) {
            sMediaTitleTextView.setVisibility(View.GONE);
            sMediaTimeDuration.setVisibility(View.GONE);
            sMediaTimelineLength.setVisibility(View.GONE);
            mSeekBarTimeline.setVisibility(View.GONE);
            sConnectionButton.setText(getString(R.string.connect_and_disconnect_bsm));
            mConnectionStateTextView.setText(getString(R.string.bsm_enabled));
            mVolumeTextView.setText(getString(R.string.volume));
        }
        resetDurations();


        sConnectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(sConnState) {
                    if(mNewCommandConnector != null) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                closeConnection();

                            }
                        }).start();
                    }
                } else {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            estabConnection();
                        }
                    }).start();
                }
            }
        });

        mVolumeIncButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!mBatterySaverMode)
                    sLocalVolume = sVolume;
                sLocalVolume += 256 / 10;
                updateVolume();
            }
        });

        mVolumeDecButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!mBatterySaverMode)
                    sLocalVolume = sVolume;
                sLocalVolume -= 256 / 10;
                updateVolume();
            }
        });

        mNextVideoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mNewCommandConnector != null) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            mNewCommandConnector.send("next");
                        }
                    }).start();
                }

            }
        });

        mPPVideoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mNewCommandConnector != null) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            mNewCommandConnector.send("pause");
                        }
                    }).start();
                }
            }
        });

        mPrevVideoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mNewCommandConnector != null) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            mNewCommandConnector.send("prev");
                        }
                    }).start();
                }
            }
        });

        mSeekBarTimeline.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            private int progress;
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                progress = i;

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(final SeekBar seekBar) {
                if(!mBatterySaverMode) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            mNewCommandConnector.send("seek", progress);
                        }
                    }).start();
                }
            }
        });

        return view;
    }

    private void updateVolume() {
        if (sLocalVolume > 256)
            sLocalVolume = 256;
        else if (sLocalVolume < 0)
            sLocalVolume = 0;
        if(mConnectorVolume != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    mConnectorVolume.send("volume", sLocalVolume);
                    if(mBatterySaverMode)
                        mProgressHandler.sendMessage(Message.obtain(mProgressHandler, -2, sLocalVolume, 0));
                }
            }).start();
        }
    }

    private static class ProgressHandler extends Handler {
        private WeakReference<Activity> wrActivity;

        public ProgressHandler(Activity activity) {
            wrActivity = new WeakReference<Activity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            Activity activity = wrActivity.get();
            super.handleMessage(msg);
            if(activity != null) {
                if (msg.what == -2) {
                    sVolume = msg.arg1;
                    mVolumeTextView.setText(activity.getResources().getString(R.string.volume) + " " + Integer.toString((msg.arg1 * 100) / 256) + "%");
                } else if (msg.what == -1) {
                    mSeekBarTimeline.setMax(msg.arg1);
                    String lengthString = "";
                    String lengthMin = "";
                    String lengthSec = "";
                    int minutes = msg.arg1 / 60;
                    int seconds = msg.arg1 - minutes * 60;
                    if (seconds / 10 == 0)
                        lengthSec += "0" + Integer.toString(seconds);
                    else
                        lengthSec += Integer.toString(seconds);
                    if (minutes / 10 == 0)
                        lengthMin += "0" + Integer.toString(minutes);
                    else
                        lengthMin += Integer.toString(minutes);
                    if (minutes / 60 != 0)
                        lengthString = Integer.toString(minutes / 60);
                    lengthString += lengthMin + ":" + lengthSec;
                    sMediaTimelineLength.setText(lengthString);
                }
                if (msg.what == -3) {
                    String title = msg.getData().getString(TITLE_MEDIA_MSG);
                    if (title == null)
                        title = activity.getResources().getString(R.string.no_title);
                    sMediaTitleTextView.setText(title);
                    //Log.d("MVLCR title", title);
                } else if (msg.what == 0) {
                    String lengthString = "";
                    String lengthMin = "";
                    String lengthSec = "";
                    int minutes = msg.arg1 / 60;
                    int seconds = msg.arg1 - minutes * 60;
                    if (seconds / 10 == 0)
                        lengthSec += "0" + Integer.toString(seconds);
                    else
                        lengthSec += Integer.toString(seconds);
                    if (minutes / 10 == 0)
                        lengthMin += "0" + Integer.toString(minutes);
                    else
                        lengthMin += Integer.toString(minutes);
                    if (minutes / 60 != 0)
                        lengthString = Integer.toString(minutes / 60);
                    lengthString += lengthMin + ":" + lengthSec;

                    sMediaTimeDuration.setText(lengthString);
                    mSeekBarTimeline.setProgress(msg.arg1);
                } else if (msg.what == 1) {
                    resetDurations();
                }
            }
        }
    }

    private static class ConnectionStatusHandler extends Handler {
        private WeakReference<Activity> wrActivity;

        public ConnectionStatusHandler(Activity activity) {
            wrActivity = new WeakReference<Activity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            Activity activity = wrActivity.get();
            super.handleMessage(msg);
            if(activity != null) {
                if (msg.what == 1) {
                    mConnectionStateTextView.setText(activity.getResources().getString(R.string.connected));
                    mConnectionStateTextView.setTextColor(activity.getResources().getColor(android.R.color.holo_green_dark));
                    changeButtonState(true);
                } else if (msg.what == 0) {
                    mConnectionStateTextView.setText(activity.getResources().getString(R.string.disconnected));
                    mConnectionStateTextView.setTextColor(activity.getResources().getColor(android.R.color.holo_red_dark));
                    changeButtonState(false);
                } else if (msg.what == 2) {
                    sConnectionButton.setText(activity.getResources().getString(R.string.disconnect));
                    changeButtonState(true);
                    //Log.d("CONN", "HERE! DIS");
                } else if (msg.what == 3) {
                    sConnectionButton.setText(activity.getResources().getString(R.string.connect));
                    changeButtonState(false);
                    //Log.d("CONN", "HERE! CON");
                } else if (msg.what == 4) {
                    Log.d("HM?", "HERE!");
                    Snackbar.make(activity.findViewById(R.id.fragment_container), activity.getResources().getString(R.string.err_connection_snackbar), Snackbar.LENGTH_SHORT).show();
                }
            }
        }
    }

    private class ConnectionStatusThread implements Runnable {
        Thread thread;
        private int count;

        public ConnectionStatusThread() {
            thread = new Thread(this);
        }

        public void start() {
            thread.start();
        }

        public void run() {
            /**
             * Count try for connecting to server
             * Disabled for now
             */
            count = 5;
            while(!globalStop) {
                /*if(count <= 0)
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            stopSrv();
                        }
                    }).start();
                    */
                if(mPingPongConnector.isConnected()) {
                    count = 5;
                    mConnectionStatusHandler.sendEmptyMessage(1);
                } else {
                    /*count--;
                    mProgressHandler.obtainMessage(1).sendToTarget();
                    mConnectionStatusHandler.sendEmptyMessage(0);*/
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            stopSrv();
                        }
                    }).start();
                }
                try {
                    Thread.sleep(mRefreshFreq * 1000);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class TimelineMonitor implements Runnable {
        private int currentTime;
        Thread thread;

        public TimelineMonitor() {
            thread = new Thread(this);
        }

        public void start() {
            thread.start();
        }

        public void run() {
            if (!globalStop) {
                mProgressHandler.sendMessage(Message.obtain(mProgressHandler, -1, mConnectorTimeline.getState("get_length"), 0));
                /**
                 * First time checking media title
                 */
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Bundle data = new Bundle();
                        data.putString(TITLE_MEDIA_MSG, mCommonConnector.getTitle());
                        Message msg = new Message();
                        msg.setData(data);
                        msg.what = -3;
                        mProgressHandler.sendMessage(msg);
                    }
                }).start();
            }

            while (!globalStop) {
                try {
                    currentTime = mConnectorTimeline.getState("get_time");
                    //Log.d("Curr", Integer.toString(currentTime));
                    mProgressHandler.sendMessage(Message.obtain(mProgressHandler, 0, currentTime, 0));
                    mProgressHandler.sendMessage(Message.obtain(mProgressHandler, -2, mConnectorVolume.getState("volume"), 0));
                    //Log.d("VOL", Integer.toString(mConnectorVolume.getState("volume")));
                    if (currentTime == 0) {
                        mProgressHandler.sendMessage(Message.obtain(mProgressHandler, -1, mConnectorTimeline.getState("get_length"), 0));
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                Bundle data = new Bundle();
                                data.putString(TITLE_MEDIA_MSG, mCommonConnector.getTitle());
                                Message msg = new Message();
                                msg.setData(data);
                                msg.what = -3;
                                mProgressHandler.sendMessage(msg);
                            }
                        }).start();
                    }

                    Thread.sleep(sDelay);
                } catch (InterruptedException e) {

                }

            }
        }
    }

    private static class ConnectionButtonStateHandler extends Handler {
        WeakReference<Activity> wrActivity;

        public ConnectionButtonStateHandler(Activity activity) {
            wrActivity = new WeakReference<Activity>(activity);
        }

        public void handleMessage(Message msg) {
            Activity activity = wrActivity.get();
            if(activity != null) {
                if(msg.what == 0)
                    sConnectionButton.setEnabled(false);
                else if(msg.what == 1)
                    sConnectionButton.setEnabled(true);
            }
        }
    }

    private void connectButtonStateChanger() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String host = sp.getString("host", "");
        String port = sp.getString("port", "");
        String password = sp.getString("password", "");
        if((host.equals("") || port.equals("") || password.equals("")))
            mConnectionButtonStateHandler.sendEmptyMessage(0);
        else
            mConnectionButtonStateHandler.sendEmptyMessage(1);

    }

    private static void changeButtonState(boolean state) {
        mSeekBarTimeline.setEnabled(state);
        mVolumeDecButton.setEnabled(state);
        mVolumeIncButton.setEnabled(state);
        mNextVideoButton.setEnabled(state);
        mPrevVideoButton.setEnabled(state);
        mPPVideoButton.setEnabled(state);
    }

    @Override
    public void onStop() {
        super.onStop();
        if(mBackgroundActivityStop)
            stopSrv();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(mBackgroundActivityStop)
            if(mNewCommandConnector != null)
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        estabConnection();
                    }
                }).start();
        if(!mBatterySaverMode) {
            changeButtonState(false);
            connectButtonStateChanger();
        }
    }


    private void stopSrv() {
        try {
            if (sConnState) {
                connectButtonStateChanger();

                Log.d("MyVLCRemote", "Stopping.");
                globalStop = true;
                mNewCommandConnector.close();
                if(!mBatterySaverMode) {
                    mConnectionButtonStateHandler.sendEmptyMessage(0);
                    mConnectorTimeline.close();
                    mConnectorVolume.close();
                    mPingPongConnector.close();
                    mTimelineThread.thread.join();
                    mConnectionStatusThread.thread.join();
                }
                sConnState = false;
                Log.d("MyVLCRemote", "Stopped!.");
            } else
                Log.i("stopSrv", "No need stopping!");
        } catch(NullPointerException e) {
            e.printStackTrace();
        } catch(InterruptedException e) {
            e.printStackTrace();
        }
        if(!mBatterySaverMode) {
            mConnectionStatusHandler.sendEmptyMessage(3);
            mConnectionStatusHandler.sendEmptyMessage(0);
            mConnectionButtonStateHandler.sendEmptyMessage(1);
            Bundle data = new Bundle();
            data.putString(TITLE_MEDIA_MSG, "");
            Message msg = new Message();
            msg.setData(data);
            msg.what = -3;
            mProgressHandler.sendMessage(msg);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopSrv();
    }

}
