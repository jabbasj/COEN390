package com.coen390.teamc.cardiograph;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import zephyr.android.HxMBT.*;

public class NewConnectedListener extends ConnectListenerImpl
{
    private Handler _OldHandler;
    private Handler _aNewHandler;
    private int GP_MSG_ID = 0x20;
    private int GP_HANDLER_ID = 0x20;
    private int HR_SPD_DIST_PACKET =0x26;

    private final int HEART_RATE = 0x100;
    private final int BATTERY_PERCENT = 0x102;
    private final int HEART_BEAT_TIMESTAMPS = 0x103;
    private final int HEART_BEAT_NUMBER = 0x104;
    private HRSpeedDistPacketInfo HRSpeedDistPacket = new HRSpeedDistPacketInfo();
    public NewConnectedListener(Handler handler,Handler _NewHandler) {
        super(handler, null);
        _OldHandler= handler;
        _aNewHandler = _NewHandler;

        // TODO Auto-generated constructor stub

    }
    public void Connected(ConnectedEvent<BTClient> eventArgs) {
        System.out.println(String.format("Connected to BioHarness %s.", eventArgs.getSource().getDevice().getName()));

        //Creates a new ZephyrProtocol object and passes it the BTComms object
        ZephyrProtocol _protocol = new ZephyrProtocol(eventArgs.getSource().getComms());
        //ZephyrProtocol _protocol = new ZephyrProtocol(eventArgs.getSource().getComms(), );
        _protocol.addZephyrPacketEventListener(new ZephyrPacketListener() {
            public void ReceivedPacket(ZephyrPacketEvent eventArgs) {
                ZephyrPacketArgs msg = eventArgs.getPacket();
                byte CRCFailStatus;
                byte RcvdBytes;

                CRCFailStatus = msg.getCRCStatus();
                RcvdBytes = msg.getNumRvcdBytes() ;
                if (HR_SPD_DIST_PACKET==msg.getMsgID())
                {


                    byte [] DataArray = msg.getBytes();

                    //***************Displaying the Heart Rate********************************
                    int HRate =  HRSpeedDistPacket.GetHeartRate(DataArray);

                    /* unsigned int */
                    if (HRate < 0)  {
                        HRate = -1 * HRate;
                    }

                    Message text1 = _aNewHandler.obtainMessage(HEART_RATE);
                    Bundle b1 = new Bundle();
                    b1.putString("HeartRate", String.valueOf(HRate));
                    text1.setData(b1);
                    _aNewHandler.sendMessage(text1);

                    System.out.println("Heart Rate is "+ HRate);

                    //***************Displaying the Instant Speed********************************
                    /*)
                    double InstantSpeed = HRSpeedDistPacket.GetInstantSpeed(DataArray);

                    text1 = _aNewHandler.obtainMessage(INSTANT_SPEED);
                    b1.putString("InstantSpeed", String.valueOf(InstantSpeed));
                    text1.setData(b1);
                    _aNewHandler.sendMessage(text1);

                    System.out.println("Instant Speed is "+ InstantSpeed);
                    */
                    //***************Displaying the Battery********************************
                    int Battery = HRSpeedDistPacket.GetBatteryChargeInd(DataArray);

                    /* unsigned int */
                    if (Battery < 0)  {
                        Battery = -1 * Battery;
                    }

                    text1 = _aNewHandler.obtainMessage(BATTERY_PERCENT);
                    b1.putString("BatteryPercent", String.valueOf(Battery));
                    text1.setData(b1);
                    _aNewHandler.sendMessage(text1);

                    System.out.println("Battery is " + Battery + "%");

                    //***************Displaying the Heart beat Timestamps********************************

                    int[] heartBeatTS = HRSpeedDistPacket.GetHeartBeatTS(DataArray);
                    int heartbeatNumber = HRSpeedDistPacket.GetHeartBeatNum(DataArray);
                    /* unsigned int
                    if (heartbeatNumber < 0)  {
                        heartbeatNumber = -1 * heartbeatNumber;
                    }*/

                    text1 = _aNewHandler.obtainMessage(HEART_BEAT_TIMESTAMPS);
                    b1.putIntArray("HeartbeatTimeStamps", heartBeatTS);
                    b1.putInt("HeartbeatNumber", heartbeatNumber);
                    text1.setData(b1);
                    _aNewHandler.sendMessage(text1);

                    /*
                    for (int i =0; i < heartBeatTS.length; i ++ ) {
                        System.out.println("HeartBeat @ " + heartBeatTS[i] + " ms");
                    }
                    */


                }
            }
        });
    }

}