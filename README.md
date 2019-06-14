Metrics
=======

This program can automatically collect metrics for a given project.

Configuration
-------------

The file `config.edn` contains configurable things, including the SonarQube
project key. The token must be generated in SonarQube (My Account -> Security
-> Generate Token). A second token that is needed is for GitLab. You can
generate that token by navigating in GitLab to Account -> Settings ->
Access Tokens. Enable the scope 'api' for this token.

In order to keep your secrets safe, you may choose to put them in
`config.local.edn` instead of `config.edn`. The two maps are merged, where
the anything in the `local` one will overwrite the other. The file
`config.local.edn` is ignored by git, so this way you don't have to fear
accidentally committing your secrets to git.

Running
-------

```bash
lein run
```


Unit Tests
----------

```bash
lein test
```

*Note:* See https://github.com/venantius/ultra/issues/108. This project uses
Ultra for better test support, but there is currently an open issue on a
dependency of Ultra. Bottom line: it works when using Java 8 (and probably
any Java before 12).

Global Leiningen configuration for Ultra is done in `~/.lein/profiles.clj`:

```clojure
{:user {:plugins [[venantius/ultra "0.6.0"]]}}
```
