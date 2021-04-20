+---------+------------------------------------------------------------+
| Version | Change                                                     |
+=========+============================================================+
| 1.8     | -   Bugfix for Groovy Lang 3.0                             |
+---------+------------------------------------------------------------+
| 1.7     | -   Support for Docker-based execution. Available on       |
|         |     Docker Hub.                                            |
|         |                                                            |
|         | -   The `-rf` option no longer expects any IDEA            |
|         |     configuration (`.idea`) to be present in the project.  |
|         |                                                            |
|         | -   More robust processing: No longer need to pass only    |
|         |     absolute paths to `-rf` or `-p`                        |
+---------+------------------------------------------------------------+
| 1.6     | -   New `--rootfile` option to allow inspection of maven   |
|         |     projects without `.iml` files committed to SCM.\       |
|         |                                                            |
|         | -   The `--profile` option now accepts an absolute path to |
|         |     the inspection profile xml file. If the path is not    |
|         |     absolute, then the fle is supposed to be in            |
|         |     `rootdir/.idea/inspectionProfiles/`.                   |
|         |                                                            |
|         | -   Dry-run option `-n` now does no longer touch the       |
|         |     `idea.properties` file.                                |
|         |                                                            |
|         | -   The `-v` option now also increases the verbosity of    |
|         |     the IDEA process.                                      |
|         |                                                            |
|         | -   Add example for usage with Travis CI / Docker          |
|         |                                                            |
|         | -   **Eat your own dogfood** - `idea-cli-inspector` now    |
|         |     tests itself via Travis CI using Gradle &              |
|         |     `idea-cli-inspector`                                   |
|         |                                                            |
|         | -   Fix problem on parsing values with contains `:`, i.e.  |
|         |     like the path to the IDEA installation directory in    |
|         |     windows like `C:\Program Files\…`. Thanks              |
|         |     to \@ColmBhandal. Fixes \#19                           |
+---------+------------------------------------------------------------+
| 1.5.4   | -   CLI options override config values inside the          |
|         |     `.ideainspect` file.                                   |
|         |                                                            |
|         | -   The `--verbose` option now prints the effective        |
|         |     configuration.                                         |
|         |                                                            |
|         | -   Add documentation about scoping.\                      |
|         |                                                            |
|         | -   Add more FAQ entries.                                  |
+---------+------------------------------------------------------------+
| 1.5.3   | -   Render reported violations to STDERR. This should      |
|         |     display them more prominently i.e. in CI environments. |
+---------+------------------------------------------------------------+
| 1.5.2   | -   Critical bugfix: Return code not returned on failed    |
|         |     analysis. Introduced with 1.5.0.                       |
+---------+------------------------------------------------------------+
| 1.5.1   | -   Increase process timeout to avoid seeing an unexpected |
|         |     return code of 143                                     |
+---------+------------------------------------------------------------+
| 1.5     | -   Support for custom Intellij scopes\                    |
+---------+------------------------------------------------------------+
| 1.4     | -   Support for Android Studio installations\              |
+---------+------------------------------------------------------------+
| 1.3     | -   Support for Mac OSX locations of IntelliJ IDEA         |
|         |     executables\                                           |
+---------+------------------------------------------------------------+
| 1.2     | -   Add support for configuration file                     |
|         |                                                            |
|         | -   Add debugging flag `-v`                                |
|         |                                                            |
|         | -   Replaced all mandatory CLI options with default values |
|         |     (root directory, IDEA home, Inspection profile name,   |
|         |     ...​)                                                  |
+---------+------------------------------------------------------------+
| 1.1     | -   Support for ignoring issues affecting specific source  |
|         |     files using a regular expression (Option `-sf`)        |
+---------+------------------------------------------------------------+
| 1.0     | -   First release                                          |
+---------+------------------------------------------------------------+
