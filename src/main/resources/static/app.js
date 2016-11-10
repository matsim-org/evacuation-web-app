/*
 * Copyright (c) 2016 Gregor Lämmel
 * This file is part of evacuation.
 * evacuation is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * See also LICENSE and WARRANTY file
 */

function toHHMMSS(time) {
    var sec_num = Math.round(time);//, 10); // don't forget the second param
    var hours = Math.floor(sec_num / 3600);
    var minutes = Math.floor((sec_num - (hours * 3600)) / 60);
    var seconds = sec_num - (hours * 3600) - (minutes * 60);

    if (hours < 10) {
        hours = "0" + hours;
    }
    if (minutes < 10) {
        minutes = "0" + minutes;
    }
    if (seconds < 10) {
        seconds = "0" + seconds;
    }
    return hours + ':' + minutes + ':' + seconds;
}


mapboxgl.accessToken = 'pk.eyJ1IjoiZ3JlZ29ybGFlbW1lbCIsImEiOiJjaW92N3FjNzgwMDVld2RranFibGV3NjNxIn0.OqCU0VqbtWa_90fz11UArw';
var map = new mapboxgl.Map({
    container: 'map', // container id
    style: 'mapbox://styles/mapbox/streets-v10', //stylesheet location
    center: [15, 53], // starting position
    zoom: 9 // starting zoom
});

map.addControl(new mapboxgl.Geocoder());

draw = mapboxgl.Draw({
    drawing: true,
    displayControlsDefault: false,
    controls: {
        polygon: true,
        trash: true
    },
    styles: [
        // ACTIVE (being drawn)
        // line stroke
        {
            "id": "gl-draw-line",
            "type": "line",
            "filter": ["all", ["==", "$type", "LineString"], ["!=", "mode", "static"]],
            "layout": {
                "line-cap": "round",
                "line-join": "round"
            },
            "paint": {
                "line-color": "#FF0C0C",
                "line-dasharray": [0.2, 2],
                "line-width": 4
            }
        },
        // polygon fill
        {
            "id": "gl-draw-polygon-fill",
            "type": "fill",
            "filter": ["all", ["==", "$type", "Polygon"], ["!=", "mode", "static"]],
            "paint": {
                "fill-color": "#D20C0C",
                "fill-outline-color": "#D20C0C",
                "fill-opacity": 0.1
            }
        },
        // polygon outline stroke
        // This doesn't style the first edge of the polygon, which uses the line stroke styling instead
        {
            "id": "gl-draw-polygon-stroke-active",
            "type": "line",
            "filter": ["all", ["==", "$type", "Polygon"], ["!=", "mode", "static"]],
            "layout": {
                "line-cap": "round",
                "line-join": "round"
            },
            "paint": {
                "line-color": "#D20C0C",
                "line-dasharray": [0.2, 2],
                "line-width": 4
            }
        },
        // vertex point halos
        {
            "id": "gl-draw-polygon-and-line-vertex-halo-active",
            "type": "circle",
            "filter": ["all", ["==", "meta", "vertex"], ["==", "$type", "Point"], ["!=", "mode", "static"]],
            "paint": {
                "circle-radius": 5,
                "circle-color": "#FFF"
            }
        },
        // vertex points
        {
            "id": "gl-draw-polygon-and-line-vertex-active",
            "type": "circle",
            "filter": ["all", ["==", "meta", "vertex"], ["==", "$type", "Point"], ["!=", "mode", "static"]],
            "paint": {
                "circle-radius": 3,
                "circle-color": "#D20C0C"
            }
        },

        // INACTIVE (static, already drawn)
        // line stroke
        {
            "id": "gl-draw-line-static",
            "type": "line",
            "filter": ["all", ["==", "$type", "LineString"], ["==", "mode", "static"]],
            "layout": {
                "line-cap": "round",
                "line-join": "round"
            },
            "paint": {
                "line-color": "#000",
                "line-width": 3
            }
        },
        // polygon fill
        {
            "id": "gl-draw-polygon-fill-static",
            "type": "fill",
            "filter": ["all", ["==", "$type", "Polygon"], ["==", "mode", "static"]],
            "paint": {
                "fill-color": "#000",
                "fill-outline-color": "#000",
                "fill-opacity": 0.1
            }
        },
        // polygon outline
        {
            "id": "gl-draw-polygon-stroke-static",
            "type": "line",
            "filter": ["all", ["==", "$type", "Polygon"], ["==", "mode", "static"]],
            "layout": {
                "line-cap": "round",
                "line-join": "round"
            },
            "paint": {
                "line-color": "#000",
                "line-width": 3
            }
        }
    ]

});

map.addControl(draw);
map.addControl(new mapboxgl.NavigationControl());


var stompClient = null;

function setConnected(connected) {
    $("#connect").prop("disabled", connected);
    $("#submit").prop("disabled", !connected);
    $("#disconnect").prop("disabled", !connected);
    var nr = $("#number");
    nr.prop("disabled", !connected);
    nr.val("50000");

}

function setSubmitted(submitted) {
    $("#connect").prop("disabled", submitted);
    $("#submit").prop("disabled", submitted);
    $("#disconnect").prop("disabled", !submitted);
}

