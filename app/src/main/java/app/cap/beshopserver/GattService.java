package app.cap.beshopserver;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.DataFormatException;

public class GattService extends Service {

    ///////japke의 속성/////
    public byte[] round1;
    public byte[] round2;
    public byte[] round3;
    public byte[] round4;

    private static int NOTIFICATION_ID = 0;
    public static final ParcelUuid UUID = ParcelUuid.fromString("0000FED8-0000-1000-8000-00805F9B34FB");
    public static final java.util.UUID SERVICE_UUID = java.util.UUID.fromString("00001111-0000-1000-8000-00805F9B34FB");
    public static final java.util.UUID CHAR_UUID = java.util.UUID.fromString("00002222-0000-1000-8000-00805F9B34FB");
    public static final java.util.UUID DES_UUID = java.util.UUID.fromString("00003333-0000-1000-8000-00805F9B34FB");
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGattServer server;
    private BluetoothLeAdvertiser bluetoothLeAdvertiser;
    private boolean start;
    private LocalBroadcastManager broadcaster;
    static final public String COPA_RESULT = "app.cap.beshopserver.GattService.REQUEST_PROCESSED";
    static final public String DEVICE_NAME = "app.cap.beshopserver.GattService.DEVICE_NAME";
    static final public String LOGS_PROTOCOL = "app.cap.beshopserver.pos.GattService.LOGS_PROTOCOL";
    static final public String OTHER_LOGS = "app.cap.beshopserver.GattService.OTHER_LOGS";
    static final public String CLOSE = "app.cap.beshopserver.GattService.CLOSE";

    //jpake 용 init 프로토콜
    private final jPake jpake = new jPake(1);
    private Integer protocolCounter = 0;

    public Integer numPackets = 0;
    public byte[] packetData;
    public boolean packetFinish = false;
    @Override
    public void onCreate() {
        super.onCreate();
        broadcaster = LocalBroadcastManager.getInstance(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        setupBluetooth();
        return Service.START_STICKY;
    }

    private void setupBluetooth() {

        BluetoothManager bluetoothManager = (BluetoothManager) this.getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
        server = bluetoothManager.openGattServer(this, serverCallback);
        initServer();
        bluetoothAdapter = bluetoothManager.getAdapter();
        advertise();
    }
    public void setDeviceName(String message) {
        Intent intent = new Intent(COPA_RESULT);
        if(message != null)
            intent.putExtra(DEVICE_NAME, message);
        broadcaster.sendBroadcast(intent);
    }
    public void sendOtherLogs(String message){
        Intent intent = new Intent(COPA_RESULT);
        if(message != null)
            intent.putExtra(OTHER_LOGS, message);
        broadcaster.sendBroadcast(intent);
    }
    public void sendLogs(byte[] message) throws IOException, DataFormatException {
        protocolCounter++;
        System.out.println("기입 로그");
        byte[] decomp = compress.decompress(message);
        ByteArrayInputStream bais = new ByteArrayInputStream(decomp);
        DataInputStream in = new DataInputStream(bais);
        List<String> result = new ArrayList<>();
        try {
            while (in.available() > 0) {
                String element = in.readUTF();
                result.add(element);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        jpake.updateValue(result);


        String base64 = Base64.encodeToString(decomp, Base64.DEFAULT);
        Intent intent = new Intent(COPA_RESULT);
        if(message != null)
            intent.putExtra(LOGS_PROTOCOL, base64);
        broadcaster.sendBroadcast(intent);
        if(jpake.finalSKey!=""){
            sendOtherLogs("\n----------------\n 최종 키: "+jpake.getfinalSKey()+"\n 최종 토큰: "+jpake.getFinalNonce()+"\n");
        }
    }
    private void initServer() {
        BluetoothGattService service = new BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(CHAR_UUID, BluetoothGattCharacteristic.PROPERTY_WRITE|BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ|BluetoothGattCharacteristic.PERMISSION_WRITE);
        service.addCharacteristic(characteristic);
        server.addService(service);
    }

    private void advertise() {

        bluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        AdvertiseData advertisementData = getAdvertisementData();
        AdvertiseSettings advertiseSettings = getAdvertiseSettings();
        bluetoothLeAdvertiser.startAdvertising(advertiseSettings, advertisementData, advertiseCallback);
        start = true;
    }

    private AdvertiseData getAdvertisementData() {
        AdvertiseData.Builder builder = new AdvertiseData.Builder();
        builder.setIncludeTxPowerLevel(true);
        builder.addServiceUuid(UUID);
        bluetoothAdapter.setName("고객");
        builder.setIncludeDeviceName(true);
        return builder.build();
    }

    private AdvertiseSettings getAdvertiseSettings() {
        AdvertiseSettings.Builder builder = new AdvertiseSettings.Builder();
        builder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);
        builder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);
        builder.setConnectable(true);
        return builder.build();
    }

    private final AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @SuppressLint("Override")
        @Override
        public void onStartSuccess(AdvertiseSettings advertiseSettings) {
            final String message = "광고 성공";
            sendNotification(message);
        }

        @SuppressLint("Override")
        @Override
        public void onStartFailure(int i) {
            final String message = "광고 실패 에러 코드: " + i;
            sendNotification(message);

        }

    };

