package com.rawchen.feishuchatdoc.entity.gpt;

import lombok.Data;

import java.util.ArrayList;
import java.util.Map;

@Data
public class Answer {

	private int code;
	private String content;
	private String sid;
	private int status;

	private Map<String, ArrayList<Integer>> fileRefer;
}
