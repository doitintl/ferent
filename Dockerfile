# See here for better options
#https://hub.docker.com/_/clojure

FROM clojure:openjdk-8-lein-slim-buster
COPY ./src /usr/src/app/src/
COPY ./project.clj /usr/src/app
WORKDIR /usr/src/app
CMD ["lein", "run"]