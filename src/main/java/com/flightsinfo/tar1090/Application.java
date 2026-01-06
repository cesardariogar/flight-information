package com.flightsinfo.tar1090;

import com.flightsinfo.tar1090.config.OpenSkyProperties;
import com.flightsinfo.tar1090.controller.OpenSkyApiController;
import com.flightsinfo.tar1090.model.BoundingBox;
import com.flightsinfo.tar1090.model.PlaneStates;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.io.IOException;

@SpringBootApplication
public class Application {

    @Autowired
    private static final OpenSkyProperties openSkyProperties = new OpenSkyProperties();

    public static void main(String[] args) throws IOException, InterruptedException {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public CommandLineRunner run() throws Exception {
        return args -> {
            OpenSkyApiController controller = new OpenSkyApiController(
                    openSkyProperties.getClient_id(),
                    openSkyProperties.getClient_secret()
            );

            PlaneStates worldWide = controller.getStates(0, null);
            Thread.sleep(10000);
            PlaneStates switzerlannd = controller.getStates(0, null, new BoundingBox(45.8389, 47.8229, 5.9962, 10.5226));

            System.out.println(switzerlannd.getStateVectors().size() + " states in Switzerland area, and " +
                    worldWide.getStateVectors().size() + " states world-wide");
        };
    }


}
