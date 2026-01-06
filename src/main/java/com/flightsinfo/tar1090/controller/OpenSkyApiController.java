package com.flightsinfo.tar1090.controller;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightsinfo.tar1090.enums.RequestType;
import com.flightsinfo.tar1090.model.BoundingBox;
import com.flightsinfo.tar1090.model.PlaneStates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.json.JsonParseException;
import org.springframework.http.*;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.util.*;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@RestController
public class OpenSkyApiController {

    private static final String HOST = "opensky-network.org";
    private static final String API_ROOT = "https://" + HOST + "/api";
    private static final String STATES_URI = API_ROOT + "/states/all";
    private static final String MY_STATES_URI = API_ROOT + "/states/own";

    HttpHeaders headers;
    private boolean authenticated;
    private final Map<RequestType, Long> lastRequestTime;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private static final Logger logger = LoggerFactory.getLogger(OpenSkyApiController.class);

    public OpenSkyApiController(String client_id, String client_secret) {
        this();

        authenticated = nonNull(client_id) && nonNull(client_secret);
        if (authenticated) {
            headers.set("grant_type", "client_credentials");
            headers.set("client_id", client_id);
            headers.set("client_secret", client_secret);
            logger.info("OpenSky API authenticated access enabled for user {}", client_id);
        } else {
            logger.info("OpenSky API anonymous access enabled");
        }
    }

    @Autowired
    public OpenSkyApiController() {
        this.lastRequestTime = new HashMap<>();
        this.objectMapper = new ObjectMapper();
        this.restTemplate = new RestTemplate();
        this.headers = new HttpHeaders();
    }

    /**
     * Make the actual HTTP Request and return the parsed response
     *
     * @param baseUri base uri to request
     * @param nvps    name value pairs to be sent as query parameters
     * @return parsed states
     * @throws IOException if there was an HTTP error
     */
    private PlaneStates getResponse(String baseUri,
                                    Collection<AbstractMap.Entry<String, String>> nvps) throws IOException {

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUri);
        for (AbstractMap.Entry<String, String> nvp : nvps) {
            builder.queryParam(nvp.getKey(), nvp.getValue());
        }
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(null, headers);

        ResponseEntity<String> response = restTemplate.exchange(builder.toUriString(), HttpMethod.GET, request, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IOException("Could not get OpenSky Vectors, response " + response);
        }
        MediaType contentType = response.getHeaders().getContentType();
        Charset charset = (nonNull(contentType)) ? contentType.getCharset() : null;

