package com.rawchen.feishuchatdoc.util.chatgpt;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.rawchen.feishuchatdoc.chatdoc.dto.chat.ChatMessage;
import com.rawchen.feishuchatdoc.chatdoc.dto.chat.ChatRequest;
import com.rawchen.feishuchatdoc.chatdoc.util.ApiAuthUtil;
import com.rawchen.feishuchatdoc.config.Constants;
import com.rawchen.feishuchatdoc.entity.Status;
import com.rawchen.feishuchatdoc.entity.gpt.*;
import com.rawchen.feishuchatdoc.util.Task;
import com.rawchen.feishuchatdoc.util.TaskPool;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Semaphore;

@Data
@Slf4j
public class ChatService {

	private String account;
	private String cardText = "";
	private String secret;
	private volatile Status status;
	private Semaphore semaphore = new Semaphore(1);

	private StringBuilder errorString;

	public ChatService() {
		this.status = Status.FINISHED;
	}

	public ChatService(String account, String secret) {
		this.account = account;
		this.secret = secret;
		this.status = Status.FINISHED;
	}

	private void chat(String content, AnswerProcess process) throws InterruptedException {
		semaphore.acquire();
		try {

//			String createConversationUrl = proxyUrl + CHAT_URL;
//			UUID uuid = UUID.randomUUID();
//			String messageId = uuid.toString();

//			String param = CreateConversationBody.of(messageId, content, parentMessageId, conversationId, model);
			post(content, process);
		} finally {
			semaphore.release();
		}
	}

	/**
	 * 新建会话
	 *
	 * @param content 对话内容
	 * @param process 回调
	 * @throws InterruptedException
	 */
	public void newChat(String content, AnswerProcess process) throws InterruptedException {
		chat(content, process);
	}

	/**
	 * 向gpt发起请求
	 *
	 * @param content  文本
	 * @param process 响应处理器
	 */
	private void post(String content, AnswerProcess process) {

		String chatUrl = "wss://chatdoc.xfyun.cn/openapi/chat";
		String fileId  = "3f0ab50d97e04453968ff6550d5e4403";
		String question = content;
		String appId = Constants.CHAT_APP_ID;
		String secret = Constants.CHAT_APP_SECRET;

		ChatMessage message = new ChatMessage();
		message.setRole("user");
		message.setContent(question);
		// 请求内容
		ChatRequest request = ChatRequest.builder()
				.fileIds(Collections.singletonList(fileId))
				.topN(3)
				.messages(Collections.singletonList(message))
				.build();

		// 构造url鉴权
		long ts = System.currentTimeMillis() / 1000;
		String signature = ApiAuthUtil.getSignature(appId, secret, ts);
		String requestUrl = chatUrl + "?" + "appId=" + appId + "&timestamp=" + ts + "&signature=" + signature;
		// ws
		Request wsRequest = (new Request.Builder()).url(requestUrl).build();
		OkHttpClient okHttpClient = new OkHttpClient().newBuilder().build();
		StringBuffer buffer = new StringBuffer();
		WebSocket webSocket = okHttpClient.newWebSocket(wsRequest, new WebSocketListener() {
			@Override
			public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
				super.onClosed(webSocket, code, reason);
				webSocket.close(1002, "websocket finish");
				okHttpClient.connectionPool().evictAll();
			}

			@Override
			public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @Nullable Response response) {
				super.onFailure(webSocket, t, response);
				webSocket.close(1001, "websocket finish");
				okHttpClient.connectionPool().evictAll();
			}

			@Override
			public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
//				System.out.println(text);
				JSONObject jsonObject = JSONUtil.parseObj(text);
				String content = jsonObject.getStr("content");
				String sid = jsonObject.getStr("sid");
				Integer status = jsonObject.getInt("status");
				if (status == 1 || status == 2) {
					try {
						cardText += content;
						Answer answer = new Answer();
						answer.setContent(cardText);
						answer.setSid(sid);
						answer.setStatus(status);
						TaskPool.addTask(new Task(process, answer, account));
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
					buffer.append(content);
				}
				if (Objects.equals(jsonObject.getInt("status"), 2)) {
					System.out.println("回答内容：" + buffer);
					webSocket.close(1000, "websocket finish");
					okHttpClient.connectionPool().evictAll();
				}
			}
		});
		webSocket.send(JSONUtil.toJsonStr(request));
	}
}
