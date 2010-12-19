package com.butterfly;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Configration {

	public final static List<MagicBit> HTTPMAGIC = new ArrayList<MagicBit>();
	public final static List<MagicBit> REMOTEMAGIC = new ArrayList<MagicBit>();
	static {
		HTTPMAGIC.add(new MagicBit('G', 'E')); // GET
		HTTPMAGIC.add(new MagicBit('P', 'O')); // GOST
		HTTPMAGIC.add(new MagicBit('P', 'U')); // PUT
		HTTPMAGIC.add(new MagicBit('H', 'E')); // HEAD
		HTTPMAGIC.add(new MagicBit('O', 'P')); // OPTIONS
		HTTPMAGIC.add(new MagicBit('P', 'A')); // PATCH
		HTTPMAGIC.add(new MagicBit('D', 'E')); // DELETE
		HTTPMAGIC.add(new MagicBit('T', 'R')); // TRACE
		HTTPMAGIC.add(new MagicBit('C', 'O')); // CONNECT

		REMOTEMAGIC.add(new MagicBit(3, 0)); // 3, 0 is windows remote magic
	}

	private static final Configration INSTANCE = new Configration();
	private List<ProxyItem> items = new CopyOnWriteArrayList<ProxyItem>();

	public void addProxyItem(ProxyItem item) {
		items.add(item);
	}

	public boolean removeProxyItem(final String host, final int port) {
		Iterator<ProxyItem> iterator = items.iterator();
		while (iterator.hasNext()) {
			ProxyItem item = iterator.next();
			if (item.equals(host, port) && item.canDelete) {
				iterator.remove();
				return true;
			}
		}
		return false;
	}

	public static Configration getInstance() {
		return INSTANCE;
	}

	public ProxyItem match(final int magic1, final int magic2) {
		for (ProxyItem item : items) {
			if (item.match(magic1, magic2)) {
				return item;
			}
		}
		return null;
	}

	@Override
	public String toString() {

		return dump();
	}

	public String dump() {
		StringBuilder sb = new StringBuilder();

		for (ProxyItem item : items) {
			sb.append(item.toString()).append("\n");
		}
		return sb.toString();
	}

	public Configration() {
		items.add(new ProxyItem("localhost", 8080, HTTPMAGIC));
		items.add(new ProxyItem("localhost", 3389, REMOTEMAGIC));
	}

	public static class MagicBit {
		private final int magic1;
		private final int magic2;

		public static MagicBit valueOf(String args) {
			String[] split = args.split(" ");
			return new MagicBit(Integer.valueOf(split[0]),
					Integer.valueOf(split[1]));
		}

		public MagicBit(int magic1, int magic2) {
			this.magic1 = magic1;
			this.magic2 = magic2;
		}

		@Override
		public String toString() {
			return "[" + magic1 + " " + magic2 + "]";
		}

		public boolean equals(final int m1, final int m2) {
			return m1 == magic1 && m2 == magic2;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj instanceof MagicBit) {
				MagicBit another = (MagicBit) obj;
				if (another.magic1 == magic1 && another.magic2 == magic2) {
					return true;
				} else {
					return false;
				}
			}
			return false;
		}
	}

	public static class ProxyItem {

		public boolean equals(final String host, final int port) {
			return host.equals(this.host) && port == this.port;
		}

		@Override
		public String toString() {
			return host + ":" + port + " >> " + mMagic;
		}

		private volatile boolean canDelete = true;
		public final String host;
		public final int port;
		private volatile List<MagicBit> mMagic;

		public ProxyItem(String host, int port, List<MagicBit> magics) {
			this.host = host;
			this.port = port;
			mMagic = magics;
		}

		public boolean match(final int magic1, final int magic2) {

			if (mMagic != null) {
				for (MagicBit m : mMagic) {
					if (m.equals(magic1, magic2)) {
						return true;
					}
				}
			}
			return false;
		}

		public boolean isCanDelete() {
			return canDelete;
		}

		public void markAsCannotDelete() {
			canDelete = false;
		}
	}
}
