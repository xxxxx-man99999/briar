package org.briarproject.bramble.network;

import android.annotation.TargetApi;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.telephony.TelephonyManager;

import org.briarproject.bramble.api.Cancellable;
import org.briarproject.bramble.api.event.EventBus;
import org.briarproject.bramble.api.event.EventExecutor;
import org.briarproject.bramble.api.lifecycle.Service;
import org.briarproject.bramble.api.network.CellularNetworkType;
import org.briarproject.bramble.api.network.NetworkManager;
import org.briarproject.bramble.api.network.NetworkStatus;
import org.briarproject.bramble.api.network.event.NetworkStatusEvent;
import org.briarproject.bramble.api.system.TaskScheduler;
import org.briarproject.nullsafety.MethodsNotNullByDefault;
import org.briarproject.nullsafety.ParametersNotNullByDefault;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import javax.annotation.Nullable;
import javax.inject.Inject;

import static android.content.Context.CONNECTIVITY_SERVICE;
import static android.content.Context.TELEPHONY_SERVICE;
import static android.content.Context.WIFI_SERVICE;
import static android.content.Intent.ACTION_SCREEN_OFF;
import static android.content.Intent.ACTION_SCREEN_ON;
import static android.net.ConnectivityManager.CONNECTIVITY_ACTION;
import static android.net.ConnectivityManager.TYPE_WIFI;
import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED;
import static java.net.NetworkInterface.getNetworkInterfaces;
import static java.util.Collections.list;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.WARNING;
import static java.util.logging.Logger.getLogger;
import static org.briarproject.bramble.util.AndroidUtils.registerReceiver;
import static org.briarproject.bramble.util.LogUtils.logException;
import static org.briarproject.nullsafety.NullSafety.requireNonNull;

@MethodsNotNullByDefault
@ParametersNotNullByDefault
class AndroidNetworkManager implements NetworkManager, Service {

	private static final Logger LOG =
			getLogger(AndroidNetworkManager.class.getName());

	private static final String WIFI_AP_STATE_CHANGED_ACTION =
			"android.net.wifi.WIFI_AP_STATE_CHANGED";

	private final TaskScheduler scheduler;
	private final EventBus eventBus;
	private final Executor eventExecutor;
	private final Application app;
	private final ConnectivityManager connectivityManager;
	private final TelephonyManager telephonyManager;
	private final AtomicReference<Cancellable> connectivityCheck =
			new AtomicReference<>();
	private final AtomicBoolean used = new AtomicBoolean(false);

	private volatile BroadcastReceiver networkStateReceiver = null;

	@Inject
	AndroidNetworkManager(TaskScheduler scheduler, EventBus eventBus,
			@EventExecutor Executor eventExecutor, Application app) {
		this.scheduler = scheduler;
		this.eventBus = eventBus;
		this.eventExecutor = eventExecutor;
		this.app = app;
		connectivityManager = (ConnectivityManager)
				requireNonNull(app.getSystemService(CONNECTIVITY_SERVICE));
		telephonyManager = (TelephonyManager)
				requireNonNull(app.getSystemService(TELEPHONY_SERVICE));
	}

