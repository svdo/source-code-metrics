Metrics
=======

This program can automatically collect metrics for a given project.

Configuration
-------------

The file `config.edn` contains configurable things, including the SonarQube
project key. The token must be generated in SonarQube (My Account -> Security
-> Generate Token). The current token is my own for testing purposes, please
don't abuse.

In order to keep your secrets safe, you may choose to put them in
`config.local.edn` instead of `config.edn`. The two maps are merged, where
the anything in the `local` one will overwrite the other. The file
`config.local.edn` is ignored by git, so this way you don't have to fear
accidentally committing your secrets to git.

Running
-------

`lein run`
