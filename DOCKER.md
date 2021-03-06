# Analyzing project using the Docker version of idea-cli-inspector

There is an image `bentolor/idea-cli-inspector` available on Docker Hub
which you can use to start and run `idea-cli-inspector` without any
further configuration. The project to be analyzed is expected to be
available in `/project` within the running container. Example usage:

<div class="informalexample">

    docker run --rm -v $(pwd):/project  bentolor/idea-cli-inspector -rf pom.xml -p inspectionprofile.xml

</div>

# General overview creating your own Docker containers

The `Dockerfile` provided in this repository will create a minimum,
Java-based Docker environment which is able to run a IDEA CLI
inspection.

It is based on

  - IntelliJ Community Edition (vanilla)

  - AdoptOpenJDK Java 11

In your practical usage scenario you might probably have different needs
like:

  - Using the commercial Ultimate Edition for more support

  - using additional inspections provided by plugins

  - additional Tooling (like NodeJS)

This guide tries to explain how to customize your idea-cli-inspector
instance.

  - NOTE  
    [@sylvainlaurent/docker-intellij-inspect](https://github.com/sylvainlaurent/docker-intellij-inspect)
    created a very similar approach which is in some aspects more
    advanced than this implementation. You might also have a look there.

# Visually installing/adjusting IDEA settings

IntelliJ stores its settings in a `IdeaIC2018.3/config` directory (or
`.IntelliJ2018.3/config` for Ultimate editions). Most of these settings
do not matter so they are excluded in the `.dockerignore` file.

But if you want i.e. install additional plugins, set the licence server
for the Ultimate Edition or add/define additional SDKs you can do this
interactive.

Therefore just run

    xhost si:localuser:root
    docker run -it --rm -e DISPLAY=$DISPLAY \
              -v /tmp/.X11-unix:/tmp/.X11-unix  \
              -v `pwd`/home:/home \
              bentolor/idea-cli-inspector su ideainspect -c '/srv/idea.latest/bin/idea.sh'

This will launch IDEA inside the docker container. After configuration
check the modifications in `home/ideainspect` and add them to the
Container (i.e. adjusting `.dockerignore`).
