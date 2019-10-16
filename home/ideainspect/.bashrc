# Workaround: openjdk-Base image does not update links to /usr/lib/jvm/default-java/bin/java
export JAVA_HOME=/usr/local/openjdk-8/
export PATH=/usr/local/openjdk-8/bin:$PATH
# Point to IDEA installation directory
export ENV IDEA_HOME=/srv/idea.latest
# Language settings from Dockerfile. Adopt also for user bamboo
export LANG="en_US.UTF-8"
export LC_MESSAGES="C"