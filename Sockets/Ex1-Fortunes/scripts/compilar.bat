@echo off
cd /d "%~dp0.."
if not exist out mkdir out
javac -encoding UTF-8 -d out src\comum\*.java src\servidor\*.java src\cliente\*.java
if errorlevel 1 (echo ERRO na compilacao) else (echo Compilado em out\)
