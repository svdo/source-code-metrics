#!/usr/bin/env bash

clj -A:test --plugin cloverage "$@"
