package com.g2m.services.shared.persistthreads;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;

/**
 * Added 4/23/2015.
 *
 * @author Michael Borromeo
 */
public abstract class EntityPersistThread<T> extends Thread {
  final static Logger LOGGER = Logger.getLogger(EntityPersistThread.class);
  private BlockingQueue<T> queue;
  private boolean running;

  public EntityPersistThread() {
    super();
    queue = new LinkedBlockingQueue<T>();
    running = false;
  }

  public void persist(T t) {
    if (running) {
      queue.add(t);
    }
  }

  public void persist(List<T> t) {
    if (running) {
      queue.addAll(t);
    }
  }

  public boolean isRunning() {
    return running;
  }

  @Override
  public void run() {
    running = true;
    while (running) {
      saveItemsInQueue(true);
    }
  }

  private void saveItemsInQueue(boolean blocking) {
    List<T> allItems = new LinkedList<T>();
    try {
      if (blocking) {
        allItems.add(queue.take());
      }
      queue.drainTo(allItems);
      if (0 < allItems.size()) {
        saveItems(allItems);
      }
    } catch (InterruptedException e) {
      LOGGER.debug("saveItemsInQueue() interrupted");
    } catch (Exception e) {
      LOGGER.debug("saveItemsInQueue() error: " + e.getMessage());
      running = false;
    }
  }

  public void stopRunning() {
    running = false;
    interrupt();
    saveItemsInQueue(false);
  }

  protected abstract void saveItems(List<T> items);
}
