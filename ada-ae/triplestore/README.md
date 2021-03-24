### Installation

```
docker volume create --driver local --opt type=none --opt device=/home/henning/volumes/ada-virtuoso-data --opt o=bind vol-ada-virtuoso-data
docker network create --driver bridge net-ada-ae
docker build --rm -t ada_triplestore .
docker run --name ada_triplestore -d --restart=unless-stopped --network net-ada-ae -v vol-ada-virtuoso-data:/data ada_triplestore
```
