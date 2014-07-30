#!/bin/bash
set -e

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

echo "Generating javadocs..."
mvn javadoc:javadoc

echo "Renaming output folder"
rm -rf "$DIR/site/docs"
mv "$DIR/target/site/apidocs" "$DIR/site/docs"
