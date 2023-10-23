package com.rawchen.feishuchatdoc.util.chatgpt;

import com.rawchen.feishuchatdoc.entity.DocFileList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

/**
 * 操作知识库文档配置工具类
 */
@Slf4j
public class DocFileUtil {
	private static final String YAML_PATH = "docfiles.yml";

	public static DocFileList readFiles() {
		Yaml yaml = new Yaml(new Constructor(DocFileList.class));
		try (InputStream in = new FileSystemResource(YAML_PATH).getInputStream()) {
			return yaml.loadAs(in, DocFileList.class);
		} catch (IOException e) {
			throw new RuntimeException("Failed to read YAML", e);
		}
	}

	public static void writeFiles(DocFileList docFileList) {
		DumperOptions options = new DumperOptions();
		options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		options.setPrettyFlow(true);
		Yaml yaml = new Yaml(new Representer(), options);
		try (FileWriter writer = new FileWriter(YAML_PATH)) {
			yaml.dump(docFileList, writer);
		} catch (IOException e) {
			throw new RuntimeException("Failed to write YAML", e);
		}
	}
}
