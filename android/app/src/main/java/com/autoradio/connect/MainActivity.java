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
 TextView title=new TextView(this);title.setText("AutoRadio Connect • V2.1 Pioneer Control");title.setTextSize(24);l.addView(title);
 button(l,"Pioneer: selecionar e consultar serviços",()->choose(false));
 button(l,"Pioneer: conectar SPP (somente leitura)",()->choose(true));
 button(l,"Desconectar SPP",this::closeSerial);
 button(l,"Diagnóstico USB Host / Accessory",this::scanUsb);
 button(l,"Bluetooth: pareados e busca clássica",this::scanBt);
 button(l,"Bluetooth LE: buscar por 12 segundos",this::scanBle);
 button(l,"Parar buscas",this::stop);
 button(l,"Compartilhar relatório",()->{Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_TEXT,report.toString());startActivity(Intent.createChooser(i,"Relatório AutoRadio"));});
 output=new TextView(this);output.setTextIsSelectable(true);l.addView(output);log("Somente descoberta. Reset e firmware bloqueados. Android "+Build.VERSION.RELEASE);
 IntentFilter f=new IntentFilter();for(String a:new String[]{UsbManager.ACTION_USB_DEVICE_ATTACHED,UsbManager.ACTION_USB_DEVICE_DETACHED,UsbManager.ACTION_USB_ACCESSORY_ATTACHED,UsbManager.ACTION_USB_ACCESSORY_DETACHED,BluetoothDevice.ACTION_FOUND,BluetoothDevice.ACTION_UUID,BluetoothAdapter.ACTION_DISCOVERY_FINISHED})f.addAction(a);
 if(Build.VERSION.SDK_INT>=33)registerReceiver(receiver,f,Context.RECEIVER_EXPORTED);else registerReceiver(receiver,f);registered=true;scanUsb();}
 final java.util.UUID spp=java.util.UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
 volatile BluetoothSocket serial; final Handler serialHandler=new Handler(Looper.getMainLooper());
 void choose(boolean connect){if(!ready())return;ArrayList<BluetoothDevice> devices=new ArrayList<>(bt.getBondedDevices());if(devices.isEmpty()){log("Nenhum dispositivo pareado.");return;}String[] names=new String[devices.size()];for(int j=0;j<names.length;j++)names[j]=devices.get(j).getName()+" ("+devices.get(j).getAddress()+")";new android.app.AlertDialog.Builder(this).setTitle("Selecione seu Pioneer").setItems(names,(dialog,index)->{BluetoothDevice d=devices.get(index);if(connect)openSerial(d);else{log("Selecionado: "+d.getName()+" UUIDs em cache: "+Arrays.toString(d.getUuids()));log("Consulta SDP iniciada: "+d.fetchUuidsWithSdp());}}).show();}
 synchronized void closeSerial(){BluetoothSocket old=serial;serial=null;if(old!=null){try{old.close();}catch(Exception ignored){}log("SPP desconectado.");}}
 void openSerial(BluetoothDevice d){closeSerial();stop();try{final BluetoothSocket socket=d.createRfcommSocketToServiceRecord(spp);serial=socket;log("Tentando SPP seguro: "+d.getName());Runnable timeout=()->{if(serial==socket&&!socket.isConnected()){log("SPP: tempo limite de conexão.");closeSerial();}};serialHandler.postDelayed(timeout,20000);new Thread(()->{try{socket.connect();serialHandler.removeCallbacks(timeout);runOnUiThread(()->{if(serial==socket)log("SPP conectado. Escutando dados, sem transmitir comandos.");});java.io.InputStream in=socket.getInputStream();byte[] buf=new byte[256];int total=0,n;while(serial==socket&&total<4096&&(n=in.read(buf))>0){total+=n;StringBuilder hex=new StringBuilder();for(int k=0;k<n;k++)hex.append(String.format("%02X ",buf[k]&255));String line=hex.toString();runOnUiThread(()->{if(serial==socket)log("RX HEX: "+line);});} }catch(Exception e){runOnUiThread(()->{if(serial==socket)log("SPP falhou/encerrou: "+e.getMessage());});}finally{serialHandler.removeCallbacks(timeout);try{socket.close();}catch(Exception ignored){}runOnUiThread(()->{if(serial==socket){serial=null;log("Sessão SPP encerrada.");}});}},"spp-reader").start();}catch(Exception e){log("Erro SPP: "+e);}}
 void scanUsb(){log("\nUSB Host suportado: "+getPackageManager().hasSystemFeature(PackageManager.FEATURE_USB_HOST));if(usb==null){log("UsbManager indisponível");return;}Map<String,UsbDevice> ds=usb.getDeviceList();log("USB Host encontrados: "+ds.size());for(UsbDevice d:ds.values()){log("VID/PID "+String.format("%04x:%04x",d.getVendorId(),d.getProductId())+" produto="+d.getProductName()+" fabricante="+d.getManufacturerName()+" permissão="+usb.hasPermission(d));for(int j=0;j<d.getInterfaceCount();j++){UsbInterface x=d.getInterface(j);log("Interface "+x.getId()+" classe="+x.getInterfaceClass()+" subclasse="+x.getInterfaceSubclass()+" protocolo="+x.getInterfaceProtocol()+" endpoints="+x.getEndpointCount());}}UsbAccessory[] as=usb.getAccessoryList();log("USB Accessory encontrados: "+(as==null?0:as.length));if(as!=null)for(UsbAccessory a:as)log("Accessory "+a.getManufacturer()+" "+a.getModel()+" versão="+a.getVersion());if(ds.isEmpty()&&(as==null||as.length==0))log("Nenhum periférico ou acessório exposto ao Android. A porta do rádio pode operar como host de mídia ou somente alimentação.");}
 boolean permission(){String[] p=Build.VERSION.SDK_INT>=31?new String[]{Manifest.permission.BLUETOOTH_SCAN,Manifest.permission.BLUETOOTH_CONNECT}:new String[]{Manifest.permission.ACCESS_FINE_LOCATION};for(String x:p)if(checkSelfPermission(x)!=PackageManager.PERMISSION_GRANTED){requestPermissions(p,10);log("Autorize dispositivos próximos e toque novamente.");return false;}return true;}
 boolean ready(){if(!permission())return false;if(bt==null||!bt.isEnabled()){log("Ative Bluetooth nas configurações do Android.");return false;}return true;}
 void scanBt(){if(!ready())return;stop();for(BluetoothDevice d:bt.getBondedDevices())log("Pareado: "+d.getName()+" tipo="+d.getType());log("Busca clássica iniciada: "+bt.startDiscovery());}
 void scanBle(){if(!ready())return;stop();ble.scanner=bt.getBluetoothLeScanner();if(ble.scanner==null){log("Scanner BLE indisponível");return;}ble.scanner.startScan(ble.callback);log("Busca BLE iniciada");ble.handler.postDelayed(ble.end,12000);}
 void stop(){ble.handler.removeCallbacks(ble.end);if(ble.scanner!=null){try{ble.scanner.stopScan(ble.callback);}catch(Exception ignored){}ble.scanner=null;}if(bt!=null&&(Build.VERSION.SDK_INT<31||checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN)==PackageManager.PERMISSION_GRANTED))bt.cancelDiscovery();}
 class BluetoothLeScannerHolder{android.bluetooth.le.BluetoothLeScanner scanner;Handler handler=new Handler(Looper.getMainLooper());Runnable end=()->{stop();log("Busca BLE encerrada");};android.bluetooth.le.ScanCallback callback=new android.bluetooth.le.ScanCallback(){public void onScanResult(int type,android.bluetooth.le.ScanResult r){String name=r.getScanRecord()==null?null:r.getScanRecord().getDeviceName();runOnUiThread(()->log("BLE: "+name+" RSSI="+r.getRssi()));}public void onScanFailed(int code){runOnUiThread(()->log("Falha BLE: "+code));}};}
 BroadcastReceiver receiver=new BroadcastReceiver(){public void onReceive(Context c,Intent i){String a=i.getAction();try{if(BluetoothDevice.ACTION_UUID.equals(a)){BluetoothDevice d=i.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);android.os.Parcelable[] ids=i.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID);log("SDP: "+(d==null?"?":d.getName())+" UUIDs="+Arrays.toString(ids));}else if(BluetoothDevice.ACTION_FOUND.equals(a)){BluetoothDevice d=i.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);if(d!=null)log("Bluetooth encontrado: "+d.getName()+" tipo="+d.getType());}else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(a))log("Busca clássica encerrada");else{log("Evento USB: "+a);scanUsb();}}catch(Exception e){log("Evento: "+e);}}};
 @Override protected void onDestroy(){closeSerial();stop();if(registered)unregisterReceiver(receiver);super.onDestroy();}
}
