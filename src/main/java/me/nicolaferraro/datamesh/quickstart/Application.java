package me.nicolaferraro.datamesh.quickstart;

import me.nicolaferraro.datamesh.client.api.DataMeshClient;
import me.nicolaferraro.datamesh.client.api.DataMeshEvent;
import me.nicolaferraro.datamesh.springboot.annotation.DataMeshListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import reactor.core.scheduler.Schedulers;

import java.util.Optional;
import java.util.UUID;

@SpringBootApplication
@EnableScheduling
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Autowired
    DataMeshClient client;

    @DataMeshListener(namePattern = "cippa.*")
    public void cippa(DataMeshEvent<String> event) {

        System.out.println("Got event: " + event.getName());

        Optional<Integer> counter = event.projection().read("counter", Integer.class)
                .subscribeOn(Schedulers.newSingle("cippalippa"))
                .blockOptional();
        event.projection().upsert("counter", counter.map(c -> c + 1).orElse(1));

        System.out.println("Processed event: " + event.getName() + " - counter was " + counter);
    }


    @Scheduled(fixedDelay = 5000, initialDelay = 5000)
    public void push() {
        client.pushEvent("Hello", "lippa", "cippa-" + UUID.randomUUID(), "v1");
    }

}
