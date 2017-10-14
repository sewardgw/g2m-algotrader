package com.g2m.services.tradingservices.persistence;

import com.g2m.services.shared.persistthreads.EntityPersistThread;
import com.g2m.services.tradingservices.entities.Position;
import com.google.api.client.util.Lists;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created on 12/15/2015.
 */
@Component
public class SlackMessageThread extends EntityPersistThread<String> {
  private static String SLACK_WEBHOOK_URL = null; // 
  private String strategyName = "Strategy";
  private String channel = "#strategies";
  private String icon = ":dollar:";

  public static void setSlackWebhookUrl(String s){
	  SLACK_WEBHOOK_URL = s;
  }
  
  public void sendMessage(String message) {
    sendSlackMessage(message);
  }

  public void sendMessageList(List<String> messages) {
    saveItems(messages);
  }

  public void sendPositionList(List<Position> items) {
    List<String> messageList = Lists.newArrayList();
    for (Position position : items) {
      messageList.add(positionToMessage(position));
    }
    sendMessageList(messageList);
  }

  private String positionToMessage(Position position) {
    StringBuilder builder = new StringBuilder();
    builder.append(position.getSecurity().getSymbol()).append("\n");
    builder.append("Quantity: ").append(position.getQuantity()).append("\n");
    builder.append("Currency: ").append(position.getSecurity().getCurrency()).append("\n");
    builder.append("Open price: ").append(position.getOpenPrice()).append("\n");
    builder.append("Last price: ").append(position.getLastPrice()).append("\n");
    return builder.toString();
  }

  @Override
  protected void saveItems(List<String> items) {
    String combinedMessage = "";
    for (String message : items) {
      if (combinedMessage.length() > 0) {
        combinedMessage += "\n";
      }
      combinedMessage += message;
    }
    sendSlackMessage(combinedMessage);
  }

  private String createSlackPayload(String message) {
    return "{\"channel\": \"" + getChannel() + "\", \"username\": \"" + getStrategyName() + "\", \"text\": \"" +
        message + "\", \"icon_emoji\": \"" + getIcon() + "\"}";
  }

  // TODO is there any messages per second throttling going on with Slack API calls?
  private void sendSlackMessage(String message) {
	  System.out.println(isRunning());
    if (null == message || 0 == message.length() || !isRunning()
    		|| SLACK_WEBHOOK_URL == null) {
      return;
    }

    try {
      HttpClient httpClient = HttpClients.createDefault();
      HttpPost httpPost = new HttpPost(SLACK_WEBHOOK_URL);

      // request parameters and other properties
      List<NameValuePair> params = new ArrayList<NameValuePair>(2);
      params.add(new BasicNameValuePair("payload", createSlackPayload(message)));
      httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

      // execute and get the response
      HttpResponse response = httpClient.execute(httpPost);
      System.out.println("----- SENT MESSAGE TO SLACK ----");
    } catch (Exception e) {
      // TODO add proper logging
      e.printStackTrace();
    }
  }

  public String getStrategyName() {
    return strategyName;
  }

  public void setStrategyName(String strategyName) {
    this.strategyName = strategyName;
  }

  public String getChannel() {
    return channel;
  }

  public void setChannel(String channel) {
    this.channel = channel;
  }

  public String getIcon() {
    return icon;
  }

  public void setIcon(String icon) {
    this.icon = icon;
  }
}

