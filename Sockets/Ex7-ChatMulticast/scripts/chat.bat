@echo off
chcp 65001 >nul
cd /d "%~dp0.."
java -Djava.net.preferIPv4Stack=true -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp out cliente.Chat %*
