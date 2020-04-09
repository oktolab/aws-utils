package br.com.oktolab.aws;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.services.sqs.AmazonSQSAsyncClient;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import com.amazonaws.services.sqs.buffered.AmazonSQSBufferedAsyncClient;
import com.amazonaws.services.sqs.model.DeleteMessageResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;

public class SQSUtil {
	
	private static final Logger LOG = LoggerFactory.getLogger(SQSUtil.class);
	
	private static AmazonSQSBufferedAsyncClient sqs;

	private static int maxConnections = 300;
	
	static {
		sqs = createSqsClient();
	}
	
	public void setMaxConnections(int maxConnections) {
		SQSUtil.maxConnections = maxConnections;
	}

	public static AmazonSQSBufferedAsyncClient createSqsClient() {
		ClientConfiguration config = new ClientConfiguration();
		config.setMaxConnections(maxConnections);
		AmazonSQSAsyncClientBuilder builder = AmazonSQSAsyncClient.asyncBuilder();
		builder.setClientConfiguration(config);
		return new AmazonSQSBufferedAsyncClient(builder.build());
	}
	
	public static SendMessageResult sendMessageFIFO(String url, String groupId, String body) {
		try {
			SendMessageRequest request = new SendMessageRequest(url, body);
			request.setMessageGroupId(groupId);
			return sqs.sendMessage(request);
		} catch (Exception e) {
			if (e instanceof AmazonClientException) {
				sqs = createSqsClient();
				try { // retry
					return sqs.sendMessage(url, body);
				} catch (Exception e2) {} // NOOP
			}
			LOG.warn(String.format("Erro ao tentar ENVIAR mensagens SQS. %s", url), e);
		}
		return null;
	}
	
	public static SendMessageResult sendMessage(String url, String body) {
		try {
			return sqs.sendMessage(url, body);
		} catch (Exception e) {
			if (e instanceof AmazonClientException) {
				sqs = createSqsClient();
				try { // retry
					return sqs.sendMessage(url, body);
				} catch (Exception e2) {} // NOOP
			}
			LOG.warn(String.format("Erro ao tentar ENVIAR mensagens SQS. %s", url), e);
		}
		return null;
	}
	
	public static DeleteMessageResult deleteMessage(String arn, String receiptHandler) {
		try {
			return sqs.deleteMessage(arn, receiptHandler);
		} catch (Exception e) {
			if (e instanceof AmazonClientException) {
				sqs = createSqsClient();
				try { // retry
					return sqs.deleteMessage(arn, receiptHandler);
				} catch (Exception e2) {} // NOOP
			}
			LOG.warn(String.format("Erro ao tentar DELETAR mensagens SQS. %s", arn), e);
		}
		return null;
	}
	
	public static List<Message> getMessages(String arn) {
		return getMessages(arn, 10);
	}
	
	public static List<Message> getMessages(String arn, int numberOfMessages) {
		ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(arn);
		receiveMessageRequest.setMaxNumberOfMessages(numberOfMessages);
		List<Message> messages = null;
		try {
			messages = sqs.receiveMessage(receiveMessageRequest).getMessages();
			return messages;
		} catch (Exception e) {
			if (e instanceof AmazonClientException) {
				sqs = createSqsClient();
				try { // retry
					messages = sqs.receiveMessage(receiveMessageRequest).getMessages();
				} catch (Exception e2) {} // NOOP
			}
			LOG.warn(String.format("Erro ao tentar LER mensagens SQS. %s", arn), e);
		}
		return messages != null ? messages : new ArrayList<Message>();
	}
}
