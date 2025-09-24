package com.location.server.api;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/system")
public class SystemController {

  @GetMapping(value = "/ping", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter ping() {
    var emitter = new SseEmitter(0L); // no timeout
    var exec = Executors.newSingleThreadScheduledExecutor();
    exec.scheduleAtFixedRate(
        () -> {
          try {
            var data = "ping:" + Instant.now();
            emitter.send(SseEmitter.event().name("ping").data(data));
          } catch (IOException e) {
            emitter.completeWithError(e);
          }
        },
        0,
        15,
        TimeUnit.SECONDS);
    emitter.onCompletion(exec::shutdown);
    emitter.onTimeout(exec::shutdown);
    return emitter;
  }
}
