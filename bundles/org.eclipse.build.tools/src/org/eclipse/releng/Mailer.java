package org.eclipse.releng;

/**
 * A Class that sends build related email messages.  host, sender, recipient and
 * build related information set in monitor.properties
 */

import javax.mail.*;
import javax.mail.internet.*;
import java.util.StringTokenizer;
import javax.activation.*;
import java.util.Properties;

public class Mailer {

	// flag that determines whether or not to send mail
	boolean sendMail = true;

	// the Object that holds the key value pairs in monitor.properties
	private BuildProperties buildProperties;

	//convert the comma separated list of email addressed into an array of Address objects
	private Address[] getAddresses() {
		int i = 0;
		StringTokenizer recipients = new StringTokenizer(buildProperties.getRecipientList(), ",");
		Address[] addresses = new Address[recipients.countTokens()];

		while (recipients.hasMoreTokens()) {
			try {
				addresses[i++] = new InternetAddress(recipients.nextToken());
			} catch (AddressException e) {
				System.out.println("Unable to create address");
			}
		}
		return addresses;
	}

	public Mailer(){
		buildProperties = new BuildProperties();
		if (buildProperties.getHost().equals("")||buildProperties.getSender().equals("")||buildProperties.getRecipientList().equals("")){
			sendMail=false;
		}
	}


	public static void main(String args[]) {
		Mailer myMailer = new Mailer();
	}

	public void sendMessage(String aSubject, String aMessage) {
		if (aSubject == null || aMessage == null || sendMail == false){
			printEmailFailureNotice(aSubject,aMessage);
		}

		// Get system properties
		Properties props = System.getProperties();

		// Setup mail server
		props.put("mail.smtp.host", buildProperties.getHost());

		// Get session
		Session session = Session.getDefaultInstance(props, null);

		// Define message
		MimeMessage message = new MimeMessage(session);

		try {
			// Set the from address
			message.setFrom(new InternetAddress(buildProperties.getSender()));

			// Set the to address
			message.addRecipients(Message.RecipientType.TO, getAddresses());

			// Set the subject
			message.setSubject(buildProperties.getBuildSubjectPrefix()+
						"Build "
							+ buildProperties.getBuildid()
							+ " (Timestamp:  "
							+ buildProperties.getTimestamp()
							+ "):"
							+ aSubject);

			// Set the content
			message.setText(
				"Build "
					+ buildProperties.getBuildid()
					+ " (Timestamp: "
					+ buildProperties.getTimestamp()
					+ "):  "
					+ aMessage);

			// Send message
			Transport.send(message);

		} catch (AddressException e) {
			e.printStackTrace();
		} catch (MessagingException e) {
			e.printStackTrace();
		}
	}

	public void sendMultiPartMessage(
	// a method for sending mail with attachments
	String aSubject, String aMessage, String[] attachments) {
		if (aSubject == null || aMessage == null || sendMail == false){
			printEmailFailureNotice(aSubject,aMessage);
		}

		// Get system properties
		Properties props = System.getProperties();

		// Setup mail server
		props.put("mail.smtp.host", buildProperties.getHost());

		// Get session
		Session session = Session.getDefaultInstance(props, null);

		// Define message
		MimeMessage message = new MimeMessage(session);

		Multipart mp = new MimeMultipart();

		try {
			// Set the from address
			message.setFrom(new InternetAddress(buildProperties.getSender()));

			// Set the to address
			message.addRecipients(Message.RecipientType.TO, getAddresses());

			// Set the subject
			message.setSubject(buildProperties.getBuildSubjectPrefix()+
			"Build "
				+ buildProperties.getBuildid()
				+ " (Timestamp:  "
				+ buildProperties.getTimestamp()
				+ "):"
				+ aSubject);

			// create and fill the first message part 
			MimeBodyPart part = new MimeBodyPart();
			part.setText(
				"Build "
					+ buildProperties.getBuildid()
					+ " (Timestamp: "
					+ buildProperties.getTimestamp()
					+ "):  "
					+ aMessage);
			mp.addBodyPart(part);

			//for each attachment create new message part
			for (int i = 0; i < attachments.length; i++) {
				MimeBodyPart attachmentPart = new MimeBodyPart();
				// attach the file to the message 
				FileDataSource attachment = new FileDataSource(attachments[i]);
				attachmentPart.setDataHandler(new DataHandler(attachment));
				attachmentPart.setFileName(attachments[i]);
				mp.addBodyPart(attachmentPart);
			}

			// add the Multipart to the message 
			message.setContent(mp);

			Transport.send(message);

		} catch (AddressException e) {
		} catch (MessagingException e) {
		}
	}

	private void printEmailFailureNotice(String aSubject, String aMessage){
		System.out.println("Email failed.  Settings:");
		System.out.println("\nhost="+buildProperties.getHost()+"\nsender="+buildProperties.getSender()+"\nrecipients="+buildProperties.getRecipientList());
		System.out.println("\nSubject="+aSubject+"\nMessage="+aMessage);
		return;
	}

}
