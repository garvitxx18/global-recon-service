package global.recon.service.feignclient;

import global.recon.service.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "llmClient",
        url = "${llm.api.url}",
        configuration = FeignConfig.class
)
public interface LlmFeignClient {

    @PostMapping(value = "${llm.api.path}", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    String complete(@RequestBody String prompt);
}
