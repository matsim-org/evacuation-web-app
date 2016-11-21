[![Build Status](https://travis-ci.org/matsim-org/evacuation-web-app.svg?branch=master)](https://travis-ci.org/matsim-org/evacuation-web-app)

This web application let you run evacuation simulations from your web browser.
While the evacuation scenario is defined on an interactive map in your web browser (frontend), 
the actual simulation run's on a (local instantiated) server (backend).

#Prerequisites

Java >= 1.8; 
Maven; 
A firewall configuration that let you open server port 8080 (for local connections)

#Installation

    git clone https://github.com/matsim-org/evacuation-web-app.git

#Bild and run

    cd evacuation-web-app
    mvn clean package
    java -Xmx8G -jar target/evacuation-web-app-0.1-SNAPSHOT.jar

This will build and start the backend.
The `-Xmx8G` option makes sure that the Java VM has at most 8 Gigabytes of RAM available. 
This is sufficient for running small and medium size szenarios (e.g. a town or a smaller district of a city).
If you intend to run large scenarios you have to adapt the `-Xmx` option accordingly.
After the backend is up and running open the web browser of your choice and enter http://localhost:8080.
This will show you the frontend with more instructions on how to define the scenario and run the simulation.

#Running test cases
Test cases relay on a local overpass server listening on port 9090. If you have docker you can start it by invoking
  
    docker pull grgrlmml/overpass-api-4-tests
    docker run -d -p 9090:9090 grgrlmml/overpass-api-4-tests

#How to contribute
If you found bugs or if you want to contribute to this project please feel free to contact us via github.

