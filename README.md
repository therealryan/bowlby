# bowlby

The [upload-artifact action](https://github.com/actions/upload-artifact) allows you to save files from an actions workflow - stuff like test results.
Unfortunately it's [not yet possible](https://github.com/actions/upload-artifact/issues/14) to actually _look_ at those files without downloading a zip and manually extracting it.
I am _very_ lazy, so that miniscule effort is entirely intolerable when I just want to see what the test failure is.

Enter **bowlby**, an application that does the tedious downloading and unzipping bits and lets you freely browse the contents of the artifacts.

We've got an instance running at https://bowlby.flowty.dev/self/ that allows access to the artifacts in this project.
You can browse the latest test results at
 * [bowlby in isolation](https://bowlby.flowty.dev/self/latest/therealryan/bowlby/testing.yml/flow_execution_reports/app/target/mctf/latest/index.html)
 * [browser in isolation](https://bowlby.flowty.dev/self/latest/therealryan/bowlby/testing.yml/flow_execution_reports/test/target/mctf/browser/latest/index.html)
 * [github in isolation](https://bowlby.flowty.dev/self/latest/therealryan/bowlby/integration.yml/flow_execution_reports/github/latest/index.html)
 * [integrated system](https://bowlby.flowty.dev/self/latest/therealryan/bowlby/integration.yml/flow_execution_reports/e2e/latest/index.html)

If you click through those you'll get sense of _how_ it works and confirmation that it _does_ work.

## Build

It's a standard java/maven project. Check it out and run `mvn package`.
This will produce an executable jar at `/app/target/bowlby` - copy that to wherever you want.

If you don't have time for that nonsense then you can download the compiled jar from the artifacts on [this workflow](https://github.com/therealryan/bowlby/actions/workflows/package.yml).
If your risk-tolerance extends to "save-as"-ing on strange links and executing the results, then [have at it](https://bowlby.flowty.dev/self/latest/therealryan/bowlby/package.yml/bowlby/bowlby).

## Execution

Run it with `java -jar bowlby`.
If you're running on linux then you can invoke it [directly](https://github.com/brianm/really-executable-jars-maven-plugin) with `./bowlby`.

By default it'll come up on port `56567` - you should see console output to that effect.

## Usage

Point a browser at the instance (e.g.: if you're running it locally then `http://localhost:56567`) and the root page will show a form that accepts two types of links:
 * Links to specific artifacts, such as you see at the bottom of the summary page of a workflow run (e.g.: `https://github.com/therealryan/bowlby/actions/runs/10391396242/artifacts/1812359413`)
   Bowlby will redirect you to a view of the files in that particular artifact.
   [This set of flows illustrates that operation](https://bowlby.flowty.dev/self/latest/therealryan/bowlby/testing.yml/flow_execution_reports/app/target/mctf/latest/index.html#?inc=chain%3Aartifact)
 * Links to workflows, such as you'd find on the "Actions" tab (e.g.: `https://github.com/therealryan/bowlby/actions/workflows/testing.yml`)
   Bowlby will redirect you to a page with stable links that display the files of the most recent artifacts of that workflow on the repo's default branch.
   [This set of flows illustrates that operation](https://bowlby.flowty.dev/self/latest/therealryan/bowlby/testing.yml/flow_execution_reports/app/target/mctf/latest/index.html#?inc=chain%3Aworkflow)

You can integrate your bowlby instance directly into your actions by:
 1. Giving your upload-artifact step an `id` ([for example](https://github.com/therealryan/bowlby/blob/main/.github/workflows/testing.yml#L30))
 1. Adding a step that uses the [artifact ID output of that step](https://github.com/actions/upload-artifact?tab=readme-ov-file#using-outputs) to emit a markdown link into the [job summary](https://github.blog/news-insights/product-news/supercharging-github-actions-with-job-summaries/) ([for example](https://github.com/therealryan/bowlby/blob/main/.github/workflows/testing.yml#L38-L41)).

This puts a link to the browsable artifact contents into the job summary, saving you the onerous burden of copy-and-paste.

## Configuration

Bowlby is configured via environment variables with command-line argument overrides:

```
Usage: bowlby [-h] [-a=<artifactValidity>] [-c=<contextPath>] [-d=<cacheDir>]
              [-g=<githubApiHost>] [-i=<iconBehaviour>] [-l=<latestValidity>]
              [-p=<port>] [-r=<repositories>] [-t=<authToken>]
A browsable proxy for github action artifacts
  -a, --artifactValidity=<artifactValidity>
                            An ISO-8601 duration string, controlling how long
                              an artifact zip is preserved for.
                            Defaults to 'P3D', which means a downloaded
                              artifact zip will be deleted 3 days after its
                              most recent access.
                            Overrides environment variable
                              'BOWLBY_ARTIFACT_VALIDITY'
  -c, --context=<contextPath>
                            Controls the path at which bowlby assumes it is
                              being served.
                            If you're accessing bowlbly directly then you can
                              leave this blank (the default), but if you're
                              proxying requests to your webserver at
                              `/some/path` to hit your bowlby instance then
                              you'll want to set this to `/some/path` so that
                              bowlby can issue redirect responses correctly.
                            Overrides environment variable 'BOWLBY_CONTEXT_PATH'
  -d, --dir=<cacheDir>      The directory in which to cache artifacts.
                            Defaults to 'bowlby' under the system's temp
                              directory.
                            Overrides environment variable 'BOWLBY_DIR'
  -g, --github=<githubApiHost>
                            The hostname to target with github API requests.
                            Defaults to 'https://api.github.com'
                            Overrides environment variable 'BOWLBY_GH_HOST'
  -h, --help                Prints help and exits
  -i, --icon=<iconBehaviour>
                            Controls the system tray icon. Choose from NONE,
                              STATIC or DYNAMIC.
                            The dynamic icon will give a visible indication of
                              request-handling activity
                            Overrides environment variable 'BOWLBY_ICON'
  -l, --latestValidity=<latestValidity>
                            An ISO-8601 duration string, controlling how long
                              the latest artifact results are cached for.
                            Defaults to 'PT10M', which means it could take up
                              to 10 minutes for the link to the latest artifact
                              to reflect the results of a new run.
                            Overrides environment variable
                              'BOWLBY_LATEST_VALIDITY'
  -p, --port=<port>         The port at which to serve artifact contents.
                            Defaults to 56567
                            Overrides environment variable 'BOWLBY_PORT'
  -r, --repos=<repositories>
                            A comma-separated list of 'owner/repo' pairs,
                              identifying the set of repositories that bowlby
                              will serve artifacts from.
                            Defaults to empty, which means _all_ repos will be
                              served.
                            Overrides environment variable 'BOWLBY_GH_REPOS'
  -t, --token=<authToken>   The github authorisation token to present on API
                              requests.
                            Overrides environment variable
                              'BOWLBY_GH_AUTH_TOKEN'
```

Note that:
 * You'll need to give it an API auth token. A [fine-grained personal access token](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens#fine-grained-personal-access-tokens) with no special permissions works fine.
 * If you're going to run an instance exposed to the internet, I'd strongly recommend limiting which repositories are addressable via `BOWLBY_GH_REPOS`.
