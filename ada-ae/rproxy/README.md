### Installation

```
sudo docker network create --driver bridge net-ada-ae
sudo docker stop ae-nginx && sudo docker rm ae-nginx
sudo docker build -t ae-nginx .
sudo docker create --network net-ada-ae --name ae-nginx --restart unless-stopped -p 9090:80 ae-nginx
sudo docker network connect net-ada-ingester ae-nginx
sudo docker start ae-nginx
```

