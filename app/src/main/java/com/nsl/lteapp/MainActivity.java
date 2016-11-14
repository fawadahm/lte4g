package com.nsl.lteapp;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.TextView;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteOrder;



//APP THAT WILL SEND THE POINT CLOUD

public class MainActivity extends AppCompatActivity {

  final boolean IS_SENDER = false;//To distinguish between sender & receiver apps


  String serverAddrStr = "52.165.36.103";
  InetAddress serverAddr;
  final int port = 1111;
  WifiManager mWifiManager;
  WifiInfo mWifiInfo;
  boolean isWifiEnabled;

  private boolean firstTimeSend;



  private TextView SocketStateView, DataRateView, DataTransferView;
  private String setSocketState, setDataRate, setDataTransfer;

  boolean debug = true; //for enabling/disabling debug messages

  final String className = "Main Activity";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);


    isWifiEnabled = false;
    try {
      serverAddr = InetAddress.getByName(serverAddrStr);
    }
    catch (IOException e)
    {
      Log.d (className, e.getMessage());
    }



    SocketStateView = (TextView) this.findViewById(R.id.SocketState);
    DataRateView = (TextView) this.findViewById(R.id.DataRate);
    DataTransferView = (TextView) this.findViewById(R.id.DataSentOrReceived);
    //create the sender thread

    if (IS_SENDER)
    {
      new Thread ( new SenderThread()).start();
      firstTimeSend = true;
    }
    else
      new Thread( new ReceiverThread()).start();








  }

  private void setView (final TextView textView, final String message)
  {
    MainActivity.this.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        textView.setText(message);
      }
    });

  }


  private void getConnectionInfo ()
  {
    mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
    isWifiEnabled = mWifiManager.isWifiEnabled();
    if (isWifiEnabled == false) {
      mWifiManager.setWifiEnabled(true);
      while (!mWifiManager.isWifiEnabled()) ;
    }
    else;

    mWifiInfo = mWifiManager.getConnectionInfo();
    int ipAddr = mWifiInfo.getIpAddress();

    // Convert little-endian to big-endianif needed
    if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
      ipAddr = Integer.reverseBytes(ipAddr);
    }

    byte[] ipByteArray = BigInteger.valueOf(ipAddr).toByteArray();

    String ipAddressString;
    try {
      ipAddressString = InetAddress.getByAddress(ipByteArray).getHostAddress();
    } catch (UnknownHostException ex) {
      Log.e("WIFIIP", "Unable to get host address.");
      ipAddressString = null;
    }

    Log.d (className, "IP Address = " + ipAddressString);








  }


  class SenderThread implements Runnable
  {

    @Override
    public void run ()
    {
      try {
        //create a socket and connect to the proxy server
        Socket clientSocket = new Socket();
        clientSocket.connect(new InetSocketAddress(serverAddrStr, port), 0);

        if (debug)
          Log.d (className, "Connected to Server on " +serverAddrStr +" at port = " +port );
        setView(SocketStateView, "SOCKET STATE: Connected");

        //open the input file

        /**
         FileInputStream fis = new FileInputStream(Environment.getExternalStorageDirectory() + "/pointcloudfiles/fivepointhree.txt");

         //Specify File Size
         int dataToSend = fis.available()/(1000 * 1000);
         if (debug)
         Log.d(className, "File size = " + fis.available()/(1000 * 1000.0f) + " MB");
         //Get the output stream
         */
        OutputStream outputStream = clientSocket.getOutputStream();

        int streamSize = 1024 * 1024;
        byte [] byteArray = new byte [streamSize];
        int flag = 0;

        long startTime = 0;
        int counter = 0;
        int myCounter = 0;
        int deltaDataSent = 0;
        long currentTime = 0;
        float elapsedTime = 0;

        outputStream.write("Sender".getBytes());
        outputStream.flush();
        outputStream.write("Sender String".getBytes());
        outputStream.flush();


        /**
         while ( (flag = fis.read(byteArray, counter * byteArray.length, byteArray.length)) != -1)
         {
         if (firstTimeSend)
         {
         Log.d(className, "Transmission Start Time = " + System.currentTimeMillis() +" ms");
         startTime = System.currentTimeMillis();//Note the Start Time
         firstTimeSend = false;
         }
         outputStream.write(byteArray);//write data to the output port



         deltaDataSent = ((myCounter+1)*byteArray.length)/(1000);
         currentTime = System.currentTimeMillis();
         elapsedTime = (currentTime - startTime)/(1000.0f*1000.0f);

         //TextViews
         setView(DataTransferView, "DATA SENT = " + deltaDataSent + " kb");
         setView(DataRateView, "DATA RATE = " +
         (deltaDataSent*8.0f)/(elapsedTime*1000*1000) + "" + " MBps");
         myCounter++;
         }

         */

        /**
         Log.d(className, "End Time = " +System.currentTimeMillis() + " ms");

         Log.d (className, "Data Sent");
         long endTime = System.currentTimeMillis();//Note the End Time
         elapsedTime = (endTime - startTime)/1000.0f;
         float dataSent = dataToSend;

         if (elapsedTime != 0)
         Log.d(className, "Speed =  " + (dataSent * 8.0f)/(elapsedTime) + " Mbps");
         Log.d (className, "Data Sent = " + (dataSent * 8.0f)/(1000*1000.0f) + " MB");
         Log.d (className, "Time = " + elapsedTime + " seconds");


         */

        outputStream.flush();
        outputStream.close();
        //fis.close();

        clientSocket.close();
      }
      catch (IOException e)
      {
        Log.d(className, e.getMessage());
        setView(SocketStateView, "SOCKET STATE: Reset App");
      }
    }

  }


  class ReceiverThread implements Runnable
  {

    @Override
    public void run ()
    {
      try {
        //create a socket and connect to the proxy server
        Socket clientSocket = new Socket();
        clientSocket.connect(new InetSocketAddress(serverAddrStr, port), 0);

        if (debug)
          Log.d (className, "Connected to Server on " +serverAddrStr +" at port = " +port );
        setView(SocketStateView, "SOCKET STATE: Connected");

        //open the input file

        /**
         FileInputStream fis = new FileInputStream(Environment.getExternalStorageDirectory() + "/pointcloudfiles/fivepointhree.txt");

         //Specify File Size
         int dataToSend = fis.available()/(1000 * 1000);
         if (debug)
         Log.d(className, "File size = " + fis.available()/(1000 * 1000.0f) + " MB");
         //Get the output stream
         */
        OutputStream outputStream = clientSocket.getOutputStream();
        InputStream inputStream = clientSocket.getInputStream();

        int streamSize = 1024 * 1024;
        byte [] byteArray = new byte [streamSize];
        int flag = 0;

        long startTime = 0;
        int counter = 0;
        int myCounter = 0;
        int deltaDataSent = 0;
        long currentTime = 0;
        float elapsedTime = 0;

        outputStream.write("Receiver".getBytes());
        outputStream.flush();
          Log.d (className, "Now going to send data");
        //outputStream.write(" do not display this".getBytes());


          int bytesRead = inputStream.read(byteArray);

          if (bytesRead != -1) {
              Log.d(className, "Received Data = " + byteArray.toString());
              Log.d (className, "Received Data Bytes = " + byteArray);
          }
          else
            Log.d(className, "Nothing to read");






        /**
         while ( (flag = fis.read(byteArray, counter * byteArray.length, byteArray.length)) != -1)
         {
         if (firstTimeSend)
         {
         Log.d(className, "Transmission Start Time = " + System.currentTimeMillis() +" ms");
         startTime = System.currentTimeMillis();//Note the Start Time
         firstTimeSend = false;
         }
         outputStream.write(byteArray);//write data to the output port



         deltaDataSent = ((myCounter+1)*byteArray.length)/(1000);
         currentTime = System.currentTimeMillis();
         elapsedTime = (currentTime - startTime)/(1000.0f*1000.0f);

         //TextViews
         setView(DataTransferView, "DATA SENT = " + deltaDataSent + " kb");
         setView(DataRateView, "DATA RATE = " +
         (deltaDataSent*8.0f)/(elapsedTime*1000*1000) + "" + " MBps");
         myCounter++;
         }

         */

        /**
         Log.d(className, "End Time = " +System.currentTimeMillis() + " ms");

         Log.d (className, "Data Sent");
         long endTime = System.currentTimeMillis();//Note the End Time
         elapsedTime = (endTime - startTime)/1000.0f;
         float dataSent = dataToSend;

         if (elapsedTime != 0)
         Log.d(className, "Speed =  " + (dataSent * 8.0f)/(elapsedTime) + " Mbps");
         Log.d (className, "Data Sent = " + (dataSent * 8.0f)/(1000*1000.0f) + " MB");
         Log.d (className, "Time = " + elapsedTime + " seconds");


         */

        //outputStream.flush();
        //outputStream.close();
        //fis.close();

        clientSocket.close();
      }
      catch (IOException e)
      {
        Log.d(className, e.getMessage());
        setView(SocketStateView, "SOCKET STATE: Reset App");
      }
    }

  }







}
