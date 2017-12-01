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
  # Decrypt service account
  openssl aes-256-cbc -K $encrypted_d7b8d9290299_key -iv $encrypted_d7b8d9290299_iv \
    -in java/service-account.json.enc -out java/service-account.json -d

  # Run test suite
  mvn -Dsurefire.rerunFailingTestsCount=2 -Dsurefire.useFile=false test
fi