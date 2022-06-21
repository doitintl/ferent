
FROM google/cloud-sdk:alpine
RUN mkdir -p /usr/src/app
RUN mkdir -p /usr/src/app/resources
RUN mkdir -p /usr/src/app/scripts

WORKDIR /usr/src/app
COPY project.clj /usr/src/app/
RUN lein deps
COPY ./src /usr/src/app/src/
COPY scripts/run.sh  /usr/src/app/scripts/

ENTRYPOINT ["./scripts/run.sh" ]
