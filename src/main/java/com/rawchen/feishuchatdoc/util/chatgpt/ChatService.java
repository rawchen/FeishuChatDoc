package com.rawchen.feishuchatdoc.util.chatgpt;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.rawchen.feishuchatdoc.chatdoc.dto.chat.ChatMessage;
import com.rawchen.feishuchatdoc.chatdoc.dto.chat.ChatRequest;
import com.rawchen.feishuchatdoc.chatdoc.util.ApiAuthUtil;
import com.rawchen.feishuchatdoc.chatdoc.util.ChatDocUtil;
import com.rawchen.feishuchatdoc.entity.Status;
import com.rawchen.feishuchatdoc.entity.gpt.*;
import com.rawchen.feishuchatdoc.entity.gptRequestBody.CreateConversationBody;
import com.rawchen.feishuchatdoc.util.Task;
import com.rawchen.feishuchatdoc.util.TaskPool;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Semaphore;

@Data
@Slf4j
public class ChatService {

	private String account;
	private String password;
	private String accessToken;
	private volatile Status status;
	private Semaphore semaphore = new Semaphore(1);

	private StringBuilder errorString;

	public ChatService() {
		this.status = Status.FINISHED;
	}

	public ChatService(String account, String password, String accessToken) {
		this.accessToken = accessToken;
		this.account = account;
		this.password = password;
		this.status = Status.FINISHED;
	}

