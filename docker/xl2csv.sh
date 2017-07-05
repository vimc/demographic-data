#!/bin/sh
SRC=$1
DEST=$(echo $SRC | sed -E 's/xlsx?/csv/i')
echo "Converting $SRC => $DEST"
ssconvert -S $SRC $DEST
