# Docker-based IntelliJ IDEA Inspections using idea-cli-inspector
#
# NOTE:
#  This Dockerfile provides quite a bunch of commented-out statements
#  as template for creatng your own, derived docker image in case i.e.
#  you want to use the Ultimate edition or need additional build tools.
#
FROM        openjdk:8
MAINTAINER  Benjamin Schmid <dockerhub@benjamin-schmid.de>


# First install some basic tools to get them or their latest versions (wget, apt).
RUN  apt-get update &&  apt-get install -y wget sudo locales groovy && \
    apt-get autoremove --purge -y && apt-get clean && \
    rm /var/lib/apt/lists/*.* && rm -fr /tmp/* /var/tmp/*

# --------------- Install Oracle Java PPAs
#RUN echo "deb http://ppa.launchpad.net/webupd8team/java/ubuntu xenial main" | tee /etc/apt/sources.list.d/webupd8team-java.list
#RUN apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys EEA14886
#RUN echo "deb http://ppa.launchpad.net/linuxuprising/java/ubuntu xenial main" | tee /etc/apt/sources.list.d/linuxuprising-java.list
#RUN apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys 73C3DB2A
#
# Mark Oracle license accepted
#RUN echo debconf shared/accepted-oracle-license-v1-1 select true | debconf-set-selections
#RUN echo debconf shared/accepted-oracle-license-v1-1 seen true | debconf-set-selections
#RUN echo oracle-java10-installer shared/accepted-oracle-license-v1-1 select true | debconf-set-selections
#
# Install Java 8, Java 10, Groovy, mongodb-client & graphviz via package repository
#RUN apt-get update && \
#    apt-get install -y --no-install-recommends \
#                       oracle-java8-installer \
#                       oracle-java8-set-default \
#                       oracle-java8-unlimited-jce-policy \
#                       oracle-java10-installer \
#                       && \
#    apt-get autoremove --purge -y && \
#    apt-get clean && \
#    rm -fr /var/cache/oracle-jdk* && \
#    rm /var/lib/apt/lists/*.* && \
#    rm -fr /tmp/* /var/tmp/*
#
#
# --------------- Install Android SDK Tools
#RUN mkdir -p /srv/android-sdk && cd /srv/android-sdk && \
#    wget -nv https://dl.google.com/android/repository/sdk-tools-linux-4333796.zip && \
#    echo "92ffee5a1d98d856634e8b71132e8a95d96c83a63fde1099be3d86df3106def9  sdk-tools-linux-4333796.zip" | sha256sum -c - && \
#    unzip -q sdk-tools-linux-4333796.zip && \
#    rm sdk-tools-linux-4333796.zip && \
#    find /srv/android-sdk -executable  -type f -exec chmod o+x \{\} \;
#
# Install Android SDKs & Build Tools
#RUN yes | /srv/android-sdk/tools/bin/sdkmanager --licenses > /dev/null && \
#    /srv/android-sdk/tools/bin/sdkmanager "platforms;android-26" "platforms;android-27" "build-tools;26.0.3" "build-tools;27.0.3" "platform-tools" > /dev/null
#
# --------------- Install 8.x node - this does an implicit apt-get update!
#RUN ( curl -sL https://deb.nodesource.com/setup_8.x | bash - ) && \
#    apt-get install -y nodejs && \
#    apt-get clean && \
#    rm /var/lib/apt/lists/*.* && \
#    rm -fr /tmp/* /var/tmp/*


# Provide a non-privileged user for running IntelliJ
RUN useradd -mUs /bin/bash ideainspect

#
# Install IntelliJ IDEA
#
# IMPORTANT NOTES
#
# 1. V_IDEA_EDITION defines, which edition to build. Use C for Community or U for Ultimate
#
# 2. IDEA_CONFDIR is depending on the edition & version:
#    I.e. its .IdeaIC2018.3 for the 2018.3 Community edition and .IntelliJIdea2018.3 for the same ultimate dition
#
# 3. The first run to pre-populate the indexes won't work with ultimate edition, yet. This is due to outstanding features in
#    the current Docker daemon. See https://github.com/moby/buildkit/issues/763
#
ENV V_IDEA 2018.3.3
ENV V_IDEA_EDITION C
ENV IDEA_CONFDIR .IntelliJIdea2018.3
# For Ultimate it is: ENV IDEA_CONFDIR .IntelliJIdea2018.3
RUN cd /srv && \
    wget -nv https://download.jetbrains.com/idea/ideaI$V_IDEA_EDITION-$V_IDEA-no-jdk.tar.gz && \
    tar xf ideaI$V_IDEA_EDITION-$V_IDEA-no-jdk.tar.gz && \
    ln -s idea-I$V_IDEA_EDITION-* idea.latest && \
    # The idea-cli-inspector needs write access to the IDEA bin directory as a hack for scope
    chown -R ideainspect:ideainspect /srv/idea.latest/bin && \
    mkdir /home/ideainspect/$IDEA_CONFDIR && \
    ln -s /home/ideainspect/$IDEA_CONFDIR idea.config.latest && \
    rm ideaI$V_IDEA_EDITION-$V_IDEA-no-jdk.tar.gz

# Point inspector to the new home
# NOTE: This only takes effect for user `root`. For user ideainspect check home/ideainspect/.bashrc
ENV IDEA_HOME /srv/idea.latest

# The default locale is POSIX which breaks UTF-8 based javac files
# NOTE:
#    This only taked effect for user root. Check home/ideainspect/.bashrc for main user
#    environment variables
RUN locale-gen en_US.UTF-8
RUN update-locale en_US.UTF8
ENV LANG "en_US.UTF-8"
ENV LC_MESSAGES "C"

# Copy files into container
COPY /idea-cli-inspector /
COPY /docker-entrypoint.sh /

# Bash Environments & Default IDEA config
COPY /home /home
RUN chown -R ideainspect:ideainspect /home/ideainspect

# Prepare a sample project
COPY / /project
RUN chown -R ideainspect:ideainspect /project

# Initial run to populate index i.e. for JDKs. This should reduce startup times.
# NOTE: This won't run for Ultimate Edition, as a licence key is missing during execution and current docker
#       version provide no means to inject secrets during build time. JUST COMMENT IT OUT FOR NOW IN CASE OF ISSUES
RUN [ "/docker-entrypoint.sh", "-r", "/project" ]
#
#
#  At some time this might work, by providing the idea.key as a secret during build time:
#RUN --mount=type=secret,id=idea.key,target=/srv/idea.config.latest/idea.key,required,mode=0444 [ "/docker-entrypoint.sh", "-r","/project" ]
#
#  To get this working you need to:
#   1. add th following line on the very top of this file
#         # syntax = docker/dockerfile:experimental
#   2. Build the image with BuildKit enabled:
#       DOCKER_BUILDKIT=1 docker build --secret id=idea.key,src=/home/ben/.IntelliJIdea2018.3/config/idea.key \
#                                          -t bentolor/idea-cli-inspector .
#

# Provide an entry point script which also creates starts Bamboo with a
# dedicated user
ENTRYPOINT ["/docker-entrypoint.sh"]

# Define default command.
CMD ["--help"]
