package com.rawchen.feishuchatdoc.util.chatgpt;

import com.rawchen.feishuchatdoc.entity.gpt.Answer;

public interface AnswerProcess {

    void process(Answer answer) throws Exception;
}
