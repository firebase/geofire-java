#!/bin/bash
set -e

DIR=$(dirname "${BASH_SOURCE[0]}")
pushd $DIR

echo "Generating javadocs..."
mvn javadoc:javadoc

echo "Renaming output folder"
rm -rf site/docs
mv target/site/apidocs site/docs
popd
