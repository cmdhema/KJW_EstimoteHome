package kr.kau.kjw.estimote_home;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.Source;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.estimote.sdk.Utils;

@EActivity(R.layout.layout_main)
public class EstimoteActivity extends Activity {

	private String TAG = EstimoteActivity.class.getSimpleName();

	// Y positions are relative to height of bg_distance image.
	private static final double RELATIVE_START_POS = 320.0 / 1110.0;
	private static final double RELATIVE_STOP_POS = 885.0 / 1110.0;

	private static final int REQUEST_ENABLE_BT = 1234;
	private static final Region ALL_ESTIMOTE_BEACONS_REGION = new Region("rid", null, null, null);

	@ViewById(R.id.range_tv)
	TextView rangeTv;
	@ViewById(R.id.todayfortune_tv)
	TextView fortuneTv;
	
	private BeaconManager beaconManager;
	private Beacon beacon;
	private Region region;
	
	private String fortuneUrl = "http://www.erumy.com/free/todayFortuneReport.aspx?m=dummy&uday=19900202&lunar=1";

	@AfterInject
	void afterInject() {

		beaconManager = new BeaconManager(this);

		// Check if device supports Bluetooth Low Energy.
		if (!beaconManager.hasBluetooth()) {
			Toast.makeText(this, "Device does not have Bluetooth Low Energy", Toast.LENGTH_LONG).show();
			return;
		}

		// If Bluetooth is not enabled, let user enable it.
		if (!beaconManager.isBluetoothEnabled()) {
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}
	}

	@AfterViews
	void afterViews() {
		connectToService();
		beaconManager.setRangingListener(new BeaconManager.RangingListener() {
			
			@Override
			public void onBeaconsDiscovered(Region region, List<Beacon> beacons) {
				beacon = beacons.get(0);
				
				runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						getActionBar().setSubtitle(beacon.getName());
						Log.i(TAG, String.format("%.2f", Utils.computeAccuracy(beacon)));
						rangeTv.setText(String.format("%.2f", Utils.computeAccuracy(beacon))+"m");
					}
				});
			}
		});
		
		getTodayFortune();
	}

	private void getTodayFortune() {
		new TodayFortuneTask().execute();
	}
	
	private class TodayFortuneTask extends AsyncTask<Void, Void, String> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}

		@Override
		protected String doInBackground(Void... params) {

			try {
				URL url = new URL(fortuneUrl);
				Source source = new Source(url);
				List<Element> list = source.getAllElements(HTMLElementName.LABEL);
				Element element;
				Element fortuneTableLabel = null;
				Element fortuneUl = null;
				Log.e(TAG, list.size()+"¿‘¥œ¥Ÿ.");
				for ( int i = 0; i < list.size(); i++) {
					element = list.get(i);
//					Log.e(TAG, "Element " + element.getFirstStartTag().toString());
					Log.e(TAG, element.getFirstStartTag().getAttributes().toString());
					String fortuneAttr = " id=" + "\"ctl00_ctl00_ctl00_ContentPlaceHolder1_ContentPlaceHolder1_ContentPlaceHolder1_TodayFortune\"";
					if ( element.getFirstStartTag().getAttributes().toString().equals(fortuneAttr)) {
						fortuneTableLabel = element;
						fortuneUl = fortuneTableLabel.getFirstElement();

						Log.e(TAG, "Hi");
						break;
					}
				}

				String todayFortune = fortuneUl.getTextExtractor().toString();
				return todayFortune;
				
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);

			fortuneTv.setText(result);
		}
		
		
		
	}
	
	private void connectToService() {
		getActionBar().setSubtitle("Scanning...");
		beaconManager.connect(new BeaconManager.ServiceReadyCallback() {

			@Override
			public void onServiceReady() {
				try {
					beaconManager.startRanging(ALL_ESTIMOTE_BEACONS_REGION);
				} catch (RemoteException e) {
					Toast.makeText(EstimoteActivity.this, "Cannot start ranging, something terrible happened", Toast.LENGTH_LONG).show();
					e.printStackTrace();
				}
			}
		});
	}

	@Override
	protected void onStop() {
		try {
			beaconManager.stopRanging(ALL_ESTIMOTE_BEACONS_REGION);
		} catch (RemoteException e) {
			Log.d(TAG, "Error while stopping ranging", e);
		}

		super.onStop();
	}
}
