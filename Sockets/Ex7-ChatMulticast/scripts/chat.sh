#!/bin/sh
cd "$(dirname "$0")/.." || exit 1
java -Djava.net.preferIPv4Stack=true -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp out cliente.Chat "$@"
