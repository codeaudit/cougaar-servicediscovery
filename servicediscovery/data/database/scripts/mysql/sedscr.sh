#!/bin/sh
for x
do
# The gzip | gunzip hack below prevents sed stdout from re-inserting the <CR>s
sed s/\\r\\n/\\n/g $x | gzip | gunzip > $x.tmp
done
