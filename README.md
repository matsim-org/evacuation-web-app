[![Build Status](https://travis-ci.org/matsim-org/evacuation-web-app.svg?branch=master)](https://travis-ci.org/matsim-org/evacuation-web-app)

This web application let you run evacuation simulations from your web browser.
While the evacuation scenario is defined on an interactive map in your web browser (frontend), 
the actual simulation run's on a (local instantiated) server (backend).

# Prerequisites

Java >= 1.8; 
Maven; 
A firewall configuration that let you open server port 8080 (for local connections)

# Installation

    git clone https://github.com/matsim-org/evacuation-web-app.git

# Bild and run

    cd evacuation-web-app
    mvn clean package
    java -Xmx8G -jar target/evacuation-web-app-0.1-SNAPSHOT.jar

This will build and start the backend.
The `-Xmx8G` option makes sure that the Java VM has at most 8 Gigabytes of RAM available. 
This is sufficient for running small and medium size szenarios (e.g. a town or a smaller district of a city).
If you intend to run large scenarios you have to adapt the `-Xmx` option accordingly.
After the backend is up and running open the web browser of your choice and enter http://localhost:8080.

# Run in Docker

To start the application in Docker enter

    docker run -p 8080:8080 grgrlmml/evacuation-web-app
    
To receive the latest updates of the application enter
    
    docker pull grgrlmml/evacuation-web-app

# Usage

1. Zoom and pan to the desired location.
2. Select the evacuation area using the polygon tool (button on the top left)
3. Once evacuation area is selected, press connect.
4. Enter the number of evacuees.
5. Press submit.
6. After a while the evacuation analysis results will be shown.
7. You may no click anywhere inside the evacuation area to get a suggested evacuation route.

# Demo instance
A demo instance is running on [http://evac.vsp.tu-berlin.de](http://evac.vsp.tu-berlin.de)

# Running test cases
Test cases relay on a local overpass server listening on port 9090. If you have docker you can start it by invoking
  
    docker pull grgrlmml/overpass-api-4-tests
    docker run -d -p 9090:9090 grgrlmml/overpass-api-4-tests

# How to contribute
If you find bugs or if you want to contribute to this project please feel free to contact us via github
