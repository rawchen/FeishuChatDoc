package com.rawchen.feishuchatdoc.util.chatgpt;

import com.rawchen.feishuchatdoc.config.Constants;
import com.rawchen.feishuchatdoc.entity.DocFile;
import com.rawchen.feishuchatdoc.entity.Status;
import com.rawchen.feishuchatdoc.util.TaskPool;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库文档池
 *
 * @author RawChen
 * @date 2023-10-23
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Data
public class AccountPool {

	protected final Environment environment;

	private int size;

	public static Map<String, ChatService> accountPool = new HashMap<>();

	/**
	 * 初始化账号池（目前就一个账号/应用）
	 */
	@PostConstruct
	public void init() {
		List<String> usefulAccounts = new ArrayList<>();

		ChatService chatService = new ChatService(Constants.CHAT_APP_ID, Constants.CHAT_APP_SECRET);
		accountPool.put(Constants.CHAT_APP_ID, chatService);
		usefulAccounts.add(Constants.CHAT_APP_ID);
		TaskPool.init(usefulAccounts);
		TaskPool.runTask();
	}

	public ChatService getFreeChatService() {
		if (accountPool.size() > 0) {
			for (String s : accountPool.keySet()) {
				ChatService chatService = accountPool.get(s);
				if (chatService.getStatus() == Status.FINISHED) {
					chatService.setStatus(Status.RUNNING);
					return chatService;
				}
			}
		}
		return null;
	}
}
