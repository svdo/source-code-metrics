Metrics
=======

This program can automatically collect metrics for a given project.

Purpose
-------

This project should be viewed as a personal tool, not necessarily intended
for other purposes than mine. For me, it's both useful and a great vehicle
to learn Clojure better. Having said that, you're of course welcome to
use it if you see value in it.

Configuration
-------------

The file `config.edn` contains configurable things, including the SonarQube
project key. The token must be generated in SonarQube (My Account -> Security
-> Generate Token). A second token that is needed is for GitLab. You can
generate that token by navigating in GitLab to Account -> Settings ->
Access Tokens. Enable the scope 'api' for this token.

In order to keep your secrets safe, you may should put them in
`config.local.edn` instead of `config.edn`. The two maps are merged, where
the anything in the `local` one will overwrite the other. The file
`config.local.edn` is ignored by git, so this way you don't have to fear
accidentally committing your secrets to git. So before running this,
you should create `config.local.edn` and put all the Gitlab and SonarQube
information in there by following the instructions in `config.edn`. For example,
your final `config.local.edn` when you want to report on two different projects
should look like this:

```edn
{:sonar/base-url      "https://sonar.example.com"
 :gitlab/base-url     "https://gitlab.example.com/api/v4"

 :report/projects
 [{:sonar/token       "abcdefghijklmnopqrstuvwxyz"
   :sonar/project-id  "my.project-id"
   :gitlab/token      "aBCdeFGHijkLMN"
   :gitlab/project-id 42}

  {:sonar/token       "zyxwvutsrqponmlkjihgfedcba"
   :sonar/project-id  "my.other-project-id"
   :gitlab/token      "NMLkjiHGFedCBa"
   :gitlab/project-id 24}]}
```

Running
-------

```bash
./run.sh
```

Unit Tests
----------

```bash
./test.sh --watch
```
