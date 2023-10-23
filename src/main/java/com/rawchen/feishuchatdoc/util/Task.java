package com.rawchen.feishuchatdoc.util;

import com.rawchen.feishuchatdoc.entity.gpt.Answer;
import com.rawchen.feishuchatdoc.util.chatgpt.AnswerProcess;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class Task {
	private AnswerProcess process;
	private Answer answer;
	private String account;

	public Task(AnswerProcess process, Answer answer, String account) {
		this.process = process;
		this.answer = answer;
		this.account = account;
	}

	public void run() {
		try {
			process.process(answer);
		} catch (Exception e) {
			log.error("处理gpt响应出错", e);
			log.error(answer.toString());

		}
	}
}
