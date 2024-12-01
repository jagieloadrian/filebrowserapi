#!/bin/bash
dir=$(pwd)
echo "$dir"
cd ..
echo $(pwd)
cd /FilebrowserBE/build/libs
cp filebrowserapi.jar $dir
