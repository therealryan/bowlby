# bowlby

github actions artifact proxy

# TODO

 1. better browser control in testing - headless default, stepping, etc
 1. GUI for local use
 1. Wait for about 90 days for the artifact-link tests to fail, then fix them
   
We've got an instance running at http://132.226.129.14:56567/

Browse the latest test results:
 * [bowlby in isolation](http://132.226.129.14:56567/latest/therealryan/bowlby/testing.yml/flow_execution_reports/app/target/mctf/latest/index.html)
 * [browser in isolation](http://132.226.129.14:56567/latest/therealryan/bowlby/testing.yml/flow_execution_reports/test/target/mctf/browser/latest/index.html)
 * [github in isolation](http://132.226.129.14:56567/latest/therealryan/bowlby/integration.yml/flow_execution_reports/github/latest/index.html)
 * [integrated system](http://132.226.129.14:56567/latest/therealryan/bowlby/integration.yml/flow_execution_reports/e2e/latest/index.html)