        if (isNull(charset)) {
            throw new IOException("Could not read charset in response. Content-Type is " + contentType);
        } else {
            return objectMapper.readValue(
                    new InputStreamReader(new ByteArrayInputStream(response.getBody().getBytes()), charset),
                    PlaneStates.class
            );
        }
    }

    /**
     * Prevent client from sending too many requests. Checks are applied on server-side, too.
     *
     * @param type           identifies calling function (GET_STATES or GET_MY_STATES)
     * @param timeDiffAuth   time im ms that must be in between two consecutive calls if user is authenticated
     * @param timeDiffNoAuth time im ms that must be in between two consecutive calls if user is not authenticated
     * @return true if request may be issued, false otherwise
     */
    private boolean checkRateLimit(RequestType type, long timeDiffAuth, long timeDiffNoAuth) {
        Long t = lastRequestTime.get(type);
        long now = System.currentTimeMillis();
        lastRequestTime.put(type, now);
        return (t == null || (authenticated && now - t > timeDiffAuth) || (!authenticated && now - t > timeDiffNoAuth));
    }


    /**
     * Get states from server and handle errors
     *
     * @throws IOException if there was an HTTP error`
     */
    private PlaneStates getOpenSkyStateErrorsIfExists(String baseUri,
                                                      ArrayList<AbstractMap.Entry<String, String>> nvps) throws IOException {
        try {
            return getResponse(baseUri, nvps);
        } catch (MalformedURLException e) {
            // this should not happen
            e.printStackTrace();
            throw new RuntimeException("Programming Error in OpenSky API. Invalid URI. Please report a bug");
        } catch (JsonParseException | JsonMappingException e) {
            // this should not happen
            e.printStackTrace();
            throw new RuntimeException("Programming Error in OpenSky API. Could not parse JSON Data. Please report a bug");
        }
    }

    /**
     * Retrieve state vectors for a given time. If time == 0 the most recent ones are taken.
     * Optional filters might be applied for ICAO24 addresses.
     *
     * @param time   Unix time stamp (seconds since epoch).
     * @param icao24 retrieve only state vectors for the given ICAO24 addresses. If {@code null}, no filter will be applied on the ICAO24 address.
     * @return {@link PlaneStates} if request was successful, {@code null} otherwise or if there's no new data/rate limit reached
     * @throws IOException if there was an HTTP error
     */
    public PlaneStates getStates(int time, String[] icao24) throws IOException {
        ArrayList<AbstractMap.Entry<String, String>> nvps = new ArrayList<>();
        if (icao24 != null) {
            for (String i : icao24) {
                nvps.add(new AbstractMap.SimpleImmutableEntry<>("icao24", i));
            }
        }
        nvps.add(new AbstractMap.SimpleImmutableEntry<>("time", Integer.toString(time)));
        return checkRateLimit(RequestType.GET_STATES, 4900, 9900) ? getOpenSkyStateErrorsIfExists(STATES_URI, nvps) : null;
    }

    /**
     * Retrieve state vectors for a given time. If time == 0 the most recent ones are taken.
     * Optional filters might be applied for ICAO24 addresses.
     * Furthermore, data can be retrieved for a certain area by using a bounding box.
     *
     * @param time   Unix time stamp (seconds since epoch).
     * @param icao24 retrieve only state vectors for the given ICAO24 addresses. If {@code null}, no filter will be applied on the ICAO24 address.
     * @param bbox   bounding box to retrieve data for a certain area. If {@code null}, no filter will be applied on the position.
     * @return {@link PlaneStates} if request was successful, {@code null} otherwise or if there's no new data/rate limit reached
     * @throws IOException if there was an HTTP error
     */
    public PlaneStates getStates(int time, String[] icao24, BoundingBox bbox) throws IOException {
        if (bbox == null) return getStates(time, icao24);

        ArrayList<AbstractMap.Entry<String, String>> nvps = new ArrayList<>();
        if (icao24 != null) {
            for (String i : icao24) {
                nvps.add(new AbstractMap.SimpleImmutableEntry<>("icao24", i));
            }
        }
        nvps.add(new AbstractMap.SimpleImmutableEntry<>("time", Integer.toString(time)));
        nvps.add(new AbstractMap.SimpleImmutableEntry<>("lamin", Double.toString(bbox.minLatitude())));
        nvps.add(new AbstractMap.SimpleImmutableEntry<>("lamax", Double.toString(bbox.maxLatitude())));
        nvps.add(new AbstractMap.SimpleImmutableEntry<>("lomin", Double.toString(bbox.minLongitude())));
        nvps.add(new AbstractMap.SimpleImmutableEntry<>("lomax", Double.toString(bbox.maxLongitude())));
        return checkRateLimit(RequestType.GET_STATES, 4900, 9900) ? getOpenSkyStateErrorsIfExists(STATES_URI, nvps) : null;
    }

    /**
     * Retrieve state vectors for your own sensors. Authentication is required for this operation.
     * If time = 0 the most recent ones are taken. Optional filters may be applied for ICAO24 addresses and sensor
     * serial numbers.
     *
     * @param time    Unix time stamp (seconds since epoch).
     * @param icao24  retrieve only state vectors for the given ICAO24 addresses. If {@code null}, no filter will be applied on the ICAO24 address.
     * @param serials retrieve only states of vehicles as seen by the given sensors. It expects an array of sensor serial numbers which belong to the given account. If {@code null}, no filter will be applied on the sensor.
     * @return {@link PlaneStates} if request was successful, {@code null} otherwise or if there's no new data/rate limit reached
     * @throws IOException if there was an HTTP error
     */
    public PlaneStates getMyStates(int time, String[] icao24, Integer[] serials) throws IOException {
        if (!authenticated) {
            throw new IllegalAccessError("Anonymous access of 'myStates' not allowed");
        }

        ArrayList<AbstractMap.Entry<String, String>> nvps = new ArrayList<>();
        if (icao24 != null) {
            for (String i : icao24) {
                nvps.add(new AbstractMap.SimpleImmutableEntry<>("icao24", i));
            }
        }
        if (serials != null) {
            for (Integer s : serials) {
                nvps.add(new AbstractMap.SimpleImmutableEntry<>("serials", Integer.toString(s)));
            }
        }
        nvps.add(new AbstractMap.SimpleImmutableEntry<>("time", Integer.toString(time)));
        return checkRateLimit(RequestType.GET_MY_STATES, 900, 0) ? getOpenSkyStateErrorsIfExists(MY_STATES_URI, nvps) : null;
    }

}
