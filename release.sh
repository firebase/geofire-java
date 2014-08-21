#!/bin/bash
set -e

cd $(dirname $0)

###########################
#  VALIDATE GEOFIRE REPO  #
###########################
# Ensure the checked out geofire branch is master
CHECKED_OUT_BRANCH="$(git branch | grep "*" | awk -F ' ' '{print $2}')"
if [[ $CHECKED_OUT_BRANCH != "master" ]]; then
  echo "Error: Your geofire repo is not on the master branch."
  exit 1
fi

# Make sure the geofire branch does not have existing changes
if ! git --git-dir=".git" diff --quiet; then
  echo "Error: Your geofire repo has existing changes on the master branch. Make sure you commit and push the new version before running this release script."
  exit 1
fi

##############################
#  VALIDATE CLIENT VERSIONS  #
##############################

VERSION=$(grep version pom.xml |head -2|tail -1|awk -F '>' '{print $2}'|awk -F '<' '{print $1}'|awk -F '-' '{print $1}')
read -p "We are releasing $VERSION, is this correct? (press enter to continue) " DERP
if [[ ! -z $DERP ]]; then
  echo "Cancelling release, please update pom.xml to desired version"
fi

# Ensure there is not an existing git tag for the new version
# XXX this is wrong; needs to be semver sorted as my other scripts are
LAST_GIT_TAG="$(git tag --list | tail -1 | awk -F 'v' '{print $2}')"
if [[ $VERSION == $LAST_GIT_TAG ]]; then
  echo "Error: git tag v${VERSION} already exists. Make sure you are not releasing an already-released version."
  exit 1
fi

#####################
# BUILD THE LIBRARY #
#####################

# Kick off standalone build
echo "Building artifact"
mvn -DskipTests clean install

if [[ $? -ne 0 ]]; then
  echo "Error building artifact."
  exit 1
fi

# Create docs
./create-docs.sh
if [[ $? -ne 0 ]]; then
  echo "error: There was an error creating the docs."
  exit 1
fi

STANDALONE_SRC="target/geofire-${VER}-SNAPSHOT.jar"
if [[ ! -e $STANDALONE_SRC ]]; then
  echo "Source artifact not found. Check $STANDALONE_SRC"
  exit 1
fi

###################
# DEPLOY TO MAVEN #
###################
read -p "Next, make sure this repo is clean and up to date. We will be kicking off a deploy to maven." DERP
mvn clean
mvn release:clean release:prepare release:perform

if [[ $? -ne 0 ]]; then
  echo "error: Error building and releasing to maven."
  exit 1
fi

##############
# UPDATE GIT #
##############

# Create a git tag for the new version
git tag v$VERSION
if [[ $? -ne 0 ]]; then
  echo "Error: Failed to do 'git tag' from geofire repo."
  exit 1
fi

# Push the new git tag
git push --tags
if [[ $? -ne 0 ]]; then
  echo "Error: Failed to do 'git push --tags' from geofire repo."
  exit 1
fi

################
# MANUAL STEPS #
################

echo "Manual steps:"
echo "  1) release maven repo at http://oss.sonatype.org/"
echo "  2) Deply new docs: $> firebase deploy"
echo "  3) Update the release notes for GeoFire version ${VERSION} on GitHub and jars as download"
echo "  4) Tweet @FirebaseRelease: 'v${VERSION} of GeoFire for Java is available https://github.com/firebase/geofire-java"
echo ---
echo "Done! Woo!"
