#!/bin/bash

# Exit on error
set -e

# Run test suite
mvn clean compile
mvn test -Dsurefire.useFile=false -Dsurefire.rerunFailingTestsCount=3