### Installation

```
docker network create --driver bridge net-ada-ae
docker build --rm -t ada_rest_api .
docker run --name ada_rest_api -d --restart unless-stopped --network net-ada-ae -e API_TOKEN=CHANGE#THIS ada_rest_api http://ada_triplestore:8890/sparql /api
```

sudo docker stop  ada_rest_api && sudo docker rm  ada_rest_api && sudo docker build --rm -t ada_rest_api . && sudo docker run --name ada_rest_api -d --restart unless-stopped --network net-ada-ae -e API_TOKEN=ADA+API+1234 ada_rest_api http://ada_triplestore:8890/sparql /api