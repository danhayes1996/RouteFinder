package com.routefinder.dan.routefinder;

import com.google.android.gms.maps.model.Marker;

import java.io.Serializable;

public class Location implements Serializable {

    private String name;
    private double lat, lng;
    private transient Marker marker;

    public Location(Marker marker){
        this.marker = marker;
        this.name = marker.getTitle();
        this.lat = marker.getPosition().latitude;
        this.lng = marker.getPosition().longitude;
    }

    @Override
    public boolean equals(Object obj){
        if(obj == null) return false;
        if(obj instanceof Marker){
            Marker m = (Marker) obj;

            return marker.getTitle().equals(m.getTitle())
                    && marker.getPosition().latitude == m.getPosition().latitude
                    && marker.getPosition().longitude == m.getPosition().longitude;
        }

        Location other = (Location) obj;
        return equals(other.marker);
    }

    public String getName(){
        return name;
    }

    public double getLat(){
        return lat;
    }

    public double getLng(){
        return lng;
    }

    public Marker getMarker(){
        return marker;
    }
}
