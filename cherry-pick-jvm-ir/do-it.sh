#!/bin/bash
git checkout master &&
  git branch -D cherry-picking &&
  git checkout upstream/1.3.70 -b cherry-picking &&
  /usr/local/google/home/ager/ssd/cherry-pick.sh &&
  ./gradlew clean &&
  ./gradlew compiler:test --tests *.Ir* --parallel
