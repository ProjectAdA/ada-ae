### Installation

```
docker network create --driver bridge net-ada-ae
docker build -t ae-nginx .
docker run --name ae-nginx -d --restart unless-stopped --network net-ada-ae -p 9090:80 ae-nginx
```

