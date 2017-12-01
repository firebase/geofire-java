#!/bin/bash

# Work off travis
if [[ ! -z TRAVIS_PULL_REQUEST ]]; then
  echo "TRAVIS_PULL_REQUEST: $TRAVIS_PULL_REQUEST"
else
  echo "TRAVIS_PULL_REQUEST: unset, setting to false"
  TRAVIS_PULL_REQUEST=false
fi

# Exit on error
set -e

# Build
mvn clean compile

# Only run test suite for non-PR builds
if [ $TRAVIS_PULL_REQUEST = false ] ; then
  mvn -Dsurefire.rerunFailingTestsCount=2 -Dsurefire.useFile=false test
fi