package com.rawchen.feishuchatdoc.handler;

import com.lark.oapi.Client;
import com.lark.oapi.service.contact.v3.model.User;
import com.lark.oapi.service.im.v1.model.*;
import com.rawchen.feishuchatdoc.entity.Conversation;
import com.rawchen.feishuchatdoc.entity.Status;
import com.rawchen.feishuchatdoc.entity.gpt.Answer;
import com.rawchen.feishuchatdoc.entity.gpt.ErrorCode;
import com.rawchen.feishuchatdoc.entity.gpt.Models;
import com.rawchen.feishuchatdoc.service.MessageService;
import com.rawchen.feishuchatdoc.service.UserService;
import com.rawchen.feishuchatdoc.util.chatgpt.ChatService;
import com.rawchen.feishuchatdoc.util.chatgpt.ConversationPool;
import com.rawchen.feishuchatdoc.util.chatgpt.RequestIdSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@RequiredArgsConstructor
public class MessageHandler {


	protected final Client client;
	protected final UserService userService;
	protected final MessageService messageService;
	protected final ConversationPool conversationPool;

	/**
	 * 飞书推送事件会重复，用于去重
	 *
	 * @param event
	 * @return
	 */
	private boolean checkInvalidEvent(P2MessageReceiveV1 event) {
		String requestId = event.getRequestId();
		// 根据内存记录的消息事件id去重
		if (RequestIdSet.requestIdSet.contains(requestId)) {
			//log.warn("重复请求，requestId:{}", requestId);
			return false;
		}
		RequestIdSet.requestIdSet.add(requestId);

		String createTime = event.getEvent().getMessage().getCreateTime();
		Long createTimeLong = Long.valueOf(createTime);
		Long now = System.currentTimeMillis();
		if (now - createTimeLong > 1000 * 10) {
			// 根据消息事件的创建时间去重（如果现在的时间比创建时间大10秒就说明是重复的，正常服务器时间同步正确应该只有1s的差别）
			//log.warn("消息过期，requestId:{}", requestId);
			return false;
		}

		P2MessageReceiveV1Data messageEvent = event.getEvent();
		EventMessage message = messageEvent.getMessage();

		String chatType = message.getChatType();
		String msgType = message.getMessageType();

		log.info("对话类型: {}，消息类型: {}", chatType, msgType);


		// 只处理私聊文本消息
//    if (!chatType.equals("p2p") || !msgType.equals("text")) {
//      log.warn("不支持的ChatType或MsgType,ChatGpt不处理");
//      return false;
//    }

		// 不是group群组@机器人的消息和机器人私聊消息，或者不是文本就不处理
		if ((!chatType.equals("group") && !chatType.equals("p2p")) || !msgType.equals("text")) {
			log.warn("不支持的ChatType或MsgType,ChatGpt不处理");
			return false;
		}
		return true;
	}


	public void process(P2MessageReceiveV1 event) throws Exception {
		P2MessageReceiveV1Data messageEvent = event.getEvent();
		EventMessage message = messageEvent.getMessage();
		// 如果是重复消息不处理
		if (!checkInvalidEvent(event)) {
			return;
		}
		JSONObject jsonObject = new JSONObject(message.getContent());
		String text = jsonObject.optString("text");

		// 去掉事件获取开头的"@_user_1"
		if (text.startsWith("@_user_1")) {
			text = text.substring(9);
		}

		try {
			User user = userService.getUserByOpenId(event.getEvent().getSender().getSenderId().getOpenId());
			String name = user.getName();
			log.info("{}: {}", name, text);
		} catch (Exception e) {
			log.error("获取用户信息失败", e);
		}

		if (text == null || text.equals("")) {
			return;
		}
		String chatId = message.getChatId();
		//尝试从会话池中获取会话，从而保持上下文
		Conversation conversation = conversationPool.getConversation(chatId);

		ChatService chatService = null;

		String title = "当前文档数量: " + 2;
		String firstText = "正在生成中，请稍后...";

		// 首次发送的消息卡片
		CreateMessageResp resp = messageService.sendGptAnswerMessage(chatId, title, firstText);
		String messageId = resp.getData().getMessageId();

//    Map<String, String> selections = createSelection(conversation);

//		String modelParam = Models.modelMap.get(conversation.getModel()).getSlug();


//		log.info("新建会话");
		conversation.setTitle(title);
		String finalTitle = title;
		ChatService finalChatService = chatService;
		chatService.newChat(text, answer -> {
			processAnswer(answer, finalTitle, chatId, finalChatService, messageId, event);
		});


		log.info("服务完成: {}|{}|{}", chatService.getAccount(), chatId, messageId);
	}

	private void processAnswer(Answer answer, String title, String chatId, ChatService chatService, String messageId, P2MessageReceiveV1 event) throws Exception {
		if (!answer.isSuccess()) {
			// gpt请求失败
			if (answer.getErrorCode() == ErrorCode.INVALID_JWT || answer.getErrorCode() == ErrorCode.INVALID_API_KEY) {

				messageService.modifyGptAnswerMessageCard(messageId, title, (String) answer.getError());
				RequestIdSet.requestIdSet.remove(event.getRequestId());
				process(event);
				return;
			} else if (answer.getErrorCode() == ErrorCode.ACCOUNT_DEACTIVATED) {
				answer.setError(ErrorCode.map.get(ErrorCode.ACCOUNT_DEACTIVATED));
				log.error("账号{} {}", chatService.getAccount(), ErrorCode.map.get(ErrorCode.ACCOUNT_DEACTIVATED));
//				AccountPool.removeAccount(chatService.getAccount());
			}
			messageService.modifyGptAnswerMessageCard(messageId, title, (String) answer.getError());
			chatService.setStatus(Status.FINISHED);
			conversationPool.getConversation(chatId).setStatus(Status.FINISHED);
			return;
		}
		Conversation conversation = conversationPool.getConversation(chatId);
		conversation.setParentMessageId(answer.getMessage().getId());
		conversation.setConversationId(answer.getConversationId());

		if (!answer.isFinished()) {
			chatService.setStatus(Status.RUNNING);
			conversation.setStatus(Status.RUNNING);
		} else {
			chatService.setStatus(Status.FINISHED);
			conversation.setStatus(Status.FINISHED);
		}

		String content = answer.getAnswer();
		if (content == null || content.equals("")) {
			return;
		}
		PatchMessageResp resp1 = messageService.modifyGptAnswerMessageCard(messageId, title, content);

		if (answer.isFinished() && resp1.getCode() == 230020) {
			//保证最后完整的gpt响应 不会被飞书消息频率限制
			while (answer.isFinished() && resp1.getCode() != 0) {
				log.warn("重试中 code: {} msg: {}", resp1.getCode(), resp1.getMsg());
				TimeUnit.MILLISECONDS.sleep(500);
				resp1 = messageService.modifyGptAnswerMessageCard(messageId, title, content);
			}
			log.info("重试成功");
		}
	}
}