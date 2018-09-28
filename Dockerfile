#
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
RUN apt-get clean && \
    apt-get update && \
    apt-get install -y \
        wget sudo \
#        curl zip \
#        openssh-client git subversion \
#        software-properties-common  \
        && \
#    apt-get dist-upgrade -yqq && \
    apt-get autoremove --purge -y && \
    apt-get clean && \
    rm /var/lib/apt/lists/*.* && \
    rm -fr /tmp/* /var/tmp/*

# Install Oracle Java PPAs
#RUN echo "deb http://ppa.launchpad.net/webupd8team/java/ubuntu xenial main" | tee /etc/apt/sources.list.d/webupd8team-java.list
#RUN apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys EEA14886
#RUN echo "deb http://ppa.launchpad.net/linuxuprising/java/ubuntu xenial main" | tee /etc/apt/sources.list.d/linuxuprising-java.list
#RUN apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys 73C3DB2A

# Mark Oracle license accepted
#RUN echo debconf shared/accepted-oracle-license-v1-1 select true | debconf-set-selections
#RUN echo debconf shared/accepted-oracle-license-v1-1 seen true | debconf-set-selections
#RUN echo oracle-java10-installer shared/accepted-oracle-license-v1-1 select true | debconf-set-selections

# Install Java 8, Java 10, Groovy, mongodb-client & graphviz via package repository
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
#                       build-essential \
#                       oracle-java8-installer \
#                       oracle-java8-set-default \
#                       oracle-java8-unlimited-jce-policy \
#                       oracle-java10-installer \
                       groovy && \
    apt-get autoremove --purge -y && \
    apt-get clean && \
#    rm -fr /var/cache/oracle-jdk* && \
    rm /var/lib/apt/lists/*.* && \
    rm -fr /tmp/* /var/tmp/*

# Provide a non-privileged user for running IntelliJ
RUN useradd -mUs /bin/bash ideainspect

#
# Install various tools into /srv
#
# Install IntelliJ IDEA
ENV V_IDEA 2018.2.4
# Set C for Community or U for Ultimate
ENV V_IDEA_EDITION C
ENV IDEA_CONFDIR .IdeaIC2018.2
# For Ultimate it is: ENV IDEA_CONFDIR .IntelliJIdea2018.2
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


# Install Android SDK Tools
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
# === Install the remaining, smaller libs
#
# Install Maven -- Check: https://maven.apache.org/download.cgi
#ENV V_MAVEN3 3.5.4
#RUN cd /srv && \
#    curl -sL http://mirror.synyx.de/apache/maven/maven-3/$V_MAVEN3/binaries/apache-maven-$V_MAVEN3-bin.tar.gz | tar -xz && \
#    ln -s apache-maven-$V_MAVEN3 maven-3.x
#
# Install 8.x node - this does an implicit apt-get update!
#RUN ( curl -sL https://deb.nodesource.com/setup_8.x | bash - ) && \
#    apt-get install -y nodejs && \
#    apt-get clean && \
#    rm /var/lib/apt/lists/*.* && \
#    rm -fr /tmp/* /var/tmp/*

ENV A A
# Copy files into container
COPY / /

# Fix `root` users from COPY and other commands
RUN chown -R ideainspect:ideainspect /home/ideainspect

# Provide an entry point script which also creates starts Bamboo with a
# dedicated user
ENTRYPOINT ["/docker-entrypoint.sh"]

# Define default command.
CMD ["--help"]
