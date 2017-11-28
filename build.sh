#!/bin/bash

# Exit on error
set -e

# Copy service account file
echo "Using mock service-account.json"
echo "TODO"

mvn clean compile
mvn test -Dsurefire.useFile=false -Dsurefire.rerunFailingTestsCount=3