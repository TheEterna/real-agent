package com.ai.agent.real.web.config;

import com.ai.agent.real.common.entity.roleplay.PlaygroundRoleplayRole.*;
import com.ai.agent.real.common.entity.roleplay.PlaygroundRoleplaySessionMessage.*;
import com.ai.agent.real.web.config.R2dbcConfig.VoiceEnumReadingConverter.*;
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
	public class MessageTypeReadingConverter implements Converter<String, MessageType> {

		@Override
		public MessageType convert(String source) {
			return source != null ? MessageType.valueOf(source.toUpperCase()) : null;
		}

	}

	@WritingConverter
	public class MessageTypeWritingConverter implements Converter<MessageType, String> {

		@Override
		public String convert(MessageType source) {
			return source != null ? source.name() : null;
		}

	}

	// MessageRole 转换器
	@ReadingConverter
	public class MessageRoleReadingConverter implements Converter<String, MessageRole> {

		@Override
		public MessageRole convert(String source) {
			return source != null ? MessageRole.valueOf(source.toUpperCase()) : null;
		}

	}

	@WritingConverter
	public class MessageRoleWritingConverter implements Converter<MessageRole, String> {

		@Override
		public String convert(MessageRole source) {
			return source != null ? source.name() : null;
		}

	}

	// MessageRole 转换器
	@ReadingConverter
	public class VoiceEnumReadingConverter implements Converter<String, VoiceEnum> {

		@Override
		public VoiceEnum convert(String source) {

			// 加一步操作, 转成大写
			return source != null ? VoiceEnum.valueOf(source.toUpperCase()) : null;

		}

	}

	@WritingConverter
	public class VoiceEnumWritingConverter implements Converter<VoiceEnum, String> {

		@Override
		public String convert(VoiceEnum source) {
			// 写库统一存显示值，与现有数据（如 "Marcus"）保持一致
			return source != null ? source.getValue() : null;
		}

	}

}
