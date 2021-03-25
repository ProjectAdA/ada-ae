### Installation

```
docker network create --driver bridge net-ada-ae
docker build --rm -t ada_explorer .
docker run --name ada_explorer -d --restart unless-stopped --network net-ada-ae  ada_explorer
```
