package com.location.server.api;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/system")
public class SystemController {

  private static final ScheduledExecutorService SCHEDULER =
      Executors.newSingleThreadScheduledExecutor(
          r -> {
            Thread t = new Thread(r, "system-ping");
            t.setDaemon(true);
            return t;
          });

  @GetMapping(value = "/ping", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter ping() {
    var emitter = new SseEmitter(0L);
    AtomicReference<ScheduledFuture<?>> futureRef = new AtomicReference<>();
    Runnable cleanup =
        () -> {
          ScheduledFuture<?> future = futureRef.getAndSet(null);
          if (future != null) {
            future.cancel(true);
          }
        };
    ScheduledFuture<?> future =
        SCHEDULER.scheduleAtFixedRate(
            () -> {
              try {
                emitter.send(
                    SseEmitter.event().name("ping").data(Instant.now().toString(), MediaType.TEXT_PLAIN));
              } catch (IllegalStateException e) {
                cleanup.run();
                emitter.complete();
              } catch (IOException e) {
                cleanup.run();
                emitter.completeWithError(e);
              }
            },
            0,
            15,
            TimeUnit.SECONDS);
    futureRef.set(future);
    emitter.onCompletion(cleanup);
    emitter.onTimeout(
        () -> {
          cleanup.run();
          emitter.complete();
        });
    emitter.onError(e -> cleanup.run());
    return emitter;
  }
}
