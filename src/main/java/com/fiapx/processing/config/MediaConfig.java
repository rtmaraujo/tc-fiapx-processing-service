package com.fiapx.processing.config;

import com.fiapx.processing.infrastructure.media.DefaultProcessRunner;
import com.fiapx.processing.infrastructure.media.FfmpegFrameExtractor;
import com.fiapx.processing.infrastructure.media.ProcessRunner;
import com.fiapx.processing.infrastructure.media.ZipService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MediaConfig {

    @Bean
    public ProcessRunner processRunner() {
        return new DefaultProcessRunner();
    }

    @Bean
    public FfmpegFrameExtractor frameExtractor(ProcessRunner processRunner, FfmpegProperties properties) {
        return new FfmpegFrameExtractor(processRunner, properties);
    }

    @Bean
    public ZipService zipService() {
        return new ZipService();
    }
}
