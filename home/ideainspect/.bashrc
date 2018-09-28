# Workaround: openjdk-Base image does not update links to /usr/lib/jvm/default-java/bin/java
export JAVA_HOME=/docker-java-home
# Point to IDEA installation directory
export ENV IDEA_HOME=/srv/idea.latest