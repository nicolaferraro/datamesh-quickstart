package me.nicolaferraro.datamesh.quickstart;

import me.nicolaferraro.datamesh.client.api.DataMeshClient;
import me.nicolaferraro.datamesh.client.api.DataMeshEvent;
import me.nicolaferraro.datamesh.springboot.annotation.DataMeshListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@SpringBootApplication
@EnableScheduling
public class Application {

    private static Logger LOG = LoggerFactory.getLogger(Application.class);

    @Autowired
    private DataMeshClient client;

    private AtomicLong eventCounter = new AtomicLong();

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @DataMeshListener(namePattern = "cippa.*")
    public void onEvent(DataMeshEvent<String> event) {

        System.out.println("Got event: " + event.getName());

        Optional<Integer> counter = event.projection().read("counter", Integer.class)
                .subscribeOn(Schedulers.newSingle("cippalippa"))
                .blockOptional();
        event.projection().upsert("counter", counter.map(c -> c + 1).orElse(1));

        LOG.info("Processed event: " + event.getName() + " - counter was " + counter);
    }


    @Scheduled(fixedDelay = 5000, initialDelay = 5000)
    public void push() {
        LOG.info("Tick: generating a new event...");
        AtomicBoolean error = new AtomicBoolean();
        Flux.from(client.pushEvent("Hello", "lippa", "cippa-" + UUID.randomUUID(), "v1")).subscribe(ok -> {
        }, e -> {
            error.set(true);
            LOG.error("Cannot enqueue event", e);
        }, () -> {
            if (!error.get()) {
                long evts = eventCounter.incrementAndGet();
                LOG.info("{} events enqueued so far", evts);
            }
        });
    }

}