    private BluetoothGattServerCallback serverCallback = new BluetoothGattServerCallback() {



        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);

            if(newState == BluetoothProfile.STATE_CONNECTED) {
                sendNotification("고객 연결");
                if(device.getName() != null){
                    setDeviceName(device.getAddress());
                    //try {
                        //sendLogs(round1);
                    //} catch (IOException e) {
                }else { setDeviceName(device.getAddress());}
                }
                if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                String message = "닫기";
                Intent intent = new Intent(COPA_RESULT);
                if(message != null)
                    intent.putExtra(CLOSE, message);
                    broadcaster.sendBroadcast(intent);
            }

        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            super.onServiceAdded(status, service);
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {

            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
            byte[] bytes = value;

            if(packetData == null){
                sendNotification("받은 메시지");
            }
            if(numPackets==0) {
                packetFinish=false;
                packetData = new byte[0];
                try{
                    numPackets = Integer.valueOf(new String(bytes));
                } catch (NumberFormatException e) {
                }
                Log.d("numPackets", numPackets.toString());

            }else{

                packetData =combineByte(packetData,value);
                numPackets--;
                Log.d("numPackets", numPackets.toString());
                Log.d("value", new String(value));
                if(numPackets==0){
                   // packetData = null;
                    packetFinish=true;
                }
            }
            if(packetFinish){
                System.out.println("PacketLength: "+packetData.length);
                try {
                    sendLogs(packetData);
                    packetData = null;
                    if(protocolCounter == 2){
                        //send round 1 level 2
                        sendData(characteristic,round1,device);
                        //run round 2 level 1 but not send yet, send after received
                        if(!jpake.round2){
                        round2 = jpake.jpakeRound2();
                        jpake.round2=true;
                            sendLogs(round2);
                        }
                    }else if (protocolCounter == 4){
                        //고객에게 이미 레벨1, 2라운드를 받은 다음, 라운드 2 lvl 2를 보냅니다.
                        Log.d("protocol4 running","sendDatamethod");
                        sendData(characteristic,round2,device);
                    }else if (protocolCounter == 5){
                        if(!jpake.round3){
                            round3 = jpake.pprotocolRound1();
                            jpake.round3=true;
                            sendLogs(round3);
                            sendData(characteristic,round3,device);
                        }
                    }else if (protocolCounter == 7){
                        if(!jpake.round4){
                            round4 = jpake.pprotocolRound2();
                            jpake.round4=false;
                            sendNotification("결제 성공!");
                            sendLogs(round4);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (DataFormatException e) {
                    e.printStackTrace();
                }
            }
            if(packetData!=null){
            Log.d("packetDataSize", String.valueOf(packetData.length));
            }
                server.sendResponse(device, requestId, 0, offset, value);
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            super.onDescriptorReadRequest(device, requestId, offset, descriptor);
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
        }

        @Override
        public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute){
            super.onExecuteWrite(device, requestId, execute);
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            super.onNotificationSent(device, status);
        }

        @Override
        public void onMtuChanged(BluetoothDevice device, int mtu) {
            super.onMtuChanged(device, mtu);
        }
    };

    @Override
    public void onDestroy() {
        if(start){
            bluetoothLeAdvertiser.stopAdvertising(advertiseCallback);
        }
        super.onDestroy();
    }

    private void sendNotification(String message){
        NotificationManager mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);


        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.server)
                        .setContentTitle(getString(R.string.app_name))
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(message))
                        .setAutoCancel(true)
                        .setContentText(message);
        Notification note = mBuilder.build();
        note.defaults |= Notification.DEFAULT_VIBRATE;
        note.defaults |= Notification.DEFAULT_SOUND;
        mNotificationManager.notify(NOTIFICATION_ID++, note);
    }
    public void sendData(BluetoothGattCharacteristic characteristic,byte [] data,BluetoothDevice device){
        int chunksize = 20;
        Integer packetsToSend = (int) Math.ceil( data.length / (double)chunksize);
        characteristic.setValue(packetsToSend.toString().getBytes());
        server.notifyCharacteristicChanged(device, characteristic, false);
        byte[][] packets = new byte[packetsToSend][chunksize];
        Integer start =0;
        for(int i = 0; i < packets.length; i++) {
            int end = start+chunksize;
            if(end>data.length){end = data.length;}
            packets[i] = Arrays.copyOfRange(data,start, end);
            start += chunksize;
            characteristic.setValue(packets[i]);
            server.notifyCharacteristicChanged(device, characteristic, false);
        }

    }
    public byte[] combineByte(byte[] byte1, byte[] byte2){
        byte[] combined = new byte[byte1.length + byte2.length];

        for (int i = 0; i < combined.length; ++i)
        {
            combined[i] = i < byte1.length ? byte1[i] : byte2[i - byte1.length];
        }
        return combined;
    }
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }



}
