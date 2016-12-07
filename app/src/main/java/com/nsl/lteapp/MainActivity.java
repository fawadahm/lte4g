package com.nsl.lteapp;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Date;


//APP THAT WILL SEND THE POINT CLOUD

public class MainActivity extends AppCompatActivity {

  final boolean IS_SENDER = false;//To distinguish between sender & receiver apps


  String serverAddrStr = "52.165.36.103";
  InetAddress serverAddr;
  final int port = 1111;
  WifiManager mWifiManager;
  WifiInfo mWifiInfo;
  boolean isWifiEnabled;

    Process process;

    public LocationManager mLocationManager;

    public int pcCounter = 0;
    private final int DIVISOR = 4;
    private final int ITERATIONS = 16;
    public int pcIndex = 0;

    public int receiverCounter = 0;

  private boolean firstTimeSend;

    private String pointCloudID = "fullPointCloud";



  private TextView AppVersion, ConnectionStatus, ConnectionType, SocketState, Iteration, DataRate, DataSendReceived, AppStatus;
  private String setNetworkState, setSendReceiveState, setSocketState, setDataRate, setDataTransfer;

  boolean debug = true; //for enabling/disabling debug messages

  final String className = "Main Activity";

    float averageSpeed = 0;
    int speedReadings = 0;
    float totalSpeed = 0;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);


      //Logging Speed

      //Speed Logging
      SimpleDateFormat sdf = new SimpleDateFormat("dd_HHmmss_SSS");
      final String currentDateandTime = sdf.format(new Date());
      try {
          mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
          final FileOutputStream speedFOS = new FileOutputStream(Environment.getExternalStorageDirectory() + "/LocationSpeedLog_LTEApp.txt", true);
          speedFOS.write(("\n\nTime,Latitude,Longitude,Speed\n\n").getBytes());
          //Define a listener to its events
          LocationListener mLocationListener = new LocationListener() {
              @Override
              public void onLocationChanged(Location location) {
                  try {
                      //Get current timestamp
                      final String timeStamp = new SimpleDateFormat("HHmmss_SSS").format(new Date());
                      speedFOS.write(((timeStamp) + "," + (location.getLatitude()) + "," + (location.getLatitude()) + "," + (location.getSpeed()) + "\n").getBytes());
                      //setView(CurrentSpeedView, "Speed : " + location.getSpeed());

                      //Get the speed of the device
                      totalSpeed += location.getSpeed();
                      speedReadings++;
                  }
                  catch (IOException e)
                  {
                      Log.d ("Writing File", e.getMessage());
                  }
              }

              @Override
              public void onStatusChanged(String provider, int status, Bundle extras) {

              }

              @Override
              public void onProviderEnabled(String provider) {

              }

              @Override
              public void onProviderDisabled(String provider) {

              }
          };
          //Subscribe to listener
          mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
      }
      catch (IOException e)
      {
          Log.d ("Location Manager", e.getMessage());
      }











      isWifiEnabled = false;
    try {
      serverAddr = InetAddress.getByName(serverAddrStr);
    }
    catch (IOException e)
    {
      Log.d (className, e.getMessage());
    }


    AppVersion = (TextView) this.findViewById(R.id.AppVersion);
    ConnectionStatus = (TextView) this.findViewById(R.id.ConnectionStatus);
    ConnectionType = (TextView) this.findViewById(R.id.ConnectionType);
    SocketState = (TextView) this.findViewById(R.id.SocketState);
    Iteration = (TextView) this.findViewById(R.id.Iteration);
      DataRate = (TextView) this.findViewById(R.id.DataRate);
      DataSendReceived = (TextView) this.findViewById(R.id.DataSentReceived);
      AppStatus = (TextView) this.findViewById(R.id.AppStatus);
    //create the sender thread

    if (IS_SENDER)
    {
      new Thread ( new SenderThread()).start();
      setView(AppVersion, "App Version : SENDER");
      firstTimeSend = true;
    }
    else {
      new Thread(new ReceiverThread()).start();
      setView(AppVersion, "App Version : RECEIVER");
        Log.d("onCreate", "Hello");
    }

      checkConnectionType();



  }



    public void checkConnectionType()
    {
        ConnectivityManager cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null) { // connected to the internet
            setView(ConnectionStatus, "Connection Status : Connected");
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                // connected to wifi
                setView(ConnectionType, "Connection Type : WiFi");
                Toast.makeText(this, activeNetwork.getTypeName(), Toast.LENGTH_SHORT).show();
            } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                // connected to the mobile provider's data plan
                setView(ConnectionType, "Connection Type : 4G/LTE");
                Toast.makeText(this, activeNetwork.getTypeName(), Toast.LENGTH_SHORT).show();
            }
        }
        else
            setView(ConnectionStatus, "Connection Type : Disconnected");
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


    private void resetViews ()
    {
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String message;
                if (IS_SENDER)
                    message = "Sent";
                else
                    message = "Received";
                setView(SocketState, "Socket State : Done");
                setView(Iteration, "Iteration : Done");
                setView(DataRate, "Data Rate : Done");
                setView(DataSendReceived, "Data Sent/Received : " + message);
                setView(AppStatus, "App Status : Completed");
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

    // Convert little-endian to big-endian if needed
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


        new Thread (new tcpDumpThread()).start();

        FileOutputStream FOS = startLoggingData("Sender");
        //while (pcCounter < ITERATIONS) {
            setView(Iteration, "Iteration : One");

            try {
                //create a socket and connect to the proxy server
                setView(AppStatus, "App Status : Connecting to Socket");
                Socket clientSocket = new Socket();
                clientSocket.connect(new InetSocketAddress(serverAddrStr, port), 0);
                setView(SocketState, "Socket State : Connected");
                setView(AppStatus, "App Status : Sending ID");

                pcIndex = pcCounter / DIVISOR;
                FileInputStream fis = new FileInputStream(Environment.getExternalStorageDirectory() + "/" + pointCloudID + ".txt");
                Log.d(className, fis.available() + " bytes");

                //Specify File Size
                float dataToSend = fis.available() / (1000 * 1000.0f);
                OutputStream outputStream = clientSocket.getOutputStream();


                int streamSize = 1024;
                byte[] byteArray = new byte[streamSize];
                int flag = 0;

                long startTime = 0;
                int counter = 0;
                int myCounter = 0;
                int deltaDataSent = 0;
                long currentTime = 0;
                float elapsedTime = 0;

                outputStream.write("S".getBytes());
                outputStream.flush();

                while ((flag = fis.read(byteArray, counter * byteArray.length, byteArray.length)) != -1) {
                    if (firstTimeSend) {
                        Log.d(className, "Transmission Start Time = " + System.currentTimeMillis() + " ms");
                        startTime = System.currentTimeMillis();//Note the Start Time
                        firstTimeSend = false;
                        setView(AppStatus, "App Status : Sending File");
                    }
                    outputStream.write(byteArray);//write data to the output port

                    deltaDataSent = ((myCounter + 1) * byteArray.length) / (1000);
                    currentTime = System.currentTimeMillis();
                    elapsedTime = (currentTime - startTime) / (1000.0f * 1000.0f);

                    //TextViews
                    setView(DataSendReceived, "Data Sent : " + deltaDataSent/(1000.0f) + " MB");
                    setView(DataRate, "Data Rate = " +
                            (deltaDataSent * 8.0f) / (1000 * 1000 * elapsedTime) + " MBps");
                    myCounter++;
                }

                setView(AppStatus, "App Status : File Sent");


                Log.d(className, "End Time = " + System.currentTimeMillis() + " ms");

                Log.d(className, "Data Sent");
                long endTime = System.currentTimeMillis();//Note the End Time
                elapsedTime = (endTime - startTime) / 1000.0f;
                float dataSent = dataToSend;

                if (elapsedTime != 0)
                    Log.d(className, "Speed =  " + (dataSent * 8.0f) / (elapsedTime) + " Mbps");
                Log.d(className, "Data Sent = " + (dataSent * 8.0f) / (1000 * 1000.0f) + " MB");
                Log.d(className, "Time = " + elapsedTime + " seconds");


                FOS.write(("\nFile Size = " + dataToSend + " MB\n" +
                        "Start Time = " +startTime + "\n" +
                        "End Time = " +endTime + "\n" +
                        "Flow Completion Time = " + elapsedTime + "\n" +
                        "Flow Rate = " + (dataSent/elapsedTime*1.0f) + "\n" +
                        "Average Speed = " + (totalSpeed/speedReadings*1.0f) + "\n").getBytes());


                totalSpeed = 0;
                speedReadings = 0;


                setView(DataSendReceived, "Data Sent = " + (dataSent * 8.0f) / (1000 * 1000.0f) + " MB");
                setView(DataRate, "Data Rate : " + ((dataSent * 8.0f) / (elapsedTime)) + "" + " MBps");


                outputStream.flush();
                outputStream.close();
                firstTimeSend = false;
                //fis.close();

                clientSocket.close();
                try {
                    setView(AppStatus, "App Status : Sleeping");
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Log.d(className, "Error in sleeping");
                }
                resetViews();
            } catch (IOException e) {
                Log.d(className, e.getMessage());
                setView(SocketState, "Socket State: Reset App");
            }




            //pcCounter++;//increment pcCounter
        //}
        //end of run
    }

  }

    public FileOutputStream startLoggingData (String threadID)
    {
        try {
            final String timeStamp = new SimpleDateFormat("HHmmss_SSS").format(new Date());
            final FileOutputStream speedFOS = new FileOutputStream(Environment.getExternalStorageDirectory() + "/" + threadID + "_LTEApp.txt", true);
            speedFOS.write(("\n\nSession ID : " + timeStamp + "\n\n").getBytes());
            return speedFOS;
        }
        catch (IOException e)
        {
            Log.d (className, e.getMessage());
            return null;
        }
    }


  class ReceiverThread implements Runnable
  {

    @Override
    public void run ()
    {

        new Thread (new tcpDumpThread()).start();
        //while (receiverCounter < ITERATIONS) {
            setView(Iteration, "Iteration : " + receiverCounter);
            Log.d (className, "Hello");
            setView(AppStatus, "Up here");

            try {
                //create a socket and connect to the proxy server
                setView(AppStatus, "App Status : Connecting to Socket");
                Socket clientSocket = new Socket();
                clientSocket.connect(new InetSocketAddress(serverAddrStr, port), 0);
                setView(SocketState, "Socket State : Connected");


                setView(AppStatus, "App Status : Sending ID");
                OutputStream outputStream = clientSocket.getOutputStream();
                InputStream inputStream = clientSocket.getInputStream();

                int streamSize = 1024 * 1024;
                byte[] byteArray = new byte[streamSize];
                Log.d (className, "Stream size =" +streamSize);
                int flag = 0;
                long startTime = System.currentTimeMillis();
                int counter = 0;
                int myCounter = 0;
                int deltaDataSent = 0;
                long currentTime = 0;
                float elapsedTime = 0;


                outputStream.write("Receiver".getBytes());
                outputStream.flush();
                Log.d("Now going to send data", "Hello");
                //outputStream.write(" do not display this".getBytes());

                long totalBytes = 0;


                int bytesRead;// = inputStream.read(byteArray);
                setView(AppStatus, "App Status : Waiting for Data");


                while ((bytesRead = inputStream.read(byteArray)) != -1) {
                    if (firstTimeSend) {
                        //runTCPdump("Server");
                        Log.d(className, "Started Receiving Data at = " + startTime + " ms");
                        firstTimeSend = false;
                        setView(AppStatus, "App Status : Receiving Data");
                    }


                    // AVERAGE MEASUREMENTS
                    totalBytes += bytesRead;
                    currentTime = System.currentTimeMillis();
                    elapsedTime = ((currentTime - startTime) / 1000.0f);
                    setView(DataSendReceived, "Data Received : " + (totalBytes) / (1000 * 1000.0f) + " MB");


                    //String.format("%.5g%n", 0.912300);
                    byteArray = new byte [streamSize];


                    setView(DataRate, "Data Rate : " + (totalBytes * 8.0f) / (1000 * 1000 * elapsedTime) + " MBps");
                }
                setView(DataSendReceived, "Data Received : " + (totalBytes) / (1000 * 1000.0f) + " MB");
                setView(DataRate, "Data Rate : " + (totalBytes * 8.0f) / (1000 * 1000 * elapsedTime) + " MBps");


                clientSocket.close();

                setView(AppStatus, "App Status : File Received");
                Log.d(className, "Receiver Counter = " + receiverCounter);
                receiverCounter++;
                resetViews();
            } catch (IOException e) {
                Log.d(className, e.getMessage());
                Log.d ("Was busted here","onCreate");
                setView(SocketState, "Socket State : Reset App");
            }



        }

    //}//end of run

  }




    class tcpDumpThread implements Runnable
    {

        @Override
        public void run ()
        {
            String ID;
            if (IS_SENDER)
                ID = "SenderLTE";
            else
                ID = "ReceiverLTE";

            SimpleDateFormat sdf = new SimpleDateFormat("dd_HHmmss_SSS");
            final String currentDateandTime = sdf.format(new Date());
            final String fileName = "/sdcard/" + ID + "_tcpdump_" + currentDateandTime;
            try {
                process = Runtime.getRuntime().exec("su");
                process.getOutputStream().write(("tcpdump -i any -w " + fileName +".pcap\n").getBytes());
                process.getOutputStream().flush();
                process.getOutputStream().write("exit\n".getBytes());
                process.getOutputStream().flush();
                //setView(TcpDumpView, "TCP Dump Status : Logging");
                process.waitFor();
            } catch (IOException e) {
                Log.d ("Terminal", e.getMessage());
                //setView(TcpDumpView, "TCP Dump Status : Error");
            }
            catch (InterruptedException e)
            {
                //setView(TcpDumpView, "TCP Dump Status : Error");
                Log.d ("Terminal", e.getMessage());
            }
        }
    }







}
