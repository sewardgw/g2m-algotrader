package com.g2m.services.tradingservices.mains;

import java.util.concurrent.Executors;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

/**
 * @author Michael Borromeo
 */
public class Main3 {
	public static void main(String[] args) {
		EventBus bus = new AsyncEventBus(Executors.newCachedThreadPool());
		bus.register(new TestListener());
		(new TestProducer("first", bus)).start();
		(new TestProducer("second", bus)).start();
	}

	static class TestProducer extends Thread {
		EventBus bus;
		String src;

		public TestProducer(String src, EventBus bus) {
			this.src = src;
			this.bus = bus;
		}

		@Override
		public void run() {
			for (int i = 0; i < 10; i++) {
				bus.post(new TestEvent(src, i));
			}
		}
	}

	static class TestListener {
		@Subscribe
		public void onEvent(TestEvent event) {
			if (event.getValue() == 5) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			System.out.println(event.getSrc() + ": " + event.getValue());
		}
	}

	static class TestEvent {
		String src;
		int value;

		public TestEvent(String src, int value) {
			this.src = src;
			this.value = value;
		}

		public String getSrc() {
			return src;
		}

		public int getValue() {
			return value;
		}
	}
}
