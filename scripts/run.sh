#!/usr/bin/env sh

scan(){

  if [ -z "$ORG_ID" ]
  then
        echo "ORG_ID is empty; exiting" ; exit 1
  else
       export ORG_ID=$ORG_ID
       ./projects.sh
  fi
}
case $1 in
  "scan,calculate")
   scan
   lein run
    ;;

  "scan")
    scan
    ;;
 "calculate")
    lein run
    ;;
  *)
    echo "Unknown argument $1; exiting" && exit 1
    ;;
esac

lein run