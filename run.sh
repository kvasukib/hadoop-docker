echo "Creating docker machine using virtualbox driver..."
docker-machine create --driver virtualbox vbk
docker-machine env vbk
eval $(docker-machine env vbk)
echo "docker machine created"

echo "Creating Image..."
cd master
docker build -t hd/master .
cd ..

cd slave
docker build -t hd/slave .
cd ..

echo "Image created for master and slave"
#docker images

echo "Creating network"
docker network create vbknetwork

echo "Starting master..."
docker run -itd --net=vbknetwork --name master -h master hd/master /etc/bootstrap.sh -bash
echo "Container started with name 'master'"

N=4
i=0
while [ $i -lt $N ]
do
    echo "Start slave$i container..."
    docker run -itd --net=vbknetwork --name slave$i -h slave$i  hd/slave /etc/bootstrap.sh -d
    i=$(( $i + 1 ))
done

echo "Containers created."
#docker ps -a

echo "Waiting for 60 seconds for Hadoop Cluster to setup and HDFS to synchronize..."
sleep 60

docker exec -it master /bin/bash /usr/local/hadoop/bin/hdfs dfsadmin -report
docker exec -it master /bin/bash /usr/local/hadoop/runjob.sh

echo "Stopping and cleaning up containers..."

docker stop slave0
docker rm -f slave0

docker stop slave1
docker rm -f slave1

docker stop slave2
docker rm -f slave2

docker stop slave3
docker rm -f slave3

docker stop master
docker rm -f master

docker network rm vbknetwork

#docker-machine stop vbk
#docker-machine rm -y vbk

echo "Done. Bye!"
