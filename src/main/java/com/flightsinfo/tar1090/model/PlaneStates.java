package com.flightsinfo.tar1090.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.flightsinfo.tar1090.mappers.OpenSkyStatesDeserializer;

import java.io.Serializable;
import java.util.Collection;

import static java.util.Objects.nonNull;

@JsonDeserialize(using = OpenSkyStatesDeserializer.class)
public class PlaneStates implements Serializable {

    @JsonProperty("time")
    private int time;

    @JsonProperty("states")
    private Collection<StateVector> stateVectors;

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public Collection<StateVector> getStateVectors() {
        return (nonNull(stateVectors)) ? stateVectors : null;
    }

    public void setStateVectors(Collection<StateVector> stateVectors) {
        this.stateVectors = stateVectors;
    }
}
