FROM nginx:stable

COPY index.html /usr/share/nginx/html/index.html
COPY nginx.conf /etc/nginx/conf.d/default.conf

# sudo docker stop  ae-nginx && sudo docker rm  ae-nginx && sudo docker build -t ae-nginx . && sudo docker run --name ae-nginx -d -p 9090:80 ae-nginx
# sudo docker stop  ae-nginx && sudo docker rm  ae-nginx && sudo docker build -t ae-nginx . && sudo docker run --name ae-nginx -d -p 9090:80 --network net-ada-ae ae-nginx

