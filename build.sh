#!/bin/bash

# Exit on error
set -e

# Work off travis
if [[ ! -z TRAVIS_PULL_REQUEST ]]; then
  echo "TRAVIS_PULL_REQUEST: $TRAVIS_PULL_REQUEST"
else
  echo "TRAVIS_PULL_REQUEST: unset, setting to false"
  TRAVIS_PULL_REQUEST=false
fi

# Copy service account file
echo "Using mock service-account.json"
echo "TODO"

# Build
if [ $TRAVIS_PULL_REQUEST = false ] ; then
  echo "Building full project"
  # For a merged commit, build all configurations.
  mvn clean build test
else
  # On a pull request, just build debug which is much faster and catches
  # obvious errors.
  echo "Quick build test"
  mvn clean compile
fi