#!/bin/sh

docker run -d --name=overpass-api.de overpass-api
docker run -d -p 80:8080 --link overpass-api.de --name=evacuation-web-app evacuation-web-app
