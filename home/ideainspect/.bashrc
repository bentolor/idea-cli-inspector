# Workaround: openjdk-Base image does not update links to /usr/lib/jvm/default-java/bin/java
export JAVA_HOME=/opt/java/openjdk/
export PATH=/opt/java/openjdk/bin:$PATH

# Point to IDEA installation directory
export ENV IDEA_HOME=/srv/idea.latest

# Language settings from Dockerfile. Adopt also for user bamboo
export LANG="en_US.UTF-8"
export LC_MESSAGES="C"

#THIS MUST BE AT THE END OF THE FILE FOR SDKMAN TO WORK!!!
export SDKMAN_DIR="/home/ideainspect/.sdkman"
[[ -s "/home/ideainspect/.sdkman/bin/sdkman-init.sh" ]] && source "/home/ideainspect/.sdkman/bin/sdkman-init.sh"