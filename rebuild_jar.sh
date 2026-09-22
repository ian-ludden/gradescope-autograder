#!/bin/bash
cd builder/ && mvn clean package
cd -
cp builder/target/project-builder-1.0-SNAPSHOT-jar-with-dependencies.jar ./AutograderBuilder.jar

