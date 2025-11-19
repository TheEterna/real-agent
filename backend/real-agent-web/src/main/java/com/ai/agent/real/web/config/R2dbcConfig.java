package com.ai.agent.real.web.config;

import com.ai.agent.real.domain.entity.roleplay.PlaygroundRoleplayRole;
import com.ai.agent.real.domain.entity.roleplay.PlaygroundRoleplaySessionMessage;
import org.springframework.context.annotation.*;
import org.springframework.data.r2dbc.convert.*;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.r2dbc.dialect.*;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

import java.util.*;

/**
 * @author han
 * @time 2025/9/28 5:48
 */

@Configuration
@EnableR2dbcRepositories(basePackages = "com.ai.agent.real.domain.repository")
public class R2dbcConfig {

	@Bean
	public R2dbcCustomConversions r2dbcCustomConversions() {
		List<Object> converters = new ArrayList<>();
		converters.add(new MessageTypeReadingConverter());
		converters.add(new MessageTypeWritingConverter());
		converters.add(new MessageRoleReadingConverter());
		converters.add(new MessageRoleWritingConverter());
		converters.add(new VoiceEnumReadingConverter());
		converters.add(new VoiceEnumWritingConverter());
		return R2dbcCustomConversions.of(MySqlDialect.INSTANCE, converters);
	}

	// MessageType 转换器
	@ReadingConverter
	public class MessageTypeReadingConverter
			implements Converter<String, PlaygroundRoleplaySessionMessage.MessageType> {

		@Override
		public PlaygroundRoleplaySessionMessage.MessageType convert(String source) {
			return source != null ? PlaygroundRoleplaySessionMessage.MessageType.valueOf(source.toUpperCase()) : null;
		}

	}

	@WritingConverter
	public class MessageTypeWritingConverter
			implements Converter<PlaygroundRoleplaySessionMessage.MessageType, String> {

		@Override
		public String convert(PlaygroundRoleplaySessionMessage.MessageType source) {
			return source != null ? source.name() : null;
		}

	}

	// MessageRole 转换器
	@ReadingConverter
	public class MessageRoleReadingConverter
			implements Converter<String, PlaygroundRoleplaySessionMessage.MessageRole> {

		@Override
		public PlaygroundRoleplaySessionMessage.MessageRole convert(String source) {
			return source != null ? PlaygroundRoleplaySessionMessage.MessageRole.valueOf(source.toUpperCase()) : null;
		}

	}

	@WritingConverter
	public class MessageRoleWritingConverter
			implements Converter<PlaygroundRoleplaySessionMessage.MessageRole, String> {

		@Override
		public String convert(PlaygroundRoleplaySessionMessage.MessageRole source) {
			return source != null ? source.name() : null;
		}

	}

	// MessageRole 转换器
	@ReadingConverter
	public class VoiceEnumReadingConverter implements Converter<String, PlaygroundRoleplayRole.VoiceEnum> {

		@Override
		public PlaygroundRoleplayRole.VoiceEnum convert(String source) {

			// 加一步操作, 转成大写
			return source != null ? PlaygroundRoleplayRole.VoiceEnum.valueOf(source.toUpperCase()) : null;

		}

	}

	@WritingConverter
	public class VoiceEnumWritingConverter implements Converter<PlaygroundRoleplayRole.VoiceEnum, String> {

		@Override
		public String convert(PlaygroundRoleplayRole.VoiceEnum source) {
			// 写库统一存显示值，与现有数据（如 "Marcus"）保持一致
			return source != null ? source.getValue() : null;
		}

	}

}
