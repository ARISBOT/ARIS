<!--
SPDX-FileCopyrightText: Katastima Authors

SPDX-License-Identifier: EUPL-1.2
-->

[![EUPL](https://img.shields.io/badge/licence-EUPL-blue.svg)](https://eupl.eu/)

# ARIS

<!-- ADD DESCRIPTION HERE -->

## Getting started

Documentation for the APK Scanner is available [here](https://katastima.org/en/apkscanner/overview).

## Releasing

```
# Build including shadowJars, publish will deploy to a local staging repository.
./gradlew clean assemble shadowJar publish

# Release, uploading to MavenCentral and create releases on various forges.
./gradlew jreleaserRelease
```

## Contributing

The following tools are required to ensure the project stays [REUSE](https://reuse.software/) compliant.

- [`reuse`](https://github.com/fsfe/reuse-tool)
  - `$ pipx install reuse`
  - `$ pipx ensurepath`
- [`pre-commit`](https://pre-commit.com/)
  - `$ pre-commit install`

## Licensing

This repository is [REUSE](https://reuse.software/) compliant.

Generally, all "APK Scanner" specific source code is licensed under [EUPL-1.2](https://eupl.eu/).

## Funding

This project is funded through [NGI Mobifree Fund](https://nlnet.nl/mobifree), a fund established by [NLnet](https://nlnet.nl) with
financial support from the European Commission's [Next Generation Internet](https://ngi.eu) program.
Learn more at the [NLnet project page](https://nlnet.nl/project/IzzyOnDroid).

<p align="center">
  <a href="https://nlnet.nl"><img src="https://nlnet.nl/logo/banner.png" alt="NLnet foundation logo" height="50px" /></a>
  &emsp;
  <a href="https://nlnet.nl/mobifree/"><img src="https://nlnet.nl/image/logos/NGI_Mobifree_tag.svg" height="50px" /></a>
</p>