function connect() {
    var data = draw.getAll();

    if (data.features.length <= 0) {
        alert("Use the draw tools to draw an evacuation area!");
    } else {
        var socket = new SockJS('/evacuation');
        stompClient = Stomp.over(socket);
        stompClient.connect({}, function (frame) {
            setConnected(true);
            console.log('Connected: ' + frame);
            stompClient.subscribe('/topic/evacuation', function (greeting) {
                if (greeting != null) {
                    showGreeting(JSON.parse(greeting.body));
                } else {
                    alert("Could not run evacuation simulation, please select smaller area and try again!");
                }
            });
            stompClient.subscribe('/topic/routing', function (route) {
                if (route != null) {
                    showRoute(JSON.parse(route.body));
                } else {
                    alert("Could not find evacuation route for given origin, please choose a different one!");
                }

            })
        });
    }
}

function disconnect() {


    if (stompClient != null) {
        stompClient.disconnect();
    }

    map.removeSource('hexgrid');
    map.removeLayer('hexgrid');

    setConnected(false);

}

function submit() {
    var data = draw.getAll();
    var coords = data.features;//[0].geometry.coordinates
    coords[0].properties["num"] = $("#number").val();
    stompClient.send("/app/evac", {}, JSON.stringify(coords));
    setSubmitted(true);
}

function showRoute(route) {

    try {
        map.removeSource("route");
        map.removeLayer("route");
        map.removeSource("tt");
        map.removeLayer("tt");
    }
    catch (err) {
        //        alert("Error!");
    }
    map.addSource("route", {
        type: 'geojson',
        data: route
    });
    map.addLayer({
        'id': 'route',
        'type': 'line',
        'source': 'route',
        "layout": {
            "line-join": "round",
            "line-cap": "round"
        },
        "paint": {
            "line-color": "#000",
            "line-width": 4
        }
    }, 'start');

    var time = route.features[idx].properties["time"];
    var coords = route.features[route.features.length - 1].geometry.coordinates;
    var m = coords[coords.length - 1];
    map.addSource("tt", {
        "type": "geojson",
        "data": {
            "type": "FeatureCollection",
            "features": [{
                "type": "Feature",
                "geometry": {
                    "type": "Point",
                    "coordinates": [m[0], m[1]]
                },
                "properties": {
                    "title": "Arrive in " + toHHMMSS(time),
                    "icon": "marker"
                }
            }]
        }
    });
    map.addLayer({
        "id": "tt",
        "type": "symbol",
        "source": "tt",
        "layout": {
            "icon-image": "{icon}-15",
            "text-field": "{title}",
            "text-font": ["Open Sans Semibold", "Arial Unicode MS Bold"],
            "text-offset": [0, 0.6],
            "text-anchor": "top"
        },
        "paint": {
            "text-color": "#000",
            "text-halo-color": "#fff",
            "text-halo-width": 1,
            "text-halo-blur": 3
        }
    });


}

function showGreeting(message) {
    map.addSource("hexgrid", {
        type: 'geojson',
        data: message
    });
    map.addLayer({
        'id': 'hexgrid',
        'type': 'fill',
        'source': 'hexgrid',
        'layout': {},
        'paint': {
            //                'fill-color': '#088',
            'fill-opacity': 0.8,

            'fill-color': {
                property: 'color',
                type: 'categorical',
                stops: [
                    ['green', '#008000'],
                    ['lime', '#00ff00'],
                    ['yellow', '#ffff00'],
                    ['orange', '#ffa500'],
                    ['red', '#ff0000'],
                    ['fuchsia', '#ff00ff'],
                    ['purple', '#800080'],
                    ['white', '#ffffff']]
            }
        }
    }, 'aeroway');
    routing = true;
    map.on('click', function (e) {

        try {
            map.removeSource("start");
            map.removeLayer("start");
        }
        catch (err) {
            //        alert("Error!");
        }
        map.addSource("start", {
            "type": "geojson",
            "data": {
                "type": "FeatureCollection",
                "features": [{
                    "type": "Feature",
                    "geometry": {
                        "type": "Point",
                        "coordinates": [e.lngLat.lng, e.lngLat.lat]
                    },
                    "properties": {
                        "title": "Start now",
                        "icon": "marker"
                    }
                }]
            }
        });
        map.addLayer({
            "id": "start",
            "type": "symbol",
            "source": "start",
            "layout": {
                "icon-image": "{icon}-15",
                //                "icon-color": "#ff1177",
                "text-field": "{title}",
                "text-font": ["Open Sans Semibold", "Arial Unicode MS Bold"],
                "text-offset": [0, 0.6],
                "text-anchor": "top"
            },
            "paint": {
                "text-color": "#000",
                "text-halo-color": "#fff",
                "text-halo-width": 1,
                "text-halo-blur": 3
            }
        });

        var str = JSON.stringify(e.lngLat);
        stompClient.send("/app/route", {}, str);

    });
}

$(function () {
    $("form").on('submit', function (e) {
        e.preventDefault();
    });
    $("#connect").click(function () {
        connect();
    });
    $("#disconnect").click(function () {
        disconnect();
    });
    $("#submit").click(function () {
        submit();
    });
});


