### Installation

```
docker network create --driver bridge net-ada-ae
docker build --rm -t ada_rest_api .
docker run --name ada_rest_api -d --restart unless-stopped --network net-ada-ae -e API_TOKEN=CHANGE#THIS ada_rest_api http://ada_triplestore:8890/sparql /api
```
