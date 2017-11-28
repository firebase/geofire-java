#!/bin/bash

# Exit on error
set -e

# Run test suite
mvn clean compile
mvn -Dsurefire.rerunFailingTestsCount=2 -Dsurefire.useFile=false test