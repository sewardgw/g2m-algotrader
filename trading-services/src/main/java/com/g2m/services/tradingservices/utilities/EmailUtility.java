package com.g2m.services.tradingservices.utilities;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.springframework.stereotype.Component;

import com.google.api.services.gmail.Gmail;

@Component
public class EmailUtility {

	final String username = "g2malgos@gmail.com";
	final String password = "nlnyybcrlwptwsyy";
	final String host = "smtp.gmail.com";
	Properties props;

	public EmailUtility() {
		
		props = new Properties();
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", host);
		props.put("mail.smtp.user", username);
		props.put("mail.smtp.password", password);
		props.put("mail.smtp.port", "587");
		//props.put("mail.smtp.auth", false);
	}

	public void send(String subjectText, String messageText){

		//Properties props = new Properties();
		//Session session = Session.getDefaultInstance(props, null);


				Session session = Session.getInstance(props, new javax.mail.Authenticator() {
					protected PasswordAuthentication getPasswordAuthentication() {
						return new PasswordAuthentication(username, password);
					}
				  });

		try {

			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress("g2malgos@gmail.com"));
			message.setRecipients(Message.RecipientType.TO,
					InternetAddress.parse("g2malgos@gmail.com"));
			message.setSubject(subjectText);
			message.setText(messageText);

			Transport transport = session.getTransport("smtp");
			
			transport.connect(host, username, password);
			transport.sendMessage(message, message.getAllRecipients());

		} catch (MessagingException e) {
			throw new RuntimeException(e);
		}
	}

}
