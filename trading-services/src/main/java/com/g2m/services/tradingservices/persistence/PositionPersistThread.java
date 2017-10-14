package com.g2m.services.tradingservices.persistence;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.api.client.util.Lists;
import javafx.geometry.Pos;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.g2m.services.shared.persistthreads.EntityPersistThread;
import com.g2m.services.tradingservices.entities.Position;
import com.g2m.services.tradingservices.entities.orders.Order;
import com.g2m.services.tradingservices.entities.orders.OrderStates;

/**
 * Added 5/31/2015.
 *
 * @author Michael Borromeo
 */
@Component
public class PositionPersistThread extends EntityPersistThread<Position> {
  @Autowired
  private PositionRepository positionRepository;

  @Autowired
  private SlackMessageThread slackMessageThread;

  private boolean sendSlackMessages = true;

  public boolean isSendSlackMessages() {
    return sendSlackMessages;
  }

  public void setSendSlackMessages(boolean sendSlackMessages) {
    this.sendSlackMessages = sendSlackMessages;
  }

  @Override
  protected void saveItems(List<Position> items) {
    if (sendSlackMessages) {
      slackMessageThread.sendPositionList(items);
    }
    positionRepository.save(items);
  }
}
