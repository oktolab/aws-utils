package br.com.oktolab.aws;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.sqs.AmazonSQSAsyncClient;
import com.amazonaws.services.sqs.buffered.AmazonSQSBufferedAsyncClient;
import com.amazonaws.services.sqs.buffered.QueueBufferConfig;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;

public class SQSUtil {

	private static final Logger LOG = LoggerFactory.getLogger(SQSUtil.class);
	
	private static AmazonSQSBufferedAsyncClient sqs;
	
	static {
		QueueBufferConfig config = new QueueBufferConfig();
//			.withMaxBatchOpenMs(5000)
//			.withMaxDoneReceiveBatches(100);
		AmazonSQSAsyncClient client = new AmazonSQSAsyncClient();
		sqs = new AmazonSQSBufferedAsyncClient(client, config);
	}
	
	public static void sendMessage(String url, String body) {
		try {
			sqs.sendMessage(url, body);
		} catch (Exception e) {
			if (e instanceof AmazonClientException) {
				sqs = new AmazonSQSBufferedAsyncClient(
						new AmazonSQSAsyncClient());
				try { // retry
					sqs.sendMessage(url, body);
				} catch (Exception e2) {} // NOOP
			}
			LOG.warn("Erro ao tentar ENVIAR mensagens SQS.", e);
		}
	}
	
	public static void deleteMessage(String arn, String receiptHandler) {
		try {
			sqs.deleteMessage(arn, receiptHandler);
		} catch (Exception e) {
			if (e instanceof AmazonClientException) {
				sqs = new AmazonSQSBufferedAsyncClient(
						new AmazonSQSAsyncClient());
				try { // retry
					sqs.deleteMessage(arn, receiptHandler);
				} catch (Exception e2) {} // NOOP
			}
			LOG.warn("Erro ao tentar DELETAR mensagens SQS.", e);
		}
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
				sqs = new AmazonSQSBufferedAsyncClient(
						new AmazonSQSAsyncClient());
				try { // retry
					messages = sqs.receiveMessage(receiveMessageRequest).getMessages();
				} catch (Exception e2) {} // NOOP
			}
			LOG.warn("Erro ao tentar LER mensagens SQS.", e);
		}
		return messages != null ? messages : new ArrayList<Message>();
	}
}
