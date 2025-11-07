<!--
SPDX-FileCopyrightText: Katastima Authors

SPDX-License-Identifier: EUPL-1.2
-->

# Notes

## H2

Start H2 web console to browse DB:

- `java -cp /tmp/h2/bin/h2-2.4.240.jar org.h2.tools.Server -ifNotExists -webPort 19999 -webSSL`

My current path to the H2 database:

- `jdbc:h2:/opt/workspace/projects/Katastima/tools/apkscanner/apkscanner`

## SQLite

Broken because it does not support arrays.
