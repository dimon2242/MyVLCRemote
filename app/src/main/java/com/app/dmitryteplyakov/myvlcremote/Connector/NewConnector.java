package com.app.dmitryteplyakov.myvlcremote.Connector;

import android.util.Log;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;

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

public class NewConnector {

    private static final String TAG = "MVLCR:NEWConnector";
    volatile private static Socket mSocket;
    volatile private BufferedReader mBufferedReader;
    volatile private BufferedWriter mBufferedWriter;
    volatile private boolean globalStop;
    private boolean mSignal;
    private String mServerAddress;
    private String mPort;
    private String mPassword;

    synchronized public boolean isConnected() {
        if(mSocket == null) {
            Log.i(TAG, "No connect!");
            return false;
        }
        try {
            mBufferedWriter.write("hello?\n");
            mBufferedWriter.flush();
            if(mBufferedReader.readLine() != null) {
                Log.d(TAG, "Response received.");
                return true;
            }
        } catch(SocketException e) {
            Log.i(TAG, "SocketException. Connection reset?");
            return false;
        } catch(IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public NewConnector(final String serverAddress, final String port, final String password) {
        mServerAddress = serverAddress;
        mPort = port;
        mPassword = password;
        Log.d(TAG, "Init connector");
    }

    synchronized public boolean connect() {
        try {
            InetAddress ipAddr = InetAddress.getByName(mServerAddress);
            mSocket = new Socket(ipAddr, Integer.parseInt(mPort));
            mSocket.setSoTimeout(5000);
            InputStream istream = mSocket.getInputStream();
            OutputStream ostream = mSocket.getOutputStream();

            mBufferedReader = new BufferedReader(new InputStreamReader(istream));
            mBufferedWriter = new BufferedWriter(new OutputStreamWriter(ostream));

            mBufferedWriter.write(mPassword + "\n");
            mBufferedWriter.flush();
            /**
             * Skip response and welcome message
             */
            mBufferedReader.readLine();
            mBufferedReader.readLine();
            if(mBufferedReader.readLine().equals("Wrong password")) {
                Log.d(TAG, "Wrong password.");
                return false;
            }

        } catch(ConnectException e) {
            Log.i(TAG, "Cannot connected. Connection refused by server");
            return false;
        } catch(SocketException e) {
            if(e.getMessage().equals("Network is unreachable")) {
                Log.i(TAG, "Network is unreachable");
                return false;
            }
        } catch(IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    synchronized public void send(final String command, final int value) {
        if (mSocket == null) {
            Log.i(TAG, "No connect!");
            return;
        }
        String param;
        param = Integer.toString(value);
        try {
            mBufferedWriter.write(command + " " + value + "\n");
            mBufferedWriter.flush();
            Log.i(TAG, "Sending command: " + command + " " + Integer.toString(value));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    synchronized public void send(final String command) {
        if (mSocket == null) {
            Log.i(TAG, "No connect!");
            return;
        }
        try {
            Log.d(TAG, "STATE: " + Boolean.toString(mBufferedReader.ready()));
            mBufferedWriter.write(command + "\n");
            mBufferedWriter.flush();
            Log.i(TAG, "Sending command: " + command);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    synchronized public int getState(String command) {
        Log.d(TAG, "GetState");
        if (mSocket == null) {
            Log.i(TAG, "No connect!");
            return 0;
        }
        Log.d(TAG, "Success!");
        String sBuf = null;
        int progress = 0;

        try {
            mBufferedWriter.write(command + "\n");
            mBufferedWriter.flush();
            sBuf = mBufferedReader.readLine();
            if(sBuf != null) {
                sBuf = sBuf.replaceAll(">", "").replaceAll(" ", "");
                if (sBuf.equals(""))
                    return 0;
                progress = Integer.parseInt(sBuf);

                //Log.d(TAG, "RESULT: " + Integer.toString(progress));
            } else
                return 0;
        } catch(IOException | NumberFormatException e) {
            if(e.getMessage().equals("Broken pipe")) {
                Log.d(TAG, "Connection refused by host");
            } else
                e.printStackTrace();
            return 0;
        }
        return progress;
    }

    synchronized public String getTitle() {
        if(mSocket == null) {
            Log.i(TAG, "No connect!");
            return null;
        }
        String result = null;
        try {
            mBufferedWriter.write("status\n");
            mBufferedWriter.flush();
            result = mBufferedReader.readLine();
            /**
             * Skip unused messages
             */
            mBufferedReader.readLine();
            mBufferedReader.readLine();
            int start;
            if(result != null) {
                start = result.lastIndexOf('/');
                if (start == -1)
                    start = result.lastIndexOf('\\');
                if (!(start == -1)) {
                    result = result.substring(start + 1);
                    result = result.substring(0, result.length() - 2);
                }
            } else
                return null;
        } catch(IOException e) {
            e.printStackTrace();
            return null;
        }
        return result;
    }

    synchronized public void close() {
        try {
            if (mSocket != null) {
                if (!mSocket.isInputShutdown())
                    mSocket.shutdownInput();
                if(!mSocket.isOutputShutdown())
                    mSocket.shutdownOutput();
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
}
