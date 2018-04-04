#!/bin/bash

# Exit on error
set -e

# Set travis environment variables even when run locally

# 1) Pull request
if [ -z "$TRAVIS_PULL_REQUEST" ]; then
  echo "TRAVIS_PULL_REQUEST: unset, setting to false"
  TRAVIS_PULL_REQUEST=false
else
  echo "TRAVIS_PULL_REQUEST: $TRAVIS_PULL_REQUEST"
fi

# 2) Secure env variables
if [ -z "$TRAVIS_SECURE_ENV_VARS" ]; then
  echo "TRAVIS_SECURE_ENV_VARS: unset, setting to false"
  TRAVIS_SECURE_ENV_VARS=false
else
  echo "TRAVIS_SECURE_ENV_VARS: $TRAVIS_SECURE_ENV_VARS"
fi

# Build
mvn -e clean compile test-compile

# Run unit tests
mvn -e test

# Only run test suite when we can decode the service acct
if [ "$TRAVIS_SECURE_ENV_VARS" = false ]; then
  echo "Could not find secure environment variables, skipping integration tests."
else
  # Decrypt service account
  openssl aes-256-cbc -K $encrypted_d7b8d9290299_key -iv $encrypted_d7b8d9290299_iv \
    -in java/service-account.json.enc -out java/service-account.json -d

  # Run test suite
  mvn -Dfailsafe.rerunFailingTestsCount=2 verify
fi
