package com.linuxfunkar.mousekeysremote;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Toast;

public class PingService extends Service {
	private final IBinder binder = new PingBinder();
	private Handler handler;
	private Runnable timer;
	private boolean connected = false;

	@Override
	public void onCreate() {
		super.onCreate();

		handler = new Handler();

		timer = new Runnable() {
			@Override
			public void run() {
				String host = getHost();
				int port = getPort();
				if (!host.equals("")) {
					if (!connected) {
						connected = ping(host, port);
						if (connected) {
							Toast.makeText(
									getApplicationContext(),
									getString(R.string.connection_established_with)
											+ host + ":" + port,
									Toast.LENGTH_SHORT).show();

						}
					} else {
						boolean c = ping(host, port);
						if (c == false) {
							Toast.makeText(
									getApplicationContext(),
									getString(R.string.connection_lost) + host
											+ ":" + port, Toast.LENGTH_LONG)
									.show();
						}
						connected = c;
					}
				}
				handler.postDelayed(timer, 10000);
			}
		};

		handler.postDelayed(timer, 100);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		handler.removeCallbacks(timer);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return binder;
	}

	public class PingBinder extends Binder {
		PingService getService() {
			return PingService.this;
		}
	}

	private boolean ping(String host, int port) {
		try {
			Security security = new Security(Preferences.getInstance(this)
					.getPassword());

			DatagramSocket socket = new DatagramSocket();
			socket.connect(new InetSocketAddress(host, port));
			byte[] msg = security.encrypt("ping").getBytes("UTF-8");
			socket.send(new DatagramPacket(msg, msg.length));
			socket.setSoTimeout(5000);

			DatagramPacket response = new DatagramPacket(msg, 4);
			socket.receive(response);
			BufferedReader input = new BufferedReader(new InputStreamReader(
					new ByteArrayInputStream(response.getData()),
					Charset.forName("UTF-8")));
			String s = input.readLine();

			if (!s.startsWith("pong")) {
				Toast.makeText(
						getApplicationContext(),
						getString(R.string.wrong_response_from_server_at)
								+ host + ":" + port, Toast.LENGTH_LONG).show();
			}
			socket.close();
		} catch (Exception ex) {
			Toast.makeText(
					getApplicationContext(),
					getString(R.string.connection_failed_with) + host + ":"
							+ port, Toast.LENGTH_LONG).show();
			return false;
		}
		return true;
	}

	private String getHost() {
		SharedPreferences prefs = getApplicationContext().getSharedPreferences(
				"MY_PREFS", Activity.MODE_PRIVATE);
		return prefs.getString("host", "");
	}

	private int getPort() {
		SharedPreferences prefs = getApplicationContext().getSharedPreferences(
				"MY_PREFS", Activity.MODE_PRIVATE);
		return prefs.getInt("port", Constants.UDP_PORT);
	}
}
