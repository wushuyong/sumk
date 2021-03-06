package org.yx.rpc.client.route;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.yx.log.Log;
import org.yx.rpc.Host;

public class HostChecker {

	private static HostChecker holder = new HostChecker();
	private static long maxDownTime = 1000 * 120;

	private HostChecker() {
		new Thread(new checker(), "socket_check").start();
	}

	public static HostChecker instance() {
		return holder;
	}

	private ConcurrentHashMap<Host, Long> downUrls = new ConcurrentHashMap<Host, Long>();

	/**
	 * 判断该url是否可用，可用返回true，不可用返回false
	 * 
	 * @return
	 */
	public boolean isDowned(Host url) {
		return downUrls.containsKey(url);
	}

	/**
	 * 将列表中不可用的url过滤掉
	 * 
	 * @param urls
	 * @return
	 */
	public List<Host> available(List<Host> urls) {
		List<Host> us = new ArrayList<Host>(urls);
		List<Host> avas = new ArrayList<Host>(us.size());
		for (Host u : us) {
			if (!downUrls.containsKey(u)) {
				avas.add(u);
			}
		}
		return avas;
	}

	public void addDownUrl(Host url) {
		downUrls.putIfAbsent(url, System.currentTimeMillis());
	}

	private class checker implements Runnable {

		@Override
		public void run() {
			while (true) {
				try {
					check();
					long t = 5000 - (2000 * downUrls.size());
					if (t > 0) {
						Thread.sleep(t);
					}
				} catch (Throwable e) {
				}
			}

		}

		private int getTimeOut(int urlSize) {
			if (urlSize == 1) {
				return 3000;
			} else if (urlSize == 2) {
				return 2500;
			} else if (urlSize > 5) {
				return 1000;
			}
			return 2000;
		}

		private void check() {
			if (downUrls.isEmpty()) {
				return;
			}
			Host[] urls = downUrls.keySet().toArray(new Host[0]);
			int timeout = getTimeOut(urls.length);
			for (Host url : urls) {
				try {
					long t = downUrls.get(url);
					if (System.currentTimeMillis() - t >= maxDownTime) {
						downUrls.remove(url);
						continue;
					}
					Socket socket = new Socket();

					socket.connect(new InetSocketAddress(url.getIp(), url.getPort()), timeout);
					if (socket.isConnected()) {
						socket.close();
						downUrls.remove(url);
					}
				} catch (UnknownHostException e) {
					Log.get("SYS.1").error(e.getMessage(), e);
				} catch (Exception e) {
				}
			}

		}

	}

}
