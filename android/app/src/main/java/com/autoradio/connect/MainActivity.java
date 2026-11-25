package com.autoradio.connect;
import android.Manifest;
import android.app.Activity;
import android.os.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.hardware.usb.*;
import android.bluetooth.*;
import android.widget.*;
import android.view.*;
import java.util.*;
public class MainActivity extends Activity {
 TextView output; BluetoothAdapter bt; UsbManager usb; StringBuilder report=new StringBuilder(); boolean registered=false; BluetoothLeScannerHolder ble=new BluetoothLeScannerHolder();
 void log(String s){report.append(s).append("\n");output.setText(report.toString());}
 void button(LinearLayout l,String name,Runnable action){Button b=new Button(this);b.setText(name);b.setOnClickListener(v->{try{action.run();}catch(Exception e){log("Erro: "+e);}});l.addView(b);}
 @Override public void onCreate(Bundle b){super.onCreate(b);usb=(UsbManager)getSystemService(USB_SERVICE);BluetoothManager bm=getSystemService(BluetoothManager.class);bt=bm==null?null:bm.getAdapter();
 LinearLayout l=new LinearLayout(this);l.setOrientation(1);l.setPadding(24,48,24,24);ScrollView scroll=new ScrollView(this);scroll.addView(l);setContentView(scroll);
 TextView title=new TextView(this);title.setText("AutoRadio Connect • V2 Alpha");title.setTextSize(24);l.addView(title);
 button(l,"Diagnóstico USB Host / Accessory",this::scanUsb);
 button(l,"Bluetooth: pareados e busca clássica",this::scanBt);
 button(l,"Bluetooth LE: buscar por 12 segundos",this::scanBle);
 button(l,"Parar buscas",this::stop);
 button(l,"Compartilhar relatório",()->{Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_TEXT,report.toString());startActivity(Intent.createChooser(i,"Relatório AutoRadio"));});
 output=new TextView(this);output.setTextIsSelectable(true);l.addView(output);log("Somente descoberta. Reset e firmware bloqueados. Android "+Build.VERSION.RELEASE);
 IntentFilter f=new IntentFilter();for(String a:new String[]{UsbManager.ACTION_USB_DEVICE_ATTACHED,UsbManager.ACTION_USB_DEVICE_DETACHED,UsbManager.ACTION_USB_ACCESSORY_ATTACHED,UsbManager.ACTION_USB_ACCESSORY_DETACHED,BluetoothDevice.ACTION_FOUND,BluetoothAdapter.ACTION_DISCOVERY_FINISHED})f.addAction(a);
 if(Build.VERSION.SDK_INT>=33)registerReceiver(receiver,f,Context.RECEIVER_EXPORTED);else registerReceiver(receiver,f);registered=true;scanUsb();}
 void scanUsb(){log("\nUSB Host suportado: "+getPackageManager().hasSystemFeature(PackageManager.FEATURE_USB_HOST));if(usb==null){log("UsbManager indisponível");return;}Map<String,UsbDevice> ds=usb.getDeviceList();log("USB Host encontrados: "+ds.size());for(UsbDevice d:ds.values()){log("VID/PID "+String.format("%04x:%04x",d.getVendorId(),d.getProductId())+" produto="+d.getProductName()+" fabricante="+d.getManufacturerName()+" permissão="+usb.hasPermission(d));for(int j=0;j<d.getInterfaceCount();j++){UsbInterface x=d.getInterface(j);log("Interface "+x.getId()+" classe="+x.getInterfaceClass()+" subclasse="+x.getInterfaceSubclass()+" protocolo="+x.getInterfaceProtocol()+" endpoints="+x.getEndpointCount());}}UsbAccessory[] as=usb.getAccessoryList();log("USB Accessory encontrados: "+(as==null?0:as.length));if(as!=null)for(UsbAccessory a:as)log("Accessory "+a.getManufacturer()+" "+a.getModel()+" versão="+a.getVersion());if(ds.isEmpty()&&(as==null||as.length==0))log("Nenhum periférico ou acessório exposto ao Android. A porta do rádio pode operar como host de mídia ou somente alimentação.");}
 boolean permission(){String[] p=Build.VERSION.SDK_INT>=31?new String[]{Manifest.permission.BLUETOOTH_SCAN,Manifest.permission.BLUETOOTH_CONNECT}:new String[]{Manifest.permission.ACCESS_FINE_LOCATION};for(String x:p)if(checkSelfPermission(x)!=PackageManager.PERMISSION_GRANTED){requestPermissions(p,10);log("Autorize dispositivos próximos e toque novamente.");return false;}return true;}
 boolean ready(){if(!permission())return false;if(bt==null||!bt.isEnabled()){log("Ative Bluetooth nas configurações do Android.");return false;}return true;}
 void scanBt(){if(!ready())return;stop();for(BluetoothDevice d:bt.getBondedDevices())log("Pareado: "+d.getName()+" tipo="+d.getType());log("Busca clássica iniciada: "+bt.startDiscovery());}
 void scanBle(){if(!ready())return;stop();ble.scanner=bt.getBluetoothLeScanner();if(ble.scanner==null){log("Scanner BLE indisponível");return;}ble.scanner.startScan(ble.callback);log("Busca BLE iniciada");ble.handler.postDelayed(ble.end,12000);}
 void stop(){ble.handler.removeCallbacks(ble.end);if(ble.scanner!=null){try{ble.scanner.stopScan(ble.callback);}catch(Exception ignored){}ble.scanner=null;}if(bt!=null&&(Build.VERSION.SDK_INT<31||checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN)==PackageManager.PERMISSION_GRANTED))bt.cancelDiscovery();}
 class BluetoothLeScannerHolder{android.bluetooth.le.BluetoothLeScanner scanner;Handler handler=new Handler(Looper.getMainLooper());Runnable end=()->{stop();log("Busca BLE encerrada");};android.bluetooth.le.ScanCallback callback=new android.bluetooth.le.ScanCallback(){public void onScanResult(int type,android.bluetooth.le.ScanResult r){String name=r.getScanRecord()==null?null:r.getScanRecord().getDeviceName();runOnUiThread(()->log("BLE: "+name+" RSSI="+r.getRssi()));}public void onScanFailed(int code){runOnUiThread(()->log("Falha BLE: "+code));}};}
 BroadcastReceiver receiver=new BroadcastReceiver(){public void onReceive(Context c,Intent i){String a=i.getAction();try{if(BluetoothDevice.ACTION_FOUND.equals(a)){BluetoothDevice d=i.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);if(d!=null)log("Bluetooth encontrado: "+d.getName()+" tipo="+d.getType());}else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(a))log("Busca clássica encerrada");else{log("Evento USB: "+a);scanUsb();}}catch(Exception e){log("Evento: "+e);}}};
 @Override protected void onDestroy(){stop();if(registered)unregisterReceiver(receiver);super.onDestroy();}
}
