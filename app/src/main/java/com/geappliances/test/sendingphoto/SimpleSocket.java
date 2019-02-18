package com.geappliances.test.sendingphoto;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.geappliances.test.sendingphoto.common.Constants;
import com.geappliances.test.sendingphoto.common.Pref;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * @file SimpleSocket.java
 * @date 13/02/2019
 * @brief
 * @copyright GE Appliances, a Haier Company (Confidential). All rights reserved.
 */
public class SimpleSocket extends Thread {

    private Socket socket;

    private BufferedReader buffRecv;
    private BufferedWriter buffSend;
    private DataOutputStream dos;
    private FileInputStream fis;
    private PrintWriter out;


    private String addr;
    private int port;
    private Handler handler = null;
    private String imgUrl;


    public enum MessageType {

        SIMSOCK_CONNECTED(1), SIMSOCK_DATA(2), SIMSOCK_DISCONNECTED(3), SIMSOCK_REQIMAGE(4), SIMSOCK_ERROR(5);

        private final int value;

        private MessageType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public SimpleSocket(String addr, int port, Handler handler) {
        this.addr = addr;
        this.port = port;
        this.handler = handler;
    }

    private void makeMessage(MessageType what, Object obj) {
        Message msg = Message.obtain();
        msg.what = what.getValue();
        msg.obj = obj;
        handler.sendMessage(msg);
    }

    private boolean connect(String addr, int port) {
        Log.d("socket","Connecting...");

        try {
            InetSocketAddress socketAddress = new InetSocketAddress(InetAddress.getByName(addr), port);
            socket = new Socket();
            socket.connect(socketAddress, 5000);
            buffRecv = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            buffSend = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            out = new PrintWriter(buffSend, true);
            makeMessage(MessageType.SIMSOCK_CONNECTED, "connected");
            Pref.put(Constants.HOST,addr);
            Pref.put(Constants.PORT,port);


        } catch (IOException e) {
            System.out.println(e);
            e.printStackTrace();
            makeMessage(MessageType.SIMSOCK_ERROR, "connection error");
            return false;
        }
        return true;
    }

    @Override
    public void run() {
        if (!connect(addr, port)) {
            makeMessage(MessageType.SIMSOCK_ERROR, "connection error");
            return; // connect failed
        }
        if (socket == null){
            makeMessage(MessageType.SIMSOCK_ERROR, "connection error");
            return;
        }

        Log.d("SimpleSocket", "socket_thread loop started");

        String aLine = null;
        while (!Thread.interrupted()) {
            try {
                if(socket.isInputShutdown() || socket.isOutputShutdown()){
                    disconnected();
                    makeMessage(MessageType.SIMSOCK_DISCONNECTED, "disconnected");
                    return;

                }
                aLine = buffRecv.readLine();
                if (aLine == null) {
                    disconnected();
                    makeMessage(MessageType.SIMSOCK_DISCONNECTED, "disconnected");
                    return;
                } else {
                    if (aLine.contains("image")) {
                        makeMessage(MessageType.SIMSOCK_REQIMAGE, aLine);
                    } else if (aLine.contains("Brand")) {
                        makeMessage(MessageType.SIMSOCK_DATA, aLine);
                    } else {
                        Log.d("SimpleSocket", "socket_thread loop started");
                        makeMessage(MessageType.SIMSOCK_DATA, aLine);
                    }
                    aLine = null;

                }
            } catch (IOException e) {
                makeMessage(MessageType.SIMSOCK_ERROR, "error " + e.getMessage());
//                disconnected();
                // TODO Auto-generated catch block
                e.printStackTrace();
                break;
            }
        }


    }

    public void disconnected() {
        makeMessage(MessageType.SIMSOCK_DISCONNECTED, "");
        Log.d("SimpleSocket", "socket_thread loop terminated");

        try {
            buffRecv.close();
            buffSend.close();

            dos.close();
            socket.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    synchronized boolean isConnected() {
        return socket.isConnected();
    }

    public void sendFile(String filePath) {
        try {
            Log.d("소켓", "데이터 보냄 " + filePath);

            // sendfile

            File myFile = new File(filePath);
            dos = new DataOutputStream(socket.getOutputStream());
            fis = new FileInputStream(myFile);
            byte[] buffer = new byte[1024];

            while (fis.read(buffer) > 0) {
                dos.write(buffer);
                dos.flush();
            }
            fis.close();

            Log.d("소켓", "데이터 보냄 완료" + myFile.exists() + socket.isConnected());

        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void sendString(String str) {
        Log.d("소켓", "데이터 보냄 " + str + out);
        out.println(str);
        out.flush();
    }
}
