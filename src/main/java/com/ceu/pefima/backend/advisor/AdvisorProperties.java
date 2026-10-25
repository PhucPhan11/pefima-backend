package com.ceu.pefima.backend.advisor;

import java.net.URI;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * The investment profile advisor and its local LLM. Turn it on in the git-ignored
 * {@code pefima-local.properties}.
 *
 * @param enabled turns the advisor on
 * @param baseUrl LM Studio's OpenAI-compatible server; must be a loopback address
 * @param model LM Studio model key
 * @param timeout how long to wait for one review. On this machine (4 GB GPU) gemma runs
 * mostly on the CPU at about 3 tokens a second; keep it under the frontend's 300 s fetch limit
 * @param temperature sampling temperature; low, for steady comments
 * @param maxTokens cap on the answer, which bounds the wait
 */
@ConfigurationProperties("pefima.advisor")
public record AdvisorProperties(@DefaultValue("false") boolean enabled,
        @DefaultValue("http://127.0.0.1:1234") URI baseUrl, @DefaultValue("google/gemma-4-e4b") String model,
        @DefaultValue("285s") Duration timeout, @DefaultValue("0.3") double temperature,
        @DefaultValue("600") int maxTokens) {
}
