#!/bin/bash

s=anonymous
r=anonymous

cd .

grep -rl "$s" * | while read f; do
  sed -i "s|$s|$r|g" "$f"
done
