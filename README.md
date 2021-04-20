A little command-line tool to integrate the awesome IntelliJ IDEA code
inspections in your continuous integration (CI) process using Jenkins,
Bamboo, et. al.

# Quick start (using Docker and Maven)

1.  Add a valid IDEA inspection profile file to your project (find them
    at `.idea/inspectionProfiles`)

2.  Run within your project directory:

<div class="informalexample">

    docker run --rm -v $(pwd):/project bentolor/idea-cli-inspector -rf pom.xml -p inspectionprofile.xml

</div>

Optionally you can also

  - Use/Add a complete IDEA configuration (`.idea`) to your project.
    This allows more fine control (i.e. defining inspection scopes)

  - add a `.ideainspect` to control i.e. ignored files/inspections and
    other details. See the example file provided with
    idea-cli-inspector.

# Why this tool?

IntelliJ IDEA offers a vast amount of very high-quality, built-in code
inspections. Currently it has more than 1073 inspections to offer. Using
project-shared inspection profiles it’s possible **to guide developers**
in a wide range of coding aspects in a **non-annoying way**.

In contrast to well-known quality tools like
[SonarQube](http://www.sonarqube.org/) these inspections do have quite a
few benefits:

  - **violations are instantly visible** during code writing with freely
    configurable severities and warning levels. (i.e. bold red errors,
    yellow warning or discreet hints as recommendation)

  - **false alarms can be easily and immediately suppressed.** A simple
    Alt-Enter directly at the location where they occur is enough.
    Alternatively you can adjust your inspection settings just
    on-the-fly (i.e. reduce level, disable inspection or configure
    inspection properties)

  - IDEA allows you to **check for new violations introduced with your
    changes** *while you are trying to commit them*

  - Many inspections in IDEA provide semi-automatic quickfixes and
    auto-corrections. So again sometimes addressing an issue is as
    simply as pressing Alt-Enter Right arrow Enter.

  - Because all inspections are based on IDEA´s Psi engine (which is a
    sort of permanent running syntax compiler) the inspections
    effectively work on an actual AST. So IDEA *understands* the code
    and does not only look for (textual) patterns which in return leads
    to a **significantly lower rate of false-positives.**

Nevertheless, though IDEA offers on-the-fly analysis and error
visualization it does not stop the developer in committing code
violating these helpful guidelines into the project repository.
JetBrain´s CI solution [TeamCity](https://www.jetbrains.com/teamcity/)
does offer a easy and good integration. It is also possible to execute
the IDEA project inspection [on the command
line](https://www.jetbrains.com/idea/help/working-with-intellij-idea-features-from-command-line.html),
which will produce a hard-readable set of XML files and no further
support for integrating this into an automated tool chain.

Therefore I did hack this little Groovy script to easily include &
report inspection checks into your CI chain.

# What it does

This tool is aimed to simplify the inclusion of IDEA code inspection in
your CI chain. It executes a command-line based run of an IntelliJ
inspection run. This produces a set of hardly human-readable XML files
in the target directory.

Therefore the tool subsequently parses the generated inspection result
files, allows to filter out selected result files and looks for messages
with given severity (WARNING and ERROR by default).

The tool will list issues in a humand-readable form and exit with return
code 1 if it did find any issues (your CI tool should interpret this as
failure) or will happily tell you that everyhing is fine.

# Prerequisites & Limitations

The script is developed in Groovy, so this has to be installed.
Furthermore you need a valid installation of IntelliJ IDEA.

<div class="note">

Due to a limitation of IDEA itself it is not possible to run more than
one IDEA instance at the same time. Therefore you must ensure, that no
other IDEA is running on your PC / on your CI agents.

</div>

# Usage

## Configuration file base usage

IDEA CLI Inspector supports configuration via a `.ideainspect` file in
the project root directory. Below is a example:

    # Levels to look for. Default: WARNING,ERROR
    #levels: WARNING,ERROR,INFO

    # Apply an "Custom scope" for the analysis run
    # This is _the prefered way_ to limit your inspection run to a part of your project files
    # as it takes effect within IDEA itself.
    # See: https://www.jetbrains.com/help/idea/2016.2/specify-inspection-scope-dialog.html
    #
    # HOWTO:
    # 1) Create a new scope excluding undesired folders/files (node_modules, doc, ...)
    # 2) Share the .idea/scopes/scopename.xml with the project
    # 3) Use the _name_ of the scope (not the file).
    # 4) Stick to a single word for best compability
    scope: inspector-code

    # Inspection result files to skip. For example "TodoComment" or "TodoComment.xml".
    #
    # NOTE: This does not have an effect on which inspections are effectively run by IDEA!
    #       For the sake of performance better disable these inspections within your
    #       inspection profile. This here is a last-resort mechanism if you want them
    #       to appear in your IDE but not your CI process
    #skip: TodoComment,Annotator
    skip: GroovyAssignabilityCheck

    # Ignore issues affecting source files matching given regex. Example ".*/generated/.*".
    #
    # NOTE: This does not have an effect on the places IDEA looks. Therefore please prefer
    #       declaring an "scope" and exclude those locations via the IDEA scoping mechanism
    #       for the sake of performance.
    #       This here is a last-resort mechanism if you have no other options to supress
    #       specific places/warning.
    #skipfile: .*/generated/.*,src/main/Foo.java

    # Target directory to place the IDEA inspection XML result files. Default: target/inspection-results
    #resultdir: target/inspection-results

    # IDEA installation home directory. Default: IDEA_HOME environment variable or "idea".
    # ideahome: /home/ben/devel/idea

    # Limit IDEA inspection to this directory (This overrides scoping)
    # dir: .

    # Use this inspection profile file located ".idea/inspectionProfiles".
    profile: bentolor_2018.xml

    # IDEA project root directory containing the ".idea" directory
    # rootdir: .

    # Full path to the local idea.properties file. More info at:
    # http://tools.android.com/tech-docs/configuration
    # iprops: /Users/Shared/User/Library/Preferences/AndroidStudio2.1/idea.properties

## Command line based usage

For a full / up-to-date list of options please run `idea-cli-inspector
-h`:

    = IntellIJ IDEA Code Analysis Wrapper - v1.5.2 - @bentolor

    This tools runs IntelliJ IDEA inspection via command line and
    tries to parse the output.

    Example usage:
      ./idea-cli-inspector -i ~/devel/idea -r . -p myinspections.xml \
         -d src/main/java -s unused,Annotator,TodoComment.xml -l ERROR

    For more convenience you can pass all options in a `.ideainspect` file
    instead of passing it via command line

    usage: groovy idea-cli-inspector [options]
     -d,--dir <dir>           Limit IDEA inspection to this directory.
                              Overrides the scope argument.
     -h,--help                Show usage information and quit
     -i,--ideahome <dir>      IDEA or Android Studio installation home
                              directory. Default: IDEA_HOME env var or `idea`
     -ip,--iprops <file>      Full path to your `idea.properties`. Only
                              required if 1) you use --scope and 2) file is
                              not located under in the default.
                              Default: `<ideahome>/idea/bin/idea.properties`
     -l,--levels <level>      Levels to look for. Default: WARNING,ERROR
     -n,--dry-run             Dry-run: Do not start IDEA, but run parsing
     -p,--profile <file>      Use this inspection profile file. If given an
                              absolute path, the target file is used,
                              otherwise the arg denotes a file located under
                              `.idea/inspectionProfiles`.
                              Default: `Project_Default.xml`
     -r,--rootdir <dir>       IDEA project root directory containing the
                              `.idea` directory. Default: Working directory
     -rf,--rootfile <file>    full path to the pom.xml or build.gradle file
                              for the project. Useful if the project is maven
                              or gradle based and its rootdir does not contain
                              all the *.iml and .idea/modules.xml files
     -s,--skip <file>         Analysis result files to skip. For example
                              `TodoComment` or `TodoComment.xml`.
     -sc,--scope <string>     The name of the "Custom scope" to apply. Custom
                              scopes can be defined in the IDE. Share the
                              resulting file in .idea/scopes/scopename.xml and
                              provide the name of the scope (not file) here.
     -sf,--skipfile <regex>   Ignore issues affecting source files matching
                              given regex. Example: `.*/generated/.*`.
     -t,--resultdir <dir>     Target directory to place the IDEA inspection
                              XML result files.
                              Default: `target/inspection-results`
     -v,--verbose             Enable verbose logging

# Example usage

    $ groovy idea-cli-inspector \
        -i ~/devel/idea \
        -r ~/projects/p1 \
        -p myinspections.xml \
        -d server \
        -s unused,Annotator,TodoComment.xml \
        -l ERROR

This looks for a IntelliJ installation in `~/devel/idea`, tries to
perform a CLI-based code inspection run on the IDEA project
`~/projects/p1/.idea` with an inspection profile
`~/projects/p1/.idea/inspectionProfiles/myinspections.xml` limiting the
inspection run to the subdirectory `server` within your project.

The IDEA inspection run will produce a set of `.xml` files. The amount,
levels and result is based on the inspection profile you passed. Option
`-s` tells to skip & ignore the warnings contained in the inspection
result files `unused.xml`, `Annotator.xml` and `TodoComment.xml`. You
can ommit the `.xml` suffix for convenience.

By default it will then look for entries marked as `[WARNING]` or
`[ERROR]` within the remaining inspection result report files. In our
case we only care for ERROR entries. If it finds entries, it will report
the file joined with a description pointing to the file location and the
inspection rule.

# Vanilla Maven and Gradle projects

For many maven and gradle-based projects, .iml files and xml files under
`.idea/libraries` are not committed to SCM as they are generated by
IntelliJ based on maven/gradle files (see
<https://intellij-support.jetbrains.com/hc/en-us/articles/206544839-How-to-manage-projects-under-Version-Control-Systems>
after "You may consider not to share the following").

For such projects, the inspection must be launched by passing the path
to the maven/gradle project file like in the following example:

    $ groovy idea-cli-inspector \
        --ideahome ~/devel/idea \
        --rootdir ~/projects/p1 \
        --rootfile ~/projects/p1/pom.xml
        --profile ~/myinspections.xml \
        -l ERROR

# Example output

    ➜ idea-cli-inspector.git git:(master) ✗ ./idea-cli-inspector -i ~/devel/idea -r ../dashboard.git -p bens_idea15_2015_11.xml -d server -s Annotator,JSUnresolvedLibraryURL.xml,JavaDoc,TodoComment -l ERROR,WARNING

    = IntellIJ IDEA Code Analysis Wrapper - v1.0 - @bentolor
    #
    # Running IDEA IntelliJ Inspection
    #
    Executing: /home/ben/devel/idea/bin/idea.sh [/home/ben/devel/idea/bin/idea.sh, inspect, /home/ben/projects/idea-cli-inspector.git/../dashboard.git, /home/ben/projects/idea-cli-inspector.git/../dashboard.git/.idea/inspectionProfiles/bens_idea15_2015_11.xml, /home/ben/projects/idea-cli-inspector.git/../dashboard.git/target/inspection-results, -d, server]
    log4j:WARN No appenders could be found for logger (io.netty.util.internal.logging.InternalLoggerFactory).
    log4j:WARN Please initialize the log4j system properly.
    log4j:WARN See http://logging.apache.org/log4j/1.2/faq.html#noconfig for more info.
    Please configure library 'Node.js v4.2.1 Core Modules' which is used in module 'client'

       ...
       IDEA spilling out quite a bunch of exceptions during inspection run
       ...

    #
    # Inspecting produced result files in ../dashboard.git/target/inspection-results
    #
    # Looking for: [[WARNING], [ERROR]]
    # Ignoring : [Annotator, JSUnresolvedLibraryURL, JavaDoc, TodoComment]
    --- ClassNamePrefixedWithPackageName.xml
    [WARNING] server/src/main/java/de/foo/dashboard/data/DatasetVerticle.java:28 -- Class name <code>DatasetVerticle</code> begins with its package name #loc
    [WARNING] server/src/main/java/de/foo/dashboard/data/DatasetBuilder.java:17 -- Class name <code>DatasetBuilder</code> begins with its package name #loc

    --- InterfaceNamingConvention.xml
    [WARNING] server/src/main/java/de/foo/dashboard/constants/Events.java:11 -- Interface name <code>Events</code> is too short (6 < 8) #loc

    --- SameParameterValue.xml
    [WARNING] server/src/main/java/de/foo/dashboard/data/DatasetBuilder.java:30 -- Actual value of parameter '<code>type</code>' is always '<code>de.exxcellent.dashboard.constants.DatasetType.ARRAY</code>'

    --- Skipping JavaDoc.xml
    --- Skipping TodoComment.xml
    --- DeprecatedClassUsageInspection.xml
    [WARNING] server/pom.xml:99 -- 'io.vertx.core.Starter' is deprecated

    --- Skipping JSUnresolvedLibraryURL.xml
    --- Skipping Annotator.xml
    --- unused.xml
    [WARNING] server/src/main/java/de/foo/dashboard/data/DatasetBuilder.java:40 -- Method is never used.
    [WARNING] server/src/main/java/de/foo/dashboard/constants/DatasetType.java:14 -- Field has no usages.
    [WARNING] server/src/main/java/de/foo/dashboard/constants/DatasetType.java:14 -- Field has no usages.
    [WARNING] server/src/main/java/de/foo/dashboard/data/DatasetVerticle.java:28 -- Class is not instantiated.
    [WARNING] server/src/main/java/de/foo/dashboard/transformers/History.java:23 -- Class is not instantiated.

    #
    # Analysis Result
    #
    Entries found. return code: 1

# Running within a Docker container (i.e. Travis CI)

Here is a `.travis.yml` which demonstrates how to run
`idea-cli-inspector` within a Docker container. You can see this in
practice running [here on Travis
CI](https://www.travis-ci.org/bentolor/microframeworks-showcase/) with
the source inspected [in my microframeworks-showcase
project](https://github.com/bentolor/microframeworks-showcase/)

Two things to note:

  - IDEA needs some very basic configuration already existing. At least
    i.e. the `.IntelliJIdea2018.1/config/options/jdk.table.xml` which
    defines the locations of the installed JDKs

  - The IDEA configuration directory location *varies from version to
    version and edition to edition*. I.e. it’s `~/.IntelliJIdea2018.1`
    for the IDEA 2018.1 Ultimate edition and `~/.IdeaIC2018.1` for the
    community edition

  - If you are using i.e. Node, Scala, VueJS etc. in your project please
    note, that these plugins bring more inspections to the table. If you
    want to have them in your CI/Docker run to, ensure that you add them
    to i.e. `.IntelliJIdea2017.3/config/plugins/` directory so they are
    picked up and effective.

  - You can build these required configurations either by manually
    adjusting and then copying those configuration file into the
    container or by i.e. manually starting IDEA within the container
    once with the configuration directories mapped as Docker volumes
    i.e. like:

<!-- end list -->

    xhost si:localuser:root
    docker run -it --rm \
               --dns 192.168.144.18 --dns 8.8.8.8 \
               -e DISPLAY=$DISPLAY -v /tmp/.X11-unix:/tmp/.X11-unix \
               -v `pwd`/root/.IntelliJIdea2018.1:/root/.IntelliJIdea2018.1 \
               -v `pwd`/root/.java:/root/.java  debug-ideacli-dockeragent  \
               /bin/bash

    language: java

    before_cache:
      - rm -f  $HOME/.gradle/caches/modules-2/modules-2.lock
      - rm -fr $HOME/.gradle/caches/*/plugin-resolution/

    cache:
      directories:
        - $HOME/.gradle/caches/
        - $HOME/.gradle/wrapper/

    before_install:
      - sudo add-apt-repository ppa:mmk2410/intellij-idea -y
      - sudo apt-get update -q
      - sudo apt-get install intellij-idea-community -y
      - sudo apt-get install groovy -y

    install:
      - wget https://github.com/bentolor/idea-cli-inspector/archive/master.zip
      - unzip master.zip
      - sudo chmod -R aog+w /opt/intellij-idea-community/bin

    script:
      # Copy idea configuration template (mostly .IntelliJIdea2018.1/config/options/jdk.table.xml)
      - cp -r ./tools/idea-cli-inspector/root/.IntelliJIdea2018.1 /home/travis/
      # Duplicate for community config dir
      - cp -r ./tools/idea-cli-inspector/root/.IntelliJIdea2018.1 /home/travis/.IdeaIC2018.1
      - ./idea-cli-inspector-master/idea-cli-inspector -i /opt/intellij-idea-community

# Troubleshooting & FAQ

**My inspection runs very long and takes to much time. What can I do?.**

First: Introduce and use a new scope where you exclude all folders
and/or include only those folders which are relevant for your
inspection. Typical folders which are not relevant are i.e.
`node_modules`, `docs` or build output folders. Secondly think about
creating and using a custom inspection profile for the purpose of the
CI. There you i.e. might disable all inspections with INFO/HINT level or
i.e. the spell checking.

**I receive a error message *Please, specify sdk 'null' for module
'foo'*.**

Probably you excluded `misc.xml` from the versionied IDEA project. Which
is fine because this file is quite volatile. But this is the file where
IDEA stores the "Root JDK".

To fix this error simply assign every module a SDK other than "Project
SDK".

**The analysis seems to produce different results on subsequent runs on
the same sources (esp. JavaScript).**

This seems to be an issue with the IDEA caches which IntelliJ keeps i.e.
under `.IntelliJ201X.X/system`. Try if deleting this directory prior to
executing the analysis runs produces stable results.

**What shall I pass as IDEA home directory for Mac OSX?.**

`/Applications/IntelliJ\ IDEA.app` should be the default installation
folder.

**I’m using Android Studio and I can’t find `idea.properties`\!.**

See the details about the location at
<http://tools.android.com/tech-docs/configuration>

**Scoping does not work?.**

First: Did you versionate the `.idea/scope/scopename.xml` file in your
project? Did you specify the Custom scope *name* and not the filename?

If no, it might be that there is an issue that you are not pointing the
`--iprops` option to the right `idea.properties` file or you i.e. don’t
have write access for it.

*Background information and troubleshooting:* Unfortunately IDEA yet
does not offer a direct CLI option for applying scope. Therefore we need
to pass a System property entries via modifying the `idea.properties`
file. Check the output of the script if you spot any issues during this
process. Check the content of `idea.properties` during the run and look
out for you scope name.

**I receive different results in my IDE vs. on the CI server.**

Please check the following:

  - ❏ Same version of IDEA installed on CI vs. you installation?

  - ❏ Do you have the same plugins installed? Some bring own inspections
    or support fur understanding file formats (like `.vue` files with
    the vue plugin).

  - ❏ Do you delete the `.IntelliJ201X.X/system` folder prior to every
    run on CI (see above)?

# Source code & Contributions

The source code is located under
<https://github.com/bentolor/idea-cli-inspector>.

# License

Licensed under the Apache License, Version 2.0 (the "License"); you may
not use this file except in compliance with the License.

You may obtain a copy of the License at
<http://www.apache.org/licenses/LICENSE-2.0>