	@Override
	public void startService() {
		if (used.getAndSet(true)) throw new IllegalStateException();
		networkStateReceiver = new NetworkStateReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(CONNECTIVITY_ACTION);
		filter.addAction(ACTION_SCREEN_ON);
		filter.addAction(ACTION_SCREEN_OFF);
		filter.addAction(WIFI_AP_STATE_CHANGED_ACTION);
		filter.addAction(WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
		if (SDK_INT >= 23) filter.addAction(ACTION_DEVICE_IDLE_MODE_CHANGED);
		registerReceiver(app, networkStateReceiver, filter, false);
	}

	@Override
	public void stopService() {
		if (networkStateReceiver != null)
			app.unregisterReceiver(networkStateReceiver);
	}

	@Override
	public NetworkStatus getNetworkStatus() {
		try {
			NetworkInfo net = connectivityManager.getActiveNetworkInfo();
			boolean connected = net != null && net.isConnected();
			boolean wifi = false, ipv6Only = false;
			if (connected) {
				wifi = net.getType() == TYPE_WIFI;
				if (SDK_INT >= 23) ipv6Only = isActiveNetworkIpv6Only();
				else ipv6Only = areAllAvailableNetworksIpv6Only();
			}
			CellularNetworkType cellularType = wifi || !connected
					? CellularNetworkType.NONE : getCellularNetworkType();
			return new NetworkStatus(connected, wifi, ipv6Only, cellularType);
		} catch (SecurityException e) {
			logException(LOG, WARNING, e);
			LOG.info("ConnectivityManager is broken, guessing connectivity");
			boolean connected = true, wifi = false, ipv6Only = true;
			WifiManager wm = (WifiManager) app.getSystemService(WIFI_SERVICE);
			if (wm != null) {
				WifiInfo info = wm.getConnectionInfo();
				if (info != null && info.getIpAddress() != 0) {
					LOG.info("Connected to wifi");
					wifi = true;
					ipv6Only = false;
				}
			}
			CellularNetworkType cellularType = wifi
					? CellularNetworkType.NONE : getCellularNetworkType();
			return new NetworkStatus(connected, wifi, ipv6Only, cellularType);
		}
	}

	private CellularNetworkType getCellularNetworkType() {
		try {
			if (SDK_INT >= 23) {
				Network active = connectivityManager.getActiveNetwork();
				if (active != null) {
					NetworkCapabilities capabilities =
							connectivityManager.getNetworkCapabilities(active);
					if (capabilities != null &&
							capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
						int type = telephonyManager.getDataNetworkType();
						return mapTelephonyType(type);
					}
				}
			}
			return mapTelephonyType(telephonyManager.getNetworkType());
		} catch (SecurityException e) {
			logException(LOG, WARNING, e);
			return CellularNetworkType.UNKNOWN;
		}
	}

	private CellularNetworkType mapTelephonyType(int type) {
		switch (type) {
			case TelephonyManager.NETWORK_TYPE_GPRS:
			case TelephonyManager.NETWORK_TYPE_EDGE:
			case TelephonyManager.NETWORK_TYPE_CDMA:
			case TelephonyManager.NETWORK_TYPE_1xRTT:
			case TelephonyManager.NETWORK_TYPE_IDEN:
				return CellularNetworkType.G2;
			case TelephonyManager.NETWORK_TYPE_UMTS:
			case TelephonyManager.NETWORK_TYPE_EVDO_0:
			case TelephonyManager.NETWORK_TYPE_EVDO_A:
			case TelephonyManager.NETWORK_TYPE_EVDO_B:
			case TelephonyManager.NETWORK_TYPE_HSDPA:
			case TelephonyManager.NETWORK_TYPE_HSUPA:
			case TelephonyManager.NETWORK_TYPE_HSPA:
			case TelephonyManager.NETWORK_TYPE_HSPAP:
			case TelephonyManager.NETWORK_TYPE_EHRPD:
				return CellularNetworkType.G3;
			case TelephonyManager.NETWORK_TYPE_LTE:
				return CellularNetworkType.LTE;
			case TelephonyManager.NETWORK_TYPE_NR:
				return CellularNetworkType.NR;
			default:
				return CellularNetworkType.UNKNOWN;
		}
	}

	@TargetApi(23)
	private boolean isActiveNetworkIpv6Only() {
		try {
			Network net = connectivityManager.getActiveNetwork();
			if (net == null) return false;
			LinkProperties props = connectivityManager.getLinkProperties(net);
			if (props == null) return false;
			boolean hasIpv6Unicast = false;
			for (LinkAddress linkAddress : props.getLinkAddresses()) {
				InetAddress addr = linkAddress.getAddress();
				if (addr instanceof Inet4Address) return false;
				if (!addr.isMulticastAddress()) hasIpv6Unicast = true;
			}
			return hasIpv6Unicast;
		} catch (SecurityException e) {
			logException(LOG, WARNING, e);
			return false;
		}
	}

	private boolean areAllAvailableNetworksIpv6Only() {
		try {
			Enumeration<NetworkInterface> interfaces = getNetworkInterfaces();
			if (interfaces == null) return false;
			boolean hasIpv6Unicast = false;
			for (NetworkInterface i : list(interfaces)) {
				if (i.isLoopback() || !i.isUp()) continue;
				for (InetAddress addr : list(i.getInetAddresses())) {
					if (addr instanceof Inet4Address) return false;
					if (!addr.isMulticastAddress()) hasIpv6Unicast = true;
				}
			}
			return hasIpv6Unicast;
		} catch (SocketException e) {
			logException(LOG, WARNING, e);
			return false;
		}
	}

	private void updateConnectionStatus() {
		eventBus.broadcast(new NetworkStatusEvent(getNetworkStatus()));
	}

	private void scheduleConnectionStatusUpdate(int delay, TimeUnit unit) {
		Cancellable newConnectivityCheck = scheduler.schedule(
				this::updateConnectionStatus, eventExecutor, delay, unit);
		Cancellable oldConnectivityCheck =
				connectivityCheck.getAndSet(newConnectivityCheck);
		if (oldConnectivityCheck != null) oldConnectivityCheck.cancel();
	}

	private class NetworkStateReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context ctx, Intent i) {
			String action = i.getAction();
			if (LOG.isLoggable(INFO)) LOG.info("Received broadcast " + action);
			updateConnectionStatus();
			if (isSleepOrDozeEvent(action)) {
				scheduleConnectionStatusUpdate(1, MINUTES);
			} else if (isApEvent(action)) {
				scheduleConnectionStatusUpdate(5, SECONDS);
			}
		}

		private boolean isSleepOrDozeEvent(@Nullable String action) {
			boolean isSleep = ACTION_SCREEN_ON.equals(action) ||
					ACTION_SCREEN_OFF.equals(action);
			boolean isDoze = SDK_INT >= 23 &&
					ACTION_DEVICE_IDLE_MODE_CHANGED.equals(action);
			return isSleep || isDoze;
		}

		private boolean isApEvent(@Nullable String action) {
			return WIFI_AP_STATE_CHANGED_ACTION.equals(action) ||
					WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action);
		}
	}
}