	public String getToken() {
		return "Bearer " + accessToken;
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
	 * @param urlStr  请求的地址
	 * @param process 响应处理器
	 */
	private void post(String content, AnswerProcess process) {

		String chatUrl = "wss://chatdoc.xfyun.cn/openapi/chat";
		String fileId  = "3f0ab50d97e04453968ff6550d5e4403";
		String question = content;
		String appId = "f20ff234";
		String secret = "NmY3ODE2ZmE4YjAxZjUzYzkwNTQ3MDcw";

		URL url = null;
		Answer answer = null;


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
				System.out.println(text);
				cn.hutool.json.JSONObject jsonObject = JSONUtil.parseObj(text);
				String content = jsonObject.getStr("content");
				if (StrUtil.isNotEmpty(content)) {
					//异步处理
					Answer new
					TaskPool.addTask(new Task(process, answer, this.account));

					buffer.append(content);
				}
				if (Objects.equals(jsonObject.getInt("status"), 2)) {
					System.out.println("回答内容：" + buffer);
					webSocket.close(1000, "websocket finish");
					okHttpClient.connectionPool().evictAll();
//					System.exit(0);
				}
			}
		});
		webSocket.send(JSONUtil.toJsonStr(request));




		url = new URL(urlStr);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Accept", "application/json");
		connection.setRequestProperty("Authorization", getToken());
		connection.setRequestProperty("Content-Type", "application/json");
		//设置请求体
		connection.setDoOutput(true);

		try (OutputStream output = connection.getOutputStream()) {
			output.write(param.getBytes(StandardCharsets.UTF_8));
		}

		// 获取并处理响应
		int status = connection.getResponseCode();
		Reader streamReader = null;
		boolean error = false;
		errorString = new StringBuilder();
		if (status > 299) {
			streamReader = new InputStreamReader(connection.getErrorStream());
			log.error("请求失败，状态码：{}", status);
			error = true;
		} else {
			streamReader = new InputStreamReader(connection.getInputStream());
		}

		BufferedReader reader = new BufferedReader(streamReader);
		String line;

		int count = 0;
		while ((line = reader.readLine()) != null) {
			if (line.length() == 0) {
				continue;
			}
			if (error) {
				errorString.append(line);
				log.error(line);

				continue;
			}

			try {
				count++;
				answer = parse(line);

				if (answer == null) {
					continue;
				}

				answer.setSeq(count);
				//每10行 才处理一次 为了防止飞书发消息太快被限制频率
				if (answer.isSuccess() && !answer.isFinished() && count % 10 != 0) {
					continue;
				}

				if (answer.isSuccess() && !answer.getMessage().getAuthor().getRole().equals("assistant")) {
					continue;
				}


			} catch (Exception e) {
				log.error("解析ChatGpt响应出错", e);
				log.error(line);
			}
		}

		if (error) {
			answer = new Answer();
			answer.setError(errorString.toString());
			answer.setErrorCode(ErrorCode.RESPONSE_ERROR);
			answer.setSuccess(false);

			try {
				JSONObject jsonObject = new JSONObject(errorString.toString());
				String detail = jsonObject.optString("detail");
				if (detail != null) {
					JSONObject detailObject = new JSONObject(detail);
					String code = detailObject.optString("code");
					if (code.equals("account_deactivated")) {
						answer.setErrorCode(ErrorCode.ACCOUNT_DEACTIVATED);
					}
				}


			} catch (JSONException ignored) {
			}
			TaskPool.addTask(new Task(process, answer, this.account));
		}

		reader.close();
		connection.disconnect();

	}


	private Answer parse(String body) {

		Answer answer;

		if (body.startsWith("data: [DONE]") || body.startsWith("data: {\"conversation_id\"")) {
			return null;
		}
		if (body.startsWith("data:")) {

			body = body.substring(body.indexOf("{"));
			answer = JSONUtil.toBean(body, Answer.class);
			answer.setSuccess(true);
			if (answer.getMessage().getStatus().equals("finished_successfully")) {
				answer.setFinished(true);
			}
			Message message = answer.getMessage();
			Content content = message.getContent();
			List<String> parts = content.getParts();
			if (parts != null) {
				String part = parts.get(0);
				answer.setAnswer(part);
			}
			if (content.getText() != null) {
				answer.setAnswer(content.getText());
			}

		} else {
			answer = new Answer();
			answer.setSuccess(false);
			JSONObject jsonObject = new JSONObject(body);
			String detail = jsonObject.optString("detail");
			if (detail != null && detail.contains("Only one message")) {
				log.warn("账号{}忙碌中", account);
				answer.setErrorCode(ErrorCode.BUSY);
				answer.setError(detail);
				return answer;
			}
			if (detail != null && detail.contains("code")) {
				JSONObject error = jsonObject.optJSONObject("detail");
				String code = (String) error.opt("code");
				if (code.equals("invalid_jwt")) {
					answer.setErrorCode(ErrorCode.INVALID_JWT);
				} else if (code.equals("invalid_api_key")) {
					answer.setErrorCode(ErrorCode.INVALID_API_KEY);
				} else if (code.equals("model_cap_exceeded")) {
					answer.setErrorCode(ErrorCode.CHAT_LIMIT);
				} else {
					log.error(body);
					log.warn("账号{} token失效", account);
				}
				answer.setError(error.get("message"));
				return answer;
			}
			log.error("未知错误：{}", body);
			log.error("账号{}未知错误：{}", account, body);
			answer.setError(body);
		}
		return answer;
	}

	/**
	 * 查询账号可用模型从而判断账号是否plus用户
	 *
	 * @return 查询是否成功 不成功的原因一般为token失效
	 */
	public boolean queryAccountLevel() {
		String url = proxyUrl + ACCOUNT_LEVEL_URL;
		HttpResponse response = HttpRequest.get(url).header("Authorization", getToken()).execute();
		String body = response.body();
		JSONObject jsonObject = new JSONObject(body);
		String models = jsonObject.optString("models");
		if (models == null || models.length() == 0) {
			log.warn("账号{}查询模型解析失败 : {}", account, body);
			return false;
		}
		JSONArray objects = JSONUtil.parseArray(models);
		List<Model> list = JSONUtil.toList(objects, Model.class);
		boolean plus = false;
		for (Model model : list) {
			Models.modelMap.put(model.getTitle(), model);
			if (model.getSlug().startsWith("gpt-4")) {
				Models.plusModelTitle.add(model.getTitle());
				plus = true;
			} else {
				Models.normalModelTitle.add(model.getTitle());
				Models.NORMAL_DEFAULT_MODEL = model.getTitle();
			}
		}
		this.setLevel(plus ? 4 : 3);
		return true;
	}
}
