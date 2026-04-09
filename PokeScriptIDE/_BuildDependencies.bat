@echo off
cd submodules/RSyntaxTextArea
gradlew jar
gradlew publishToMavenLocal
