package com.fiapx.processing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fiapx.ffmpeg")
public class FfmpegProperties {

    private String binary = "ffmpeg";
    private int fps = 1;
    private int qualidade = 2;
    private String logLevel = "error";
    private long timeoutSeconds = 600;

    public String getBinary() {
        return binary;
    }

    public void setBinary(String binary) {
        this.binary = binary;
    }

    public int getFps() {
        return fps;
    }

    public void setFps(int fps) {
        this.fps = fps;
    }

    public int getQualidade() {
        return qualidade;
    }

    public void setQualidade(int qualidade) {
        this.qualidade = qualidade;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    public long getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(long timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }
}
